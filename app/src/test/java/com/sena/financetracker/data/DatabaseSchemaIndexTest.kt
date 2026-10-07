package com.sena.financetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class DatabaseSchemaIndexTest {

    @Test
    fun testDatabaseVersionIsFive() {
        assertEquals(5, AppDatabase.DATABASE_VERSION)
    }

    @Test
    fun testIndexNamesAndDdlStatements() {
        assertEquals("idx_transactions_date", AppDatabase.INDEX_TX_DATE)
        assertEquals("idx_transactions_category", AppDatabase.INDEX_TX_CATEGORY)
        assertEquals("idx_transactions_account", AppDatabase.INDEX_TX_ACCOUNT)
        assertEquals("idx_budgets_category", AppDatabase.INDEX_BUDGETS_CATEGORY)

        assertEquals(4, AppDatabase.INDEX_DDL_STATEMENTS.size)
        assertTrue(AppDatabase.INDEX_DDL_STATEMENTS.any { it.contains("idx_transactions_date") && it.contains("DESC") })
        assertTrue(AppDatabase.INDEX_DDL_STATEMENTS.any { it.contains("idx_transactions_category") })
        assertTrue(AppDatabase.INDEX_DDL_STATEMENTS.any { it.contains("idx_transactions_account") })
        assertTrue(AppDatabase.INDEX_BUDGETS_CATEGORY.let { name -> AppDatabase.INDEX_DDL_STATEMENTS.any { it.contains(name) } })
    }

    @Test
    fun testSqliteInMemoryPragmaIndexList() {
        // Uji eksekusi DDL langsung di SQLite in-memory engine (sqlite-jdbc jika ada, atau simulasi DDL check)
        var connectionExecuted = false
        try {
            Class.forName("org.sqlite.JDBC")
            val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
            conn.createStatement().use { stmt ->
                // Buat skema dasar seperti di AppDatabase.onCreate
                stmt.execute(
                    """
                    CREATE TABLE transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        date TEXT NOT NULL,
                        account_id INTEGER NOT NULL DEFAULT 1,
                        account_name TEXT NOT NULL DEFAULT 'Dompet Tunai',
                        notes TEXT DEFAULT ''
                    );
                    """.trimIndent()
                )
                stmt.execute(
                    """
                    CREATE TABLE budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        limit_amount REAL NOT NULL,
                        period TEXT NOT NULL,
                        is_active INTEGER NOT NULL DEFAULT 1
                    );
                    """.trimIndent()
                )

                // Eksekusi semua DDL statement AppDatabase
                for (ddl in AppDatabase.INDEX_DDL_STATEMENTS) {
                    stmt.execute(ddl)
                }

                // Verifikasi PRAGMA index_list('transactions')
                val txIndexes = mutableListOf<String>()
                stmt.executeQuery("PRAGMA index_list('transactions');").use { rs ->
                    while (rs.next()) {
                        txIndexes.add(rs.getString("name"))
                    }
                }
                assertTrue("Harus memiliki idx_transactions_date", txIndexes.contains("idx_transactions_date"))
                assertTrue("Harus memiliki idx_transactions_category", txIndexes.contains("idx_transactions_category"))
                assertTrue("Harus memiliki idx_transactions_account", txIndexes.contains("idx_transactions_account"))

                // Verifikasi PRAGMA index_list('budgets')
                val budgetIndexes = mutableListOf<String>()
                stmt.executeQuery("PRAGMA index_list('budgets');").use { rs ->
                    while (rs.next()) {
                        budgetIndexes.add(rs.getString("name"))
                    }
                }
                assertTrue("Harus memiliki idx_budgets_category", budgetIndexes.contains("idx_budgets_category"))
                connectionExecuted = true
            }
            conn.close()
        } catch (_: ClassNotFoundException) {
            // Jika sqlite-jdbc tidak ada di JVM unit test classpath, validasi via DDL syntax parsing
            connectionExecuted = false
        }

        // Jika sqlite-jdbc tidak tersedia di host test classpath, pastikan DDL format valid secara statis
        if (!connectionExecuted) {
            AppDatabase.INDEX_DDL_STATEMENTS.forEach { ddl ->
                assertTrue(ddl.startsWith("CREATE INDEX IF NOT EXISTS"))
                assertTrue(ddl.contains(" ON "))
            }
        }
    }
}
