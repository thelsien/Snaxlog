package com.snaxlog.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.snaxlog.app.R
import com.snaxlog.app.data.local.entity.ServingUnit
import com.snaxlog.app.ui.theme.Spacing

/**
 * C-030: ServingUnitDropdown
 * EPIC-006: User-Created Foods and Recipes
 * US-018 AC-013-003: Serving unit selection from dropdown
 *
 * Dropdown selector for choosing a serving unit.
 * Shows all available ServingUnit options with both display name and abbreviation.
 *
 * @param selectedUnit Currently selected serving unit.
 * @param onUnitChange Callback when selection changes.
 * @param modifier Modifier for the component.
 * @param label Label for the dropdown.
 * @param enabled Whether the dropdown is enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingUnitDropdown(
    selectedUnit: ServingUnit,
    onUnitChange: (ServingUnit) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.serving_unit_default_label),
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selectorDescription = stringResource(
        R.string.serving_unit_selector_description,
        selectedUnit.displayName
    )

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedUnit.displayName,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .semantics {
                        contentDescription = selectorDescription
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ServingUnit.entries.forEach { unit ->
                    val selectDescription = stringResource(
                        R.string.serving_unit_select_option,
                        unit.displayName
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.serving_unit_option,
                                    unit.displayName,
                                    unit.abbreviation
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            onUnitChange(unit)
                            expanded = false
                        },
                        modifier = Modifier.semantics {
                            contentDescription = selectDescription
                        }
                    )
                }
            }
        }
    }
}

/**
 * Compact version of the ServingUnitDropdown that shows only the abbreviation.
 * Useful for inline use where space is limited.
 *
 * @param selectedUnit Currently selected serving unit.
 * @param onUnitChange Callback when selection changes.
 * @param modifier Modifier for the component.
 * @param enabled Whether the dropdown is enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingUnitDropdownCompact(
    selectedUnit: ServingUnit,
    onUnitChange: (ServingUnit) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val compactDescription = stringResource(
        R.string.serving_unit_compact_description,
        selectedUnit.displayName
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit.abbreviation,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .semantics {
                    contentDescription = compactDescription
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ServingUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                R.string.serving_unit_option,
                                unit.displayName,
                                unit.abbreviation
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onUnitChange(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
