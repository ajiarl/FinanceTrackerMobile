package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AccountReconcileTest {

    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeAccountDao: FakeAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var repository: TransactionRepository

    class FakeAccountDao : AccountDao {
        private val accountsMap = mutableMapOf<Long, AccountEntity>()
        private var idCounter = 1L
        private val _flow = MutableStateFlow<List<AccountEntity>>(emptyList())

        override fun getAllAccounts(): Flow<List<AccountEntity>> = _flow

        override suspend fun getAccountById(id: Long): AccountEntity? = accountsMap[id]

        override suspend fun insertAccount(account: AccountEntity): Long {
            val id = if (account.id != 0L) account.id else idCounter++
            val saved = account.copy(id = id)
            accountsMap[id] = saved
            _flow.value = accountsMap.values.toList()
            return id
        }

        override suspend fun updateBalance(id: Long, newBalance: Double) {
            val acc = accountsMap[id] ?: return
            accountsMap[id] = acc.copy(balance = newBalance)
            _flow.value = accountsMap.values.toList()
        }

        override suspend fun adjustBalance(id: Long, delta: Double) {
            val acc = accountsMap[id] ?: return
            accountsMap[id] = acc.copy(balance = acc.balance + delta)
            _flow.value = accountsMap.values.toList()
        }

        override suspend fun deleteAccount(id: Long) {
            accountsMap.remove(id)
            _flow.value = accountsMap.values.toList()
        }
    }

    class FakeTransactionDao : TransactionDao {
        private val txMap = mutableMapOf<Long, TransactionEntity>()
        private var idCounter = 1L
        private val _flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _flow

        override suspend fun getTransactionById(id: Long): TransactionEntity? = txMap[id]

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val id = if (transaction.id != 0L) transaction.id else idCounter++
            val saved = transaction.copy(id = id)
            txMap[id] = saved
            _flow.value = txMap.values.toList()
            return id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            txMap[transaction.id] = transaction
            _flow.value = txMap.values.toList()
        }

        override suspend fun deleteTransaction(id: Long) {
            txMap.remove(id)
            _flow.value = txMap.values.toList()
        }
    }

    class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun insertCategory(category: CategoryEntity): Long = 1L
        override suspend fun deleteCategory(id: Long) {}
    }

    @Before
    fun setUp() {
        fakeTransactionDao = FakeTransactionDao()
        fakeAccountDao = FakeAccountDao()
        fakeCategoryDao = FakeCategoryDao()
        repository = TransactionRepository(fakeTransactionDao, fakeAccountDao, fakeCategoryDao)
    }

    @Test
    fun testAddAccountWithInitialBalance() = runBlocking {
        val accountId = repository.addAccount(
            name = "SeaBank",
            type = "bank",
            initialBalance = 250000.0
        )

        // Verifikasi akun tersimpan dengan saldo 250k
        val account = fakeAccountDao.getAccountById(accountId)
        assertNotNull(account)
        assertEquals("SeaBank", account!!.name)
        assertEquals("bank", account.type)
        assertEquals(250000.0, account.balance, 0.001)

        // Verifikasi transaksi Saldo Awal tercatat
        val transactions = repository.getAllTransactions().first()
        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals("Saldo Awal", tx.title)
        assertEquals(250000.0, tx.amount, 0.001)
        assertEquals("INCOME", tx.type)
        assertEquals("Saldo Awal", tx.category)
        assertEquals(accountId, tx.accountId)
    }

    @Test
    fun testAddAccountWithZeroInitialBalance() = runBlocking {
        val accountId = repository.addAccount(
            name = "Dompet Receh",
            type = "cash",
            initialBalance = 0.0
        )

        val account = fakeAccountDao.getAccountById(accountId)
        assertNotNull(account)
        assertEquals(0.0, account!!.balance, 0.001)

        // Tidak ada transaksi tercatat jika saldo awal 0
        val transactions = repository.getAllTransactions().first()
        assertEquals(0, transactions.size)
    }

    @Test
    fun testReconcileAccountSurplus() = runBlocking {
        val accountId = fakeAccountDao.insertAccount(
            AccountEntity(id = 10, name = "BCA", type = "bank", balance = 250000.0)
        )
        val account = fakeAccountDao.getAccountById(accountId)!!

        // Saldo sistem 250k, saldo fisik riil 300k (Surplus +50k)
        repository.reconcileAccount(
            account = account,
            actualBalance = 300000.0,
            date = "2026-10-07"
        )

        // Saldo akun terupdate menjadi 300k
        val updatedAccount = fakeAccountDao.getAccountById(accountId)!!
        assertEquals(300000.0, updatedAccount.balance, 0.001)

        // Transaksi penyesuaian tercatat sebagai INCOME sebesar 50k
        val transactions = repository.getAllTransactions().first()
        assertEquals(1, transactions.size)
        val adjustmentTx = transactions[0]
        assertEquals("Penyesuaian Saldo Sistem", adjustmentTx.title)
        assertEquals(50000.0, adjustmentTx.amount, 0.001)
        assertEquals("INCOME", adjustmentTx.type)
        assertEquals("Penyesuaian", adjustmentTx.category)
        assertEquals(accountId, adjustmentTx.accountId)
    }

    @Test
    fun testReconcileAccountDeficit() = runBlocking {
        val accountId = fakeAccountDao.insertAccount(
            AccountEntity(id = 20, name = "GoPay", type = "e-wallet", balance = 300000.0)
        )
        val account = fakeAccountDao.getAccountById(accountId)!!

        // Saldo sistem 300k, saldo fisik riil 200k (Defisit -100k)
        repository.reconcileAccount(
            account = account,
            actualBalance = 200000.0,
            date = "2026-10-07"
        )

        // Saldo akun terupdate menjadi 200k
        val updatedAccount = fakeAccountDao.getAccountById(accountId)!!
        assertEquals(200000.0, updatedAccount.balance, 0.001)

        // Transaksi penyesuaian tercatat sebagai EXPENSE sebesar 100k
        val transactions = repository.getAllTransactions().first()
        assertEquals(1, transactions.size)
        val adjustmentTx = transactions[0]
        assertEquals("Penyesuaian Saldo Sistem", adjustmentTx.title)
        assertEquals(100000.0, adjustmentTx.amount, 0.001)
        assertEquals("EXPENSE", adjustmentTx.type)
        assertEquals("Penyesuaian", adjustmentTx.category)
        assertEquals(accountId, adjustmentTx.accountId)
    }

    @Test
    fun testReconcileAccountZeroDiff() = runBlocking {
        val accountId = fakeAccountDao.insertAccount(
            AccountEntity(id = 30, name = "Dompet Tunai", type = "cash", balance = 150000.0)
        )
        val account = fakeAccountDao.getAccountById(accountId)!!

        // Saldo sistem 150k, saldo fisik riil 150k (Selisih 0)
        repository.reconcileAccount(
            account = account,
            actualBalance = 150000.0,
            date = "2026-10-07"
        )

        val updatedAccount = fakeAccountDao.getAccountById(accountId)!!
        assertEquals(150000.0, updatedAccount.balance, 0.001)

        // Tidak ada transaksi baru yang dibuat
        val transactions = repository.getAllTransactions().first()
        assertEquals(0, transactions.size)
    }
}
