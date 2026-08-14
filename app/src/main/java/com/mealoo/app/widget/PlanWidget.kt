package com.mealoo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mealoo.app.R
import com.mealoo.app.data.MealDefsStore
import com.mealoo.app.db.MealPlanRepository
import com.mealoo.app.model.DEFAULT_BREAKFAST_ID
import com.mealoo.app.model.DEFAULT_DINNER_ID
import com.mealoo.app.model.DEFAULT_LUNCH_ID
import com.mealoo.app.notify.ACTION_PLAN_CONFIRM
import com.mealoo.app.notify.EXTRA_PLAN_ID
import com.mealoo.app.notify.PlanActionReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

// region Widget models
const val ACTION_WIDGET_REFRESH = "com.mealoo.app.ACTION_WIDGET_REFRESH"

enum class WidgetMealStatus {
    DONE, PAST, NOW, UPCOMING
}

data class WidgetMealItem(
    val mealId: String,
    val mealLabel: String,
    val planId: String?,
    val planName: String?,
    val status: WidgetMealStatus
)

// endregion

// region Widget provider
class PlanWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || action == ACTION_WIDGET_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PlanWidgetProvider::class.java)
            val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(component)
            widgetIds.forEach { widgetId ->
                renderWidget(context, appWidgetManager, widgetId)
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            renderWidget(context, appWidgetManager, widgetId)
        }
    }
}
// endregion

// region Widget action receiver
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_REFRESH) return
        val updateIntent = Intent(context, PlanWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_REFRESH
        }
        context.sendBroadcast(updateIntent)
    }
}
// endregion

// region Widget rendering
fun renderWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.plan_widget)
    views.removeAllViews(R.id.plan_widget_list)

    loadTodayPlans(context).forEach { item ->
        val row = RemoteViews(context.packageName, R.layout.plan_widget_row)
        val mealLabel = item.mealLabel.ifBlank { context.getString(R.string.meal) }
        val planLabel = item.planName?.takeIf { it.isNotBlank() } ?: "—"
        row.setTextViewText(R.id.plan_meal_label, mealLabel)
        row.setTextViewText(R.id.plan_name, planLabel)
        when (item.status) {
            WidgetMealStatus.DONE,
            WidgetMealStatus.PAST -> {
                row.setBoolean(R.id.plan_done, "setEnabled", false)
                // 視覺上讓已完成的變淡，增加 "Done" 的成就感
                row.setFloat(R.id.plan_meal_label, "setAlpha", 0.5f)
                row.setFloat(R.id.plan_name, "setAlpha", 0.5f)
                // 移除舊的監聽器以防萬一
                row.setOnClickPendingIntent(R.id.plan_done, null)
            }

            WidgetMealStatus.NOW,
            WidgetMealStatus.UPCOMING -> {
                if (item.planId != null) {
                    // 1. 建立指向 Receiver 的 Intent
                    val confirmIntent = Intent(context, PlanActionReceiver::class.java).apply {
                        action = ACTION_PLAN_CONFIRM
                        putExtra(EXTRA_PLAN_ID, item.planId)
                        // 雖然是顯式 Intent，但加上 Package 有時能避免少數系統的安全性過濾
                        setPackage(context.packageName)
                    }

                    // 2. 產生唯一的 RequestCode (關鍵！)
                    // 使用 planId 的 hashCode 確保每個按鈕對應正確的 Plan
                    val uniqueRequestCode = item.planId.hashCode()

                    // 3. 建立 PendingIntent
                    val confirmPending = PendingIntent.getBroadcast(
                        context,
                        uniqueRequestCode,
                        confirmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // 4. 綁定到 UI 按鈕 (假設 R.id.plan_done 是那個勾選按鈕)
                    row.setBoolean(R.id.plan_done, "setEnabled", true)
                    row.setOnClickPendingIntent(R.id.plan_done, confirmPending)

                    // 恢復透明度
                    row.setFloat(R.id.plan_meal_label, "setAlpha", 1.0f)
                    row.setFloat(R.id.plan_name, "setAlpha", 1.0f)

                } else {
                    // 有餐段定義但沒有計畫 (No Plan)
                    row.setBoolean(R.id.plan_done, "setEnabled", false)
                    row.setOnClickPendingIntent(R.id.plan_done, null)
                }
            }
        }

        views.addView(R.id.plan_widget_list, row)
    }

    val refreshIntent = Intent(context, WidgetActionReceiver::class.java).apply {
        action = ACTION_WIDGET_REFRESH
    }
    val refreshPending = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.plan_widget_refresh, refreshPending)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
// endregion
private const val NOW_WINDOW_MIN = 60
private const val GRACE_MIN = 30

private fun parseHmToMin(hm: String?): Int? {
    if (hm == null || !hm.contains(":")) return null
    val parts = hm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return h * 60 + m
}

private fun nowMin(): Int {
    val cal = java.util.Calendar.getInstance()
    return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            cal.get(java.util.Calendar.MINUTE)
}
// region Widget data
fun loadTodayPlans(context: Context): List<WidgetMealItem> = runBlocking(Dispatchers.IO) {
    val defsStore = MealDefsStore(context)
    val mealDefs = runCatching { defsStore.getAll() }.getOrElse { emptyList() }
    val mealIds = if (mealDefs.isNotEmpty()) {
        mealDefs.map { it.id }
    } else {
        listOf(DEFAULT_BREAKFAST_ID, DEFAULT_LUNCH_ID, DEFAULT_DINNER_ID)
    }
    val defsById = mealDefs.associateBy { it.id }
    val planStore = MealPlanRepository.getInstance(context)

    mealIds.map { mealId ->
        val label = defsById[mealId]?.name ?: mealId
        val planAny = planStore.getTodayPlanByMealId(mealId)
        val planPending = planStore.getTodayPendingPlanByMealId(mealId)
        // meal time（先硬编码 fallback，后面可接 userPref）
        val mealTime = when (mealId) {
            DEFAULT_BREAKFAST_ID -> "07:30"
            DEFAULT_LUNCH_ID -> "12:00"
            DEFAULT_DINNER_ID -> "18:30"
            else -> null
        }

        val now = nowMin()
        val mealMin = parseHmToMin(mealTime)

        val status = when {
            planAny != null && planAny.isCompleted -> WidgetMealStatus.DONE

            mealMin != null && kotlin.math.abs(now - mealMin) <= NOW_WINDOW_MIN ->
                WidgetMealStatus.NOW

            mealMin != null && now > mealMin + GRACE_MIN ->
                WidgetMealStatus.PAST

            else -> WidgetMealStatus.UPCOMING
        }

        WidgetMealItem(
            mealId = mealId,
            mealLabel = label,
            planId = planPending?.id,   // 只给 pending 的 id
            planName = planPending?.name,
            status = status
        )

    }
}
// endregion
