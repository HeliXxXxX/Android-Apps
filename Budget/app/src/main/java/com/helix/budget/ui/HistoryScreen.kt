package com.helix.budget.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helix.budget.BudgetApp
import com.helix.budget.data.TYPE_INCOME
import com.helix.budget.util.Money
import com.helix.budget.util.TimeRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetApp
    val db = app.database
    val symbol = app.settings.currencySymbol

    val all by db.transactionDao().getAll().collectAsStateWithLifecycle(emptyList())

    // Group by calendar day (newest first; list already sorted desc)
    val groups = remember(all) {
        all.groupBy { TimeRange.startOfDay(it.createdAt) }
            .toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = { CompactHeader(title = "History", onBack = onBack) },
        containerColor = DarkBg
    ) { padding ->
        if (all.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No transactions yet.", color = Accent, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { (dayStart, txs) ->
                    val dayNet = txs.sumOf { if (it.type == TYPE_INCOME) it.amountMinor else -it.amountMinor }
                    item(key = "header_$dayStart") {
                        Row(
                            Modifier.responsiveWidth().padding(top = 8.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionLabel(dayLabel(dayStart).uppercase())
                            Spacer(Modifier.weight(1f))
                            Text(
                                Money.formatSigned(dayNet, symbol),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (dayNet >= 0) IncomeText else ExpenseText
                            )
                        }
                    }
                    items(txs, key = { it.id }) { tx ->
                        TransactionRow(tx, symbol, Modifier.responsiveWidth(), onClick = { onEdit(tx.id) })
                    }
                }
            }
        }
    }
}

private fun dayLabel(dayStart: Long): String {
    val today = TimeRange.startOfToday()
    return when (dayStart) {
        today -> "Today"
        today - 86_400_000L -> "Yesterday"
        else -> dateFormat.format(Date(dayStart))
    }
}

private val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
