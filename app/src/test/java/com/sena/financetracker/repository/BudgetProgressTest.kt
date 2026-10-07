package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BudgetProgressTest {

    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeAccountDao: FakeAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeBudgetDao: FakeBudgetDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        fakeTransactionDao = FakeTransactionDao()
        fakeAccountDao = FakeAccountDao()
        fakeCategoryDao = FakeCategoryDao()
        fakeBudgetDao = FakeBudgetDao()

        repository = TransactionRepository(
            transactionDao = fakeTransactionDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao,
            budgetDao = fakeBudgetDao
        )
    }

    @Test
    fun testCalculationSpentAmountAndWarningPercentage() = runBlocking {
        // 1. Add budget limit 1.000.000 for "Makanan" in "2026-10"
        repository.addBudget(
            name = "Makan Siang",
            category = "Makanan",
            limitAmount = 1000000.0,
            period = "2026-10"
        )

        // 2. Add expenses in "Makanan" (500.000 + 350.000 = 850.000 -> 85%)
        repository.insertTransaction(
            TransactionEntity(
                id = 1,
                title = "Makan Siang",
                amount = 500000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-10-05"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                id = 2,
                title = "Kopi & Snack",
                amount = 350000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-10-12"
            )
        )

        // 3. Add expense in different category (Transportasi) -> Should NOT count towards Makanan
        repository.insertTransaction(
            TransactionEntity(
                id = 3,
                title = "Bensin Motor",
                amount = 100000.0,
                type = "EXPENSE",
                category = "Transportasi",
                date = "2026-10-15"
            )
        )

        // 4. Add income in same category (Makanan) -> Should NOT count as spent
        repository.insertTransaction(
            TransactionEntity(
                id = 4,
                title = "Reimburse Makan",
                amount = 200000.0,
                type = "INCOME",
                category = "Makanan",
                date = "2026-10-18"
            )
        )

        // 5. Add expense in previous month -> Should NOT count for 2026-10
        repository.insertTransaction(
            TransactionEntity(
                id = 5,
                title = "Makan Bulan Lalu",
                amount = 250000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-09-20"
            )
        )

        val progressList = repository.getBudgetProgress("2026-10").first()
        assertEquals(1, progressList.size)

        val item = progressList[0]
        assertEquals("Makan Siang", item.budget.name)
        assertEquals("Makanan", item.budget.category)
        assertEquals(850000.0, item.spentAmount, 0.001)
        assertEquals(85, item.percentage)
        assertFalse(item.isOver)
        assertEquals("WARNING", item.statusLevel)
    }

    @Test
    fun testOverbudgetCriticalStatus() = runBlocking {
        // Add budget limit 1.000.000
        repository.addBudget(
            name = "Belanja Gadget",
            category = "Belanja",
            limitAmount = 1000000.0,
            period = "2026-10"
        )

        // Add expense 1.200.000 (120% -> CRITICAL & isOver = true)
        repository.insertTransaction(
            TransactionEntity(
                id = 1,
                title = "Beli Headphone",
                amount = 1200000.0,
                type = "EXPENSE",
                category = "Belanja",
                date = "2026-10-08"
            )
        )

        val progressList = repository.getBudgetProgress("2026-10").first()
        assertEquals(1, progressList.size)

        val item = progressList[0]
        assertEquals(1200000.0, item.spentAmount, 0.001)
        assertEquals(120, item.percentage)
        assertTrue(item.isOver)
        assertEquals("CRITICAL", item.statusLevel)
    }

    @Test
    fun testSafeStatusWhenBelow80Percent() = runBlocking {
        repository.addBudget(
            name = "Langganan Cloud",
            category = "Tagihan",
            limitAmount = 500000.0,
            period = "2026-10"
        )

        repository.insertTransaction(
            TransactionEntity(
                id = 1,
                title = "VPS Server",
                amount = 250000.0,
                type = "EXPENSE",
                category = "Tagihan",
                date = "2026-10-01"
            )
        )

        val progressList = repository.getBudgetProgress("2026-10").first()
        assertEquals(1, progressList.size)

        val item = progressList[0]
        assertEquals(250000.0, item.spentAmount, 0.001)
        assertEquals(50, item.percentage)
        assertFalse(item.isOver)
        assertEquals("SAFE", item.statusLevel)
    }

    @Test
    fun testAddAndRemoveBudget() = runBlocking {
        val budgetId = repository.addBudget(
            name = "Transportasi Harian",
            category = "Transportasi",
            limitAmount = 300000.0,
            period = "2026-10"
        )

        var list = repository.getBudgetProgress("2026-10").first()
        assertEquals(1, list.size)
        assertEquals(budgetId, list[0].budget.id)

        repository.deleteBudget(budgetId)
        list = repository.getBudgetProgress("2026-10").first()
        assertEquals(0, list.size)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────
    private class FakeBudgetDao : BudgetDao {
        private val budgets = mutableListOf<BudgetEntity>()
        private val _flow = MutableStateFlow<List<BudgetEntity>>(emptyList())
        private var idCounter = 1L

        override fun getAllBudgets(): Flow<List<BudgetEntity>> = _flow.asStateFlow()

        override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> =
            _flow.map { list -> list.filter { it.period == period } }

        override suspend fun getBudgetById(id: Long): BudgetEntity? =
            budgets.find { it.id == id }

        override suspend fun insertBudget(budget: BudgetEntity): Long {
            val newId = if (budget.id == 0L) idCounter++ else budget.id
            val entity = budget.copy(id = newId)
            budgets.add(entity)
            _flow.value = budgets.toList()
            return newId
        }

        override suspend fun updateBudget(budget: BudgetEntity) {
            val idx = budgets.indexOfFirst { it.id == budget.id }
            if (idx != -1) {
                budgets[idx] = budget
                _flow.value = budgets.toList()
            }
        }

        override suspend fun deleteBudget(id: Long) {
            budgets.removeAll { it.id == id }
            _flow.value = budgets.toList()
        }
    }

    private class FakeTransactionDao : TransactionDao {
        private val transactions = mutableListOf<TransactionEntity>()
        private val _flow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        private var idCounter = 1L

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _flow.asStateFlow()

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val newId = if (transaction.id == 0L) idCounter++ else transaction.id
            val entity = transaction.copy(id = newId)
            transactions.add(0, entity)
            _flow.value = transactions.toList()
            return newId
        }

        override suspend fun deleteTransaction(id: Long) {
            transactions.removeAll { it.id == id }
            _flow.value = transactions.toList()
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val idx = transactions.indexOfFirst { it.id == transaction.id }
            if (idx != -1) {
                transactions[idx] = transaction
                _flow.value = transactions.toList()
            }
        }

        override suspend fun getTransactionById(id: Long): TransactionEntity? =
            transactions.find { it.id == id }
    }

    private class FakeAccountDao : AccountDao {
        private val accounts = mutableListOf<AccountEntity>()
        private val _flow = MutableStateFlow<List<AccountEntity>>(emptyList())

        override fun getAllAccounts(): Flow<List<AccountEntity>> = _flow.asStateFlow()
        override suspend fun getAccountById(id: Long): AccountEntity? = accounts.find { it.id == id }
        override suspend fun insertAccount(account: AccountEntity): Long {
            accounts.add(account)
            _flow.value = accounts.toList()
            return account.id
        }
        override suspend fun updateBalance(id: Long, newBalance: Double) {
            val idx = accounts.indexOfFirst { it.id == id }
            if (idx != -1) {
                accounts[idx] = accounts[idx].copy(balance = newBalance)
                _flow.value = accounts.toList()
            }
        }
        override suspend fun adjustBalance(id: Long, delta: Double) {
            val idx = accounts.indexOfFirst { it.id == id }
            if (idx != -1) {
                accounts[idx] = accounts[idx].copy(balance = accounts[idx].balance + delta)
                _flow.value = accounts.toList()
            }
        }
        override suspend fun deleteAccount(id: Long) {
            accounts.removeAll { it.id == id }
            _flow.value = accounts.toList()
        }
    }

    private class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> =
            MutableStateFlow(emptyList())
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
            MutableStateFlow(emptyList())
        override suspend fun insertCategory(category: CategoryEntity): Long = 0L
        override suspend fun updateCategory(category: CategoryEntity): Int = 1
        override suspend fun deleteCategory(id: Long): Int = 1
        override suspend fun getCategoryById(id: Long): CategoryEntity? = null
    }
}
