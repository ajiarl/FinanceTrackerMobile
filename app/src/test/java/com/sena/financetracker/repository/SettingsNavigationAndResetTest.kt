package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.NotificationDao
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk logika pengaturan, rute navigasi Settings,
 * dan operasi reset riwayat transaksi pada repository.
 */
class SettingsNavigationAndResetTest {

    private lateinit var fakeTransactionDao: InMemoryTransactionDao
    private lateinit var repository: TransactionRepository

    private class InMemoryTransactionDao : TransactionDao {
        private val list = mutableListOf<TransactionEntity>()
        private var idCounter = 1L
        private val flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flow

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val id = if (transaction.id != 0L) transaction.id else idCounter++
            val saved = transaction.copy(id = id)
            list.add(saved)
            flow.value = list.toList()
            return id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val idx = list.indexOfFirst { it.id == transaction.id }
            if (idx >= 0) {
                list[idx] = transaction
                flow.value = list.toList()
            }
        }

        override suspend fun deleteTransaction(id: Long) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }

        override suspend fun getTransactionById(id: Long): TransactionEntity? = list.find { it.id == id }

        override suspend fun clearAllTransactions() {
            list.clear()
            flow.value = emptyList()
        }
    }

    private class StubAccountDao : AccountDao {
        override fun getAllAccounts(): Flow<List<AccountEntity>> = flowOf(emptyList())
        override suspend fun getAccountById(id: Long): AccountEntity? = null
        override suspend fun insertAccount(account: AccountEntity): Long = 1L
        override suspend fun updateBalance(id: Long, newBalance: Double) {}
        override suspend fun adjustBalance(id: Long, delta: Double) {}
        override suspend fun deleteAccount(id: Long) {}
    }

    private class StubCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun insertCategory(category: CategoryEntity): Long = 1L
        override suspend fun updateCategory(category: CategoryEntity): Int = 1
        override suspend fun deleteCategory(id: Long): Int = 1
        override suspend fun getCategoryById(id: Long): CategoryEntity? = null
    }

    private class StubBudgetDao : BudgetDao {
        override fun getAllBudgets(): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override suspend fun getBudgetById(id: Long): BudgetEntity? = null
        override suspend fun insertBudget(budget: BudgetEntity): Long = 1L
        override suspend fun updateBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(id: Long) {}
    }

    private class StubNotificationDao : NotificationDao {
        override fun getAllNotifications(): Flow<List<NotificationEntity>> = flowOf(emptyList())
        override fun getUnreadCount(): Flow<Int> = flowOf(0)
        override suspend fun insertNotification(notification: NotificationEntity): Long = 1L
        override suspend fun markAsRead(id: Long) {}
        override suspend fun markAllAsRead() {}
        override suspend fun clearAllNotifications() {}
        override suspend fun deleteNotification(id: Long) {}
    }

    @Before
    fun setUp() {
        fakeTransactionDao = InMemoryTransactionDao()
        repository = TransactionRepository(
            transactionDao = fakeTransactionDao,
            accountDao = StubAccountDao(),
            categoryDao = StubCategoryDao(),
            budgetDao = StubBudgetDao(),
            notificationDao = StubNotificationDao()
        )
    }

    @Test
    fun screenSettings_routeAndMetadata_correct() {
        assertEquals("settings", Screen.Settings.route)
        assertEquals("Pengaturan", Screen.Settings.title)
        assertNotNull(Screen.Settings.icon)
    }

    @Test
    fun resetTransactions_whenDatabaseHasTransactions_clearsAllTransactions() = runBlocking {
        // Arrange: Masukkan 3 transaksi
        repository.transactionDao.insertTransaction(
            TransactionEntity(title = "Kopi Neobrutal", amount = 25000.0, type = "EXPENSE", category = "Makanan", date = "2026-10-07")
        )
        repository.transactionDao.insertTransaction(
            TransactionEntity(title = "Gaji Freelance", amount = 3500000.0, type = "INCOME", category = "Gaji", date = "2026-10-07")
        )
        repository.transactionDao.insertTransaction(
            TransactionEntity(title = "Buku Desain", amount = 150000.0, type = "EXPENSE", category = "Pendidikan", date = "2026-10-07")
        )

        val beforeReset = repository.getAllTransactions().first()
        assertEquals(3, beforeReset.size)

        // Act: Eksekusi resetTransactions
        repository.resetTransactions()

        // Assert: Seluruh data transaksi kosong
        val afterReset = repository.getAllTransactions().first()
        assertTrue("Transaksi harus kosong setelah reset", afterReset.isEmpty())
    }

    @Test
    fun resetTransactions_whenEmptyDatabase_remainsEmpty() = runBlocking {
        val initialList = repository.getAllTransactions().first()
        assertTrue(initialList.isEmpty())

        repository.resetTransactions()

        val afterReset = repository.getAllTransactions().first()
        assertTrue(afterReset.isEmpty())
    }
}
