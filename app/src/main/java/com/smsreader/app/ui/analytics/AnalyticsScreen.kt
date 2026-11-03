@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smsreader.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsreader.app.domain.model.AnalyticsPeriod
import com.smsreader.app.ui.components.SelectionButton
import com.smsreader.app.util.Dates
import com.smsreader.app.util.Money

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mainCategories = state.categories.filter { it.level == 1 }
    var showCustom by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Analytics") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SelectionButton(
                    label = "Period",
                    selectedText = when (state.filter.period) {
                        AnalyticsPeriod.THIS_MONTH -> "This month"
                        AnalyticsPeriod.THIS_YEAR -> "This year"
                        AnalyticsPeriod.CUSTOM -> "Custom"
                    },
                    options = AnalyticsPeriod.entries,
                    optionText = {
                        when (it) {
                            AnalyticsPeriod.THIS_MONTH -> "This month"
                            AnalyticsPeriod.THIS_YEAR -> "This year"
                            AnalyticsPeriod.CUSTOM -> "Custom date range"
                        }
                    },
                    onSelected = { if (it == AnalyticsPeriod.CUSTOM) showCustom = true else viewModel.setPeriod(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                SelectionButton(
                    label = "Account",
                    selectedText = state.accounts.firstOrNull { it.id == state.filter.accountId }?.name ?: "All accounts",
                    options = listOf<AccountChoice>(AccountChoice.All) + state.accounts.map { AccountChoice.Item(it.id, it.name) },
                    optionText = { it.label },
                    onSelected = { viewModel.setAccount((it as? AccountChoice.Item)?.id) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                SelectionButton(
                    label = "Category",
                    selectedText = mainCategories.firstOrNull { it.id == state.filter.categoryId }?.name ?: "All categories",
                    options = listOf<CategoryChoice>(CategoryChoice.All) + mainCategories.map { CategoryChoice.Item(it.id, it.name) },
                    optionText = { it.label },
                    onSelected = { viewModel.setCategory((it as? CategoryChoice.Item)?.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Debit", Money.format(state.snapshot.summary.totalDebitMinor), Modifier.weight(1f))
                    MetricCard("Credit", Money.format(state.snapshot.summary.totalCreditMinor), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                MetricCard(
                    "Net",
                    Money.format(state.snapshot.summary.totalCreditMinor - state.snapshot.summary.totalDebitMinor),
                    Modifier.fillMaxWidth()
                )
            }

            item { Text("Spending by category", style = MaterialTheme.typography.titleLarge) }
            if (state.snapshot.categorySpend.isEmpty()) {
                item { Text("No debit transactions in this period.") }
            } else {
                val max = state.snapshot.categorySpend.maxOfOrNull { it.totalMinor }?.coerceAtLeast(1L) ?: 1L
                items(state.snapshot.categorySpend, key = { "category_${it.categoryId}" }) { row ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.categoryName)
                            Text(Money.format(row.totalMinor))
                        }
                        LinearProgressIndicator(
                            progress = { (row.totalMinor.toFloat() / max.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Text("Recent transactions", style = MaterialTheme.typography.titleLarge) }
            items(state.snapshot.recent, key = { "tx_${it.id}" }) { tx ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(tx.merchantName ?: tx.description, maxLines = 1)
                            Text(Dates.format(tx.transactionDate), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${if (tx.transactionType.name == "DEBIT") "−" else "+"}${Money.format(tx.amountMinor, tx.currency)}")
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showCustom) {
        CustomDateDialog(
            initialStart = state.filter.customStart,
            initialEnd = state.filter.customEnd,
            onDismiss = { showCustom = false },
            onApply = { start, end -> viewModel.setCustomDates(start, end); showCustom = false }
        )
    }
}

private sealed interface AccountChoice {
    val label: String
    data object All : AccountChoice { override val label = "All accounts" }
    data class Item(val id: Long, override val label: String) : AccountChoice
}

private sealed interface CategoryChoice {
    val label: String
    data object All : CategoryChoice { override val label = "All categories" }
    data class Item(val id: Long, override val label: String) : CategoryChoice
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CustomDateDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit
) {
    var selectingStart by remember { mutableStateOf(true) }
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var pickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom date range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { selectingStart = true; pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("From: ${Dates.format(start)}")
                }
                Button(onClick = { selectingStart = false; pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("To: ${Dates.format(end)}")
                }
                if (end < start) Text("End date must be after start date", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = { if (end >= start) onApply(start, end) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickerOpen) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = if (selectingStart) start else end)
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { if (selectingStart) start = it else end = it }
                    pickerOpen = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickerOpen = false }) { Text("Cancel") } }
        ) { DatePicker(picker) }
    }
}
