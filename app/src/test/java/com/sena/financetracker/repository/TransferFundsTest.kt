package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransferFundsTest {

    private lateinit var fakeAccountDao: FakeAccountDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var repository: TransactionRepository

    private val accountBca = AccountEntity(id = 1L, name = "BCA", type = "bank", balance = 1000000.0)
    private val accountGopay = AccountEntity(id = 2L, name = "GoPay", type = "e-wallet", balance = 250000.0)

    @Before
    fun setUp() {
        fakeAccountDao = FakeAccountDao(listOf(accountBca, accountGopay))
        fakeTransactionDao = FakeTransactionDao()
        fakeCategoryDao = FakeCategoryDao()
        repository = TransactionRepository(fakeTransactionDao, fakeAccountDao, fakeCategoryDao)
    }

    @Test
    fun `transferFunds atomic updates reduce fromAccount and increase toAccount balance`() = runBlocking {
        val transferAmount = 200000.0
        repository.transferFunds(
            fromAccount = accountBca,
            toAccount = accountGopay,
            amount = transferAmount,
            notes = "Transfer uang jajan",
            date = "2026-10-07"
        )

        // BCA should decrease by 200k (1M -> 800k)
        val updatedBca = fakeAccountDao.getAccountById(1L)
        assertNotNull(updatedBca)
        assertEquals(800000.0, updatedBca!!.balance, 0.001)

        // GoPay should increase by 200k (250k -> 450k)
        val updatedGopay = fakeAccountDao.getAccountById(2L)
        assertNotNull(updatedGopay)
        assertEquals(450000.0, updatedGopay!!.balance, 0.001)

        // Recorded transaction check
        assertEquals(1, fakeTransactionDao.transactions.size)
        val recordedTx = fakeTransactionDao.transactions.first()
        assertEquals("Transfer ke GoPay", recordedTx.title)
        assertEquals(200000.0, recordedTx.amount, 0.001)
        assertEquals("TRANSFER", recordedTx.type)
        assertEquals("Transfer", recordedTx.category)
        assertEquals(1L, recordedTx.accountId)
        assertEquals("BCA", recordedTx.accountName)
        assertEquals(2L, recordedTx.toAccountId)
        assertEquals("GoPay", recordedTx.toAccountName)
        assertEquals("Transfer uang jajan", recordedTx.notes)
    }

    @Test
    fun `transferFunds throws exception when fromAccount and toAccount are identical`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = accountBca,
                    toAccount = accountBca,
                    amount = 50000.0,
                    notes = "Self transfer invalid",
                    date = "2026-10-07"
                )
            }
        }
    }

    @Test
    fun `transferFunds throws exception when amount is zero or negative`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = accountBca,
                    toAccount = accountGopay,
                    amount = 0.0,
                    notes = "Zero amount",
                    date = "2026-10-07"
                )
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = accountBca,
                    toAccount = accountGopay,
                    amount = -50000.0,
                    notes = "Negative amount",
                    date = "2026-10-07"
                )
            }
        }
    }

    @Test
    fun `deleteTransaction with TRANSFER type reverts both accounts`() = runBlocking {
        repository.transferFunds(
            fromAccount = accountBca,
            toAccount = accountGopay,
            amount = 100000.0,
            notes = "Test revert",
            date = "2026-10-07"
        )

        val tx = fakeTransactionDao.transactions.first()
        assertEquals(900000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(350000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)

        // Delete the transfer transaction
        repository.deleteTransaction(tx)

        // Balances should be reverted
        assertEquals(1000000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(250000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)
        assertTrue(fakeTransactionDao.transactions.isEmpty())
    }

    // Helper fake DAOs
    private class FakeAccountDao(initialAccounts: List<AccountEntity>) : AccountDao {
        private val accountsMap = initialAccounts.associateBy { it.id }.toMutableMap()
        private val _flow = MutableStateFlow(accountsMap.values.toList())

        override fun getAllAccounts(): Flow<List<AccountEntity>> = _flow

        override suspend fun insertAccount(account: AccountEntity): Long {
            val id = (accountsMap.keys.maxOrNull() ?: 0L) + 1L
            accountsMap[id] = account.copy(id = id)
            _flow.value = accountsMap.values.toList()
            return id
        }

        override suspend fun adjustBalance(accountId: Long, delta: Double) {
            val acc = accountsMap[accountId] ?: return
            accountsMap[accountId] = acc.copy(balance = acc.balance + delta)
            _flow.value = accountsMap.values.toList()
        }

        override suspend fun updateBalance(id: Long, newBalance: Double) {
            val acc = accountsMap[id] ?: return
            accountsMap[id] = acc.copy(balance = newBalance)
            _flow.value = accountsMap.values.toList()
        }

        override suspend fun deleteAccount(id: Long) {
            accountsMap.remove(id)
            _flow.value = accountsMap.values.toList()
        }

        override suspend fun getAccountById(id: Long): AccountEntity? = accountsMap[id]
    }

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L
        private val _flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val id = nextId++
            val saved = transaction.copy(id = id)
            transactions.add(saved)
            _flow.value = transactions.toList()
            return id
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _flow

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

        override suspend fun getTransactionById(id: Long): TransactionEntity? {
            return transactions.find { it.id == id }
        }
    }

    private class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun insertCategory(category: CategoryEntity): Long = 1L
        override suspend fun deleteCategory(id: Long) {}
    }
}
