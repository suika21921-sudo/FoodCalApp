package com.mealoo.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        FoodEntity::class,
        InventoryItemEntity::class,
        ShopEntity::class,
        ShopProductEntity::class,
        SavingsRecordEntity::class,
        ShoppingReplacementEntity::class,
        MealPlanEntryEntity::class,
        IntakeEntryEntity::class,
        WasteRecordEntity::class,
        RecipeEntity::class,
        PlanEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun shopDao(): ShopDao
    abstract fun savingsDao(): SavingsDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun intakeDao(): IntakeDao
    abstract fun wasteDao(): WasteDao
    abstract fun recipeDao(): RecipeDao
    abstract fun planDao(): PlanDao

    companion object {
        private const val DB_NAME = "mealoo.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = com.mealoo.app.util.SecurityHelper.getDatabasePassphrase(context.applicationContext)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .openHelperFactory(factory)
                    // TODO: replace with proper migrations before release.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        fun forceCheckpoint() {
            INSTANCE?.let { db ->
                if (db.isOpen) {
                    try {
                        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun safeClose() {
            INSTANCE?.let { db ->
                if (db.isOpen) {
                    db.close()
                }
            }
            INSTANCE = null
        }
    }
}
