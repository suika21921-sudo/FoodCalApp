package com.mealoo.app.db

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object FoodDbSeeder {
    private const val TAG = "FoodDbSeeder"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BaseFood(
        val fdcId: Int,
        val nameEn: String? = null,
        val nameZh: String? = null,
        val nameJa: String? = null,
        val categoryEn: String? = null,
        val categoryZh: String? = null,
        val categoryJa: String? = null
    )

    private data class Nutrients(
        val energyKcal: Double?,
        val proteinG: Double?,
        val availableCarbG: Double?,
        val carbByDiffG: Double?,
        val fiberG: Double?,
        val fatG: Double?,
        val sodiumMg: Double?,
        val cholesterolMg: Double?
    )

    suspend fun ensureSeeded(context: Context, db: AppDatabase) {
        val dao = db.foodDao()
        try {
            if (dao.countWithEnergy() > 0) return
            val baseFoods = withContext(Dispatchers.IO) {
                loadJpFoods(context)
            }
            Log.i(TAG, "Base foods (jp_foods.json): ${baseFoods.size}")
            if (baseFoods.isEmpty()) {
                Log.w(TAG, "No base foods to seed.")
                return
            }
            db.withTransaction {
                dao.upsertAll(baseFoods.map { it.toEntityWithNullNutrients() })
            }
            val nutrientsMap = withContext(Dispatchers.IO) {
                loadNutrientsMap(context)
            }
            Log.i(TAG, "Nutrients entries (data.json): ${nutrientsMap.size}")
            db.withTransaction {
                nutrientsMap.forEach { (fdcId, nutrients) ->
                    dao.updateNutrients(
                        fdcId = fdcId,
                        energyKcal = nutrients.energyKcal,
                        proteinG = nutrients.proteinG,
                        availableCarbG = nutrients.availableCarbG,
                        carbByDiffG = nutrients.carbByDiffG,
                        fiberG = nutrients.fiberG,
                        fatG = nutrients.fatG,
                        sodiumMg = nutrients.sodiumMg,
                        cholesterolMg = nutrients.cholesterolMg
                    )
                }
            }
            Log.i(TAG, "Food DB seeded. count=${dao.count()}")
        } catch (e: Exception) {
            Log.e(TAG, "Food DB seeding failed.", e)
        }
    }

    private fun loadJpFoods(context: Context): List<BaseFood> {
        val baseJson = context.assets.open("jp_foods.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<List<BaseFood>>(baseJson)
    }

    private fun loadNutrientsMap(context: Context): Map<Int, Nutrients> {
        val dataJson = context.assets.open("data.json").bufferedReader().use { it.readText() }
        val dataArray = Json.parseToJsonElement(dataJson).jsonArray
        return buildMap {
            dataArray.forEach { jsonEl ->
                val obj = jsonEl.jsonObject
                val fdcId = obj["foodId"]?.jsonPrimitive?.intOrNull ?: return@forEach
                put(
                    fdcId,
                    Nutrients(
                        energyKcal = obj["enercKcal"]?.jsonPrimitive?.doubleOrNull,
                        proteinG = obj["prot"]?.jsonPrimitive?.doubleOrNull,
                        availableCarbG = obj["choavl"]?.jsonPrimitive?.doubleOrNull,
                        carbByDiffG = obj["chocdf"]?.jsonPrimitive?.doubleOrNull,
                        fiberG = obj["fib"]?.jsonPrimitive?.doubleOrNull,
                        fatG = obj["fat"]?.jsonPrimitive?.doubleOrNull,
                        sodiumMg = obj["na"]?.jsonPrimitive?.doubleOrNull,
                        cholesterolMg = obj["chole"]?.jsonPrimitive?.doubleOrNull
                    )
                )
            }
        }
    }

    private fun BaseFood.toEntityWithNullNutrients(): FoodEntity {
        val fallbackName = listOf(nameZh, nameJa, nameEn)
            .firstOrNull { !it.isNullOrBlank() }
            ?: "Unknown"

        return FoodEntity(
            fdcId = fdcId,
            nameEn = nameEn?.takeIf { it.isNotBlank() } ?: fallbackName,
            nameZh = nameZh?.takeIf { it.isNotBlank() } ?: fallbackName,
            nameJa = nameJa?.takeIf { it.isNotBlank() } ?: fallbackName,
            categoryEn = categoryEn?.takeIf { it.isNotBlank() },
            categoryZh = categoryZh?.takeIf { it.isNotBlank() },
            categoryJa = categoryJa?.takeIf { it.isNotBlank() },
            energyKcal = null,
            proteinG = null,
            availableCarbG = null,
            carbByDiffG = null,
            fiberG = null,
            fatG = null,
            sodiumMg = null,
            cholesterolMg = null
        )
    }
}
