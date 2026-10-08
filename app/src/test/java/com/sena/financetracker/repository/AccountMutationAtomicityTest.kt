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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit test pembuktian atomisitas transaksi SQLite dan mekanisme rollback
 * untuk mutasi rekening: transferFunds, addAccount, dan reconcileAccount.
 *
 * Menguji jaminan ACID (Atomicity, Consistency, Isolation, Durability)
 * bahwa ketika terjadi kegagalan di langkah manapun (failing block / database error),
 * status saldo rekening dan catatan transaksi kembali utuh ke kondisi awal (zero partial state).
 */
class AccountMutationAtomicityTest {

    private lateinit var fakeTransactionDao: TestTransactionalTransactionDao
    private lateinit var fakeAccountDao: TestTransactionalAccountDao
    private lateinit var fakeCategoryDao: SimpleTestCategoryDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        fakeTransactionDao = TestTransactionalTransactionDao()
        fakeAccountDao = TestTransactionalAccountDao()
        fakeCategoryDao = SimpleTestCategoryDao()

        fakeTransactionDao.bindAccountDao(fakeAccountDao)

        repository = TransactionRepository(
            transactionDao = fakeTransactionDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao
        )
    }

    @Test
    fun testTransferFunds_whenSecondAccountAdjustmentFails_rollsBackFirstAccountBalanceAndZeroTransactions() = runBlocking {
        // Setup: Dua rekening dengan saldo awal
        val acc1Id = fakeAccountDao.insertAccount(AccountEntity(name = "BCA", type = "bank", balance = 1000000.0))
        val acc2Id = fakeAccountDao.insertAccount(AccountEntity(name = "GoPay", type = "e-wallet", balance = 500000.0))

        val acc1 = fakeAccountDao.getAccountById(acc1Id)!!
        val acc2 = fakeAccountDao.getAccountById(acc2Id)!!

        // Simulasikan kegagalan database pada langkah kedua (penambahan saldo akun tujuan)
        fakeAccountDao.shouldFailOnAdjustAccount = acc2Id

        try {
            repository.transferFunds(acc1, acc2, 300000.0, "Transfer Uang Jajan", "2026-10-09")
            fail("Seharusnya melempar RuntimeException ketika penyesuaian saldo akun penerima gagal")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("Simulated failure adjusting balance for account $acc2Id") == true)
        }

        // Verifikasi Rollback:
        // Saldo akun pengirim (BCA) TIDAK terpotong, saldo akun penerima (GoPay) tetap
        val freshAcc1 = fakeAccountDao.getAccountById(acc1Id)!!
        val freshAcc2 = fakeAccountDao.getAccountById(acc2Id)!!

        assertEquals(1000000.0, freshAcc1.balance, 0.001)
        assertEquals(500000.0, freshAcc2.balance, 0.001)

        // Riwayat transaksi tetap 0 (tidak ada catatan transaksi phantom/yatim)
        assertEquals(0, fakeTransactionDao.currentTransactions.size)
    }

    @Test
    fun testTransferFunds_whenTransactionInsertFails_rollsBackBothAccountBalancesAndZeroTransactions() = runBlocking {
        val acc1Id = fakeAccountDao.insertAccount(AccountEntity(name = "BCA", type = "bank", balance = 1000000.0))
        val acc2Id = fakeAccountDao.insertAccount(AccountEntity(name = "GoPay", type = "e-wallet", balance = 500000.0))

        val acc1 = fakeAccountDao.getAccountById(acc1Id)!!
        val acc2 = fakeAccountDao.getAccountById(acc2Id)!!

        // Simulasikan kegagalan database saat insert catatan transaksi transfer
        fakeTransactionDao.shouldFailOnInsert = true

        try {
            repository.transferFunds(acc1, acc2, 300000.0, "Transfer Operasional", "2026-10-09")
            fail("Seharusnya melempar IllegalStateException ketika insert transaksi gagal")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Simulated insert transaction failure") == true)
        }

        // Verifikasi Rollback:
        // Kedua saldo akun harus kembali persis seperti kondisi sebelum transfer
        val freshAcc1 = fakeAccountDao.getAccountById(acc1Id)!!
        val freshAcc2 = fakeAccountDao.getAccountById(acc2Id)!!

        assertEquals(1000000.0, freshAcc1.balance, 0.001)
        assertEquals(500000.0, freshAcc2.balance, 0.001)
        assertEquals(0, fakeTransactionDao.currentTransactions.size)
    }

    @Test
    fun testAddAccount_whenInitialTransactionInsertFails_rollsBackAccountCreation() = runBlocking {
        // Simulasikan kegagalan saat pencatatan transaksi saldo awal
        fakeTransactionDao.shouldFailOnInsert = true

        try {
            repository.addAccount("Tabungan Baru", "bank", 750000.0, "2026-10-09")
            fail("Seharusnya melempar IllegalStateException ketika insert transaksi saldo awal gagal")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Simulated insert transaction failure") == true)
        }

        // Verifikasi Rollback: Akun tidak boleh tersimpan dalam database (zero partial state)
        val allAccounts = fakeAccountDao.createSnapshot()
        assertEquals(0, allAccounts.size)
        assertEquals(0, fakeTransactionDao.currentTransactions.size)
    }

    @Test
    fun testReconcileAccount_readsFreshBalanceFromDatabaseAndIgnoresStaleUiBalance() = runBlocking {
        // Akun awalnya dibuat dengan saldo 100.000
        val accId = fakeAccountDao.insertAccount(AccountEntity(name = "Kas Kecil", type = "cash", balance = 100000.0))

        // Objek UI lama (stale) memegang saldo 100.000
        val staleUiAccount = fakeAccountDao.getAccountById(accId)!!

        // Diam-diam di background (misalnya mutasi transaksi lain), saldo di database berubah menjadi 200.000
        fakeAccountDao.updateBalance(accId, 200000.0)

        // Pengguna melakukan rekonsiliasi ke saldo fisik nyata 250.000 menggunakan objek UI lama
        repository.reconcileAccount(staleUiAccount, 250000.0, "2026-10-09")

        // Verifikasi saldo di database terupdate menjadi 250.000
        val freshAccount = fakeAccountDao.getAccountById(accId)!!
        assertEquals(250000.0, freshAccount.balance, 0.001)

        // Verifikasi selisih dihitung berdasarkan fresh balance (250.000 - 200.000 = +50.000),
        // BUKAN berdasarkan saldo lama UI (250.000 - 100.000 = +150.000)
        assertEquals(1, fakeTransactionDao.currentTransactions.size)
        val tx = fakeTransactionDao.currentTransactions.first()
        assertEquals(50000.0, tx.amount, 0.001)
        assertEquals("INCOME", tx.type)
        assertEquals("Penyesuaian", tx.category)
        assertTrue(tx.notes.contains("saldo lama 200000, saldo baru 250000, selisih +50000"))
    }

    @Test
    fun testReconcileAccount_whenTransactionInsertFails_rollsBackBalanceUpdate() = runBlocking {
        val accId = fakeAccountDao.insertAccount(AccountEntity(name = "BCA Bisnis", type = "bank", balance = 200000.0))
        val account = fakeAccountDao.getAccountById(accId)!!

        // Simulasikan insert penyesuaian transaksi gagal
        fakeTransactionDao.shouldFailOnInsert = true

        try {
            repository.reconcileAccount(account, 300000.0, "2026-10-09")
            fail("Seharusnya melempar IllegalStateException saat insert transaksi penyesuaian gagal")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Simulated insert transaction failure") == true)
        }

        // Verifikasi Rollback: Saldo akun harus tetap 200.000, bukan 300.000
        val freshAccount = fakeAccountDao.getAccountById(accId)!!
        assertEquals(200000.0, freshAccount.balance, 0.001)
        assertEquals(0, fakeTransactionDao.currentTransactions.size)
    }

    @Test
    fun testDirectAccountHandlerTransfer_executesInTransactionAndRollsBackOnFailure() = runBlocking {
        val acc1Id = fakeAccountDao.insertAccount(AccountEntity(name = "Dompet", type = "cash", balance = 100000.0))
        val acc2Id = fakeAccountDao.insertAccount(AccountEntity(name = "Bank", type = "bank", balance = 50000.0))

        val acc1 = fakeAccountDao.getAccountById(acc1Id)!!
        val acc2 = fakeAccountDao.getAccountById(acc2Id)!!

        fakeTransactionDao.shouldFailOnInsert = true

        try {
            repository.accountHandler.transferFunds(acc1, acc2, 30000.0, "Transfer Mandiri", "2026-10-09")
            fail("Seharusnya melempar IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Simulated insert transaction failure") == true)
        }

        assertEquals(100000.0, fakeAccountDao.getAccountById(acc1Id)!!.balance, 0.001)
        assertEquals(50000.0, fakeAccountDao.getAccountById(acc2Id)!!.balance, 0.001)
        assertEquals(0, fakeTransactionDao.currentTransactions.size)
    }
}

