package com.mealoo.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods WHERE nameZh IS NOT NULL")
    suspend fun getAllFoodsForMatching(): List<FoodEntity>
    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Query("SELECT * FROM foods WHERE fdcId = :id LIMIT 1")
    suspend fun getById(id: Int): FoodEntity?

    @Query("SELECT COUNT(*) FROM foods WHERE energyKcal IS NOT NULL")
    suspend fun countWithEnergy(): Int

    @Query("SELECT COUNT(*) FROM foods WHERE nameZh IS NOT NULL AND nameZh != ''")
    suspend fun countWithZhName(): Int

    @Query(
        """
        SELECT * FROM foods
        WHERE nameZh LIKE :q OR nameJa LIKE :q OR nameEn LIKE :q
        ORDER BY nameZh
        LIMIT :limit
        """
    )
    suspend fun search(q: String, limit: Int = 50): List<FoodEntity>

    @Query(
        """
        UPDATE foods SET
          energyKcal = :energyKcal,
          proteinG = :proteinG,
          availableCarbG = :availableCarbG,
          carbByDiffG = :carbByDiffG,
          fiberG = :fiberG,
          fatG = :fatG,
          sodiumMg = :sodiumMg,
          cholesterolMg = :cholesterolMg
        WHERE fdcId = :fdcId
        """
    )
    suspend fun updateNutrients(
        fdcId: Int,
        energyKcal: Double?,
        proteinG: Double?,
        availableCarbG: Double?,
        carbByDiffG: Double?,
        fiberG: Double?,
        fatG: Double?,
        sodiumMg: Double?,
        cholesterolMg: Double?
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<FoodEntity>)
}

@Dao
interface InventoryDao {
    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun count(): Int

    @Query("SELECT * FROM inventory_items")
    suspend fun getAll(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items")
    fun getAllFlow(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<InventoryItemEntity>)

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @androidx.room.Delete
    suspend fun deleteItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET location = :newLocation, note = :newNote WHERE id = :itemId")
    suspend fun updateLocationAndNote(itemId: String, newLocation: String, newNote: String)
}

@Dao
interface WasteDao {
    @Insert
    suspend fun insertWaste(record: WasteRecordEntity)

    // 用於 Dashboard：統計每月的浪費總額
    // Note: SQLite doesn't have BigDecimal sum directly, so we might need to fetch and sum in code OR cast if format allows.
    // However, since we store as String, standard SUM might return float/double which loses precision or 0.
    // Ideally we fetch all records and sum in Kotlin.
    // But for now, let's just fetch records.
    @Query("SELECT * FROM waste_records WHERE recordedAt BETWEEN :startTime AND :endTime AND currency = :targetCurrency")
    suspend fun getRecordsByDateRange(startTime: Long, endTime: Long, targetCurrency: String): List<WasteRecordEntity>

    @Query("SELECT * FROM waste_records ORDER BY recordedAt DESC")
    fun getAllWasteRecordsFlow(): Flow<List<WasteRecordEntity>>
}

@Dao
interface ShopDao {
    @Query("SELECT COUNT(*) FROM shops")
    suspend fun countShops(): Int

    @Query("SELECT COUNT(*) FROM shop_products")
    suspend fun countProducts(): Int

    @Query("SELECT * FROM shops")
    suspend fun getAllShops(): List<ShopEntity>

