package com.mealoo.app.model
import kotlinx.serialization.Serializable

@Serializable
data class MealDef(
    val id: String,
    val name: String,
    val kind: MealKind? = null,
    val trackColorHex: String? = null,
    val iconName: String? = null
)

const val DEFAULT_BREAKFAST_ID = "breakfast"
const val DEFAULT_LUNCH_ID = "lunch"
const val DEFAULT_DINNER_ID = "dinner"
const val UNCLASSIFIED_MEAL_ID = "unclassified"
