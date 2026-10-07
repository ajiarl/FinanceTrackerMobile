package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportsAnalyticsCalculationTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.MONTH, Calendar.OCTOBER)
        set(Calendar.DAY_OF_MONTH, 7)
    }
    private val refDate = cal.time // 2026-10-07

    private val testCategories = listOf(
        CategoryEntity(id = 1, name = "Makanan", type = "EXPENSE", color = "#F97316"),
        CategoryEntity(id = 2, name = "Transportasi", type = "EXPENSE", color = "#3B82F6"),
        CategoryEntity(id = 3, name = "Gaji", type = "INCOME", color = "#10B981")
    )

    @Test
    fun testEmptyTransactionsReports() {
        val state = calculateReportsAnalytics(
            transactions = emptyList(),
            categories = testCategories,
            preset = "THIS_MONTH",
            referenceDate = refDate
        )

        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(0.0, state.netSavings, 0.001)
        assertEquals(0, state.savingRate)
        assertEquals("NORMAL", state.savingStatus)
        assertTrue(state.categoryBreakdown.isEmpty())
        assertEquals(3, state.cashflowBars.size)
    }

    @Test
    fun testSavingRateHematCalculation() {
        val transactions = listOf(
            TransactionEntity(id = 1, title = "Gaji", amount = 10000000.0, type = "INCOME", category = "Gaji", date = "2026-10-01"),
            TransactionEntity(id = 2, title = "Makan Siang", amount = 2000000.0, type = "EXPENSE", category = "Makanan", date = "2026-10-02")
        )
        val state = calculateReportsAnalytics(
            transactions = transactions,
            categories = testCategories,
            preset = "THIS_MONTH",
            referenceDate = refDate
        )

        assertEquals(10000000.0, state.totalIncome, 0.001)
        assertEquals(2000000.0, state.totalExpense, 0.001)
        assertEquals(8000000.0, state.netSavings, 0.001)
        assertEquals(80, state.savingRate) // (8jt / 10jt) * 100 = 80%
        assertEquals("HEMAT", state.savingStatus) // >= 30%
    }

    @Test
    fun testSavingRateBorosCalculation() {
        val transactions = listOf(
            TransactionEntity(id = 1, title = "Freelance", amount = 1000000.0, type = "INCOME", category = "Gaji", date = "2026-10-01"),
            TransactionEntity(id = 2, title = "Belanja Gadget", amount = 2000000.0, type = "EXPENSE", category = "Elektronik", date = "2026-10-02")
        )
        val state = calculateReportsAnalytics(
            transactions = transactions,
            categories = testCategories,
            preset = "THIS_MONTH",
            referenceDate = refDate
        )

        assertEquals(1000000.0, state.totalIncome, 0.001)
        assertEquals(2000000.0, state.totalExpense, 0.001)
        assertEquals(-1000000.0, state.netSavings, 0.001)
        assertTrue(state.savingRate < 10)
        assertEquals("BOROS", state.savingStatus)
    }

    @Test
    fun testCategoryBreakdownSortingAndPercentage() {
        val transactions = listOf(
            TransactionEntity(id = 1, title = "Makan 1", amount = 600000.0, type = "EXPENSE", category = "Makanan", date = "2026-10-01"),
            TransactionEntity(id = 2, title = "Bensin", amount = 300000.0, type = "EXPENSE", category = "Transportasi", date = "2026-10-02"),
            TransactionEntity(id = 3, title = "Kopi", amount = 100000.0, type = "EXPENSE", category = "Makanan", date = "2026-10-03")
        )
        val state = calculateReportsAnalytics(
            transactions = transactions,
            categories = testCategories,
            preset = "THIS_MONTH",
            referenceDate = refDate
        )

        assertEquals(1000000.0, state.totalExpense, 0.001)
        assertEquals(2, state.categoryBreakdown.size)

        // Top category: Makanan = 700.000 (70%)
        val topCategory = state.categoryBreakdown[0]
        assertEquals("Makanan", topCategory.category)
        assertEquals(700000.0, topCategory.totalAmount, 0.001)
        assertEquals(70, topCategory.percentage)
        assertEquals("#F97316", topCategory.color)

        // Second category: Transportasi = 300.000 (30%)
        val secondCategory = state.categoryBreakdown[1]
        assertEquals("Transportasi", secondCategory.category)
        assertEquals(300000.0, secondCategory.totalAmount, 0.001)
        assertEquals(30, secondCategory.percentage)
    }

    @Test
    fun testPeriodPresetFiltering() {
        val transactions = listOf(
            TransactionEntity(id = 1, title = "Okt Income", amount = 5000000.0, type = "INCOME", category = "Gaji", date = "2026-10-01"),
            TransactionEntity(id = 2, title = "Sep Income", amount = 4000000.0, type = "INCOME", category = "Gaji", date = "2026-09-15"),
            TransactionEntity(id = 3, title = "Agt Income", amount = 3000000.0, type = "INCOME", category = "Gaji", date = "2026-08-10"),
            TransactionEntity(id = 4, title = "Jul Income", amount = 2000000.0, type = "INCOME", category = "Gaji", date = "2026-07-05")
        )

        // 1. THIS_MONTH (2026-10)
        val thisMonthState = calculateReportsAnalytics(transactions, testCategories, "THIS_MONTH", refDate)
        assertEquals(5000000.0, thisMonthState.totalIncome, 0.001)

        // 2. LAST_MONTH (2026-09)
        val lastMonthState = calculateReportsAnalytics(transactions, testCategories, "LAST_MONTH", refDate)
        assertEquals(4000000.0, lastMonthState.totalIncome, 0.001)

        // 3. LAST_3_MONTHS (2026-10, 2026-09, 2026-08) = 5jt + 4jt + 3jt = 12jt
        val last3MonthsState = calculateReportsAnalytics(transactions, testCategories, "LAST_3_MONTHS", refDate)
        assertEquals(12000000.0, last3MonthsState.totalIncome, 0.001)

        // 4. ALL_TIME (semua) = 5jt + 4jt + 3jt + 2jt = 14jt
        val allTimeState = calculateReportsAnalytics(transactions, testCategories, "ALL_TIME", refDate)
        assertEquals(14000000.0, allTimeState.totalIncome, 0.001)
    }
}