    @Query("SELECT * FROM shops")
    fun shopsFlow(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shop_products")
    fun productsFlow(): Flow<List<ShopProductEntity>>

    @Query("SELECT * FROM shop_products")
    suspend fun getAllProducts(): List<ShopProductEntity>

    @Query("SELECT * FROM shop_products WHERE shopId = :shopId")
    suspend fun productsByShop(shopId: String): List<ShopProductEntity>

    @Query("SELECT * FROM shop_products WHERE name LIKE :q LIMIT :limit")
    suspend fun searchProducts(q: String, limit: Int = 50): List<ShopProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShops(list: List<ShopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(list: List<ShopProductEntity>)

    @Query("DELETE FROM shop_products WHERE shopId = :shopId")
    suspend fun deleteProductsByShop(shopId: String)

    @Query("DELETE FROM shop_products WHERE shopId = :shopId AND productId = :productId")
    suspend fun deleteProduct(shopId: String, productId: String)

    @Query("DELETE FROM shops WHERE shopId = :shopId")
    suspend fun deleteShopById(shopId: String)
}

@Dao
interface SavingsDao {
    @Query("SELECT COUNT(*) FROM savings_records")
    suspend fun count(): Int

    @Query("SELECT * FROM savings_records")
    fun allFlow(): Flow<List<SavingsRecordEntity>>

    @Query("SELECT * FROM savings_records")
    suspend fun getAll(): List<SavingsRecordEntity>

    @Query("SELECT * FROM savings_records WHERE date >= :from AND date <= :to")
    suspend fun listByDateRange(from: String, to: String): List<SavingsRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SavingsRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SavingsRecordEntity>)

    @Query("DELETE FROM savings_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM savings_records")
    suspend fun clearAll()
}

@Dao
interface ShoppingDao {
    @Query("SELECT COUNT(*) FROM shopping_replacements")
    suspend fun count(): Int

    @Query("SELECT * FROM shopping_replacements")
    suspend fun getReplacements(): List<ShoppingReplacementEntity>

    @Query("SELECT * FROM shopping_replacements")
    fun replacementsFlow(): Flow<List<ShoppingReplacementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReplacements(list: List<ShoppingReplacementEntity>)

    @Query("DELETE FROM shopping_replacements")
    suspend fun clearAll()
}

@Dao
interface MealPlanDao {
    @Query("SELECT COUNT(*) FROM meal_plan_entries")
    suspend fun count(): Int

    @Query("SELECT * FROM meal_plan_entries")
    suspend fun getAll(): List<MealPlanEntryEntity>

    @Query("SELECT * FROM meal_plan_entries WHERE date = :date")
    suspend fun listByDate(date: String): List<MealPlanEntryEntity>

    @Query("SELECT * FROM meal_plan_entries WHERE date = :date")
    fun listByDateFlow(date: String): Flow<List<MealPlanEntryEntity>>

    @Query("SELECT * FROM meal_plan_entries WHERE date = :date AND isCompleted = 0")
    suspend fun pendingByDate(date: String): List<MealPlanEntryEntity>

    @Query("SELECT * FROM meal_plan_entries WHERE date = :date AND mealId = :mealId LIMIT 1")
    suspend fun getByDateAndMealId(date: String, mealId: String): MealPlanEntryEntity?

    @Query("SELECT * FROM meal_plan_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MealPlanEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<MealPlanEntryEntity>)

    @Update
    suspend fun update(entry: MealPlanEntryEntity)

    @Query("DELETE FROM meal_plan_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM meal_plan_entries")
    suspend fun clearAll()
    @Query("DELETE FROM meal_plan_entries WHERE sourcePlanId = :planId")
    suspend fun deleteBySourcePlanId(planId: String)
}

@Dao
interface IntakeDao {
    @Query("SELECT * FROM intake_entries WHERE date = :date")
    suspend fun getByDate(date: String): List<IntakeEntryEntity>

    @Query("SELECT * FROM intake_entries WHERE date = :date")
    fun getByDateFlow(date: String): Flow<List<IntakeEntryEntity>>

    @Query("SELECT * FROM intake_entries WHERE date >= :startDate AND date <= :endDate")
    suspend fun getByDateRange(startDate: String, endDate: String): List<IntakeEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<IntakeEntryEntity>)

    @Query("DELETE FROM intake_entries WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM intake_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) > 0 FROM intake_entries WHERE sourcePlanId = :planId")
    suspend fun existsBySourcePlanId(planId: String): Boolean

    @Query("SELECT DISTINCT date FROM intake_entries")
    suspend fun getAllDates(): List<String>

    @Query("DELETE FROM intake_entries")
    suspend fun clearAll()
}
@Dao
interface PlanDao {
    @androidx.room.Transaction
    @Query("SELECT * FROM plans ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<PlanWithEntries>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: PlanEntity)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deleteById(id: String)
}