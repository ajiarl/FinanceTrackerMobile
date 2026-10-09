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
import com.sena.financetracker.viewmodel.CashflowBarItem
import com.sena.financetracker.viewmodel.CategoryBreakdownItem
import com.sena.financetracker.viewmodel.ReportsAnalyticsState
import java.text.SimpleDateFormat
import java.util.Locale

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
    private val _transactionUpdateTrigger = MutableStateFlow(0L)
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

    /**
     * Menjalankan blok operasi database di dalam transaksi atomic SQLite (ACID).
     * Jika terjadi kegagalan atau exception, seluruh perubahan dibatalkan (rollback).
     * Setelah transaksi selesai (baik sukses maupun rollback), seluruh reactive StateFlow diperbarui.
     *
     * @param block Blok kode suspend yang dieksekusi di dalam transaksi.
     * @return Hasil pengembalian dari [block].
     */
    suspend fun <T> runInTransaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        val db = writableDatabase
        if (db.inTransaction()) {
            return@withContext block()
        }
        db.beginTransaction()
        try {
            val result = block()
            db.setTransactionSuccessful()
            result
        } finally {
            if (db.inTransaction()) {
                db.endTransaction()
            }
            refreshAllFlows()
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
        // Hentikan eager loading seluruh tabel transaksi ke RAM (zero RAM leak).
        // Sinyal pembaruan transaksi dialirkan murni via _transactionUpdateTrigger.
        _transactionUpdateTrigger.value = _transactionUpdateTrigger.value + 1L
    }

    /**
     * Mengambil snapshot isi cache memori _transactionsFlow untuk pengujian memory hygiene.
     * Sesuai arsitektur, StateFlow ini tidak lagi memuat baris transaksi secara eager (harus tetap kosong).
     */
    fun getTransactionsFlowMemoryCacheSnapshot(): List<TransactionEntity> {
        return _transactionsFlow.value
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
        override suspend fun <T> runInTransaction(block: suspend () -> T): T {
            return this@AppDatabase.runInTransaction(block)
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _transactionUpdateTrigger.map {
            withContext(Dispatchers.IO) {
                val list = mutableListOf<TransactionEntity>()
                val db = readableDatabase
                val cursor = db.query(TABLE_TRANSACTIONS, null, null, null, null, null, "$COL_TX_ID DESC")
                cursor.use { c ->
                    while (c.moveToNext()) {
                        list.add(DatabaseMappers.mapTransaction(c))
                    }
                }
                list
            }
        }

        override fun getTransactionUpdateTrigger(): Flow<Long> = _transactionUpdateTrigger.asStateFlow()

        override suspend fun getTransactionCount(): Int = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_TRANSACTIONS", null)
            cursor.use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        }

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

        override suspend fun getTransactionsPagedWithHasMore(limit: Int, offset: Int): PagedTransactionsResult =
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
                    "$offset, ${limit + 1}"
                )
                cursor.use { c ->
                    while (c.moveToNext()) {
                        list.add(DatabaseMappers.mapTransaction(c))
                    }
                }
                val hasMore = list.size > limit
                val resultList = if (hasMore) list.take(limit) else list
                PagedTransactionsResult(transactions = resultList, hasMore = hasMore)
            }

        override suspend fun getFinanceSummary(
            query: String?,
            category: String?,
            startDate: String?,
            endDate: String?
        ): FinanceSummary = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()

            if (!query.isNullOrBlank()) {
                conditions.add("(title LIKE ? OR notes LIKE ?)")
                val q = "%${query.trim()}%"
                args.add(q)
                args.add(q)
            }
            if (!category.isNullOrBlank() && !category.trim().equals("ALL", ignoreCase = true)) {
                conditions.add("category = ?")
                args.add(category.trim())
            }
            if (!startDate.isNullOrBlank()) {
                conditions.add("date >= ?")
                args.add(startDate.trim())
            }
            if (!endDate.isNullOrBlank()) {
                conditions.add("date <= ?")
                args.add(endDate.trim())
            }

            val where = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val sql = """
                SELECT 
                    COALESCE(SUM(CASE WHEN UPPER(type) = 'INCOME' THEN amount ELSE 0.0 END), 0.0) AS totalIncome,
                    COALESCE(SUM(CASE WHEN UPPER(type) = 'EXPENSE' THEN amount ELSE 0.0 END), 0.0) AS totalExpense
                FROM $TABLE_TRANSACTIONS
                $where
            """.trimIndent()

            val cursor = db.rawQuery(sql, if (args.isNotEmpty()) args.toTypedArray() else null)
            cursor.use { c ->
                if (c.moveToFirst()) {
                    val income = c.getDouble(c.getColumnIndexOrThrow("totalIncome"))
                    val expense = c.getDouble(c.getColumnIndexOrThrow("totalExpense"))
                    FinanceSummary(totalIncome = income, totalExpense = expense)
                } else {
                    FinanceSummary(totalIncome = 0.0, totalExpense = 0.0)
                }
            }
        }

        override suspend fun getFilteredTransactionsPaged(
            query: String?,
            type: String?,
            category: String?,
            startDate: String?,
            endDate: String?,
            limit: Int,
            offset: Int
        ): PagedTransactionsResult = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()

            if (!query.isNullOrBlank()) {
                conditions.add("(title LIKE ? OR notes LIKE ?)")
                val q = "%${query.trim()}%"
                args.add(q)
                args.add(q)
            }
            if (!type.isNullOrBlank() && !type.trim().equals("ALL", ignoreCase = true)) {
                conditions.add("UPPER(type) = ?")
                args.add(type.trim().uppercase(Locale.ROOT))
            }
            if (!category.isNullOrBlank() && !category.trim().equals("ALL", ignoreCase = true)) {
                conditions.add("category = ?")
                args.add(category.trim())
            }
            if (!startDate.isNullOrBlank()) {
                conditions.add("date >= ?")
                args.add(startDate.trim())
            }
            if (!endDate.isNullOrBlank()) {
                conditions.add("date <= ?")
                args.add(endDate.trim())
            }

            val where = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val fetchLimit = limit + 1
            val sql = """
                SELECT * FROM $TABLE_TRANSACTIONS
                $where
                ORDER BY $COL_TX_DATE DESC, $COL_TX_ID DESC
                LIMIT $fetchLimit OFFSET $offset
            """.trimIndent()

            val cursor = db.rawQuery(sql, if (args.isNotEmpty()) args.toTypedArray() else null)
            val list = mutableListOf<TransactionEntity>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(DatabaseMappers.mapTransaction(c))
                }
            }
            val hasMore = list.size > limit
            val resultList = if (hasMore) list.take(limit) else list
            PagedTransactionsResult(transactions = resultList, hasMore = hasMore)
        }

        override suspend fun getCategoryExpenseSummary(
            startDate: String?,
            endDate: String?
        ): List<CategoryExpenseSummary> = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val conditions = mutableListOf("UPPER(type) = 'EXPENSE'")
            val args = mutableListOf<String>()
            if (!startDate.isNullOrBlank()) {
                conditions.add("date >= ?")
                args.add(startDate.trim())
            }
            if (!endDate.isNullOrBlank()) {
                conditions.add("date <= ?")
                args.add(endDate.trim())
            }
            val where = "WHERE " + conditions.joinToString(" AND ")
            val sql = """
                SELECT TRIM(category) AS category, COALESCE(SUM(amount), 0.0) AS totalAmount
                FROM $TABLE_TRANSACTIONS
                $where
                GROUP BY LOWER(TRIM(category))
                ORDER BY totalAmount DESC
            """.trimIndent()

            val result = mutableListOf<CategoryExpenseSummary>()
            val cursor = db.rawQuery(sql, if (args.isNotEmpty()) args.toTypedArray() else null)
            cursor.use { c ->
                while (c.moveToNext()) {
                    val catName = c.getString(c.getColumnIndexOrThrow("category")) ?: ""
                    val amount = c.getDouble(c.getColumnIndexOrThrow("totalAmount"))
                    result.add(CategoryExpenseSummary(category = catName, totalAmount = amount))
                }
            }
            result
        }

        override suspend fun getMonthlyCashFlowSummary(
            startDate: String?,
            endDate: String?
        ): List<MonthlyCashFlowSummary> = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()
            if (!startDate.isNullOrBlank()) {
                conditions.add("date >= ?")
                args.add(startDate.trim())
            }
            if (!endDate.isNullOrBlank()) {
                conditions.add("date <= ?")
                args.add(endDate.trim())
            }
            val where = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val sql = """
                SELECT 
                    substr(date, 1, 7) AS month,
                    COALESCE(SUM(CASE WHEN UPPER(type) = 'INCOME' THEN amount ELSE 0.0 END), 0.0) AS income,
                    COALESCE(SUM(CASE WHEN UPPER(type) = 'EXPENSE' THEN amount ELSE 0.0 END), 0.0) AS expense
                FROM $TABLE_TRANSACTIONS
                $where
                GROUP BY substr(date, 1, 7)
                ORDER BY month ASC
            """.trimIndent()

            val result = mutableListOf<MonthlyCashFlowSummary>()
            val cursor = db.rawQuery(sql, if (args.isNotEmpty()) args.toTypedArray() else null)
            cursor.use { c ->
                while (c.moveToNext()) {
                    val monthStr = c.getString(c.getColumnIndexOrThrow("month")) ?: ""
                    val inc = c.getDouble(c.getColumnIndexOrThrow("income"))
                    val exp = c.getDouble(c.getColumnIndexOrThrow("expense"))
                    result.add(MonthlyCashFlowSummary(month = monthStr, income = inc, expense = exp))
                }
            }
            result
        }

        override suspend fun getReportsAnalytics(
            startDate: String?,
            endDate: String?
        ): ReportsAnalyticsState = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()
            if (!startDate.isNullOrBlank()) {
                conditions.add("date >= ?")
                args.add(startDate.trim())
            }
            if (!endDate.isNullOrBlank()) {
                conditions.add("date <= ?")
                args.add(endDate.trim())
            }
            val where = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val totalSql = """
                SELECT 
                    COALESCE(SUM(CASE WHEN UPPER(type) = 'INCOME' THEN amount ELSE 0.0 END), 0.0) AS totalIncome,
                    COALESCE(SUM(CASE WHEN UPPER(type) = 'EXPENSE' THEN amount ELSE 0.0 END), 0.0) AS totalExpense
                FROM $TABLE_TRANSACTIONS
                $where
            """.trimIndent()

            var totalIncome = 0.0
            var totalExpense = 0.0
            val summaryCursor = db.rawQuery(totalSql, if (args.isNotEmpty()) args.toTypedArray() else null)
            summaryCursor.use { c ->
                if (c.moveToFirst()) {
                    totalIncome = c.getDouble(c.getColumnIndexOrThrow("totalIncome"))
                    totalExpense = c.getDouble(c.getColumnIndexOrThrow("totalExpense"))
                }
            }

            // Ambil mapping warna dari tabel categories
            val colorMap = mutableMapOf<String, String>()
            val catCursor = db.query(TABLE_CATEGORIES, arrayOf(COL_CAT_NAME, COL_CAT_COLOR), null, null, null, null, null)
            catCursor.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(c.getColumnIndexOrThrow(COL_CAT_NAME)).trim().lowercase(Locale.ROOT)
                    val color = c.getString(c.getColumnIndexOrThrow(COL_CAT_COLOR))
                    colorMap[name] = color
                }
            }

            val defaultColors = listOf("#F97316", "#3B82F6", "#EC4899", "#8B5CF6", "#10B981", "#EAB308", "#64748B")
            val catSummaries = getCategoryExpenseSummary(startDate, endDate)
            val categoryBreakdown = catSummaries.map { cat ->
                val displayName = cat.category.trim().ifBlank { "Lainnya" }
                val pct = if (totalExpense > 0.0) ((cat.totalAmount / totalExpense) * 100).toInt() else 0
                val col = colorMap[cat.category.trim().lowercase(Locale.ROOT)]
                    ?: defaultColors[Math.abs(displayName.hashCode()) % defaultColors.size]
                CategoryBreakdownItem(
                    category = displayName,
                    totalAmount = cat.totalAmount,
                    percentage = pct,
                    color = col
                )
            }

            val cfSummaries = getMonthlyCashFlowSummary(startDate, endDate)
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.ROOT)
            val sdfDisplayMonth = SimpleDateFormat("MMM yy", Locale.forLanguageTag("id-ID"))
            val cashflowBars = cfSummaries.map { cf ->
                val label = try {
                    val d = sdfMonth.parse(cf.month)
                    if (d != null) sdfDisplayMonth.format(d).uppercase(Locale.ROOT) else cf.month
                } catch (e: Exception) {
                    cf.month
                }
                CashflowBarItem(label = label, income = cf.income, expense = cf.expense)
            }

            val netSavings = totalIncome - totalExpense
            val savingRate = if (totalIncome > 0) {
                val rate = ((netSavings / totalIncome) * 100).toInt()
                rate.coerceIn(-100, 100)
            } else if (totalExpense > 0) {
                -100
            } else {
                0
            }

            val savingStatus = when {
                totalIncome == 0.0 && totalExpense == 0.0 -> "NORMAL"
                savingRate >= 30 -> "HEMAT"
                savingRate >= 10 -> "NORMAL"
                else -> "BOROS"
            }

            ReportsAnalyticsState(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netSavings = netSavings,
                savingRate = savingRate,
                savingStatus = savingStatus,
                categoryBreakdown = categoryBreakdown,
                cashflowBars = cashflowBars
            )
        }

        override suspend fun getCategoryExpensesForPeriod(periodPrefix: String): List<CategorySpentSummary> =
            withContext(Dispatchers.IO) {
                val list = mutableListOf<CategorySpentSummary>()
                val db = readableDatabase
                val sql = """
                    SELECT $COL_TX_CATEGORY AS category, COALESCE(SUM($COL_TX_AMOUNT), 0.0) AS spent
                    FROM $TABLE_TRANSACTIONS
                    WHERE UPPER($COL_TX_TYPE) = 'EXPENSE' AND $COL_TX_DATE LIKE ? || '%'
                    GROUP BY $COL_TX_CATEGORY
                """.trimIndent()
                val cursor = db.rawQuery(sql, arrayOf(periodPrefix))
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val cat = c.getString(c.getColumnIndexOrThrow("category")) ?: ""
                        val spent = c.getDouble(c.getColumnIndexOrThrow("spent"))
                        list.add(CategorySpentSummary(category = cat, spent = spent))
                    }
                }
                list
            }

        override suspend fun getCategorySpentForPeriod(category: String, periodPrefix: String): Double =
            withContext(Dispatchers.IO) {
                val db = readableDatabase
                val sql = """
                    SELECT COALESCE(SUM($COL_TX_AMOUNT), 0.0)
                    FROM $TABLE_TRANSACTIONS
                    WHERE UPPER($COL_TX_TYPE) = 'EXPENSE'
                      AND TRIM(LOWER($COL_TX_CATEGORY)) = TRIM(LOWER(?))
                      AND $COL_TX_DATE LIKE ? || '%'
                """.trimIndent()
                val cursor = db.rawQuery(sql, arrayOf(category, periodPrefix))
                cursor.use { c ->
                    if (c.moveToFirst()) c.getDouble(0) else 0.0
                }
            }

        override suspend fun getSpentForCategoryAndPeriod(category: String, periodPrefix: String): Double {
            return getCategorySpentForPeriod(category, periodPrefix)
        }

        override suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(transaction)
            val id = db.insertOrThrow(TABLE_TRANSACTIONS, null, values)
            if (id == -1L) {
                throw IllegalStateException("Gagal menyimpan transaksi ke database: insert mengembalikan -1")
            }
            if (!db.inTransaction()) {
                refreshTransactionsFlowInternal()
            }
            id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(transaction)
            db.update(TABLE_TRANSACTIONS, values, "$COL_TX_ID = ?", arrayOf(transaction.id.toString()))
            if (!db.inTransaction()) {
                refreshTransactionsFlowInternal()
            }
            Unit
        }

        override suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_TRANSACTIONS, "$COL_TX_ID = ?", arrayOf(id.toString()))
            if (!db.inTransaction()) {
                refreshTransactionsFlowInternal()
            }
            Unit
        }

        override suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_TRANSACTIONS, null, null)
            if (!db.inTransaction()) {
                refreshTransactionsFlowInternal()
            }
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

        override suspend fun getTransactionsForPeriod(
            startDate: String?,
            endDate: String?,
            limit: Int
        ): List<TransactionEntity> = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()

            if (!startDate.isNullOrBlank()) {
                conditions.add("$COL_TX_DATE >= ?")
                args.add(startDate.trim())
            }
            if (!endDate.isNullOrBlank()) {
                conditions.add("$COL_TX_DATE <= ?")
                args.add(endDate.trim())
            }

            val where = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val sql = """
                SELECT * FROM $TABLE_TRANSACTIONS
                $where
                ORDER BY $COL_TX_DATE DESC, $COL_TX_ID DESC
                LIMIT $limit
            """.trimIndent()

            val cursor = db.rawQuery(sql, if (args.isNotEmpty()) args.toTypedArray() else null)
            val list = mutableListOf<TransactionEntity>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(DatabaseMappers.mapTransaction(c))
                }
            }
            list
        }

        override suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> =
            withContext(Dispatchers.IO) {
                val db = writableDatabase
                val ids = mutableListOf<Long>()
                val wasInTx = db.inTransaction()
                if (!wasInTx) db.beginTransaction()
                try {
                    for (tx in transactions) {
                        val values = DatabaseMappers.toContentValues(tx)
                        val id = db.insertOrThrow(TABLE_TRANSACTIONS, null, values)
                        if (id == -1L) {
                            throw IllegalStateException("Gagal menyimpan transaksi batch ke database: insert mengembalikan -1")
                        }
                        ids.add(id)
                    }
                    if (!wasInTx) db.setTransactionSuccessful()
                } finally {
                    if (!wasInTx && db.inTransaction()) {
                        db.endTransaction()
                    }
                }
                if (!wasInTx) {
                    refreshTransactionsFlowInternal()
                }
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
            val id = db.insertOrThrow(TABLE_ACCOUNTS, null, values)
            if (id == -1L) {
                throw IllegalStateException("Gagal menyimpan akun ke database: insert mengembalikan -1")
            }
            if (!db.inTransaction()) {
                refreshAccountsFlowInternal()
            }
            id
        }

        override suspend fun updateBalance(id: Long, newBalance: Double) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = android.content.ContentValues().apply { put(COL_ACC_BALANCE, newBalance) }
            db.update(TABLE_ACCOUNTS, values, "$COL_ACC_ID = ?", arrayOf(id.toString()))
            if (!db.inTransaction()) {
                refreshAccountsFlowInternal()
            }
            Unit
        }

        override suspend fun adjustBalance(id: Long, delta: Double) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.execSQL(
                "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_BALANCE = $COL_ACC_BALANCE + ? WHERE $COL_ACC_ID = ?",
                arrayOf(delta.toString(), id.toString())
            )
            if (!db.inTransaction()) {
                refreshAccountsFlowInternal()
            }
            Unit
        }

        override suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_ACCOUNTS, "$COL_ACC_ID = ?", arrayOf(id.toString()))
            if (!db.inTransaction()) {
                refreshAccountsFlowInternal()
            }
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
            val id = db.insertOrThrow(TABLE_CATEGORIES, null, values)
            if (id == -1L) {
                throw IllegalStateException("Gagal menyimpan kategori ke database: insert mengembalikan -1")
            }
            if (!db.inTransaction()) {
                refreshCategoriesFlowInternal()
            }
            id
        }

        override suspend fun updateCategory(category: CategoryEntity): Int = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(category)
            val count = db.update(TABLE_CATEGORIES, values, "$COL_CAT_ID = ?", arrayOf(category.id.toString()))
            if (!db.inTransaction()) {
                refreshCategoriesFlowInternal()
            }
            count
        }

        override suspend fun deleteCategory(id: Long): Int = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val count = db.delete(TABLE_CATEGORIES, "$COL_CAT_ID = ?", arrayOf(id.toString()))
            if (!db.inTransaction()) {
                refreshCategoriesFlowInternal()
            }
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
            val id = db.insertOrThrow(TABLE_BUDGETS, null, values)
            if (id == -1L) {
                throw IllegalStateException("Gagal menyimpan anggaran ke database: insert mengembalikan -1")
            }
            if (!db.inTransaction()) {
                refreshBudgetsFlowInternal()
            }
            id
        }

        override suspend fun updateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = DatabaseMappers.toContentValues(budget)
            db.update(TABLE_BUDGETS, values, "$COL_BUDGET_ID = ?", arrayOf(budget.id.toString()))
            if (!db.inTransaction()) {
                refreshBudgetsFlowInternal()
            }
            Unit
        }

        override suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_BUDGETS, "$COL_BUDGET_ID = ?", arrayOf(id.toString()))
            if (!db.inTransaction()) {
                refreshBudgetsFlowInternal()
            }
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
            val id = db.insertOrThrow(TABLE_NOTIFICATIONS, null, values)
            if (id == -1L) {
                throw IllegalStateException("Gagal menyimpan notifikasi ke database: insert mengembalikan -1")
            }
            if (!db.inTransaction()) {
                refreshNotificationsFlowInternal()
            }
            id
        }

        override suspend fun markAsRead(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = android.content.ContentValues().apply { put(COL_NOTIF_IS_READ, 1) }
            db.update(TABLE_NOTIFICATIONS, values, "$COL_NOTIF_ID = ?", arrayOf(id.toString()))
            if (!db.inTransaction()) {
                refreshNotificationsFlowInternal()
            }
            Unit
        }

        override suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = android.content.ContentValues().apply { put(COL_NOTIF_IS_READ, 1) }
            db.update(TABLE_NOTIFICATIONS, values, null, null)
            if (!db.inTransaction()) {
                refreshNotificationsFlowInternal()
            }
            Unit
        }

        override suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_NOTIFICATIONS, null, null)
            if (!db.inTransaction()) {
                refreshNotificationsFlowInternal()
            }
            Unit
        }

        override suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_NOTIFICATIONS, "$COL_NOTIF_ID = ?", arrayOf(id.toString()))
            if (!db.inTransaction()) {
                refreshNotificationsFlowInternal()
            }
            Unit
        }
    }

    override fun close() {
        super.close()
        dbScope.cancel()
    }
}
