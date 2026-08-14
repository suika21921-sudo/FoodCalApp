package com.mealoo.app.model

data class IntakeStats(
    val totalKcal: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFats: Double = 0.0,
    val totalSugar: Double = 0.0,
    val totalCaffeine: Double = 0.0,
    val totalWater: Double = 0.0,
    val totalAlcoholGram: Double = 0.0,
    val totalCholesterol: Double = 0.0 // ✅ 新增字段
)

data class MealMacroSummary(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fats: Double
)

fun calculateStats(entries: List<IntakeEntry>): IntakeStats {
    val eatEntries = entries.filterIsInstance<EatEntry>()
    val drinkEntries = entries.filterIsInstance<DrinkEntry>()

    // EatEntry 已有的 carbs 来自食物（grams->nutrient），
    // DrinkEntry 目前只记录 sugarGrams，我们把 sugar 也计入碳水
    val carbsFromFood = eatEntries.sumOf { it.carbs.toDouble() }
    val carbsFromDrinks = drinkEntries.sumOf { it.sugarGrams.toDouble() }

    val totalCarbs = carbsFromFood + carbsFromDrinks
    val totalSugar = drinkEntries.sumOf { it.sugarGrams.toDouble() } // 如果将来 EatEntry 也有 sugar 字段，可一起累加

    return IntakeStats(
        totalKcal = entries.sumOf { it.kcal.takeIf { it.isFinite() } ?: 0.0 },
        totalProtein = eatEntries.sumOf { it.protein.toDouble() },
        totalCarbs = totalCarbs,
        totalFats = eatEntries.sumOf { it.fats.toDouble() },
        totalSugar = totalSugar,
        totalCaffeine = drinkEntries.sumOf { it.caffeine.toDouble() },
        totalWater = drinkEntries.sumOf { it.waterML.toDouble() },
        totalAlcoholGram = drinkEntries.sumOf {
            val volume = (it.alcoholVolumeML ?: 0f).toDouble()
            val percent = (it.alcoholPercent ?: 0f).toDouble()
            alcoholGramFromVolume(volume, percent)
        },
        totalCholesterol = eatEntries.sumOf { it.cholesterol.toDouble() }
    )
}

fun alcoholGramFromVolume(volumeMl: Double, percent: Double): Double {
    val density = 0.789f
    return volumeMl * percent / 100f * density
}