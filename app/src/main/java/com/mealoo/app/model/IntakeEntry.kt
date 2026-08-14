package com.mealoo.app.model

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import com.mealoo.app.util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
enum class MealKind {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    CUSTOM
}
@Serializable
sealed class IntakeEntry {
    abstract val id: String
    abstract val name: String
    abstract val kcal: Double // 營養素保留 Double 即可，除非你需要極高精度的營養計算
    abstract val date: String
    abstract val category: String?

    // 🔥 改為 BigDecimal
    @Serializable(with = BigDecimalSerializer::class)
    abstract val cost: BigDecimal?

    abstract val currency: String?
    abstract val mealId: String?
    abstract val sourcePlanId: String?
}

@Serializable
@SerialName("eat")
data class EatEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val kcal: Double,
    val grams: Double? = 0.0, // 考慮到向後兼容，暫時保留 Double，計算時轉 BD 即可
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val cholesterol: Double,
    val fdcId: Int? = null,
    val source: String? = null,
    override val category: String?,

    @Serializable(with = BigDecimalSerializer::class)
    override val cost: BigDecimal? = null,

    override val currency: String? = null,
    override val date: String,
    override val mealId: String? = null,
    override val sourcePlanId: String? = null,
) : IntakeEntry()

@Serializable
@SerialName("drink")
data class DrinkEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val kcal: Double,
    val sugarGrams: Double = 0.0,
    val caffeine: Double,
    val waterML: Double,
    val alcoholVolumeML: Double? = null,
    val alcoholPercent: Float? = null,
    override val category: String?,
    override val date: String,
    val timestamp: Long = System.currentTimeMillis(),

    @Serializable(with = BigDecimalSerializer::class)
    override val cost: BigDecimal? = null,

    override val currency: String? = null,
    override val mealId: String? = null,
    override val sourcePlanId: String? = null,
) : IntakeEntry()

@Serializable
data class RecipeEntry(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val kcalPerServing: Double = 0.0,
    val totalWeightGram: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fats: Double = 0.0,
    val tagsV2: Map<String, List<String>> = emptyMap(),
    val timeRequired: Int? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val ingredientsText: List<String> = emptyList(),
    val subRecipe: List<RecipeEntry> = emptyList(),
    val description: String? = null,
    val localImagePath: String? = "",
    val category: List<String> = emptyList(),

    @Serializable(with = BigDecimalSerializer::class)
    val cost: BigDecimal? = null,

    val currency: String? = "TWD"
)

@Serializable
data class RecipeIngredient(
    val name: String = "",
    val amount: Double = 0.0,
    val source: String = "DATA",
    val nutrientsPer100g: Map<String, Double>? = null,
    val matchedFdcId: Int? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val cost: BigDecimal? = null
)
@Serializable
data class UserFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nutrientsPer100g: Map<String, Double>,
    val tags: List<String> = emptyList(),
    val cost : Float ?= null
)
data class UnifiedFoodDisplayItem(
    val id: String,
    val name: String,
    val source: String,
    val category: String?,
    val nutrients: Map<String, Double>
)

object UserFoodStorage {
    private val json = Json { prettyPrint = true }
    const val USER_FOODS_FILE = "user_foods.json"


    suspend fun saveUserFoods(context: Context, foods: List<UserFoodItem>) {
        val file = File(context.filesDir, USER_FOODS_FILE)
        file.writeText(json.encodeToString(foods))
    }

    suspend fun loadUserFoods(context: Context): List<UserFoodItem> {
        val file = File(context.filesDir, USER_FOODS_FILE)
        return if (file.exists()) {
            json.decodeFromString(file.readText())
        } else {
            emptyList()
        }
    }
}
@Serializable
data class DiaryNoteEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val title: String,
    val content: String
)
@Serializable
data class NutrientEntry(
    val amount: Double? = null,
    val unit: String,
    val note: String? = null
)
@Serializable
data class MealPlanEntry(
    val id: String = UUID.randomUUID().toString(),

    // --- 时间与餐次 ---
    val date: String,
    val mealId: String,

    // --- 显示 & 语义 ---
    val name: String,
    val source: String? = null,

    // --- 份量 ---
    val grams: Double = 0.0,
    val servings: Double = 1.0,
    val amount: Double? = null,
    val amountUnit: String? = null,

    // --- 营养（已聚合，非 per100）---
    val kcal: Double,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fats: Double = 0.0,
    val cholesterol: Double = 0.0,

    // --- 追溯用 ---
    val fdcId: Int? = null,
    val recipeId: String? = null,
    val inventoryId: String? = null,

    // --- 成本（可选，未来用）---
    @Serializable(with = BigDecimalSerializer::class)
    val cost: BigDecimal? = null,
    val currency: String? = null,
    val sourcePlanId: String? = null,
    val isCompleted: Boolean = false
)
@Serializable
data class MenuPlanSummary(
    val id: String,
    val name: String,
    val description: String ?= null,
    val dayCount: Int,
    val targetMealId: String? = null,
    val entries: List<MealPlanEntry>
){
    val avgKcal: Double get() = if (dayCount == 0) 0.0 else entries.sumOf { it.kcal } / dayCount
    val totalCost: BigDecimal get() = entries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.cost ?: BigDecimal.ZERO) }
}