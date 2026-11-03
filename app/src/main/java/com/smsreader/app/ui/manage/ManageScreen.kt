@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smsreader.app.ui.manage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.domain.model.AccountType
import com.smsreader.app.ui.components.SelectionButton
import com.smsreader.app.ui.review.ReviewViewModel

@Composable
fun ManageScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportTransactionsCsv(context.contentResolver, it) }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackupJson(context.contentResolver, it) }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackupJson(context.contentResolver, it) }
    }

    LaunchedEffect(state.message, state.error) {
        val text = state.error ?: state.message
        if (!text.isNullOrBlank()) {
            snackbar.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Manage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Data Export & Backup Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Data Export & Backup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export confirmed transactions to CSV, or back up and restore your full app database using JSON files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { exportCsvLauncher.launch("transactions_${System.currentTimeMillis()}.csv") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export CSV")
                            }

                            OutlinedButton(
                                onClick = { exportJsonLauncher.launch("smsreader_backup_${System.currentTimeMillis()}.json") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Backup JSON")
                            }
                        }

                        Button(
                            onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restore JSON Backup")
                        }
                    }
                }
            }

            // Accounts Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Accounts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(onClick = { showAddAccountDialog = true }) {
                                Text("Add Account")
                            }
                        }

                        if (state.accounts.isEmpty()) {
                            Text(
                                text = "No accounts created yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.accounts.forEach { account ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${account.accountType.name}${account.bank?.let { " • $it" } ?: ""}${account.cardLast4?.let { " ••••$it" } ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { accountToEdit = account }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Account",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // Categories Section
            item {
                val mainCategories = state.categories.filter { it.level == 1 }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(onClick = { showAddCategoryDialog = true }) {
                                Text("Add Category")
                            }
                        }

                        Text(
                            text = "${mainCategories.size} main categories, ${state.categories.count { it.level == 2 }} subcategories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        mainCategories.forEach { mainCat ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mainCat.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { categoryToEdit = mainCat }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Category",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                val subs = state.categories.filter { it.level == 2 && it.parentId == mainCat.id }
                                subs.forEach { subCat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${subCat.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { categoryToEdit = subCat }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Subcategory",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            // SMS History & Rescan Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SMS History & Scan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scan SMS inbox for the previous 3 months to check for unimported or past transaction alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = viewModel::rescanThreeMonths,
                            enabled = !state.isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.isSyncing) "Scanning 3 Months…" else "Rescan Previous 3 Months")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Add Account") },
            text = {
                AccountForm(
                    account = null,
                    onSave = { name, type, bank, last4 ->
                        viewModel.addAccount(name, type, bank, last4)
                        showAddAccountDialog = false
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) { Text("Cancel") }
            }
        )
    }

    accountToEdit?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToEdit = null },
            title = { Text("Edit Account") },
            text = {
                AccountForm(
                    account = account,
                    onSave = { name, type, bank, last4 ->
                        viewModel.updateAccount(account.id, name, type, bank, last4)
                        accountToEdit = null
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { accountToEdit = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Category") },
            text = {
                AddCategoryForm(
                    categories = state.categories,
                    onAddMain = viewModel::addMainCategory,
                    onAddSub = viewModel::addSubCategory,
                    onDone = { showAddCategoryDialog = false }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    categoryToEdit?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text(if (category.level == 1) "Edit Main Category" else "Edit Subcategory") },
            text = {
                EditCategoryForm(
                    category = category,
                    categories = state.categories,
                    onSave = { name, parentId ->
                        viewModel.updateCategory(category.id, name, parentId)
                        categoryToEdit = null
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AccountForm(
    account: AccountEntity? = null,
    onSave: (String, AccountType, String?, String?) -> Unit
) {
    var name by remember(account) { mutableStateOf(account?.name.orEmpty()) }
    var bank by remember(account) { mutableStateOf(account?.bank.orEmpty()) }
    var last4 by remember(account) { mutableStateOf(account?.cardLast4.orEmpty()) }
    var type by remember(account) { mutableStateOf(account?.accountType ?: AccountType.SAVINGS) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Account name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        SelectionButton(
            label = "Type",
            selectedText = type.name,
            options = AccountType.entries,
            optionText = { it.name },
            onSelected = { type = it },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bank,
            onValueChange = { bank = it },
            label = { Text("Bank (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = last4,
            onValueChange = { last4 = it.filter(Char::isDigit).take(4) },
            label = { Text("Card/account last 4 (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(name, type, bank.ifBlank { null }, last4.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (account == null) "Save account" else "Update account")
        }
    }
}

@Composable
private fun AddCategoryForm(
    categories: List<CategoryEntity>,
    onAddMain: (String) -> Unit,
    onAddSub: (String, Long) -> Unit,
    onDone: () -> Unit
) {
    val mains = categories.filter { it.level == 1 }
    var name by remember { mutableStateOf("") }
    var isSub by remember { mutableStateOf(false) }
    var parentId by remember { mutableStateOf<Long?>(null) }
    val parent = mains.firstOrNull { it.id == parentId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { isSub = false; parentId = null }) { Text("Main") }
            OutlinedButton(onClick = { isSub = true }) { Text("Subcategory") }
        }
        if (isSub) {
            SelectionButton(
                label = "Parent",
                selectedText = parent?.name ?: "Select parent",
                options = mains,
                optionText = { it.name },
                onSelected = { parentId = it.id },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = {
                if (name.isBlank()) return@Button
                if (isSub) parentId?.let { onAddSub(name, it) } else onAddMain(name)
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save category")
        }
    }
}

@Composable
private fun EditCategoryForm(
    category: CategoryEntity,
    categories: List<CategoryEntity>,
    onSave: (String, Long?) -> Unit
) {
    val mains = categories.filter { it.level == 1 }
    var name by remember(category) { mutableStateOf(category.name) }
    var parentId by remember(category) { mutableStateOf(category.parentId) }
    val parent = mains.firstOrNull { it.id == parentId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (category.level == 2) {
            SelectionButton(
                label = "Parent category",
                selectedText = parent?.name ?: "Select parent",
                options = mains,
                optionText = { it.name },
                onSelected = { parentId = it.id },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(name, if (category.level == 2) parentId else null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update category")
        }
    }
}
