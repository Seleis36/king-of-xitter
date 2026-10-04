package com.kingofxitter.app.content

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Resolves the `strings.xml` keys that `:content` and `:cards` carry instead of display text.
 * Those modules are pure Kotlin, so they can't name an `R.string` ID; this is the one place that
 * turns a key into one, caching the lookup per key.
 *
 * A missing key returns 0 and reads as an empty string rather than crashing a screen, which is
 * what previews of half-written content need. `ContentStringsTest` fails if any key the bank
 * declares is missing, so a typo is caught at build time instead of at runtime.
 */
class ContentStrings(
    context: Context,
) {
    private val resources = context.applicationContext.resources
    private val packageName = context.applicationContext.packageName
    private val ids = HashMap<String, Int>()

    /** The string resource ID for [key], or 0 when the app has no such string. */
    @StringRes
    fun id(key: String): Int = ids.getOrPut(key) { resources.getIdentifier(key, "string", packageName) }

    /** [key]'s text with [formatArgs] applied, or an empty string when the key is unknown. */
    fun text(
        key: String,
        vararg formatArgs: Any,
    ): String {
        val id = id(key)
        if (id == 0) return ""
        return if (formatArgs.isEmpty()) resources.getString(id) else resources.getString(id, *formatArgs)
    }
}

/** A [ContentStrings] bound to the composition's context. */
@Composable
fun rememberContentStrings(): ContentStrings {
    val context = LocalContext.current
    return remember(context) { ContentStrings(context) }
}
