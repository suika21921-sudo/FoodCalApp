package com.mealoo.app.util

import com.mealoo.app.R
import com.mealoo.app.data.ActivityLevel
import com.mealoo.app.data.Goal
import com.mealoo.app.data.PromptKind
import com.mealoo.app.data.PromptVariable
import com.mealoo.app.data.SavedPrompt
import com.mealoo.app.data.UserPreference
import com.mealoo.app.model.InventoryItem
import com.mealoo.app.utils.dailyMacroTargets
import java.time.LocalDate
import java.util.Locale

sealed class PromptInputRequirement {
    data object Servings : PromptInputRequirement()
    data object ExtraNote : PromptInputRequirement()
    data object InventoryBlock : PromptInputRequirement()
    data object PreferenceContext : PromptInputRequirement()
}
fun parsePromptRequirements(template: String): Set<PromptInputRequirement> {
    val result = mutableSetOf<PromptInputRequirement>()

    if (template.contains("{{SERVINGS}}")) {
        result += PromptInputRequirement.Servings
    }
    if (template.contains("{{EXTRA_NOTE}}")) {
        result += PromptInputRequirement.ExtraNote
    }
    if (template.contains("{{INVENTORY_BLOCK}}")) {
        result += PromptInputRequirement.InventoryBlock
    }
    if (
        template.contains("{{MACRO_PREF_BLOCK}}") ||
        template.contains("{{LIFESTYLE_BLOCK}}") ||
        template.contains("{{GOAL}}") ||
        template.contains("{{ACTIVITY_LEVEL}}") ||
        template.contains("{{DIET_TYPE}}") ||
        template.contains("{{MEAL_RATIO_MAP}}")
    ) {
        result += PromptInputRequirement.PreferenceContext
    }

    return result
}

fun resolvePromptRequirements(prompt: SavedPrompt): Set<PromptInputRequirement> {
    if (prompt.variables.isEmpty()) return parsePromptRequirements(prompt.body)

    val result = mutableSetOf<PromptInputRequirement>()
    val vars = prompt.variables

    if (vars.contains(PromptVariable.SERVINGS)) result += PromptInputRequirement.Servings
    if (vars.contains(PromptVariable.EXTRA_NOTE)) result += PromptInputRequirement.ExtraNote
    if (vars.contains(PromptVariable.INVENTORY_BLOCK)) result += PromptInputRequirement.InventoryBlock

    val needsPrefContext = vars.any {
        it == PromptVariable.MACRO_PREF_BLOCK ||
                it == PromptVariable.LIFESTYLE_BLOCK ||
                it == PromptVariable.GOAL ||
                it == PromptVariable.ACTIVITY_LEVEL ||
                it == PromptVariable.DIET_TYPE ||
                it == PromptVariable.MEAL_RATIO_MAP ||
                it == PromptVariable.WEIGHT_KG ||
                it == PromptVariable.HEIGHT_CM ||
                it == PromptVariable.AGE ||
                it == PromptVariable.DAILY_KCAL_TARGET ||
                it == PromptVariable.TODAY_DATE
    }
    if (needsPrefContext) result += PromptInputRequirement.PreferenceContext

    return result
}

object DefaultPrompts {

