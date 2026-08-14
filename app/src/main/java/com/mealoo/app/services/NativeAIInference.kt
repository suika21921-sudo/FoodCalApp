package com.mealoo.app.services

interface NativeInferenceCallback {
    fun onTokenGenerated(token: String)
}

/**
 * llama.cpp JNI 橋接單例。
 *
 * 透過 System.loadLibrary 載入由 CMake 編譯的 mealoo_native 共享庫，
 * 提供模型載入、推理、釋放三個核心 native 方法。
 */
object NativeAIInference {

    init {
        System.loadLibrary("mealoo_native")
    }

    /**
     * 載入 GGUF 模型檔案。
     * - 啟用 mmap 模式以節省 RAM
     * - 固定 4 執行緒
     *
     * @param modelPath 模型檔案的絕對路徑
     * @return true 表示載入成功
     */
    external fun loadModel(modelPath: String): Boolean

    /**
     * 對給定的 prompt 執行推理，回傳生成的文字。
     *
     * @param prompt    完整的 prompt 文字 (含系統指令與使用者輸入)
     * @param maxTokens 最大生成 token 數
     * @return 模型生成的回應文字
     */
    external fun generateResponse(prompt: String, maxTokens: Int): String

    /**
     * 對給定的 prompt 執行推理，並透過 callback 逐字回傳生成的文字。
     */
    external fun generateResponseWithCallback(prompt: String, maxTokens: Int, callback: NativeInferenceCallback): String

    /**
     * 釋放模型與 context 佔用的記憶體。
     * 應在不再需要推理時呼叫 (例如 Activity onDestroy)。
     */
    external fun freeModel()
}
