@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smsreader.app.ui.budget

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.domain.model.BudgetPeriod
import com.smsreader.app.ui.components.SelectionButton
import com.smsreader.app.util.Money

@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Budget") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.setPeriod(BudgetPeriod.MONTHLY) }) { Text("Monthly") }
                    OutlinedButton(onClick = { viewModel.setPeriod(BudgetPeriod.YEARLY) }) { Text("Yearly") }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Add / update budget") }
            }

            if (state.rows.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("No ${state.period.name.lowercase()} budgets yet", style = MaterialTheme.typography.titleMedium)
                            Text("Add a budget for any main category to compare planned and actual spending.")
                        }
                    }
                }
            } else {
                items(state.rows, key = { it.budgetId }) { row ->
                    val progress = if (row.limitMinor <= 0) 0f else (row.spentMinor.toFloat() / row.limitMinor.toFloat()).coerceIn(0f, 1f)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.categoryName, style = MaterialTheme.typography.titleMedium)
                                Text("${Money.format(row.spentMinor)} / ${Money.format(row.limitMinor)}")
                            }
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            if (row.spentMinor > row.limitMinor) {
                                Text("Over budget by ${Money.format(row.spentMinor - row.limitMinor)}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showDialog) {
        BudgetDialog(
            categories = state.categories,
            onDismiss = { showDialog = false },
            onSave = { categoryId, amount -> viewModel.saveBudget(categoryId, amount); showDialog = false }
        )
    }
}

@Composable
private fun BudgetDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val category = categories.firstOrNull { it.id == categoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton(
                    label = "Category",
                    selectedText = category?.name ?: "Select category",
                    options = categories,
                    optionText = { it.name },
                    onSelected = { categoryId = it.id },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Budget amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val minor = Money.parseToMinor(amount)
                error = when {
                    categoryId == null -> "Select a category"
                    minor == null || minor <= 0L -> "Enter a valid amount"
                    else -> null
                }
                if (error == null) onSave(categoryId!!, minor!!)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
