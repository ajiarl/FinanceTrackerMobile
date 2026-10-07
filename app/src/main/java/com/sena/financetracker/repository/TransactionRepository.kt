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
        if (transaction.type.equals("TRANSFER", ignoreCase = true)) {
            accountDao.adjustBalance(transaction.accountId, transaction.amount)
            transaction.toAccountId?.let { toId ->
                accountDao.adjustBalance(toId, -transaction.amount)
            }
        } else {
            val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) {
                -transaction.amount
            } else {
                transaction.amount
            }
            accountDao.adjustBalance(transaction.accountId, delta)
        }
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String
    ) {
        require(fromAccount.id != toAccount.id) { "Akun asal dan akun tujuan tidak boleh sama" }
        require(amount > 0) { "Nominal transfer harus lebih besar dari 0" }

        // 1) Kurangi saldo fromAccount.id sebesar amount
        accountDao.adjustBalance(fromAccount.id, -amount)

        // 2) Tambah saldo toAccount.id sebesar amount
        accountDao.adjustBalance(toAccount.id, amount)

        // 3) Catat transaksi transfer ke database
        val transferTx = TransactionEntity(
            title = "Transfer ke ${toAccount.name}",
            amount = amount,
            type = "TRANSFER",
            category = "Transfer",
            date = date,
            accountId = fromAccount.id,
            accountName = fromAccount.name,
            notes = if (notes.isNotBlank()) notes else "Transfer dari ${fromAccount.name} ke ${toAccount.name}",
            toAccountId = toAccount.id,
            toAccountName = toAccount.name
        )
        transactionDao.insertTransaction(transferTx)
    }

    suspend fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        date: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    ): Long {
        val newAccount = AccountEntity(
            name = name,
            type = type,
            balance = initialBalance
        )
        val newAccountId = accountDao.insertAccount(newAccount)

        if (initialBalance > 0) {
            val initialTx = TransactionEntity(
                title = "Saldo Awal",
                amount = initialBalance,
                type = "INCOME",
                category = "Saldo Awal",
                date = date,
                accountId = newAccountId,
                accountName = name,
                notes = "Saldo awal saat pembuatan akun"
            )
            transactionDao.insertTransaction(initialTx)
        }
        return newAccountId
    }

    suspend fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    ) {
        val diff = actualBalance - account.balance
        if (kotlin.math.abs(diff) < 0.001) return

        // 1) Update saldo akun ke actualBalance
        accountDao.updateBalance(account.id, actualBalance)

        // 2) Buat transaksi penyesuaian sistem
        val adjustmentTx = TransactionEntity(
            title = "Penyesuaian Saldo Sistem",
            amount = kotlin.math.abs(diff),
            type = if (diff > 0) "INCOME" else "EXPENSE",
            category = "Penyesuaian",
            date = date,
            accountId = account.id,
            accountName = account.name,
            notes = "Rekonsiliasi: saldo lama ${account.balance.toLong()}, saldo baru ${actualBalance.toLong()}, selisih ${if (diff > 0) "+" else ""}${diff.toLong()}"
        )
        transactionDao.insertTransaction(adjustmentTx)
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
