#include <jni.h>
#include <string>
#include <vector>      // 修正: 必須包含此標頭檔，解決 std::vector 找不到的問題
#include <android/log.h>
#include "llama.h"

#define TAG "MealooNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
static int g_n_system_tokens = -1;
static llama_model   *g_model   = nullptr;
static llama_context *g_ctx     = nullptr;

extern "C" {

// ────────────────────────────────────────────────────────
//  loadModel: 載入模型 (確認大括號正確閉合)
// ────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL
Java_com_mealoo_app_services_NativeAIInference_loadModel(
        JNIEnv *env, jobject /* this */, jstring jModelPath) {

    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }

    const char *model_path = env->GetStringUTFChars(jModelPath, nullptr);
    LOGI("正在載入模型: %s", model_path);

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true;
    model_params.n_gpu_layers = 100; // 強制將模型所有層都交給 GPU (Vulkan)

    g_model = llama_model_load_from_file(model_path, model_params);
    env->ReleaseStringUTFChars(jModelPath, model_path);

    if (!g_model) return JNI_FALSE;

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx     = 2048;
    ctx_params.n_batch     = 512;
    ctx_params.n_ubatch     = 512;
    ctx_params.n_threads = 6;
    ctx_params.n_threads_batch = 4;

    g_ctx = llama_init_from_model(g_model, ctx_params);
    return (g_ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
}

// ────────────────────────────────────────────────────────
//  generateResponse: 執行推理 (修正 API 調用)
// ────────────────────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_mealoo_app_services_NativeAIInference_generateResponse(
        JNIEnv *env, jobject /* this */, jstring jPrompt, jint maxTokens) {

    if (!g_model || !g_ctx) return env->NewStringUTF("錯誤：模型尚未載入");

    const char *prompt_cstr = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt(prompt_cstr);
    env->ReleaseStringUTFChars(jPrompt, prompt_cstr);

    const llama_vocab *vocab = llama_model_get_vocab(g_model);

    // 1. Tokenize 整個 Prompt
    std::vector<llama_token> tokens(prompt.size() + 128);
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                                  tokens.data(), tokens.size(), true, false);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                                  tokens.data(), tokens.size(), true, false);
    }
    tokens.resize(n_tokens);

    // 2. 🚀 [關鍵優化]：辨識 System Prompt 邊界
    // 如果是第一次執行，找出 <|im_end|> 第一次出現的位置 (System 結束處)
    if (g_n_system_tokens == -1) {
        // 簡單邏輯：假設 System Prompt 佔用了前 N 個 token
        // 在你的專案中，System 是固定的，我們可以手動算或找標記
        // 這裡建議在 Kotlin 傳入前先算好，或是在這裡搜尋第一個 <|im_end|> 的 token id
        // 暫定為前 50 個 token (請根據你實際的 System Prompt 長度調整，或動態偵測)
        g_n_system_tokens = 45;
        LOGI("初始化 System Cache, 長度: %d", g_n_system_tokens);
    }

    llama_memory_t mem = llama_get_memory(g_ctx);

    // 3. 🚀 [關鍵優化]：只清理 User 部分，保留 System
    // llama_memory_seq_rm(mem, seq_id, p_start, p_end)
    // 我們要保留 [0, g_n_system_tokens)，所以從 g_n_system_tokens 開始刪除到最後 (-1)
    llama_memory_seq_rm(mem, 0, g_n_system_tokens, -1);

    // 4. 建立 Batch：只需要 decode 從 System 之後的新 token
    // 注意：如果是第一次跑，要從 0 開始；如果 cache 已存在，從 g_n_system_tokens 開始
    int n_past = g_n_system_tokens;
    int n_new_tokens = n_tokens - n_past;

    llama_batch batch = llama_batch_init(n_new_tokens, 0, 1);
    for (int i = 0; i < n_new_tokens; i++) {
        batch.token[batch.n_tokens]    = tokens[n_past + i];
        batch.pos[batch.n_tokens]      = n_past + i;
        batch.n_seq_id[batch.n_tokens] = 1;
        batch.seq_id[batch.n_tokens][0]= 0;
        batch.logits[batch.n_tokens]   = false;
        batch.n_tokens++;
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(g_ctx, batch) != 0) {
        llama_batch_free(batch);
        return env->NewStringUTF("錯誤：Decode 失敗");
    }

    // 5. 生成迴圈 (保持不變)
    std::string result;
    int n_cur = n_tokens;
    const int n_max = n_tokens + (int)maxTokens;

    // ── 修正後的採樣鏈 ──
    auto *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());

    // 1. [解決重複問題]：加入重複懲罰 (Repetition Penalty)
    // 參數依序為：最後 N 個 token, 懲罰強度(>1.0), 頻率懲罰, 存在懲罰
    // 這樣可以強迫模型不要一直噴「。」或是重複的字
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(64, 1.15f, 0.1f, 0.1f));

    // 2. [解決亂碼問題]：加入 Top-K 與 Top-P 篩選
    // 這會過濾掉機率極低的「噪音 token」，避免出現 ??? 這種無意義字元
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));

    // 3. 基礎參數
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f)); // 稍微調低溫度，讓輸出更穩定
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(42));

    while (n_cur < n_max) {
        llama_token new_token = llama_sampler_sample(smpl, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, new_token)) break;

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);

        // [硬性終止] 攔截關鍵字，防止模型胡言亂語
        if (result.find("<|im_end|>") != std::string::npos ||
            result.find("<|i") != std::string::npos ||
            result.find("user") != std::string::npos ||
            result.find("\nuser\n") != std::string::npos ||
            result.find("食材分析師") != std::string::npos) {
            break;
        }

        batch.n_tokens = 0;
        batch.token[batch.n_tokens]    = new_token;
        batch.pos[batch.n_tokens]      = n_cur;
        batch.n_seq_id[batch.n_tokens] = 1;
        batch.seq_id[batch.n_tokens][0]= 0;
        batch.logits[batch.n_tokens]   = true;
        batch.n_tokens++;

        if (llama_decode(g_ctx, batch) != 0) break;
        n_cur++;
    }

    llama_sampler_free(smpl);
    llama_batch_free(batch);
    return env->NewStringUTF(result.c_str());
}

