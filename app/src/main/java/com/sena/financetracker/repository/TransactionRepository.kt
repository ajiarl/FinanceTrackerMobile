package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.AppDatabase
import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.CategoryExpenseSummary
import com.sena.financetracker.data.CategorySpentSummary
import com.sena.financetracker.data.FinanceSummary
import com.sena.financetracker.data.MonthlyCashFlowSummary
import com.sena.financetracker.data.NotificationDao
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.PagedTransactionsResult
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.viewmodel.ReportsAnalyticsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository utama pengelola transaksi, rekening (accounts), kategori, dan anggaran (budgets).
 * Bertindak sebagai fasad terkoordinasi untuk ACID integrity dan pendelegasian domain.
 */
class TransactionRepository(
    val transactionDao: TransactionDao,
    val accountDao: AccountDao,
    val categoryDao: CategoryDao,
    val budgetDao: BudgetDao = createDefaultBudgetDao(),
    val notificationDao: NotificationDao = createDefaultNotificationDao(),
    private val preferences: android.content.SharedPreferences? = null,
    val appDatabase: AppDatabase? = null
) {
    // Delegasi domain handler
    val accountHandler = AccountDomainHandler(
        accountDao = accountDao,
        transactionDao = transactionDao,
        transactionRunner = object : TransactionRunner {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T =
                this@TransactionRepository.runInTransaction(block)
        }
    )
    val categoryHandler = CategoryDomainHandler(categoryDao)
    val budgetHandler = BudgetDomainHandler(budgetDao, transactionDao, notificationDao)
    val notificationHandler = NotificationDomainHandler(notificationDao)

    private val _hapticEnabledFlow = kotlinx.coroutines.flow.MutableStateFlow(
        preferences?.getBoolean("KEY_HAPTIC_ENABLED", true) ?: true
    )

    fun isHapticEnabled(): Flow<Boolean> = _hapticEnabledFlow

    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabledFlow.value = enabled
        preferences?.edit()?.putBoolean("KEY_HAPTIC_ENABLED", enabled)?.apply()
    }

    /**
     * Menjalankan operasi database di dalam transaksi atomic SQLite (ACID).
     * Mendelegasikan ke [appDatabase.runInTransaction] jika database tersedia,
     * atau ke [transactionDao.runInTransaction].
     *
     * @param block Blok kode suspend yang dieksekusi di dalam transaksi.
     * @return Hasil pengembalian dari [block].
     */
    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return appDatabase?.runInTransaction(block) ?: transactionDao.runInTransaction(block)
    }

    /**
     * Konstruktor praktis berbasis database Room/SQLite terpadu [AppDatabase].
     */
    constructor(db: AppDatabase, preferences: android.content.SharedPreferences? = null) : this(
        db.transactionDao,
        db.accountDao,
        db.categoryDao,
        db.budgetDao,
        db.notificationDao,
        preferences,
        db
    )

    /**
     * Konstruktor backward-compatible untuk testing unit dengan fake TransactionDao.
     */
    constructor(transactionDao: TransactionDao) : this(
        transactionDao = transactionDao,
        accountDao = createDefaultAccountDao(),
        categoryDao = createDefaultCategoryDao()
    )

    /**
     * Konstruktor praktis untuk testing unit dengan fake TransactionDao, AccountDao, dan CategoryDao.
     */
    constructor(
        transactionDao: TransactionDao,
        accountDao: AccountDao,
        categoryDao: CategoryDao
    ) : this(
        transactionDao = transactionDao,
        accountDao = accountDao,
        categoryDao = categoryDao,
        budgetDao = createDefaultBudgetDao(),
        notificationDao = createDefaultNotificationDao()
    )

    // ── TRANSACTIONS CORE API ──────────────────────────────────────────────────
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    /**
     * Mengambil daftar transaksi terpaginasi langsung dari database menggunakan query LIMIT dan OFFSET.
     * Mencegah pemuatan seluruh record transaksi ke memori RAM.
     *
     * @param limit Jumlah transaksi maksimal yang dimuat.
     * @param offset Baris transaksi yang dilewati.
     * @return Daftar entitas transaksi terpaginasi.
     */
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity> =
        transactionDao.getTransactionsPaged(limit, offset)

    /**
     * Mengambil daftar transaksi terpaginasi beserta status ketersediaan halaman berikutnya ([PagedTransactionsResult.hasMore]).
     * Menerapkan strategi database query `limit + 1` untuk mendeteksi apakah masih ada halaman lanjutan.
     *
     * @param limit Batas jumlah transaksi yang diinginkan pada halaman aktif.
     * @param offset Posisi awal baris transaksi yang dilewati.
     * @return [PagedTransactionsResult] berisi transaksi maksimal sejumlah limit dan boolean hasMore.
     */
    suspend fun getTransactionsPagedWithHasMore(limit: Int, offset: Int): PagedTransactionsResult =
        transactionDao.getTransactionsPagedWithHasMore(limit, offset)

    /**
     * Menghitung total pemasukan dan pengeluaran secara langsung di tingkat basis data (SQLite engine).
     *
     * @param query Kata kunci pencarian pada judul atau catatan transaksi.
     * @param category Filter kategori transaksi.
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD).
     * @return [FinanceSummary] berisi akumulasi totalIncome dan totalExpense.
     */
    suspend fun getFinanceSummary(
        query: String? = null,
        category: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): FinanceSummary = transactionDao.getFinanceSummary(query, category, startDate, endDate)

    /**
     * Mengambil daftar transaksi terpaginasi dengan penyaringan multi-kriteria langsung dari SQLite.
     *
     * @param query Kata kunci pencarian pada judul atau catatan transaksi.
     * @param type Filter tipe transaksi ("INCOME", "EXPENSE", atau null untuk semua).
     * @param category Filter kategori transaksi.
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD).
     * @param limit Jumlah transaksi per halaman yang diminta.
     * @param offset Pergeseran baris data.
     * @return [PagedTransactionsResult] memuat daftar transaksi dan status hasMore.
     */
    suspend fun getFilteredTransactionsPaged(
        query: String? = null,
        type: String? = null,
        category: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): PagedTransactionsResult = transactionDao.getFilteredTransactionsPaged(
        query = query,
        type = type,
        category = category,
        startDate = startDate,
        endDate = endDate,
        limit = limit,
        offset = offset
    )

    /**
     * Mengambil daftar agregasi pengeluaran per kategori langsung dari SQLite tanpa memuat seluruh baris transaksi.
     *
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @return Daftar [CategoryExpenseSummary] terurut dari nominal pengeluaran terbesar.
     */
    suspend fun getCategoryExpenseSummary(
        startDate: String? = null,
        endDate: String? = null
    ): List<CategoryExpenseSummary> = transactionDao.getCategoryExpenseSummary(startDate, endDate)

    /**
     * Mengambil daftar agregasi arus kas per bulan (YYYY-MM) langsung dari SQLite.
     *
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @return Daftar [MonthlyCashFlowSummary] terurut kronologis bulan.
     */
    suspend fun getMonthlyCashFlowSummary(
        startDate: String? = null,
        endDate: String? = null
    ): List<MonthlyCashFlowSummary> = transactionDao.getMonthlyCashFlowSummary(startDate, endDate)

    /**
     * Menghitung dan menghasilkan ringkasan analitik laporan keuangan lengkap
     * langsung via agregasi SQL SQLite murni tanpa memuat seluruh entitas transaksi ke RAM.
     *
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD atau null untuk semua).
     * @return [ReportsAnalyticsState] ringkas berisi total income, expense, breakdown kategori, dan diagram arus kas bulanan.
     */
    suspend fun getReportsAnalytics(
        startDate: String? = null,
        endDate: String? = null
    ): ReportsAnalyticsState = transactionDao.getReportsAnalytics(startDate, endDate)

    /**
     * Mengambil daftar transaksi dalam rentang periode tanggal tertentu dari SQLite
     * dengan pembatasan jumlah baris ([limit]) setelah dilakukan filter tanggal.
     * Digunakan secara khusus untuk sampel data AI Insight agar transaksi periode
     * tidak terpotong oleh transaksi bulan lain.
     *
     * @param startDate Batas awal tanggal transaksi (format YYYY-MM-DD atau null).
     * @param endDate Batas akhir tanggal transaksi (format YYYY-MM-DD atau null).
     * @param limit Batas maksimal entitas transaksi yang dikembalikan (default 100).
     * @return Daftar entitas transaksi terurut tanggal dan id descending.
     */
    suspend fun getTransactionsForPeriod(
        startDate: String? = null,
        endDate: String? = null,
        limit: Int = 100
    ): List<TransactionEntity> = transactionDao.getTransactionsForPeriod(startDate, endDate, limit)

    /**
     * Mengalirkan sinyal pembaruan data transaksi dari DAO untuk observasi reaktif hemat memori.
     */
    fun getTransactionUpdateTrigger(): Flow<Long> = transactionDao.getTransactionUpdateTrigger()

    /**
     * Menghitung total transaksi tersimpan di database secara efisien melalui query COUNT(*).
     */
    suspend fun getTransactionCount(): Int =
        transactionDao.getTransactionCount()

    /**
     * Mengalirkan data transaksi terpaginasi secara reaktif berbasis query database LIMIT & OFFSET.
     * Mendengarkan sinyal update trigger transaksi tanpa memuat seluruh tabel ke memori RAM (zero RAM leak).
     *
     * @param limit Batas maksimal baris yang diambil.
     * @param offset Posisi baris awal pengambilan data (default 0).
     * @return Flow berisi daftar transaksi terpaginasi.
     */
    fun getTransactionsPagedFlow(limit: Int, offset: Int = 0): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionUpdateTrigger().map {
            val paged = transactionDao.getTransactionsPaged(limit, offset)
            if (paged.isNotEmpty()) paged else transactionDao.getAllTransactions().firstOrNull()?.drop(offset)?.take(limit) ?: emptyList()
        }

    /**
     * Mengalirkan data transaksi terpaginasi secara dinamis berdasarkan StateFlow batas [limitFlow].
     * Terpicu secara reaktif saat jumlah transaksi terlihat bertambah (load more pagination)
     * atau ketika terjadi mutasi data transaksi di database SQLite tanpa eager-loading seluruh tabel.
     *
     * @param limitFlow Flow yang memancarkan batas limit transaksi aktif.
     * @param offset Posisi baris awal pengambilan data (default 0).
     * @return Flow berisi daftar transaksi terpaginasi sesuai limit dinamis.
     */
    fun getTransactionsPagedFlow(limitFlow: Flow<Int>, offset: Int = 0): Flow<List<TransactionEntity>> =
        combine(transactionDao.getTransactionUpdateTrigger(), limitFlow) { _, limit ->
            val paged = transactionDao.getTransactionsPaged(limit, offset)
            if (paged.isNotEmpty()) paged else transactionDao.getAllTransactions().firstOrNull()?.drop(offset)?.take(limit) ?: emptyList()
        }

    /**
     * Mengalirkan data transaksi terpaginasi dinamis beserta status [PagedTransactionsResult.hasMore].
     *
     * @param limitFlow Flow batas limit transaksi aktif.
     * @param offset Posisi baris awal data.
     * @return Flow [PagedTransactionsResult] dengan transaksi terpaginasi dan status hasMore.
     */
    fun getTransactionsPagedWithHasMoreFlow(limitFlow: Flow<Int>, offset: Int = 0): Flow<PagedTransactionsResult> =
        combine(transactionDao.getTransactionUpdateTrigger(), limitFlow) { _, limit ->
            transactionDao.getTransactionsPagedWithHasMore(limit, offset)
        }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val insertedId = runInTransaction {
            val id = transactionDao.insertTransaction(transaction)
            val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) {
                transaction.amount
            } else {
                -transaction.amount
            }
            accountDao.adjustBalance(transaction.accountId, delta)
            id
        }

        if (transaction.type.equals("EXPENSE", ignoreCase = true)) {
            budgetHandler.checkAndTriggerBudgetAlert(transaction)
        }

        return insertedId
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        runInTransaction {
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
    }

    suspend fun deleteTransaction(id: Long) {
        val tx = transactionDao.getTransactionById(id)
        if (tx != null) {
            deleteTransaction(tx)
        } else {
            transactionDao.deleteTransaction(id)
        }
    }

    suspend fun resetTransactions() = transactionDao.clearAllTransactions()

    /**
     * Memasukkan daftar transaksi secara batch dan memperbarui saldo rekening secara atomik (ACID).
     * Seluruh operasi batch insert dan penyesuaian delta saldo dibungkus dalam transaksi SQLite.
     * Jika terjadi kegagalan atau exception pada salah satu langkah, seluruh operasi dibatalkan (rollback)
     * dan saldo rekening tidak mengalami distorsi.
     *
     * @param transactions Daftar transaksi yang akan disimpan.
     * @return Daftar ID transaksi yang berhasil dibuat.
     */
    suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> {
        if (transactions.isEmpty()) return emptyList()

        val ids = runInTransaction {
            val insertedIds = transactionDao.insertTransactionsBatch(transactions)

            val deltasByAccount = mutableMapOf<Long, Double>()
            for (tx in transactions) {
                if (tx.type.equals("TRANSFER", ignoreCase = true)) {
                    val currentFrom = deltasByAccount.getOrDefault(tx.accountId, 0.0)
                    deltasByAccount[tx.accountId] = currentFrom - tx.amount
                    tx.toAccountId?.let { toId ->
                        val currentTo = deltasByAccount.getOrDefault(toId, 0.0)
                        deltasByAccount[toId] = currentTo + tx.amount
                    }
                } else {
                    val delta = if (tx.type.equals("INCOME", ignoreCase = true)) {
                        tx.amount
                    } else {
                        -tx.amount
                    }
                    val current = deltasByAccount.getOrDefault(tx.accountId, 0.0)
                    deltasByAccount[tx.accountId] = current + delta
                }
            }

            deltasByAccount.forEach { (accountId, totalDelta) ->
                accountDao.adjustBalance(accountId, totalDelta)
            }

            insertedIds
        }

        transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .forEach { tx ->
                budgetHandler.checkAndTriggerBudgetAlert(tx)
            }

        return ids
    }

    /**
     * Memperbarui transaksi dan menyesuaikan saldo rekening secara konsisten.
     * Mendukung perubahan jenis transaksi antara INCOME, EXPENSE, dan TRANSFER secara atomik dan akurat.
     *
     * @param oldTransaction Data transaksi sebelum pembaruan (digunakan untuk membatalkan mutasi saldo lama).
     * @param newTransaction Data transaksi baru yang akan disimpan dan diterapkan mutasi saldonya.
     */
    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        runInTransaction {
            transactionDao.updateTransaction(newTransaction)

            // 1. Batalkan mutasi saldo transaksi lama
            if (oldTransaction.type.equals("TRANSFER", ignoreCase = true)) {
                accountDao.adjustBalance(oldTransaction.accountId, oldTransaction.amount)
                oldTransaction.toAccountId?.let { toId ->
                    accountDao.adjustBalance(toId, -oldTransaction.amount)
                }
            } else {
                val oldDelta = if (oldTransaction.type.equals("INCOME", ignoreCase = true)) {
                    -oldTransaction.amount
                } else {
                    oldTransaction.amount
                }
                accountDao.adjustBalance(oldTransaction.accountId, oldDelta)
            }

            // 2. Terapkan mutasi saldo transaksi baru
            if (newTransaction.type.equals("TRANSFER", ignoreCase = true)) {
                accountDao.adjustBalance(newTransaction.accountId, -newTransaction.amount)
                newTransaction.toAccountId?.let { toId ->
                    accountDao.adjustBalance(toId, newTransaction.amount)
                }
            } else {
                val newDelta = if (newTransaction.type.equals("INCOME", ignoreCase = true)) {
                    newTransaction.amount
                } else {
                    -newTransaction.amount
                }
                accountDao.adjustBalance(newTransaction.accountId, newDelta)
            }
        }

        if (newTransaction.type.equals("EXPENSE", ignoreCase = true)) {
            budgetHandler.checkAndTriggerBudgetAlert(newTransaction)
        }
    }

    suspend fun updateTransaction(newTransaction: TransactionEntity) {
        val oldTx = transactionDao.getTransactionById(newTransaction.id)
        if (oldTx != null) {
            updateTransaction(oldTx, newTransaction)
        } else {
            transactionDao.updateTransaction(newTransaction)
        }
    }

    // ── ACCOUNTS DOMAIN FACADE ────────────────────────────────────────────────
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountHandler.getAllAccounts()

    /**
     * Mendelegasikan transfer dana antar rekening ke [AccountDomainHandler] dengan validasi overdraft
     * dan jaminan atomisitas ACID via [runInTransaction].
     *
     * @throws IllegalArgumentException Jika rekening sama, nominal tidak valid, atau saldo asal tidak cukup.
     */
    suspend fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String
    ) = runInTransaction {
        accountHandler.transferFunds(fromAccount, toAccount, amount, notes, date)
    }

    /**
     * Menambahkan akun baru dan mencatat saldo awal secara atomik melalui [AccountDomainHandler].
     */
    suspend fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Long = runInTransaction {
        accountHandler.addAccount(name, type, initialBalance, date)
    }

    /**
     * Merekonsiliasi saldo akun dengan saldo aktual secara atomik melalui [AccountDomainHandler].
     */
    suspend fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) = runInTransaction {
        accountHandler.reconcileAccount(account, actualBalance, date)
    }

    // ── CATEGORIES DOMAIN FACADE ──────────────────────────────────────────────
    val systemCategoryNames: Set<String> get() = categoryHandler.systemCategoryNames

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryHandler.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryHandler.getCategoriesByType(type)

    fun isSystemCategory(name: String): Boolean = categoryHandler.isSystemCategory(name)

    suspend fun insertCategory(name: String, type: String, color: String = "#FAFF00"): Long =
        categoryHandler.insertCategory(name, type, color)

    suspend fun updateCategory(id: Long, name: String, type: String, color: String): Int =
        categoryHandler.updateCategory(id, name, type, color)

    suspend fun deleteCategory(id: Long): Int = categoryHandler.deleteCategory(id)

    // ── BUDGETS DOMAIN FACADE ─────────────────────────────────────────────────
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetHandler.getAllBudgets()

    fun getBudgetProgress(
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ): Flow<List<BudgetProgressItem>> = budgetHandler.getBudgetProgress(period)

    suspend fun addBudget(
        name: String,
        category: String,
        limitAmount: Double,
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ): Long = budgetHandler.addBudget(name, category, limitAmount, period)

    suspend fun deleteBudget(id: Long) = budgetHandler.deleteBudget(id)

    suspend fun updateBudget(budget: BudgetEntity) = budgetHandler.updateBudget(budget)

    /**
     * Mengambil daftar ringkasan pengeluaran seluruh kategori untuk periode awalan tertentu
     * langsung via engine SQL SQLite tanpa memuat seluruh baris transaksi ke RAM.
     *
     * @param periodPrefix Awalan teks tanggal transaksi (contoh: "2026-10").
     * @return Daftar [CategorySpentSummary] berisi kategori dan nominal pengeluaran.
     */
    suspend fun getCategoryExpensesForPeriod(periodPrefix: String): List<CategorySpentSummary> {
        return transactionDao.getCategoryExpensesForPeriod(periodPrefix)
    }

    /**
     * Menghitung total pengeluaran untuk satu kategori spesifik pada periode awalan tertentu
     * langsung via engine SQL SQLite.
     *
     * @param category Nama kategori pengeluaran.
     * @param periodPrefix Awalan teks tanggal transaksi (contoh: "2026-10").
     * @return Akumulasi nominal pengeluaran bertipe EXPENSE.
     */
    suspend fun getCategorySpentForPeriod(category: String, periodPrefix: String): Double {
        return transactionDao.getCategorySpentForPeriod(category, periodPrefix)
    }

    // ── NOTIFICATIONS DOMAIN FACADE ───────────────────────────────────────────
    fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationHandler.getAllNotifications()

    fun getUnreadNotificationCount(): Flow<Int> = notificationHandler.getUnreadNotificationCount()

    suspend fun markNotificationAsRead(id: Long) = notificationHandler.markNotificationAsRead(id)

    suspend fun markAllNotificationsAsRead() = notificationHandler.markAllNotificationsAsRead()

    suspend fun clearAllNotifications() = notificationHandler.clearAllNotifications()

    suspend fun deleteNotification(id: Long) = notificationHandler.deleteNotification(id)

    suspend fun insertNotification(notification: NotificationEntity): Long =
        notificationHandler.insertNotification(notification)

    companion object {
        private fun createDefaultBudgetDao(): BudgetDao = object : BudgetDao {
            private val _flow = kotlinx.coroutines.flow.MutableStateFlow<List<BudgetEntity>>(emptyList())
            override fun getAllBudgets(): Flow<List<BudgetEntity>> = _flow
            override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> = _flow
            override suspend fun getBudgetById(id: Long): BudgetEntity? = null
            override suspend fun insertBudget(budget: BudgetEntity): Long = 0L
            override suspend fun updateBudget(budget: BudgetEntity) {}
            override suspend fun deleteBudget(id: Long) {}
        }

        private fun createDefaultNotificationDao(): NotificationDao = object : NotificationDao {
            private val _flow = kotlinx.coroutines.flow.MutableStateFlow<List<NotificationEntity>>(emptyList())
            override fun getAllNotifications(): Flow<List<NotificationEntity>> = _flow
            override fun getUnreadCount(): Flow<Int> = flowOf(0)
            override suspend fun insertNotification(notification: NotificationEntity): Long = 0L
            override suspend fun markAsRead(id: Long) {}
            override suspend fun markAllAsRead() {}
            override suspend fun clearAllNotifications() {}
            override suspend fun deleteNotification(id: Long) {}
        }

        private fun createDefaultAccountDao(): AccountDao = object : AccountDao {
            override fun getAllAccounts(): Flow<List<AccountEntity>> = flowOf(emptyList())
            override suspend fun getAccountById(id: Long): AccountEntity? = null
            override suspend fun insertAccount(account: AccountEntity): Long = 0L
            override suspend fun updateBalance(id: Long, newBalance: Double) {}
            override suspend fun adjustBalance(id: Long, delta: Double) {}
            override suspend fun deleteAccount(id: Long) {}
        }

        private fun createDefaultCategoryDao(): CategoryDao = object : CategoryDao {
            override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override suspend fun insertCategory(category: CategoryEntity): Long = 0L
            override suspend fun updateCategory(category: CategoryEntity): Int = 0
            override suspend fun deleteCategory(id: Long): Int = 0
            override suspend fun getCategoryById(id: Long): CategoryEntity? = null
        }
    }
}
