package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.repository.TransactionRepository
import com.sena.financetracker.util.CsvImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite untuk memverifikasi eliminasi N+1 Database Query pada operasi CSV Import (PERF-01).
 *
 * Memvalidasi:
 * 1. Pre-fetching accounts & categories sekali ke dalam Map in-memory dan pencocokan O(1).
 * 2. Akun dan kategori baru yang dibuat secara on-the-fly hanya memicu 1x insert per nama unik,
 *    dan dicache untuk baris-baris berikutnya tanpa memicu query database berulang kali.
 * 3. Pemanggilan [FinanceViewModel.importFromCsv] baik via list parsed maupun raw CSV string.
 */
class CsvImportPerformanceTest {

    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeAccountDao: CountingFakeAccountDao
    private lateinit var fakeCategoryDao: CountingFakeCategoryDao
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: FinanceViewModel

    private class FakeTransactionDao : TransactionDao {
        val inserted = mutableListOf<TransactionEntity>()
        private val _flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _flow.asStateFlow()
        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            inserted.add(transaction)
            return inserted.size.toLong()
        }
        override suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> {
            val startId = inserted.size + 1L
            val ids = mutableListOf<Long>()
            transactions.forEachIndexed { idx, tx ->
                val assigned = tx.copy(id = startId + idx)
                inserted.add(assigned)
                ids.add(assigned.id)
            }
            _flow.value = inserted.toList()
            return ids
        }
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(id: Long) {}
        override suspend fun getTransactionById(id: Long): TransactionEntity? = inserted.find { it.id == id }
    }

    private class CountingFakeAccountDao : AccountDao {
        val accounts = mutableListOf<AccountEntity>()
        private val _flow = MutableStateFlow<List<AccountEntity>>(emptyList())
        var insertAccountCallCount = 0

        fun seed(list: List<AccountEntity>) {
            accounts.clear()
            accounts.addAll(list)
            _flow.value = accounts.toList()
        }

        override fun getAllAccounts(): Flow<List<AccountEntity>> = _flow.asStateFlow()
        override suspend fun getAccountById(id: Long): AccountEntity? = accounts.find { it.id == id }
        override suspend fun insertAccount(account: AccountEntity): Long {
            insertAccountCallCount++
            val assigned = account.copy(id = accounts.size + 1L)
            accounts.add(assigned)
            _flow.value = accounts.toList()
            return assigned.id
        }
        override suspend fun updateBalance(id: Long, newBalance: Double) {}
        override suspend fun adjustBalance(id: Long, delta: Double) {}
        override suspend fun deleteAccount(id: Long) {}
    }

    private class CountingFakeCategoryDao : CategoryDao {
        val categories = mutableListOf<CategoryEntity>()
        private val _flow = MutableStateFlow<List<CategoryEntity>>(emptyList())
        var insertCategoryCallCount = 0

        fun seed(list: List<CategoryEntity>) {
            categories.clear()
            categories.addAll(list)
            _flow.value = categories.toList()
        }

        override fun getAllCategories(): Flow<List<CategoryEntity>> = _flow.asStateFlow()
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun insertCategory(category: CategoryEntity): Long {
            insertCategoryCallCount++
            val assigned = category.copy(id = categories.size + 1L)
            categories.add(assigned)
            _flow.value = categories.toList()
            return assigned.id
        }
        override suspend fun updateCategory(category: CategoryEntity): Int = 1
        override suspend fun deleteCategory(id: Long): Int = 1
        override suspend fun getCategoryById(id: Long): CategoryEntity? = categories.find { it.id == id }
    }

    private class StubBudgetDao : BudgetDao {
        override fun getAllBudgets(): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override suspend fun getBudgetById(id: Long): BudgetEntity? = null
        override suspend fun insertBudget(budget: BudgetEntity): Long = 1L
        override suspend fun updateBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(id: Long) {}
    }

    @Before
    fun setup() {
        fakeTransactionDao = FakeTransactionDao()
        fakeAccountDao = CountingFakeAccountDao()
        fakeCategoryDao = CountingFakeCategoryDao()

        // Seed initial accounts & categories
        fakeAccountDao.seed(
            listOf(
                AccountEntity(id = 1L, name = "Dompet Tunai", type = "CASH", balance = 500000.0),
                AccountEntity(id = 2L, name = "BCA", type = "BANK", balance = 2000000.0)
            )
        )
        fakeCategoryDao.seed(
            listOf(
                CategoryEntity(id = 1L, name = "Makanan", type = "EXPENSE", color = "#F97316"),
                CategoryEntity(id = 2L, name = "Gaji", type = "INCOME", color = "#22C55E")
            )
        )

        repository = TransactionRepository(
            transactionDao = fakeTransactionDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao,
            budgetDao = StubBudgetDao()
        )

        // Gunakan CoroutineScope dengan Dispatchers.Unconfined untuk testing deterministik
        viewModel = FinanceViewModel(
            repository = repository,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun importFromCsv_resolvesExistingAccountsAndCategoriesInMemoryWithoutExtraInserts() = runBlocking {
        val rows = (1..50).map { i ->
            CsvImporter.ParsedTransaction(
                title = "Transaksi $i",
                amount = 25000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-10-01",
                rawAccountName = "BCA",
                isValid = true
            )
        }

        var successCount = 0
        viewModel.importFromCsv(
            parsedTransactions = rows,
            onSuccess = { successCount = it }
        )

        assertEquals(50, successCount)
        assertEquals(50, fakeTransactionDao.inserted.size)
        // Tidak boleh ada penambahan akun atau kategori baru karena sudah ada di in-memory map
        assertEquals(0, fakeAccountDao.insertAccountCallCount)
        assertEquals(0, fakeCategoryDao.insertCategoryCallCount)

        // Verifikasi seluruh transaksi memiliki accountId dan kategori yang tepat
        fakeTransactionDao.inserted.forEach { tx ->
            assertEquals(2L, tx.accountId)
            assertEquals("BCA", tx.accountName)
            assertEquals("Makanan", tx.category)
        }
    }

    @Test
    fun importFromCsv_createsNewAccountAndCategoryOnlyOnceAndCachesForSubsequentRows() = runBlocking {
        // 10 baris dengan nama akun baru "Jago" dan kategori baru "Hiburan"
        val rows = (1..10).map { i ->
            CsvImporter.ParsedTransaction(
                title = "Nonton Bioskop $i",
                amount = 50000.0,
                type = "EXPENSE",
                category = "Hiburan",
                date = "2026-10-02",
                rawAccountName = "Jago",
                isValid = true
            )
        }

        var successCount = 0
        viewModel.importFromCsv(
            parsedTransactions = rows,
            onSuccess = { successCount = it }
        )

        assertEquals(10, successCount)
        // Akun "Jago" dan kategori "Hiburan" hanya boleh di-insert tepat 1x ke DAO, bukan 10x (N+1 eliminated!)
        assertEquals(1, fakeAccountDao.insertAccountCallCount)
        assertEquals(1, fakeCategoryDao.insertCategoryCallCount)

        // Seluruh 10 transaksi terhubung ke ID akun baru yang sama
        val newAccountId = fakeAccountDao.accounts.find { it.name == "Jago" }?.id
        assertTrue(newAccountId != null)
        fakeTransactionDao.inserted.forEach { tx ->
            assertEquals(newAccountId, tx.accountId)
            assertEquals("Jago", tx.accountName)
            assertEquals("Hiburan", tx.category)
        }
    }

    @Test
    fun importFromCsv_rawCsvString_parsesAndBatchInsertsSuccessfully() = runBlocking {
        val rawCsv = """
            Tanggal,Tipe,Judul,Nominal,Kategori,Akun,Catatan
            2026-10-05,EXPENSE,Beli Bensin,30000,Transportasi,Dompet Tunai,Pertalite
            2026-10-06,INCOME,Bonus Projek,500000,Gaji,BCA,Freelance
        """.trimIndent()

        var successCount = 0
        viewModel.importFromCsv(
            csvContent = rawCsv,
            onSuccess = { successCount = it }
        )

        assertEquals(2, successCount)
        assertEquals(2, fakeTransactionDao.inserted.size)
        // Transportasi kategori baru diinsert 1x
        assertEquals(1, fakeCategoryDao.insertCategoryCallCount)
        // Akun Dompet Tunai dan BCA sudah ada, tidak boleh ada akun baru
        assertEquals(0, fakeAccountDao.insertAccountCallCount)
    }
}
