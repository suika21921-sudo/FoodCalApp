package com.mealoo.app.util

import com.mealoo.app.data.UserPreference
import com.mealoo.app.model.FoodItem
import com.mealoo.app.model.IntakeStats
import com.mealoo.app.model.RecipeEntry
import com.mealoo.app.model.NutrientKeys
import com.mealoo.app.utils.dailyMacroTargets
import com.mealoo.app.utils.mealMacroTargets
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

fun FoodItem.toStandardPer100g(): Map<String, Double> =
    buildStandardNutrientMap(this.nutrients)

fun RecipeEntry.toStandardPer100gOrNull(): Map<String, Double>? {
    if (totalWeightGram <= 0.0) return null
    val base = mapOf(
        "Energy" to (kcalPerServing / totalWeightGram * 100.0),
        "Protein" to (protein / totalWeightGram * 100.0),
        "Carbohydrate, by difference" to (carbs / totalWeightGram * 100.0),
        "Total lipid (fat)" to (fats / totalWeightGram * 100.0)
    )
    return NutrientKeys.STANDARD_KEYS.associate { key ->
        key to (base[key] ?: 0.0)
    }
}

data class NutritionScoreDetail(
    val total: Int,
    val energyDensityScore: Int,
    val proteinScore: Int,
    val fatScore: Int,
    val fiberScore: Int,
    val sodiumScore: Int,
    val notes: List<String> = emptyList()
) {
    val grade: String
        get() = when {
            total >= 85 -> "A"
            total >= 70 -> "B"
            total >= 55 -> "C"
            total >= 40 -> "D"
            else -> "E"
        }
}

data class ScoringConfig(
    val energyTarget: Double = 150.0,
    val energyTol: Double = 100.0,
    val proteinTarget: Double = 0.20,
    val proteinBaseScore: Int = 90,
    val fatTarget: Double = 0.25,
    val fatTol: Double = 0.15,
    val fiberTarget: Double = 3.0,
    val fiberBaseScore: Int = 90,
    val sodiumLow: Double = 120.0,
    val sodiumHigh: Double = 600.0
) {
    companion object {
        val DEFAULT = ScoringConfig()
        val VEGETABLES = ScoringConfig(
            energyTarget = 30.0,
            energyTol = 100.0,
            proteinTarget = 0.05,
            proteinBaseScore = 80,
            fatTarget = 0.0,
            fatTol = 0.20,
            fiberTarget = 2.5,
            fiberBaseScore = 95
        )
        val HIGH_PROTEIN = ScoringConfig(
            energyTarget = 180.0,
            energyTol = 120.0,
            proteinTarget = 0.40,
            proteinBaseScore = 95,
            fatTarget = 0.30,
            fatTol = 0.25,
            fiberTarget = 0.5,
            fiberBaseScore = 80
        )
        val GRAINS = ScoringConfig(
            energyTarget = 160.0,
            energyTol = 80.0,
            proteinTarget = 0.08,
            proteinBaseScore = 85,
            fatTarget = 0.05,
            fatTol = 0.10,
            fiberTarget = 2.0,
            fiberBaseScore = 90
        )
        val FRUITS = ScoringConfig(
            energyTarget = 60.0,
            energyTol = 60.0,
            proteinTarget = 0.0,
            proteinBaseScore = 100,
            fatTarget = 0.0,
            fatTol = 0.10,
            fiberTarget = 1.5,
            fiberBaseScore = 90
        )
        val FATS = ScoringConfig(
            energyTarget = 880.0,
            energyTol = 100.0,
            fatTarget = 1.0,
            fatTol = 0.1
        )
    }
}

fun FoodItem.getScoringConfig(): ScoringConfig {
    return when (this.categoryEn) {
        "Vegetables", "Mushrooms", "Seaweeds" -> ScoringConfig.VEGETABLES
        "Meats", "Fish and Shellfish", "Eggs", "Legumes", "Dairy Products" -> ScoringConfig.HIGH_PROTEIN
        "Cereals", "Potatoes and Starches", "Sugars and Sweeteners" -> ScoringConfig.GRAINS
        "Fruits" -> ScoringConfig.FRUITS
        "Fats and Oils" -> ScoringConfig.FATS
        "Confectioneries", "Snacks" -> ScoringConfig.DEFAULT
        else -> ScoringConfig.DEFAULT
    }
}

object NutritionScorer {

