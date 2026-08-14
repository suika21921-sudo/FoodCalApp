package com.mealoo.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mealoo.app.R
import com.mealoo.app.db.AppDatabase
import com.mealoo.app.db.InventoryItemEntity
import com.mealoo.app.db.MealPlanEntryEntity
import com.mealoo.app.db.SavingsRecordEntity
import com.mealoo.app.db.ShopEntity
import com.mealoo.app.db.ShopProductEntity
import com.mealoo.app.db.ShoppingReplacementEntity
import com.mealoo.app.model.DEFAULT_BREAKFAST_ID
import com.mealoo.app.model.DEFAULT_DINNER_ID
import com.mealoo.app.model.DEFAULT_LUNCH_ID
import com.mealoo.app.model.DiaryNoteEntry
import com.mealoo.app.model.IntakeEntry
import androidx.room.withTransaction
import com.mealoo.app.db.PlanEntity
import com.mealoo.app.model.AppCurrency
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.mealoo.app.model.DrinkEntry
import com.mealoo.app.model.EatEntry
import com.mealoo.app.model.InventoryItem
import com.mealoo.app.model.MealDef
import com.mealoo.app.model.MealKind
import com.mealoo.app.model.MealPlanEntry
import com.mealoo.app.model.RecipeEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json.Default.decodeFromString
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random
import com.mealoo.app.pricing.EventRule
import com.mealoo.app.pricing.Step
import com.mealoo.app.pricing.requiredItemRange
import com.mealoo.app.util.BigDecimalSerializer // 請確保有這個 import
import com.mealoo.app.util.InventoryAlert
import com.mealoo.app.util.InventoryAlertEngine
import com.mealoo.app.util.Result
import com.mealoo.app.util.buildThawNote
import com.mealoo.app.util.hasThawTag
import kotlinx.serialization.Serializable
import kotlin.collections.filter
import kotlin.collections.map

private val Context.dataStore by preferencesDataStore(name = "intake_store")



class IntakeDataStore(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).intakeDao()

    suspend fun addEntry(entry: IntakeEntry) {
        dao.upsertAll(listOf(entry.toEntity()))
    }
    suspend fun getDrinkEntriesForDate(date: String): List<DrinkEntry> {
        return getEntriesForDate(date).filterIsInstance<DrinkEntry>()
    }
    suspend fun updateEntriesForDate(date: String, entries: List<IntakeEntry>) {
        AppDatabase.getInstance(context).withTransaction {
            dao.deleteByDate(date)
            dao.upsertAll(entries.map { it.toEntity() })
        }
    }
    suspend fun getEntriesForDate(date: String): List<IntakeEntry> {
        return dao.getByDate(date).map { it.toModel() }
    }
    fun entriesFlow(date: String): Flow<List<IntakeEntry>> =
        dao.getByDateFlow(date).map { list -> list.map { it.toModel() } }

    suspend fun getEntriesForDateRange(dates: List<String>): Map<String, List<IntakeEntry>> {
        if (dates.isEmpty()) return emptyMap()
        val sortedDates = dates.sorted()
        val start = sortedDates.first()
        val end = sortedDates.last()
        val flat = dao.getByDateRange(start, end)
        val grouped = flat.map { it.toModel() }.groupBy { it.date }
        return dates.associateWith { grouped[it] ?: emptyList() }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun existsBySourcePlanId(planId: String): Boolean {
        return dao.existsBySourcePlanId(planId)
    }

    suspend fun getAllDates(): List<String> {
        return dao.getAllDates()
    }
}
private val Context.recipedataStore by preferencesDataStore(name = "recipe_store")

class RecipeDataStore(private val context: Context) {

    companion object {
        private val jsonFormatter = Json {
            ignoreUnknownKeys = true
        }
    }

    private val recipesKey = stringPreferencesKey("recipes")

    fun recipesFlow(): Flow<List<RecipeEntry>> =
        context.recipedataStore.data.map { prefs ->
            val raw = prefs[recipesKey] ?: "[]"
            try { jsonFormatter.decodeFromString(raw) } catch (_: Exception) { emptyList() }
        }

    /**
     * 保存所有食谱
     */
    suspend fun saveRecipes(recipes: List<RecipeEntry>) {
        val json = jsonFormatter.encodeToString(recipes)
        context.recipedataStore.edit { prefs ->
            prefs[recipesKey] = json
        }
    }

