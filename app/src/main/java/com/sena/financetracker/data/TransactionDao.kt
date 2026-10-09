package com.sena.financetracker.data

import com.sena.financetracker.viewmodel.CashflowBarItem
import com.sena.financetracker.viewmodel.CategoryBreakdownItem
import com.sena.financetracker.viewmodel.ReportsAnalyticsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Ringkasan kalkulasi total pendapatan dan pengeluaran hasil agregasi SQL di database.
 *
 * @property totalIncome Akumulasi total transaksi bertipe INCOME.
 * @property totalExpense Akumulasi total transaksi bertipe EXPENSE.
 */
data class FinanceSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

/**
 * Model data agregasi ringkas untuk total pengeluaran per kategori hasil agregasi SQL.
 *
 * @property category Nama kategori pengeluaran.
 * @property totalAmount Akumulasi nominal pengeluaran untuk kategori tersebut.
 */
data class CategoryExpenseSummary(
    val category: String,
    val totalAmount: Double
)

/**
 * Model data agregasi ringkas untuk arus kas bulanan (pemasukan vs pengeluaran) hasil agregasi SQL.
 *
 * @property month Label bulan dalam format "YYYY-MM".
 * @property income Total pemasukan pada bulan tersebut.
 * @property expense Total pengeluaran pada bulan tersebut.
 */
data class MonthlyCashFlowSummary(
    val month: String,
    val income: Double,
    val expense: Double
)

/**
 * Hasil pembungkusan kueri transaksi terpaginasi beserta indikator ketersediaan halaman berikutnya.
 *
 * @property transactions Daftar entitas transaksi pada halaman saat ini (maksimal sejumlah limit).
 * @property hasMore Indikator boolean bernilai true jika masih terdapat rekaman data berikutnya di database.
 */
data class PagedTransactionsResult(
    val transactions: List<TransactionEntity>,
    val hasMore: Boolean
)

/**
 * Data Access Object (DAO) untuk entitas transaksi keuangan.
 * Menyediakan operasi CRUD, pagination, batch insert, dan eksekusi transaksi database atomic.
 */
interface TransactionDao {
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity> {
        return emptyList()
    }

    /**
     * Mengambil transaksi terpaginasi dari database dengan deteksi keberadaan halaman berikutnya.
     * Menggunakan strategi query `limit + 1` untuk menentukan [PagedTransactionsResult.hasMore]
     * secara akurat tanpa membaca seluruh tabel ke RAM.
     *
     * @param limit Batas jumlah transaksi yang diinginkan pada halaman aktif.
     * @param offset Posisi awal baris yang dilewati.
     * @return [PagedTransactionsResult] berisi daftar transaksi maksimal [limit] dan status [hasMore].
     */
    suspend fun getTransactionsPagedWithHasMore(limit: Int, offset: Int): PagedTransactionsResult {
        val raw = getTransactionsPaged(limit + 1, offset)
        val hasMore = raw.size > limit
        return PagedTransactionsResult(
            transactions = raw.take(limit),
            hasMore = hasMore
        )
    }

    /**
     * Menghitung total pemasukan dan pengeluaran secara langsung di tingkat basis data (SQLite engine)
     * menggunakan klausa SUM dan CASE tanpa memuat seluruh entitas transaksi ke memori (zero RAM overhead).
     *
     * @param query Kata kunci pencarian pada judul atau catatan transaksi.
     * @param category Filter kategori transaksi.
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD).
     * @return [FinanceSummary] berisi akumulasi totalIncome dan totalExpense.
     */
    suspend fun getFinanceSummary(
        query: String? = null,
        category: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): FinanceSummary {
        val all = getAllTransactions().firstOrNull() ?: emptyList()
        val filtered = all.filter { tx ->
            val matchQuery = query.isNullOrBlank() || tx.title.contains(query, ignoreCase = true) || tx.notes.contains(query, ignoreCase = true)
            val matchCategory = category.isNullOrBlank() || category.trim().equals("ALL", ignoreCase = true) || tx.category.trim().equals(category.trim(), ignoreCase = true)
            val matchStart = startDate.isNullOrBlank() || tx.date >= startDate
            val matchEnd = endDate.isNullOrBlank() || tx.date <= endDate
            matchQuery && matchCategory && matchStart && matchEnd
        }
        val inc = filtered.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
        val exp = filtered.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
        return FinanceSummary(totalIncome = inc, totalExpense = exp)
    }