    fun scorePer100g(
        per100g: Map<String, Double>,
        config: ScoringConfig = ScoringConfig.DEFAULT
    ): NutritionScoreDetail {
        val kcal = per100g["Energy"] ?: 0.0
        val protein = per100g["Protein"] ?: 0.0
        val carbs = per100g["Available carbohydrate"]
            ?: per100g["Carbohydrate, by difference"] ?: 0.0
        val fat = per100g["Total lipid (fat)"] ?: 0.0
        val fiber = per100g["Fiber, total dietary"] ?: 0.0
        val sodium = per100g["Sodium, Na"] ?: 0.0

        val totalMacroKcal = max(protein * 4.0 + carbs * 4.0 + fat * 9.0, 1.0)
        val proteinRatio = protein * 4.0 / totalMacroKcal
        val fatRatio = fat * 9.0 / totalMacroKcal

        val energyScore = scoreRange(kcal, config.energyTarget, config.energyTol)
        val proteinScore = scoreDiminishingReturns(
            actual = proteinRatio,
            threshold = config.proteinTarget,
            baseScore = config.proteinBaseScore
        )
        val fatScore = scoreRange(fatRatio, config.fatTarget, config.fatTol)
        val fiberScore = scoreDiminishingReturns(
            actual = fiber,
            threshold = config.fiberTarget,
            baseScore = config.fiberBaseScore
        )
        val sodiumScore = scoreSodium(sodium, kcal, config.sodiumLow, config.sodiumHigh)

        val weights = mapOf(
            "energy" to 0.15,
            "protein" to 0.30,
            "fat" to 0.15,
            "fiber" to 0.25,
            "sodium" to 0.15
        )

        val total = (
                energyScore * weights.getValue("energy") +
                        proteinScore * weights.getValue("protein") +
                        fatScore * weights.getValue("fat") +
                        fiberScore * weights.getValue("fiber") +
                        sodiumScore * weights.getValue("sodium")
                ).roundToInt()

        val notes = buildList {
            if (proteinRatio >= 0.25) add("High protein")
            if (fiber >= 3.0) add("High fiber")
            if (sodiumScore < 50) add("High sodium")
            if (fatRatio < 0.10) add("Low fat")
        }

        return NutritionScoreDetail(
            total = clamp(total),
            energyDensityScore = energyScore,
            proteinScore = proteinScore,
            fatScore = fatScore,
            fiberScore = fiberScore,
            sodiumScore = sodiumScore,
            notes = notes
        )
    }

    fun score(food: FoodItem): NutritionScoreDetail {
        return scorePer100g(food.toStandardPer100g(), food.getScoringConfig())
    }

    fun score(recipe: RecipeEntry): NutritionScoreDetail? {
        val per100g = recipe.toStandardPer100gOrNull() ?: return null
        return scorePer100g(per100g, ScoringConfig.DEFAULT)
    }

    private fun scoreDiminishingReturns(actual: Double, threshold: Double, baseScore: Int): Int {
        if (threshold <= 0.001) return 100
        if (actual < 0) return 0

        return if (actual < threshold) {
            ((actual / threshold) * baseScore).roundToInt()
        } else {
            val extra = actual - threshold
            val remainingRoom = 100.0 - baseScore
            val k = threshold * 0.5

            val bonus = remainingRoom * (extra / (extra + k))
            (baseScore + bonus).roundToInt()
        }
    }

    private fun scoreRange(actual: Double, target: Double, tol: Double): Int {
        val diff = abs(actual - target)
        if (diff <= tol) return 100

        val overflow = diff - tol
        val penalty = (overflow / tol) * 50.0

        return clamp(100.0 - penalty)
    }

    private fun scoreSodium(sodiumMg: Double, kcal: Double, low: Double, high: Double): Int {
        if (sodiumMg <= 0.0) return 100
        val effectiveKcal = max(kcal, 50.0)

        val mgPer100kcal = sodiumMg / effectiveKcal * 100.0
        return when {
            mgPer100kcal <= low -> 100
            mgPer100kcal >= high -> 0
            else -> {
                val ratio = (high - mgPer100kcal) / (high - low)
                clamp(ratio * 100.0)
            }
        }
    }

    private fun clamp(v: Double): Int = v.roundToInt().coerceIn(0, 100)
    private fun clamp(v: Int): Int = v.coerceIn(0, 100)

    fun scoreDailyIntake(
        stats: IntakeStats,
        userPref: UserPreference?
    ): IntakeMacroScoreDetail? {
        if (userPref == null) return null
        val dailyTargets = dailyMacroTargets(
            weightKg = userPref.weightKg.toDouble(),
            dailyKcalTarget = userPref.dailyKcalTarget,
            proteinGPerKg = userPref.proteinGPerKg,
            fatGPerKg = userPref.fatGPerKg
        )
        val targetKcal = dailyTargets.kcal

        return scoreMacroIntake(
            actualProteinG = stats.totalProtein,
            actualCarbsG = stats.totalCarbs,
            actualFatsG = stats.totalFats,
            actualKcal = stats.totalKcal,
            targetProteinG = dailyTargets.proteinG,
            targetCarbsG = dailyTargets.carbsG,
            targetFatsG = dailyTargets.fatG,
            targetKcal = targetKcal,
            label = "Today"
        )
    }

