package com.mealoo.app.db

import android.content.Context
import androidx.room.withTransaction
import com.mealoo.app.data.Replacement
import com.mealoo.app.data.SavingsRecord
import com.mealoo.app.data.SavingsState
import com.mealoo.app.data.ShopCategory
import com.mealoo.app.data.ShopInfo
import com.mealoo.app.data.ShopProduct
import com.mealoo.app.data.ThawError
import com.mealoo.app.model.FoodItem
import com.mealoo.app.model.FoodSelectionCandidate
import com.mealoo.app.model.InventoryItem
import com.mealoo.app.model.LocalizedNutrientEntry
import com.mealoo.app.model.MealPlanEntry
import com.mealoo.app.model.MenuPlanSummary
import com.mealoo.app.model.NutrientEntry
import com.mealoo.app.util.Result
import com.mealoo.app.util.buildThawNote
import com.mealoo.app.util.hasThawTag
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FoodDbRepository private constructor(private val db: AppDatabase) {
    private val dao = db.foodDao()

    suspend fun ensureSeeded(context: Context) {
        FoodDbSeeder.ensureSeeded(context, db)
    }

    suspend fun getAllFoodsForMatching(): List<FoodEntity> {
        return dao.getAllFoodsForMatching()
    }

    suspend fun searchFoods(query: String, limit: Int = 50): List<FoodSelectionCandidate> {
        val processedQuery = query.replace("-", " ")

        return searchEntities(processedQuery, limit).map { entity ->
            FoodSelectionCandidate(
                id = entity.fdcId.toString(),
                name = localizedName(entity),
                nutrientsPer100g = entity.toNutrientsMap()
            )
        }
    }

    suspend fun searchLocalizedFoods(query: String, limit: Int = 50): List<LocalizedNutrientEntry> {
        return searchEntities(query, limit).map { entity ->
            LocalizedNutrientEntry(
                fdcId = entity.fdcId,
                nameEn = entity.nameEn,
                nameZh = entity.nameZh,
                nameJa = entity.nameJa,
                categoryEn = entity.categoryEn,
                categoryZh = entity.categoryZh,
                categoryJa = entity.categoryJa,
                nutrients = entity.toNutrientsMap()
            )
        }
    }

    suspend fun getFoodById(fdcId: Int): FoodItem? {
        return dao.getById(fdcId)?.toFoodItem()
    }

    private suspend fun searchEntities(query: String, limit: Int): List<FoodEntity> {
        val trimmed = query.trim()
        val q = if (trimmed.isEmpty()) "%" else "%$trimmed%"
        return dao.search(q, limit)
    }

    private fun localizedName(entity: FoodEntity, locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "zh" -> entity.nameZh
            "ja" -> entity.nameJa
            else -> entity.nameEn
        }
    }

    private fun FoodEntity.toNutrientsMap(): Map<String, Double> {
        val carbs = carbByDiffG ?: 0.0
        val fiber = fiberG ?: 0.0
        val availableCarbs = availableCarbG ?: (carbs - fiber)
        return mapOf(
            "Energy" to (energyKcal ?: 0.0),
            "Protein" to (proteinG ?: 0.0),
            "Available carbohydrate" to availableCarbs,
            "Carbohydrate, by difference" to carbs,
            "Fiber, total dietary" to fiber,
            "Total lipid (fat)" to (fatG ?: 0.0),
            "Sodium, Na" to (sodiumMg ?: 0.0),
            "Cholesterol" to (cholesterolMg ?: 0.0)
        )
    }

    private fun FoodEntity.toFoodItem(): FoodItem {
        val nutrients = buildMap<String, NutrientEntry> {
            energyKcal?.let { put("Energy", NutrientEntry(amount = it, unit = "kcal")) }
            proteinG?.let { put("Protein", NutrientEntry(amount = it, unit = "g")) }
            availableCarbG?.let { put("Available carbohydrate", NutrientEntry(amount = it, unit = "g")) }
            carbByDiffG?.let { put("Carbohydrate, by difference", NutrientEntry(amount = it, unit = "g")) }
            fiberG?.let { put("Fiber, total dietary", NutrientEntry(amount = it, unit = "g")) }
            fatG?.let { put("Total lipid (fat)", NutrientEntry(amount = it, unit = "g")) }
            sodiumMg?.let { put("Sodium, Na", NutrientEntry(amount = it, unit = "mg")) }
            cholesterolMg?.let { put("Cholesterol", NutrientEntry(amount = it, unit = "mg")) }
        }

        return FoodItem(
            fdcId = fdcId,
            source = "DATA",
            nameEn = nameEn,
            nameZh = nameZh,
            nameJa = nameJa,
            categoryEn = categoryEn,
            categoryZh = categoryZh,
            categoryJa = categoryJa,
            nutrients = nutrients
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: FoodDbRepository? = null

        fun getInstance(context: Context): FoodDbRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FoodDbRepository(AppDatabase.getInstance(context))
                INSTANCE = instance
                instance
            }
        }
    }
}

