package com.sena.financetracker.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Seeder data bawaan untuk inisialisasi awal database SQLite Finance Tracker.
 */
object DatabaseSeeder {

    fun seedInitialData(db: SQLiteDatabase) {
        seedAccounts(db)
        seedCategories(db)
    }

    private fun seedAccounts(db: SQLiteDatabase) {
        val initialAccounts = listOf(
            Triple("Dompet Tunai", "cash", 500000.0),
            Triple("BCA", "bank", 5000000.0),
            Triple("GoPay", "e-wallet", 250000.0)
        )
        for ((name, type, balance) in initialAccounts) {
            val cv = ContentValues().apply {
                put(DatabaseSchema.COL_ACC_NAME, name)
                put(DatabaseSchema.COL_ACC_TYPE, type)
                put(DatabaseSchema.COL_ACC_BALANCE, balance)
            }
            db.insert(DatabaseSchema.TABLE_ACCOUNTS, null, cv)
        }
    }

    private fun seedCategories(db: SQLiteDatabase) {
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
                put(DatabaseSchema.COL_CAT_NAME, name)
                put(DatabaseSchema.COL_CAT_TYPE, type)
                put(DatabaseSchema.COL_CAT_COLOR, color)
            }
            db.insert(DatabaseSchema.TABLE_CATEGORIES, null, cv)
        }
    }
}
