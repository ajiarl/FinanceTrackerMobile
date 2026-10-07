package com.sena.financetracker.ui.dashboard.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.dashboard.FinanceDashboardContent
import com.sena.financetracker.viewmodel.FinanceUiState

object DashboardPreviewData {
    val dummyAccounts = listOf(
        AccountEntity(id = 1, name = "BCA", type = "bank", balance = 2500000.0),
        AccountEntity(id = 2, name = "Dompet Tunai", type = "cash", balance = 350000.0),
        AccountEntity(id = 3, name = "GoPay", type = "e-wallet", balance = 400000.0)
    )

    val dummyTransactions = listOf(
        TransactionEntity(
            id = 1,
            title = "Gaji Project",
            amount = 4500000.0,
            type = "INCOME",
            category = "Freelance",
            date = "2026-10-01",
            accountId = 1,
            accountName = "BCA"
        ),
        TransactionEntity(
            id = 2,
            title = "Makan Siang & Kopi",
            amount = 45000.0,
            type = "EXPENSE",
            category = "Makanan",
            date = "2026-10-02",
            accountId = 2,
            accountName = "Dompet Tunai"
        ),
        TransactionEntity(
            id = 3,
            title = "Beli Token Listrik",
            amount = 100000.0,
            type = "EXPENSE",
            category = "Tagihan",
            date = "2026-10-03",
            accountId = 3,
            accountName = "GoPay"
        ),
        TransactionEntity(
            id = 4,
            title = "Transfer ke GoPay",
            amount = 150000.0,
            type = "TRANSFER",
            category = "Transfer",
            date = "2026-10-04",
            accountId = 1,
            accountName = "BCA",
            notes = "Topup saldo GoPay",
            toAccountId = 3,
            toAccountName = "GoPay"
        )
    )

    val dummyUiState = FinanceUiState(
        transactions = dummyTransactions,
        accounts = dummyAccounts,
        totalBalance = 3250000.0,
        totalIncome = 4500000.0,
        totalExpense = 1250000.0,
        isLoading = false
    )
}

@Preview(
    name = "Finance Tracker Neobrutal Phone",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun FinanceDashboardPreview() {
    FinanceDashboardContent(
        uiState = DashboardPreviewData.dummyUiState,
        onAddTransaction = { _, _, _, _, _, _, _, _ -> },
        onDeleteTransaction = {}
    )
}
