package com.mealoo.app.data

import com.mealoo.app.model.EatEntry
import com.mealoo.app.model.DrinkEntry
import com.mealoo.app.model.IntakeEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.Firebase
import com.mealoo.app.model.RecipeIngredient
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.firestore
import com.mealoo.app.model.RecipeEntry
import com.mealoo.app.util.alcoholGramFromVolume
import kotlin.math.max
import kotlinx.serialization.Serializable
import com.mealoo.app.util.*
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import com.google.api.services.drive.Drive
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.File as DriveFile
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.collections.get
import kotlin.math.round

import com.mealoo.app.db.IntakeEntryEntity

private fun Any?.numD(): Double = (this as? Number)?.toDouble() ?: 0.0
private fun Any?.numDOrNull(): Double? = (this as? Number)?.toDouble()
private fun Any?.numFOrNull(): Float? = (this as? Number)?.toFloat()

fun IntakeEntryEntity.toModel(): IntakeEntry {
    return if (type == "EAT") {
        EatEntry(
            id = id,
            name = name,
            kcal = kcal,
            grams = grams ?: 0.0,
            protein = protein ?: 0.0,
            carbs = carbs ?: 0.0,
            fats = fats ?: 0.0,
            cholesterol = cholesterol ?: 0.0,
            fdcId = fdcId,
            source = source,
            category = category,
            cost = cost?.toBigDecimalOrNull(),
            currency = currency,
            date = date,
            mealId = mealId,
            sourcePlanId = sourcePlanId
        )
    } else {
        DrinkEntry(
            id = id,
            name = name,
            kcal = kcal,
            sugarGrams = sugarGrams ?: 0.0,
            caffeine = caffeine ?: 0.0,
            waterML = waterML ?: 0.0,
            alcoholVolumeML = alcoholVolumeML,
            alcoholPercent = alcoholPercent,
            category = category,
            date = date,
            timestamp = timestamp ?: 0L,
            cost = cost?.toBigDecimalOrNull(),
            currency = currency,
            mealId = mealId,
            sourcePlanId = sourcePlanId
        )
    }
}

fun IntakeEntry.toEntity(): IntakeEntryEntity {
    return when (this) {
        is EatEntry -> IntakeEntryEntity(
            id = id, type = "EAT", date = date, name = name, kcal = kcal,
            grams = grams, protein = protein, carbs = carbs, fats = fats, cholesterol = cholesterol,
            fdcId = fdcId, source = source, sourcePlanId = sourcePlanId, mealId = mealId,
            cost = cost?.toPlainString(), currency = currency, category = category,
            sugarGrams = null, caffeine = null, waterML = null, alcoholVolumeML = null, alcoholPercent = null, timestamp = null
        )
        is DrinkEntry -> IntakeEntryEntity(
            id = id, type = "DRINK", date = date, name = name, kcal = kcal,
            grams = null, protein = null, carbs = null, fats = null, cholesterol = null,
            fdcId = null, source = null, sourcePlanId = sourcePlanId, mealId = mealId,
            cost = cost?.toPlainString(), currency = currency, category = category,
            sugarGrams = sugarGrams, caffeine = caffeine, waterML = waterML,
            alcoholVolumeML = alcoholVolumeML, alcoholPercent = alcoholPercent, timestamp = timestamp
        )
    }
}
// Helper for cost
private fun String?.toBigDecimalOrNull(): java.math.BigDecimal? {
    if (this == null) return null
    return runCatching { java.math.BigDecimal(this) }.getOrNull()
}
private fun java.math.BigDecimal.toPlainString(): String {
    return this.toPlainString()
}

@Serializable
data class RecipeEntryDTO(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fats: Double = 0.0,
    val tagsV2: Map<String, List<String>> = emptyMap(),   // 新
    val timeRequired: Int? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val ingredientsText: List<String> = emptyList(),
    val creatorId: String = "",
    val description: String? = null,
    val cost: Double? = null,
    val currency: String? = null
)

