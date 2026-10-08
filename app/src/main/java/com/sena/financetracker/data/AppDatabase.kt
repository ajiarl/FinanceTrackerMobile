package com.sena.financetracker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SQLite Open Helper terpadu pengelola koneksi dan StateFlow reaktif.
 */
class AppDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "finance_tracker.db"
        const val DATABASE_VERSION = 6

        // Delegasi backward-compatible untuk konstanta skema & indeks
        const val INDEX_TX_DATE = DatabaseSchema.INDEX_TX_DATE
        const val INDEX_TX_CATEGORY = DatabaseSchema.INDEX_TX_CATEGORY
        const val INDEX_TX_ACCOUNT = DatabaseSchema.INDEX_TX_ACCOUNT
        const val INDEX_BUDGETS_CATEGORY = DatabaseSchema.INDEX_BUDGETS_CATEGORY

        val INDEX_DDL_STATEMENTS = DatabaseSchema.INDEX_DDL_STATEMENTS

        const val TABLE_TRANSACTIONS = DatabaseSchema.TABLE_TRANSACTIONS
        const val COL_TX_ID = DatabaseSchema.COL_TX_ID
        const val COL_TX_TITLE = DatabaseSchema.COL_TX_TITLE
        const val COL_TX_AMOUNT = DatabaseSchema.COL_TX_AMOUNT
        const val COL_TX_TYPE = DatabaseSchema.COL_TX_TYPE
        const val COL_TX_CATEGORY = DatabaseSchema.COL_TX_CATEGORY
        const val COL_TX_DATE = DatabaseSchema.COL_TX_DATE
        const val COL_TX_ACCOUNT_ID = DatabaseSchema.COL_TX_ACCOUNT_ID
        const val COL_TX_ACCOUNT_NAME = DatabaseSchema.COL_TX_ACCOUNT_NAME
        const val COL_TX_NOTES = DatabaseSchema.COL_TX_NOTES
        const val COL_TX_TO_ACCOUNT_ID = DatabaseSchema.COL_TX_TO_ACCOUNT_ID
        const val COL_TX_TO_ACCOUNT_NAME = DatabaseSchema.COL_TX_TO_ACCOUNT_NAME

        const val TABLE_ACCOUNTS = DatabaseSchema.TABLE_ACCOUNTS
        const val COL_ACC_ID = DatabaseSchema.COL_ACC_ID
        const val COL_ACC_NAME = DatabaseSchema.COL_ACC_NAME
        const val COL_ACC_TYPE = DatabaseSchema.COL_ACC_TYPE
        const val COL_ACC_BALANCE = DatabaseSchema.COL_ACC_BALANCE

        const val TABLE_CATEGORIES = DatabaseSchema.TABLE_CATEGORIES
        const val COL_CAT_ID = DatabaseSchema.COL_CAT_ID
        const val COL_CAT_NAME = DatabaseSchema.COL_CAT_NAME
        const val COL_CAT_TYPE = DatabaseSchema.COL_CAT_TYPE
        const val COL_CAT_COLOR = DatabaseSchema.COL_CAT_COLOR

        const val TABLE_BUDGETS = DatabaseSchema.TABLE_BUDGETS
        const val COL_BUDGET_ID = DatabaseSchema.COL_BUDGET_ID
        const val COL_BUDGET_NAME = DatabaseSchema.COL_BUDGET_NAME
        const val COL_BUDGET_CATEGORY = DatabaseSchema.COL_BUDGET_CATEGORY
        const val COL_BUDGET_LIMIT = DatabaseSchema.COL_BUDGET_LIMIT
        const val COL_BUDGET_PERIOD = DatabaseSchema.COL_BUDGET_PERIOD
        const val COL_BUDGET_IS_ACTIVE = DatabaseSchema.COL_BUDGET_IS_ACTIVE

        const val TABLE_NOTIFICATIONS = DatabaseSchema.TABLE_NOTIFICATIONS
        const val COL_NOTIF_ID = DatabaseSchema.COL_NOTIF_ID
        const val COL_NOTIF_TITLE = DatabaseSchema.COL_NOTIF_TITLE
        const val COL_NOTIF_MESSAGE = DatabaseSchema.COL_NOTIF_MESSAGE
        const val COL_NOTIF_TYPE = DatabaseSchema.COL_NOTIF_TYPE
        const val COL_NOTIF_IS_READ = DatabaseSchema.COL_NOTIF_IS_READ
        const val COL_NOTIF_CREATED_AT = DatabaseSchema.COL_NOTIF_CREATED_AT

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: AppDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val _accountsFlow = MutableStateFlow<List<AccountEntity>>(emptyList())
    private val _categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private val _budgetsFlow = MutableStateFlow<List<BudgetEntity>>(emptyList())
    private val _notificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())

    init {
        dbScope.launch {
            refreshAllFlows()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        DatabaseSchema.createAccountsTable(db)
        DatabaseSchema.createCategoriesTable(db)
        DatabaseSchema.createTransactionsTable(db)
        DatabaseSchema.createBudgetsTable(db)
        DatabaseSchema.createNotificationsTable(db)
        DatabaseSchema.createDatabaseIndexes(db)
        DatabaseSeeder.seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        DatabaseMigrations.onUpgrade(db, oldVersion, newVersion)
    }

    suspend fun <T> runInTransaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = block()
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
        }
    }

    private suspend fun refreshAllFlows() = withContext(Dispatchers.IO) {
        refreshTransactionsFlowInternal()
        refreshAccountsFlowInternal()
        refreshCategoriesFlowInternal()
        refreshBudgetsFlowInternal()
        refreshNotificationsFlowInternal()
    }

    private fun refreshTransactionsFlowInternal() {
        val list = mutableListOf<TransactionEntity>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TRANSACTIONS, null, null, null, null, null, "$COL_TX_ID DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(DatabaseMappers.mapTransaction(c))
            }
        }
        _transactionsFlow.value = list
    }

    private fun refreshAccountsFlowInternal() {
        val list = mutableListOf<AccountEntity>()
        val db = readableDatabase
        val cursor = db.query(TABLE_ACCOUNTS, null, null, null, null, null, "$COL_ACC_ID ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(DatabaseMappers.mapAccount(c))
            }
        }
        _accountsFlow.value = list
    }

    private fun refreshCategoriesFlowInternal() {
        val list = mutableListOf<CategoryEntity>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, "$COL_CAT_ID ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(DatabaseMappers.mapCategory(c))
            }
        }
        _categoriesFlow.value = list
    }

    private fun refreshBudgetsFlowInternal() {
        val list = mutableListOf<BudgetEntity>()
        val db = readableDatabase
        val cursor = db.query(TABLE_BUDGETS, null, null, null, null, null, "$COL_BUDGET_ID ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(DatabaseMappers.mapBudget(c))
            }
        }
        _budgetsFlow.value = list
    }

    private fun refreshNotificationsFlowInternal() {
        val list = mutableListOf<NotificationEntity>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NOTIFICATIONS, null, null, null, null, null, "$COL_NOTIF_CREATED_AT DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(DatabaseMappers.mapNotification(c))
            }
        }
        _notificationsFlow.value = list
    }

    // ── TransactionDao Implementation ─────────────────────────────────────────
    val transactionDao: TransactionDao = object : TransactionDao {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _transactionsFlow.asStateFlow()

        override suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity> =
            withContext(Dispatchers.IO) {
                val list = mutableListOf<TransactionEntity>()
                val db = readableDatabase
                val cursor = db.query(
                    TABLE_TRANSACTIONS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "$COL_TX_DATE DESC, $COL_TX_ID DESC",
                    "$offset, $limit"
                )
                cursor.use { c ->
                    while (c.moveToNext()) {
                        list.add(DatabaseMappers.mapTransaction(c))
                    }
                }
                list
            }

        override suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(transaction)
            val id = db.insert(TABLE_TRANSACTIONS, null, values)
            refreshTransactionsFlowInternal()
            id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(transaction)
            db.update(TABLE_TRANSACTIONS, values, "$COL_TX_ID = ?", arrayOf(transaction.id.toString()))
            refreshTransactionsFlowInternal()
            Unit
        }

        override suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_TRANSACTIONS, "$COL_TX_ID = ?", arrayOf(id.toString()))
            refreshTransactionsFlowInternal()
            Unit
        }

        override suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_TRANSACTIONS, null, null)
            refreshTransactionsFlowInternal()
            Unit
        }

        override suspend fun getTransactionById(id: Long): TransactionEntity? = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_TRANSACTIONS,
                null,
                "$COL_TX_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            cursor.use { c ->
                if (c.moveToFirst()) DatabaseMappers.mapTransaction(c) else null
            }
        }

        override suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> =
            withContext(Dispatchers.IO) {
                val db = writableDatabase
                val ids = mutableListOf<Long>()
                db.beginTransaction()
                try {
                    for (tx in transactions) {
                        val values = DatabaseMappers.toContentValues(tx)
                        val id = db.insert(TABLE_TRANSACTIONS, null, values)
                        ids.add(id)
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                refreshTransactionsFlowInternal()
                ids
            }
    }

    // ── AccountDao Implementation ─────────────────────────────────────────────
    val accountDao: AccountDao = object : AccountDao {
        override fun getAllAccounts(): Flow<List<AccountEntity>> = _accountsFlow.asStateFlow()

        override suspend fun getAccountById(id: Long): AccountEntity? = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_ACCOUNTS,
                null,
                "$COL_ACC_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            cursor.use { c ->
                if (c.moveToFirst()) DatabaseMappers.mapAccount(c) else null
            }
        }

        override suspend fun insertAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(account)
            val id = db.insert(TABLE_ACCOUNTS, null, values)
            refreshAccountsFlowInternal()
            id
        }

        override suspend fun updateBalance(id: Long, newBalance: Double) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = android.content.ContentValues().apply { put(COL_ACC_BALANCE, newBalance) }
            db.update(TABLE_ACCOUNTS, values, "$COL_ACC_ID = ?", arrayOf(id.toString()))
            refreshAccountsFlowInternal()
            Unit
        }

        override suspend fun adjustBalance(id: Long, delta: Double) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.execSQL(
                "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_BALANCE = $COL_ACC_BALANCE + ? WHERE $COL_ACC_ID = ?",
                arrayOf(delta.toString(), id.toString())
            )
            refreshAccountsFlowInternal()
            Unit
        }

        override suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_ACCOUNTS, "$COL_ACC_ID = ?", arrayOf(id.toString()))
            refreshAccountsFlowInternal()
            Unit
        }
    }

    // ── CategoryDao Implementation ────────────────────────────────────────────
    val categoryDao: CategoryDao = object : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = _categoriesFlow.asStateFlow()

        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = _categoriesFlow.map { list ->
            list.filter { it.type.equals(type, ignoreCase = true) }
        }

        override suspend fun getCategoryById(id: Long): CategoryEntity? = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CATEGORIES,
                null,
                "$COL_CAT_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            cursor.use { c ->
                if (c.moveToFirst()) DatabaseMappers.mapCategory(c) else null
            }
        }

        override suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(category)
            val id = db.insert(TABLE_CATEGORIES, null, values)
            refreshCategoriesFlowInternal()
            id
        }

        override suspend fun updateCategory(category: CategoryEntity): Int = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(category)
            val count = db.update(TABLE_CATEGORIES, values, "$COL_CAT_ID = ?", arrayOf(category.id.toString()))
            refreshCategoriesFlowInternal()
            count
        }

        override suspend fun deleteCategory(id: Long): Int = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val count = db.delete(TABLE_CATEGORIES, "$COL_CAT_ID = ?", arrayOf(id.toString()))
            refreshCategoriesFlowInternal()
            count
        }
    }

    // ── BudgetDao Implementation ──────────────────────────────────────────────
    val budgetDao: BudgetDao = object : BudgetDao {
        override fun getAllBudgets(): Flow<List<BudgetEntity>> = _budgetsFlow.asStateFlow()

        override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> = _budgetsFlow.map { list ->
            list.filter { it.period == period }
        }

        override suspend fun getBudgetById(id: Long): BudgetEntity? = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_BUDGETS,
                null,
                "$COL_BUDGET_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            cursor.use { c ->
                if (c.moveToFirst()) DatabaseMappers.mapBudget(c) else null
            }
        }

        override suspend fun insertBudget(budget: BudgetEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(budget)
            val id = db.insert(TABLE_BUDGETS, null, values)
            refreshBudgetsFlowInternal()
            id
        }

        override suspend fun updateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(budget)
            db.update(TABLE_BUDGETS, values, "$COL_BUDGET_ID = ?", arrayOf(budget.id.toString()))
            refreshBudgetsFlowInternal()
            Unit
        }

        override suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_BUDGETS, "$COL_BUDGET_ID = ?", arrayOf(id.toString()))
            refreshBudgetsFlowInternal()
            Unit
        }
    }

    // ── NotificationDao Implementation ───────────────────────────────────────
    val notificationDao: NotificationDao = object : NotificationDao {
        override fun getAllNotifications(): Flow<List<NotificationEntity>> = _notificationsFlow.asStateFlow()

        override fun getUnreadCount(): Flow<Int> = _notificationsFlow.map { list -> list.count { !it.isRead } }

        override suspend fun insertNotification(notification: NotificationEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(notification)
            val id = db.insert(TABLE_NOTIFICATIONS, null, values)
            refreshNotificationsFlowInternal()
            id
        }

        override suspend fun markAsRead(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = android.content.ContentValues().apply { put(COL_NOTIF_IS_READ, 1) }
            db.update(TABLE_NOTIFICATIONS, values, "$COL_NOTIF_ID = ?", arrayOf(id.toString()))
            refreshNotificationsFlowInternal()
            Unit
        }

        override suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = android.content.ContentValues().apply { put(COL_NOTIF_IS_READ, 1) }
            db.update(TABLE_NOTIFICATIONS, values, null, null)
            refreshNotificationsFlowInternal()
            Unit
        }

        override suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_NOTIFICATIONS, null, null)
            refreshNotificationsFlowInternal()
            Unit
        }

        override suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_NOTIFICATIONS, "$COL_NOTIF_ID = ?", arrayOf(id.toString()))
            refreshNotificationsFlowInternal()
            Unit
        }
    }

    override fun close() {
        super.close()
        dbScope.cancel()
    }
}
