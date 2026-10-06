package com.sena.financetracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "finance_tracker.db"
        const val DATABASE_VERSION = 2

        // Table Transactions
        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TX_ID = "id"
        const val COL_TX_TITLE = "title"
        const val COL_TX_AMOUNT = "amount"
        const val COL_TX_TYPE = "type"
        const val COL_TX_CATEGORY = "category"
        const val COL_TX_DATE = "date"
        const val COL_TX_ACCOUNT_ID = "account_id"
        const val COL_TX_ACCOUNT_NAME = "account_name"
        const val COL_TX_NOTES = "notes"

        // Table Accounts
        const val TABLE_ACCOUNTS = "accounts"
        const val COL_ACC_ID = "id"
        const val COL_ACC_NAME = "name"
        const val COL_ACC_TYPE = "type"
        const val COL_ACC_BALANCE = "balance"

        // Table Categories
        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_TYPE = "type"
        const val COL_CAT_COLOR = "color"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: AppDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dbScope = CoroutineScope(Dispatchers.IO)
    private val _transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val _accountsFlow = MutableStateFlow<List<AccountEntity>>(emptyList())
    private val _categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    init {
        dbScope.launch {
            refreshAllFlows()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_ACCOUNTS (
                $COL_ACC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ACC_NAME TEXT NOT NULL,
                $COL_ACC_TYPE TEXT NOT NULL,
                $COL_ACC_BALANCE REAL NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_TYPE TEXT NOT NULL,
                $COL_CAT_COLOR TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_TX_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TX_TITLE TEXT NOT NULL,
                $COL_TX_AMOUNT REAL NOT NULL,
                $COL_TX_TYPE TEXT NOT NULL,
                $COL_TX_CATEGORY TEXT NOT NULL,
                $COL_TX_DATE TEXT NOT NULL,
                $COL_TX_ACCOUNT_ID INTEGER NOT NULL DEFAULT 1,
                $COL_TX_ACCOUNT_NAME TEXT NOT NULL DEFAULT 'Dompet Tunai',
                $COL_TX_NOTES TEXT DEFAULT ''
            )
            """.trimIndent()
        )

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACCOUNTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // Seed default accounts
        val initialAccounts = listOf(
            Triple("Dompet Tunai", "cash", 500000.0),
            Triple("BCA", "bank", 5000000.0),
            Triple("GoPay", "e-wallet", 250000.0)
        )
        for ((name, type, balance) in initialAccounts) {
            val cv = ContentValues().apply {
                put(COL_ACC_NAME, name)
                put(COL_ACC_TYPE, type)
                put(COL_ACC_BALANCE, balance)
            }
            db.insert(TABLE_ACCOUNTS, null, cv)
        }

        // Seed default categories
        val defaultCategories = listOf(
            Triple("Makanan & Minuman", "EXPENSE", "#F97316"),
            Triple("Transportasi", "EXPENSE", "#3B82F6"),
            Triple("Belanja", "EXPENSE", "#EC4899"),
            Triple("Tagihan & Utilitas", "EXPENSE", "#8B5CF6"),
            Triple("Hiburan", "EXPENSE", "#F59E0B"),
            Triple("Lainnya", "EXPENSE", "#64748B"),
            Triple("Gaji", "INCOME", "#22C55E"),
            Triple("Freelance", "INCOME", "#10B981"),
            Triple("Investasi", "INCOME", "#6366F1"),
            Triple("Bonus", "INCOME", "#84CC16"),
            Triple("Lainnya", "INCOME", "#64748B")
        )
        for ((name, type, color) in defaultCategories) {
            val cv = ContentValues().apply {
                put(COL_CAT_NAME, name)
                put(COL_CAT_TYPE, type)
                put(COL_CAT_COLOR, color)
            }
            db.insert(TABLE_CATEGORIES, null, cv)
        }
    }

    private suspend fun refreshAllFlows() = withContext(Dispatchers.IO) {
        refreshTransactionsFlowInternal()
        refreshAccountsFlowInternal()
        refreshCategoriesFlowInternal()
    }

    private fun refreshTransactionsFlowInternal() {
        val list = mutableListOf<TransactionEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TX_ID DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_TX_ID)
            val titleIdx = c.getColumnIndexOrThrow(COL_TX_TITLE)
            val amountIdx = c.getColumnIndexOrThrow(COL_TX_AMOUNT)
            val typeIdx = c.getColumnIndexOrThrow(COL_TX_TYPE)
            val catIdx = c.getColumnIndexOrThrow(COL_TX_CATEGORY)
            val dateIdx = c.getColumnIndexOrThrow(COL_TX_DATE)
            val accIdIdx = c.getColumnIndex(COL_TX_ACCOUNT_ID)
            val accNameIdx = c.getColumnIndex(COL_TX_ACCOUNT_NAME)
            val notesIdx = c.getColumnIndex(COL_TX_NOTES)

            while (c.moveToNext()) {
                list.add(
                    TransactionEntity(
                        id = c.getLong(idIdx),
                        title = c.getString(titleIdx),
                        amount = c.getDouble(amountIdx),
                        type = c.getString(typeIdx),
                        category = c.getString(catIdx),
                        date = c.getString(dateIdx),
                        accountId = if (accIdIdx != -1) c.getLong(accIdIdx) else 1L,
                        accountName = if (accNameIdx != -1) c.getString(accNameIdx) else "Dompet Tunai",
                        notes = if (notesIdx != -1) c.getString(notesIdx) ?: "" else ""
                    )
                )
            }
        }
        _transactionsFlow.value = list
    }

    private fun refreshAccountsFlowInternal() {
        val list = mutableListOf<AccountEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ACCOUNTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_ACC_ID ASC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_ACC_ID)
            val nameIdx = c.getColumnIndexOrThrow(COL_ACC_NAME)
            val typeIdx = c.getColumnIndexOrThrow(COL_ACC_TYPE)
            val balanceIdx = c.getColumnIndexOrThrow(COL_ACC_BALANCE)

            while (c.moveToNext()) {
                list.add(
                    AccountEntity(
                        id = c.getLong(idIdx),
                        name = c.getString(nameIdx),
                        type = c.getString(typeIdx),
                        balance = c.getDouble(balanceIdx)
                    )
                )
            }
        }
        _accountsFlow.value = list
    }

    private fun refreshCategoriesFlowInternal() {
        val list = mutableListOf<CategoryEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CATEGORIES,
            null,
            null,
            null,
            null,
            null,
            "$COL_CAT_ID ASC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_CAT_ID)
            val nameIdx = c.getColumnIndexOrThrow(COL_CAT_NAME)
            val typeIdx = c.getColumnIndexOrThrow(COL_CAT_TYPE)
            val colorIdx = c.getColumnIndexOrThrow(COL_CAT_COLOR)

            while (c.moveToNext()) {
                list.add(
                    CategoryEntity(
                        id = c.getLong(idIdx),
                        name = c.getString(nameIdx),
                        type = c.getString(typeIdx),
                        color = c.getString(colorIdx)
                    )
                )
            }
        }
        _categoriesFlow.value = list
    }

    // ── TransactionDao Implementation ─────────────────────────────────────────
    val transactionDao: TransactionDao = object : TransactionDao {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> {
            return _transactionsFlow.asStateFlow()
        }

        override suspend fun insertTransaction(transaction: TransactionEntity): Long =
            withContext(Dispatchers.IO) {
                val db = writableDatabase
                val values = ContentValues().apply {
                    put(COL_TX_TITLE, transaction.title)
                    put(COL_TX_AMOUNT, transaction.amount)
                    put(COL_TX_TYPE, transaction.type)
                    put(COL_TX_CATEGORY, transaction.category)
                    put(COL_TX_DATE, transaction.date)
                    put(COL_TX_ACCOUNT_ID, transaction.accountId)
                    put(COL_TX_ACCOUNT_NAME, transaction.accountName)
                    put(COL_TX_NOTES, transaction.notes)
                }
                val insertedId = db.insert(TABLE_TRANSACTIONS, null, values)
                refreshTransactionsFlowInternal()
                insertedId
            }

        override suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_TRANSACTIONS, "$COL_TX_ID = ?", arrayOf(id.toString()))
            refreshTransactionsFlowInternal()
            Unit
        }
    }

    // ── AccountDao Implementation ─────────────────────────────────────────────
    val accountDao: AccountDao = object : AccountDao {
        override fun getAllAccounts(): Flow<List<AccountEntity>> {
            return _accountsFlow.asStateFlow()
        }

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
                if (c.moveToFirst()) {
                    AccountEntity(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_ACC_ID)),
                        name = c.getString(c.getColumnIndexOrThrow(COL_ACC_NAME)),
                        type = c.getString(c.getColumnIndexOrThrow(COL_ACC_TYPE)),
                        balance = c.getDouble(c.getColumnIndexOrThrow(COL_ACC_BALANCE))
                    )
                } else null
            }
        }

        override suspend fun insertAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_ACC_NAME, account.name)
                put(COL_ACC_TYPE, account.type)
                put(COL_ACC_BALANCE, account.balance)
            }
            val id = db.insert(TABLE_ACCOUNTS, null, values)
            refreshAccountsFlowInternal()
            id
        }

        override suspend fun updateBalance(id: Long, newBalance: Double) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_ACC_BALANCE, newBalance)
            }
            db.update(TABLE_ACCOUNTS, values, "$COL_ACC_ID = ?", arrayOf(id.toString()))
            refreshAccountsFlowInternal()
            Unit
        }

        override suspend fun adjustBalance(id: Long, delta: Double) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.execSQL(
                "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_BALANCE = $COL_ACC_BALANCE + ? WHERE $COL_ACC_ID = ?",
                arrayOf<Any>(delta, id)
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
        override fun getAllCategories(): Flow<List<CategoryEntity>> {
            return _categoriesFlow.asStateFlow()
        }

        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> {
            return _categoriesFlow.map { list ->
                list.filter { it.type.equals(type, ignoreCase = true) }
            }
        }

        override suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CAT_NAME, category.name)
                put(COL_CAT_TYPE, category.type)
                put(COL_CAT_COLOR, category.color)
            }
            val id = db.insert(TABLE_CATEGORIES, null, values)
            refreshCategoriesFlowInternal()
            id
        }

        override suspend fun deleteCategory(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete(TABLE_CATEGORIES, "$COL_CAT_ID = ?", arrayOf(id.toString()))
            refreshCategoriesFlowInternal()
            Unit
        }
    }
}
