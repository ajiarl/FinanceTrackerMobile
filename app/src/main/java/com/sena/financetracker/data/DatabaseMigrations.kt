package com.sena.financetracker.data

import android.database.sqlite.SQLiteDatabase

/**
 * Pengelola migrasi skema database SQLite (evolusi v1 sampai v5).
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
    }
}
