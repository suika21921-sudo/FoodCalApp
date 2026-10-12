package com.mealoo.app.cloud

import android.content.Context

/** No-op cloud AI bridge for standalone builds. */
object ExternalCloudAiHandoff {
    const val isAvailable: Boolean = false

    fun openHome(context: Context): Boolean = false

    fun sendPrompt(context: Context, prompt: String): Boolean = false
}