    fun getAllDefaults(context: android.content.Context): List<SavedPrompt> {

        // ── 原有 5 個 ────────────────────────────────────────────────────────
        val inventoryToRecipe = SavedPrompt(
            id = "preset_inventory_recipe",
            title = context.getString(R.string.prompt_inventory_to_recipe_title),
            kind = PromptKind.INVENTORY_RECIPE,
            variables = setOf(PromptVariable.INVENTORY_BLOCK, PromptVariable.SERVINGS, PromptVariable.MACRO_PREF_BLOCK),
            body = context.getString(R.string.prompt_inventory_to_recipe_body)
        )

        val analyzeMealJson = SavedPrompt(
            id = "preset_analyze_meal_json",
            title = context.getString(R.string.prompt_analyze_meal_json_title),
            kind = PromptKind.GENERIC,
            variables = setOf(PromptVariable.EXTRA_NOTE),
            body = context.getString(R.string.prompt_analyze_meal_json_body)
        )

        val analyzeDrink = SavedPrompt(
            id = "preset_analyze_drink",
            title = context.getString(R.string.prompt_analyze_drink_title),
            kind = PromptKind.GENERIC,
            variables = setOf(PromptVariable.EXTRA_NOTE),
            body = context.getString(R.string.prompt_analyze_drink_body)
        )

        val inventoryWeeklyPlan = SavedPrompt(
            id = "preset_inventory_weekly",
            title = context.getString(R.string.prompt_inventory_weekly_plan_title),
            kind = PromptKind.INVENTORY_WEEKLY_PLAN,
            variables = setOf(PromptVariable.INVENTORY_BLOCK, PromptVariable.LIFESTYLE_BLOCK),
            body = context.getString(R.string.prompt_inventory_weekly_plan_body)
        )

        val structuralRecipe = SavedPrompt(
            id = "preset_structural_recipe",
            title = context.getString(R.string.prompt_structural_recipe_title),
            kind = PromptKind.INVENTORY_RECIPE,
            variables = setOf(PromptVariable.INVENTORY_BLOCK, PromptVariable.EXTRA_NOTE),
            body = context.getString(R.string.prompt_structural_recipe_body)
        )

        // ── 新增 7 個 ────────────────────────────────────────────────────────
        val fatLossGroceries = SavedPrompt(
            id = "preset_fat_loss_groceries",
            title = context.getString(R.string.prompt_fat_loss_groceries_title),
            kind = PromptKind.INVENTORY_SHOPPING_LIST,
            variables = setOf(
                PromptVariable.LIFESTYLE_BLOCK,
                PromptVariable.MACRO_PREF_BLOCK,
                PromptVariable.INVENTORY_BLOCK
            ),
            body = context.getString(R.string.prompt_fat_loss_groceries_body)
        )

        val mealPrepSunday = SavedPrompt(
            id = "preset_meal_prep_sunday",
            title = context.getString(R.string.prompt_meal_prep_sunday_title),
            kind = PromptKind.INVENTORY_WEEKLY_PLAN,
            variables = setOf(
                PromptVariable.INVENTORY_BLOCK,
                PromptVariable.SERVINGS,
                PromptVariable.GOAL
            ),
            body = context.getString(R.string.prompt_meal_prep_sunday_body)
        )

        val nutritionCoach = SavedPrompt(
            id = "preset_nutrition_coach",
            title = context.getString(R.string.prompt_nutrition_coach_title),
            kind = PromptKind.GENERIC,
            variables = setOf(
                PromptVariable.LIFESTYLE_BLOCK,
                PromptVariable.MACRO_PREF_BLOCK,
                PromptVariable.EXTRA_NOTE
            ),
            body = context.getString(R.string.prompt_nutrition_coach_body)
        )

        val budgetRecipe = SavedPrompt(
            id = "preset_budget_recipe",
            title = context.getString(R.string.prompt_budget_recipe_title),
            kind = PromptKind.INVENTORY_RECIPE,
            variables = setOf(
                PromptVariable.INVENTORY_BLOCK,
                PromptVariable.SERVINGS,
                PromptVariable.DAILY_KCAL_TARGET,
                PromptVariable.EXTRA_NOTE
            ),
            body = context.getString(R.string.prompt_budget_recipe_body)
        )

        val expiryRescue = SavedPrompt(
            id = "preset_expiry_rescue",
            title = context.getString(R.string.prompt_expiry_rescue_title),
            kind = PromptKind.INVENTORY_RECIPE,
            variables = setOf(
                PromptVariable.INVENTORY_BLOCK,
                PromptVariable.EXTRA_NOTE
            ),
            body = context.getString(R.string.prompt_expiry_rescue_body)
        )

        val bmiHealthReport = SavedPrompt(
            id = "preset_bmi_health_report",
            title = context.getString(R.string.prompt_bmi_health_report_title),
            kind = PromptKind.GENERIC,
            variables = setOf(
                PromptVariable.WEIGHT_KG,
                PromptVariable.HEIGHT_CM,
                PromptVariable.AGE,
                PromptVariable.GOAL,
                PromptVariable.ACTIVITY_LEVEL,
                PromptVariable.DAILY_KCAL_TARGET
            ),
            body = context.getString(R.string.prompt_bmi_health_report_body)
        )

        val dailyMacroAnalysis = SavedPrompt(
            id = "preset_daily_macro_log_analysis",
            title = context.getString(R.string.prompt_daily_macro_analysis_title),
            kind = PromptKind.GENERIC,
            variables = setOf(
                PromptVariable.TODAY_DATE,
                PromptVariable.MACRO_PREF_BLOCK,
                PromptVariable.LIFESTYLE_BLOCK,
                PromptVariable.EXTRA_NOTE
            ),
            body = context.getString(R.string.prompt_daily_macro_analysis_body)
        )

        return listOf(
            inventoryToRecipe,
            analyzeMealJson,
            analyzeDrink,
            inventoryWeeklyPlan,
            structuralRecipe,
            fatLossGroceries,
            mealPrepSunday,
            nutritionCoach,
            budgetRecipe,
            expiryRescue,
            bmiHealthReport,
            dailyMacroAnalysis
        )
    }
}
data class PromptRuntimeInputs(
    val servings: Int = 1,
    val extraNote: String = ""
)

