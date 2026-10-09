package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data item komposisi pengeluaran per kategori untuk laporan analitik.
 *
 * @property category Nama kategori pengeluaran.
 * @property totalAmount Total nominal pengeluaran dalam mata uang IDR.
 * @property percentage Persentase kontribusi terhadap total pengeluaran (0 - 100).
 * @property color Warna hex kategori untuk styling bar visual.
 */
data class CategoryBreakdownItem(
    val category: String,
    val totalAmount: Double,
    val percentage: Int,
    val color: String = "#000000"
)

/**
 * Data item diagram batang arus kas per periode (bulan/minggu).
 *
 * @property label Label sumbu X periode (misal "Jul 26", "Agt 26", "Okt 26").
 * @property income Total pemasukan pada periode tersebut.
 * @property expense Total pengeluaran pada periode tersebut.
 */
data class CashflowBarItem(
    val label: String,
    val income: Double,
    val expense: Double
)

/**
 * Pilihan preset periode waktu untuk laporan analitik keuangan.
 */
enum class ReportsPreset {
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    ALL_TIME;

    companion object {
        fun fromString(value: String): ReportsPreset {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: THIS_MONTH
        }
    }
}

/**
 * Menghitung rentang tanggal [startDate, endDate] dalam format YYYY-MM-DD berdasarkan preset analitik laporan.
 *
 * @param preset Pilihan preset periode laporan ([ReportsPreset]).
 * @param referenceDate Tanggal acuan kalender (default hari ini).
 * @return Pasangan tanggal batas awal dan akhir (startDate, endDate). Untuk [ReportsPreset.ALL_TIME], bernilai Pair(null, null).
 */
fun resolveReportsDateRange(
    preset: ReportsPreset,
    referenceDate: Date = Date()
): Pair<String?, String?> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    return when (preset) {
        ReportsPreset.THIS_MONTH -> {
            val cal = Calendar.getInstance().apply { time = referenceDate }
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val start = sdf.format(cal.time)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val end = sdf.format(cal.time)
            Pair(start, end)
        }
        ReportsPreset.LAST_MONTH -> {
            val cal = Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.MONTH, -1)
            }
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val start = sdf.format(cal.time)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val end = sdf.format(cal.time)
            Pair(start, end)
        }
        ReportsPreset.LAST_3_MONTHS -> {
            val endCal = Calendar.getInstance().apply {
                time = referenceDate
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            val end = sdf.format(endCal.time)
            val startCal = Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.MONTH, -2)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val start = sdf.format(startCal.time)
            Pair(start, end)
        }
        ReportsPreset.ALL_TIME -> Pair(null, null)
    }
}

/**
 * Menghitung rentang tanggal [startDate, endDate] berdasarkan string preset.
 */
fun resolveReportsDateRange(
    preset: String,
    referenceDate: Date = Date()
): Pair<String?, String?> = resolveReportsDateRange(ReportsPreset.fromString(preset), referenceDate)

/**
 * State analitik laporan keuangan & diagram arus kas.
 *
 * @property periodPreset Pilihan filter periode laporan ("THIS_MONTH", "LAST_MONTH", "LAST_3_MONTHS", "ALL_TIME").
 * @property totalIncome Total pendapatan dalam periode terpilih.
 * @property totalExpense Total pengeluaran dalam periode terpilih.
 * @property netSavings Selisih bersih (Pendapatan - Pengeluaran).
 * @property savingRate Rasio tabungan dalam persentase (0 - 100%).
 * @property savingStatus Status kesehatan tabungan ("HEMAT", "NORMAL", "BOROS").
 * @property categoryBreakdown Daftar pengeluaran per kategori terurut dari terbesar.
 * @property cashflowBars Data diagram batang perbandingan pemasukan vs pengeluaran.
 */
data class ReportsAnalyticsState(
    val periodPreset: String = "THIS_MONTH",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingRate: Int = 0,
    val savingStatus: String = "NORMAL", // "HEMAT", "NORMAL", "BOROS"
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val cashflowBars: List<CashflowBarItem> = emptyList()
)

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
 * @property reportsAnalytics State kalkulasi laporan analitik & grafik arus kas.
 */
