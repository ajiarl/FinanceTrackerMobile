package com.sena.financetracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalFastAddDialog
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.DashboardAccountsSection
import com.sena.financetracker.ui.dashboard.components.DashboardBalanceSection
import com.sena.financetracker.ui.dashboard.components.DashboardTransactionsSection
import com.sena.financetracker.ui.dashboard.components.NeobrutalAddAccountDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalEditTransactionDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalReconcileDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalTransferDialog
import com.sena.financetracker.viewmodel.FinanceUiState
import com.sena.financetracker.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    FinanceDashboardContent(
        uiState = uiState,
        onAddTransaction = { title, amount, type, category, date, accId, accName, notes ->
            viewModel.addTransaction(title, amount, type, category, date, accId, accName, notes)
        },
        onDeleteTransaction = { viewModel.deleteTransaction(it) },
        onUpdateTransaction = { oldTx, newTx -> viewModel.updateTransaction(oldTx, newTx) },
        onTransferFunds = { fromAcc, toAcc, amount, notes, date ->
            viewModel.transferFunds(fromAcc, toAcc, amount, notes, date)
        },
        onAddAccount = { name, type, initialBalance ->
            viewModel.addAccount(name, type, initialBalance)
        },
        onReconcileAccount = { account, actualBalance ->
            viewModel.reconcileAccount(account, actualBalance)
        },
        modifier = modifier
    )
}

@Composable
fun FinanceDashboardContent(
    uiState: FinanceUiState,
    onAddTransaction: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: String,
        accountId: Long,
        accountName: String,
        notes: String
    ) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onUpdateTransaction: (oldTransaction: TransactionEntity, newTransaction: TransactionEntity) -> Unit = { _, _ -> },
    onTransferFunds: (fromAccount: AccountEntity, toAccount: AccountEntity, amount: Double, notes: String, date: String) -> Unit = { _, _, _, _, _ -> },
    onAddAccount: (name: String, type: String, initialBalance: Double) -> Unit = { _, _, _ -> },
    onReconcileAccount: (account: AccountEntity, actualBalance: Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var reconcilingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedFilterTab by remember { mutableStateOf("ALL") }

    val currentMonthText = remember {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date()).uppercase()
    }

    val filteredTransactions = remember(uiState.transactions, selectedFilterTab) {
        when (selectedFilterTab) {
            "EXPENSE" -> uiState.transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            "INCOME" -> uiState.transactions.filter { it.type.equals("INCOME", ignoreCase = true) }
            else -> uiState.transactions
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RetroCanvas,
        floatingActionButton = {
            DashboardFab(onClick = { showAddDialog = true })
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "RINGKASAN KEUANGAN",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = "HALO, AJI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp,
                            color = Color.Black
                        )
                    )
                }
            }

            item {
                DashboardBalanceSection(
                    totalBalance = uiState.totalBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    currentMonthText = currentMonthText
                )
            }

            item {
                DashboardAccountsSection(
                    accounts = uiState.accounts,
                    onTransferClick = { showTransferDialog = true },
                    onAddAccountClick = { showAddAccountDialog = true },
                    onAccountClick = { reconcilingAccount = it }
                )
            }

            item {
                DashboardTransactionsSection(
                    transactions = filteredTransactions,
                    selectedFilterTab = selectedFilterTab,
                    onFilterTabSelected = { selectedFilterTab = it },
                    onDeleteTransaction = onDeleteTransaction,
                    onEditTransaction = { editingTransaction = it }
                )
            }
        }
    }

    if (showAddDialog) {
        NeobrutalFastAddDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onSave = onAddTransaction
        )
    }

    if (showTransferDialog) {
        NeobrutalTransferDialog(
            accounts = uiState.accounts,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromAcc, toAcc, amount, notes, date ->
                onTransferFunds(fromAcc, toAcc, amount, notes, date)
                showTransferDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        NeobrutalAddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, type, initialBalance ->
                onAddAccount(name, type, initialBalance)
                showAddAccountDialog = false
            }
        )
    }

    reconcilingAccount?.let { accToReconcile ->
        NeobrutalReconcileDialog(
            account = accToReconcile,
            onDismiss = { reconcilingAccount = null },
            onReconcile = { account, actualBalance ->
                onReconcileAccount(account, actualBalance)
                reconcilingAccount = null
            }
        )
    }

    editingTransaction?.let { txToEdit ->
        NeobrutalEditTransactionDialog(
            transaction = txToEdit,
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTx ->
                onUpdateTransaction(txToEdit, updatedTx)
                editingTransaction = null
            }
        )
    }
}

@Composable
private fun DashboardFab(onClick: () -> Unit) {
    Box(modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(Color.Black, RectangleShape)
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(RetroYellow, RectangleShape)
                .border(3.dp, Color.Black, RectangleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Transaksi",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
