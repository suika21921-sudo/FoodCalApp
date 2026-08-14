package com.mealoo.app.model

import android.content.Context
import androidx.annotation.StringRes
import com.mealoo.app.R
import com.mealoo.app.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID
import kotlinx.serialization.json.*

@Serializable
enum class AppCurrency(val code: String, val symbol: String, @StringRes val displayName: Int, val flagEmoji: String) {
    TWD("TWD", "NT$", R.string.currency_twd, "🇹🇼"),
    JPY("JPY", "¥", R.string.currency_jpy, "🇯🇵"),
    USD("USD", "$", R.string.currency_usd, "🇺🇸"),
    CNY("CNY", "¥", R.string.currency_cny, "🇨🇳"),
    EUR("EUR", "€", R.string.currency_eur, "🇪🇺"),
    HKD("HKD", "HK$", R.string.currency_hkd, "🇭🇰"),
    GBP("GBP", "£", R.string.currency_gbp, "🇬🇧"),
    KRW("KRW", "₩", R.string.currency_krw, "🇰🇷"),
    OTHER("OTHER", "$", R.string.currency_other, "🌐");

    companion object {
        fun fromCode(code: String?): AppCurrency {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: TWD
        }
        fun fromLanguage(language: String?): AppCurrency {
            return when (language?.lowercase()) {
                "ja" -> JPY
                "zh" -> TWD
                "en" -> USD
                else -> TWD
            }
        }
    }
}
// UI 與儲存使用的標準貨幣 DTO
data class CurrencyInfo(
    val code: String,
    val name: String,      // 顯示名稱 (已在地化，或 fallback 英文)
    val symbol: String,
    val flagEmoji: String
)

// 讓 AppCurrency 提供對應的 CurrencyInfo
fun AppCurrency.toCurrencyInfo(context: android.content.Context): CurrencyInfo {
    return CurrencyInfo(
        code = this.code,
        name = context.getString(this.displayName),
        symbol = this.symbol,
        flagEmoji = this.flagEmoji
    )
}

// 動態生成 CurrencyInfo 的工廠方法
fun createCurrencyInfo(context: android.content.Context, code: String, floatRatesName: String? = null): CurrencyInfo {
    val known = AppCurrency.entries.find { it.code.equals(code, ignoreCase = true) }
    if (known != null && known != AppCurrency.OTHER) {
        return known.toCurrencyInfo(context)
    }
    // 未知貨幣 fallback
    return CurrencyInfo(
        code = code.uppercase(),
        name = floatRatesName ?: code.uppercase(),
        symbol = "$", // 可透過其他開源庫補齊，或統一代入 $
        flagEmoji = "🏳️"
    )
}
@Serializable
data class FoodItem(
    val fdcId: Int,
    val source: String? = null,
    val nameEn: String,
    val nameZh: String,
    val nameJa: String,
    val categoryEn: String? = null,
    val categoryZh: String? = null,
    val categoryJa: String? = null,
    val nutrients: Map<String, NutrientEntry>
)

@Serializable
data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,

    // 🔥 庫存數量改為 BigDecimal，避免 0.3 - 0.1 = 0.199999
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal = BigDecimal.ONE,

    val unit: String = "pcs",
    val location: String = "Pantry",
    val expiryDate: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val fdcId: Int? = null,
    val standardNutrientsPer100g: Map<String, Double>? = null,
    val gramsPerUnit: Double? = null,
    val category: String? = null,

    // 🔥 成本改為 BigDecimal
    @Serializable(with = BigDecimalSerializer::class)
    val cost: BigDecimal? = null,

    val currency: String = AppCurrency.TWD.code,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val parentId: String? = null,
    val source: String? = null // New field
)
// 👉 新增：给 InventoryItem 用的显示字串
fun InventoryItem.displayCostLabel(): String? {
    val value = cost ?: return null
    val symbol = AppCurrency.fromCode(currency).symbol
    // 你要几位小数自己改，这里先用 2 位
    return "$symbol${"%.2f".format(value)}"
}

data class LocalizedNutrientEntry(
    val fdcId: Int? = null,
    val nameEn: String,
    val nameZh: String,
    val nameJa: String,
    val categoryEn: String? = null,
    val categoryZh: String? = null,
    val categoryJa: String? = null,
    val nutrients: Map<String, Double> // 英文 key
)

