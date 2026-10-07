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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite untuk Manajemen Kategori Kustom dan Proteksi Kategori Sistem.
 *
 * Menguji:
 * 1. Insert kategori kustom baru dengan tipe dan warna hex valid.
 * 2. Update detail kategori kustom (nama, tipe, warna).
 * 3. Validasi penolakan nama kategori kosong.
 * 4. Validasi penolakan tipe kategori yang bukan EXPENSE atau INCOME.
 * 5. Proteksi penghapusan kategori sistem bawaan (harus melempar IllegalStateException).
 * 6. Penghapusan kategori kustom buatan user berhasil tanpa kendala.
 */
class CategoryManagementTest {

    private lateinit var repository: TransactionRepository
    private lateinit var fakeCategoryDao: InMemoryFakeCategoryDao

    private class InMemoryFakeCategoryDao : CategoryDao {
        private val list = mutableListOf<CategoryEntity>()
        private val _flow = MutableStateFlow<List<CategoryEntity>>(emptyList())
        private var nextId = 1L

        override fun getAllCategories(): Flow<List<CategoryEntity>> = _flow.asStateFlow()

        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
            flowOf(list.filter { it.type.equals(type, ignoreCase = true) })

        override suspend fun insertCategory(category: CategoryEntity): Long {
            val assigned = category.copy(id = nextId++)
            list.add(assigned)
            _flow.value = list.toList()
            return assigned.id
        }

        override suspend fun updateCategory(category: CategoryEntity): Int {
            val index = list.indexOfFirst { it.id == category.id }
            return if (index != -1) {
                list[index] = category
                _flow.value = list.toList()
                1
            } else {
                0
            }
        }

        override suspend fun deleteCategory(id: Long): Int {
            val removed = list.removeAll { it.id == id }
            if (removed) {
                _flow.value = list.toList()
                return 1
            }
            return 0
        }

        override suspend fun getCategoryById(id: Long): CategoryEntity? {
            return list.find { it.id == id }
        }
    }

    private class StubTransactionDao : TransactionDao {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun insertTransaction(transaction: TransactionEntity): Long = 1L
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(id: Long) {}
        override suspend fun getTransactionById(id: Long): TransactionEntity? = null
    }

    private class StubAccountDao : AccountDao {
        override fun getAllAccounts(): Flow<List<AccountEntity>> = flowOf(emptyList())
        override suspend fun getAccountById(id: Long): AccountEntity? = null
        override suspend fun insertAccount(account: AccountEntity): Long = 1L
        override suspend fun updateBalance(id: Long, newBalance: Double) {}
        override suspend fun adjustBalance(id: Long, delta: Double) {}
        override suspend fun deleteAccount(id: Long) {}
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
        fakeCategoryDao = InMemoryFakeCategoryDao()
        repository = TransactionRepository(
            transactionDao = StubTransactionDao(),
            accountDao = StubAccountDao(),
            categoryDao = fakeCategoryDao,
            budgetDao = StubBudgetDao()
        )
    }

    @Test
    fun insertCategory_validCustomCategory_success() = runBlocking {
        val newId = repository.insertCategory(
            name = "Langganan Netflix",
            type = "EXPENSE",
            color = "#EC4899"
        )

        assertTrue(newId > 0)
        val created = fakeCategoryDao.getCategoryById(newId)
        assertNotNull(created)
        assertEquals("Langganan Netflix", created?.name)
        assertEquals("EXPENSE", created?.type)
        assertEquals("#EC4899", created?.color)
    }

    @Test
    fun updateCategory_existingCustomCategory_success() = runBlocking {
        val catId = repository.insertCategory(
            name = "Side Hustle",
            type = "INCOME",
            color = "#00E676"
        )

        val rowsUpdated = repository.updateCategory(
            id = catId,
            name = "Side Hustle VIP",
            type = "INCOME",
            color = "#FAFF00"
        )

        assertEquals(1, rowsUpdated)
        val updated = fakeCategoryDao.getCategoryById(catId)
        assertEquals("Side Hustle VIP", updated?.name)
        assertEquals("#FAFF00", updated?.color)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertCategory_blankName_throwsException() = runBlocking {
        repository.insertCategory(name = "   ", type = "EXPENSE")
        fail("Harus melempar IllegalArgumentException ketika nama kategori kosong")
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertCategory_invalidType_throwsException() = runBlocking {
        repository.insertCategory(name = "Investasi Crypto", type = "UNRECOGNIZED_TYPE")
        fail("Harus melempar IllegalArgumentException ketika tipe bukan EXPENSE atau INCOME")
    }

    @Test
    fun deleteCategory_systemCategory_throwsIllegalStateException() = runBlocking {
        // Simulasikan kategori default sistem Makanan & Minuman
        val sysCatId = fakeCategoryDao.insertCategory(
            CategoryEntity(id = 0, name = "Makanan & Minuman", type = "EXPENSE", color = "#EF4444")
        )

        try {
            repository.deleteCategory(sysCatId)
            fail("Harus menolak penghapusan kategori bawaan sistem")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("tidak dapat dihapus") == true)
        }

        // Pastikan entitas masih ada di database
        val stillExists = fakeCategoryDao.getCategoryById(sysCatId)
        assertNotNull(stillExists)
    }

    @Test
    fun deleteCategory_userCustomCategory_deletesSuccessfully() = runBlocking {
        val customCatId = repository.insertCategory(
            name = "Uang Kopi Senja",
            type = "EXPENSE",
            color = "#A855F7"
        )

        val deletedCount = repository.deleteCategory(customCatId)
        assertEquals(1, deletedCount)

        val checkDeleted = fakeCategoryDao.getCategoryById(customCatId)
        assertNull(checkDeleted)
    }
}