class IntakeRepository(
    /** Google Drive service，可為 null（本機模式） */
    private val drive: Drive? = null
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** 是否有可用的 Drive（給 UI 判斷用，可選） */
    fun hasDrive(): Boolean = drive != null

    /** 內部使用：需要 Drive 的操作先拿到，不然就跳過 */
    private fun requireDriveOrNull(opName: String): Drive? {
        if (drive == null) {
            Log.w(
                "IntakeRepository",
                "Drive is null, skip op: $opName（目前為本機模式，未啟用雲端備份）"
            )
            return null
        }
        return drive
    }

    // ✅ 显式提供防御性检查，避免初始化期间触发 GMS 异常
    private fun getUserIdOrThrow(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("未登录，无法上传/读取数据")
    }

    private fun userCollection(): CollectionReference {
        val uid = getUserIdOrThrow()
        return firestore.collection("users").document(uid).collection("entries")
    }
    /**
     * 將最近 7 天的紀錄上傳到 Google Drive 的 appDataFolder
     * 會產生/更新檔名：mealoo_daily_intake_YYYY-MM-DD.json
     */
    suspend fun uploadRecent7Days(
        localDataSource: suspend (String) -> List<IntakeEntry>
    ): Boolean {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_DATE
        val pending = mutableListOf<Pair<String, List<IntakeEntry>>>()

        for (i in 0 until 7) {
            val date = today.minusDays(i.toLong()).format(formatter)
            // 這裡可以直接呼叫 suspend 函式，不會阻塞
            val entries = localDataSource(date)
            pending += date to entries
        }

        if (pending.all { it.second.isEmpty() }) {
            Log.w("Drive", "Recent 7 days are all empty, skip upload.")
            return false
        }

        try {
            pending
                .filter { it.second.isNotEmpty() }
                .forEach { (date, entries) ->
                    upsertDailyIntakeFile(date, entries)
                }
        } catch (e: Exception) {
            Log.e("Drive", "Error uploading 7-day entries to Drive", e)
            throw e
        }
        return true
    }

    /**
     * 單日上傳／覆蓋到 Google Drive appDataFolder
     */
    suspend fun uploadDailyEntries(date: String, entries: List<IntakeEntry>) {
        try {
            upsertDailyIntakeFile(date, entries)
        } catch (e: Exception) {
            Log.e("Drive", "Error uploading daily entries to Drive", e)
        }
    }

    /**
     * 從 Google Drive appDataFolder 讀取指定日期的紀錄
     */
    suspend fun loadEntriesForDate(date: String): List<IntakeEntry> {
        return withContext(Dispatchers.IO) {
            val drive = requireDriveOrNull("loadEntriesForDate") ?: return@withContext emptyList()

            val fileName = dailyFileName(date)
            val query =
                "name = '$fileName' and 'appDataFolder' in parents and trashed = false"

            val listResult = try {
                drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ(query)
                    .setFields("files(id, name)")
                    .execute()
            } catch (e: Exception) {
                Log.e("Drive", "Error searching daily intake file on Drive", e)
                null
            }

            if (listResult == null || listResult.files.isNullOrEmpty()) {
                return@withContext emptyList()
            }

            val fileId = listResult.files[0].id
            val output = ByteArrayOutputStream()

            try {
                drive.files().get(fileId).executeMediaAndDownloadTo(output)
                val rawString = output.toString(Charsets.UTF_8.name())
                val jsonString = com.mealoo.app.util.SecurityHelper.decryptCloudData(rawString)
                parseEntriesFromJson(jsonString)
            } catch (e: Exception) {
                Log.e("Drive", "Error downloading daily intake file from Drive", e)
                emptyList()
            }
        }
    }
    /** Google Drive 裡面每日檔案的命名規則 */
    private fun dailyFileName(date: String): String =
        "mealoo_daily_intake_${date}.json"

    /**
     * 實際做「有就 update，沒有就 create」的 Drive 寫入
     */
    private suspend fun upsertDailyIntakeFile(
        date: String,
        entries: List<IntakeEntry>
    ) {
        withContext(Dispatchers.IO) {
            val drive = requireDriveOrNull("upsertDailyIntakeFile") ?: return@withContext

            val fileName = dailyFileName(date)

            // 1) 把 entries 轉成 JSON
            val entryMaps = entries.map { it.toMap() }     // 利用你原本的 toMap()
            val arr = JSONArray()
            entryMaps.forEach { map ->
                arr.put(JSONObject(map))
            }
            val root = JSONObject().apply {
                put("date", date)
                put("entries", arr)
            }
            val jsonString = root.toString()

            val query =
                "name = '$fileName' and 'appDataFolder' in parents and trashed = false"

            val existing = try {
                drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ(query)
                    .setFields("files(id, name)")
                    .execute()
            } catch (e: Exception) {
                Log.e("Drive", "Error querying existing daily file", e)
                null
            }

            val encryptedString = try {
                com.mealoo.app.util.SecurityHelper.encryptCloudData(jsonString)
            } catch (e: Exception) {
                jsonString
            }
            val content = ByteArrayContent.fromString("application/json", encryptedString)

            if (existing == null || existing.files.isNullOrEmpty()) {
                // 新增檔案到 appDataFolder
                val meta = DriveFile().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
                try {
                    drive.files()
                        .create(meta, content)
                        .setFields("id")
                        .execute()
                } catch (e: Exception) {
                    Log.e("Drive", "Error creating daily intake file", e)
                }
            } else {
                // 更新第一個找到的檔案
                val id = existing.files[0].id
                try {
                    drive.files()
                        .update(id, null, content)
                        .execute()
                } catch (e: Exception) {
                    Log.e("Drive", "Error updating daily intake file", e)
                }
            }
        }
    }

    /**
     * Download ALL daily intake files from Drive appDataFolder.
     * Returns a flat list of all IntakeEntry found in all files.
     */
    suspend fun downloadAllDailyFiles(): List<IntakeEntry> {
        return withContext(Dispatchers.IO) {
            val drive = requireDriveOrNull("downloadAllDailyFiles") ?: return@withContext emptyList()

            // 1. List all files starting with prefix
            val query = "name contains 'mealoo_daily_intake_' and 'appDataFolder' in parents and trashed = false"
            val allFiles = mutableListOf<DriveFile>()
            var pageToken: String? = null

            do {
                val result = try {
                    drive.files().list()
                        .setSpaces("appDataFolder")
                        .setQ(query)
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute()
                } catch (e: Exception) {
                    Log.e("Drive", "Error listing daily files", e)
                    null
                }
                result?.files?.let { allFiles.addAll(it) }
                pageToken = result?.nextPageToken
            } while (pageToken != null)

            if (allFiles.isEmpty()) return@withContext emptyList()

            val allEntries = mutableListOf<IntakeEntry>()

            // 2. Download and parse each file
            for (file in allFiles) {
                val output = ByteArrayOutputStream()
                try {
                    drive.files().get(file.id).executeMediaAndDownloadTo(output)
                    val rawString = output.toString(Charsets.UTF_8.name())
                    val jsonString = com.mealoo.app.util.SecurityHelper.decryptCloudData(rawString)
                    val entries = parseEntriesFromJson(jsonString)
                    allEntries.addAll(entries)
                } catch (e: Exception) {
                    Log.e("Drive", "Error downloading/parsing file ${file.name}", e)
                    // Continue to next file even if one fails
                }
            }

            allEntries
        }
    }

    /**
     * Uploads daily files for dates that exist locally but NOT on Drive.
     * @param localDateSource Function to get all local dates (e.g. from DB)
     * @param localDataSource Function to get entries for a specific date (e.g. from DB)
     * @return Summary message
     */
    suspend fun uploadMissingFiles(
        localDateSource: suspend () -> List<String>,
        localDataSource: suspend (String) -> List<IntakeEntry>
    ): String {
        return withContext(Dispatchers.IO) {
            val drive = requireDriveOrNull("uploadMissingFiles") ?: return@withContext "Drive not connected"

            // 1. List all files on Drive to find existing dates
            val query = "name contains 'mealoo_daily_intake_' and 'appDataFolder' in parents and trashed = false"
            val existingDates = mutableSetOf<String>()
            var pageToken: String? = null

            do {
                val result = try {
                    drive.files().list()
                        .setSpaces("appDataFolder")
                        .setQ(query)
                        .setFields("nextPageToken, files(name)")
                        .setPageToken(pageToken)
                        .execute()
                } catch (e: Exception) {
                    Log.e("Drive", "Error listing for missing check", e)
                    null
                }
                result?.files?.forEach { file ->
                    // Filename format: mealoo_daily_intake_YYYY-MM-DD.json
                    val name = file.name
                    if (name.startsWith("mealoo_daily_intake_") && name.endsWith(".json")) {
                        val datePart = name.removePrefix("mealoo_daily_intake_").removeSuffix(".json")
                        existingDates.add(datePart)
                    }
                }
                pageToken = result?.nextPageToken
            } while (pageToken != null)

            // 2. Get local dates
            val localDates = localDateSource()

            // 3. Find missing
            val missingDates = localDates.filter { !existingDates.contains(it) }

            if (missingDates.isEmpty()) {
                return@withContext "No missing files to upload."
            }

            // 4. Upload missing
            var successCount = 0
            for (date in missingDates) {
                val entries = localDataSource(date)
                if (entries.isNotEmpty()) {
                    try {
                        upsertDailyIntakeFile(date, entries)
                        successCount++
                    } catch (e: Exception) {
                        Log.e("Drive", "Failed to upload missing file for $date", e)
                    }
                }
            }

            "Uploaded $successCount files (out of ${missingDates.size} missing)."
        }
    }

    /**
     * 從 JSON 字串還原成 List<IntakeEntry>
     * 會透過你原本的 Map<String, Any>.toIntakeEntry() 來建構 model
     */
    private fun parseEntriesFromJson(json: String): List<IntakeEntry> {
        return try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("entries") ?: return emptyList()

            val result = mutableListOf<IntakeEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val map = mutableMapOf<String, Any>()

                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = obj.get(key)
                    map[key] = value
                }

                result += map.toIntakeEntry()
            }
            result
        } catch (e: Exception) {
            Log.e("Drive", "Error parsing intake JSON", e)
            emptyList()
        }
    }
}
fun IntakeEntry.toMap(): Map<String, Any?> = when (this) {
    is EatEntry -> mapOf(
        "id" to id,
        "grams" to grams,
        "type" to "eat",
        "name" to name,
        "fdcId" to fdcId,
        "source" to source,
        "sourcePlanId" to sourcePlanId,
        "kcal" to kcal,
        "protein" to protein,
        "carbs" to carbs,
        "fats" to fats,
        "cholesterol" to cholesterol,
        "date" to date,
        "mealId" to mealId,
        "cost" to cost,
        "currency" to currency,
        )
    is DrinkEntry -> mapOf(
        "id" to id,
        "type" to "drink",
        "name" to name,
        "kcal" to kcal,
        "sourcePlanId" to sourcePlanId,
        "sugarGrams" to sugarGrams,
        "caffeine" to caffeine,
        "waterML" to waterML,
        "alcoholVolumeML" to alcoholVolumeML,
        "alcoholPercent" to alcoholPercent,
        "date" to date,
        "timestamp" to timestamp,
        "cost" to cost,
        "currency" to currency,
        "category" to category,
        "mealId" to mealId
        )

}
/**
 * 專門處理「社群食譜」相關的遠端存取（Firestore）。
 * - 個人飲食紀錄 → 走 IntakeRepository（Google Drive）
 * - 社群 feed / 分享 → 走 CommunityRepository（Firestore）
 *
 * RecipeEntryDTO / RepostRecipeDTO / toRecipeEntry() 等 DTO 與 mapping
 * 已經在同一個 package 的 IntakeRepository.kt 中定義，可以直接使用。
 */
class CommunityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * 檢查某個食譜是否已經分享到社群
     */
    suspend fun isRecipeShared(recipeId: String): Boolean {
        return try {
            val docRef = Firebase.firestore
                .collection("community").document("recipes")
                .collection("shared").document(recipeId)
            val snapshot = docRef.get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e("CheckShared", "检查是否已分享失败", e)
            false // 無法確認時預設未分享
        }
    }

    /**
     * 讀取社群裡所有公開食譜（DTO 格式），呼叫端再用 toRecipeEntry() 轉成 model。
     */
    suspend fun loadSharedRecipes(): List<RecipeEntryDTO> {
        val snapshot = firestore.collection("community")
            .document("recipes")
            .collection("shared")
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            it.toObject(RecipeEntryDTO::class.java)
        }
    }
}
fun Map<String, Any>.toIntakeEntry(): IntakeEntry {
    val type = this["type"] as? String ?: return error("type missing")
    return when (type) {
        "eat" -> EatEntry(
            id = this["id"] as? String ?: "",
            name = this["name"] as? String ?: "",
            fdcId = (this["fdcId"] as? Number)?.toInt(),
            source = this["source"] as? String,
            kcal = this["kcal"].numD(),
            protein = this["protein"].numD(),
            carbs = this["carbs"].numD(),
            fats = this["fats"].numD(),
            cholesterol = this["cholesterol"].numD(),
            date = this["date"] as? String ?: "",
            cost = when (val v = this["cost"]) {
                is BigDecimal -> v
                is Number -> BigDecimal.valueOf(v.toDouble())
                is String -> v.toBigDecimalOrNull()
                else -> null
            },
            currency = this["currency"] as? String,
            category = this["category"] as? String,
            mealId = this["mealId"] as? String,
            sourcePlanId = this["sourcePlanId"] as? String,
        )
        "drink" -> {
            // 兼容：优先读取 sugarGrams；若不存在再回落读取 carbs（旧数据）
            val sugar = (this["sugarGrams"] as? Number)?.toDouble()
                ?: (this["carbs"] as? Number)?.toDouble()
                ?: 0.0

            DrinkEntry(
                id = this["id"] as? String ?: "",
                name = this["name"] as? String ?: "",
                kcal = this["kcal"].numD(),
                sugarGrams = sugar,
                caffeine = this["caffeine"].numD(),
                waterML = this["waterML"].numD(),
                alcoholVolumeML = this["alcoholVolumeML"].numDOrNull(),
                alcoholPercent = (this["alcoholPercent"] as? Number)?.toFloat(),
                date = this["date"] as? String ?: "",
                timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                cost = when (val v = this["cost"]) {
                    is BigDecimal -> v
                    is Number -> BigDecimal.valueOf(v.toDouble())
                    is String -> v.toBigDecimalOrNull()
                    else -> null
                },
                currency = this["currency"] as? String,
                category = null,
                sourcePlanId = this["sourcePlanId"] as? String,
                mealId = this["mealId"] as? String,

                )
        }
        else -> throw IllegalArgumentException("Unknown type: $type")
    }
}
fun RecipeEntryDTO.toRecipeEntry(): RecipeEntry {
    val v2 = tagsV2
    return RecipeEntry(
        id = id, name = name, imageUrl = imageUrl,
        kcalPerServing = kcal, protein = protein, carbs = carbs, fats = fats, tagsV2 = v2,                            // ⬅️ 新增
        timeRequired = timeRequired, ingredients = ingredients, steps = steps,
        ingredientsText = ingredientsText, description = description,currency=currency
    )
}
/** 可配置的净含水惩罚系数（默认 0：不开启惩罚） */
data class HydrationPenalty(
    val caffeineMlPerMg: Double = 0.0,   // 每 1 mg 咖啡因，扣多少 ml 含水
    val alcoholMlPerGram: Double = 0.0   // 每 1 g 酒精，扣多少 ml 含水
)

