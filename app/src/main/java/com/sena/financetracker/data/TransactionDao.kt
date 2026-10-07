package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow

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
}
