package com.helix.budget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.helix.budget.BudgetApp
import com.helix.budget.data.AccentOptions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = (context.applicationContext as BudgetApp).settings
    val selected = settings.accentIndex

    var showPeriodDialog by remember { mutableStateOf(false) }
    var currencyField by remember { mutableStateOf(settings.currencySymbol) }

    Scaffold(
        topBar = { CompactHeader(title = "Settings", onBack = onBack) },
        containerColor = DarkBg
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.responsiveWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SectionLabel("ACCENT COLOUR")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AccentOptions.forEachIndexed { i, opt ->
                        Swatch(color = opt.color, selected = i == selected, onClick = { settings.setAccent(i) })
                    }
                }

                HorizontalDivider(color = DarkCard)

                SectionLabel("CURRENCY SYMBOL")
                OutlinedTextField(
                    value = currencyField,
                    onValueChange = {
                        currencyField = it.take(4)
                        settings.updateCurrency(currencyField)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLight, cursorColor = AccentLight)
                )

                HorizontalDivider(color = DarkCard)

                SectionLabel("BUDGET PERIOD")
                SettingsRow(
                    title = if (settings.periodActive)
                        "Active · ${settings.daysLeft()} days left"
                    else "Not set",
                    onClick = { showPeriodDialog = true }
                )
            }
        }
    }

    if (showPeriodDialog) {
        PeriodDialog(settings = settings, onDismiss = { showPeriodDialog = false })
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) AccentLight else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, "Selected", tint = Color.White)
    }
}

@Composable
private fun SettingsRow(title: String, onClick: () -> Unit) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AccentLight, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Accent)
        }
    }
}
