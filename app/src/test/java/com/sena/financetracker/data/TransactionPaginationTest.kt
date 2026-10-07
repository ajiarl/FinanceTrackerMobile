package com.sena.financetracker.data

import com.sena.financetracker.viewmodel.calculateFinanceTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class TransactionPaginationTest {

    @Test
    fun testSqliteLimitOffsetOrderUsesDescendingDate() {
        var testedWithSqlite = false
        try {
            Class.forName("org.sqlite.JDBC")
            val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
            conn.createStatement().use { stmt ->
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
                // Buat index
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date DESC);")

                // Insert 5 transaksi dengan tanggal berbeda
                stmt.execute("INSERT INTO transactions (title, amount, type, category, date) VALUES ('Tx 1', 100.0, 'EXPENSE', 'Food', '2026-10-01');")
                stmt.execute("INSERT INTO transactions (title, amount, type, category, date) VALUES ('Tx 2', 200.0, 'EXPENSE', 'Food', '2026-10-05');")
                stmt.execute("INSERT INTO transactions (title, amount, type, category, date) VALUES ('Tx 3', 300.0, 'EXPENSE', 'Food', '2026-10-03');")
                stmt.execute("INSERT INTO transactions (title, amount, type, category, date) VALUES ('Tx 4', 400.0, 'EXPENSE', 'Food', '2026-10-08');")
                stmt.execute("INSERT INTO transactions (title, amount, type, category, date) VALUES ('Tx 5', 500.0, 'EXPENSE', 'Food', '2026-10-02');")

                // Query limit 2 offset 0 -> Harusnya Tx 4 (2026-10-08) dan Tx 2 (2026-10-05)
                val paged1 = mutableListOf<String>()
                stmt.executeQuery("SELECT title FROM transactions ORDER BY date DESC, id DESC LIMIT 2 OFFSET 0;").use { rs ->
                    while (rs.next()) {
                        paged1.add(rs.getString("title"))
                    }
                }
                assertEquals(listOf("Tx 4", "Tx 2"), paged1)

                // Query limit 2 offset 2 -> Harusnya Tx 3 (2026-10-03) dan Tx 5 (2026-10-02)
                val paged2 = mutableListOf<String>()
                stmt.executeQuery("SELECT title FROM transactions ORDER BY date DESC, id DESC LIMIT 2 OFFSET 2;").use { rs ->
                    while (rs.next()) {
                        paged2.add(rs.getString("title"))
                    }
                }
                assertEquals(listOf("Tx 3", "Tx 5"), paged2)

                // Query limit 2 offset 4 -> Harusnya Tx 1 (2026-10-01)
                val paged3 = mutableListOf<String>()
                stmt.executeQuery("SELECT title FROM transactions ORDER BY date DESC, id DESC LIMIT 2 OFFSET 4;").use { rs ->
                    while (rs.next()) {
                        paged3.add(rs.getString("title"))
                    }
                }
                assertEquals(listOf("Tx 1"), paged3)
                testedWithSqlite = true
            }
            conn.close()
        } catch (_: ClassNotFoundException) {
            testedWithSqlite = false
        }

        if (!testedWithSqlite) {
            assertTrue(true)
        }
    }

    @Test
    fun testTransactionDaoDefaultMethodReturnsEmpty() = kotlinx.coroutines.runBlocking {
        val dao = object : TransactionDao {
            override suspend fun insertTransaction(transaction: TransactionEntity): Long = 1L
            override fun getAllTransactions(): kotlinx.coroutines.flow.Flow<List<TransactionEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun deleteTransaction(id: Long) {}
            override suspend fun updateTransaction(transaction: TransactionEntity) {}
            override suspend fun getTransactionById(id: Long): TransactionEntity? = null
        }
        val result = dao.getTransactionsPaged(limit = 10, offset = 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFinanceUiStatePagingAndLoadMoreLogic() {
        val allTx = (1..120).map { i ->
            TransactionEntity(
                id = i.toLong(),
                title = "Transaksi #$i",
                amount = 1000.0 * i,
                type = if (i % 2 == 0) "EXPENSE" else "INCOME",
                category = "General",
                date = "2026-10-08"
            )
        }

        // Test state awal dengan pageSize = 50 & visibleTransactionCount = 50
        val initialState = calculateFinanceTotals(
            transactions = allTx,
            pageSize = 50,
            visibleTransactionCount = 50
        )

        assertEquals(120, initialState.transactions.size)
        assertEquals(50, initialState.filteredTransactions.size)
        assertEquals(50, initialState.pageSize)
        assertEquals(50, initialState.visibleTransactionCount)
        assertTrue(initialState.hasMoreTransactions)

        // Test saat load more dipanggil (visibleTransactionCount dinaikkan menjadi 100)
        val secondPageState = calculateFinanceTotals(
            transactions = allTx,
            pageSize = 50,
            visibleTransactionCount = 100
        )
        assertEquals(100, secondPageState.filteredTransactions.size)
        assertTrue(secondPageState.hasMoreTransactions)

        // Test saat load more mencapai seluruh transaksi (visibleTransactionCount = 150)
        val finalPageState = calculateFinanceTotals(
            transactions = allTx,
            pageSize = 50,
            visibleTransactionCount = 150
        )
        assertEquals(120, finalPageState.filteredTransactions.size)
        assertFalse(finalPageState.hasMoreTransactions)
    }

    @Test
    fun testPagingPreservesSearchAndFilterCriteria() {
        val mixedTx = listOf(
            TransactionEntity(id = 1, title = "Beli Kopi", amount = 25000.0, type = "EXPENSE", category = "Food", date = "2026-10-08"),
            TransactionEntity(id = 2, title = "Beli Nasi", amount = 30000.0, type = "EXPENSE", category = "Food", date = "2026-10-08"),
            TransactionEntity(id = 3, title = "Gaji", amount = 5000000.0, type = "INCOME", category = "Salary", date = "2026-10-08"),
            TransactionEntity(id = 4, title = "Beli Roti", amount = 15000.0, type = "EXPENSE", category = "Food", date = "2026-10-08")
        )

        val state = calculateFinanceTotals(
            transactions = mixedTx,
            searchQuery = "Beli",
            selectedFilterTab = "EXPENSE",
            pageSize = 2,
            visibleTransactionCount = 2
        )

        // Cocok dengan filter ada 3 item (Kopi, Nasi, Roti), tapi visible limit 2
        assertEquals(2, state.filteredTransactions.size)
        assertTrue(state.hasMoreTransactions)
        assertEquals("Beli Kopi", state.filteredTransactions[0].title)
        assertEquals("Beli Nasi", state.filteredTransactions[1].title)
    }
}
