package com.snaxlog.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.snaxlog.app.ui.theme.CarbsDark
import com.snaxlog.app.ui.theme.CarbsLight
import com.snaxlog.app.ui.theme.FatDark
import com.snaxlog.app.ui.theme.FatLight
import com.snaxlog.app.ui.theme.ProteinDark
import com.snaxlog.app.ui.theme.ProteinLight
import com.snaxlog.app.ui.theme.Spacing

/**
 * C-028: MacroInputField
 * EPIC-006: User-Created Foods and Recipes
 * US-018: Create Simple Custom Food
 *
 * Numeric input field specifically for macro nutrients (protein, fat, carbs).
 * Features color-coded styling matching the macro indicators and validates
 * that input is a non-negative number.
 *
 * @param value Current input value as string.
 * @param onValueChange Callback when value changes.
 * @param macroType Type of macro (PROTEIN, FAT, CARBS) - determines color styling.
 * @param modifier Modifier for the component.
 * @param errorMessage Optional error message to display.
 * @param enabled Whether the field is enabled.
 */
@Composable
fun MacroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    macroType: MacroType,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val macroColor = getMacroColor(macroType, isDarkTheme)
    val label = getMacroLabel(macroType)
    val isError = errorMessage != null

    val fieldDescription = if (isError) {
        "$label field. Error: $errorMessage"
    } else {
        "$label field. Enter grams."
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Filter to only allow valid numeric input (digits and decimal point)
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                // Ensure only one decimal point
                val singleDecimal = if (filtered.count { it == '.' } > 1) {
                    val firstDot = filtered.indexOf('.')
                    filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
                } else {
                    filtered
                }
                onValueChange(singleDecimal)
            },
            label = { Text(label) },
            suffix = { Text("g") },
            isError = isError,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = fieldDescription
                    if (isError) {
                        error(errorMessage!!)
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = macroColor,
                focusedLabelColor = macroColor,
                cursorColor = macroColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )

        if (isError) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.base, top = Spacing.xs)
            )
        }
    }
}

/**
 * Returns the color for a macro type.
 */
private fun getMacroColor(macroType: MacroType, isDarkTheme: Boolean): Color {
    return when (macroType) {
        MacroType.PROTEIN -> if (isDarkTheme) ProteinDark else ProteinLight
        MacroType.FAT -> if (isDarkTheme) FatDark else FatLight
        MacroType.CARBS -> if (isDarkTheme) CarbsDark else CarbsLight
    }
}

/**
 * Returns the label for a macro type.
 */
private fun getMacroLabel(macroType: MacroType): String {
    return when (macroType) {
        MacroType.PROTEIN -> "Protein"
        MacroType.FAT -> "Fat"
        MacroType.CARBS -> "Carbs"
    }
}

/**
 * Extension function to calculate luminance of a color.
 */
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