object NutrientKeys {
    val STANDARD_KEYS = listOf(
        "Energy",
        "Protein",
        "Carbohydrate, by difference",
        "Available carbohydrate",
        "Fiber, total dietary",
        "Total lipid (fat)",
        "Sodium, Na",
        "Cholesterol"
    )
}
data class SelectedFood(
    val fdcId: Int?=null,
    val source: String,
    val name: String,
    val grams: Double,
    val standardNutrients: Map<String, Double>,
    val inventoryId: String? = null,
    val category: String? = null,
)
object FoodJsonLoader {
    val DefaultUnits = mapOf(
        "Energy" to "kcal",
        "Protein" to "g",
        "Available carbohydrate" to "g",
        "Carbohydrate, by difference" to "g",
        "Fiber, total dietary" to "g",
        "Total lipid (fat)" to "g",
        "Sodium, Na" to "mg",
        "Cholesterol" to "mg"
    )
    private val json = Json {
        ignoreUnknownKeys = true
    }
    suspend fun loadFoodsBySource(
        context: Context,
        source: String
    ): List<FoodItem> {
        val fileName = when (source.uppercase()) {
            "JP" -> "jp_foods.json"
            else -> throw IllegalArgumentException("Unsupported source: $source")
        }
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return json.decodeFromString(jsonString)
    }
    suspend fun loadAllFoods(context: Context): List<FoodItem> {
        val jp = loadFoodsBySource(context, "JP")
        return jp
    }
    suspend fun loadMergedFoods(context: Context): List<FoodItem> {
        val baseFoods = loadFoodsBySource(context, "JP").associateBy { it.fdcId }
        val jsonString = context.assets.open("data.json").bufferedReader().use { it.readText() }
        val jsonArray = Json.parseToJsonElement(jsonString).jsonArray
        return jsonArray.mapNotNull { jsonEl ->
            val obj = jsonEl.jsonObject
            val fdcId = obj["foodId"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val base = baseFoods[fdcId] ?: return@mapNotNull null
            val raw = mapOf(
                "Energy" to obj["enercKcal"]?.jsonPrimitive?.doubleOrNull,
                "Protein" to obj["prot"]?.jsonPrimitive?.doubleOrNull,
                "Available carbohydrate" to obj["choavl"]?.jsonPrimitive?.doubleOrNull,
                "Carbohydrate, by difference" to obj["chocdf"]?.jsonPrimitive?.doubleOrNull,
                "Fiber, total dietary" to obj["fib"]?.jsonPrimitive?.doubleOrNull,
                "Total lipid (fat)" to obj["fat"]?.jsonPrimitive?.doubleOrNull,
                "Sodium, Na" to obj["na"]?.jsonPrimitive?.doubleOrNull,
                "Cholesterol" to obj["chole"]?.jsonPrimitive?.doubleOrNull
            )
            val nutrients = NutrientKeys.STANDARD_KEYS.mapNotNull { key ->
                val amount = raw[key] ?: return@mapNotNull null
                val unit = DefaultUnits[key] ?: "g"
                key to NutrientEntry(amount = amount, unit = unit)
            }.toMap()

            base.copy(nutrients = nutrients)
        }
    }
    suspend fun loadLocalizedNutrientsFromDataJson(context: Context): List<LocalizedNutrientEntry> {
        val jpFoods = loadFoodsBySource(context, "JP").associateBy { it.fdcId }
        val jsonString = context.assets.open("data.json").bufferedReader().use { it.readText() }
        val jsonArray = kotlinx.serialization.json.Json.parseToJsonElement(jsonString).jsonArray
        return jsonArray.mapNotNull { jsonEl ->
            val obj = jsonEl.jsonObject
            val fdcId = obj["foodId"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val food = jpFoods[fdcId]
            // 营养素字段取值（原本就有）
            val raw = mapOf(
                "Energy" to obj["enercKcal"]?.jsonPrimitive?.doubleOrNull,
                "Protein" to obj["prot"]?.jsonPrimitive?.doubleOrNull,
                "Available carbohydrate" to obj["choavl"]?.jsonPrimitive?.doubleOrNull,
                "Carbohydrate, by difference" to obj["chocdf"]?.jsonPrimitive?.doubleOrNull,
                "Fiber, total dietary" to obj["fib"]?.jsonPrimitive?.doubleOrNull,
                "Total lipid (fat)" to obj["fat"]?.jsonPrimitive?.doubleOrNull,
                "Sodium, Na" to obj["na"]?.jsonPrimitive?.doubleOrNull,
                "Cholesterol" to obj["chole"]?.jsonPrimitive?.doubleOrNull
            )
            val nutrients = NutrientKeys.STANDARD_KEYS.associate { key ->
                key to (raw[key] ?: 0.0)
            }

            LocalizedNutrientEntry(
                fdcId = fdcId,
                nameEn = food?.nameEn ?: "",
                nameZh = food?.nameZh ?: "",
                nameJa = food?.nameJa ?: "",
                categoryEn = food?.categoryEn,
                categoryZh = food?.categoryZh,
                categoryJa = food?.categoryJa,
                nutrients = nutrients
            )
        }
    }
}
//wrapper
// 定義來源類型，方便 UI 做不同的提示 (例如顯示 "庫存剩餘")
enum class FoodSourceType {
    DATABASE, // 一般資料庫
    INVENTORY // 冰箱/庫存
}

data class FoodSelectionCandidate(
    val id: String,
    val name: String,
    val nutrientsPer100g: Map<String, Double>,

    val sourceType: FoodSourceType = FoodSourceType.DATABASE,

    // --- 庫存專用 ---
    val inventoryStockAmount: Double? = null,
    val inventoryUnit: String? = null,
    val inventoryCost: BigDecimal? = null,

    // 🔥 新增這個，如果是 pcs，我們需要知道一顆多重才能算 grams
    val inventoryGramsPerUnit: Double? = null
)
fun mapInventoryToCandidate(item: InventoryItem): FoodSelectionCandidate {
    return FoodSelectionCandidate(
        id = item.id,
        name = item.name,
        // 如果 InventoryItem 有存標準營養素就用，沒有就給空 map (或去查資料庫)
        nutrientsPer100g = item.standardNutrientsPer100g ?: emptyMap(),

        sourceType = FoodSourceType.INVENTORY,
        inventoryStockAmount = item.quantity.toDouble(), // BigDecimal -> Double for UI
        inventoryUnit = item.unit,
        inventoryCost = item.cost,
        inventoryGramsPerUnit = item.gramsPerUnit // 🔥 帶入單顆重量
    )
}
