package com.sena.financetracker.ui.dashboard

import com.sena.financetracker.util.formatCompactAmount
import com.sena.financetracker.viewmodel.CashflowBarItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pengujian unit untuk logika perhitungan dan penyajian grafik arus kas (CashflowChart).
 *
 * Menguji kalkulasi ambang batas label (threshold), penentuan label dominan saat terjadi
 * benturan horizontal (horizontal collision), serta format ringkas mata uang (compact formatting).
 */
class CashflowChartLogicTest {

    @Test
    fun testCompactAmountFormatting() {
        assertEquals("0", formatCompactAmount(0.0))
        assertEquals("500", formatCompactAmount(500.0))
        assertEquals("450rb", formatCompactAmount(450_000.0))
        assertEquals("1.5jt", formatCompactAmount(1_500_000.0))
        assertEquals("25jt", formatCompactAmount(25_000_000.0))
        assertEquals("1.2M", formatCompactAmount(1_200_000_000.0))
    }

    @Test
    fun testLabelThresholdFiltering_SingleExpenseMonth() {
        // Simulasi bulan Oktober: Income Rp 0, Expense Rp 450.000
        val safeMax = 10_000_000.0
        val minLabelThreshold = safeMax * 0.01 // 100.000

        val item = CashflowBarItem(
            label = "Okt",
            income = 0.0,
            expense = 450_000.0
        )

        val hasIncome = item.income > 0
        val hasExpense = item.expense > 0

        val canShowIncomeLabel = hasIncome && item.income >= minLabelThreshold
        val canShowExpenseLabel = hasExpense && item.expense >= minLabelThreshold

        assertFalse("Income Rp 0 tidak boleh menampilkan label teks", canShowIncomeLabel)
        assertTrue("Expense Rp 450.000 wajib menampilkan label teks", canShowExpenseLabel)
        assertEquals("450rb", formatCompactAmount(item.expense))
    }

    @Test
    fun testLabelThresholdFiltering_InsignificantValueOmitted() {
        // Nilai di bawah 1% dari nilai maksimum tidak boleh memicu label bertumpuk
        val safeMax = 50_000_000.0
        val minLabelThreshold = safeMax * 0.01 // 500.000

        val insignificantIncome = 50_000.0 // < 500.000
        val significantExpense = 5_000_000.0

        val canShowIncomeLabel = (insignificantIncome > 0) && (insignificantIncome >= minLabelThreshold)
        val canShowExpenseLabel = (significantExpense > 0) && (significantExpense >= minLabelThreshold)

        assertFalse("Nominal kecil di bawah 1% harus diabaikan", canShowIncomeLabel)
        assertTrue("Nominal signifikan harus ditampilkan", canShowExpenseLabel)
    }

    @Test
    fun testDominantLabelSelectionOnCollision() {
        // Ketika terjadi tabrakan horizontal, nilai yang lebih besar (dominan) dipilih
        val itemIncomeDominant = CashflowBarItem(
            label = "Sep",
            income = 7_500_000.0,
            expense = 2_000_000.0
        )

        val dominantText1 = if (itemIncomeDominant.income >= itemIncomeDominant.expense) {
            formatCompactAmount(itemIncomeDominant.income)
        } else {
            formatCompactAmount(itemIncomeDominant.expense)
        }
        assertEquals("7.5jt", dominantText1)

        val itemExpenseDominant = CashflowBarItem(
            label = "Agu",
            income = 1_000_000.0,
            expense = 3_200_000.0
        )

        val dominantText2 = if (itemExpenseDominant.income >= itemExpenseDominant.expense) {
            formatCompactAmount(itemExpenseDominant.income)
        } else {
            formatCompactAmount(itemExpenseDominant.expense)
        }
        assertEquals("3.2jt", dominantText2)
    }
}
