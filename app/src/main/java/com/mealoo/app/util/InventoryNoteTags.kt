package com.mealoo.app.util

const val NOTE_TAG_THAW = "[THAW]"

fun buildThawNote(fromName: String, extra: String? = null): String {
    val from = fromName.trim()
    val base = if (from.isNotBlank()) {
        "$NOTE_TAG_THAW from=$from"
    } else {
        NOTE_TAG_THAW
    }
    val extraText = extra?.trim()?.takeIf { it.isNotBlank() }
    return if (extraText != null) "$base - $extraText" else base
}

fun String.hasThawTag(): Boolean = contains(NOTE_TAG_THAW)

fun String.extractThawFrom(): String? {
    val tagIndex = indexOf(NOTE_TAG_THAW)
    if (tagIndex < 0) return null
    val rest = substring(tagIndex + NOTE_TAG_THAW.length).trimStart()
    val prefix = "from="
    if (!rest.startsWith(prefix)) return null
    val raw = rest.removePrefix(prefix)
    val endIndex = raw.indexOf(" - ").let { if (it < 0) raw.length else it }
    val from = raw.substring(0, endIndex).trim()
    return from.takeIf { it.isNotBlank() }
}
