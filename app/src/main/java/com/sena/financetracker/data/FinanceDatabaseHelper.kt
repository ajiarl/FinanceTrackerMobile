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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FinanceDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
), TransactionDao {

    companion object {
        const val DATABASE_NAME = "finance_tracker.db"
        const val DATABASE_VERSION = 1

        const val TABLE_TRANSACTIONS = "transactions"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_TYPE = "type"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_DATE = "date"
    }

    private val dbScope = CoroutineScope(Dispatchers.IO)
    private val _transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    init {
        dbScope.launch {
            refreshFlow()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_AMOUNT REAL NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_DATE TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        onCreate(db)
    }

    private fun readAllTransactionsFromDb(): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ID DESC"
        )

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
            val titleIndex = c.getColumnIndexOrThrow(COLUMN_TITLE)
            val amountIndex = c.getColumnIndexOrThrow(COLUMN_AMOUNT)
            val typeIndex = c.getColumnIndexOrThrow(COLUMN_TYPE)
            val categoryIndex = c.getColumnIndexOrThrow(COLUMN_CATEGORY)
            val dateIndex = c.getColumnIndexOrThrow(COLUMN_DATE)

            while (c.moveToNext()) {
                list.add(
                    TransactionEntity(
                        id = c.getLong(idIndex),
                        title = c.getString(titleIndex),
                        amount = c.getDouble(amountIndex),
                        type = c.getString(typeIndex),
                        category = c.getString(categoryIndex),
                        date = c.getString(dateIndex)
                    )
                )
            }
        }
        return list
    }

    private suspend fun refreshFlow() {
        val items = withContext(Dispatchers.IO) {
            readAllTransactionsFromDb()
        }
        _transactionsFlow.value = items
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, transaction.title)
            put(COLUMN_AMOUNT, transaction.amount)
            put(COLUMN_TYPE, transaction.type)
            put(COLUMN_CATEGORY, transaction.category)
            put(COLUMN_DATE, transaction.date)
        }
        val insertedId = db.insert(TABLE_TRANSACTIONS, null, values)
        refreshFlow()
        insertedId
    }

    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return _transactionsFlow.asStateFlow()
    }

    override suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_TRANSACTIONS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        refreshFlow()
        Unit
    }
}
