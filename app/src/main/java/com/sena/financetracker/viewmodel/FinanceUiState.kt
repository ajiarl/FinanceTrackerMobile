package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.TransactionEntity

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

fun calculateFinanceTotals(transactions: List<TransactionEntity>): FinanceUiState {
    val totalIncome = transactions
        .filter { it.type.equals("INCOME", ignoreCase = true) }
        .sumOf { it.amount }

    val totalExpense = transactions
        .filter { it.type.equals("EXPENSE", ignoreCase = true) }
        .sumOf { it.amount }

    val totalBalance = totalIncome - totalExpense

    return FinanceUiState(
        transactions = transactions,
        totalBalance = totalBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense
    )
}
