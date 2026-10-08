package com.sena.financetracker.data

import android.database.sqlite.SQLiteDatabase

/**
 * Pengelola migrasi skema database SQLite (evolusi v1 sampai v6).
 */
object DatabaseMigrations {

    fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            DatabaseSchema.createBudgetsTable(db)
        }
        if (oldVersion < 4) {
            DatabaseSchema.createNotificationsTable(db)
        }
        if (oldVersion < 5) {
            DatabaseSchema.createDatabaseIndexes(db)
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE ${DatabaseSchema.TABLE_TRANSACTIONS} ADD COLUMN ${DatabaseSchema.COL_TX_TO_ACCOUNT_ID} INTEGER;")
            db.execSQL("ALTER TABLE ${DatabaseSchema.TABLE_TRANSACTIONS} ADD COLUMN ${DatabaseSchema.COL_TX_TO_ACCOUNT_NAME} TEXT;")
        }
    }
}
