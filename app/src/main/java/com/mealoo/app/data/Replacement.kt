package com.mealoo.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Replacement(
    val storageName: String,
    val grams: Double,
    val shopProductId: String? = null,
    val shopId: String? = null
)