object HydrationCalc {
    /** 单杯标准杯（std drinks）：按 14g 乙醇为 1 标准杯 */
    fun stdDrinksFor(entry: DrinkEntry): Double {
        val vol = (entry.alcoholVolumeML ?: 0.0)
        val abv = (entry.alcoholPercent ?: 0f).toDouble()
        val grams = alcoholGramFromVolume(vol, abv)
        return grams / 14.0
    }

    /** 全日净含水（ml）：水量 * 含水系数 - 咖啡因/酒精惩罚 */
    fun netWater(entries: List<DrinkEntry>, penalty: HydrationPenalty = HydrationPenalty()): Double {
        var water = 0.0
        var cafMg = 0.0
        var alcG = 0.0
        entries.forEach { e ->
            val factor = 1.0 // 这里按记录本身没有 factor；若未来按模板带入，可平滑接入
            water += e.waterML * factor
            cafMg += e.caffeine
            val vol = (e.alcoholVolumeML ?: 0.0)
            val abv = (e.alcoholPercent ?: 0f).toDouble()
            alcG += alcoholGramFromVolume(vol, abv)
        }
        val penaltyMl = cafMg * penalty.caffeineMlPerMg + alcG * penalty.alcoholMlPerGram
        return max(0.0, water - penaltyMl)
    }

