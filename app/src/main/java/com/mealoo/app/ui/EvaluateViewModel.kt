package com.mealoo.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealoo.app.services.AIInferenceService
import com.mealoo.app.services.NativeAIInference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.mealoo.app.db.toModel
import com.mealoo.app.db.toEntity

class EvaluateViewModel : ViewModel() {
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    private val _aiResult = MutableStateFlow("")
    val aiResult = _aiResult.asStateFlow()

    private val _testInput = MutableStateFlow("")
    val testInput = _testInput.asStateFlow()
    private val _selectedRawFoods = MutableStateFlow<List<com.mealoo.app.model.SelectedFood>>(emptyList())
    val selectedRawFoods = _selectedRawFoods.asStateFlow()

    fun updateTestInput(input: String) {
        _testInput.value = input
    }
    fun updateSelectedRawFoods(foods: List<com.mealoo.app.model.SelectedFood>) {
        _selectedRawFoods.value = foods
    }

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady = _isEngineReady.asStateFlow()

    private val _isEngineLoading = MutableStateFlow(false)
    val isEngineLoading = _isEngineLoading.asStateFlow()

    private val COMMON_VERNACULAR_MAP = mapOf(
        "冰糖" to "糖", "黑糖" to "糖"
    )

    private val COMMON_INGREDIENT_FALLBACK_NUTRIENTS = mapOf(
        "鹽" to mapOf("Energy" to 0.0),
        "糖" to mapOf("Energy" to 387.0, "Carbohydrate, by difference" to 100.0),
        "醬油" to mapOf("Energy" to 53.0),
        "油" to mapOf("Energy" to 884.0, "Total lipid (fat)" to 100.0),
        "蒜" to mapOf("Energy" to 149.0), "薑" to mapOf("Energy" to 80.0),
        "蔥" to mapOf("Energy" to 32.0), "水" to mapOf("Energy" to 0.0)
    )

    // 演算法：計算 AI 俗名與 DB 標籤名稱的「字元交集覆蓋率」
    private fun calculateTokenCoverage(aiInput: String, dbName: String): Double {
        // 1. 將 AI 輸入 (如 "去皮雞胸肉") 拆成單字集合 -> ['去', '皮', '雞', '胸', '肉']
        val aiChars = aiInput.filter { !it.isWhitespace() }.toSet()
        if (aiChars.isEmpty()) return 0.0

        // 2. 將 DB 名稱 (如 "雞-胸肉-去皮-生") 去除符號後拆成單字集合
        val dbChars = dbName.replace("-", "").replace(" ", "").toSet()

        // 3. 計算交集 (命中幾個字)
        val intersectionCount = aiChars.intersect(dbChars).size.toDouble()

        // 4. 計算分數：
        // 覆蓋率：確保 AI 要求的屬性，DB 都有對應到
        val coverageScore = intersectionCount / aiChars.size

        // 懲罰項：DB 名字越長扣越小幅度的分數。
        // 這樣當 AI 說 "雞肉" 時，會優先選 "雞-肉-生" 而不是名字超長的 "雞-胸肉-去皮-熟-冷凍"
        val lengthPenalty = dbChars.size * 0.01

        return coverageScore - lengthPenalty
    }

    // 手動觸發：載入模型到記憶體
    fun loadEngine(context: Context) {
        // 如果已經準備好，或正在載入中，就不要重複執行
        if (_isEngineReady.value || _isEngineLoading.value) return

        _isEngineLoading.value = true
        viewModelScope.launch {
            try {
                val success = AIInferenceService.initialize(context)
                _isEngineReady.value = success
            } catch (e: Exception) {
                Log.e("MealooAI", "AI 引擎初始化失敗", e)
                _isEngineReady.value = false
            } finally {
                _isEngineLoading.value = false
            }
        }
    }

