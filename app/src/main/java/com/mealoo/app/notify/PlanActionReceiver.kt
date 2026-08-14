package com.mealoo.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mealoo.app.data.IntakeDataStore
import com.mealoo.app.db.MealPlanRepository
import com.mealoo.app.model.EatEntry
import com.mealoo.app.widget.ACTION_WIDGET_REFRESH
import com.mealoo.app.widget.PlanWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val PLAN_ACTION_TAG = "PlanAction"

class PlanActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PLAN_CONFIRM) return
        val planId = intent.getStringExtra(EXTRA_PLAN_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(PLAN_ACTION_TAG, "clicked planId=$planId")
                val appContext = context.applicationContext
                val intakeStore = IntakeDataStore(appContext)
                val planStore = MealPlanRepository.getInstance(appContext)

                if (intakeStore.existsBySourcePlanId(planId)) {
                    Log.d(PLAN_ACTION_TAG, "dedupe hit planId=$planId")
                    return@launch
                }

                val plan = planStore.getById(planId)
                if (plan == null) {
                    Log.w(PLAN_ACTION_TAG, "plan not found planId=$planId")
                    return@launch
                }

                val entry = EatEntry(
                    name = plan.name,
                    kcal = plan.kcal,
                    grams = plan.grams,
                    protein = plan.protein,
                    carbs = plan.carbs,
                    fats = plan.fats,
                    cholesterol = plan.cholesterol,
                    fdcId = plan.fdcId,
                    source = plan.source,
                    category = null,
                    cost = plan.cost,
                    currency = plan.currency,
                    date = plan.date,
                    mealId = plan.mealId,
                    sourcePlanId = plan.id
                )
                intakeStore.addEntry(entry)
                planStore.markCompleted(planId)
                Log.d(PLAN_ACTION_TAG, "write success planId=$planId")

                val refreshIntent = Intent(appContext, PlanWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REFRESH
                }
                appContext.sendBroadcast(refreshIntent)
            } catch (e: Exception) {
                Log.e(PLAN_ACTION_TAG, "action failed planId=$planId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
