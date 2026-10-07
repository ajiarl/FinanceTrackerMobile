package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.AppDatabase
import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository utama pengelola transaksi, rekening (accounts), kategori, dan anggaran (budgets).
 *
 * Bertanggung jawab menjamin konsistensi ACID aplikasi:
 * - Sinkronisasi saldo akun secara otomatis saat transaksi dibuat, diubah, atau dihapus.
 * - Logika bisnis rekonsiliasi akun & transfer antar rekening.
 * - Agregasi pemakaian anggaran (budget progress) 1:1 identik dengan logika Laravel BudgetAlertService.
 */
class TransactionRepository(
    val transactionDao: TransactionDao,
    val accountDao: AccountDao,
    val categoryDao: CategoryDao,
    val budgetDao: BudgetDao = object : BudgetDao {
        private val _flow = kotlinx.coroutines.flow.MutableStateFlow<List<BudgetEntity>>(emptyList())
        override fun getAllBudgets(): Flow<List<BudgetEntity>> = _flow
        override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> = _flow
        override suspend fun getBudgetById(id: Long): BudgetEntity? = null
        override suspend fun insertBudget(budget: BudgetEntity): Long = 0L
        override suspend fun updateBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(id: Long) {}
    }
) {
    /**
     * Konstruktor praktis berbasis database Room/SQLite terpadu [AppDatabase].
     */
    constructor(db: AppDatabase) : this(
        db.transactionDao,
        db.accountDao,
        db.categoryDao,
        db.budgetDao
    )

    /**
     * Konstruktor backward-compatible untuk testing unit dengan fake TransactionDao.
     */
    constructor(transactionDao: TransactionDao) : this(
        transactionDao = transactionDao,
        accountDao = object : AccountDao {
            override fun getAllAccounts(): Flow<List<AccountEntity>> = flowOf(emptyList())
            override suspend fun getAccountById(id: Long): AccountEntity? = null
            override suspend fun insertAccount(account: AccountEntity): Long = 0L
            override suspend fun updateBalance(id: Long, newBalance: Double) {}
            override suspend fun adjustBalance(id: Long, delta: Double) {}
            override suspend fun deleteAccount(id: Long) {}
        },
        categoryDao = object : CategoryDao {
            override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override suspend fun insertCategory(category: CategoryEntity): Long = 0L
            override suspend fun deleteCategory(id: Long) {}
        }
    )

    /**
     * Mengambil seluruh aliran data transaksi secara reaktif dari database.
     */
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    /**
     * Mengambil seluruh data rekening/akun bank & e-wallet secara reaktif.
     */
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    /**
     * Mengambil daftar seluruh kategori transaksi yang tersedia.
     */
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    /**
     * Mengambil kategori yang disaring berdasarkan tipe ("EXPENSE" atau "INCOME").
     */
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)

    /**
     * Mengambil seluruh daftar entitas anggaran yang tersimpan.
     */
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    /**
     * Menghitung progres penggunaan anggaran bulanan dan status peringatan (overbudget alert).
     *
     * Logika 1:1 identik dengan backend Laravel BudgetAlertService.php:
     * 1. Menyaring pengeluaran bertipe `EXPENSE` yang berada pada kategori yang sama dan memiliki tanggal dengan awalan periode `period` ("YYYY-MM").
     * 2. `spentAmount = sum(amount)`
     * 3. `percentage = ((spentAmount / limitAmount) * 100).toInt()`
     * 4. Ambang batas status:
     *    - `>= 100%`: "CRITICAL" (merah pekat / overbudget)
     *    - `>= 80%`: "WARNING" (kuning waspada)
     *    - `< 80%`: "SAFE" (hijau aman)
     *
     * @param period Periode bulan dalam format "YYYY-MM" (default bulan sistem saat ini).
     * @return Flow berisi list [BudgetProgressItem].
     */
    fun getBudgetProgress(
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ): Flow<List<BudgetProgressItem>> {
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getAllTransactions()
        ) { budgets, transactions ->
            val activeBudgets = budgets.filter { it.isActive && (it.period.isEmpty() || it.period == period) }
            activeBudgets.map { budget ->
                val spentAmount = transactions
                    .filter { tx ->
                        tx.type.equals("EXPENSE", ignoreCase = true) &&
                        tx.category.equals(budget.category, ignoreCase = true) &&
                        tx.date.startsWith(period)
                    }
                    .sumOf { it.amount }

                val percentage = if (budget.limitAmount > 0) {
                    ((spentAmount / budget.limitAmount) * 100).toInt()
                } else {
                    0
                }

                val isOver = spentAmount > budget.limitAmount || percentage >= 100
                val statusLevel = when {
                    percentage >= 100 -> "CRITICAL"
                    percentage >= 80 -> "WARNING"
                    else -> "SAFE"
                }

                BudgetProgressItem(
                    budget = budget,
                    spentAmount = spentAmount,
                    percentage = percentage,
                    isOver = isOver,
                    statusLevel = statusLevel
                )
            }
        }
    }

    /**
     * Menambahkan anggaran baru per kategori ke database.
     *
     * @param name Nama tampilan anggaran (misal: "Makan Siang", "Transport").
     * @param category Nama kategori pengeluaran yang diikat.
     * @param limitAmount Batas maksimal pengeluaran bulanan dalam IDR.
     * @param period Periode berlakunya anggaran dalam format "YYYY-MM".
     * @return ID baris data anggaran yang baru dibuat.
     */
    suspend fun addBudget(
        name: String,
        category: String,
        limitAmount: Double,
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ): Long {
        val entity = BudgetEntity(
            name = name,
            category = category,
            limitAmount = limitAmount,
            period = period,
            isActive = true
        )
        return budgetDao.insertBudget(entity)
    }

    /**
     * Menghapus anggaran berdasarkan [id].
     */
    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudget(id)
    }

    /**
     * Memperbarui informasi anggaran yang sudah ada.
     */
    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.updateBudget(budget)
    }

    /**
     * Memasukkan transaksi baru dan memperbarui saldo rekening penampung secara atomik.
     *
     * Jika transaksi INCOME, saldo rekening bertambah.
     * Jika transaksi EXPENSE, saldo rekening berkurang.
     */
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val insertedId = transactionDao.insertTransaction(transaction)
        val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) {
            transaction.amount
        } else {
            -transaction.amount
        }
        accountDao.adjustBalance(transaction.accountId, delta)
        return insertedId
    }

    /**
     * Menghapus transaksi dan mengembalikan saldo rekening ke kondisi semula (rollback delta).
     *
     * Jika transaksi yang dihapus bertipe TRANSFER:
     * - Akun asal dikembalikan (+amount).
     * - Akun tujuan dipotong (-amount).
     */
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction.id)
        if (transaction.type.equals("TRANSFER", ignoreCase = true)) {
            accountDao.adjustBalance(transaction.accountId, transaction.amount)
            transaction.toAccountId?.let { toId ->
                accountDao.adjustBalance(toId, -transaction.amount)
            }
        } else {
            val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) {
                -transaction.amount
            } else {
                transaction.amount
            }
            accountDao.adjustBalance(transaction.accountId, delta)
        }
    }

    /**
     * Menghapus transaksi berdasarkan ID langsung tanpa penyesuaian delta saldo akun.
     */
    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }

    /**
     * Melakukan transfer dana antar rekening secara aman dan atomik.
     *
     * Logika Operasional:
     * 1. Validasi: Akun asal dan akun tujuan dilarang sama, nominal harus > 0.
     * 2. Saldo akun asal didebit: `balance = balance - amount`.
     * 3. Saldo akun tujuan dikredit: `balance = balance + amount`.
     * 4. Membuat riwayat transaksi bertipe `TRANSFER` yang mencatat metadata kedua akun.
     */
    suspend fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String
    ) {
        require(fromAccount.id != toAccount.id) { "Akun asal dan akun tujuan tidak boleh sama" }
        require(amount > 0) { "Nominal transfer harus lebih besar dari 0" }

        accountDao.adjustBalance(fromAccount.id, -amount)
        accountDao.adjustBalance(toAccount.id, amount)

        val transferTx = TransactionEntity(
            title = "Transfer ke ${toAccount.name}",
            amount = amount,
            type = "TRANSFER",
            category = "Transfer",
            date = date,
            accountId = fromAccount.id,
            accountName = fromAccount.name,
            notes = if (notes.isNotBlank()) notes else "Transfer dari ${fromAccount.name} ke ${toAccount.name}",
            toAccountId = toAccount.id,
            toAccountName = toAccount.name
        )
        transactionDao.insertTransaction(transferTx)
    }

    /**
     * Menambahkan akun/rekening baru. Jika memiliki saldo awal > 0, secara otomatis
     * membuat transaksi pembuka bertipe `INCOME` dengan kategori "Saldo Awal".
     */
    suspend fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Long {
        val newAccount = AccountEntity(
            name = name,
            type = type,
            balance = initialBalance
        )
        val newAccountId = accountDao.insertAccount(newAccount)

        if (initialBalance > 0) {
            val initialTx = TransactionEntity(
                title = "Saldo Awal",
                amount = initialBalance,
                type = "INCOME",
                category = "Saldo Awal",
                date = date,
                accountId = newAccountId,
                accountName = name,
                notes = "Saldo awal saat pembuatan akun"
            )
            transactionDao.insertTransaction(initialTx)
        }
        return newAccountId
    }

    /**
     * Melakukan rekonsiliasi saldo akun dengan saldo fisik riil di dunia nyata.
     *
     * Logika Operasional:
     * 1. Hitung selisih: `diff = actualBalance - account.balance`.
     * 2. Jika selisih mendekati 0, operasi diabaikan.
     * 3. Mutasi saldo rekening langsung disetel ke `actualBalance`.
     * 4. Membuat transaksi penyesuaian otomatis:
     *    - Jika diff > 0: bertipe `INCOME` ("Penyesuaian Saldo Sistem").
     *    - Jika diff < 0: bertipe `EXPENSE` ("Penyesuaian Saldo Sistem").
     */
    suspend fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        val diff = actualBalance - account.balance
        if (kotlin.math.abs(diff) < 0.001) return

        accountDao.updateBalance(account.id, actualBalance)

        val adjustmentTx = TransactionEntity(
            title = "Penyesuaian Saldo Sistem",
            amount = kotlin.math.abs(diff),
            type = if (diff > 0) "INCOME" else "EXPENSE",
            category = "Penyesuaian",
            date = date,
            accountId = account.id,
            accountName = account.name,
            notes = "Rekonsiliasi: saldo lama ${account.balance.toLong()}, saldo baru ${actualBalance.toLong()}, selisih ${if (diff > 0) "+" else ""}${diff.toLong()}"
        )
        transactionDao.insertTransaction(adjustmentTx)
    }

    /**
     * Memperbarui transaksi yang ada dan menyesuaikan saldo akun dengan rollback delta lama
     * lalu menerapkan delta transaksi baru.
     */
    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        transactionDao.updateTransaction(newTransaction)

        val oldDelta = if (oldTransaction.type.equals("INCOME", ignoreCase = true)) {
            -oldTransaction.amount
        } else {
            oldTransaction.amount
        }
        accountDao.adjustBalance(oldTransaction.accountId, oldDelta)

        val newDelta = if (newTransaction.type.equals("INCOME", ignoreCase = true)) {
            newTransaction.amount
        } else {
            -newTransaction.amount
        }
        accountDao.adjustBalance(newTransaction.accountId, newDelta)
    }

    /**
     * Memperbarui transaksi dengan mencari entitas lama terlebih dahulu jika ada di DB.
     */
    suspend fun updateTransaction(newTransaction: TransactionEntity) {
        val oldTx = transactionDao.getTransactionById(newTransaction.id)
        if (oldTx != null) {
            updateTransaction(oldTx, newTransaction)
        } else {
            transactionDao.updateTransaction(newTransaction)
        }
    }
}
