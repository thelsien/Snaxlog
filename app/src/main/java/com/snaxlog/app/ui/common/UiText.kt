package com.snaxlog.app.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/**
 * A small abstraction for user-facing text produced by ViewModels.
 *
 * ViewModels do not have access to a [Context], so they cannot resolve string resources
 * themselves. Instead they emit a [UiText] describing which resource (and arguments) to use,
 * and the UI layer resolves it to a [String] via [asString] (composable) or [asString] with a
 * [Context].
 *
 * This keeps all user-facing copy in `res/values/strings.xml` while leaving message *selection*
 * logic in the ViewModel where it can be unit tested against resource IDs.
 */
sealed interface UiText {

    /**
     * A string-resource-backed piece of text, optionally with positional format arguments.
     *
     * @param resId the `@StringRes` resource id.
     * @param args positional arguments for the resource's format placeholders (e.g. `%1$d`).
     */
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    /** Resolves this [UiText] to a [String] using a [Context] (e.g. from a ViewModel-driven flow). */
    fun asString(context: Context): String = when (this) {
        is StringResource ->
            if (args.isEmpty()) {
                context.getString(resId)
            } else {
                // Nested UiText args are resolved recursively so that, e.g., a macro label
                // resource can be embedded into a validation-message resource.
                val resolved = args.map { arg ->
                    if (arg is UiText) arg.asString(context) else arg
                }
                context.getString(resId, *resolved.toTypedArray())
            }
    }

    companion object {
        /** Convenience factory for a resource with positional arguments. */
        fun res(@StringRes resId: Int, vararg args: Any): UiText =
            StringResource(resId, args.toList())
    }
}

/** Resolves a [UiText] to a [String] inside a composable using [stringResource]. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.StringResource ->
        if (args.isEmpty()) {
            stringResource(resId)
        } else {
            // Resolve nested UiText args against the current Context so that embedded
            // resources (e.g. a macro label) are rendered as text, not toString().
            val context = LocalContext.current
            val resolved = args.map { arg ->
                if (arg is UiText) arg.asString(context) else arg
            }
            stringResource(resId, *resolved.toTypedArray())
        }
}