    fun scoreMealIntake(
        mealId: String,
        mealLabel: String,
        mealKcal: Double,
        mealProtein: Double,
        mealCarbs: Double,
        mealFats: Double,
        userPref: UserPreference?,
        overrideMealRatioPercent: Double? = null
    ): IntakeMacroScoreDetail? {
        if (userPref == null) return null

        val mealIds = userPref.mealRatioMap.keys
        val fallbackRatio = if (mealIds.isNotEmpty()) 100.0 / mealIds.size else 0.0
        val mealRatio = overrideMealRatioPercent
            ?: userPref.mealRatioMap[mealId]
            ?: fallbackRatio

        if (mealRatio <= 0.0) return null

        val dailyTargets = dailyMacroTargets(
            weightKg = userPref.weightKg.toDouble(),
            dailyKcalTarget = userPref.dailyKcalTarget,
            proteinGPerKg = userPref.proteinGPerKg,
            fatGPerKg = userPref.fatGPerKg
        )
        val mealTargets = mealMacroTargets(dailyTargets, mealRatio)

        return scoreMacroIntake(
            actualProteinG = mealProtein,
            actualCarbsG = mealCarbs,
            actualFatsG = mealFats,
            actualKcal = mealKcal,
            targetProteinG = mealTargets.proteinG,
            targetCarbsG = mealTargets.carbsG,
            targetFatsG = mealTargets.fatG,
            targetKcal = mealTargets.kcal,
            label = mealLabel
        )
    }

    private fun scoreQuantityByTarget(
        actual: Double,
        target: Double,
        tolRatio: Double
    ): Int {
        if (target <= 0.0) return 50
        val diffRatio = kotlin.math.abs(actual - target) / target
        val raw = 100.0 - (diffRatio / tolRatio) * 100.0
        return clamp(raw)
    }

    private fun scoreMacroIntake(
        actualProteinG: Double,
        actualCarbsG: Double,
        actualFatsG: Double,
        actualKcal: Double,
        targetProteinG: Double,
        targetCarbsG: Double,
        targetFatsG: Double,
        targetKcal: Double,
        label: String
    ): IntakeMacroScoreDetail {
        val kcalScore = scoreQuantityByTarget(
            actual = actualKcal,
            target = targetKcal,
            tolRatio = 0.4
        )
        val proteinScore = scoreQuantityByTarget(
            actual = actualProteinG,
            target = targetProteinG,
            tolRatio = 0.5
        )
        val carbScore = scoreQuantityByTarget(
            actual = actualCarbsG,
            target = targetCarbsG,
            tolRatio = 0.5
        )
        val fatScore = scoreQuantityByTarget(
            actual = actualFatsG,
            target = targetFatsG,
            tolRatio = 0.6
        )

        val weights = mapOf(
            "kcal" to 0.4,
            "protein" to 0.25,
            "carb" to 0.2,
            "fat" to 0.15
        )

        val total = (
                kcalScore * weights.getValue("kcal") +
                        proteinScore * weights.getValue("protein") +
                        carbScore * weights.getValue("carb") +
                        fatScore * weights.getValue("fat")
                ).roundToInt()

        val notes = buildList {
            if (targetKcal > 0.0) {
                val low = targetKcal * 0.85
                val high = targetKcal * 1.15
                when {
                    actualKcal < low  -> add("$label kcal low")
                    actualKcal > high -> add("$label kcal high")
                }
            }
            if (targetProteinG > 0.0) {
                val ratio = actualProteinG / targetProteinG
                when {
                    ratio < 0.7 -> add("$label protein low")
                    ratio < 0.9 -> add("$label protein slightly low")
                    ratio > 1.4 -> add("$label protein high")
                }
            }
            if (targetCarbsG > 0.0) {
                val ratio = actualCarbsG / targetCarbsG
                when {
                    ratio < 0.7 -> add("$label carbs low")
                    ratio > 1.4 -> add("$label carbs high")
                }
            }
            if (targetFatsG > 0.0) {
                val ratio = actualFatsG / targetFatsG
                when {
                    ratio < 0.5 -> add("$label fat low")
                    ratio > 1.5 -> add("$label fat high")
                }
            }
        }

        return IntakeMacroScoreDetail(
            total = clamp(total),
            kcalScore = clamp(kcalScore),
            proteinScore = clamp(proteinScore),
            carbScore = clamp(carbScore),
            fatScore = clamp(fatScore),
            notes = notes
        )
    }
}