object PromptFillerEngine {

    fun fill(
        template: SavedPrompt,
        inventory: List<InventoryItem>,
        userPref: UserPreference,
        inputs: PromptRuntimeInputs
    ): String {
        var text = template.body

        // 1) Inventory
        if (text.contains("{{${PromptVariable.INVENTORY_BLOCK.name}}}")) {
            val invString = inventoryToPromptBlock(inventory)
            text = text.replace("{{${PromptVariable.INVENTORY_BLOCK.name}}}", invString)
        }

        // 2) Preference blocks
        if (text.contains("{{${PromptVariable.MACRO_PREF_BLOCK.name}}}")) {
            text = text.replace(
                "{{${PromptVariable.MACRO_PREF_BLOCK.name}}}",
                PromptContextBuilder.buildMacroPreferenceBlock(userPref)
            )
        }
        if (text.contains("{{${PromptVariable.LIFESTYLE_BLOCK.name}}}")) {
            text = text.replace(
                "{{${PromptVariable.LIFESTYLE_BLOCK.name}}}",
                PromptContextBuilder.buildLifestyleBlock(userPref)
            )
        }

        // 3) Fine-grained preference variables (for custom templates)
        text = text.replace("{{${PromptVariable.GOAL.name}}}", PromptContextBuilder.goalLabel(userPref.goal))
        text = text.replace("{{${PromptVariable.ACTIVITY_LEVEL.name}}}", PromptContextBuilder.activityLabel(userPref.activityLevel))
        text = text.replace("{{${PromptVariable.DIET_TYPE.name}}}", userPref.dietType)
        text = text.replace(
            "{{${PromptVariable.MEAL_RATIO_MAP.name}}}",
            PromptContextBuilder.formatMealRatioMap(userPref.mealRatioMap) ?: "無設定餐次熱量分配"
        )

        // 4) Runtime inputs
        text = text.replace("{{${PromptVariable.SERVINGS.name}}}", inputs.servings.toString())
        text = text.replace(
            "{{${PromptVariable.EXTRA_NOTE.name}}}",
            inputs.extraNote.ifBlank { "無特殊要求" }
        )

        // 5) New auto-filled variables
        text = text.replace("{{${PromptVariable.TODAY_DATE.name}}}", LocalDate.now().toString())
        text = text.replace("{{${PromptVariable.WEIGHT_KG.name}}}", userPref.weightKg.toInt().toString())
        text = text.replace("{{${PromptVariable.HEIGHT_CM.name}}}", userPref.heightCm.toString())
        text = text.replace("{{${PromptVariable.AGE.name}}}", userPref.age.toString())
        text = text.replace(
            "{{${PromptVariable.DAILY_KCAL_TARGET.name}}}",
            userPref.dailyKcalTarget.toInt().toString()
        )

        return text
    }

