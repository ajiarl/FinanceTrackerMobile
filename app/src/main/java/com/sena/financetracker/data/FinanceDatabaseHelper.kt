package com.sena.financetracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Backward-compatible helper that delegates to [AppDatabase].
 */
class FinanceDatabaseHelper(context: Context) : TransactionDao {
    private val appDatabase = AppDatabase.getInstance(context)

    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return appDatabase.transactionDao.getAllTransactions()
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return appDatabase.transactionDao.insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(id: Long) {
        appDatabase.transactionDao.deleteTransaction(id)
    }

    val database: AppDatabase
        get() = appDatabase
}
