package com.mealoo.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.drinkTemplateStore by preferencesDataStore(name = "drink_templates")

@Serializable
data class DrinkTemplate(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val waterMl: Double,
    val caffeineMg: Double? = null,
    val abvPercent: Double? = null,
    val lastUsed: Long? = null,
    // 预留：对某类饮品的含水修正（例如 0.9 表示 90% 可计入水）
    val hydrationFactor: Double = 1.0
)

class DrinkTemplateStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("templates_json")

    val templatesFlow: Flow<List<DrinkTemplate>> =
        context.drinkTemplateStore.data.map { prefs ->
            prefs[key]?.let { runCatching { json.decodeFromString<List<DrinkTemplate>>(it) }.getOrNull() }
                ?: emptyList()
        }

    suspend fun getAll(): List<DrinkTemplate> {
        val prefs = context.drinkTemplateStore.data.first()
        val raw = prefs[key] ?: return emptyList()
        return runCatching { json.decodeFromString<List<DrinkTemplate>>(raw) }
            .getOrDefault(emptyList())
    }

    private suspend fun saveAll(list: List<DrinkTemplate>) {
        val value = json.encodeToString(list)
        context.drinkTemplateStore.edit { it[key] = value }
    }

    suspend fun upsert(t: DrinkTemplate) {
        val cur = getAll().toMutableList()
        val idx = cur.indexOfFirst { it.id == t.id }
        if (idx >= 0) cur[idx] = t else cur.add(0, t) // 新的靠前
        saveAll(cur)
    }

    suspend fun remove(id: String) {
        val cur = getAll().filterNot { it.id == id }
        saveAll(cur)
    }
}
