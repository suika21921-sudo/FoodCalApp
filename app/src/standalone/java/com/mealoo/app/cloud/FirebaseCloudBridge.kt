package com.mealoo.app.cloud

/**
 * Flavor bridge for the optional legacy Firebase identity.
 *
 * The standalone implementation never links or initializes Firebase. The cloud
 * implementation may read the current Firebase user when explicitly configured.
 */
object FirebaseCloudBridge {
    const val isEnabled: Boolean = false
    const val unavailableReason: String =
        "Legacy Firebase identity is unavailable in the standalone build"

    fun currentUserId(): String? = null
}
