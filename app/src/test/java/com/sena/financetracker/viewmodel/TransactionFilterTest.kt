package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFilterTest {

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = 1,
            title = "Makan Siang Soto",
            amount = 35000.0,
            type = "EXPENSE",
            category = "Makanan",
            date = "2026-10-07",
            accountId = 1,
            accountName = "Dompet Tunai",
            notes = "Makan siang di warung soto"
        ),
        TransactionEntity(
            id = 2,
            title = "Beli Kopi Arabika",
            amount = 25000.0,
            type = "EXPENSE",
            category = "Makanan",
            date = "2026-10-01",
            accountId = 1,
            accountName = "Dompet Tunai",
            notes = "Kopi pagi"
        ),
        TransactionEntity(
            id = 3,
            title = "Gaji Freelance Web",
            amount = 5000000.0,
            type = "INCOME",
            category = "Gaji",
            date = "2026-10-07",
            accountId = 2,
            accountName = "BCA",
            notes = "Pembayaran milestone 1"
        ),
        TransactionEntity(
            id = 4,
            title = "Beli Baju Kaos",
            amount = 150000.0,
            type = "EXPENSE",
            category = "Pakaian",
            date = "2026-09-20",
            accountId = 2,
            accountName = "BCA",
            notes = "Diskon akhir bulan lalu"
        )
    )

    @Test
    fun testSearchFilterByTitleAndNotes() {
        // Query cocok dengan Title
        val searchByTitle = filterTransactions(
            transactions = sampleTransactions,
            searchQuery = "Kopi"
        )
        assertEquals(1, searchByTitle.size)
        assertEquals("Beli Kopi Arabika", searchByTitle[0].title)

        // Query cocok dengan Notes
        val searchByNotes = filterTransactions(
            transactions = sampleTransactions,
            searchQuery = "milestone"
        )
        assertEquals(1, searchByNotes.size)
        assertEquals("Gaji Freelance Web", searchByNotes[0].title)

        // Query tidak cocok
        val searchNotFound = filterTransactions(
            transactions = sampleTransactions,
            searchQuery = "Kucing"
        )
        assertTrue(searchNotFound.isEmpty())
    }

    @Test
    fun testSearchFilterCaseInsensitiveAndTrim() {
        // Query huruf kecil dan spasi berlebih
        val result = filterTransactions(
            transactions = sampleTransactions,
            searchQuery = "  soto  "
        )
        assertEquals(1, result.size)
        assertEquals("Makan Siang Soto", result[0].title)
    }

    @Test
    fun testCombinationTypeAndCategoryFilter() {
        // Filter EXPENSE + Makanan -> Soto dan Kopi
        val expenseFood = filterTransactions(
            transactions = sampleTransactions,
            filterTab = "EXPENSE",
            categoryFilter = "Makanan"
        )
        assertEquals(2, expenseFood.size)
        assertTrue(expenseFood.all { it.type == "EXPENSE" && it.category == "Makanan" })

        // Filter INCOME + Makanan -> Kosong
        val incomeFood = filterTransactions(
            transactions = sampleTransactions,
            filterTab = "INCOME",
            categoryFilter = "Makanan"
        )
        assertTrue(incomeFood.isEmpty())

        // Filter INCOME + Gaji -> Gaji Freelance Web
        val incomeSalary = filterTransactions(
            transactions = sampleTransactions,
            filterTab = "INCOME",
            categoryFilter = "Gaji"
        )
        assertEquals(1, incomeSalary.size)
        assertEquals("Gaji Freelance Web", incomeSalary[0].title)
    }

    @Test
    fun testDateFilterTodayVsThisMonth() {
        val referenceToday = "2026-10-07"

        // Filter TODAY: Hanya transaksi tanggal 2026-10-07 (id 1 dan 3)
        val todayResults = filterTransactions(
            transactions = sampleTransactions,
            dateFilter = "TODAY",
            currentDate = referenceToday
        )
        assertEquals(2, todayResults.size)
        assertTrue(todayResults.all { it.date == "2026-10-07" })

        // Filter THIS_MONTH: Transaksi bulan Oktober 2026 (id 1, 2, 3), exclude September (id 4)
        val thisMonthResults = filterTransactions(
            transactions = sampleTransactions,
            dateFilter = "THIS_MONTH",
            currentDate = referenceToday
        )
        assertEquals(3, thisMonthResults.size)
        assertTrue(thisMonthResults.none { it.id == 4L })

        // Filter ALL: Semua transaksi (4)
        val allResults = filterTransactions(
            transactions = sampleTransactions,
            dateFilter = "ALL",
            currentDate = referenceToday
        )
        assertEquals(4, allResults.size)
    }

    @Test
    fun testCalculateFinanceTotalsPreservesOverallBalanceWhileFilteringTransactions() {
        val accounts = listOf(
            AccountEntity(id = 1, name = "Dompet Tunai", type = "cash", balance = 50000.0),
            AccountEntity(id = 2, name = "BCA", type = "bank", balance = 5000000.0)
        )
        val categories = listOf(
            CategoryEntity(id = 1, name = "Makanan", type = "EXPENSE"),
            CategoryEntity(id = 2, name = "Gaji", type = "INCOME")
        )

        val state = calculateFinanceTotals(
            transactions = sampleTransactions,
            accounts = accounts,
            categories = categories,
            searchQuery = "Soto",
            selectedCategoryFilter = "Makanan",
            selectedDateFilter = "TODAY",
            selectedFilterTab = "EXPENSE",
            currentDate = "2026-10-07"
        )

        // Filtered transactions hanya menyisakan Soto
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("Makan Siang Soto", state.filteredTransactions[0].title)

        // Raw transactions tetap utuh 4 item
        assertEquals(4, state.transactions.size)

        // Total ringkasan keuangan tetap menghitung total keseluruhan akun dan transaksi
        assertEquals(5050000.0, state.totalBalance, 0.001)
        assertEquals(5000000.0, state.totalIncome, 0.001)
        assertEquals(210000.0, state.totalExpense, 0.001)
        assertEquals("Soto", state.searchQuery)
        assertEquals("Makanan", state.selectedCategoryFilter)
        assertEquals("TODAY", state.selectedDateFilter)
        assertEquals("EXPENSE", state.selectedFilterTab)
    }
}