    fun generateFoodRecipe(context: Context, foodId: String) {
        if (foodId.isBlank()) return

        _aiLoading.value = true
        _aiResult.value = ""

        viewModelScope.launch {
            try {
                val foodRepo = com.mealoo.app.db.FoodDbRepository.getInstance(context)

                // 🚀 Stage 0: 錨點解析 (Anchor Resolution) - 個別解析主食材並保留營養數據
                val anchorFoods = mutableListOf<com.mealoo.app.model.SelectedFood>()
                val anchorNames = mutableListOf<String>()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    foodRepo.ensureSeeded(context)
                    foodId.split(",").forEach { id ->
                        val fdcId = id.toIntOrNull()
                        if (fdcId != null) {
                            val food = foodRepo.getFoodById(fdcId)
                            if (food != null) {
                                val cleanName = com.mealoo.app.util.stripBrackets(food.nameZh)
                                anchorNames.add(cleanName)
                                // 預先打包實體數據，確保後續寫入時熱量精準
                                anchorFoods.add(com.mealoo.app.model.SelectedFood(
                                    fdcId = fdcId,
                                    source = "DATA",
                                    name = cleanName,
                                    grams = 0.0,
                                    standardNutrients = food.nutrients.mapValues { it.value.amount ?: 0.0 }
                                ))
                            }
                        }
                        // 此處可擴充 UserFood 或 Recipe 的解析邏輯
                    }
                }

                if (anchorNames.isEmpty()) {
                    _aiResult.value = "無法解析選擇的食材數據。"
                    _aiLoading.value = false
                    return@launch
                }

                val mainFoodDisplay = anchorNames.joinToString("、")

                // 🚀 Stage 1: LLM 給出 3 種配方候選
                _aiResult.value = "【系統】AI 正在設計多組配方候選，請稍候...\n"
                var draftedMenusStr = ""
                AIInferenceService.draftMultipleMenus(mainFoodDisplay).collect { token ->
                    draftedMenusStr += token
                }

                // 🚀 Stage 2: 錨點保護淘汰賽 (Anchor-Protected Rejection Sampling)
                val allDbFoods = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    foodRepo.getAllFoodsForMatching()
                }

                val candidateMenus = draftedMenusStr.lines()
                    .filter { it.contains(":") || it.contains("：") }
                    .map { line ->
                        line.substringAfter(":").substringAfter("：")
                            .split(",", "、", " ", "，")
                            .map { it.trim() }.filter { it.isNotBlank() }
                    }

                var bestMenuToUse: List<String> = emptyList()
                var bestMatchedFoodsToSave: List<com.mealoo.app.model.SelectedFood> = emptyList()
                var maxScore = -1.0

                for (menu in candidateMenus) {
                    // 每一組候選菜單都從「主食材錨點」開始構建
                    val currentValidNames = anchorNames.toMutableList()
                    val currentMatchedFoods = anchorFoods.toMutableList()
                    var accessoryCount = 0

                    for (ingName in menu) {
                        // 如果 AI 提到的食材已經在錨點中，跳過不重複計算
                        if (anchorNames.any { it.contains(ingName) || ingName.contains(it) }) continue

                        val searchName = COMMON_VERNACULAR_MAP[ingName] ?: ingName

                        // 針對 AI 額外發想的食材進行 DB 或 兜底比對
                        val bestMatch = allDbFoods.maxByOrNull { calculateTokenCoverage(searchName, it.nameZh) }
                        val score = bestMatch?.let { calculateTokenCoverage(searchName, it.nameZh) } ?: 0.0
                        val fallback = COMMON_INGREDIENT_FALLBACK_NUTRIENTS.entries.find { searchName.contains(it.key) }

                        if (score >= 0.5 && bestMatch != null) { // 對副食材門檻稍微放寬到 0.5 增加多樣性
                            val safeName = com.mealoo.app.util.stripBrackets(bestMatch.nameZh)
                            currentValidNames.add(safeName)
                            accessoryCount++
                            currentMatchedFoods.add(com.mealoo.app.model.SelectedFood(
                                fdcId = bestMatch.fdcId,
                                source = "DATA",
                                name = safeName,
                                grams = 0.0,
                                standardNutrients = mapOf(
                                    "Energy" to (bestMatch.energyKcal ?: 0.0),
                                    "Protein" to (bestMatch.proteinG ?: 0.0),
                                    "Available carbohydrate" to (bestMatch.availableCarbG ?: ((bestMatch.carbByDiffG ?: 0.0) - (bestMatch.fiberG ?: 0.0))),
                                    "Carbohydrate, by difference" to (bestMatch.carbByDiffG ?: 0.0),
                                    "Fiber, total dietary" to (bestMatch.fiberG ?: 0.0),
                                    "Total lipid (fat)" to (bestMatch.fatG ?: 0.0),
                                    "Sodium, Na" to (bestMatch.sodiumMg ?: 0.0),
                                    "Cholesterol" to (bestMatch.cholesterolMg ?: 0.0)
                                )
                            ))
                        } else if (fallback != null) {
                            currentValidNames.add(ingName)
                            accessoryCount++
                            currentMatchedFoods.add(com.mealoo.app.model.SelectedFood(
                                fdcId = null,
                                source = "FALLBACK_DATA",
                                name = ingName,
                                grams = 0.0,
                                standardNutrients = fallback.value
                            ))
                        }
                    }

                    // 評分邏輯：優先錄取「主食材全在」且「副食材/調味最多」的方案
                    val currentTotalScore = currentMatchedFoods.size.toDouble()
                    if (currentTotalScore > maxScore) {
                        maxScore = currentTotalScore
                        bestMenuToUse = currentValidNames
                        bestMatchedFoodsToSave = currentMatchedFoods
                    }
                }

                // 🌟 關鍵：將此階段確定的 DB 實體數據注入狀態，供 saveGeneratedRecipe 直接使用
                updateSelectedRawFoods(bestMatchedFoodsToSave)

                val allowedIngredientsStr = bestMenuToUse.distinct().joinToString("、")
                Log.i("MealooAI", "最終決定提供給 LLM 的安全名單: $allowedIngredientsStr")

                _aiResult.value = ""
                AIInferenceService.generateStrictRecipe(mainFoodDisplay, allowedIngredientsStr).collect { token ->
                    _aiResult.value += token
                }

            } catch (e: Exception) {
                _aiResult.value = "Error: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun generateFoodRecipeByFlavor(profile: IntArray) {
        _aiLoading.value = true
        _aiResult.value = ""

        viewModelScope.launch {
            try {
                AIInferenceService.generateRecipeByFlavor(profile).collect { token ->
                    _aiResult.value += token
                }
            } catch (e: Exception) {
                _aiResult.value = "Error: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private val _recipes = MutableStateFlow<List<com.mealoo.app.model.RecipeEntry>>(emptyList())
    val recipes = _recipes.asStateFlow()

    fun loadRecipes(context: Context) {
        viewModelScope.launch {
            val repo = com.mealoo.app.data.RecipeRepository(context.applicationContext)
            repo.allRecipes.collect { recipeEntities ->
                _recipes.value = recipeEntities.map { it.toModel() }
            }
        }
    }

    fun generateRecipeSubstitutes(recipeName: String, recipeIngredients: String, missing: String) {
        if (recipeName.isBlank() || missing.isBlank()) return

        _aiLoading.value = true
        _aiResult.value = ""

        viewModelScope.launch {
            try {
                AIInferenceService.generateIngredientSubstitutes(recipeName, recipeIngredients, missing).collect { token ->
                    _aiResult.value += token
                }
            } catch (e: Exception) {
                _aiResult.value = "Error: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun saveGeneratedRecipe(context: Context, tabIndex: Int, selectedRecipe: com.mealoo.app.model.RecipeEntry? = null, useAiAssistant: Boolean = false) {
        val resultText = _aiResult.value
        if (resultText.isBlank()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val repo = com.mealoo.app.data.RecipeRepository(context.applicationContext)
                val foodRepo = com.mealoo.app.db.FoodDbRepository.getInstance(context)

                foodRepo.ensureSeeded(context)

                // 🚀 套用大括號掃描器，萃取所有食譜 JSON
                val parsedJsonObjects = mutableListOf<org.json.JSONObject>()
                var bracketCount = 0
                var startIndex = -1
                for (i in resultText.indices) {
                    if (resultText[i] == '{') {
                        if (bracketCount == 0) startIndex = i
                        bracketCount++
                    } else if (resultText[i] == '}') {
                        bracketCount--
                        if (bracketCount == 0 && startIndex != -1) {
                            try {
                                parsedJsonObjects.add(org.json.JSONObject(resultText.substring(startIndex, i + 1)))
                            } catch (e: Exception) { }
                            startIndex = -1
                        }
                    }
                }

                if (parsedJsonObjects.isEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "找不到有效的食譜格式", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val allDbFoods = foodRepo.getAllFoodsForMatching() // 載入全表準備匹配
                var successCount = 0

                // 迴圈處理每一個掃描到的食譜
                for (recipeJson in parsedJsonObjects) {
                    var parsedName = recipeJson.optString("name", "AI 生成食譜").takeIf { it.isNotBlank() } ?: "AI 生成食譜"
                    val descriptionText = recipeJson.optString("description", "")

                    val parsedSteps = mutableListOf<String>()
                    val rawSteps = recipeJson.optJSONArray("steps")
                    if (rawSteps != null) {
                        for (i in 0 until rawSteps.length()) {
                            val stepStr = rawSteps.optString(i, "")
                            if (stepStr.isNotBlank()) parsedSteps.add(stepStr)
                        }
                    }

                    data class ParsedAiIngredient(val rawLine: String, val name: String, val role: String, var grams: Double)
                    val aiIngredients = mutableListOf<ParsedAiIngredient>()

                    val rawIngs = recipeJson.optJSONArray("ingredients")
                    if (rawIngs != null) {
                        for (i in 0 until rawIngs.length()) {
                            val ingObj = rawIngs.optJSONObject(i) ?: continue
                            val iName = ingObj.optString("name", "")
                            val iAmt = ingObj.optDouble("amount", 0.0)

                            if (iName.isNotBlank()) {
                                var role = "UNKNOWN"
                                var cleanName = iName
                                if (cleanName.contains(":")) {
                                    role = cleanName.substringBefore(":").trim().uppercase()
                                    cleanName = cleanName.substringAfter(":").trim()
                                } else if (cleanName.contains("：")) {
                                    role = cleanName.substringBefore("：").trim().uppercase()
                                    cleanName = cleanName.substringAfter("：").trim()
                                }
                                aiIngredients.add(ParsedAiIngredient(iName, cleanName, role, iAmt))
                            }
                        }
                    }

                    // ==========================================================
                    // 🚀 步驟 1：廚藝數學分配引擎 (重新計算克數)
                    // ==========================================================
                    val TARGET_TOTAL_GRAMS = 400.0
                    val ROLE_WEIGHTS = mapOf(
                        "MAIN_CARB" to 40.0, "MAIN_PROTEIN" to 30.0, "SIDE_VEG" to 20.0,
                        "LIQUID" to 6.0, "OIL" to 3.0, "DRY_SPICE" to 1.0
                    )

                    val activeRoles = aiIngredients.map { it.role }.toSet()
                    val activeTotalWeight = activeRoles.sumOf { ROLE_WEIGHTS[it] ?: 0.0 }.takeIf { it > 0.0 } ?: 1.0
                    val roleCounts = aiIngredients.groupingBy { it.role }.eachCount()

                    aiIngredients.forEachIndexed { index, ing ->
                        val roleWeight = ROLE_WEIGHTS[ing.role]
                        if (roleWeight != null) {
                            val allocatedForRole = (roleWeight / activeTotalWeight) * TARGET_TOTAL_GRAMS
                            val finalGrams = allocatedForRole / (roleCounts[ing.role] ?: 1)
                            val roundedGrams = if (ing.role == "DRY_SPICE" || ing.role == "OIL") {
                                Math.round(finalGrams * 10.0) / 10.0 // 調味料保留一位小數
                            } else {
                                Math.round(finalGrams).toDouble()    // 主食取整數
                            }
                            aiIngredients[index] = ing.copy(grams = roundedGrams)
                        } else {
                            val clamped = ing.grams.coerceIn(5.0, 150.0)
                            aiIngredients[index] = ing.copy(grams = clamped)
                        }
                    }

                    // ==========================================================
                    // 🚀 步驟 2：從 FoodDb 提取具體營養數據 (精準映射)
                    // ==========================================================
                    val ingredientsList = mutableListOf<com.mealoo.app.model.RecipeIngredient>()
                    for (aiIng in aiIngredients) {
                        val searchName = COMMON_VERNACULAR_MAP[aiIng.name] ?: aiIng.name
                        val preMatched = _selectedRawFoods.value.find { it.name == aiIng.name || it.name == searchName }

                        if (preMatched != null) {
                            ingredientsList.add(com.mealoo.app.model.RecipeIngredient(
                                name = preMatched.name,
                                matchedFdcId = preMatched.fdcId,
                                source = preMatched.source,
                                amount = aiIng.grams,
                                nutrientsPer100g = preMatched.standardNutrients
                            ))
                            continue
                        }

                        val bestMatch = allDbFoods.maxByOrNull { calculateTokenCoverage(searchName, it.nameZh) }
                        val bestScore = bestMatch?.let { calculateTokenCoverage(searchName, it.nameZh) } ?: 0.0
                        val fallbackNuts = COMMON_INGREDIENT_FALLBACK_NUTRIENTS.entries.find { searchName.contains(it.key) }?.value

                        if (bestScore >= 0.5 && bestMatch != null) {
                            val nutrientMap = mapOf(
                                "Energy" to (bestMatch.energyKcal ?: 0.0),
                                "Protein" to (bestMatch.proteinG ?: 0.0),
                                "Available carbohydrate" to (bestMatch.availableCarbG ?: ((bestMatch.carbByDiffG ?: 0.0) - (bestMatch.fiberG ?: 0.0))),
                                "Carbohydrate, by difference" to (bestMatch.carbByDiffG ?: 0.0),
                                "Fiber, total dietary" to (bestMatch.fiberG ?: 0.0),
                                "Total lipid (fat)" to (bestMatch.fatG ?: 0.0),
                                "Sodium, Na" to (bestMatch.sodiumMg ?: 0.0),
                                "Cholesterol" to (bestMatch.cholesterolMg ?: 0.0)
                            )
                            ingredientsList.add(com.mealoo.app.model.RecipeIngredient(
                                name = aiIng.name,
                                matchedFdcId = bestMatch.fdcId,
                                source = "DATA",
                                amount = aiIng.grams,
                                nutrientsPer100g = nutrientMap
                            ))
                        } else if (fallbackNuts != null) {
                            ingredientsList.add(com.mealoo.app.model.RecipeIngredient(
                                name = aiIng.name,
                                matchedFdcId = null,
                                source = "FALLBACK_DATA",
                                amount = aiIng.grams,
                                nutrientsPer100g = fallbackNuts
                            ))
                        } else {
                            ingredientsList.add(com.mealoo.app.model.RecipeIngredient(
                                name = aiIng.name,
                                matchedFdcId = null,
                                source = "AI_EXTRA",
                                amount = aiIng.grams,
                                nutrientsPer100g = null
                            ))
                        }
                    }

                    // ==========================================================
                    // 🚀 步驟 3：結算總營養素並儲存
                    // ==========================================================
                    var totalKcal = 0.0
                    var totalProtein = 0.0
                    var totalCarbs = 0.0
                    var totalFats = 0.0
                    var totalWeight = 0.0

                    ingredientsList.forEach { ing ->
                        val ratio = ing.amount / 100.0
                        val nuts = ing.nutrientsPer100g ?: emptyMap()
                        totalKcal += (nuts["Energy"] ?: 0.0) * ratio
                        totalProtein += (nuts["Protein"] ?: 0.0) * ratio
                        totalCarbs += (nuts["Carbohydrate, by difference"] ?: nuts["Available carbohydrate"] ?: 0.0) * ratio
                        totalFats += (nuts["Total lipid (fat)"] ?: 0.0) * ratio
                        totalWeight += ing.amount
                    }

                    var finalRecipeName = parsedName
                    if (tabIndex == 1) {
                        finalRecipeName = if (parsedName != "AI 生成食譜") "AI 風味食譜 - $parsedName" else "AI 風味食譜"
                    } else if (tabIndex == 2) {
                        finalRecipeName = "AI 替代方案 - ${selectedRecipe?.name ?: "未知"}"
                    }

                    val recipeEntry = com.mealoo.app.model.RecipeEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        name = finalRecipeName,
                        ingredients = ingredientsList,
                        steps = parsedSteps,
                        description = descriptionText,
                        currency = null,
                        kcalPerServing = totalKcal,
                        protein = totalProtein,
                        carbs = totalCarbs,
                        fats = totalFats,
                        totalWeightGram = totalWeight
                    )

                    repo.addRecipe(recipeEntry.toEntity())
                    successCount++
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (successCount > 0) {
                        android.widget.Toast.makeText(context, "成功寫入 $successCount 筆 Recipe 庫！", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "沒有有效的食譜可寫入", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "寫入失敗: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            NativeAIInference.freeModel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}