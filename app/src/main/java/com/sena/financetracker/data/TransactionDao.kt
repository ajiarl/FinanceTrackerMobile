package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow

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
