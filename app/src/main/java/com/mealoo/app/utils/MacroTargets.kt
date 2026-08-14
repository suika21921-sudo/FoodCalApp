package com.mealoo.app.utils

import kotlin.math.max

data class MacroTargets(
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val kcal: Double
)

fun dailyMacroTargets(
    weightKg: Double,
    dailyKcalTarget: Double,
    proteinGPerKg: Double,
    fatGPerKg: Double
): MacroTargets {
    val proteinG = weightKg * proteinGPerKg
    val fatG = weightKg * fatGPerKg
    val carbsKcal = max(0.0, dailyKcalTarget - (proteinG * 4.0) - (fatG * 9.0))
    val carbsG = carbsKcal / 4.0
    val kcal = (proteinG * 4.0) + (carbsG * 4.0) + (fatG * 9.0)
    return MacroTargets(
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        kcal = kcal
    )
}

fun mealMacroTargets(
    dailyTargets: MacroTargets,
    mealRatioPct: Double
): MacroTargets {
    val ratio = mealRatioPct / 100.0
    val proteinG = dailyTargets.proteinG * ratio
    val carbsG = dailyTargets.carbsG * ratio
    val fatG = dailyTargets.fatG * ratio
    val kcal = (proteinG * 4.0) + (carbsG * 4.0) + (fatG * 9.0)
    return MacroTargets(
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        kcal = kcal
    )
}