    /**
     * 获取所有食谱
     */
    suspend fun getRecipes(): List<RecipeEntry> {
        val prefs = context.recipedataStore.data.first()
        val json = prefs[recipesKey] ?: return emptyList()
        return try {
            jsonFormatter.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun loadRecipesFromAsset(): List<RecipeEntry> {
        return try {
            val inputStream = context.assets.open("recipes.json")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            jsonFormatter.decodeFromString(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }
}



//=====diaryNoteDataStore=====//
private val Context.diaryNoteDataStore by preferencesDataStore(name = "diary_notes")

class DiaryNoteStorage(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun getKey(date: String) = stringPreferencesKey("note_$date")

    suspend fun getNotesForDate(date: String): List<DiaryNoteEntry> {
        val prefs = context.diaryNoteDataStore.data.first()
        val raw = prefs[getKey(date)] ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun getNotesForDateRange(dates: List<String>): Map<String, List<DiaryNoteEntry>> {
        val prefs = context.diaryNoteDataStore.data.first()
        val result = mutableMapOf<String, List<DiaryNoteEntry>>()

        dates.forEach { dateStr ->
            val json = prefs[getKey(dateStr)]
            if (json != null) {
                try {
                    result[dateStr] = decodeFromString(json)
                } catch (e: Exception) {
                    result[dateStr] = emptyList()
                }
            } else {
                result[dateStr] = emptyList()
            }
        }
        return result
    }
    suspend fun saveNotesForDate(date: String, notes: List<DiaryNoteEntry>) {
        val encoded = json.encodeToString(notes)
        context.diaryNoteDataStore.edit { prefs ->
            prefs[getKey(date)] = encoded
        }
    }
}
//=====MenuPlanDataStore=====//
private val Context.menuPlanStore by preferencesDataStore(name = "menu_plan_store")

@Serializable
data class PlannedIngredient(
    val fdcId: Int? = null,     // 与库存优先用 fdcId 匹配
    val name: String,           // 没有 fdcId 时兜底匹配
    val grams: Double           // 该配料所需克数
)

@Serializable
data class PlannedRecipe(
    val id: String = UUID.randomUUID().toString(),
    val mealId: String? = null,
    val recipeId: String? = null,
    val recipeName: String,
    val servings: Double = 1.0,
    val ingredients: List<PlannedIngredient>,
    val approxKcal: Double = 0.0 // ✅ 新增：存储该计划项的估算总热量
)

private val Context.mealNamesStore by preferencesDataStore(name = "meal_names_store")
private val KEY_MEAL_NAMES_JSON = stringPreferencesKey("custom_meal_names_json")

class MealNamesStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 讀取有序餐名清單；空表代表未自訂 */
    fun mealsFlow(): Flow<List<String>> =
        context.mealNamesStore.data.map { prefs ->
            prefs[KEY_MEAL_NAMES_JSON]?.let run@{
                return@run try { json.decodeFromString<List<String>>(it) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }

    suspend fun getMeals(): List<String> = mealsFlow().first()

    /** 覆寫；自動去頭尾空白、去空值、去重、保序 */
    suspend fun setMeals(names: List<String>) {
        val cleaned = names.map { it.trim() }
            .filter { it.isNotEmpty() }
            .fold(mutableListOf<String>()) { acc, s -> if (s !in acc) acc.apply { add(s) } else acc }

        context.mealNamesStore.edit { it[KEY_MEAL_NAMES_JSON] = json.encodeToString(cleaned) }
    }
}
private val Context.mealConfigStore by preferencesDataStore(name = "meal_config_store")

@Serializable
data class DailyMealConfig(
    val mealIds: List<String> = emptyList(),
    val ratioByMealId: Map<String, Double> = emptyMap() // 0.1% 量化后、总和=100
)

class DailyMealConfigStore(private val context: Context) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
        private fun keyFor(date: String) = stringPreferencesKey("meal_cfg_$date")
    }
    fun flow(date: String): Flow<DailyMealConfig?> =
        context.mealConfigStore.data.map { prefs ->
            prefs[keyFor(date)]?.let { runCatching { json.decodeFromString(DailyMealConfig.serializer(), it) }.getOrNull() }
        }

    suspend fun get(date: String): DailyMealConfig? {
        val prefs = context.mealConfigStore.data.first()
        val raw = prefs[keyFor(date)] ?: return null
        return runCatching { json.decodeFromString(DailyMealConfig.serializer(), raw) }.getOrNull()
    }

    suspend fun save(date: String, cfg: DailyMealConfig) {
        val raw = json.encodeToString(DailyMealConfig.serializer(), cfg)
        context.mealConfigStore.edit { it[keyFor(date)] = raw }
    }

    suspend fun clearAll() {
        context.mealConfigStore.edit { prefs ->
            val keys = prefs.asMap().keys.filter { it.name.startsWith("meal_cfg_") }
            keys.forEach { prefs.remove(it) }
        }
    }
}

private val Context.discountEventStore by preferencesDataStore(name = "discount_event_store")
private val DISCOUNT_EVENTS_KEY = stringPreferencesKey("discount_events_json")
@Serializable
data class DiscountItem(
    val id: String = newId(),
    val name: String,
    val unit: String? = null,
    val currency: String = AppCurrency.TWD.code,
    @Serializable(with = BigDecimalSerializer::class)
    val qty: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val unitPrice: BigDecimal? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val note: String? = null
)
@Serializable
data class DiscountEvent(
    val id: String = newId(),
    val shopId: String,
    val title: String,
    val startDate: String,
    val endDate: String? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val url: String? = null,
    val rule: EventRule,

    // 這裡存的是 "該活動特有的特價品" (例如 DM 上的促銷項)
    val items: List<DiscountItem> = emptyList()
)


fun DiscountEvent.validateItemsForRule(): String? {
    val need = requiredItemRange(this.rule)         // requires DiscountEvent.rule: EventRule
    val n = items.size
    if (n < need.first) return "此規則至少需要 ${need.first} 個品項"
    if (need.last != Int.MAX_VALUE && n > need.last) return "此規則最多允許 ${need.last} 個品項"
    return null
}

private fun newId(): String = "${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"

class DiscountEventDataStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun eventsFlow(): Flow<List<DiscountEvent>> =
        context.discountEventStore.data.map { prefs ->
            prefs[DISCOUNT_EVENTS_KEY]?.let {
                runCatching { json.decodeFromString<List<DiscountEvent>>(it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun getEvents(): List<DiscountEvent> = eventsFlow().first()

    private suspend fun saveAll(events: List<DiscountEvent>) {
        context.discountEventStore.edit { prefs ->
            prefs[DISCOUNT_EVENTS_KEY] = json.encodeToString(events)
        }
    }

    suspend fun addEvent(event: DiscountEvent) {
        val current = getEvents()
        val next = (listOf(event) + current.filterNot { it.id == event.id })
            .distinctBy { it.id }
        saveAll(next)
    }

    suspend fun updateEvent(event: DiscountEvent) {
        val current = getEvents().toMutableList()
        val idx = current.indexOfFirst { it.id == event.id }
        if (idx >= 0) current[idx] = event else current.add(0, event)
        saveAll(current.distinctBy { it.id })
    }

    suspend fun deleteEvent(id: String) {
        val current = getEvents().filterNot { it.id == id }
        saveAll(current)
    }

    suspend fun addItem(eventId: String, item: DiscountItem) {
        val current = getEvents().toMutableList()
        val idx = current.indexOfFirst { it.id == eventId }
        if (idx >= 0) {
            val e = current[idx]
            current[idx] = e.copy(items = e.items + item)
            saveAll(current)
        }
    }

    suspend fun updateItem(eventId: String, item: DiscountItem) {
        val current = getEvents().toMutableList()
        val idx = current.indexOfFirst { it.id == eventId }
        if (idx >= 0) {
            val e = current[idx]
            val list = e.items.toMutableList()
            val iIdx = list.indexOfFirst { it.id == item.id }
            if (iIdx >= 0) list[iIdx] = item
            current[idx] = e.copy(items = list)
            saveAll(current)
        }
    }

    suspend fun removeItem(eventId: String, itemId: String) {
        val current = getEvents().toMutableList()
        val idx = current.indexOfFirst { it.id == eventId }
        if (idx >= 0) {
            val e = current[idx]
            current[idx] = e.copy(items = e.items.filterNot { it.id == itemId })
            saveAll(current)
        }
    }

}

private val Context.shoppingItemsStore by preferencesDataStore(name = "shopping_items_store")

enum class PurchaseSlot { ANY, MORNING, AFTERNOON, EVENING, NIGHT }
// 给时间段一个稳定排序：早上 < 中午 < 下午 < 晚上 < 其它
val PurchaseSlot.orderIndex: Int
    get() = when (this.name.lowercase()) {
        "morning", "am", "breakfast" -> 0
        "noon", "midday", "lunch"    -> 1
        "afternoon", "pm"            -> 2
        "evening", "dinner", "night" -> 3
        else                         -> 9
    }

// 转成用户可读文本（按你 App 的文案改）
val PurchaseSlot.label: String
    get() = when (this.name.lowercase()) {
        "morning", "am", "breakfast" -> "上午"
        "noon", "midday", "lunch"    -> "中午"
        "afternoon", "pm"            -> "下午"
        "evening", "dinner", "night" -> "晚上"
        else                         -> "时段"
    }
@Serializable
data class CartItem(
    val id: String = "",
    val image: String? = null,
    val name: String? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val unitPrice: BigDecimal,

    val currency : String ?= null,

    @Serializable(with = BigDecimalSerializer::class)
    val Qty: BigDecimal? = null,

    val discount: EventRule?= null,
    val plannedDate: String? = null,
    val plannedSlot: PurchaseSlot? = null,
)
@Serializable
data class ShoppingListItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",

    // [Fix] Double -> BigDecimal
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal = BigDecimal.ONE,

    val unit: String = "pcs",
    val imageUri: String? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),

    // [Fix] Double -> BigDecimal
    @Serializable(with = BigDecimalSerializer::class)
    val basePrice: BigDecimal? = null,

    val currency: String ?= AppCurrency.USD.code,
    val discountEventId: String? = null,
    val plannedDate: String? = null,
    val plannedSlot: PurchaseSlot? = null,
    val shopId: String? = null,
    val shopProductId: String? = null
)

class ShoppingCartStore(private val context: Context) {
    companion object {
        private val key = stringPreferencesKey("shopping_items_json")
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    }

    fun itemsFlow(): Flow<List<ShoppingListItem>> =
        context.shoppingItemsStore.data.map { prefs ->
            val raw = prefs[key] ?: "[]"
            try { json.decodeFromString(raw) } catch (_: Exception) { emptyList() }
        }

    suspend fun getAll(): List<ShoppingListItem> = itemsFlow().first()

    suspend fun upsert(item: ShoppingListItem) {
        val current = getAll().toMutableList()
        val i = current.indexOfFirst { it.id == item.id }
        if (i >= 0) current[i] = item else current.add(0, item)
        saveAll(current)
    }

    suspend fun delete(id: String) {
        val current = getAll().filterNot { it.id == id }
        saveAll(current)
    }

    suspend fun replaceAll(items: List<ShoppingListItem>) = saveAll(items)

    private suspend fun saveAll(items: List<ShoppingListItem>) {
        val raw = json.encodeToString(items)
        context.shoppingItemsStore.edit { it[key] = raw }
    }
    // DataStore.kt 里：ShoppingCartStore 附近追加 —— 选中的折扣活动 ID
    private val SELECTED_EVENT_ID_KEY = stringPreferencesKey("shopping_selected_event_id")

    fun selectedEventIdFlow(): Flow<String?> =
        context.shoppingItemsStore.data.map { it[SELECTED_EVENT_ID_KEY] }

    suspend fun setSelectedEventId(eventId: String?) {
        context.shoppingItemsStore.edit { prefs ->
            if (eventId == null) prefs.remove(SELECTED_EVENT_ID_KEY)
            else prefs[SELECTED_EVENT_ID_KEY] = eventId
        }
    }
    suspend fun setPlannedWindow(itemId: String, date: String?, slot: PurchaseSlot?) {
        val all = getAll().toMutableList()
        val idx = all.indexOfFirst { it.id == itemId }
        if (idx >= 0) {
            val old = all[idx]
            all[idx] = old.copy(plannedDate = date, plannedSlot = slot)
            saveAll(all)
        }
    }
}
enum class ShopCategory {
    RETAIL,      // 零售 (全聯、家樂福)：主打購物
    RESTAURANT,  // 餐飲 (麥當勞、星巴克)：主打飲食
    HYBRID       // 混合 (Costco, IKEA)：有商品也有餐飲部
}

// 修改 DataStore.kt

// 1. ShopInfo: 重型載體 (地點、地圖、常駐商品)
@Serializable
data class ShopInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                  // 店名 (Costco, 麥當勞)
    val address: String? = null,       // 地址 (用於導航url)
    val googlePlaceId: String? = null, // Google Maps Place ID (更精準的導航)
    val type: ShopCategory = ShopCategory.RETAIL,
    val activeDiscountEventIds: List<String> = emptyList(),
    val catalog: List<ShopProduct> = emptyList()
)

@Serializable
data class ShopProduct(
    val id: String = UUID.randomUUID().toString(),
    val shopId: String,
    val name: String,

    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal? = null,

    val currency: String = AppCurrency.TWD.code,
    val nutrients: Map<String, Double>? = null, // 營養素保留 Double
    val category: List<String>? = null,
    val discountItemId: String? = null,
    val eligibleDiscountEventIds: List<String> = emptyList()
)


// ===================== Prompt Store =====================

private val Context.promptStore by preferencesDataStore(name = "prompt_store")
private val KEY_PROMPTS_JSON = stringPreferencesKey("saved_prompts_json")

class PromptStore(private val context: Context) {

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    fun promptsFlow(): Flow<List<SavedPrompt>> =
        context.promptStore.data.map { prefs ->
            prefs[KEY_PROMPTS_JSON]?.let { raw ->
                try {
                    json.decodeFromString<List<SavedPrompt>>(raw)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun getAll(): List<SavedPrompt> = promptsFlow().first()

    private suspend fun saveAll(list: List<SavedPrompt>) {
        val raw = json.encodeToString(list)
        context.promptStore.edit { prefs ->
            prefs[KEY_PROMPTS_JSON] = raw
        }
    }

    suspend fun upsert(prompt: SavedPrompt) {
        val current = getAll().toMutableList()
        val idx = current.indexOfFirst { it.id == prompt.id }
        if (idx >= 0) {
            current[idx] = prompt
        } else {
            // 新的放在最前面，方便最近使用
            current.add(0, prompt)
        }
        saveAll(current)
    }

    suspend fun delete(id: String) {
        val current = getAll().filterNot { it.id == id }
        saveAll(current)
    }

    suspend fun clear() {
        saveAll(emptyList())
    }
}
enum class PromptKind {
    GENERIC,                  // 任意用途
    INVENTORY_RECIPE,         // 库存 → 单次食谱
    INVENTORY_WEEKLY_PLAN,    // 库存 → 一周菜单
    INVENTORY_SHOPPING_LIST   // 库存 → 购物清单
}

/**
 * 模板里“可以被数据块替换”的占位符种类
 */
enum class PromptVariable {
    INVENTORY_BLOCK,      // {{INVENTORY_BLOCK}}
    MACRO_PREF_BLOCK,     // {{MACRO_PREF_BLOCK}}
    LIFESTYLE_BLOCK,      // {{LIFESTYLE_BLOCK}}
    SERVINGS,             // {{SERVINGS}}
    EXTRA_NOTE,           // {{EXTRA_NOTE}}
    GOAL,                 // {{GOAL}}
    ACTIVITY_LEVEL,       // {{ACTIVITY_LEVEL}}
    DIET_TYPE,            // {{DIET_TYPE}}
    MEAL_RATIO_MAP,       // {{MEAL_RATIO_MAP}}
    OUTPUT_SCHEMA,        // {{OUTPUT_SCHEMA}}
    TODAY_DATE,           // {{TODAY_DATE}}  → 今日日期 (系統時間)
    WEIGHT_KG,            // {{WEIGHT_KG}}   → 使用者體重 (kg)
    HEIGHT_CM,            // {{HEIGHT_CM}}   → 使用者身高 (cm)
    AGE,                  // {{AGE}}         → 使用者年齡
    DAILY_KCAL_TARGET,    // {{DAILY_KCAL_TARGET}} → 每日熱量目標
}
@Serializable
data class SavedPrompt(
    val id: String,
    val title: String,
    val body: String,                   // 真正写给 LLM 的模板文字
    val kind: PromptKind,
    val variables: Set<PromptVariable>  // 这个模板会用到哪些变量
)

@Serializable
data class SavingsRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val shopId: String? = null,
    val eventId: String? = null,
    val currency: String = AppCurrency.TWD.code,

    // [Fix] Double -> BigDecimal
    @Serializable(with = BigDecimalSerializer::class)
    val original: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val afterDiscount: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val savings: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val savingsRate: BigDecimal,

    val itemCount: Int,
    val note: String? = null,
    val computedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SavingsState(
    val records: List<SavingsRecord> = emptyList()
)







private val Context.mealDefsStore by preferencesDataStore(name = "meal_defs_store")
private val KEY_MEAL_DEFS_JSON = stringPreferencesKey("meal_defs_json")

class MealDefsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun mealDefsFlow(): Flow<List<MealDef>> =
        context.mealDefsStore.data.map { prefs ->
            decode(prefs[KEY_MEAL_DEFS_JSON]) ?: emptyList()
        }.map { defs ->
            if (defs.isEmpty()) defaultMealDefs() else defs
        }

    suspend fun getAll(): List<MealDef> {
        val prefs = context.mealDefsStore.data.first()
        val defs = decode(prefs[KEY_MEAL_DEFS_JSON]) ?: emptyList()
        if (defs.isEmpty()) {
            val defaults = defaultMealDefs()
            setAll(defaults)
            return defaults
        }
        return defs
    }

    suspend fun setAll(defs: List<MealDef>) {
        val cleaned = defs
            .map { it.copy(name = it.name.trim()) }
            .filter { it.name.isNotEmpty() }
            .fold(mutableListOf<MealDef>()) { acc, def ->
                if (acc.any { it.id == def.id }) acc else acc.apply { add(def) }
            }
        context.mealDefsStore.edit { prefs ->
            prefs[KEY_MEAL_DEFS_JSON] = json.encodeToString(cleaned)
        }
    }

    suspend fun add(name: String, kind: MealKind? = MealKind.CUSTOM): MealDef {
        val finalName = name.trim().ifBlank { context.getString(R.string.meal) }
        val newDef = MealDef(
            id = UUID.randomUUID().toString(),
            name = finalName,
            kind = kind
        )
        setAll(getAll() + newDef)
        return newDef
    }

    suspend fun delete(id: String) {
        setAll(getAll().filterNot { it.id == id })
    }

    suspend fun rename(id: String, newName: String) {
        val finalName = newName.trim().ifBlank { context.getString(R.string.meal) }
        val updated = getAll().map { def ->
            if (def.id == id) def.copy(name = finalName) else def
        }
        setAll(updated)
    }

    suspend fun reorder(idsInOrder: List<String>) {
        val current = getAll()
        val byId = current.associateBy { it.id }
        val ordered = idsInOrder.mapNotNull { byId[it] }
        val remaining = current.filter { it.id !in idsInOrder }
        setAll(ordered + remaining)
    }

    suspend fun clearAll() {
        context.mealDefsStore.edit { prefs ->
            prefs.remove(KEY_MEAL_DEFS_JSON)
        }
    }

    private fun decode(raw: String?): List<MealDef>? {
        return raw?.let { runCatching { json.decodeFromString<List<MealDef>>(it) }.getOrNull() }
    }

    private fun defaultMealDefs(): List<MealDef> = listOf(
        MealDef(
            id = DEFAULT_BREAKFAST_ID,
            name = context.getString(R.string.breakfast),
            kind = MealKind.BREAKFAST
        ),
        MealDef(
            id = DEFAULT_LUNCH_ID,
            name = context.getString(R.string.lunch),
            kind = MealKind.LUNCH
        ),
        MealDef(
            id = DEFAULT_DINNER_ID,
            name = context.getString(R.string.dinner),
            kind = MealKind.DINNER
        )
    )
}
@Serializable
data class MealPlanSummaryDto(
    val type: String = "MealPlanSummary",
    val name: String,
    val description: String? = null,
    val dayCount: Int = 1,
    val entries: List<MealPlanEntryDto> = emptyList()
)

@Serializable
data class MealPlanEntryDto(
    val mealId: String,
    val name: String,
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fats: Double = 0.0
)
fun MealPlanSummaryDto.toEntities(targetDate: String): Pair<PlanEntity, List<MealPlanEntryEntity>> {
    val planId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()

    val planEntity = PlanEntity(
        id = planId,
        name = this.name,
        description = this.description,
        dayCount = this.dayCount,
        createdAt = now,
        updatedAt = now
    )

    val entryEntities = this.entries.map { dto ->
        MealPlanEntryEntity(
            id = UUID.randomUUID().toString(),
            date = targetDate,           // 根據需求填入目標日期
            mealId = dto.mealId,
            name = dto.name,
            source = "AI_MAGIC_INJECT",  // 標記來源，方便追溯
            grams = 0.0,                 // AI Schema 若沒提供則給預設
            servings = 1.0,
            amount = null,
            amountUnit = null,
            kcal = dto.kcal,
            protein = dto.protein,
            carbs = dto.carbs,
            fats = dto.fats,
            cholesterol = 0.0,           // Schema 未要求
            fdcId = null,
            recipeId = null,
            inventoryId = null,
            cost = null,
            currency = null,
            sourcePlanId = planId,       // 綁定 FK 關聯
            isCompleted = false,         // 新匯入的預設都是未完成
            updatedAt = now
        )
    }

    return Pair(planEntity, entryEntities)
}