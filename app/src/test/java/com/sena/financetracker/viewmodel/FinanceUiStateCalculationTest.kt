package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceUiStateCalculationTest {

    @Test
    fun testEmptyTransactionsCalculation() {
        val transactions = emptyList<TransactionEntity>()
        val state = calculateFinanceTotals(transactions)

        assertEquals(0.0, state.totalBalance, 0.001)
        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(0.0, state.totalExpense, 0.001)
    }

    @Test
    fun testMultipleTransactionsCalculation() {
        val transactions = listOf(
            TransactionEntity(id = 1, title = "Gaji Bulanan", amount = 10000000.0, type = "INCOME", category = "Gaji", date = "2026-10-01"),
            TransactionEntity(id = 2, title = "Makan Siang", amount = 50000.0, type = "EXPENSE", category = "Makanan", date = "2026-10-02"),
            TransactionEntity(id = 3, title = "Beli Kopi", amount = 25000.0, type = "EXPENSE", category = "Minuman", date = "2026-10-03"),
            TransactionEntity(id = 4, title = "Freelance Project", amount = 2500000.0, type = "INCOME", category = "Side Hustle", date = "2026-10-04")
        )
        val state = calculateFinanceTotals(transactions)

        assertEquals(12500000.0, state.totalIncome, 0.001)
        assertEquals(75000.0, state.totalExpense, 0.001)
        assertEquals(12425000.0, state.totalBalance, 0.001)
    }
}
