package com.mealoo.app.util

import com.mealoo.app.data.MealPlanSummaryDto
import com.mealoo.app.model.DrinkEntry
import com.mealoo.app.model.EatEntry
import com.mealoo.app.model.RecipeEntry
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class AiParseResult {
    // 加入 rawJson，讓呼叫端除了拿到強型別的 data，也可以拿到純淨的 JSON 字串備用
    data class Success(val data: Any, val typeStr: String, val rawJson: String) : AiParseResult()
    data class Failure(val errorMessage: String, val rawJson: String? = null) : AiParseResult()
}

object JsonSanitizer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true // 允許 null 退階為預設值
    }

    fun sanitize(input: String): String? {
        val trimmed = input.trim()
        val firstBrace = trimmed.indexOf('{')
        val firstBracket = trimmed.indexOf('[')

        val startIndex: Int
        val endIndex: Int

        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            startIndex = firstBrace
            endIndex = trimmed.lastIndexOf('}')
        } else if (firstBracket != -1) {
            startIndex = firstBracket
            endIndex = trimmed.lastIndexOf(']')
        } else {
            return null
        }

        if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) return null
        return trimmed.substring(startIndex, endIndex + 1)
    }

    /**
     * 預補正引擎：修正 LLM 常犯的欄位命名錯誤
     */
    private fun preCorrectHeuristics(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element

        val map = element.toMutableMap()

        // 1. 修正常見的熱量命名幻覺
        if (map.containsKey("calories") && !map.containsKey("kcal")) {
            map["kcal"] = map.remove("calories")!!
        }
        // 2. 修正常見的脂肪命名幻覺
        if (map.containsKey("fat") && !map.containsKey("fats")) {
            map["fats"] = map.remove("fat")!!
        }
        // 3. 修正碳水命名
        if (map.containsKey("carbohydrates") && !map.containsKey("carbs")) {
            map["carbs"] = map.remove("carbohydrates")!!
        }

        // 4. 陣列內的遞迴修正 (例如 Recipe 的 ingredients 或 MealPlan 的 entries)
        map.forEach { (key, value) ->
            if (value is JsonArray) {
                val newArray = JsonArray(value.map { preCorrectHeuristics(it) })
                map[key] = newArray
            } else if (value is JsonObject) {
                map[key] = preCorrectHeuristics(value)
            }
        }

        return JsonObject(map)
    }

    fun parseAiResponse(rawInput: String): AiParseResult {
        val cleanJson = sanitize(rawInput)
            ?: return AiParseResult.Failure("No valid JSON object found in input.", rawInput)

        return try {
            val rawElement = json.parseToJsonElement(cleanJson)
            // 執行預補正
            val correctedElement = preCorrectHeuristics(rawElement)

            val jsonObject = try {
                correctedElement.jsonObject
            } catch (e: IllegalArgumentException) {
                return AiParseResult.Failure("Expected a JSON object, but found something else.", cleanJson)
            }

            val type = jsonObject["type"]?.jsonPrimitive?.contentOrNull

            when {
                type.equals("EatEntry", ignoreCase = true) -> {
                    val entry = json.decodeFromJsonElement<EatEntry>(correctedElement)
                    AiParseResult.Success(entry, "EatEntry", correctedElement.toString())
                }
                type.equals("DrinkEntry", ignoreCase = true) -> {
                    val entry = json.decodeFromJsonElement<DrinkEntry>(correctedElement)
                    AiParseResult.Success(entry, "DrinkEntry", correctedElement.toString())
                }
                type.equals("RecipeEntry", ignoreCase = true) || type.equals("Recipe", ignoreCase = true) -> {
                    val entry = json.decodeFromJsonElement<RecipeEntry>(correctedElement)
                    AiParseResult.Success(entry, "RecipeEntry", correctedElement.toString())
                }
                type.equals("MealPlanSummary", ignoreCase = true) -> {
                    val entry = json.decodeFromJsonElement<MealPlanSummaryDto>(correctedElement)
                    AiParseResult.Success(entry, "MealPlanSummary", correctedElement.toString())
                }
                jsonObject.containsKey("ingredients") && jsonObject.containsKey("steps") -> {
                    val entry = json.decodeFromJsonElement<RecipeEntry>(correctedElement)
                    AiParseResult.Success(entry, "RecipeEntry", correctedElement.toString())
                }
                jsonObject.containsKey("protein") && jsonObject.containsKey("kcal") -> {
                    val entry = json.decodeFromJsonElement<EatEntry>(correctedElement)
                    AiParseResult.Success(entry, "EatEntry", correctedElement.toString())
                }
                else -> {
                    AiParseResult.Failure("Unknown data type or missing 'type' field.", cleanJson)
                }
            }
        } catch (e: SerializationException) {
            AiParseResult.Failure("JSON 缺少必填欄位或格式錯誤: ${e.message}", cleanJson)
        } catch (e: Exception) {
            AiParseResult.Failure("Parse Error: ${e.localizedMessage}", cleanJson)
        }
    }
}