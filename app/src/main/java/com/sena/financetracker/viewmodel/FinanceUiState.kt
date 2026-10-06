package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

fun calculateFinanceTotals(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList()
): FinanceUiState {
    val totalIncome = transactions
        .filter { it.type.equals("INCOME", ignoreCase = true) }
        .sumOf { it.amount }

    val totalExpense = transactions
        .filter { it.type.equals("EXPENSE", ignoreCase = true) }
        .sumOf { it.amount }

    val totalBalance = if (accounts.isNotEmpty()) {
        accounts.sumOf { it.balance }
    } else {
        totalIncome - totalExpense
    }

    return FinanceUiState(
        transactions = transactions,
        accounts = accounts,
        categories = categories,
        totalBalance = totalBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense
    )
}
