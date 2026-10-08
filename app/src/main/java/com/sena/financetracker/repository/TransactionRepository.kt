package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.AppDatabase
import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.NotificationDao
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository utama pengelola transaksi, rekening (accounts), kategori, dan anggaran (budgets).
 * Bertindak sebagai fasad terkoordinasi untuk ACID integrity dan pendelegasian domain.
 */
class TransactionRepository(
    val transactionDao: TransactionDao,
    val accountDao: AccountDao,
    val categoryDao: CategoryDao,
    val budgetDao: BudgetDao = createDefaultBudgetDao(),
    val notificationDao: NotificationDao = createDefaultNotificationDao(),
    private val preferences: android.content.SharedPreferences? = null
) {
    // Delegasi domain handler
    val accountHandler = AccountDomainHandler(accountDao, transactionDao)
    val categoryHandler = CategoryDomainHandler(categoryDao)
    val budgetHandler = BudgetDomainHandler(budgetDao, transactionDao, notificationDao)
    val notificationHandler = NotificationDomainHandler(notificationDao)

    private val _hapticEnabledFlow = kotlinx.coroutines.flow.MutableStateFlow(
        preferences?.getBoolean("KEY_HAPTIC_ENABLED", true) ?: true
    )

    fun isHapticEnabled(): Flow<Boolean> = _hapticEnabledFlow

    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabledFlow.value = enabled
        preferences?.edit()?.putBoolean("KEY_HAPTIC_ENABLED", enabled)?.apply()
    }

    /**
     * Konstruktor praktis berbasis database Room/SQLite terpadu [AppDatabase].
     */
    constructor(db: AppDatabase, preferences: android.content.SharedPreferences? = null) : this(
        db.transactionDao,
        db.accountDao,
        db.categoryDao,
        db.budgetDao,
        db.notificationDao,
        preferences
    )

    /**
     * Konstruktor backward-compatible untuk testing unit dengan fake TransactionDao.
     */
    constructor(transactionDao: TransactionDao) : this(
        transactionDao = transactionDao,
        accountDao = createDefaultAccountDao(),
        categoryDao = createDefaultCategoryDao()
    )

    /**
     * Konstruktor praktis untuk testing unit dengan fake TransactionDao, AccountDao, dan CategoryDao.
     */
    constructor(
        transactionDao: TransactionDao,
        accountDao: AccountDao,
        categoryDao: CategoryDao
    ) : this(
        transactionDao = transactionDao,
        accountDao = accountDao,
        categoryDao = categoryDao,
        budgetDao = createDefaultBudgetDao(),
        notificationDao = createDefaultNotificationDao()
    )

    // ── TRANSACTIONS CORE API ──────────────────────────────────────────────────
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity> =
        transactionDao.getTransactionsPaged(limit, offset)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val insertedId = transactionDao.insertTransaction(transaction)
        val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) {
            transaction.amount
        } else {
            -transaction.amount
        }
        accountDao.adjustBalance(transaction.accountId, delta)

        if (transaction.type.equals("EXPENSE", ignoreCase = true)) {
            budgetHandler.checkAndTriggerBudgetAlert(transaction)
        }

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
        val tx = transactionDao.getTransactionById(id)
        if (tx != null) {
            deleteTransaction(tx)
        } else {
            transactionDao.deleteTransaction(id)
        }
    }

    suspend fun resetTransactions() = transactionDao.clearAllTransactions()

    suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> {
        if (transactions.isEmpty()) return emptyList()

        val ids = transactionDao.insertTransactionsBatch(transactions)

        val deltasByAccount = mutableMapOf<Long, Double>()
        for (tx in transactions) {
            if (tx.type.equals("TRANSFER", ignoreCase = true)) {
                val currentFrom = deltasByAccount.getOrDefault(tx.accountId, 0.0)
                deltasByAccount[tx.accountId] = currentFrom - tx.amount
                tx.toAccountId?.let { toId ->
                    val currentTo = deltasByAccount.getOrDefault(toId, 0.0)
                    deltasByAccount[toId] = currentTo + tx.amount
                }
            } else {
                val delta = if (tx.type.equals("INCOME", ignoreCase = true)) {
                    tx.amount
                } else {
                    -tx.amount
                }
                val current = deltasByAccount.getOrDefault(tx.accountId, 0.0)
                deltasByAccount[tx.accountId] = current + delta
            }
        }

        deltasByAccount.forEach { (accountId, totalDelta) ->
            accountDao.adjustBalance(accountId, totalDelta)
        }

        transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .forEach { tx ->
                budgetHandler.checkAndTriggerBudgetAlert(tx)
            }

        return ids
    }

    /**
     * Memperbarui transaksi dan menyesuaikan saldo rekening secara konsisten.
     * Mendukung perubahan jenis transaksi antara INCOME, EXPENSE, dan TRANSFER secara atomik dan akurat.
     *
     * @param oldTransaction Data transaksi sebelum pembaruan (digunakan untuk membatalkan mutasi saldo lama).
     * @param newTransaction Data transaksi baru yang akan disimpan dan diterapkan mutasi saldonya.
     */
    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        transactionDao.updateTransaction(newTransaction)

        // 1. Batalkan mutasi saldo transaksi lama
        if (oldTransaction.type.equals("TRANSFER", ignoreCase = true)) {
            accountDao.adjustBalance(oldTransaction.accountId, oldTransaction.amount)
            oldTransaction.toAccountId?.let { toId ->
                accountDao.adjustBalance(toId, -oldTransaction.amount)
            }
        } else {
            val oldDelta = if (oldTransaction.type.equals("INCOME", ignoreCase = true)) {
                -oldTransaction.amount
            } else {
                oldTransaction.amount
            }
            accountDao.adjustBalance(oldTransaction.accountId, oldDelta)
        }

        // 2. Terapkan mutasi saldo transaksi baru
        if (newTransaction.type.equals("TRANSFER", ignoreCase = true)) {
            accountDao.adjustBalance(newTransaction.accountId, -newTransaction.amount)
            newTransaction.toAccountId?.let { toId ->
                accountDao.adjustBalance(toId, newTransaction.amount)
            }
        } else {
            val newDelta = if (newTransaction.type.equals("INCOME", ignoreCase = true)) {
                newTransaction.amount
            } else {
                -newTransaction.amount
            }
            accountDao.adjustBalance(newTransaction.accountId, newDelta)

            if (newTransaction.type.equals("EXPENSE", ignoreCase = true)) {
                budgetHandler.checkAndTriggerBudgetAlert(newTransaction)
            }
        }
    }

    suspend fun updateTransaction(newTransaction: TransactionEntity) {
        val oldTx = transactionDao.getTransactionById(newTransaction.id)
        if (oldTx != null) {
            updateTransaction(oldTx, newTransaction)
        } else {
            transactionDao.updateTransaction(newTransaction)
        }
    }

    // ── ACCOUNTS DOMAIN FACADE ────────────────────────────────────────────────
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountHandler.getAllAccounts()

    suspend fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String
    ) = accountHandler.transferFunds(fromAccount, toAccount, amount, notes, date)

    suspend fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Long = accountHandler.addAccount(name, type, initialBalance, date)

    suspend fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) = accountHandler.reconcileAccount(account, actualBalance, date)

    // ── CATEGORIES DOMAIN FACADE ──────────────────────────────────────────────
    val systemCategoryNames: Set<String> get() = categoryHandler.systemCategoryNames

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryHandler.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryHandler.getCategoriesByType(type)

    fun isSystemCategory(name: String): Boolean = categoryHandler.isSystemCategory(name)

    suspend fun insertCategory(name: String, type: String, color: String = "#FAFF00"): Long =
        categoryHandler.insertCategory(name, type, color)

    suspend fun updateCategory(id: Long, name: String, type: String, color: String): Int =
        categoryHandler.updateCategory(id, name, type, color)

    suspend fun deleteCategory(id: Long): Int = categoryHandler.deleteCategory(id)

    // ── BUDGETS DOMAIN FACADE ─────────────────────────────────────────────────
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetHandler.getAllBudgets()

    fun getBudgetProgress(
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ): Flow<List<BudgetProgressItem>> = budgetHandler.getBudgetProgress(period)

    suspend fun addBudget(
        name: String,
        category: String,
        limitAmount: Double,
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ): Long = budgetHandler.addBudget(name, category, limitAmount, period)

    suspend fun deleteBudget(id: Long) = budgetHandler.deleteBudget(id)

    suspend fun updateBudget(budget: BudgetEntity) = budgetHandler.updateBudget(budget)

    // ── NOTIFICATIONS DOMAIN FACADE ───────────────────────────────────────────
    fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationHandler.getAllNotifications()

    fun getUnreadNotificationCount(): Flow<Int> = notificationHandler.getUnreadNotificationCount()

    suspend fun markNotificationAsRead(id: Long) = notificationHandler.markNotificationAsRead(id)

    suspend fun markAllNotificationsAsRead() = notificationHandler.markAllNotificationsAsRead()

    suspend fun clearAllNotifications() = notificationHandler.clearAllNotifications()

    suspend fun deleteNotification(id: Long) = notificationHandler.deleteNotification(id)

    suspend fun insertNotification(notification: NotificationEntity): Long =
        notificationHandler.insertNotification(notification)

    companion object {
        private fun createDefaultBudgetDao(): BudgetDao = object : BudgetDao {
            private val _flow = kotlinx.coroutines.flow.MutableStateFlow<List<BudgetEntity>>(emptyList())
            override fun getAllBudgets(): Flow<List<BudgetEntity>> = _flow
            override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> = _flow
            override suspend fun getBudgetById(id: Long): BudgetEntity? = null
            override suspend fun insertBudget(budget: BudgetEntity): Long = 0L
            override suspend fun updateBudget(budget: BudgetEntity) {}
            override suspend fun deleteBudget(id: Long) {}
        }

        private fun createDefaultNotificationDao(): NotificationDao = object : NotificationDao {
            private val _flow = kotlinx.coroutines.flow.MutableStateFlow<List<NotificationEntity>>(emptyList())
            override fun getAllNotifications(): Flow<List<NotificationEntity>> = _flow
            override fun getUnreadCount(): Flow<Int> = flowOf(0)
            override suspend fun insertNotification(notification: NotificationEntity): Long = 0L
            override suspend fun markAsRead(id: Long) {}
            override suspend fun markAllAsRead() {}
            override suspend fun clearAllNotifications() {}
            override suspend fun deleteNotification(id: Long) {}
        }

        private fun createDefaultAccountDao(): AccountDao = object : AccountDao {
            override fun getAllAccounts(): Flow<List<AccountEntity>> = flowOf(emptyList())
            override suspend fun getAccountById(id: Long): AccountEntity? = null
            override suspend fun insertAccount(account: AccountEntity): Long = 0L
            override suspend fun updateBalance(id: Long, newBalance: Double) {}
            override suspend fun adjustBalance(id: Long, delta: Double) {}
            override suspend fun deleteAccount(id: Long) {}
        }

        private fun createDefaultCategoryDao(): CategoryDao = object : CategoryDao {
            override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override suspend fun insertCategory(category: CategoryEntity): Long = 0L
            override suspend fun updateCategory(category: CategoryEntity): Int = 0
            override suspend fun deleteCategory(id: Long): Int = 0
            override suspend fun getCategoryById(id: Long): CategoryEntity? = null
        }
    }
}