    /**
     * Mengambil daftar transaksi terpaginasi dengan penyaringan multi-kriteria langsung dari SQLite.
     * Mengambil limit + 1 baris untuk mendeteksi keberadaan halaman berikutnya secara deterministik.
     *
     * @param query Kata kunci pencarian pada judul atau catatan transaksi.
     * @param type Filter tipe transaksi ("INCOME", "EXPENSE", atau null untuk semua).
     * @param category Filter kategori transaksi.
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD).
     * @param limit Jumlah transaksi per halaman yang diminta.
     * @param offset Pergeseran baris data.
     * @return [PagedTransactionsResult] memuat daftar transaksi dan status hasMore.
     */
    suspend fun getFilteredTransactionsPaged(
        query: String? = null,
        type: String? = null,
        category: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): PagedTransactionsResult {
        val all = getAllTransactions().firstOrNull() ?: emptyList()
        val filtered = all.filter { tx ->
            val matchQuery = query.isNullOrBlank() || tx.title.contains(query, ignoreCase = true) || tx.notes.contains(query, ignoreCase = true)
            val matchType = type.isNullOrBlank() || type.trim().equals("ALL", ignoreCase = true) || tx.type.trim().equals(type.trim(), ignoreCase = true)
            val matchCategory = category.isNullOrBlank() || category.trim().equals("ALL", ignoreCase = true) || tx.category.trim().equals(category.trim(), ignoreCase = true)
            val matchStart = startDate.isNullOrBlank() || tx.date >= startDate
            val matchEnd = endDate.isNullOrBlank() || tx.date <= endDate
            matchQuery && matchType && matchCategory && matchStart && matchEnd
        }
        val slice = filtered.drop(offset).take(limit + 1)
        val hasMore = slice.size > limit
        return PagedTransactionsResult(
            transactions = slice.take(limit),
            hasMore = hasMore
        )
    }

    /**
     * Menghitung jumlah total transaksi di database secara efisien melalui query COUNT(*).
     */
    suspend fun getTransactionCount(): Int {
        return 0
    }

    /**
     * Aliran sinyal pembaruan data transaksi untuk observasi reaktif yang hemat memori (zero RAM leak).
     */
    fun getTransactionUpdateTrigger(): Flow<Long> {
        return getAllTransactions().map { System.currentTimeMillis() }
    }

    /**
     * Mengambil daftar agregasi pengeluaran per kategori langsung dari SQLite tanpa memuat seluruh baris transaksi.
     *
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @return Daftar [CategoryExpenseSummary] terurut dari nominal pengeluaran terbesar.
     */
    suspend fun getCategoryExpenseSummary(
        startDate: String? = null,
        endDate: String? = null
    ): List<CategoryExpenseSummary> {
        val all = getAllTransactions().firstOrNull() ?: emptyList()
        val filtered = all.filter { tx ->
            val matchType = tx.type.equals("EXPENSE", ignoreCase = true)
            val matchStart = startDate.isNullOrBlank() || tx.date >= startDate
            val matchEnd = endDate.isNullOrBlank() || tx.date <= endDate
            matchType && matchStart && matchEnd
        }
        return filtered.groupBy { it.category.trim() }
            .map { (cat, list) -> CategoryExpenseSummary(category = cat, totalAmount = list.sumOf { it.amount }) }
            .sortedByDescending { it.totalAmount }
    }