data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = transactions,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetProgressItem> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val selectedDateFilter: String = "ALL", // "ALL", "TODAY", "THIS_MONTH"
    val selectedFilterTab: String = "ALL", // "ALL", "EXPENSE", "INCOME"
    val reportsAnalytics: ReportsAnalyticsState = ReportsAnalyticsState(),
    val aiInsightText: String? = null,
    val isAiInsightLoading: Boolean = false,
    val aiInsightError: String? = null,
    val hasApiKey: Boolean = false,
    val isHapticEnabled: Boolean = true,
    val pageSize: Int = 50,
    val visibleTransactionCount: Int = 50,
    val hasMoreTransactions: Boolean = false
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
        val matchesCategory = if (categoryFilter.isNullOrBlank() || categoryFilter.trim().equals("ALL", ignoreCase = true)) {
            true
        } else {
            tx.category.trim().equals(categoryFilter.trim(), ignoreCase = true)
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
 * Menghitung analitik laporan keuangan murni (pure function) berdasarkan transaksi dan preset periode:
 * - "THIS_MONTH": Transaksi pada bulan berjalan ("YYYY-MM").
 * - "LAST_MONTH": Transaksi pada bulan sebelumnya ("YYYY-MM").
 * - "LAST_3_MONTHS": Transaksi pada 3 bulan kalender terakhir.
 * - "ALL_TIME": Seluruh transaksi tercatat.
 */
fun calculateReportsAnalytics(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity> = emptyList(),
    preset: String = "THIS_MONTH",
    referenceDate: Date = Date()
): ReportsAnalyticsState {
    val cal = Calendar.getInstance().apply { time = referenceDate }
    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    val thisMonthStr = sdfMonth.format(cal.time)

    val lastMonthCal = Calendar.getInstance().apply {
        time = referenceDate
        add(Calendar.MONTH, -1)
    }
    val lastMonthStr = sdfMonth.format(lastMonthCal.time)

    val last3MonthsSet = mutableSetOf<String>()
    for (i in 0..2) {
        val c = Calendar.getInstance().apply {
            time = referenceDate
            add(Calendar.MONTH, -i)
        }
        last3MonthsSet.add(sdfMonth.format(c.time))
    }

    // 1. Filter transaksi berdasarkan preset periode
    val periodTransactions = transactions.filter { tx ->
        val txMonth = if (tx.date.length >= 7) tx.date.take(7) else ""
        when (preset) {
            "THIS_MONTH" -> txMonth == thisMonthStr
            "LAST_MONTH" -> txMonth == lastMonthStr
            "LAST_3_MONTHS" -> last3MonthsSet.contains(txMonth)
            "ALL_TIME" -> true
            else -> true
        }
    }

    val periodIncome = periodTransactions
        .filter { it.type.equals("INCOME", ignoreCase = true) }
        .sumOf { it.amount }

    val periodExpense = periodTransactions
        .filter { it.type.equals("EXPENSE", ignoreCase = true) }
        .sumOf { it.amount }

    val netSavings = periodIncome - periodExpense

    // Saving Rate % = (netSavings / totalIncome) * 100 jika income > 0
    val savingRate = if (periodIncome > 0) {
        val rate = ((netSavings / periodIncome) * 100).toInt()
        rate.coerceIn(-100, 100)
    } else if (periodExpense > 0) {
        -100
    } else {
        0
    }

    val savingStatus = when {
        periodIncome == 0.0 && periodExpense == 0.0 -> "NORMAL"
        savingRate >= 30 -> "HEMAT"
        savingRate >= 10 -> "NORMAL"
        else -> "BOROS"
    }

    // 2. Breakdown Pengeluaran per Kategori
    val categoryColorMap = categories.associate { it.name.trim().lowercase(Locale.ROOT) to it.color }
    val defaultColors = listOf("#F97316", "#3B82F6", "#EC4899", "#8B5CF6", "#10B981", "#EAB308", "#64748B")

    val expenseTransactions = periodTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
    // Normalisasi case-insensitive & whitespace pada kategori pengeluaran
    val groupedByCategory = expenseTransactions.groupBy { it.category.trim().lowercase(Locale.ROOT) }

    val categoryBreakdown = groupedByCategory.map { (lowerCatName, txList) ->
        val totalCatAmount = txList.sumOf { it.amount }
        val pct = if (periodExpense > 0) {
            ((totalCatAmount / periodExpense) * 100).toInt()
        } else {
            0
        }
        val displayName = txList.firstOrNull()?.category?.trim()?.ifBlank { "Lainnya" } ?: "Lainnya"
        val col = categoryColorMap[lowerCatName] ?: defaultColors[Math.abs(displayName.hashCode()) % defaultColors.size]
        CategoryBreakdownItem(
            category = displayName,
            totalAmount = totalCatAmount,
            percentage = pct,
            color = col
        )
    }.sortedByDescending { it.totalAmount }

    // 3. Breakdown Cashflow Bar Data
    val sdfDisplayMonth = SimpleDateFormat("MMM yy", Locale.forLanguageTag("id-ID"))
    val cashflowBars = mutableListOf<CashflowBarItem>()

    when (preset) {
        "THIS_MONTH", "LAST_MONTH" -> {
            // Tampilkan perbandingan 3 bulan terakhir agar diagram batang selalu informatif
            for (i in 2 downTo 0) {
                val c = Calendar.getInstance().apply {
                    time = referenceDate
                    add(Calendar.MONTH, -i)
                }
                val mKey = sdfMonth.format(c.time)
                val mLabel = sdfDisplayMonth.format(c.time).uppercase()
                val mIncome = transactions
                    .filter { it.type.equals("INCOME", ignoreCase = true) && it.date.startsWith(mKey) }
                    .sumOf { it.amount }
                val mExpense = transactions
                    .filter { it.type.equals("EXPENSE", ignoreCase = true) && it.date.startsWith(mKey) }
                    .sumOf { it.amount }
                cashflowBars.add(CashflowBarItem(label = mLabel, income = mIncome, expense = mExpense))
            }
        }
        "LAST_3_MONTHS" -> {
            for (i in 2 downTo 0) {
                val c = Calendar.getInstance().apply {
                    time = referenceDate
                    add(Calendar.MONTH, -i)
                }
                val mKey = sdfMonth.format(c.time)
                val mLabel = sdfDisplayMonth.format(c.time).uppercase()
                val mIncome = transactions
                    .filter { it.type.equals("INCOME", ignoreCase = true) && it.date.startsWith(mKey) }
                    .sumOf { it.amount }
                val mExpense = transactions
                    .filter { it.type.equals("EXPENSE", ignoreCase = true) && it.date.startsWith(mKey) }
                    .sumOf { it.amount }
                cashflowBars.add(CashflowBarItem(label = mLabel, income = mIncome, expense = mExpense))
            }
        }
        "ALL_TIME" -> {
            // Tampilkan hingga 4 bulan terakhir
            for (i in 3 downTo 0) {
                val c = Calendar.getInstance().apply {
                    time = referenceDate
                    add(Calendar.MONTH, -i)
                }
                val mKey = sdfMonth.format(c.time)
                val mLabel = sdfDisplayMonth.format(c.time).uppercase()
                val mIncome = transactions
                    .filter { it.type.equals("INCOME", ignoreCase = true) && it.date.startsWith(mKey) }
                    .sumOf { it.amount }
                val mExpense = transactions
                    .filter { it.type.equals("EXPENSE", ignoreCase = true) && it.date.startsWith(mKey) }
                    .sumOf { it.amount }
                cashflowBars.add(CashflowBarItem(label = mLabel, income = mIncome, expense = mExpense))
            }
        }
    }

    return ReportsAnalyticsState(
        periodPreset = preset,
        totalIncome = periodIncome,
        totalExpense = periodExpense,
        netSavings = netSavings,
        savingRate = savingRate,
        savingStatus = savingStatus,
        categoryBreakdown = categoryBreakdown,
        cashflowBars = cashflowBars
    )
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
 * - `reportsAnalytics`: Agregasi analitik laporan, rasio tabungan, breakdown kategori, dan cashflow bars.
 */
fun calculateFinanceTotals(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    budgets: List<BudgetProgressItem> = emptyList(),
    notifications: List<NotificationEntity> = emptyList(),
    unreadNotificationCount: Int = 0,
    searchQuery: String = "",
    selectedCategoryFilter: String? = null,
    selectedDateFilter: String = "ALL",
    selectedFilterTab: String = "ALL",
    reportsPreset: String = "THIS_MONTH",
    aiInsightText: String? = null,
    isAiInsightLoading: Boolean = false,
    aiInsightError: String? = null,
    hasApiKey: Boolean = false,
    isHapticEnabled: Boolean = true,
    pageSize: Int = 50,
    visibleTransactionCount: Int = 50,
    currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    referenceDate: Date = Date(),
    hasMoreOverride: Boolean? = null
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

    val reportsState = calculateReportsAnalytics(
        transactions = transactions,
        categories = categories,
        preset = reportsPreset,
        referenceDate = referenceDate
    )

    val pagedFiltered = filtered.take(visibleTransactionCount)
    val hasMore = hasMoreOverride ?: (filtered.size > visibleTransactionCount)

    return FinanceUiState(
        transactions = transactions,
        filteredTransactions = pagedFiltered,
        accounts = accounts,
        categories = categories,
        budgets = budgets,
        notifications = notifications,
        unreadNotificationCount = unreadNotificationCount,
        totalBalance = totalBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        searchQuery = searchQuery,
        selectedCategoryFilter = selectedCategoryFilter,
        selectedDateFilter = selectedDateFilter,
        selectedFilterTab = selectedFilterTab,
        reportsAnalytics = reportsState,
        aiInsightText = aiInsightText,
        isAiInsightLoading = isAiInsightLoading,
        aiInsightError = aiInsightError,
        hasApiKey = hasApiKey,
        isHapticEnabled = isHapticEnabled,
        pageSize = pageSize,
        visibleTransactionCount = visibleTransactionCount,
        hasMoreTransactions = hasMore
    )
}