// ── TEST TRANSACTIONAL FAKES ──────────────────────────────────────────────────

class TestTransactionalTransactionDao : TransactionDao {
    val currentTransactions = mutableListOf<TransactionEntity>()
    private var nextId = 1L
    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private var accountDaoRef: TestTransactionalAccountDao? = null
    var shouldFailOnInsert = false

    fun bindAccountDao(dao: TestTransactionalAccountDao) {
        this.accountDaoRef = dao
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        if (shouldFailOnInsert) {
            throw IllegalStateException("Simulated insert transaction failure (insert returned -1)")
        }
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
        val snapshotTransactions = currentTransactions.map { it.copy() }.toMutableList()
        val accountSnapshot = accountDaoRef?.createSnapshot()

        return try {
            block()
        } catch (t: Throwable) {
            currentTransactions.clear()
            currentTransactions.addAll(snapshotTransactions)
            transactionsFlow.value = currentTransactions.toList()
            accountDaoRef?.restoreSnapshot(accountSnapshot)
            throw t
        }
    }
}

class TestTransactionalAccountDao : AccountDao {
    private val accounts = mutableMapOf<Long, AccountEntity>()
    private var nextId = 1L
    private val accountsFlow = MutableStateFlow<List<AccountEntity>>(emptyList())
    var shouldFailOnAdjustAccount: Long? = null

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
        if (shouldFailOnAdjustAccount == id) {
            throw RuntimeException("Simulated failure adjusting balance for account $id")
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

class SimpleTestCategoryDao : CategoryDao {
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
