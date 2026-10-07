package com.sena.financetracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalFastAddDialog
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.NeobrutalAddAccountDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalAddBudgetDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalConfirmDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalEditTransactionDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalReconcileDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalTransferDialog
import com.sena.financetracker.ui.dashboard.screens.AccountsScreen
import com.sena.financetracker.ui.dashboard.screens.CategoriesScreen
import com.sena.financetracker.ui.dashboard.screens.BudgetsScreen
import com.sena.financetracker.ui.dashboard.screens.HomeScreen
import com.sena.financetracker.ui.dashboard.screens.ReportsScreen
import com.sena.financetracker.ui.dashboard.screens.TransactionsScreen
import com.sena.financetracker.ui.navigation.NeobrutalBottomNav
import com.sena.financetracker.ui.navigation.Screen
import com.sena.financetracker.util.formatRupiah
import com.sena.financetracker.viewmodel.FinanceUiState
import com.sena.financetracker.viewmodel.FinanceViewModel

/**
 * Layar utama aplikasi dengan Scaffolding Neobrutalisme dan 4-Tab Bottom Navigation Bar terpadu.
 *
 * Mengintegrasikan:
 * - NavHost dengan 4 tab: Beranda, Transaksi, Anggaran, Akun.
 * - State sharing reaktif terpusat melalui [FinanceViewModel].
 * - Dialog modal atomik (Fast Add, Transfer, Akun Baru, Budget, Edit Tx, dan Konfirmasi Hapus).
 */
@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    FinanceDashboardContent(
        uiState = uiState,
        navController = navController,
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
        onAddBudget = { name, category, limitAmount ->
            viewModel.addBudget(name, category, limitAmount)
        },
        onDeleteBudget = { id ->
            viewModel.deleteBudget(id)
        },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onFilterTabSelected = { viewModel.setSelectedFilterTab(it) },
        onDateFilterSelected = { viewModel.setSelectedDateFilter(it) },
        onCategoryFilterSelected = { viewModel.setSelectedCategoryFilter(it) },
        onResetFilters = { viewModel.clearFilters() },
        onReportsPresetSelected = { viewModel.setReportsPeriodPreset(it) },
        onAddCategory = { name, type, color ->
            viewModel.addCategory(name, type, color)
        },
        onUpdateCategory = { id, name, type, color ->
            viewModel.updateCategory(id, name, type, color)
        },
        onDeleteCategory = { id ->
            viewModel.deleteCategory(id)
        },
        modifier = modifier
    )
}

