package com.helix.budget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helix.budget.BudgetApp
import com.helix.budget.data.TYPE_INCOME
import com.helix.budget.data.Transaction
import com.helix.budget.util.Money
import com.helix.budget.util.TimeRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onSpend: () -> Unit,
    onAddMoney: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetApp
    val db = app.database
    val settings = app.settings
    val symbol = settings.currencySymbol
    val accent = settings.accent

    val balance by db.transactionDao().balanceMinor().collectAsStateWithLifecycle(0L)
    val spentToday by db.transactionDao()
        .spentBetween(TimeRange.startOfToday(), TimeRange.startOfTomorrow())
        .collectAsStateWithLifecycle(0L)
    val recent by db.transactionDao().getAll().collectAsStateWithLifecycle(emptyList())

    var showPeriodDialog by remember { mutableStateOf(false) }

    // Daily allowance: active period's per-day if set, else balance / 7
    val perDay = if (settings.periodActive) balance / settings.daysLeft() else balance / 7
    val leftToday = perDay - spentToday

    Scaffold(
        topBar = {
            CompactHeader(
                title = "Budget",
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = AccentLight)
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FloatingActionButton(
                    onClick = onAddMoney,
                    containerColor = DarkSurface,
                    contentColor = IncomeText
                ) { Icon(Icons.Default.Add, "Add money") }
                ExtendedFloatingActionButton(
                    onClick = onSpend,
                    containerColor = DarkCard,
                    contentColor = AccentLight,
                    icon = { Icon(Icons.Default.Remove, null) },
                    text = { Text("Spend") }
                )
            }
        },
        containerColor = accent.copy(alpha = 0.12f).compositeOver(DarkBg)
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Balance ──
            item {
                Surface(Modifier.responsiveWidth(), color = DarkCard, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        SectionLabel("BALANCE")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            Money.format(balance, symbol),
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (balance < 0) ExpenseText else AccentLight
                        )
                    }
                }
            }

            // ── Today ──
            item {
                Surface(Modifier.responsiveWidth(), color = DarkCard, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        SectionLabel("TODAY")
                        Spacer(Modifier.height(10.dp))
                        val fraction = if (perDay > 0) (spentToday.toFloat() / perDay).coerceIn(0f, 1f) else 0f
                        val overBudget = spentToday > perDay && perDay > 0
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (overBudget) ExpenseText else accent,
                            trackColor = DarkSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${Money.format(spentToday, symbol)} spent of ${Money.format(perDay, symbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Accent
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (leftToday >= 0) "${Money.format(leftToday, symbol)} left for today"
                            else "${Money.format(-leftToday, symbol)} over today's budget",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (leftToday >= 0) IncomeText else ExpenseText
                        )
                    }
                }
            }

            // ── Active period ──
            item {
                Surface(
                    Modifier.responsiveWidth().clickable { showPeriodDialog = true },
                    color = DarkCard,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel("BUDGET PERIOD")
                            Spacer(Modifier.height(6.dp))
                            if (settings.periodActive) {
                                Text(
                                    "Make it last ${weeksLabel(settings.periodLengthDays)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AccentLight
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${Money.format(perDay, symbol)}/day · ${settings.daysLeft()} days left",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Accent
                                )
                            } else {
                                Text(
                                    "Set a budget period",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AccentLight
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Make your money last a set time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Accent
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Accent)
                    }
                }
            }

            // ── Quick reference ──
            item {
                Surface(Modifier.responsiveWidth(), color = DarkCard, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        SectionLabel("SPEND PER DAY")
                        Spacer(Modifier.height(10.dp))
                        ReferenceRow("1 week", Money.format(balance / 7, symbol))
                        ReferenceRow("2 weeks", Money.format(balance / 14, symbol))
                        ReferenceRow("3 weeks", Money.format(balance / 21, symbol))
                    }
                }
            }

            // ── Recent transactions ──
            if (recent.isNotEmpty()) {
                item {
                    Row(
                        Modifier.responsiveWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("RECENT")
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onHistory) { Text("See all", color = accent) }
                    }
                }
                items(recent.take(5), key = { it.id }) { tx ->
                    TransactionRow(tx, symbol, Modifier.responsiveWidth())
                }
            } else {
                item {
                    Box(Modifier.responsiveWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No transactions yet.\nTap Spend or Add money to begin.",
                            color = Accent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showPeriodDialog) {
        PeriodDialog(
            settings = settings,
            onDismiss = { showPeriodDialog = false }
        )
    }
}

@Composable
private fun ReferenceRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Accent)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = AccentLight)
    }
}

@Composable
fun TransactionRow(tx: Transaction, symbol: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val isIncome = tx.type == TYPE_INCOME
    val signed = (if (isIncome) tx.amountMinor else -tx.amountMinor)
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = DarkCard,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    tx.note.ifBlank { if (isIncome) "Income" else "Expense" },
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(timeFormat.format(Date(tx.createdAt)), style = MaterialTheme.typography.labelSmall, color = CounterSoft)
            }
            Text(
                Money.formatSigned(signed, symbol),
                style = MaterialTheme.typography.titleMedium,
                color = if (isIncome) IncomeText else ExpenseText
            )
        }
    }
}

private fun weeksLabel(days: Int): String = when (days) {
    7 -> "1 week"
    14 -> "2 weeks"
    21 -> "3 weeks"
    28 -> "4 weeks"
    else -> "$days days"
}

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
