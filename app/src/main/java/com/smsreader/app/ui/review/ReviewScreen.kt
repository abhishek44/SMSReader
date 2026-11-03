@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smsreader.app.ui.review

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.local.entity.SmsReviewItemEntity
import com.smsreader.app.data.repository.ConfirmTransactionInput
import com.smsreader.app.domain.model.TransactionType
import com.smsreader.app.ui.components.SelectionButton
import com.smsreader.app.util.Dates
import com.smsreader.app.util.Money

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onNavigateToManage: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbar = remember { SnackbarHostState() }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var selectedItem by remember { mutableStateOf<SmsReviewItemEntity?>(null) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var permissionBlocked by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequested = true
        hasPermission = granted
        permissionBlocked = !granted && activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) != true
        if (granted) viewModel.onSmsPermissionAvailable()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            permissionBlocked = false
            viewModel.onSmsPermissionAvailable()
        } else if (permissionRequested &&
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) != true
        ) {
            permissionBlocked = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.onSmsPermissionAvailable()
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
                title = { Text("Review transactions") },
                actions = {
                    IconButton(onClick = onNavigateToManage) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Manage Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (!hasPermission) {
                PermissionCard(
                    showRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) == true,
                    permissionBlocked = permissionBlocked,
                    onRequest = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                )
            } else {
                Column(Modifier.fillMaxWidth()) {
                    Text("Pending: ${state.pending.size}", style = MaterialTheme.typography.titleMedium)
                    Text("First run imports the previous 3 months.", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                if (state.isSyncing && state.pending.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (state.pending.isEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Nothing to review", style = MaterialTheme.typography.titleMedium)
                            Text("New transaction alerts will appear here the next time you scan the SMS inbox.")
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.pending, key = { it.id }) { item ->
                            ReviewCard(
                                item = item,
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        TransactionReviewDialog(
            item = item,
            accounts = state.accounts,
            categories = state.categories,
            onDismiss = { selectedItem = null },
            onSave = {
                viewModel.confirm(it)
                selectedItem = null
            },
            onIgnore = {
                viewModel.ignore(item.id)
                selectedItem = null
            },
            onAddAccount = {
                selectedItem = null
                onNavigateToManage()
            }
        )
    }
}

@Composable
private fun PermissionCard(
    showRationale: Boolean,
    permissionBlocked: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SMS access required", style = MaterialTheme.typography.titleLarge)
            Text(
                when {
                    permissionBlocked ->
                        "SMS access is currently blocked. Open app settings and enable SMS permission. If the SMS permission is unavailable there, reinstall using an installer that allows restricted SMS permissions."
                    showRationale ->
                        "SMSReader reads your local SMS inbox only while you use the app. Access is needed to find bank and card transaction alerts."
                    else ->
                        "Grant SMS access to import transaction alerts. On the first successful scan, SMSReader checks the previous 3 months."
                }
            )
            Button(onClick = if (permissionBlocked) onOpenSettings else onRequest) {
                Text(if (permissionBlocked) "Open app settings" else "Grant SMS access")
            }
        }
    }
}

@Composable
private fun ReviewCard(item: SmsReviewItemEntity, onClick: () -> Unit) {
    val headerTitle = item.sender ?: item.detectedMerchant ?: "SMS Alert"
    val avatarChar = headerTitle.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'S'

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = avatarChar.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Money.format(item.detectedAmountMinor, item.detectedCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subTitleText = when {
                        item.detectedMerchant != null && item.sender != null -> item.detectedMerchant
                        item.accountHintLast4 != null -> "Card ••••${item.accountHintLast4}"
                        else -> null
                    }
                    if (subTitleText != null) {
                        Text(
                            text = subTitleText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Dates.format(item.receivedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TransactionReviewDialog(
    item: SmsReviewItemEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (ConfirmTransactionInput) -> Unit,
    onIgnore: () -> Unit,
    onAddAccount: () -> Unit
) {
    val mainCategories = categories.filter { it.level == 1 }
    var accountId by remember(item.id) {
        mutableStateOf(accounts.firstOrNull { it.cardLast4 != null && it.cardLast4 == item.accountHintLast4 }?.id)
    }
    var l1Id by remember(item.id) { mutableStateOf<Long?>(null) }
    var l2Id by remember(item.id) { mutableStateOf<Long?>(null) }
    var type by remember(item.id) { mutableStateOf(item.suggestedType ?: TransactionType.DEBIT) }
    var amount by remember(item.id) { mutableStateOf(Money.editable(item.detectedAmountMinor)) }
    var merchant by remember(item.id) { mutableStateOf(item.detectedMerchant.orEmpty()) }
    var transactionDate by remember(item.id) { mutableLongStateOf(item.receivedAt) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }

    val subcategories = categories.filter { it.level == 2 && it.parentId == l1Id }
    val account = accounts.firstOrNull { it.id == accountId }
    val l1 = mainCategories.firstOrNull { it.id == l1Id }
    val l2 = subcategories.firstOrNull { it.id == l2Id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review transaction") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sender: ${item.sender ?: "Unknown"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Dates.format(item.receivedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider()

                Text("Transaction Details", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SelectionButton(
                    label = "Account",
                    selectedText = account?.name ?: "Select account",
                    options = accounts,
                    optionText = { it.name },
                    onSelected = { accountId = it.id },
                    modifier = Modifier.fillMaxWidth()
                )
                if (accounts.isEmpty()) {
                    TextButton(onClick = onAddAccount) { Text("Add an account first") }
                }
                SelectionButton(
                    label = "Main category",
                    selectedText = l1?.name ?: "Select category",
                    options = mainCategories,
                    optionText = { it.name },
                    onSelected = { l1Id = it.id; l2Id = null },
                    modifier = Modifier.fillMaxWidth()
                )
                SelectionButton(
                    label = "Subcategory (optional)",
                    selectedText = l2?.name ?: "None",
                    options = subcategories,
                    optionText = { it.name },
                    onSelected = { l2Id = it.id },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = l1Id != null && subcategories.isNotEmpty()
                )
                SelectionButton(
                    label = "Type",
                    selectedText = type.name,
                    options = TransactionType.entries,
                    optionText = { it.name },
                    onSelected = { type = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date: ${Dates.format(transactionDate)}")
                }
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val minor = Money.parseToMinor(amount)
                validation = when {
                    minor == null || minor <= 0L -> "Enter a valid amount"
                    accountId == null -> "Select an account"
                    l1Id == null -> "Select a category"
                    else -> null
                }
                if (validation == null) {
                    onSave(
                        ConfirmTransactionInput(
                            reviewItemId = item.id,
                            accountId = accountId!!,
                            l1CategoryId = l1Id!!,
                            l2CategoryId = l2Id,
                            amountMinor = minor!!,
                            currency = item.detectedCurrency,
                            merchantName = merchant.ifBlank { null },
                            description = merchant.ifBlank { item.body }.take(500),
                            transactionDate = transactionDate,
                            transactionType = type
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onIgnore) {
                    Text("Ignore", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = transactionDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { transactionDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}
