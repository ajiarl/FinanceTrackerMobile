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
    fun `transferFunds throws exception when amount exceeds fromAccount balance causing overdraft`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = accountBca,
                    toAccount = accountGopay,
                    amount = 1500000.0,
                    notes = "Overdraft attempt",
                    date = "2026-10-07"
                )
            }
        }
        assertTrue(exception.message!!.contains("tidak mencukupi"))
    }

    @Test
    fun `transferFunds throws exception when sender account not found in database`() {
        val ghostSender = AccountEntity(id = 999L, name = "Akun Fiktif", type = "bank", balance = 500000.0)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = ghostSender,
                    toAccount = accountGopay,
                    amount = 50000.0,
                    notes = "Sender ghost",
                    date = "2026-10-07"
                )
            }
        }
        assertTrue(ex.message!!.contains("Rekening pengirim tidak ditemukan atau sudah dihapus"))
    }

    @Test
    fun `transferFunds throws exception when receiver account not found in database`() {
        val ghostReceiver = AccountEntity(id = 888L, name = "Penerima Fiktif", type = "bank", balance = 0.0)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = accountBca,
                    toAccount = ghostReceiver,
                    amount = 50000.0,
                    notes = "Receiver ghost",
                    date = "2026-10-07"
                )
            }
        }
        assertTrue(ex.message!!.contains("Rekening penerima tidak ditemukan atau sudah dihapus"))
    }

    @Test
    fun `transferFunds uses fresh account names from database for transaction metadata`() = runBlocking {
        // Update nama akun di database
        fakeAccountDao.setAccount(accountBca.copy(name = "BCA Prioritas"))
        fakeAccountDao.setAccount(accountGopay.copy(name = "GoPay Tabungan"))

        // Panggil transfer menggunakan objek lama yang namanya belum diperbarui
        repository.transferFunds(
            fromAccount = accountBca, // masih "BCA" di objek memori lama
            toAccount = accountGopay, // masih "GoPay" di objek memori lama
            amount = 100000.0,
            notes = "",
            date = "2026-10-07"
        )

        val tx = fakeTransactionDao.transactions.first()
        assertEquals("Transfer ke GoPay Tabungan", tx.title)
        assertEquals("BCA Prioritas", tx.accountName)
        assertEquals("GoPay Tabungan", tx.toAccountName)
        assertEquals("Transfer dari BCA Prioritas ke GoPay Tabungan", tx.notes)
    }

    @Test
    fun `transferFunds succeeds when amount exactly equals fromAccount balance`() = runBlocking {
        repository.transferFunds(
            fromAccount = accountBca,
            toAccount = accountGopay,
            amount = 1000000.0,
            notes = "Full balance transfer",
            date = "2026-10-07"
        )
        val updatedBca = fakeAccountDao.getAccountById(1L)
        assertNotNull(updatedBca)
        assertEquals(0.0, updatedBca!!.balance, 0.001)
        val updatedGopay = fakeAccountDao.getAccountById(2L)
        assertNotNull(updatedGopay)
        assertEquals(1250000.0, updatedGopay!!.balance, 0.001)
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

    @Test
    fun `deleteTransaction by id with TRANSFER type reverts both accounts`() = runBlocking {
        repository.transferFunds(
            fromAccount = accountBca,
            toAccount = accountGopay,
            amount = 150000.0,
            notes = "Test revert by ID",
            date = "2026-10-07"
        )

        val tx = fakeTransactionDao.transactions.first()
        assertEquals(850000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(400000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)

        // Delete by ID
        repository.deleteTransaction(tx.id)

        // Balances should be reverted
        assertEquals(1000000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(250000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)
        assertTrue(fakeTransactionDao.transactions.isEmpty())
    }

    @Test
    fun `updateTransaction modifies transfer amount and adjusts both accounts accurately`() = runBlocking {
        repository.transferFunds(
            fromAccount = accountBca,
            toAccount = accountGopay,
            amount = 100000.0,
            notes = "Transfer Awal",
            date = "2026-10-07"
        )

        val oldTx = fakeTransactionDao.transactions.first()
        assertEquals(900000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(350000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)

        // Update amount from 100k to 250k
        val updatedTx = oldTx.copy(amount = 250000.0)
        repository.updateTransaction(oldTx, updatedTx)

        // BCA should be 1M - 250k = 750k
        assertEquals(750000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        // GoPay should be 250k + 250k = 500k
        assertEquals(500000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)

        val currentSavedTx = fakeTransactionDao.transactions.first()
        assertEquals(250000.0, currentSavedTx.amount, 0.001)
    }

    @Test
    fun `updateTransaction changes transfer destination account and adjusts balances across all three accounts`() = runBlocking {
        val accountMandiri = AccountEntity(id = 3L, name = "Mandiri", type = "bank", balance = 500000.0)
        fakeAccountDao.insertAccount(accountMandiri)

        repository.transferFunds(
            fromAccount = accountBca,
            toAccount = accountGopay,
            amount = 100000.0,
            notes = "Transfer BCA ke GoPay",
            date = "2026-10-07"
        )

        val oldTx = fakeTransactionDao.transactions.first()
        // BCA: 900k, GoPay: 350k, Mandiri: 500k
        assertEquals(900000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(350000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)
        assertEquals(500000.0, fakeAccountDao.getAccountById(3L)!!.balance, 0.001)

        // Change destination from GoPay (id 2) to Mandiri (id 3)
        val updatedTx = oldTx.copy(
            toAccountId = 3L,
            toAccountName = "Mandiri"
        )
        repository.updateTransaction(oldTx, updatedTx)

        // BCA remains deducted by 100k (900k)
        assertEquals(900000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        // GoPay reverts to original 250k
        assertEquals(250000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)
        // Mandiri receives 100k -> 600k
        assertEquals(600000.0, fakeAccountDao.getAccountById(3L)!!.balance, 0.001)
    }

    @Test
    fun `updateTransaction changing from TRANSFER to EXPENSE properly restores destination account balance`() = runBlocking {
        repository.transferFunds(
            fromAccount = accountBca,
            toAccount = accountGopay,
            amount = 100000.0,
            notes = "Transfer salah ketik",
            date = "2026-10-07"
        )

        val oldTx = fakeTransactionDao.transactions.first()
        // BCA: 900k, GoPay: 350k
        assertEquals(900000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(350000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)

        // Change from TRANSFER 100k to EXPENSE 60k on BCA
        val expenseTx = oldTx.copy(
            type = "EXPENSE",
            amount = 60000.0,
            category = "Belanja",
            toAccountId = null,
            toAccountName = null
        )
        repository.updateTransaction(oldTx, expenseTx)

        // GoPay balance must be completely reverted back to 250k
        assertEquals(250000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)
        // BCA was 1M, reverted +100k -> 1M, then debited -60k -> 940k
        assertEquals(940000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
    }

    @Test
    fun `updateTransaction changing from EXPENSE to TRANSFER properly debits source and credits destination`() = runBlocking {
        // Buat transaksi expense 50k pada BCA
        val expenseTx = TransactionEntity(
            id = 1L,
            title = "Beli Buku",
            amount = 50000.0,
            type = "EXPENSE",
            category = "Pendidikan",
            date = "2026-10-07",
            accountId = 1L,
            accountName = "BCA"
        )
        repository.insertTransaction(expenseTx)

        // BCA should be 950k, GoPay should be 250k
        assertEquals(950000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        assertEquals(250000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)

        // Ubah jadi TRANSFER 80k dari BCA ke GoPay
        val transferTx = expenseTx.copy(
            type = "TRANSFER",
            amount = 80000.0,
            toAccountId = 2L,
            toAccountName = "GoPay"
        )
        repository.updateTransaction(expenseTx, transferTx)

        // BCA: 1M (setelah revert +50k) - 80k = 920k
        assertEquals(920000.0, fakeAccountDao.getAccountById(1L)!!.balance, 0.001)
        // GoPay: 250k + 80k = 330k
        assertEquals(330000.0, fakeAccountDao.getAccountById(2L)!!.balance, 0.001)
    }

    // Helper fake DAOs
    private class FakeAccountDao(initialAccounts: List<AccountEntity>) : AccountDao {
        private val accountsMap = initialAccounts.associateBy { it.id }.toMutableMap()
        private val _flow = MutableStateFlow(accountsMap.values.toList())

        fun setAccount(account: AccountEntity) {
            accountsMap[account.id] = account
            _flow.value = accountsMap.values.toList()
        }

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
        override suspend fun updateCategory(category: CategoryEntity): Int = 1
        override suspend fun deleteCategory(id: Long): Int = 1
        override suspend fun getCategoryById(id: Long): CategoryEntity? = null
    }
}
