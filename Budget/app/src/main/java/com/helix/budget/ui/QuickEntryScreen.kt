package com.helix.budget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helix.budget.BudgetApp
import com.helix.budget.data.TYPE_EXPENSE
import com.helix.budget.data.TYPE_INCOME
import com.helix.budget.data.Transaction
import com.helix.budget.util.Money
import kotlinx.coroutines.launch

@Composable
fun QuickEntryScreen(
    initialType: String,
    editId: Long?,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetApp
    val db = app.database
    val symbol = app.settings.currencySymbol
    val accent = app.settings.accent
    val scope = rememberCoroutineScope()

    var expr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var loaded by remember { mutableStateOf(editId == null) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(editId) {
        if (editId != null && !loaded) {
            db.transactionDao().getById(editId)?.let { tx ->
                editing = tx
                type = tx.type
                note = tx.note
                expr = (tx.amountMinor / 100.0).let {
                    if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                }
            }
            loaded = true
        }
    }

    val preview = Money.parseExpression(expr)
    val isIncome = type == TYPE_INCOME
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        val amount = Money.parseExpression(expr) ?: return
        if (amount <= 0) return
        scope.launch {
            val base = editing
            if (base != null) {
                db.transactionDao().update(base.copy(amountMinor = amount, type = type, note = note.trim()))
            } else {
                db.transactionDao().insert(
                    Transaction(amountMinor = amount, type = type, note = note.trim())
                )
            }
            onDone()
        }
    }

    Scaffold(
        topBar = {
            CompactHeader(
                title = when {
                    editId != null -> "Edit"
                    isIncome -> "Add money"
                    else -> "Spend"
                },
                onBack = onDone,
                actions = {
                    if (editId != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Accent)
                        }
                    }
                }
            )
        },
        containerColor = accent.copy(alpha = 0.12f).compositeOver(DarkBg)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .responsiveWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Expense / Income toggle
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleChip("Spend", !isIncome, ExpenseText, Modifier.weight(1f)) { type = TYPE_EXPENSE }
                ToggleChip("Add money", isIncome, IncomeText, Modifier.weight(1f)) { type = TYPE_INCOME }
            }

            // Amount display
            Surface(color = DarkCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        expr.ifBlank { "0" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = AccentLight,
                        maxLines = 1
                    )
                    if (preview != null && expr.any { it == '+' || it == '-' }) {
                        Text(
                            "= ${Money.format(preview, symbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Accent
                        )
                    }
                }
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (isIncome) "Where from?" else "What was it?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLight, cursorColor = AccentLight)
            )

            Spacer(Modifier.weight(1f))

            // Keypad
            Keypad(
                onKey = { expr += it },
                onOperator = { op ->
                    // avoid two operators in a row; allow leading nothing
                    if (expr.isNotEmpty() && expr.last() !in charArrayOf('+', '-')) expr += op
                },
                onBackspace = { if (expr.isNotEmpty()) expr = expr.dropLast(1) },
                onClear = { expr = "" }
            )

            Button(
                onClick = { save() },
                enabled = preview != null && preview > 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncome) IncomeText else DarkCard,
                    contentColor = if (isIncome) DarkBg else AccentLight,
                    disabledContainerColor = DarkSurface,
                    disabledContentColor = CounterSoft
                )
            ) {
                Text(if (editId != null) "Save changes" else if (isIncome) "Add money" else "Save expense")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DarkSurface,
            title = { Text("Delete transaction?") },
            text = { Text("This can't be undone.", color = Accent) },
            confirmButton = {
                TextButton(onClick = {
                    val tx = editing!!
                    confirmDelete = false
                    scope.launch { db.transactionDao().delete(tx); onDone() }
                }) { Text("Delete", color = ExpenseText) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, selColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (selected) selColor.copy(alpha = 0.18f).compositeOver(DarkCard) else DarkCard,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) selColor else Accent, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Keypad(
    onKey: (String) -> Unit,
    onOperator: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val rows = listOf(
        listOf("7", "8", "9", "+"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "C"),
        listOf(".", "0", "DEL")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val weight = if (key == "DEL") 2f else 1f
                    Key(key, Modifier.weight(weight)) {
                        when (key) {
                            "+", "-" -> onOperator(key)
                            "C" -> onClear()
                            "DEL" -> onBackspace()
                            else -> onKey(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    val isOp = label in listOf("+", "-", "C", "DEL")
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOp) DarkSurface else DarkCard)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (label == "DEL") {
            Icon(Icons.AutoMirrored.Filled.Backspace, "Delete", tint = Accent)
        } else {
            Text(
                label,
                color = if (isOp) Accent else AccentLight,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
