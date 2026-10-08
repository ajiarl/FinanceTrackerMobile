package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryEntity
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

    @Test
    fun testAccountsBalanceCalculation() {
        val accounts = listOf(
            AccountEntity(id = 1, name = "Dompet Tunai", type = "cash", balance = 500000.0),
            AccountEntity(id = 2, name = "BCA", type = "bank", balance = 5000000.0),
            AccountEntity(id = 3, name = "GoPay", type = "e-wallet", balance = 250000.0)
        )
        val transactions = listOf(
            TransactionEntity(id = 1, title = "Makan", amount = 50000.0, type = "EXPENSE", category = "Makanan", date = "2026-10-01")
        )

        val state = calculateFinanceTotals(transactions, accounts)

        // totalBalance should sum the account balances when accounts exist
        assertEquals(5750000.0, state.totalBalance, 0.001)
        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(50000.0, state.totalExpense, 0.001)
        assertEquals(3, state.accounts.size)
    }

    @Test
    fun testCategoriesIntegration() {
        val categories = listOf(
            CategoryEntity(id = 1, name = "Makanan & Minuman", type = "EXPENSE", color = "#F97316"),
            CategoryEntity(id = 2, name = "Gaji", type = "INCOME", color = "#22C55E")
        )
        val state = calculateFinanceTotals(emptyList(), emptyList(), categories)

        assertEquals(2, state.categories.size)
        assertEquals("Makanan & Minuman", state.categories[0].name)
        assertEquals("Gaji", state.categories[1].name)
    }

    @Test
    fun testAiInsightPropertiesInState() {
        val state = calculateFinanceTotals(
            transactions = emptyList(),
            aiInsightText = "Hemat 50% bos!",
            isAiInsightLoading = false,
            aiInsightError = null
        )

        assertEquals("Hemat 50% bos!", state.aiInsightText)
        assertEquals(false, state.isAiInsightLoading)
        assertEquals(null, state.aiInsightError)
        assertEquals(true, state.isHapticEnabled)
    }

    @Test
    fun testCalculateFinanceTotalsWithHapticDisabled() {
        val state = calculateFinanceTotals(
            transactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            isHapticEnabled = false
        )

        assertEquals(false, state.isHapticEnabled)
    }

    @Test
    fun testCalculateFinanceTotalsWithHasApiKeyFlag() {
        val stateWithKey = calculateFinanceTotals(
            transactions = emptyList(),
            hasApiKey = true
        )
        assertEquals(true, stateWithKey.hasApiKey)

        val stateWithoutKey = calculateFinanceTotals(
            transactions = emptyList(),
            hasApiKey = false
        )
        assertEquals(false, stateWithoutKey.hasApiKey)
    }
}
