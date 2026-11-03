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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsreader.app.data.local.dao.BudgetProgressRow
import com.smsreader.app.data.local.dao.SubcategorySpendRow
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.domain.model.BudgetPeriod
import com.smsreader.app.ui.components.SelectionButton
import com.smsreader.app.util.Money

@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    val mainCategories = state.categories.filter { it.level == 1 }

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
                items(state.rows, key = { "budget_${it.budgetId}" }) { row ->
                    val subCategories = state.categories.filter { it.level == 2 && it.parentId == row.categoryId }
                    BudgetCard(
                        row = row,
                        subCategories = subCategories,
                        subcategorySpend = state.subcategorySpend
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showDialog) {
        BudgetDialog(
            categories = mainCategories,
            onDismiss = { showDialog = false },
            onSave = { categoryId, amount -> viewModel.saveBudget(categoryId, amount); showDialog = false }
        )
    }
}

@Composable
private fun BudgetCard(
    row: BudgetProgressRow,
    subCategories: List<CategoryEntity>,
    subcategorySpend: List<SubcategorySpendRow>
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val progress = if (row.limitMinor <= 0) 0f else (row.spentMinor.toFloat() / row.limitMinor.toFloat()).coerceIn(0f, 1f)

    val spendForCategory = subcategorySpend.filter { it.l1CategoryId == row.categoryId }
    val spendBySubcatId = spendForCategory.filter { it.l2CategoryId != null }.associate { it.l2CategoryId!! to it.totalMinor }
    val unassignedSpend = spendForCategory.firstOrNull { it.l2CategoryId == null }?.totalMinor ?: 0L

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (expanded) "Tap to collapse" else "Tap for subcategory breakup",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${Money.format(row.spentMinor)} / ${Money.format(row.limitMinor)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

            if (row.spentMinor > row.limitMinor) {
                Text(
                    text = "Over budget by ${Money.format(row.spentMinor - row.limitMinor)}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Subcategory Breakup",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (subCategories.isEmpty() && unassignedSpend == 0L) {
                    Text(
                        text = "No subcategory spending recorded for this period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        subCategories.forEach { subCat ->
                            val spent = spendBySubcatId[subCat.id] ?: 0L
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${subCat.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Money.format(spent),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (unassignedSpend > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• Direct / Uncategorized",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Money.format(unassignedSpend),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
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