data class IntakeMacroScoreDetail(
    val total: Int,
    val kcalScore: Int,
    val proteinScore: Int,
    val carbScore: Int,
    val fatScore: Int,
    val notes: List<String> = emptyList()
) {
    val grade: String
        get() = when {
            total >= 85 -> "A"
            total >= 70 -> "B"
            total >= 55 -> "C"
            total >= 40 -> "D"
            else -> "E"
        }
}

enum class ContextLevel { PRIMARY, SECONDARY, WARNING }

data class FoodContext(
    val id: String,
    val level: ContextLevel,
    val icon: String,
    val title: String,
    val description: String
)

fun buildFoodContexts(
    per100g: Map<String, Double>,
    detail: NutritionScoreDetail
): List<FoodContext> {

    fun get(key: String): Double = per100g[key] ?: 0.0

    val kcal = get("Energy")
    val protein = get("Protein")
    val carbs = get("Available carbohydrate").takeIf { it > 0 } ?: get("Carbohydrate, by difference")
    val fat = get("Total lipid (fat)")
    val fiber = get("Fiber, total dietary")
    val sodium = get("Sodium, Na")

    val out = mutableListOf<FoodContext>()

    if (kcal >= 300 || detail.energyDensityScore <= 55) {
        out += FoodContext(
            id = "ENERGY_DENSE_WARNING",
            level = ContextLevel.WARNING,
            icon = "!",
            title = "Energy dense",
            description = "High kcal per 100g; consider smaller portions or add vegetables."
        )
    }

    if (detail.sodiumScore < 50) {
        out += FoodContext(
            id = "HIGH_SODIUM_WARNING",
            level = ContextLevel.WARNING,
            icon = "!",
            title = "High sodium",
            description = "Sodium density is high; balance with lower-sodium foods."
        )
    }

    val isCarbUp = (kcal >= 250 && carbs >= 50) || (detail.energyDensityScore <= 60 && carbs >= 40)
    if (isCarbUp) {
        out += FoodContext(
            id = "CARB_UP",
            level = ContextLevel.PRIMARY,
            icon = "C",
            title = "Carb boost",
            description = "Good for training days or pre/post-workout energy."
        )
    }

    val isMuscle = (detail.proteinScore >= 75) || (protein >= 20)
    if (isMuscle) {
        out += FoodContext(
            id = "MUSCLE_GAIN_FRIENDLY",
            level = if (isCarbUp) ContextLevel.SECONDARY else ContextLevel.PRIMARY,
            icon = "M",
            title = "Muscle gain friendly",
            description = "Higher protein; still balance with carbs and total intake."
        )
    }

    val isCut = (kcal <= 150 && fiber >= 2.5) || (detail.energyDensityScore >= 85 && detail.fiberScore >= 80)
    if (isCut) {
        out += FoodContext(
            id = "WEIGHT_CONTROL",
            level = if (isCarbUp || isMuscle) ContextLevel.SECONDARY else ContextLevel.PRIMARY,
            icon = "W",
            title = "Weight control",
            description = "Lower energy density with fiber helps satiety."
        )
    }

    if (detail.sodiumScore >= 90) {
        out += FoodContext(
            id = "LOW_SODIUM",
            level = ContextLevel.SECONDARY,
            icon = "S",
            title = "Low sodium",
            description = "Lower sodium density; heart-friendly choice."
        )
    }

    if (fiber >= 3.0 || detail.fiberScore >= 90) {
        out += FoodContext(
            id = "HIGH_FIBER",
            level = ContextLevel.SECONDARY,
            icon = "F",
            title = "High fiber",
            description = "Supports satiety and gut health."
        )
    }

    val hasPrimary = out.any { it.level == ContextLevel.PRIMARY }
    val isDaily =
        (kcal in 80.0..250.0) &&
                detail.sodiumScore >= 80 &&
                detail.fiberScore >= 60 &&
                detail.proteinScore >= 55

    if (!hasPrimary && isDaily) {
        out += FoodContext(
            id = "DAILY_BALANCED",
            level = ContextLevel.PRIMARY,
            icon = "B",
            title = "Daily balanced",
            description = "Balanced profile for everyday meals."
        )
    }

    return out
        .sortedWith(compareBy<FoodContext> {
            when (it.level) {
                ContextLevel.PRIMARY -> 0
                ContextLevel.SECONDARY -> 1
                ContextLevel.WARNING -> 2
            }
        })
        .distinctBy { it.id }
        .take(3)
}
