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

/**
 * Unit Test Suite untuk Logika Bisnis Pusat Notifikasi & Sistem Peringatan Overbudget.
 *
 * Menguji:
 * 1. Insert notifikasi manual dan observasi reaktif StateFlow/Flow.
 * 2. Perhitungan unread count yang akurat secara dinamis.
 * 3. Menandai satu notifikasi sebagai sudah dibaca (markAsRead).
 * 4. Menandai semua notifikasi sebagai sudah dibaca (markAllAsRead).
 * 5. Menghapus satu notifikasi dan membersihkan semua notifikasi (clearAllNotifications).
 * 6. Overbudget Alerting Engine:
 *    - Transaksi pengeluaran >= 80% anggaran memicu notifikasi otomatis berjenis "WARNING".
 *    - Transaksi pengeluaran >= 100% anggaran memicu notifikasi otomatis berjenis "DANGER".
 *    - Transaksi pengeluaran di bawah 80% tidak memicu peringatan notifikasi.
 *    - Transaksi pendapatan (INCOME) tidak memicu peringatan anggaran pengeluaran.
 */
class NotificationBusinessLogicTest {

    private lateinit var repository: TransactionRepository
    private lateinit var fakeTransactionDao: InMemoryFakeTransactionDao
    private lateinit var fakeBudgetDao: InMemoryFakeBudgetDao
    private lateinit var fakeNotificationDao: InMemoryFakeNotificationDao
    private lateinit var fakeAccountDao: InMemoryFakeAccountDao
    private lateinit var fakeCategoryDao: InMemoryFakeCategoryDao

    // ── FAKE DAOS ─────────────────────────────────────────────────────────────

    private class InMemoryFakeNotificationDao : NotificationDao {
        val list = mutableListOf<NotificationEntity>()
        val flow = MutableStateFlow<List<NotificationEntity>>(emptyList())
        private var nextId = 1L

        override fun getAllNotifications(): Flow<List<NotificationEntity>> = flow.asStateFlow()

        override fun getUnreadCount(): Flow<Int> = flow.map { items -> items.count { !it.isRead } }

        override suspend fun insertNotification(notification: NotificationEntity): Long {
            val assigned = notification.copy(id = nextId++)
            list.add(0, assigned) // prepend LIFO
            flow.value = list.toList()
            return assigned.id
        }

        override suspend fun markAsRead(id: Long) {
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                list[index] = list[index].copy(isRead = true)
                flow.value = list.toList()
            }
        }

        override suspend fun markAllAsRead() {
            for (i in list.indices) {
                list[i] = list[i].copy(isRead = true)
            }
            flow.value = list.toList()
        }

        override suspend fun clearAllNotifications() {
            list.clear()
            flow.value = emptyList()
        }

