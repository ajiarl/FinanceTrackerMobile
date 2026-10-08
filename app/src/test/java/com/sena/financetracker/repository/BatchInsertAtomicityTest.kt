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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk memverifikasi atomisitas operasi batch insert transaksi (ARC-01).
 * Memastikan eksekusi batch insert dan update saldo berjalan dalam transaksi atomic SQLite,
 * serta memastikan rollback otomatis jika terjadi kegagalan/exception di tengah proses.
 */
class BatchInsertAtomicityTest {

    private lateinit var fakeTransactionDao: TransactionalFakeTransactionDao
    private lateinit var fakeAccountDao: TransactionalFakeAccountDao
    private lateinit var fakeCategoryDao: SimpleFakeCategoryDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        fakeTransactionDao = TransactionalFakeTransactionDao()
        fakeAccountDao = TransactionalFakeAccountDao()
        fakeCategoryDao = SimpleFakeCategoryDao()

        fakeTransactionDao.bindAccountDao(fakeAccountDao)

        repository = TransactionRepository(
            transactionDao = fakeTransactionDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao
        )
    }

    @Test
    fun testBatchInsertSuccessAtomically() = runBlocking {
        // Inisialisasi 2 rekening: Akun 1 = 500.000, Akun 2 = 200.000
        val acc1Id = fakeAccountDao.insertAccount(AccountEntity(id = 1, name = "BCA", type = "bank", balance = 500000.0))
        val acc2Id = fakeAccountDao.insertAccount(AccountEntity(id = 2, name = "Mandiri", type = "bank", balance = 200000.0))

        val transactions = listOf(
            TransactionEntity(
                id = 0,
                title = "Gaji Bonus",
                amount = 100000.0,
                type = "INCOME",
                category = "Bonus",
                date = "2026-10-08",
                accountId = acc1Id,
                accountName = "BCA"
            ),
            TransactionEntity(
                id = 0,
                title = "Makan Siang",
                amount = 50000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-10-08",
                accountId = acc1Id,
                accountName = "BCA"
            ),
            TransactionEntity(
                id = 0,
                title = "Transfer ke Mandiri",
                amount = 70000.0,
                type = "TRANSFER",
                category = "Transfer",
                date = "2026-10-08",
                accountId = acc1Id,
                accountName = "BCA",
                toAccountId = acc2Id,
                toAccountName = "Mandiri"
            )
        )

        val insertedIds = repository.insertTransactionsBatch(transactions)

        assertEquals(3, insertedIds.size)

        // Verifikasi saldo akhir kedua akun
        // Akun 1: 500.000 + 100.000 (INCOME) - 50.000 (EXPENSE) - 70.000 (TRANSFER OUT) = 480.000
        // Akun 2: 200.000 + 70.000 (TRANSFER IN) = 270.000
        val acc1 = fakeAccountDao.getAccountById(acc1Id)
        val acc2 = fakeAccountDao.getAccountById(acc2Id)

        assertEquals(480000.0, acc1?.balance ?: 0.0, 0.001)
        assertEquals(270000.0, acc2?.balance ?: 0.0, 0.001)
        assertEquals(3, fakeTransactionDao.currentTransactions.size)
    }

    @Test
    fun testBatchInsertRollbackOnExceptionPreservesDataIntegrity() = runBlocking {
        // Inisialisasi akun awal
        val acc1Id = fakeAccountDao.insertAccount(AccountEntity(id = 1, name = "BCA", type = "bank", balance = 500000.0))
        val acc2Id = fakeAccountDao.insertAccount(AccountEntity(id = 2, name = "Mandiri", type = "bank", balance = 200000.0))

        // Konfigurasi agar terjadi exception saat update saldo akun 2
        fakeAccountDao.shouldFailOnAccount = acc2Id

        val transactions = listOf(
            TransactionEntity(
                id = 0,
                title = "Gaji Part-time",
                amount = 150000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-10-08",
                accountId = acc1Id,
                accountName = "BCA"
            ),
            TransactionEntity(
                id = 0,
                title = "Transfer Kas",
                amount = 80000.0,
                type = "TRANSFER",
                category = "Transfer",
                date = "2026-10-08",
                accountId = acc1Id,
                accountName = "BCA",
                toAccountId = acc2Id,
                toAccountName = "Mandiri"
            )
        )

        try {
            repository.insertTransactionsBatch(transactions)
            fail("Seharusnya melempar RuntimeException ketika penyesuaian saldo gagal")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("Simulated database failure") == true)
        }

        // Pastikan seluruh transaksi dibatalkan (rollback 0 data tersimpan)
        assertEquals(0, fakeTransactionDao.currentTransactions.size)

        // Pastikan saldo akun 1 dan 2 kembali utuh ke kondisi sebelum transaksi (tidak terjadi saldo korup)
        val acc1 = fakeAccountDao.getAccountById(acc1Id)
        val acc2 = fakeAccountDao.getAccountById(acc2Id)

        assertEquals(500000.0, acc1?.balance ?: 0.0, 0.001)
        assertEquals(200000.0, acc2?.balance ?: 0.0, 0.001)
    }
}

