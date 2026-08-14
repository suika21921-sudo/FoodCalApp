package com.mealoo.app.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val fdcId: Int,
    val nameEn: String,
    val nameZh: String,
    val nameJa: String,
    val categoryEn: String?,
    val categoryZh: String?,
    val categoryJa: String?,
    val energyKcal: Double?,
    val proteinG: Double?,
    val availableCarbG: Double?,
    val carbByDiffG: Double?,
    val fiberG: Double?,
    val fatG: Double?,
    val sodiumMg: Double?,
    val cholesterolMg: Double?
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val qty: String,
    val unit: String,
    val location: String,
    val expiryDate: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val parentId: String?,
    val fdcId: Int?,
    val standardNutrientsJson: String?,
    val gramsPerUnit: Double?,
    val category: String?,
    val cost: String?,
    val currency: String,
    val sourceType: String?,
    val sourceId: String?,
    val source: String? = null // New field
)

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val shopId: String,
    val name: String,
    val region: String?,
    val address: String?,
    val googlePlaceId: String?,
    val type: String,
    val activeDiscountEventIdsJson: String?,
    val updatedAt: Long
)

@Entity(
    tableName = "shop_products",
    indices = [Index(value = ["shopId"])]
)
data class ShopProductEntity(
    @PrimaryKey val productId: String,
    val shopId: String,
    val name: String,
    val price: String?,
    val currency: String,
    val unit: String?,
    val nutrientsJson: String?,
    val categoryJson: String?,
    val discountItemId: String?,
    val eligibleDiscountEventIdsJson: String?,
    val updatedAt: Long
)

@Entity(
    tableName = "savings_records",
    indices = [Index(value = ["date"])]
)
data class SavingsRecordEntity(
    @PrimaryKey val id: String,
    val date: String,
    val currency: String,
    val original: String,
    val afterDiscount: String,
    val savings: String,
    val savingsRate: String,
    val itemCount: Int,
    val shopId: String?,
    val eventId: String?,
    val note: String?,
    val createdAt: Long
)

@Entity(
    tableName = "shopping_replacements",
    indices = [Index(value = ["storageName"])]
)
data class ShoppingReplacementEntity(
    @PrimaryKey val id: String,
    val storageName: String,
    val grams: Double,
    val shopProductId: String?,
    val shopId: String?
)

@Entity(
    tableName = "meal_plan_entries",
    indices = [Index(value = ["date"]), Index(value = ["mealId"])]
)
data class MealPlanEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val mealId: String,
    val name: String,
    val source: String?,
    val grams: Double,
    val servings: Double,
    val amount: Double?,
    val amountUnit: String?,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val cholesterol: Double,
    val fdcId: Int?,
    val recipeId: String?,
    val inventoryId: String?,
    val cost: String?,
    val currency: String?,
    val sourcePlanId: String?,
    val isCompleted: Boolean,
    val updatedAt: Long
)

@Entity(
    tableName = "intake_entries",
    indices = [Index(value = ["date"])]
)
data class IntakeEntryEntity(
    @PrimaryKey val id: String,
    val type: String, // "EAT" or "DRINK"
    val date: String,
    val name: String,
    val kcal: Double,
    val grams: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fats: Double?,
    val cholesterol: Double?,
    val fdcId: Int?,
    val source: String?,
    val sourcePlanId: String?,
    val mealId: String?,
    val cost: String?,
    val currency: String?,
    val category: String?,
    val sugarGrams: Double?,
    val caffeine: Double?,
    val waterML: Double?,
    val alcoholVolumeML: Double?,
    val alcoholPercent: Float?,
    val timestamp: Long?
)

@Entity(
    tableName = "waste_records",
    indices = [Index(value = ["recordedAt"])]
)
data class WasteRecordEntity(
    @PrimaryKey val id: String,
    val originalItemId: String,
    val name: String,
    val quantity: String,   // Stored as String for precision (BigDecimal)
    val unit: String,
    val costLoss: String,   // Stored as String for precision (BigDecimal)
    val currency: String,
    val reason: String,     // "EXPIRED", "SPOILED", "TASTE_BAD", "OTHER"
    val recordedAt: Long
)
@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val dayCount: Int = 7,
    val createdAt: Long,
    val updatedAt: Long
)
data class PlanWithEntries(
    @androidx.room.Embedded val plan: PlanEntity,

    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "sourcePlanId"
    )
    val entries: List<MealPlanEntryEntity>
)