private val roomJson = Json { ignoreUnknownKeys = true }

class InventoryRepository private constructor(
    private val db: AppDatabase
) {
    private val dao = db.inventoryDao()
    private val wasteDao = db.wasteDao()

    fun itemsFlow(): Flow<List<InventoryItem>> =
        dao.getAllFlow().map { list -> list.map { it.toModel() } }

    suspend fun getItems(): List<InventoryItem> =
        dao.getAll().map { it.toModel() }

    suspend fun getById(id: String): InventoryItem? =
        dao.getById(id)?.toModel()

    suspend fun upsert(item: InventoryItem) {
        dao.upsertAll(listOf(item.toEntity()))
    }

    suspend fun upsertAll(items: List<InventoryItem>) {
        dao.upsertAll(items.map { it.toEntity() })
    }

    suspend fun update(item: InventoryItem) {
        dao.update(item.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun consume(id: String, amount: Double) {
        val current = dao.getById(id) ?: return
        val amountBd = BigDecimal.valueOf(amount)
        val qty = current.qty.toBigDecimalOrZero()
        val newQty = qty.subtract(amountBd).max(BigDecimal.ZERO)
        val updated = current.copy(
            qty = newQty.toPlainString(),
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
    }

    suspend fun moveItemToFridge(itemId: String, currentNote: String?) {
        val timestamp = java.text.SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(java.util.Date())
        val newNote = if (currentNote.isNullOrBlank()) {
            "Thawed on $timestamp"
        } else {
            "$currentNote\nThawed on $timestamp"
        }
        dao.updateLocationAndNote(itemId, "Fridge", newNote)
    }

    suspend fun recordWasteAndDelete(itemId: String, reason: String) {
        db.withTransaction {
            val itemEntity = dao.getById(itemId) ?: return@withTransaction
            
            val loss = itemEntity.cost ?: "0"

            val wasteRecord = WasteRecordEntity(
                id = UUID.randomUUID().toString(),
                originalItemId = itemEntity.id,
                name = itemEntity.name,
                quantity = itemEntity.qty,
                unit = itemEntity.unit,
                costLoss = loss,
                currency = itemEntity.currency,
                reason = reason,
                recordedAt = System.currentTimeMillis()
            )

            wasteDao.insertWaste(wasteRecord)
            dao.deleteItem(itemEntity)
        }
    }

    suspend fun getMonthlyWasteLoss(year: Int, month: Int, currency: String = "NT$"): BigDecimal {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val start = calendar.timeInMillis
        
        calendar.add(java.util.Calendar.MONTH, 1)
        val end = calendar.timeInMillis
        
        val records = wasteDao.getRecordsByDateRange(start, end, currency)
        return records.fold(BigDecimal.ZERO) { acc, record ->
            acc.add(record.costLoss.toBigDecimalOrZero())
        }
    }

    suspend fun thawInventoryItem(
        parentId: String,
        needQty: Double,
        targetLocation: String = "Fridge",
        extraNote: String? = null
    ): Result<InventoryItem, ThawError> {
        val needQtyBD = BigDecimal.valueOf(needQty)
        if (needQtyBD <= BigDecimal.ZERO) {
            return Result.Err(ThawError.InvalidQuantity(needQty))
        }

        return db.withTransaction {
            val parent = dao.getById(parentId) ?: return@withTransaction Result.Err(
                ThawError.NotFound(parentId)
            )
            val parentQty = parent.qty.toBigDecimalOrZero()
            if (parentQty < needQtyBD) {
                return@withTransaction Result.Err(
                    ThawError.InsufficientQuantity(parentQty, needQtyBD)
                )
            }

            if (parentQty.compareTo(needQtyBD) == 0) {
                val mergedNote = mergeThawNote(parent.note, parent.name, extraNote)
                val updated = parent.copy(
                    location = targetLocation,
                    note = mergedNote,
                    parentId = null,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updated)
                return@withTransaction Result.Ok(updated.toModel())
            }

            val remainingQty = parentQty.subtract(needQtyBD).max(BigDecimal.ZERO)
            val parentCost = parent.cost.toBigDecimalOrNull()
            val childCost = parentCost?.takeIf { parentQty > BigDecimal.ZERO }?.let { cost ->
                cost.multiply(needQtyBD).divide(parentQty, 2, RoundingMode.HALF_UP)
            }
            val remainingCost = parentCost?.let { cost ->
                cost.subtract(childCost ?: BigDecimal.ZERO).max(BigDecimal.ZERO)
            }

            val updatedParent = parent.copy(
                qty = remainingQty.toPlainString(),
                cost = remainingCost?.toPlainString(),
                updatedAt = System.currentTimeMillis()
            )
            val child = parent.copy(
                id = UUID.randomUUID().toString(),
                qty = needQtyBD.toPlainString(),
                location = targetLocation,
                note = buildThawNote(parent.name, extraNote),
                parentId = parent.id,
                cost = childCost?.toPlainString(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            dao.update(updatedParent)
            dao.upsertAll(listOf(child))
            Result.Ok(child.toModel())
        }
    }

    private fun mergeThawNote(existing: String?, fromName: String, extraNote: String?): String {
        val trimmed = existing?.trim()?.takeIf { it.isNotBlank() }
        if (trimmed == null) {
            return buildThawNote(fromName, extraNote)
        }
        if (trimmed.hasThawTag()) {
            val extra = extraNote?.trim()?.takeIf { it.isNotBlank() }
            return if (extra != null) "$trimmed - $extra" else trimmed
        }
        val thawNote = buildThawNote(fromName, extraNote)
        return "$thawNote - $trimmed"
    }

    private fun InventoryItemEntity.toModel(): InventoryItem {
        val nutrients = standardNutrientsJson?.let { raw ->
            runCatching { roomJson.decodeFromString<Map<String, Double>>(raw) }.getOrNull()
        }
        return InventoryItem(
            id = id,
            name = name,
            quantity = qty.toBigDecimalOrZero(),
            unit = unit,
            location = location,
            expiryDate = expiryDate,
            note = note,
            createdAt = createdAt,
            fdcId = fdcId,
            standardNutrientsPer100g = nutrients,
            gramsPerUnit = gramsPerUnit,
            category = category,
            cost = cost.toBigDecimalOrNull(),
            currency = currency,
            sourceType = sourceType,
            sourceId = sourceId,
            parentId = parentId,
            source = source // Map source
        )
    }

    private fun InventoryItem.toEntity(): InventoryItemEntity {
        val nutrientsJson = standardNutrientsPer100g?.let { roomJson.encodeToString(it) }
        return InventoryItemEntity(
            id = id,
            name = name,
            qty = quantity.toPlainString(),
            unit = unit,
            location = location,
            expiryDate = expiryDate,
            note = note,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
            parentId = parentId,
            fdcId = fdcId,
            standardNutrientsJson = nutrientsJson,
            gramsPerUnit = gramsPerUnit,
            category = category,
            cost = cost?.toPlainString(),
            currency = currency,
            sourceType = sourceType,
            sourceId = sourceId,
            source = source // Map source
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: InventoryRepository? = null

        fun getInstance(context: Context): InventoryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = InventoryRepository(AppDatabase.getInstance(context))
                INSTANCE = instance
                instance
            }
        }
    }
}

class ShopRepository private constructor(
    private val db: AppDatabase
) {
    private val dao = db.shopDao()

    fun shopsFlow(): Flow<List<ShopInfo>> =
        combine(dao.shopsFlow(), dao.productsFlow()) { shops, products ->
            val byShop = products.groupBy { it.shopId }
            shops.map { shop ->
                shop.toModel(byShop[shop.shopId].orEmpty())
            }
        }

    suspend fun getAll(): List<ShopInfo> {
        val shops = dao.getAllShops()
        val products = dao.getAllProducts()
        val byShop = products.groupBy { it.shopId }
        return shops.map { it.toModel(byShop[it.shopId].orEmpty()) }
    }

    suspend fun upsertShop(shop: ShopInfo) {
        val entity = shop.toEntity()
        val products = shop.catalog.map { it.toEntity(shop.id) }
        db.withTransaction {
            dao.upsertShops(listOf(entity))
            if (products.isNotEmpty()) {
                dao.upsertProducts(products)
            }
        }
    }

    suspend fun addProductToShop(shopId: String, product: ShopProduct) {
        dao.upsertProducts(listOf(product.toEntity(shopId)))
    }

    suspend fun deleteProductFromShop(shopId: String, productId: String) {
        dao.deleteProduct(shopId, productId)
    }

    suspend fun deleteShop(shopId: String) {
        db.withTransaction {
            dao.deleteProductsByShop(shopId)
            dao.deleteShopById(shopId)
        }
    }

    suspend fun productsByShop(shopId: String): List<ShopProduct> =
        dao.productsByShop(shopId).map { it.toModel() }

    suspend fun searchProducts(query: String, limit: Int = 50): List<ShopProduct> {
        val trimmed = query.trim()
        val q = if (trimmed.isEmpty()) "%" else "%$trimmed%"
        return dao.searchProducts(q, limit).map { it.toModel() }
    }

    private fun ShopEntity.toModel(products: List<ShopProductEntity>): ShopInfo {
        val activeIds = activeDiscountEventIdsJson?.let { raw ->
            runCatching { roomJson.decodeFromString<List<String>>(raw) }.getOrNull()
        } ?: emptyList()
        return ShopInfo(
            id = shopId,
            name = name,
            address = address,
            googlePlaceId = googlePlaceId,
            type = parseShopCategory(type),
            activeDiscountEventIds = activeIds,
            catalog = products.map { it.toModel() }
        )
    }

    private fun ShopInfo.toEntity(): ShopEntity {
        val activeIdsJson = roomJson.encodeToString(activeDiscountEventIds)
        return ShopEntity(
            shopId = id,
            name = name,
            region = null,
            address = address,
            googlePlaceId = googlePlaceId,
            type = type.name,
            activeDiscountEventIdsJson = activeIdsJson,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun ShopProductEntity.toModel(): ShopProduct {
        val nutrients = nutrientsJson?.let { raw ->
            runCatching { roomJson.decodeFromString<Map<String, Double>>(raw) }.getOrNull()
        }
        val category = categoryJson?.let { raw ->
            runCatching { roomJson.decodeFromString<List<String>>(raw) }.getOrNull()
        }
        val eligibleIds = eligibleDiscountEventIdsJson?.let { raw ->
            runCatching { roomJson.decodeFromString<List<String>>(raw) }.getOrNull()
        } ?: emptyList()
        return ShopProduct(
            id = productId,
            shopId = shopId,
            name = name,
            price = price.toBigDecimalOrNull(),
            currency = currency,
            nutrients = nutrients,
            category = category,
            discountItemId = discountItemId,
            eligibleDiscountEventIds = eligibleIds
        )
    }

    private fun ShopProduct.toEntity(forShopId: String): ShopProductEntity {
        val nutrientsJson = nutrients?.let { roomJson.encodeToString(it) }
        val categoryJson = category?.let { roomJson.encodeToString(it) }
        val eligibleIdsJson = roomJson.encodeToString(eligibleDiscountEventIds)
        return ShopProductEntity(
            productId = id,
            shopId = forShopId,
            name = name,
            price = price?.toPlainString(),
            currency = currency,
            unit = null,
            nutrientsJson = nutrientsJson,
            categoryJson = categoryJson,
            discountItemId = discountItemId,
            eligibleDiscountEventIdsJson = eligibleIdsJson,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun parseShopCategory(raw: String): ShopCategory {
        return runCatching { ShopCategory.valueOf(raw) }.getOrElse { ShopCategory.RETAIL }
    }

    companion object {
        @Volatile
        private var INSTANCE: ShopRepository? = null

        fun getInstance(context: Context): ShopRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ShopRepository(AppDatabase.getInstance(context))
                INSTANCE = instance
                instance
            }
        }
    }
}

class SavingsRepository private constructor(
    private val db: AppDatabase
) {
    private val dao = db.savingsDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val savingsState: StateFlow<SavingsState> =
        dao.allFlow()
            .map { list -> SavingsState(records = list.map { it.toModel() }) }
            .stateIn(scope, SharingStarted.Eagerly, SavingsState())

    suspend fun addRecord(record: SavingsRecord) {
        dao.insert(record.toEntity())
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun deleteRecord(id: String) {
        dao.deleteById(id)
    }

    suspend fun listByDateRange(from: String, to: String): List<SavingsRecord> =
        dao.listByDateRange(from, to).map { it.toModel() }

    suspend fun sumByCurrency(from: String, to: String): Map<String, BigDecimal> {
        return listByDateRange(from, to)
            .groupBy { it.currency }
            .mapValues { (_, records) ->
                records.fold(BigDecimal.ZERO) { acc, record -> acc + record.savings }
            }
    }

    private fun SavingsRecordEntity.toModel(): SavingsRecord {
        return SavingsRecord(
            id = id,
            date = date,
            shopId = shopId,
            eventId = eventId,
            currency = currency,
            original = original.toBigDecimalOrZero(),
            afterDiscount = afterDiscount.toBigDecimalOrZero(),
            savings = savings.toBigDecimalOrZero(),
            savingsRate = savingsRate.toBigDecimalOrZero(),
            itemCount = itemCount,
            note = note,
            computedAt = createdAt
        )
    }

    private fun SavingsRecord.toEntity(): SavingsRecordEntity {
        return SavingsRecordEntity(
            id = id,
            date = date,
            currency = currency,
            original = original.toPlainString(),
            afterDiscount = afterDiscount.toPlainString(),
            savings = savings.toPlainString(),
            savingsRate = savingsRate.toPlainString(),
            itemCount = itemCount,
            shopId = shopId,
            eventId = eventId,
            note = note,
            createdAt = computedAt
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SavingsRepository? = null

        fun getInstance(context: Context): SavingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SavingsRepository(AppDatabase.getInstance(context))
                INSTANCE = instance
                instance
            }
        }
    }
}

class ShoppingRepository private constructor(
    private val db: AppDatabase
) {
    private val dao = db.shoppingDao()

    suspend fun upsertReplacements(list: List<Replacement>) {
        val entities = list.map { it.toEntity(it.storageName) }
        dao.upsertReplacements(entities)
    }

    suspend fun saveReplacements(map: Map<String, Replacement>) {
        val entities = map.mapNotNull { (key, repl) ->
            val storageName = repl.storageName.ifBlank { key }
            if (storageName.isBlank()) return@mapNotNull null
            repl.toEntity(key, storageName)
        }
        dao.upsertReplacements(entities)
    }

    suspend fun getReplacements(): Map<String, Replacement> {
        return dao.getReplacements().associate { entity ->
            entity.id to entity.toModel()
        }
    }

    fun replacementsFlow(): Flow<Map<String, Replacement>> =
        dao.replacementsFlow().map { list ->
            list.associate { it.id to it.toModel() }
        }

    private fun Replacement.toEntity(id: String, storageNameOverride: String = storageName): ShoppingReplacementEntity {
        return ShoppingReplacementEntity(
            id = id,
            storageName = storageNameOverride,
            grams = grams,
            shopProductId = shopProductId,
            shopId = shopId
        )
    }

    private fun ShoppingReplacementEntity.toModel(): Replacement {
        return Replacement(
            storageName = storageName,
            grams = grams,
            shopProductId = shopProductId,
            shopId = shopId
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ShoppingRepository? = null

        fun getInstance(context: Context): ShoppingRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ShoppingRepository(AppDatabase.getInstance(context))
                INSTANCE = instance
                instance
            }
        }
    }
}

class MealPlanRepository private constructor(
    context: Context,
    private val db: AppDatabase
) {
    private val dao = db.mealPlanDao()
    private val planDao = db.planDao()
    private val appContext = context.applicationContext

    suspend fun getEntries(date: String): List<MealPlanEntry> =
        dao.listByDate(date).map { it.toModel() }
    fun getAllPlansFlow(): Flow<List<MenuPlanSummary>> {
        return planDao.getAllFlow().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun savePlan(summary: MenuPlanSummary) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            // 1. 儲存方案資訊
            val planEntity = PlanEntity(
                id = summary.id,
                name = summary.name,
                description = summary.description,
                dayCount = summary.dayCount,
                createdAt = now,
                updatedAt = now
            )
            planDao.insert(planEntity)

            // 2. 清除該方案舊的餐點 (Overwrite 邏輯)
            dao.deleteBySourcePlanId(summary.id)

            // 3. 寫入新的餐點
            if (summary.entries.isNotEmpty()) {
                val entities = summary.entries.map { entry ->
                    entry.copy(sourcePlanId = summary.id)
                        .toEntity(updatedAt = now)
                }
                dao.upsertAll(entities)
            }
        }
    }

    suspend fun deletePlan(planId: String) {
        db.withTransaction {
            dao.deleteBySourcePlanId(planId)
            planDao.deleteById(planId)
        }
    }

    fun entriesFlow(date: String): Flow<List<MealPlanEntry>> =
        dao.listByDateFlow(date).map { list -> list.map { it.toModel() } }

    suspend fun getById(planId: String): MealPlanEntry? =
        dao.getById(planId)?.toModel()

    suspend fun markCompleted(planId: String) {
        val entry = dao.getById(planId) ?: return
        dao.update(entry.copy(isCompleted = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun getTodayFirstPendingPlan(): MealPlanEntry? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val entries = dao.listByDate(today).map { it.toModel() }
        val pending = entries.filter { !it.isCompleted }
        if (pending.isEmpty()) return null

        val mealOrder = com.mealoo.app.data.MealDefsStore(appContext).getAll().map { it.id }
        return pending
            .sortedWith(compareBy { entry ->
                val idx = mealOrder.indexOf(entry.mealId)
                if (idx == -1) Int.MAX_VALUE else idx
            })
            .firstOrNull()
    }

    suspend fun getTodayPlanByMealId(mealId: String): MealPlanEntry? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return dao.getByDateAndMealId(today, mealId)?.toModel()
    }

    suspend fun getTodayPendingPlanByMealId(mealId: String): MealPlanEntry? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return dao.listByDate(today)
            .map { it.toModel() }
            .firstOrNull { it.mealId == mealId && !it.isCompleted }
    }

    suspend fun addEntry(date: String, entry: MealPlanEntry) {
        addEntries(date, listOf(entry))
    }

    suspend fun addEntries(date: String, entries: List<MealPlanEntry>) {
        if (entries.isEmpty()) return
        dao.upsertAll(entries.map { it.copy(date = date).toEntity() })
    }

    suspend fun addEntriesToDates(dates: List<String>, templateEntries: List<MealPlanEntry>) {
        if (dates.isEmpty() || templateEntries.isEmpty()) return
        val now = System.currentTimeMillis()
        val all = dates.flatMap { date ->
            templateEntries.map { template ->
                template.copy(
                    id = UUID.randomUUID().toString(),
                    date = date
                ).toEntity(now)
            }
        }
        dao.upsertAll(all)
    }

    suspend fun upsertEntry(date: String, entry: MealPlanEntry) {
        dao.upsertAll(listOf(entry.copy(date = date).toEntity()))
    }

    suspend fun removeEntry(date: String, entryId: String) {
        dao.deleteById(entryId)
    }

    suspend fun replaceEntries(date: String, entries: List<MealPlanEntry>) {
        db.withTransaction {
            val existing = dao.listByDate(date)
            existing.forEach { dao.deleteById(it.id) }
            dao.upsertAll(entries.map { it.copy(date = date).toEntity() })
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun getEntriesForDateRange(dates: List<String>): Map<String, List<MealPlanEntry>> {
        return dates.associateWith { date ->
            dao.listByDate(date).map { it.toModel() }
        }
    }
    private fun MealPlanEntryEntity.toModel(): MealPlanEntry {
        return MealPlanEntry(
            id = id,
            date = date,
            mealId = mealId,
            name = name,
            source = source,
            grams = grams,
            servings = servings,
            amount = amount,
            amountUnit = amountUnit,
            kcal = kcal,
            protein = protein,
            carbs = carbs,
            fats = fats,
            cholesterol = cholesterol,
            fdcId = fdcId,
            recipeId = recipeId,
            inventoryId = inventoryId,
            cost = cost.toBigDecimalOrNull(),
            currency = currency,
            isCompleted = isCompleted
        )
    }

    private fun MealPlanEntry.toEntity(updatedAt: Long = System.currentTimeMillis()): MealPlanEntryEntity {
        return MealPlanEntryEntity(
            id = id,
            date = date,
            mealId = mealId,
            name = name,
            source = source,
            grams = grams,
            servings = servings,
            amount = amount,
            amountUnit = amountUnit,
            kcal = kcal,
            protein = protein,
            carbs = carbs,
            fats = fats,
            cholesterol = cholesterol,
            fdcId = fdcId,
            recipeId = recipeId,
            inventoryId = inventoryId,
            cost = cost?.toPlainString(),
            currency = currency,
            sourcePlanId = sourcePlanId,
            isCompleted = isCompleted,
            updatedAt = updatedAt
        )
    }

    private fun PlanWithEntries.toDomainModel(): MenuPlanSummary {
        return MenuPlanSummary(
            id = plan.id,
            name = plan.name,
            description = plan.description ?: "",
            dayCount = plan.dayCount,
            entries = entries.map { it.toModel() }
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: MealPlanRepository? = null

        fun getInstance(context: Context): MealPlanRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MealPlanRepository(context, AppDatabase.getInstance(context))
                INSTANCE = instance
                instance
            }
        }
    }
}

private fun String?.toBigDecimalOrNull(): BigDecimal? {
    if (this == null) return null
    return runCatching { BigDecimal(this) }.getOrNull()
}

private fun String?.toBigDecimalOrZero(): BigDecimal {
    return toBigDecimalOrNull() ?: BigDecimal.ZERO
}
