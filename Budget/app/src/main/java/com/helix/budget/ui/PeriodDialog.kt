package com.helix.budget.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.helix.budget.data.SettingsStore

private data class Preset(val label: String, val days: Int)

private val presets = listOf(
    Preset("1 week", 7),
    Preset("2 weeks", 14),
    Preset("3 weeks", 21),
    Preset("4 weeks", 28),
)

/** Dialog to set or clear the active "make it last N" budget period. */
@Composable
fun PeriodDialog(settings: SettingsStore, onDismiss: () -> Unit) {
    var customDays by remember { mutableStateOf("") }
    var selected by remember {
        mutableIntStateOf(if (settings.periodActive) settings.periodLengthDays else 14)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Budget period") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "How long should your current balance last?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Accent
                )
                Spacer(Modifier.height(4.dp))
                presets.forEach { preset ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == preset.days && customDays.isBlank(),
                                onClick = { selected = preset.days; customDays = "" }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == preset.days && customDays.isBlank(),
                            onClick = { selected = preset.days; customDays = "" },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentLight)
                        )
                        Text(preset.label, color = AccentLight)
                    }
                }
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { customDays = it.filter(Char::isDigit).take(3) },
                    label = { Text("Custom days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentLight, cursorColor = AccentLight
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val days = customDays.toIntOrNull()?.takeIf { it > 0 } ?: selected
                settings.setPeriod(days)
                onDismiss()
            }) { Text("Set") }
        },
        dismissButton = {
            Row {
                if (settings.periodActive) {
                    TextButton(onClick = { settings.clearPeriod(); onDismiss() }) {
                        Text("Clear", color = ExpenseText)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
