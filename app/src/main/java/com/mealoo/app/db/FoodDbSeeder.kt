package com.mealoo.app.db

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Seeds the `foods` table from assets/foods.json (Japan MEXT + USDA + Taiwan TFDA, merged and
 * de-duplicated by tools/build_foods.py).
 *
 * The table is a pure mirror of that file: on each FOOD_DATA_VERSION bump every row is upserted
 * and rows that are no longer in the file (e.g. removed duplicates) are deleted. Nothing else
 * writes to `foods` — user-defined foods live in UserFoodStorage.
 */
object FoodDbSeeder {
    private const val TAG = "FoodDbSeeder"
    private const val ASSET = "foods.json"

    /** Bump whenever foods.json changes so installed apps re-sync the table. */
    private const val FOOD_DATA_VERSION = 1
    private const val PREFS_NAME = "food_db_seed"
    private const val KEY_VERSION = "food_data_version"
    private const val DELETE_CHUNK = 500

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class FoodRecord(
        val fdcId: Int,
        val nameEn: String? = null,
        val nameZh: String? = null,
        val nameJa: String? = null,
        val categoryEn: String? = null,
        val categoryZh: String? = null,
        val categoryJa: String? = null,
        val nutrients: Map<String, NutrientValue> = emptyMap()
    )

    @Serializable
    private data class NutrientValue(val amount: Double? = null, val unit: String? = null)

    suspend fun ensureSeeded(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) >= FOOD_DATA_VERSION) return
        val dao = db.foodDao()
        try {
            val entities = withContext(Dispatchers.IO) {
                val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
                json.decodeFromString<List<FoodRecord>>(text).map { it.toEntity() }
            }
            if (entities.isEmpty()) {
                Log.w(TAG, "$ASSET is empty, nothing to seed.")
                return
            }
            val keep = entities.mapTo(HashSet()) { it.fdcId }
            db.withTransaction {
                dao.upsertAll(entities)
                dao.getAllIds()
                    .filterNot { it in keep }
                    .chunked(DELETE_CHUNK)
                    .forEach { dao.deleteByIds(it) }
            }
            prefs.edit().putInt(KEY_VERSION, FOOD_DATA_VERSION).apply()
            Log.i(TAG, "Food DB synced to v$FOOD_DATA_VERSION. count=${dao.count()}")
        } catch (e: Exception) {
            Log.e(TAG, "Food DB seeding failed.", e)
        }
    }

    private fun FoodRecord.toEntity(): FoodEntity {
        val fallbackName = listOf(nameZh, nameJa, nameEn)
            .firstOrNull { !it.isNullOrBlank() }
            ?: "Unknown"
        fun n(key: String): Double? = nutrients[key]?.amount
        return FoodEntity(
            fdcId = fdcId,
            nameEn = nameEn?.takeIf { it.isNotBlank() } ?: fallbackName,
            nameZh = nameZh?.takeIf { it.isNotBlank() } ?: fallbackName,
            nameJa = nameJa?.takeIf { it.isNotBlank() } ?: fallbackName,
            categoryEn = categoryEn?.takeIf { it.isNotBlank() },
            categoryZh = categoryZh?.takeIf { it.isNotBlank() },
            categoryJa = categoryJa?.takeIf { it.isNotBlank() },
            energyKcal = n("Energy"),
            proteinG = n("Protein"),
            availableCarbG = n("Available carbohydrate"),
            carbByDiffG = n("Carbohydrate, by difference"),
            fiberG = n("Fiber, total dietary"),
            fatG = n("Total lipid (fat)"),
            sodiumMg = n("Sodium, Na"),
            cholesterolMg = n("Cholesterol")
        )
    }
}
