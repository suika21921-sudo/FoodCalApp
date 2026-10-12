package com.mealoo.app.cloud

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/** External Gemini handoff retained only in the explicitly enabled cloud flavor. */
object ExternalCloudAiHandoff {
    const val isAvailable: Boolean = true
    private const val GEMINI_PACKAGE = "com.google.android.apps.bard"
    private const val GEMINI_HOME = "https://gemini.google.com/app"

    fun openHome(context: Context): Boolean {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GEMINI_HOME)).apply {
            setPackage(GEMINI_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(appIntent)
            true
        } catch (_: ActivityNotFoundException) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GEMINI_HOME)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            }.getOrDefault(false)
        }
    }

    fun sendPrompt(context: Context, prompt: String): Boolean {
        val appIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, prompt)
            type = "text/plain"
            setPackage(GEMINI_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(appIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            val encoded = URLEncoder.encode(prompt, Charsets.UTF_8.name())
            val target = if (encoded.length < 1_800) "$GEMINI_HOME?q=$encoded" else GEMINI_HOME
            return runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            }.getOrDefault(false)
        }
    }
}