    /** 汇总版：若你只拿 totals 来算 */
    fun netWaterFromTotals(
        totalWaterMl: Double,
        totalCaffeineMg: Double,
        totalAlcoholG: Double,
        penalty: HydrationPenalty = HydrationPenalty()
    ): Double {
        val penaltyMl = totalCaffeineMg * penalty.caffeineMlPerMg + totalAlcoholG * penalty.alcoholMlPerGram
        return max(0.0, totalWaterMl - penaltyMl)
    }
}
fun migrateFlatTagsToV2(flat: List<String>): Map<String, List<String>> {
    if (flat.isEmpty()) return emptyMap()
    val buckets = mutableMapOf<String, MutableList<String>>()
    flat.forEach { raw ->
        val (cat, id) = TagCatalog.categorize(raw)
        buckets.getOrPut(cat) { mutableListOf() }.add(id)
    }
    return buckets
}

// 便捷判断
fun RecipeEntry.hasTag(cat: TagCat, id: String): Boolean =
    tagsV2[cat.key]?.contains(id) == true
// 預留：可抽象出可重用規則（Hybrid 的 Rule 表）
data class DiscountRule(
    val id: String,
    val name: String
    // TODO: effect/conditions…（之後再加）
)
@Serializable
data class ResolvedDiscountItem(
    val id: String,
    val name: String,
    val matchedCartItemId: String? = null,
    val originalSubtotal: Double,    // unitPrice * qty 原價小計（若價格未知則為 0）
    val discountedSubtotal: Double,  // 折扣後小計（若沒套用折扣則等於 originalSubtotal）
    val savings: Double,             // originalSubtotal - discountedSubtotal
    val note: String? = null
)
data class ResolvedDiscountEvent(
    val eventId: String,
    val title: String,
    val items: List<CartItem>,
    val totalOriginal: BigDecimal,      // 改為 BigDecimal
    val totalAfterDiscount: BigDecimal, // 改為 BigDecimal
    val totalSavings: BigDecimal,       // 改為 BigDecimal
    val appliedRuleSummary: String?,
    val computedAt: Long
)

class DiscountRepository(
    val eventStore: DiscountEventDataStore
) {
    /** 直接读写 DiscountEvent —— 无任何 DTO/legacy 路径 */
    suspend fun getAll(): List<DiscountEvent> = eventStore.getEvents()

    suspend fun getById(id: String): DiscountEvent? =
        eventStore.getEvents().firstOrNull { it.id == id }

    suspend fun save(event: DiscountEvent) {
        val exists = eventStore.getEvents().any { it.id == event.id }
        if (exists) eventStore.updateEvent(event) else eventStore.addEvent(event)
    }

    suspend fun delete(id: String) = eventStore.deleteEvent(id)

}
private fun DiscountItem.toDto(): DiscountEventItemDTO =
    DiscountEventItemDTO(
        sku = (name.takeIf { it.isNotBlank() } ?: "item-$id").trim(),
        name = name,
        // [Fix] unitPrice 與 qty 已經是 BigDecimal?，直接取用或給預設值
        unitPrice = this.unitPrice ?: BigDecimal.ZERO,
        qty = this.qty ?: BigDecimal.ONE,
        memberPrice = null,
        amountOffEach = null,
        percentOff = null
    )
