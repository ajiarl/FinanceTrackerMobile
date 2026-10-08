package com.sena.financetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseDecompositionUnitTest {

    @Test
    fun testDatabaseSchemaConstants() {
        assertEquals("transactions", DatabaseSchema.TABLE_TRANSACTIONS)
        assertEquals("accounts", DatabaseSchema.TABLE_ACCOUNTS)
        assertEquals("categories", DatabaseSchema.TABLE_CATEGORIES)
        assertEquals("budgets", DatabaseSchema.TABLE_BUDGETS)
        assertEquals("notifications", DatabaseSchema.TABLE_NOTIFICATIONS)

        assertEquals("id", DatabaseSchema.COL_TX_ID)
        assertEquals("amount", DatabaseSchema.COL_TX_AMOUNT)
        assertEquals("balance", DatabaseSchema.COL_ACC_BALANCE)
        assertEquals("limit_amount", DatabaseSchema.COL_BUDGET_LIMIT)
        assertEquals("created_at", DatabaseSchema.COL_NOTIF_CREATED_AT)

        assertEquals(4, DatabaseSchema.INDEX_DDL_STATEMENTS.size)
    }

    @Test
    fun testAppDatabaseDelegatesToSchema() {
        assertEquals(DatabaseSchema.TABLE_TRANSACTIONS, AppDatabase.TABLE_TRANSACTIONS)
        assertEquals(DatabaseSchema.TABLE_ACCOUNTS, AppDatabase.TABLE_ACCOUNTS)
        assertEquals(DatabaseSchema.TABLE_CATEGORIES, AppDatabase.TABLE_CATEGORIES)
        assertEquals(DatabaseSchema.TABLE_BUDGETS, AppDatabase.TABLE_BUDGETS)
        assertEquals(DatabaseSchema.TABLE_NOTIFICATIONS, AppDatabase.TABLE_NOTIFICATIONS)

        assertEquals(DatabaseSchema.INDEX_TX_DATE, AppDatabase.INDEX_TX_DATE)
        assertEquals(DatabaseSchema.INDEX_TX_CATEGORY, AppDatabase.INDEX_TX_CATEGORY)
        assertEquals(DatabaseSchema.INDEX_TX_ACCOUNT, AppDatabase.INDEX_TX_ACCOUNT)
        assertEquals(DatabaseSchema.INDEX_BUDGETS_CATEGORY, AppDatabase.INDEX_BUDGETS_CATEGORY)
        assertEquals(DatabaseSchema.INDEX_DDL_STATEMENTS, AppDatabase.INDEX_DDL_STATEMENTS)
    }

    @Test
    fun testDatabaseMappersContentValuesCreation() {
        val tx = TransactionEntity(
            id = 1L,
            title = "Makan Siang",
            amount = 35000.0,
            type = "EXPENSE",
            category = "Makanan & Minuman",
            date = "2026-10-08",
            accountId = 2L,
            accountName = "BCA",
            notes = "Nasi Padang"
        )
        val txValues = DatabaseMappers.toContentValues(tx)
        org.junit.Assert.assertNotNull(txValues)

        val acc = AccountEntity(id = 1L, name = "Tunai", type = "cash", balance = 150000.0)
        val accValues = DatabaseMappers.toContentValues(acc)
        org.junit.Assert.assertNotNull(accValues)

        val cat = CategoryEntity(id = 1L, name = "Gaji", type = "INCOME", color = "#22C55E")
        val catValues = DatabaseMappers.toContentValues(cat)
        org.junit.Assert.assertNotNull(catValues)

        val b = BudgetEntity(id = 1L, name = "Bulanan", category = "Makan", limitAmount = 1000000.0, period = "MONTHLY", isActive = true)
        val bValues = DatabaseMappers.toContentValues(b)
        org.junit.Assert.assertNotNull(bValues)

        val n = NotificationEntity(id = 1L, title = "Info", message = "Pesan", type = "BUDGET_ALERT", isRead = false, createdAt = 123456L)
        val nValues = DatabaseMappers.toContentValues(n)
        org.junit.Assert.assertNotNull(nValues)
    }
}
