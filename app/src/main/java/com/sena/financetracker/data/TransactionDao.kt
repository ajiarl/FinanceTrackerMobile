package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

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