@Composable
fun FinanceDashboardContent(
    uiState: FinanceUiState,
    navController: NavHostController = rememberNavController(),
    onAddTransaction: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: String,
        accountId: Long,
        accountName: String,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onUpdateTransaction: (oldTransaction: TransactionEntity, newTransaction: TransactionEntity) -> Unit = { _, _ -> },
    onTransferFunds: (fromAccount: AccountEntity, toAccount: AccountEntity, amount: Double, notes: String, date: String) -> Unit = { _, _, _, _, _ -> },
    onAddAccount: (name: String, type: String, initialBalance: Double) -> Unit = { _, _, _ -> },
    onReconcileAccount: (account: AccountEntity, actualBalance: Double) -> Unit = { _, _ -> },
    onAddBudget: (name: String, category: String, limitAmount: Double) -> Unit = { _, _, _ -> },
    onDeleteBudget: (Long) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onFilterTabSelected: (String) -> Unit = {},
    onDateFilterSelected: (String) -> Unit = {},
    onCategoryFilterSelected: (String?) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onReportsPresetSelected: (String) -> Unit = {},
    onAddCategory: (name: String, type: String, color: String) -> Unit = { _, _, _ -> },
    onUpdateCategory: (id: Long, name: String, type: String, color: String) -> Unit = { _, _, _, _ -> },
    onDeleteCategory: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var reconcilingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // State untuk konfirmasi hapus Neobrutal (Safety UX)
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetProgressItem?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RetroCanvas,
        bottomBar = {
            NeobrutalBottomNav(
                currentRoute = currentRoute,
                onTabSelected = { targetScreen ->
                    if (currentRoute != targetScreen.route) {
                        navController.navigate(targetScreen.route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            DashboardFab(onClick = { showAddDialog = true })
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Beranda
            composable(Screen.Dashboard.route) {
                HomeScreen(
                    uiState = uiState,
                    onTransferClick = { showTransferDialog = true },
                    onAddAccountClick = { showAddAccountDialog = true },
                    onAccountClick = { reconcilingAccount = it },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Tab 2: Transaksi
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    uiState = uiState,
                    onFilterTabSelected = onFilterTabSelected,
                    onSearchQueryChange = onSearchQueryChange,
                    onDateFilterSelected = onDateFilterSelected,
                    onCategoryFilterSelected = onCategoryFilterSelected,
                    onResetFilters = onResetFilters,
                    onDeleteTransaction = { tx -> transactionToDelete = tx },
                    onEditTransaction = { tx -> editingTransaction = tx }
                )
            }

            // Tab 3: Anggaran
            composable(Screen.Budgets.route) {
                BudgetsScreen(
                    uiState = uiState,
                    onAddBudgetClick = { showAddBudgetDialog = true },
                    onDeleteBudgetClick = { budgetId ->
                        val item = uiState.budgets.find { it.budget.id == budgetId }
                        budgetToDelete = item
                    }
                )
            }

            // Tab 4: Akun & Rekening
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    uiState = uiState,
                    onAddAccountClick = { showAddAccountDialog = true },
                    onTransferClick = { showTransferDialog = true },
                    onAccountClick = { reconcilingAccount = it },
                    onManageCategoriesClick = {
                        navController.navigate(Screen.Categories.route)
                    }
                )
            }

            // Tab 5: Laporan & Grafik Analisis
            composable(Screen.Reports.route) {
                ReportsScreen(
                    uiState = uiState,
                    onPresetSelected = onReportsPresetSelected
                )
            }

            // Sub-screen: Kelola Kategori
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onAddCategory = onAddCategory,
                    onUpdateCategory = onUpdateCategory,
                    onDeleteCategory = onDeleteCategory
                )
            }
        }
    }

    // Modal Dialog Fast Add Transaksi
    if (showAddDialog) {
        NeobrutalFastAddDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onManageCategoriesClick = {
                navController.navigate(Screen.Categories.route)
            },
            onSave = onAddTransaction
        )
    }

    // Modal Dialog Transfer
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

    // Modal Dialog Tambah Akun
    if (showAddAccountDialog) {
        NeobrutalAddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, type, initialBalance ->
                onAddAccount(name, type, initialBalance)
                showAddAccountDialog = false
            }
        )
    }

    // Modal Dialog Rekonsiliasi
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

    // Modal Dialog Tambah Budget
    if (showAddBudgetDialog) {
        NeobrutalAddBudgetDialog(
            categories = uiState.categories,
            onDismiss = { showAddBudgetDialog = false },
            onSaveBudget = { name, category, limitAmount ->
                onAddBudget(name, category, limitAmount)
                showAddBudgetDialog = false
            }
        )
    }

    // Modal Dialog Edit Transaksi
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

    // Safety UX: Dialog Konfirmasi Hapus Transaksi
    transactionToDelete?.let { tx ->
        NeobrutalConfirmDialog(
            title = "HAPUS TRANSAKSI?",
            message = "Apakah kamu yakin ingin menghapus transaksi \"${tx.title}\" senilai Rp ${formatRupiah(tx.amount)}? Saldo rekening terkait akan otomatis disesuaikan kembali.",
            confirmButtonText = "HAPUS",
            cancelButtonText = "BATAL",
            onConfirm = {
                onDeleteTransaction(tx)
                transactionToDelete = null
            },
            onDismiss = {
                transactionToDelete = null
            }
        )
    }

    // Safety UX: Dialog Konfirmasi Hapus Anggaran
    budgetToDelete?.let { item ->
        NeobrutalConfirmDialog(
            title = "HAPUS ANGGARAN?",
            message = "Apakah kamu yakin ingin menghapus anggaran \"${item.budget.name}\" (${item.budget.category}) dengan limit Rp ${formatRupiah(item.budget.limitAmount)}? Data riwayat transaksi kamu tidak akan terhapus.",
            confirmButtonText = "HAPUS",
            cancelButtonText = "BATAL",
            onConfirm = {
                onDeleteBudget(item.budget.id)
                budgetToDelete = null
            },
            onDismiss = {
                budgetToDelete = null
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
