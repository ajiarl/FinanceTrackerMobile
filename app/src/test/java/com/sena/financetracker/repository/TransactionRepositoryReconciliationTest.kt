package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class TransactionRepositoryReconciliationTest {

    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeAccountDao: FakeAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var repository: TransactionRepository

    class FakeTransactionDao : TransactionDao {
        val storage = mutableMapOf<Long, TransactionEntity>()
        private var nextId = 1L
        private val flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val id = if (transaction.id == 0L) nextId++ else transaction.id
            val entity = transaction.copy(id = id)
            storage[id] = entity
            flow.value = storage.values.toList()
            return id
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flow.asStateFlow()

        override suspend fun deleteTransaction(id: Long) {
            storage.remove(id)
            flow.value = storage.values.toList()
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            storage[transaction.id] = transaction
            flow.value = storage.values.toList()
        }

        override suspend fun getTransactionById(id: Long): TransactionEntity? {
            return storage[id]
        }
    }

    class FakeAccountDao : AccountDao {
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

    class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun insertCategory(category: CategoryEntity): Long = 0L
        override suspend fun deleteCategory(id: Long) {}
    }

    @Before
    fun setup() {
        fakeTransactionDao = FakeTransactionDao()
        fakeAccountDao = FakeAccountDao()
        fakeCategoryDao = FakeCategoryDao()
        repository = TransactionRepository(fakeTransactionDao, fakeAccountDao, fakeCategoryDao)

        // Seed accounts
        fakeAccountDao.accounts[1L] = AccountEntity(id = 1L, name = "BCA", type = "bank", balance = 1000000.0)
        fakeAccountDao.accounts[2L] = AccountEntity(id = 2L, name = "GoPay", type = "e-wallet", balance = 500000.0)
    }

    @Test
    fun testUpdateTransactionAmountExpenseReconciliation() = runBlocking {
        // Initial transaction: Expense 100k on BCA
        val originalTx = TransactionEntity(
            id = 10L,
            title = "Makan Siang",
            amount = 100000.0,
            type = "EXPENSE",
            category = "Makanan",
            date = "2026-10-01",
            accountId = 1L,
            accountName = "BCA"
        )
        fakeTransactionDao.insertTransaction(originalTx)
        // BCA balance after 100k expense would be 900k
        fakeAccountDao.accounts[1L] = fakeAccountDao.accounts[1L]!!.copy(balance = 900000.0)

        // User updates expense to 150k
        val updatedTx = originalTx.copy(amount = 150000.0)
        repository.updateTransaction(originalTx, updatedTx)

        // Old 100k is reverted (+100k -> 1,000,000), new 150k applied (-150k -> 850,000)
        assertEquals(850000.0, fakeAccountDao.accounts[1L]!!.balance, 0.001)
        assertEquals(150000.0, fakeTransactionDao.storage[10L]!!.amount, 0.001)
    }

    @Test
    fun testUpdateTransactionTypeFromExpenseToIncome() = runBlocking {
        // Initial transaction: Expense 200k on BCA
        val originalTx = TransactionEntity(
            id = 11L,
            title = "Salah input",
            amount = 200000.0,
            type = "EXPENSE",
            category = "Lainnya",
            date = "2026-10-01",
            accountId = 1L,
            accountName = "BCA"
        )
        fakeTransactionDao.insertTransaction(originalTx)
        fakeAccountDao.accounts[1L] = fakeAccountDao.accounts[1L]!!.copy(balance = 800000.0)

        // User changes to Income 200k
        val updatedTx = originalTx.copy(type = "INCOME", category = "Freelance")
        repository.updateTransaction(originalTx, updatedTx)

        // Old expense reverted (+200k -> 1,000,000), new income applied (+200k -> 1,200,000)
        assertEquals(1200000.0, fakeAccountDao.accounts[1L]!!.balance, 0.001)
        assertEquals("INCOME", fakeTransactionDao.storage[11L]!!.type)
    }

    @Test
    fun testUpdateTransactionAccountSwitch() = runBlocking {
        // Initial transaction: Expense 50k on BCA (Acc 1)
        val originalTx = TransactionEntity(
            id = 12L,
            title = "Beli Kopi",
            amount = 50000.0,
            type = "EXPENSE",
            category = "Minuman",
            date = "2026-10-01",
            accountId = 1L,
            accountName = "BCA"
        )
        fakeTransactionDao.insertTransaction(originalTx)
        fakeAccountDao.accounts[1L] = fakeAccountDao.accounts[1L]!!.copy(balance = 950000.0)
        // GoPay (Acc 2) is 500000.0

        // User switches to GoPay (Acc 2)
        val updatedTx = originalTx.copy(accountId = 2L, accountName = "GoPay")
        repository.updateTransaction(originalTx, updatedTx)

        // BCA reverts old expense: 950,000 + 50,000 = 1,000,000
        assertEquals(1000000.0, fakeAccountDao.accounts[1L]!!.balance, 0.001)
        // GoPay applies new expense: 500,000 - 50,000 = 450,000
        assertEquals(450000.0, fakeAccountDao.accounts[2L]!!.balance, 0.001)
    }
}
