package com.mealoo.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.api.services.drive.Drive
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import com.mealoo.app.R
import com.mealoo.app.model.AppCurrency
import kotlinx.serialization.json.Json
import com.mealoo.app.model.DEFAULT_BREAKFAST_ID
import com.mealoo.app.model.DEFAULT_DINNER_ID
import com.mealoo.app.model.DEFAULT_LUNCH_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Serializable
enum class Gender {
    MALE, FEMALE, OTHER
}
@Serializable
enum class Goal {
    MAINTAIN, GAIN, LOSS
}
@Serializable
enum class ActivityLevel {
    LOW, MEDIUM, HIGH
}
@Serializable
data class UserPreference(
    val name: String = "",
    val age: Int = 0,
    val heightCm: Int = 0,
    val weightKg: Float = 0f,
    val gender: Gender = Gender.OTHER,
    val goal: Goal = Goal.MAINTAIN,
    val activityLevel: ActivityLevel = ActivityLevel.MEDIUM,
    val proteinGPerKg: Double = 1.6,
    val fatGPerKg: Double = 0.8,
    val carbsMode: String = "FILL_REST",
    val dailyKcalTarget: Double = 0.0,
    val dailyProteinTarget: Double = 0.0,
    val dailyCarbsTarget: Double = 0.0,
    val dailyFatsTarget: Double = 0.0,
    val dailyWaterMlTarget: Double = 2000.0,
    val dietType: String,
    val sleepHours: Int = 8,
    val breakfastTime: String = "07:30",
    val lunchTime: String = "12:30",
    val dinnerTime: String = "19:00",
    val mealTimesById: Map<String, String> = emptyMap(),
    val hasNightSnack: Boolean = false,
    val trainingDaysPerWeek: Int = 3,
    val trainingTime: String,
    val fastedTraining: Boolean = false,
    val carbFocusMeal: String,
    val snackHabitType: String,
    val snackTimes: List<String> = emptyList(),
    val snackPreferences: List<String> = emptyList(),
    val mealRatioMap: Map<String, Double> = emptyMap(),
    val homeCurrency : AppCurrency
)
class UserPreferenceDataStore(private val context: Context) {

    companion object {
        fun createLocalizedDefaultPreference(context: Context): UserPreference {
            // 1. 新增：依據 Locale 判斷預設幣別
            val locale = Locale.getDefault()
            val defaultCurrency = when (locale.language) {
                "ja" -> AppCurrency.JPY
                "zh" -> if (locale.country.equals("CN", ignoreCase = true)) AppCurrency.CNY else AppCurrency.TWD
                "en" -> if (locale.country.equals("GB", ignoreCase = true)) AppCurrency.GBP else AppCurrency.USD
                "ko" -> AppCurrency.KRW
                else -> AppCurrency.USD // 或是 AppCurrency.OTHER
            }

            return UserPreference(
                dietType = context.getString(R.string.diet_none),
                trainingTime = context.getString(R.string.time_evening),
                carbFocusMeal = context.getString(R.string.meal_lunch),
                snackHabitType = context.getString(R.string.snack_rare),
                mealTimesById = mapOf(
                    DEFAULT_BREAKFAST_ID to "07:30",
                    DEFAULT_LUNCH_ID to "12:30",
                    DEFAULT_DINNER_ID to "19:00"
                ),
                // 2. 寫入預設幣別
                homeCurrency = defaultCurrency
            )
        }
        private val jsonFormatter = Json { ignoreUnknownKeys = true }
        private val preferenceKey = stringPreferencesKey("user_pref")
    }
    /**
     * 保存偏好
     */
    suspend fun savePreference(pref: UserPreference) {
        val json = jsonFormatter.encodeToString(pref)
        context.dataStore.edit { prefs ->
            prefs[preferenceKey] = json
        }
    }
    /**
     * 读取偏好
     */
    suspend fun getPreference(): UserPreference {
        val prefs = context.dataStore.data.first()
        val json = prefs[preferenceKey]
        return if (json != null) {
            try {
                jsonFormatter.decodeFromString(json)
            } catch (e: Exception) {
                createLocalizedDefaultPreference(context)
            }
        } else {
            createLocalizedDefaultPreference(context)
        }
    }
    val hasOnboardedKey = booleanPreferencesKey("has_onboarded")

    suspend fun setOnboarded() {
        context.dataStore.edit { prefs ->
            prefs[hasOnboardedKey] = true
        }
    }
    suspend fun isOnboarded(): Boolean {
        return context.dataStore.data.first()[hasOnboardedKey] ?: false
    }
}
class PreferenceDriveRepository(driveService: Drive) {
    // 初始化 helper
    private val driveHelper = DriveServiceHelper(driveService)

    private val json = Json { ignoreUnknownKeys = true }
    private val fileName = "mealoo_user_prefs.json"

    suspend fun fetchPreference(): UserPreference? = withContext(Dispatchers.IO) {
        try {
            // 1. 搜尋檔案 ID
            val fileId = driveHelper.searchFileId(fileName) ?: return@withContext null

            // 2. 下載內容
            val content = driveHelper.readFileContent(fileId)
            if (content.isBlank()) return@withContext null

            // 3. 解析 JSON
            json.decodeFromString(UserPreference.serializer(), content)
        } catch (e: Exception) {
            Log.e("PrefRepo", "Error fetching prefs", e)
            null
        }
    }

    suspend fun uploadPreference(pref: UserPreference) = withContext(Dispatchers.IO) {
        try {
            val content = json.encodeToString(UserPreference.serializer(), pref)
            driveHelper.createOrUpdateFile(fileName, content)
            true
        } catch (e: Exception) {
            Log.e("PrefRepo", "Error uploading prefs", e)
            false
        }
    }
}