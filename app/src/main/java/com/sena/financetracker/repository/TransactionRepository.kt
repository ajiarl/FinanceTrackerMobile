package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.AppDatabase
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    val transactionDao: TransactionDao,
    val accountDao: AccountDao,
    val categoryDao: CategoryDao
) {
    constructor(db: AppDatabase) : this(
        db.transactionDao,
        db.accountDao,
        db.categoryDao
    )

    // Backward-compatible constructor
    constructor(transactionDao: TransactionDao) : this(
        transactionDao = transactionDao,
        accountDao = object : AccountDao {
            override fun getAllAccounts(): Flow<List<AccountEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getAccountById(id: Long): AccountEntity? = null
            override suspend fun insertAccount(account: AccountEntity): Long = 0L
            override suspend fun updateBalance(id: Long, newBalance: Double) {}
            override suspend fun adjustBalance(id: Long, delta: Double) {}
            override suspend fun deleteAccount(id: Long) {}
        },
        categoryDao = object : CategoryDao {
            override fun getAllCategories(): Flow<List<CategoryEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun insertCategory(category: CategoryEntity): Long = 0L
            override suspend fun deleteCategory(id: Long) {}
        }
    )

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val insertedId = transactionDao.insertTransaction(transaction)
        val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) {
            transaction.amount
        } else {
            -transaction.amount
        }
        accountDao.adjustBalance(transaction.accountId, delta)
        return insertedId
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction.id)
        val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) {
            -transaction.amount
        } else {
            transaction.amount
        }
        accountDao.adjustBalance(transaction.accountId, delta)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        transactionDao.updateTransaction(newTransaction)

        // 1. Revert old transaction effect on old account balance
        val oldDelta = if (oldTransaction.type.equals("INCOME", ignoreCase = true)) {
            -oldTransaction.amount
        } else {
            oldTransaction.amount
        }
        accountDao.adjustBalance(oldTransaction.accountId, oldDelta)

        // 2. Apply new transaction effect on new account balance
        val newDelta = if (newTransaction.type.equals("INCOME", ignoreCase = true)) {
            newTransaction.amount
        } else {
            -newTransaction.amount
        }
        accountDao.adjustBalance(newTransaction.accountId, newDelta)
    }

    suspend fun updateTransaction(newTransaction: TransactionEntity) {
        val oldTx = transactionDao.getTransactionById(newTransaction.id)
        if (oldTx != null) {
            updateTransaction(oldTx, newTransaction)
        } else {
            transactionDao.updateTransaction(newTransaction)
        }
    }
}