// ────────────────────────────────────────────────────────
//  generateResponseWithCallback: 執行推理，並透過 callback 逐字回傳
// ────────────────────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_mealoo_app_services_NativeAIInference_generateResponseWithCallback(
        JNIEnv *env, jobject /* this */, jstring jPrompt, jint maxTokens, jobject callback) {

    if (!g_model || !g_ctx) return env->NewStringUTF("錯誤：模型尚未載入");

    const char *prompt_cstr = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt(prompt_cstr);
    env->ReleaseStringUTFChars(jPrompt, prompt_cstr);

    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    std::vector<llama_token> tokens(prompt.size() + 128);

    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                                  tokens.data(), tokens.size(), true, false);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                                  tokens.data(), tokens.size(), true, false);
    }
    tokens.resize(n_tokens);

    llama_memory_t mem = llama_get_memory(g_ctx);
    llama_memory_seq_rm(mem, -1, -1, -1);

    llama_batch batch = llama_batch_init(n_tokens, 0, 1);
    for (int i = 0; i < n_tokens; i++) {
        batch.token[batch.n_tokens]    = tokens[i];
        batch.pos[batch.n_tokens]      = i;
        batch.n_seq_id[batch.n_tokens] = 1;
        batch.seq_id[batch.n_tokens][0]= 0;
        batch.logits[batch.n_tokens]   = false;
        batch.n_tokens++;
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(g_ctx, batch) != 0) {
        llama_batch_free(batch);
        return env->NewStringUTF("錯誤：Decode 失敗");
    }

    std::string result;
    int n_cur = n_tokens;
    const int n_max = n_tokens + (int)maxTokens;

    // ── 修正後的採樣鏈 ──
    auto *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());

    // 1. [解決重複問題]：加入重複懲罰 (Repetition Penalty)
    // 參數依序為：最後 N 個 token, 懲罰強度(>1.0), 頻率懲罰, 存在懲罰
    // 這樣可以強迫模型不要一直噴「。」或是重複的字
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(64, 1.15f, 0.1f, 0.1f));

    // 2. [解決亂碼問題]：加入 Top-K 與 Top-P 篩選
    // 這會過濾掉機率極低的「噪音 token」，避免出現 ??? 這種無意義字元
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));

    // 3. 基礎參數
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f)); // 稍微調低溫度，讓輸出更穩定
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(42));

    jclass cbClass = nullptr;
    jmethodID onTokenMethod = nullptr;
    if (callback != nullptr) {
        cbClass = env->GetObjectClass(callback);
        onTokenMethod = env->GetMethodID(cbClass, "onTokenGenerated", "(Ljava/lang/String;)V");
    }

    // ── 宣告在 while 迴圈外面 ──
    std::string pending_buffer;

    while (n_cur < n_max) {
        llama_token new_token = llama_sampler_sample(smpl, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, new_token)) break;

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) {
            std::string piece(buf, n);
            result += piece;

            // [硬性終止] 攔截關鍵字，防止模型胡言亂語
            if (result.find("<|im_end|>") != std::string::npos ||
                result.find("user") != std::string::npos ||
                result.find("\nuser\n") != std::string::npos ||
                result.find("食材分析師") != std::string::npos) {
                break;
            }

            // 將新收到的片段加入候車室
            pending_buffer += piece;

            // 🚀 核心邏輯：從最後面往回找 UTF-8 的「起始字節」
            int i = pending_buffer.size() - 1;
            while (i >= 0 && (pending_buffer[i] & 0xC0) == 0x80) {
                i--; // 如果是接續字節 (10xxxxxx)，就繼續往前找
            }

            int needed = 1;
            if (i >= 0) {
                unsigned char c = pending_buffer[i];
                if ((c & 0xE0) == 0xC0) needed = 2;      // 需要 2 bytes
                else if ((c & 0xF0) == 0xE0) needed = 3; // 中文通常在這裡，需要 3 bytes
                else if ((c & 0xF8) == 0xF0) needed = 4; // 需要 4 bytes (Emoji 等)
            }

            // 如果目前緩衝區的長度，已經足夠拼出這個字，就送出！
            if (i >= 0 && (pending_buffer.size() - i) >= needed) {
                if (callback != nullptr && onTokenMethod != nullptr) {
                    jstring jPiece = env->NewStringUTF(pending_buffer.c_str());
                    env->CallVoidMethod(callback, onTokenMethod, jPiece);
                    env->DeleteLocalRef(jPiece);
                }
                pending_buffer.clear(); // 送出後清空候車室
            }
        }

        // ... (底下的 batch 處理保持不變) ...
        batch.n_tokens = 0;
        batch.token[batch.n_tokens]    = new_token;
        batch.pos[batch.n_tokens]      = n_cur;
        batch.n_seq_id[batch.n_tokens] = 1;
        batch.seq_id[batch.n_tokens][0]= 0;
        batch.logits[batch.n_tokens]   = true;
        batch.n_tokens++;

        if (llama_decode(g_ctx, batch) != 0) break;
        n_cur++;
    }

    // 迴圈結束後，如果候車室還有殘留的東西（例如意外斷句），也強制送出
    if (!pending_buffer.empty() && callback != nullptr && onTokenMethod != nullptr) {
        jstring jPiece = env->NewStringUTF(pending_buffer.c_str());
        env->CallVoidMethod(callback, onTokenMethod, jPiece);
        env->DeleteLocalRef(jPiece);
    }

    if (cbClass != nullptr) {
        env->DeleteLocalRef(cbClass);
    }

    llama_sampler_free(smpl);
    llama_batch_free(batch);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_mealoo_app_services_NativeAIInference_freeModel(JNIEnv *env, jobject /* this */) {
if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
if (g_model) { llama_model_free(g_model); g_model = nullptr; }
}

} // extern "C" 結尾 (確保這裡只有一個大括號)