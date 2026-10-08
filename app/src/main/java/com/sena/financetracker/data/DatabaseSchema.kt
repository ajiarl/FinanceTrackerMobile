package com.sena.financetracker.data

import android.database.sqlite.SQLiteDatabase

/**
 * Definisi skema database lokal SQLite: konstanta tabel, nama kolom, dan DDL generator.
 */
object DatabaseSchema {

    // Database Indexes (Version 5 Migration)
    const val INDEX_TX_DATE = "idx_transactions_date"
    const val INDEX_TX_CATEGORY = "idx_transactions_category"
    const val INDEX_TX_ACCOUNT = "idx_transactions_account"
    const val INDEX_BUDGETS_CATEGORY = "idx_budgets_category"

    val INDEX_DDL_STATEMENTS = listOf(
        "CREATE INDEX IF NOT EXISTS $INDEX_TX_DATE ON $TABLE_TRANSACTIONS($COL_TX_DATE DESC);",
        "CREATE INDEX IF NOT EXISTS $INDEX_TX_CATEGORY ON $TABLE_TRANSACTIONS($COL_TX_CATEGORY);",
        "CREATE INDEX IF NOT EXISTS $INDEX_TX_ACCOUNT ON $TABLE_TRANSACTIONS($COL_TX_ACCOUNT_ID);",
        "CREATE INDEX IF NOT EXISTS $INDEX_BUDGETS_CATEGORY ON $TABLE_BUDGETS($COL_BUDGET_CATEGORY);"
    )

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
    const val COL_TX_TO_ACCOUNT_ID = "to_account_id"
    const val COL_TX_TO_ACCOUNT_NAME = "to_account_name"

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

    // Table Budgets
    const val TABLE_BUDGETS = "budgets"
    const val COL_BUDGET_ID = "id"
    const val COL_BUDGET_NAME = "name"
    const val COL_BUDGET_CATEGORY = "category"
    const val COL_BUDGET_LIMIT = "limit_amount"
    const val COL_BUDGET_PERIOD = "period"
    const val COL_BUDGET_IS_ACTIVE = "is_active"

    // Table Notifications
    const val TABLE_NOTIFICATIONS = "notifications"
    const val COL_NOTIF_ID = "id"
    const val COL_NOTIF_TITLE = "title"
    const val COL_NOTIF_MESSAGE = "message"
    const val COL_NOTIF_TYPE = "type"
    const val COL_NOTIF_IS_READ = "is_read"
    const val COL_NOTIF_CREATED_AT = "created_at"

    fun createAccountsTable(db: SQLiteDatabase) {
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
    }

    fun createCategoriesTable(db: SQLiteDatabase) {
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
    }

    fun createTransactionsTable(db: SQLiteDatabase) {
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
                $COL_TX_NOTES TEXT DEFAULT '',
                $COL_TX_TO_ACCOUNT_ID INTEGER,
                $COL_TX_TO_ACCOUNT_NAME TEXT
            )
            """.trimIndent()
        )
    }

    fun createBudgetsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_BUDGETS (
                $COL_BUDGET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BUDGET_NAME TEXT NOT NULL,
                $COL_BUDGET_CATEGORY TEXT NOT NULL,
                $COL_BUDGET_LIMIT REAL NOT NULL,
                $COL_BUDGET_PERIOD TEXT NOT NULL,
                $COL_BUDGET_IS_ACTIVE INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
    }

    fun createNotificationsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
                $COL_NOTIF_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTIF_TITLE TEXT NOT NULL,
                $COL_NOTIF_MESSAGE TEXT NOT NULL,
                $COL_NOTIF_TYPE TEXT NOT NULL,
                $COL_NOTIF_IS_READ INTEGER NOT NULL DEFAULT 0,
                $COL_NOTIF_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    fun createDatabaseIndexes(db: SQLiteDatabase) {
        for (ddl in INDEX_DDL_STATEMENTS) {
            db.execSQL(ddl)
        }
    }
}