        override suspend fun deleteNotification(id: Long) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }
    }

    private class InMemoryFakeBudgetDao : BudgetDao {
        val list = mutableListOf<BudgetEntity>()
        val flow = MutableStateFlow<List<BudgetEntity>>(emptyList())
        private var nextId = 1L

        override fun getAllBudgets(): Flow<List<BudgetEntity>> = flow.asStateFlow()

        override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> =
            flow.map { items -> items.filter { it.period == period } }

        override suspend fun getBudgetById(id: Long): BudgetEntity? = list.find { it.id == id }

        override suspend fun insertBudget(budget: BudgetEntity): Long {
            val assigned = budget.copy(id = nextId++)
            list.add(assigned)
            flow.value = list.toList()
            return assigned.id
        }

        override suspend fun updateBudget(budget: BudgetEntity) {
            val index = list.indexOfFirst { it.id == budget.id }
            if (index != -1) {
                list[index] = budget
                flow.value = list.toList()
            }
        }

        override suspend fun deleteBudget(id: Long) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }
    }

    private class InMemoryFakeTransactionDao : TransactionDao {
        val list = mutableListOf<TransactionEntity>()
        val flow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        private var nextId = 1L

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flow.asStateFlow()

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val assigned = transaction.copy(id = nextId++)
            list.add(assigned)
            flow.value = list.toList()
            return assigned.id
        }

        override suspend fun deleteTransaction(id: Long) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val index = list.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                list[index] = transaction
                flow.value = list.toList()
            }
        }

        override suspend fun getTransactionById(id: Long): TransactionEntity? = list.find { it.id == id }
    }

    private class InMemoryFakeAccountDao : AccountDao {
        val accounts = mutableMapOf<Long, AccountEntity>()
        private val flow = MutableStateFlow<List<AccountEntity>>(emptyList())

        override fun getAllAccounts(): Flow<List<AccountEntity>> = flow.asStateFlow()
        override suspend fun getAccountById(id: Long): AccountEntity? = accounts[id]
        override suspend fun insertAccount(account: AccountEntity): Long {
            accounts[account.id] = account
            flow.value = accounts.values.toList()
            return account.id
        }
        override suspend fun updateBalance(id: Long, newBalance: Double) {
            accounts[id]?.let {
                accounts[id] = it.copy(balance = newBalance)
                flow.value = accounts.values.toList()
            }
        }
        override suspend fun adjustBalance(id: Long, delta: Double) {
            accounts[id]?.let {
                accounts[id] = it.copy(balance = it.balance + delta)
                flow.value = accounts.values.toList()
            }
        }
        override suspend fun deleteAccount(id: Long) {
            accounts.remove(id)
            flow.value = accounts.values.toList()
        }
    }

    private class InMemoryFakeCategoryDao : CategoryDao {
        private val list = mutableListOf<CategoryEntity>()
        private val flow = MutableStateFlow<List<CategoryEntity>>(emptyList())
        override fun getAllCategories(): Flow<List<CategoryEntity>> = flow.asStateFlow()
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flow.asStateFlow()
        override suspend fun insertCategory(category: CategoryEntity): Long = 1L
        override suspend fun updateCategory(category: CategoryEntity): Int = 1
        override suspend fun deleteCategory(id: Long): Int = 1
        override suspend fun getCategoryById(id: Long): CategoryEntity? = null
    }

    @Before
    fun setUp() {
        fakeTransactionDao = InMemoryFakeTransactionDao()
        fakeBudgetDao = InMemoryFakeBudgetDao()
        fakeNotificationDao = InMemoryFakeNotificationDao()
        fakeAccountDao = InMemoryFakeAccountDao()
        fakeCategoryDao = InMemoryFakeCategoryDao()

        repository = TransactionRepository(
            transactionDao = fakeTransactionDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao,
            budgetDao = fakeBudgetDao,
            notificationDao = fakeNotificationDao
        )

        runBlocking {
            fakeAccountDao.insertAccount(
                AccountEntity(
                    id = 1L,
                    name = "BCA Utama",
                    type = "BANK",
                    balance = 10_000_000.0
                )
            )
        }
    }

    @Test
    fun testManualNotificationFlowAndUnreadCount() = runBlocking {
        // Awalnya kosong
        var notifs = repository.getAllNotifications().first()
        var unreadCount = repository.getUnreadNotificationCount().first()
        assertTrue(notifs.isEmpty())
        assertEquals(0, unreadCount)

        // Insert notifikasi 1
        repository.insertNotification(
            NotificationEntity(
                title = "Info Sistem",
                message = "Selamat datang di aplikasi Finance Tracker!",
                type = "INFO"
            )
        )

        // Insert notifikasi 2
        val notif2Id = repository.insertNotification(
            NotificationEntity(
                title = "Pengingat",
                message = "Jangan lupa catat pengeluaran harian",
                type = "WARNING"
            )
        )

        notifs = repository.getAllNotifications().first()
        unreadCount = repository.getUnreadNotificationCount().first()
        assertEquals(2, notifs.size)
        assertEquals(2, unreadCount)

        // Tandai notifikasi 2 dibaca
        repository.markNotificationAsRead(notif2Id)
        unreadCount = repository.getUnreadNotificationCount().first()
        assertEquals(1, unreadCount)

        // Tandai semua dibaca
        repository.markAllNotificationsAsRead()
        unreadCount = repository.getUnreadNotificationCount().first()
        assertEquals(0, unreadCount)

        // Hapus notifikasi 2
        repository.deleteNotification(notif2Id)
        notifs = repository.getAllNotifications().first()
        assertEquals(1, notifs.size)

        // Hapus semua
        repository.clearAllNotifications()
        notifs = repository.getAllNotifications().first()
        assertEquals(0, notifs.size)
    }

    @Test
    fun testOverbudgetWarningTriggerAt80Percent() = runBlocking {
        // Set anggaran bulanan untuk "Makanan & Minuman" sebesar Rp 1.000.000 periode 2026-10
        repository.addBudget(
            name = "Makan Sehat",
            category = "Makanan & Minuman",
            limitAmount = 1_000_000.0,
            period = "2026-10"
        )

        // Transaksi 1: Rp 500.000 (50%) -> Tidak memicu notifikasi
        repository.insertTransaction(
            TransactionEntity(
                title = "Makan Siang Resto",
                amount = 500_000.0,
                type = "EXPENSE",
                category = "Makanan & Minuman",
                date = "2026-10-05",
                accountId = 1L,
                accountName = "BCA Utama"
            )
        )

        var notifs = repository.getAllNotifications().first()
        assertTrue(notifs.isEmpty())

        // Transaksi 2: Rp 350.000 (Total pengeluaran jadi Rp 850.000 = 85%) -> Harus memicu "WARNING"
        repository.insertTransaction(
            TransactionEntity(
                title = "Belanja Groceries",
                amount = 350_000.0,
                type = "EXPENSE",
                category = "Makanan & Minuman",
                date = "2026-10-06",
                accountId = 1L,
                accountName = "BCA Utama"
            )
        )

        notifs = repository.getAllNotifications().first()
        assertEquals(1, notifs.size)
        assertEquals("WARNING", notifs[0].type)
        assertTrue(notifs[0].title.contains("80%+"))
        assertFalse(notifs[0].isRead)
    }

    @Test
    fun testOverbudgetDangerTriggerAt100Percent() = runBlocking {
        // Set anggaran bulanan untuk "Hiburan" sebesar Rp 500.000 periode 2026-10
        repository.addBudget(
            name = "Langganan & Game",
            category = "Hiburan",
            limitAmount = 500_000.0,
            period = "2026-10"
        )

        // Transaksi: Rp 550.000 (110%) -> Langsung melampaui 100% batas anggaran -> Harus memicu "DANGER"
        repository.insertTransaction(
            TransactionEntity(
                title = "Beli Game Steam & Top Up",
                amount = 550_000.0,
                type = "EXPENSE",
                category = "Hiburan",
                date = "2026-10-07",
                accountId = 1L,
                accountName = "BCA Utama"
            )
        )

        val notifs = repository.getAllNotifications().first()
        assertEquals(1, notifs.size)
        assertEquals("DANGER", notifs[0].type)
        assertTrue(notifs[0].title.contains("100%+"))
        assertTrue(notifs[0].message.contains("110%"))
        assertFalse(notifs[0].isRead)
    }

    @Test
    fun testIncomeTransactionDoesNotTriggerBudgetAlert() = runBlocking {
        // Set anggaran "Gaji" atau kategori lain
        repository.addBudget(
            name = "Target Gaji",
            category = "Gaji",
            limitAmount = 10_000_000.0,
            period = "2026-10"
        )

        // Transaksi pendapatan Rp 12.000.000 (INCOME)
        repository.insertTransaction(
            TransactionEntity(
                title = "Gaji Bulanan",
                amount = 12_000_000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-10-01",
                accountId = 1L,
                accountName = "BCA Utama"
            )
        )

        val notifs = repository.getAllNotifications().first()
        assertTrue(notifs.isEmpty())
    }
}