// ── FAKE DAOS DENGAN FASILITAS TRANSAKSI & ROLLBACK ──────────────────────────

class TransactionalFakeTransactionDao : TransactionDao {
    val currentTransactions = mutableListOf<TransactionEntity>()
    private var nextId = 1L
    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private var accountDaoRef: TransactionalFakeAccountDao? = null

    fun bindAccountDao(dao: TransactionalFakeAccountDao) {
        this.accountDaoRef = dao
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = if (transaction.id == 0L) nextId++ else transaction.id
        val copy = transaction.copy(id = id)
        currentTransactions.add(copy)
        transactionsFlow.value = currentTransactions.toList()
        return id
    }

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionsFlow.asStateFlow()

    override suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity> {
        return currentTransactions.drop(offset).take(limit)
    }

    override suspend fun deleteTransaction(id: Long) {
        currentTransactions.removeAll { it.id == id }
        transactionsFlow.value = currentTransactions.toList()
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        val idx = currentTransactions.indexOfFirst { it.id == transaction.id }
        if (idx != -1) {
            currentTransactions[idx] = transaction
            transactionsFlow.value = currentTransactions.toList()
        }
    }

    override suspend fun getTransactionById(id: Long): TransactionEntity? {
        return currentTransactions.find { it.id == id }
    }

    override suspend fun clearAllTransactions() {
        currentTransactions.clear()
        transactionsFlow.value = emptyList()
    }

    override suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> {
        return transactions.map { insertTransaction(it) }
    }

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        // Snapshot status awal sebelum transaksi
        val snapshotTransactions = currentTransactions.map { it.copy() }.toMutableList()
        val accountSnapshot = accountDaoRef?.createSnapshot()

        return try {
            block()
        } catch (t: Throwable) {
            // Rollback status ke snapshot awal
            currentTransactions.clear()
            currentTransactions.addAll(snapshotTransactions)
            transactionsFlow.value = currentTransactions.toList()
            accountDaoRef?.restoreSnapshot(accountSnapshot)
            throw t
        }
    }
}

class TransactionalFakeAccountDao : AccountDao {
    private val accounts = mutableMapOf<Long, AccountEntity>()
    private var nextId = 1L
    private val accountsFlow = MutableStateFlow<List<AccountEntity>>(emptyList())
    var shouldFailOnAccount: Long? = null

    override suspend fun insertAccount(account: AccountEntity): Long {
        val id = if (account.id == 0L) nextId++ else account.id
        val copy = account.copy(id = id)
        accounts[id] = copy
        accountsFlow.value = accounts.values.toList()
        return id
    }

    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountsFlow.asStateFlow()

    override suspend fun getAccountById(id: Long): AccountEntity? = accounts[id]

    override suspend fun adjustBalance(id: Long, delta: Double) {
        if (shouldFailOnAccount == id) {
            throw RuntimeException("Simulated database failure during balance adjustment for account $id")
        }
        val current = accounts[id] ?: return
        accounts[id] = current.copy(balance = current.balance + delta)
        accountsFlow.value = accounts.values.toList()
    }

    override suspend fun updateBalance(id: Long, newBalance: Double) {
        val current = accounts[id] ?: return
        accounts[id] = current.copy(balance = newBalance)
        accountsFlow.value = accounts.values.toList()
    }

    override suspend fun deleteAccount(id: Long) {
        accounts.remove(id)
        accountsFlow.value = accounts.values.toList()
    }

    fun createSnapshot(): Map<Long, AccountEntity> {
        return accounts.mapValues { it.value.copy() }
    }

    fun restoreSnapshot(snapshot: Map<Long, AccountEntity>?) {
        if (snapshot != null) {
            accounts.clear()
            accounts.putAll(snapshot)
            accountsFlow.value = accounts.values.toList()
        }
    }
}

class SimpleFakeCategoryDao : CategoryDao {
    private val categories = mutableListOf<CategoryEntity>()
    private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    override suspend fun insertCategory(category: CategoryEntity): Long {
        categories.add(category)
        categoriesFlow.value = categories.toList()
        return category.id
    }

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoriesFlow.asStateFlow()
    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override suspend fun updateCategory(category: CategoryEntity): Int = 1
    override suspend fun deleteCategory(id: Long): Int = 1
    override suspend fun getCategoryById(id: Long): CategoryEntity? = null
}
