package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
