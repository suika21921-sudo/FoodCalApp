package com.mealoo.app.services

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object AIInferenceService {
    private const val TAG = "AIInferenceService"
    const val MODEL_NAME = "mealoo_v1_q4.gguf"

    fun hasModel(context: Context): Boolean {
        return File(context.filesDir, MODEL_NAME).exists()
    }

    private suspend fun autoSetupModelFromAssets(context: Context): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(context.filesDir, MODEL_NAME)
        // 如果檔案已存在且有內容，直接跳過，加速啟動
        if (modelFile.exists() && modelFile.length() > 0) return@withContext true

        try {
            Log.i(TAG, "Debug 軌道：首次啟動，正在從 Assets 提取模型到沙盒...")
            context.assets.open(MODEL_NAME).use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "模型提取完成")
            true
        } catch (e: Exception) {
            Log.e(TAG, "模型提取失敗: ${e.message}")
            false
        }
    }

    // 🚀 新增：雙軌制核心路徑分配器
    private suspend fun getReadyModelPath(context: Context): String? = withContext(Dispatchers.IO) {
        val internalFile = File(context.filesDir, MODEL_NAME)

        // 判斷當前是 Debug 還是 Release 模式 (透過 ApplicationInfo flags)
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            // 【軌道一】：開發期 (Debug)，自動從 Assets 複製
            autoSetupModelFromAssets(context)
        } else {
            Log.i(TAG, "Release 軌道：檢查本地端是否已有模型...")
        }

        if (internalFile.exists()) internalFile.absolutePath else null
    }

    // 🚀 修改：自動化初始化流程
    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.Default) {
        // 透過分配器取得最終正確的路徑
        val modelPath = getReadyModelPath(context)

        if (modelPath == null) {
            Log.e(TAG, "模型尚未準備就緒 (找不到檔案)")
            return@withContext false
        }

        // 呼叫 JNI 載入
        return@withContext NativeAIInference.loadModel(modelPath)
    }

    suspend fun importModelFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_NAME)
            // 透過 ContentResolver 讀取外部檔案的串流
            context.contentResolver.openInputStream(uri)?.use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "模型複製成功: ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "模型匯入失敗", e)
            false
        }
    }


    fun generateRecipe(foodId: String, foodName: String): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        // 1. 強化 Prompt：明確要求時間與克數，同時簡化格式以獲得更好的遵循度，並相容多樣食材
        val prompt = """
<|im_start|>system
你是一位專業主廚。請結合以下食材設計一道食譜。
食材：$foodName
請嚴格輸出符合以下結構的 JSON 格式，絕對不要有任何其他解釋或 markdown 標記：
{
  "type": "RecipeEntry",
  "name": "料理名稱",
  "timeRequired": 15,
  "description": "一份簡單美味的家常菜",
  "ingredients": [
    { "name": "食材名稱", "amount": 100 }
  ],
  "steps": [
    "步驟一描述",
    "步驟二描述"
  ]
}
(注意：amount 只能是純數字克數，絕對禁止使用片、茶匙、適量或少許。)<|im_end|>
<|im_start|>user
請用 $foodName 幫我寫一份食譜。<|im_end|>
<|im_start|>assistant
""".trimIndent()

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultBuilder = StringBuilder()
                // 設定 2048 tokens 確保食譜不會寫到一半斷掉
                NativeAIInference.generateResponseWithCallback(prompt, 2048, object : NativeInferenceCallback {
                    override fun onTokenGenerated(token: String) {
                        resultBuilder.append(token)
                    }
                })

                val raw = resultBuilder.toString()
                val cleaned = raw
                    .replace(Regex("<\\|[^|>]*\\|?>?"), "")
                    .replace(Regex("<\\|[^>]*$"), "")
                    .replace(Regex("(?m)^\\s*(user|assistant|system)\\s*$"), "")
                    .trim()

                val jsonStartIndex = cleaned.indexOf('{')
                val jsonEndIndex = cleaned.lastIndexOf('}')
                val finalJson = if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex >= jsonStartIndex) {
                    cleaned.substring(jsonStartIndex, jsonEndIndex + 1)
                } else {
                    cleaned
                }

                trySend(finalJson)

            } catch (e: Exception) {
                trySend("\n(生成失敗：${e.message})")
            } finally {
                close()
            }
        }
        awaitClose { }
    }

    // 🚀 Stage 1: 生成多組配方候選清單
    fun draftMultipleMenus(foodName: String): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        val prompt = """
            <|im_start|>system
            你是一個食譜策劃員。請為「$foodName」設計 3 種不同食材組合的配方。
            請嚴格按照以下格式輸出，每行一個配方，食材之間用逗號分隔，不要有任何其他解釋：
            配方1: 食材A, 食材B, 食材C
            配方2: 食材A, 食材D, 食材E
            配方3: 食材A, 食材F<|im_end|>
            <|im_start|>user
            請給出 3 種「$foodName」的配方組合。<|im_end|>
            <|im_start|>assistant
            """.trimIndent()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val resultBuilder = StringBuilder()
                NativeAIInference.generateResponseWithCallback(prompt, 256, object : NativeInferenceCallback {
                    override fun onTokenGenerated(token: String) {
                        resultBuilder.append(token)
                    }
                })
                val raw = resultBuilder.toString().replace(Regex("<\\|[^|>]*\\|?>?"), "").trim()
                trySend(raw)
            } catch (e: Exception) {
                trySend("")
            } finally {
                close()
            }
        }
        awaitClose { }
    }

    // Stage 3 保持不變 (generateStrictRecipe)

    fun generateStrictRecipe(foodName: String, allowedIngredients: String): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        val prompt = """
            <|im_start|>system
            你是一位專業主廚。請嚴格使用以下【限定食材】製作「$foodName」的食譜。
            限定食材：$allowedIngredients
            
            【強制規範】
            1. 絕對不可發明或使用限定清單以外的食材！
            2. 烹飪步驟 (steps) 必須至少包含 4 個步驟，並詳細說明前置處理與火候。
            3. 你必須為每個食材指定「角色」，將角色寫在食材名稱的最前面，用冒號分隔 (例如 "MAIN_PROTEIN:牛肉")。
               可用的角色標籤：
               - MAIN_CARB (主食碳水：米、麵、地瓜)
               - MAIN_PROTEIN (主菜蛋白：肉、海鮮、豆製品)
               - SIDE_VEG (配菜蔬菜：青菜、菇類、蔥蒜等植物)
               - LIQUID (液體調味：水、高湯、醬油、酒)
               - OIL (油脂：沙拉油、奶油)
               - DRY_SPICE (乾性調味：鹽、糖、粉類)
            
            請嚴格輸出符合以下結構的 JSON 格式，絕對不要有 markdown 標記：
            {
              "type": "RecipeEntry",
              "name": "料理名稱",
              "timeRequired": 15,
              "description": "一份簡單美味的家常菜",
              "ingredients": [
                { "name": "MAIN_PROTEIN:食材名稱", "amount": 100 },
                { "name": "DRY_SPICE:鹽", "amount": 5 }
              ],
              "steps": [
                "步驟一描述"
              ]
            }
            (注意：amount 填寫大略預估的純數字克數即可。)<|im_end|>
            <|im_start|>user
            請幫我用限定食材寫一份高水準的食譜。<|im_end|>
            <|im_start|>assistant
            """.trimIndent()

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultBuilder = StringBuilder()
                NativeAIInference.generateResponseWithCallback(prompt, 2048, object : NativeInferenceCallback {
                    override fun onTokenGenerated(token: String) {
                        resultBuilder.append(token)
                    }
                })

                val raw = resultBuilder.toString()
                val cleaned = raw
                    .replace(Regex("<\\|[^|>]*\\|?>?"), "")
                    .replace(Regex("<\\|[^>]*$"), "")
                    .trim()

                val jsonStartIndex = cleaned.indexOf('{')
                val jsonEndIndex = cleaned.lastIndexOf('}')
                val finalJson = if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex >= jsonStartIndex) {
                    cleaned.substring(jsonStartIndex, jsonEndIndex + 1)
                } else {
                    cleaned
                }
                trySend(finalJson)
            } catch (e: Exception) {
                trySend("\n(生成失敗：${e.message})")
            } finally {
                close()
            }
        }
        awaitClose { }
    }

    fun generateRecipeByFlavor(profile: IntArray): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        val sweet = profile.getOrElse(0) { 0 }
        val sour = profile.getOrElse(1) { 0 }
        val bitter = profile.getOrElse(2) { 0 }
        val spicy = profile.getOrElse(3) { 0 }
        val salty = profile.getOrElse(4) { 0 }
        val umami = profile.getOrElse(5) { 0 }
        val fat = profile.getOrElse(6) { 0 }
        val prompt = """
<|im_start|>system
你是一個具備風味科學背景的 AI 主廚。請根據給定的風味 Profile (0-5 分) 設計一份食譜。
【風味要求】甜:$sweet, 酸:$sour, 苦:$bitter, 辣:$spicy, 鹹:$salty, 鮮:$umami, 油脂:$fat。
請嚴格輸出符合以下結構的 JSON 格式，絕對不要有任何其他解釋或 markdown 標記：
{
  "type": "RecipeEntry",
  "name": "料理名稱",
  "timeRequired": 20,
  "description": "風味介紹：(說明如何體現上述風味要求)",
  "ingredients": [
    { "name": "食材名稱", "amount": 100 }
  ],
  "steps": [
    "步驟一描述",
    "步驟二描述"
  ]
}
(注意：amount 只能是純數字克數。)<|im_end|>
<|im_start|>user
請根據風味 Profile [${profile.joinToString(",")}] 生成一份創意食譜。<|im_end|>
<|im_start|>assistant
""".trimIndent()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultBuilder = StringBuilder()
                // 設定 2048 tokens 確保食譜不會寫到一半斷掉
                NativeAIInference.generateResponseWithCallback(prompt, 2048, object : NativeInferenceCallback {
                    override fun onTokenGenerated(token: String) {
                        resultBuilder.append(token)
                    }
                })

                // 全部生成完成後，用正則表達式過濾所有特殊標記
                // 1. 清除完整的 <|...|> 標記（如 <|im_end|>、<|im_start|>等）
                // 2. 清除結尾殘留的不完整標記（如 <|im_、<|i、<| 等）
                val raw = resultBuilder.toString()
                val cleaned = raw
                    .replace(Regex("<\\|[^|>]*\\|?>?"), "") // 完整或半完整 <|...|> 標記
                    .replace(Regex("<\\|[^>]*$"), "")       // 結尾殘留的 <|xxx（沒有閉合的）
                    .replace(Regex("(?m)^\\s*(user|assistant|system)\\s*$"), "") // 獨立成行的角色標記
                    .trim()

                trySend(cleaned)

            } catch (e: Exception) {
                trySend("\n(生成失敗：${e.message})")
            } finally {
                close()
            }
        }
        awaitClose { }
    }

    fun generateIngredientSubstitutes(recipeName: String, recipeIngredients: String, missing: String): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        val prompt = """
<|im_start|>system
你是一位專業的主廚與營養顧問。使用者想要製作「$recipeName」，但缺少了部分食材：「$missing」。
原食譜所需食材包括：$recipeIngredients
請為缺少的食材提供替代方案，並嚴格輸出符合以下結構的 JSON 格式：
{
  "type": "RecipeEntry",
  "name": "替代菜單名稱 (或原名)",
  "timeRequired": 15,
  "description": "替代分析：(說明為何這樣替代，風味注意事項)",
  "ingredients": [
    { "name": "(全部食材，包含替代後的食材選項)", "amount": 100 }
  ],
  "steps": [
    "更新後的步驟一",
    "更新後的步驟二"
  ]
}
(注意：amount 只能是純數字克數。絕對不要輸出任何多餘的解釋或 markdown 標記。)<|im_end|>
<|im_start|>user
我缺少 $missing，有什麼可以替代的嗎？請提供完整食譜 JSON。<|im_end|>
<|im_start|>assistant
""".trimIndent()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultBuilder = StringBuilder()
                NativeAIInference.generateResponseWithCallback(prompt, 2048, object : NativeInferenceCallback {
                    override fun onTokenGenerated(token: String) {
                        resultBuilder.append(token)
                    }
                })

                val raw = resultBuilder.toString()
                val cleaned = raw
                    .replace(Regex("<\\|[^|>]*\\|?>?"), "")
                    .replace(Regex("<\\|[^>]*$"), "")
                    .replace(Regex("(?m)^\\s*(user|assistant|system)\\s*$"), "")
                    .trim()

                trySend(cleaned)

            } catch (e: Exception) {
                trySend("\n(生成失敗：${e.message})")
            } finally {
                close()
            }
        }
        awaitClose { }
    }

    fun resolveIngredientsBatch(unresolved: Map<String, List<String>>): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        // 將 Map 轉為 JSON 格式字串給模型參考
        val inputStr = unresolved.entries.joinToString(",\n") { "  \"${it.key}\": [${it.value.joinToString(", ") { v -> "\"$v\"" }}]" }
        
        val prompt = """
<|im_start|>system
你是一個專業的資料庫匹配助手。你將獲得一組未知的俗名食材與它們各自從資料庫寬鬆搜出的候選名單。
請判斷各俗名最合適的候選名稱。
如果不在此候選名單內，或者差距太大，請回傳 null。
請嚴格以 JSON 格式輸出：
{
  "俗名A": "候選名稱X",
  "俗名B": null
}
絕對不要輸出任何多餘的解釋或標記。<|im_end|>
<|im_start|>user
請幫我匹配這組食材：
{
$inputStr
}<|im_end|>
<|im_start|>assistant
{
""".trimIndent()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultBuilder = StringBuilder()
                NativeAIInference.generateResponseWithCallback(prompt, 512, object : NativeInferenceCallback {
                    override fun onTokenGenerated(token: String) {
                        resultBuilder.append(token)
                    }
                })

                val raw = resultBuilder.toString()
                val cleaned = raw
                    .replace(Regex("<\\|[^|>]*\\|?>?"), "")
                    .replace(Regex("<\\|[^>]*$"), "")
                    .replace(Regex("(?m)^\\s*(user|assistant|system)\\s*$"), "")
                    .trim()

                val jsonStartIndex = cleaned.indexOf('{')
                val jsonEndIndex = cleaned.lastIndexOf('}')
                val finalJson = if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex >= jsonStartIndex) {
                    cleaned.substring(jsonStartIndex, jsonEndIndex + 1)
                } else {
                    cleaned
                }

                trySend(finalJson)
            } catch (e: Exception) {
                // 發生錯誤時回傳空 JSON 以避免 crash
                trySend("{}")
            } finally {
                close()
            }
        }
        awaitClose { }
    }
}