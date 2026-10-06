package com.sena.financetracker.repository

import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }
}