    fun inventoryToPromptBlock(inventory: List<InventoryItem>): String {
        if (inventory.isEmpty()) return "目前冰箱沒有庫存。"

        return inventory.joinToString(separator = "\n") { item ->
            val sb = StringBuilder()
            sb.append("- ${item.name} : ${item.quantity} ${item.unit}")

            if (!item.expiryDate.isNullOrEmpty()) {
                sb.append(" (效期: ${item.expiryDate})")
            }

            val nutrients = item.standardNutrientsPer100g
            if (!nutrients.isNullOrEmpty()) {
                sb.append(" | 營養(per 100g/ml): {")
                val nutsStr = nutrients.entries
                    .filter { it.value > 0.1 }
                    .joinToString(", ") { entry -> "${entry.key}: ${entry.value.toInt()}" }
                sb.append(nutsStr)
                sb.append("}")
            }

            sb.toString()
        }
    }
}
object PromptContextBuilder {

    fun buildContext(pref: UserPreference): Map<String, Any> {
        return mapOf(
            "goal" to pref.goal.name,
            "activityLevel" to pref.activityLevel.name,
            "dietType" to pref.dietType,
            "mealRatioMap" to pref.mealRatioMap,
            "proteinGPerKg" to pref.proteinGPerKg,
            "fatGPerKg" to pref.fatGPerKg,
            "carbsMode" to pref.carbsMode
        )
    }

    fun goalLabel(goal: Goal): String = when (goal) {
        Goal.MAINTAIN -> "維持體重"
        Goal.GAIN -> "增肌/增重"
        Goal.LOSS -> "減脂/減重"
    }

    fun activityLabel(level: ActivityLevel): String = when (level) {
        ActivityLevel.LOW -> "低（久坐/少運動）"
        ActivityLevel.MEDIUM -> "中（一般活動）"
        ActivityLevel.HIGH -> "高（高強度/常運動）"
    }

    fun buildLifestyleBlock(pref: UserPreference): String {
        val lines = buildList {
            add("目標：${goalLabel(pref.goal)}")
            add("活動水平：${activityLabel(pref.activityLevel)}")
            add("飲食法：${pref.dietType}")
            add("每週訓練：${pref.trainingDaysPerWeek} 天（${pref.trainingTime}${if (pref.fastedTraining) "，空腹訓練" else ""}）")
            add("睡眠：${pref.sleepHours} 小時")
        }
        return lines.joinToString(separator = "\n")
    }

    fun buildMacroPreferenceBlock(pref: UserPreference): String {
        val dailyTargets = dailyMacroTargets(
            weightKg = pref.weightKg.toDouble(),
            dailyKcalTarget = pref.dailyKcalTarget,
            proteinGPerKg = pref.proteinGPerKg,
            fatGPerKg = pref.fatGPerKg
        )
        val proteinPerKg = String.format(Locale.getDefault(), "%.1f", pref.proteinGPerKg)
        val fatPerKg = String.format(Locale.getDefault(), "%.1f", pref.fatGPerKg)
        val lines = buildList {
            add("Daily kcal target: ${pref.dailyKcalTarget.toInt()} kcal")
            add("Macros: P $proteinPerKg g/kg / F $fatPerKg g/kg / C auto")
            add("Grams target: P ${dailyTargets.proteinG.toInt()}g, C ${dailyTargets.carbsG.toInt()}g, F ${dailyTargets.fatG.toInt()}g")

            formatMealRatioMap(pref.mealRatioMap)?.let { add(it) }
        }

        return lines.joinToString(separator = "\n")
    }

    fun formatMealRatioMap(mealRatioMap: Map<String, Double>): String? {
        if (mealRatioMap.isEmpty()) return null

        val sorted = mealRatioMap.entries.sortedBy { it.key.lowercase(Locale.getDefault()) }
        val body = sorted.joinToString(separator = "\n") { (meal, ratio) ->
            "- $meal：${ratio.formatPercent()}"
        }
        return "餐次熱量分配：\n$body"
    }

}

private fun Double.formatPercent(): String {
    val value = if (this.isFinite()) this else 0.0
    val rounded = if (kotlin.math.abs(value - value.toInt()) < 0.0001) value.toInt().toString() else "%.1f".format(value)
    return "$rounded%"
}
