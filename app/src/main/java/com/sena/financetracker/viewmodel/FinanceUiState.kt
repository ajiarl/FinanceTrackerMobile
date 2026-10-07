package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model representasi keadaan UI (UI State) yang immutabel untuk layar utama dashboard keuangan.
 *
 * @property transactions Daftar lengkap seluruh entitas transaksi yang tersimpan.
 * @property filteredTransactions Daftar transaksi terisolasi hasil penerapan multi-kriteria filter (search, category, date, type).
 * @property accounts Daftar rekening dan dompet keuangan pengguna beserta saldo masing-masing.
 * @property categories Daftar kategori pemasukan dan pengeluaran.
 * @property budgets Ringkasan progres anggaran kategori dan status overbudget alert ("SAFE", "WARNING", "CRITICAL").
 * @property totalBalance Total saldo bersih kekayaan pengguna (jumlah saldo seluruh rekening aktif).
 * @property totalIncome Total seluruh pemasukan riil dari transaksi bertipe "INCOME".
 * @property totalExpense Total seluruh pengeluaran riil dari transaksi bertipe "EXPENSE".
 * @property isLoading Indikator pemuatan data awal dari database.
 * @property errorMessage Pesan galat operasional jika terjadi pengecualian sistem.
 * @property searchQuery Kata kunci pencarian interaktif (judul, kategori, catatan).
 * @property selectedCategoryFilter Filter kategori transaksi terpilih (null = semua kategori).
 * @property selectedDateFilter Filter rentang waktu ("ALL", "TODAY", "THIS_MONTH").
 * @property selectedFilterTab Filter tab jenis transaksi ("ALL", "EXPENSE", "INCOME").
 */
data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = transactions,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetProgressItem> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val selectedDateFilter: String = "ALL", // "ALL", "TODAY", "THIS_MONTH"
    val selectedFilterTab: String = "ALL" // "ALL", "EXPENSE", "INCOME"
)

/**
 * Menyaring daftar transaksi secara murni (pure function) berdasarkan multi-kriteria filter:
 * 1. Text Search: Mencocokkan query pencarian ke judul, kategori, atau catatan transaksi.
 * 2. Type Tab: Menyaring berdasarkan jenis transaksi ("EXPENSE" / "INCOME").
 * 3. Kategori: Menyaring berdasarkan nama kategori spesifik.
 * 4. Rentang Tanggal: Menyaring transaksi hari ini ("TODAY") atau bulan ini ("THIS_MONTH").
 *
 * @return Daftar transaksi baru yang terisolasi sesuai kriteria pencarian tanpa mengubah list transaksi master.
 */
fun filterTransactions(
    transactions: List<TransactionEntity>,
    searchQuery: String = "",
    filterTab: String = "ALL",
    categoryFilter: String? = null,
    dateFilter: String = "ALL",
    currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
): List<TransactionEntity> {
    val trimmedQuery = searchQuery.trim()
    val today = currentDate.take(10)
    val thisMonth = if (currentDate.length >= 7) currentDate.take(7) else ""

    return transactions.filter { tx ->
        // 1. Text Search Filter (Title, Category, Notes)
        val matchesQuery = if (trimmedQuery.isEmpty()) {
            true
        } else {
            tx.title.contains(trimmedQuery, ignoreCase = true) ||
            tx.category.contains(trimmedQuery, ignoreCase = true) ||
            tx.notes.contains(trimmedQuery, ignoreCase = true)
        }

        // 2. Type / Tab Filter
        val matchesType = when (filterTab.uppercase()) {
            "EXPENSE" -> tx.type.equals("EXPENSE", ignoreCase = true)
            "INCOME" -> tx.type.equals("INCOME", ignoreCase = true)
            else -> true
        }

        // 3. Category Filter
        val matchesCategory = if (categoryFilter.isNullOrBlank() || categoryFilter.equals("ALL", ignoreCase = true)) {
            true
        } else {
            tx.category.equals(categoryFilter, ignoreCase = true)
        }

        // 4. Date Filter
        val matchesDate = when (dateFilter.uppercase()) {
            "TODAY" -> tx.date.take(10) == today
            "THIS_MONTH" -> tx.date.take(7) == thisMonth
            else -> true
        }

        matchesQuery && matchesType && matchesCategory && matchesDate
    }
}

/**
 * Menghitung kalkulasi agregat total keuangan dan menghasilkan instance baru [FinanceUiState].
 *
 * Logika Keuangan:
 * - `totalIncome`: Menjumlahkan nominal seluruh transaksi bertipe "INCOME".
 * - `totalExpense`: Menjumlahkan nominal seluruh transaksi bertipe "EXPENSE".
 * - `totalBalance`: Jika terdapat daftar akun/rekening, saldo dihitung dari akumulasi `accounts.sumOf { it.balance }`.
 *   Jika belum ada akun terdaftar, fallback menggunakan kalkulasi arus kas: `totalIncome - totalExpense`.
 * - `filteredTransactions`: Dihasilkan melalui fungsi [filterTransactions] sehingga pemisahan list master
 *   dan list yang dirender pada UI transaksi tetap terisolasi secara aman.
 */
fun calculateFinanceTotals(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    budgets: List<BudgetProgressItem> = emptyList(),
    searchQuery: String = "",
    selectedCategoryFilter: String? = null,
    selectedDateFilter: String = "ALL",
    selectedFilterTab: String = "ALL",
    currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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

    val filtered = filterTransactions(
        transactions = transactions,
        searchQuery = searchQuery,
        filterTab = selectedFilterTab,
        categoryFilter = selectedCategoryFilter,
        dateFilter = selectedDateFilter,
        currentDate = currentDate
    )

    return FinanceUiState(
        transactions = transactions,
        filteredTransactions = filtered,
        accounts = accounts,
        categories = categories,
        budgets = budgets,
        totalBalance = totalBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        searchQuery = searchQuery,
        selectedCategoryFilter = selectedCategoryFilter,
        selectedDateFilter = selectedDateFilter,
        selectedFilterTab = selectedFilterTab
    )
}