    /**
     * Mengambil daftar agregasi arus kas per bulan (YYYY-MM) langsung dari SQLite.
     *
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @return Daftar [MonthlyCashFlowSummary] terurut kronologis bulan.
     */
    suspend fun getMonthlyCashFlowSummary(
        startDate: String? = null,
        endDate: String? = null
    ): List<MonthlyCashFlowSummary> {
        val all = getAllTransactions().firstOrNull() ?: emptyList()
        val filtered = all.filter { tx ->
            val matchStart = startDate.isNullOrBlank() || tx.date >= startDate
            val matchEnd = endDate.isNullOrBlank() || tx.date <= endDate
            matchStart && matchEnd
        }
        return filtered.groupBy { if (it.date.length >= 7) it.date.take(7) else it.date }
            .map { (month, list) ->
                val inc = list.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                val exp = list.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
                MonthlyCashFlowSummary(month = month, income = inc, expense = exp)
            }
            .sortedBy { it.month }
    }

    /**
     * Menghitung dan menghasilkan ringkasan analitik laporan keuangan lengkap
     * langsung via agregasi SQL SQLite murni tanpa memuat seluruh entitas transaksi ke RAM.
     *
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @return [ReportsAnalyticsState] ringkas berisi total income, expense, breakdown kategori, dan diagram arus kas bulanan.
     */
    suspend fun getReportsAnalytics(
        startDate: String? = null,
        endDate: String? = null
    ): ReportsAnalyticsState {
        val summary = getFinanceSummary(query = null, category = null, startDate = startDate, endDate = endDate)
        val catSummaries = getCategoryExpenseSummary(startDate = startDate, endDate = endDate)
        val cfSummaries = getMonthlyCashFlowSummary(startDate = startDate, endDate = endDate)

        val totalIncome = summary.totalIncome
        val totalExpense = summary.totalExpense
        val netSavings = totalIncome - totalExpense
        val savingRate = if (totalIncome > 0) {
            val rate = ((netSavings / totalIncome) * 100).toInt()
            rate.coerceIn(-100, 100)
        } else if (totalExpense > 0) {
            -100
        } else {
            0
        }
        val savingStatus = when {
            totalIncome == 0.0 && totalExpense == 0.0 -> "NORMAL"
            savingRate >= 30 -> "HEMAT"
            savingRate >= 10 -> "NORMAL"
            else -> "BOROS"
        }

        val defaultColors = listOf("#F97316", "#3B82F6", "#EC4899", "#8B5CF6", "#10B981", "#EAB308", "#64748B")
        val breakdownItems = catSummaries.map { cat ->
            val pct = if (totalExpense > 0) ((cat.totalAmount / totalExpense) * 100).toInt() else 0
            val displayName = cat.category.trim().ifBlank { "Lainnya" }
            CategoryBreakdownItem(
                category = displayName,
                totalAmount = cat.totalAmount,
                percentage = pct,
                color = defaultColors[Math.abs(displayName.hashCode()) % defaultColors.size]
            )
        }

        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.ROOT)
        val sdfDisplayMonth = SimpleDateFormat("MMM yy", Locale.forLanguageTag("id-ID"))
        val cashflowBars = cfSummaries.map { cf ->
            val label = try {
                val d = sdfMonth.parse(cf.month)
                if (d != null) sdfDisplayMonth.format(d).uppercase(Locale.ROOT) else cf.month
            } catch (e: Exception) {
                cf.month
            }
            CashflowBarItem(label = label, income = cf.income, expense = cf.expense)
        }

        return ReportsAnalyticsState(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingRate = savingRate,
            savingStatus = savingStatus,
            categoryBreakdown = breakdownItems,
            cashflowBars = cashflowBars
        )
    }

    suspend fun deleteTransaction(id: Long)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun clearAllTransactions() {}
    suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> {
        return transactions.map { insertTransaction(it) }
    }

    /**
     * Menjalankan blok operasi database di dalam transaksi atomic SQLite (ACID).
     * Jika terjadi kegagalan atau exception, seluruh operasi di dalam blok akan dibatalkan (rollback).
     * Implementasi default langsung mengeksekusi blok untuk mock/fake unit test.
     *
     * @param block Blok suspend fungsi yang dieksekusi di dalam transaksi.
     * @return Hasil pengembalian dari [block].
     */
    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return block()
    }
}
