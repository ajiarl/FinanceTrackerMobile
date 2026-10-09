package com.sena.financetracker.data

import android.content.Context
import com.sena.financetracker.repository.AccountDomainHandler
import com.sena.financetracker.repository.TransactionRepository
import com.sena.financetracker.viewmodel.FinanceViewModel
import com.sena.financetracker.viewmodel.ReportsPreset
import com.sena.financetracker.viewmodel.resolveReportsDateRange
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit test verifikasi atomisitas ACID dan jaminan rollback SQLite native menggunakan Robolectric.
 * Menguji skenario transfer dan mutasi akun multi-tahap pada [AppDatabase] nyata.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseRollbackRobolectricTest {

    private lateinit var context: Context
    private lateinit var appDb: AppDatabase
    private lateinit var repository: TransactionRepository
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-10-15T00:00:00Z"),
        ZoneId.systemDefault()
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        appDb = AppDatabase(context)
        // Pastikan database SQLite fisik terinisialisasi dan dieksekusi onCreate (seeding)
        appDb.writableDatabase
        repository = TransactionRepository(
            transactionDao = appDb.transactionDao,
            accountDao = appDb.accountDao,
            categoryDao = appDb.categoryDao,
            budgetDao = appDb.budgetDao,
            notificationDao = appDb.notificationDao
        )
    }

    @After
    fun tearDown() {
        appDb.close()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
    }

    @Test
    fun testRealDatabaseSeedingMatchesInitialState() = runBlocking {
        val dompet = appDb.accountDao.getAccountById(1L)
        val bca = appDb.accountDao.getAccountById(2L)
        val gopay = appDb.accountDao.getAccountById(3L)

        assertNotNull(dompet)
        assertNotNull(bca)
        assertNotNull(gopay)

        assertEquals("Dompet Tunai", dompet!!.name)
        assertEquals(500000.0, dompet.balance, 0.001)

        assertEquals("BCA", bca!!.name)
        assertEquals(5000000.0, bca.balance, 0.001)

        assertEquals("GoPay", gopay!!.name)
        assertEquals(250000.0, gopay.balance, 0.001)

        val tx = appDb.transactionDao.getTransactionById(1L)
        org.junit.Assert.assertNull(tx)
    }

    @Test
    fun testSuccessfulTransferPersistsBothAccountsAndTransactionInSqlite() = runBlocking {
        val bca = appDb.accountDao.getAccountById(2L)!!
        val gopay = appDb.accountDao.getAccountById(3L)!!

        val transferAmount = 350000.0
        repository.transferFunds(
            fromAccount = bca,
            toAccount = gopay,
            amount = transferAmount,
            notes = "Transfer BCA ke GoPay",
            date = "2026-10-09"
        )

        // Verifikasi saldo di SQLite nyata
        val updatedBca = appDb.accountDao.getAccountById(2L)!!
        val updatedGopay = appDb.accountDao.getAccountById(3L)!!

        assertEquals(5000000.0 - transferAmount, updatedBca.balance, 0.001)
        assertEquals(250000.0 + transferAmount, updatedGopay.balance, 0.001)

        // Verifikasi transaksi tercatat di tabel SQLite
        val txs = appDb.transactionDao.getAllTransactions().first()
        assertEquals(1, txs.size)
        val tx = txs.first()
        assertEquals("Transfer ke GoPay", tx.title)
        assertEquals(transferAmount, tx.amount, 0.001)
        assertEquals("TRANSFER", tx.type)
        assertEquals(2L, tx.accountId)
        assertEquals(3L, tx.toAccountId)
    }

    @Test
    fun testPartialFailureDuringTransferTriggersFullSqliteRollback() = runBlocking {
        val bcaBefore = appDb.accountDao.getAccountById(2L)!!
        val gopayBefore = appDb.accountDao.getAccountById(3L)!!
        assertEquals(5000000.0, bcaBefore.balance, 0.001)
        assertEquals(250000.0, gopayBefore.balance, 0.001)

        val txCountBefore = appDb.transactionDao.getAllTransactions().first().size
        assertEquals(0, txCountBefore)

        // Simulasikan kegagalan atomik di tengah transaksi multi-step:
        // Langkah 1: Debit BCA sukses
        // Langkah 2: Melempar exception fatal sebelum kredit GoPay / pencatatan transaksi selesai
        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                appDb.runInTransaction {
                    // Step 1: Kurangi saldo BCA di database nyata
                    appDb.accountDao.adjustBalance(2L, -500000.0)

                    // Step 2: Lempar exception kegagalan sistem di tengah jalan
                    throw IllegalStateException("Simulasi kegagalan jaringan atau disk IO di tengah transfer")
                }
            }
        }
        assertTrue(exception.message!!.contains("Simulasi kegagalan"))

        // Pembuktian Rollback Penuh di SQLite Native:
        // 1. Saldo BCA harus tetap 5.000.000 (tidak berkurang)
        val bcaAfter = appDb.accountDao.getAccountById(2L)!!
        assertEquals(5000000.0, bcaAfter.balance, 0.001)

        // 2. Saldo GoPay harus tetap 250.000 (tidak bertambah)
        val gopayAfter = appDb.accountDao.getAccountById(3L)!!
        assertEquals(250000.0, gopayAfter.balance, 0.001)

        // 3. Tidak boleh ada baris transaksi yang tersimpan di tabel transactions
        val txsAfter = appDb.transactionDao.getAllTransactions().first()
        assertTrue(txsAfter.isEmpty())
    }

    @Test
    fun testTransferFailsWhenTargetAccountMissingAndRollsBackDebitedBalance() = runBlocking {
        val bcaBefore = appDb.accountDao.getAccountById(2L)!!
        assertEquals(5000000.0, bcaBefore.balance, 0.001)

        // Buat objek rekening penerima fiktif (ID 9999 tidak ada di database)
        val nonExistentReceiver = AccountEntity(
            id = 9999L,
            name = "Akun Hantu",
            type = "bank",
            balance = 0.0
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = bcaBefore,
                    toAccount = nonExistentReceiver,
                    amount = 100000.0,
                    notes = "Transfer ke akun hantu",
                    date = "2026-10-09"
                )
            }
        }
        assertTrue(exception.message!!.contains("Rekening penerima tidak ditemukan atau sudah dihapus"))

        // Verifikasi saldo BCA tidak berubah di SQLite
        val bcaAfter = appDb.accountDao.getAccountById(2L)!!
        assertEquals(5000000.0, bcaAfter.balance, 0.001)

        // Verifikasi tidak ada transaksi tersimpan
        val txsAfter = appDb.transactionDao.getAllTransactions().first()
        assertTrue(txsAfter.isEmpty())
    }

    @Test
    fun testTransferFailsWhenSenderAccountMissingAndPerformsZeroMutations() = runBlocking {
        val gopayBefore = appDb.accountDao.getAccountById(3L)!!
        assertEquals(250000.0, gopayBefore.balance, 0.001)

        // Buat objek rekening pengirim fiktif (ID 8888 tidak ada di database)
        val nonExistentSender = AccountEntity(
            id = 8888L,
            name = "Akun Pengirim Hantu",
            type = "bank",
            balance = 1000000.0
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.transferFunds(
                    fromAccount = nonExistentSender,
                    toAccount = gopayBefore,
                    amount = 100000.0,
                    notes = "Transfer dari pengirim hantu",
                    date = "2026-10-09"
                )
            }
        }
        assertTrue(exception.message!!.contains("Rekening pengirim tidak ditemukan atau sudah dihapus"))

        // Verifikasi saldo GoPay tidak bertambah di SQLite
        val gopayAfter = appDb.accountDao.getAccountById(3L)!!
        assertEquals(250000.0, gopayAfter.balance, 0.001)

        // Verifikasi tidak ada transaksi tersimpan
        val txsAfter = appDb.transactionDao.getAllTransactions().first()
        assertTrue(txsAfter.isEmpty())
    }

    @Test
    fun testRealDatabasePaginationLimitOffsetAndDynamicFlow() = runBlocking {
        // Masukkan 25 transaksi dummy ke dalam SQLite nyata
        for (i in 1..25) {
            val tx = TransactionEntity(
                title = "Transaksi Dummy #$i",
                amount = 10000.0 * i,
                type = "EXPENSE",
                category = "Umum",
                date = "2026-10-09",
                accountId = 2L,
                accountName = "BCA"
            )
            appDb.transactionDao.insertTransaction(tx)
        }

        // 1. Uji query paged langsung dengan LIMIT 10 OFFSET 0
        val page1 = repository.getTransactionsPaged(limit = 10, offset = 0)
        assertEquals(10, page1.size)

        // 2. Uji query paged dengan LIMIT 10 OFFSET 10
        val page2 = repository.getTransactionsPaged(limit = 10, offset = 10)
        assertEquals(10, page2.size)

        // Pastikan tidak ada duplikasi ID antara halaman 1 dan 2
        val page1Ids = page1.map { it.id }.toSet()
        val page2Ids = page2.map { it.id }.toSet()
        assertTrue(page1Ids.intersect(page2Ids).isEmpty())

        // 3. Uji getTransactionsPagedFlow dengan limit dinamis (simulasi loadMoreTransactions)
        val limitFlow = kotlinx.coroutines.flow.MutableStateFlow(5)
        val pagedFlow = repository.getTransactionsPagedFlow(limitFlow = limitFlow, offset = 0)

        val firstEmission = pagedFlow.first()
        assertEquals(5, firstEmission.size)

        // Naikkan batas limit (user menekan muat lebih banyak)
        limitFlow.value = 15
        val secondEmission = pagedFlow.first()
        assertEquals(15, secondEmission.size)
    }

    @Test
    fun testRealistic120TransactionsPaginationAndSummaryIntegrity() = runBlocking {
        // Masukkan 120 transaksi ke SQLite: 60 Income (@100.000) dan 60 Expense (@50.000)
        val dummyList = (1..120).map { i ->
            val isIncome = i % 2 != 0
            val title = when (i) {
                10 -> "Pengeluaran Khusus #10"
                80 -> "Pengeluaran Khusus #80"
                110 -> "Pemasukan Spesial #110"
                else -> if (isIncome) "Pemasukan #$i" else "Pengeluaran #$i"
            }
            TransactionEntity(
                title = title,
                amount = if (isIncome) 100000.0 else 50000.0,
                type = if (isIncome) "INCOME" else "EXPENSE",
                category = if (isIncome) "Gaji" else "Makan",
                date = "2026-10-09",
                accountId = 2L,
                accountName = "BCA"
            )
        }
        appDb.transactionDao.insertTransactionsBatch(dummyList)

        // 1. Verifikasi query DAO & Repository dengan SQLite nyata
        val totalCount = repository.getTransactionCount()
        assertEquals(120, totalCount)

        val page1Direct = repository.getTransactionsPagedWithHasMore(limit = 50, offset = 0)
        assertEquals(50, page1Direct.transactions.size)
        assertTrue(page1Direct.hasMore)

        val page2Direct = repository.getTransactionsPagedWithHasMore(limit = 100, offset = 0)
        assertEquals(100, page2Direct.transactions.size)
        assertTrue(page2Direct.hasMore)

        val page3Direct = repository.getTransactionsPagedWithHasMore(limit = 150, offset = 0)
        assertEquals(120, page3Direct.transactions.size)
        assertFalse(page3Direct.hasMore)

        // 2. Verifikasi integrasi FinanceViewModel
        val viewModel = FinanceViewModel(
            repository = repository,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined),
            clock = fixedClock
        )

        // Uji Batas RAM / Memory: Halaman pertama (limit 50) hanya memuat tepat 50 baris ke RAM
        val state1 = viewModel.uiState.filter { !it.isLoading && it.filteredTransactions.size == 50 }.first()
        assertEquals(50, state1.filteredTransactions.size)
        assertTrue(state1.filteredTransactions.size <= 50)
        assertTrue(state1.transactions.size <= 50)
        assertTrue(state1.hasMoreTransactions)

        // Uji Summary SQL: Total ringkasan (income, expense, reports) tetap menghitung 120 transaksi secara utuh via agregasi SQL
        val expectedIncome = 60 * 100000.0 // 6.000.000,0
        val expectedExpense = 60 * 50000.0 // 3.000.000,0
        assertEquals(expectedIncome, state1.totalIncome, 0.001)
        assertEquals(expectedExpense, state1.totalExpense, 0.001)
        assertEquals(expectedIncome, state1.reportsAnalytics.totalIncome, 0.001)
        assertEquals(expectedExpense, state1.reportsAnalytics.totalExpense, 0.001)

        val directSummary = repository.getFinanceSummary()
        assertEquals(expectedIncome, directSummary.totalIncome, 0.001)
        assertEquals(expectedExpense, directSummary.totalExpense, 0.001)

        // Uji Transaksi Luar Halaman: Transaksi #10 (posisi ke-111 secara kronologis) ditemukan via pencarian database murni
        viewModel.setSearchQuery("Pengeluaran Khusus #10")
        val searchState10 = viewModel.uiState.filter { it.searchQuery == "Pengeluaran Khusus #10" && it.filteredTransactions.isNotEmpty() }.first()
        assertEquals(1, searchState10.filteredTransactions.size)
        assertEquals("Pengeluaran Khusus #10", searchState10.filteredTransactions[0].title)

        // Pencarian menemukan transaksi ke-80 atau ke-110 meskipun awalnya hanya halaman pertama yang dimuat
        viewModel.setSearchQuery("Pengeluaran Khusus #80")
        val searchState80 = viewModel.uiState.filter { it.searchQuery == "Pengeluaran Khusus #80" && it.filteredTransactions.isNotEmpty() }.first()
        assertEquals(1, searchState80.filteredTransactions.size)
        assertEquals("Pengeluaran Khusus #80", searchState80.filteredTransactions[0].title)

        viewModel.setSearchQuery("Pemasukan Spesial #110")
        val searchState110 = viewModel.uiState.filter { it.searchQuery == "Pemasukan Spesial #110" && it.filteredTransactions.isNotEmpty() }.first()
        assertEquals(1, searchState110.filteredTransactions.size)
        assertEquals("Pemasukan Spesial #110", searchState110.filteredTransactions[0].title)

        // Reset query pencarian
        viewModel.setSearchQuery("")

        // Klik "muat lebih banyak" (limit 100): menampilkan 100 transaksi, hasMoreTransactions = TRUE
        viewModel.loadMoreTransactions()
        val state2 = viewModel.uiState.filter { it.visibleTransactionCount == 100 && it.searchQuery.isEmpty() && it.filteredTransactions.size == 100 }.first()
        assertEquals(100, state2.filteredTransactions.size)
        assertTrue(state2.hasMoreTransactions)

        // Halaman terakhir (limit 150): menampilkan 120 transaksi, hasMoreTransactions = FALSE
        viewModel.loadMoreTransactions()
        val state3 = viewModel.uiState.filter { it.visibleTransactionCount == 150 && it.searchQuery.isEmpty() && it.filteredTransactions.size == 120 }.first()
        assertEquals(120, state3.filteredTransactions.size)
        assertFalse(state3.hasMoreTransactions)
    }

    /**
     * Memverifikasi pemisahan alur laporan analitik (Dedicated Reports SQL Analytics & Preset Isolation).
     *
     * 1. Buktikan saat preset LAST_MONTH dipilih, total dan komposisi kategori HANYA menghitung data bulan lalu.
     * 2. Buktikan saat pencarian daftar transaksi aktif mencari keyword tertentu, data laporan reportsAnalytics tetap utuh.
     * 3. Buktikan saat pagination daftar dinaikkan dari 50 -> 100 -> 120+, seluruh isi reportsAnalytics identik dan tidak bergeser sama sekali.
     */
    @Test
    fun testDedicatedReportsSqlAnalyticsAndPresetIsolation() = runBlocking {
        val (thisMonthStart, thisMonthEnd) = resolveReportsDateRange(ReportsPreset.THIS_MONTH, fixedClock)
        val (lastMonthStart, lastMonthEnd) = resolveReportsDateRange(ReportsPreset.LAST_MONTH, fixedClock)

        assertNotNull(thisMonthStart)
        assertNotNull(lastMonthStart)

        // Bersihkan tabel transaksi dari data test sebelumnya jika ada
        val existing = repository.getAllTransactions().first()
        for (tx in existing) {
            repository.deleteTransaction(tx.id)
        }

        // Siapkan kategori spesifik dengan warna
        repository.insertCategory(name = "Makanan", type = "EXPENSE", color = "#EF4444")
        repository.insertCategory(name = "Transportasi", type = "EXPENSE", color = "#3B82F6")
        repository.insertCategory(name = "Belanja", type = "EXPENSE", color = "#10B981")

        // 1. Masukkan data transaksi bulan lalu (LAST_MONTH)
        // Income bulan lalu: Gaji 5.000.000
        val txIncomeLastMonth = TransactionEntity(
            title = "Gaji Bulan Lalu",
            amount = 5000000.0,
            type = "INCOME",
            category = "Gaji",
            date = lastMonthStart!!
        )
        // Expense 1 bulan lalu: Makanan 1.500.000
        val txExpFoodLastMonth = TransactionEntity(
            title = "Makan Siang Resto",
            amount = 1500000.0,
            type = "EXPENSE",
            category = "Makanan",
            date = lastMonthStart
        )
        // Expense 2 bulan lalu: Transportasi 500.000
        val txExpTransLastMonth = TransactionEntity(
            title = "Bensin Motor",
            amount = 500000.0,
            type = "EXPENSE",
            category = "Transportasi",
            date = lastMonthStart
        )
        repository.insertTransaction(txIncomeLastMonth)
        repository.insertTransaction(txExpFoodLastMonth)
        repository.insertTransaction(txExpTransLastMonth)

        // 2. Masukkan data transaksi bulan ini (THIS_MONTH)
        // Income bulan ini: Bonus 10.000.000
        val txIncomeThisMonth = TransactionEntity(
            title = "Bonus Kinerja",
            amount = 10000000.0,
            type = "INCOME",
            category = "Bonus",
            date = thisMonthStart!!
        )
        // Expense bulan ini: Belanja 3.000.000
        val txExpShoppingThisMonth = TransactionEntity(
            title = "Belanja Bulanan",
            amount = 3000000.0,
            type = "EXPENSE",
            category = "Belanja",
            date = thisMonthStart
        )
        repository.insertTransaction(txIncomeThisMonth)
        repository.insertTransaction(txExpShoppingThisMonth)

        // Tambahkan transaksi dummy bulan ini sebanyak 118 transaksi (total transaksi: 3 + 2 + 118 = 123)
        for (i in 1..118) {
            val isEven = i % 2 == 0
            val dummyTx = TransactionEntity(
                title = if (isEven) "Bonus Ekstra #$i" else "Belanja Harian #$i",
                amount = if (isEven) 100000.0 else 50000.0,
                type = if (isEven) "INCOME" else "EXPENSE",
                category = if (isEven) "Bonus" else "Belanja",
                date = thisMonthStart
            )
            repository.insertTransaction(dummyTx)
        }

        val allTx = repository.getAllTransactions().first()
        assertEquals(123, allTx.size)

        // Inisialisasi ViewModel
        val viewModel = FinanceViewModel(
            repository = repository,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined),
            clock = fixedClock
        )

        // Tunggu state awal siap (50 item pertama)
        val initialState = viewModel.uiState.filter { !it.isLoading && it.filteredTransactions.size == 50 }.first()
        assertEquals(50, initialState.filteredTransactions.size)

        // 3. Set preset laporan ke LAST_MONTH
        viewModel.setReportsPeriodPreset("LAST_MONTH")
        val stateLastMonth = viewModel.uiState.filter { it.reportsAnalytics.periodPreset == "LAST_MONTH" }.first()

        // PENEGASAN 1: Total dan komposisi kategori HANYA menghitung data bulan lalu
        val expectedIncomeLastMonth = 5000000.0
        val expectedExpenseLastMonth = 2000000.0 // 1.500.000 (Makanan) + 500.000 (Transportasi)
        val expectedNetSavingsLastMonth = 3000000.0

        assertEquals(expectedIncomeLastMonth, stateLastMonth.reportsAnalytics.totalIncome, 0.001)
        assertEquals(expectedExpenseLastMonth, stateLastMonth.reportsAnalytics.totalExpense, 0.001)
        assertEquals(expectedNetSavingsLastMonth, stateLastMonth.reportsAnalytics.netSavings, 0.001)

        val breakdownLastMonth = stateLastMonth.reportsAnalytics.categoryBreakdown
        assertEquals(2, breakdownLastMonth.size)
        assertEquals("Makanan", breakdownLastMonth[0].category)
        assertEquals(1500000.0, breakdownLastMonth[0].totalAmount, 0.001)
        assertEquals(75, breakdownLastMonth[0].percentage)
        assertEquals("#EF4444", breakdownLastMonth[0].color)

        assertEquals("Transportasi", breakdownLastMonth[1].category)
        assertEquals(500000.0, breakdownLastMonth[1].totalAmount, 0.001)
        assertEquals(25, breakdownLastMonth[1].percentage)
        assertEquals("#3B82F6", breakdownLastMonth[1].color)

        // Pastikan TIDAK ADA "Belanja" (yang merupakan pengeluaran bulan ini)
        assertFalse(breakdownLastMonth.any { it.category == "Belanja" })

        val originalReports = stateLastMonth.reportsAnalytics

        // PENEGASAN 2: Saat pencarian daftar transaksi aktif mencari keyword tertentu, data laporan tetap utuh sesuai preset LAST_MONTH
        viewModel.setSearchQuery("Belanja")
        val stateSearch = viewModel.uiState.filter { it.searchQuery == "Belanja" && it.filteredTransactions.isNotEmpty() }.first()

        // Daftar transaksi terfilter oleh keyword "Belanja"
        assertTrue(stateSearch.filteredTransactions.all { it.title.contains("Belanja", ignoreCase = true) || it.category.contains("Belanja", ignoreCase = true) })

        // Data laporan reportsAnalytics TETAP UTUH sesuai preset LAST_MONTH
        assertEquals(originalReports.totalIncome, stateSearch.reportsAnalytics.totalIncome, 0.001)
        assertEquals(originalReports.totalExpense, stateSearch.reportsAnalytics.totalExpense, 0.001)
        assertEquals(originalReports.netSavings, stateSearch.reportsAnalytics.netSavings, 0.001)
        assertEquals(originalReports.categoryBreakdown.size, stateSearch.reportsAnalytics.categoryBreakdown.size)
        assertEquals("Makanan", stateSearch.reportsAnalytics.categoryBreakdown[0].category)
        assertEquals("Transportasi", stateSearch.reportsAnalytics.categoryBreakdown[1].category)

        // Reset search query
        viewModel.setSearchQuery("")
        val stateSearchReset = viewModel.uiState.filter { it.searchQuery.isEmpty() }.first()

        // PENEGASAN 3: Saat pagination daftar dinaikkan dari 50 -> 100 -> 120+, seluruh isi reportsAnalytics identik dan tidak bergeser sama sekali
        // Pagination tahap 1: 50 transaksi
        assertEquals(50, stateSearchReset.filteredTransactions.size)
        assertEquals(originalReports, stateSearchReset.reportsAnalytics)

        // Pagination tahap 2: naikkan ke 100
        viewModel.loadMoreTransactions()
        val statePage100 = viewModel.uiState.filter { it.visibleTransactionCount == 100 && it.filteredTransactions.size == 100 }.first()
        assertEquals(100, statePage100.filteredTransactions.size)
        assertEquals(originalReports.totalIncome, statePage100.reportsAnalytics.totalIncome, 0.001)
        assertEquals(originalReports.totalExpense, statePage100.reportsAnalytics.totalExpense, 0.001)
        assertEquals(originalReports.netSavings, statePage100.reportsAnalytics.netSavings, 0.001)
        assertEquals(originalReports.categoryBreakdown, statePage100.reportsAnalytics.categoryBreakdown)
        assertEquals(originalReports.cashflowBars, statePage100.reportsAnalytics.cashflowBars)

        // Pagination tahap 3: naikkan ke 150 (memuat 123 transaksi)
        viewModel.loadMoreTransactions()
        val statePage123 = viewModel.uiState.filter { it.visibleTransactionCount == 150 && it.filteredTransactions.size == 123 }.first()
        assertEquals(123, statePage123.filteredTransactions.size)
        assertEquals(originalReports.totalIncome, statePage123.reportsAnalytics.totalIncome, 0.001)
        assertEquals(originalReports.totalExpense, statePage123.reportsAnalytics.totalExpense, 0.001)
        assertEquals(originalReports.netSavings, statePage123.reportsAnalytics.netSavings, 0.001)
        assertEquals(originalReports.categoryBreakdown, statePage123.reportsAnalytics.categoryBreakdown)
        assertEquals(originalReports.cashflowBars, statePage123.reportsAnalytics.cashflowBars)
    }

    @Test
    fun testBudgetProgressAndAlertWithDatabaseLevelAggregationAndZeroMemoryLeak() = runBlocking {
        // 1. Tambahkan 2 anggaran untuk periode "2026-10"
        repository.addBudget(
            name = "Anggaran Makanan",
            category = "Makanan",
            limitAmount = 2000000.0,
            period = "2026-10"
        )
        repository.addBudget(
            name = "Anggaran Transportasi",
            category = "Transportasi",
            limitAmount = 1000000.0,
            period = "2026-10"
        )

        // 2. Masukkan transaksi spesifik di bulan "2026-10"
        // Makanan: 800.000 + 900.000 = 1.700.000 (85% -> WARNING)
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Belanja Mingguan",
                amount = 800000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-10-02"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Restoran Makan Siang",
                amount = 900000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-10-10"
            )
        )
        // Reimbursement Makanan (INCOME) -> TIDAK boleh terhitung sebagai expense/spent
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Reimburse Kantor",
                amount = 500000.0,
                type = "INCOME",
                category = "Makanan",
                date = "2026-10-12"
            )
        )

        // Transportasi: 400.000 (40% -> SAFE)
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Bensin Mobil",
                amount = 400000.0,
                type = "EXPENSE",
                category = "Transportasi",
                date = "2026-10-05"
            )
        )

        // Kategori lain (Tagihan): 300.000 -> Tidak boleh mempengaruhi budget Makanan & Transportasi
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Listrik PLN",
                amount = 300000.0,
                type = "EXPENSE",
                category = "Tagihan",
                date = "2026-10-08"
            )
        )

        // Transaksi di bulan lain: "2026-09" dan "2026-11" -> TIDAK boleh terhitung di periode "2026-10"
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Makan Bulan Lalu",
                amount = 1500000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-09-25"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                id = 0,
                title = "Transport Bulan Depan",
                amount = 600000.0,
                type = "EXPENSE",
                category = "Transportasi",
                date = "2026-11-01"
            )
        )

        // 3. Masukkan 120+ transaksi dummy untuk mensimulasikan beban data riil
        for (i in 1..125) {
            val dummyType = if (i % 3 == 0) "INCOME" else "EXPENSE"
            repository.insertTransaction(
                TransactionEntity(
                    id = 0,
                    title = "Dummy Transaction #$i",
                    amount = 10000.0 * (i % 10 + 1),
                    type = dummyType,
                    category = "Hiburan",
                    date = "2026-10-15"
                )
            )
        }

        // Total transaksi di database minimal 132 transaksi
        val totalTxInDb = appDb.transactionDao.getTransactionCount()
        assertTrue("Total transaksi di database harus > 130", totalTxInDb >= 132)

        // PENEGASAN MEMORI: Penambahan 120+ transaksi TIDAK memuat seluruh row transaksi ke cache _transactionsFlow
        val inMemorySnapshot = appDb.getTransactionsFlowMemoryCacheSnapshot()
        assertEquals(
            "Cache _transactionsFlow harus tetap kosong untuk mencegah memory leak / RAM bloat",
            0,
            inMemorySnapshot.size
        )

        // PENEGASAN AGREGASI SQLITE: Verifikasi getBudgetProgress menghitung nominal terpakai (spent) presisi
        val progressList = repository.getBudgetProgress("2026-10").first()
        assertEquals(2, progressList.size)

        val makananProgress = progressList.find { it.budget.category == "Makanan" }
        assertNotNull("Budget Makanan harus ditemukan", makananProgress)
        assertEquals(1700000.0, makananProgress!!.spentAmount, 0.001)
        assertEquals(85, makananProgress.percentage)
        assertEquals("WARNING", makananProgress.statusLevel)
        assertFalse(makananProgress.isOver)

        val transportProgress = progressList.find { it.budget.category == "Transportasi" }
        assertNotNull("Budget Transportasi harus ditemukan", transportProgress)
        assertEquals(400000.0, transportProgress!!.spentAmount, 0.001)
        assertEquals(40, transportProgress.percentage)
        assertEquals("SAFE", transportProgress.statusLevel)
        assertFalse(transportProgress.isOver)

        // PENEGASAN QUERY SPESIFIK & OVERBUDGET ALERT:
        // Tambahkan pengeluaran Makanan sebesar 400.000 -> Total menjadi 2.100.000 (105% dari limit 2.000.000)
        val overBudgetTx = TransactionEntity(
            id = 0,
            title = "Makan Malam Mewah",
            amount = 400000.0,
            type = "EXPENSE",
            category = "Makanan",
            date = "2026-10-20"
        )
        repository.insertTransaction(overBudgetTx)

        // Query database langsung via getCategorySpentForPeriod
        val updatedMakananSpent = repository.getCategorySpentForPeriod("Makanan", "2026-10")
        assertEquals(2100000.0, updatedMakananSpent, 0.001)

        // Verifikasi getBudgetProgress kini menandai CRITICAL & isOver = true
        val updatedProgressList = repository.getBudgetProgress("2026-10").first()
        val updatedMakananProgress = updatedProgressList.find { it.budget.category == "Makanan" }!!
        assertEquals(2100000.0, updatedMakananProgress.spentAmount, 0.001)
        assertEquals(105, updatedMakananProgress.percentage)
        assertEquals("CRITICAL", updatedMakananProgress.statusLevel)
        assertTrue(updatedMakananProgress.isOver)

        // Verifikasi notifikasi alert telah dibuat di database
        val notifications = repository.getAllNotifications().first()
        val alertNotif = notifications.find { it.message.contains("Makanan") && it.type == "DANGER" }
        assertNotNull("Notifikasi overbudget kategori Makanan harus terpicu", alertNotif)
        assertTrue(alertNotif!!.title.contains("ANGGARAN TERLAMPAUI"))
    }

    /**
     * Memverifikasi filter periode SQL untuk AI Insight dieksekusi di basis data sebelum LIMIT 100,
     * serta menguji determinisme Clock injection pada FinanceViewModel.
     *
     * Skenario:
     * 1. Bersihkan transaksi lama.
     * 2. Sisipkan 115 transaksi pada bulan berjalan (2026-10).
     * 3. Sisipkan 10 transaksi pada bulan lalu (2026-09) dengan kategori dan nilai khusus.
     * 4. Buktikan bahwa getTransactionsPaged(limit = 100) HANYA mengembalikan data 2026-10
     *    (sehingga filter in-memory naif akan menghasilkan list kosong untuk 2026-09).
     * 5. Buktikan bahwa getTransactionsForPeriod("2026-09-01", "2026-09-30", limit = 100)
     *    berhasil mengembalikan tepat 10 transaksi bulan September 2026 via SQL filter.
     * 6. Verifikasi via FinanceViewModel dengan fixedClock (2026-10-15) bahwa:
     *    - Pemilihan preset LAST_MONTH secara deterministik menargetkan rentang 2026-09-01 s/d 2026-09-30.
     *    - fetchAiInsight() mengeksekusi getTransactionsForPeriod dan menyertakan agregasi SQL utuh
     *      sehingga teks insight memuat ringkasan transaksi bulan lalu dan tidak kosong/error.
     */
    @Test
    fun testAiInsightSqlPeriodFilterBeforeLimit100AndDeterministicClock() = runBlocking {
        // Bersihkan transaksi lama
        val existing = repository.getAllTransactions().first()
        for (tx in existing) {
            repository.deleteTransaction(tx.id)
        }

        // Siapkan akun
        val accounts = repository.getAllAccounts().first()
        val accountId = if (accounts.isNotEmpty()) accounts[0].id else {
            appDb.accountDao.insertAccount(
                com.sena.financetracker.data.AccountEntity(
                    name = "Dompet Utama",
                    type = "BANK",
                    balance = 10000000.0
                )
            )
        }

        // Siapkan 10 transaksi bulan lalu (September 2026)
        repository.insertTransaction(
            TransactionEntity(
                title = "Gaji September",
                amount = 5000000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-09-05",
                accountId = accountId
            )
        )
        for (i in 1..9) {
            repository.insertTransaction(
                TransactionEntity(
                    title = "Belanja September #$i",
                    amount = 150000.0,
                    type = "EXPENSE",
                    category = "Kebutuhan",
                    date = "2026-09-10",
                    accountId = accountId
                )
            )
        }

        // Siapkan 115 transaksi bulan ini (Oktober 2026)
        for (i in 1..115) {
            repository.insertTransaction(
                TransactionEntity(
                    title = "Transaksi Oktober #$i",
                    amount = 50000.0,
                    type = "EXPENSE",
                    category = "Harian",
                    date = "2026-10-12",
                    accountId = accountId
                )
            )
        }

        val allTotal = repository.getAllTransactions().first()
        assertEquals(125, allTotal.size)

        // 1. Buktikan limit 100 biasa tertutup oleh transaksi Oktober 2026
        val pagedTransactions = repository.getTransactionsPaged(limit = 100, offset = 0)
        assertEquals(100, pagedTransactions.size)
        // Seluruh 100 transaksi paged teratas berasal dari Oktober 2026
        assertTrue(pagedTransactions.all { it.date.startsWith("2026-10") })
        // Tidak ada transaksi September di 100 item teratas
        assertFalse(pagedTransactions.any { it.date.startsWith("2026-09") })

        // 2. Buktikan getTransactionsForPeriod menyaring di level SQL SEBELUM limit 100
        val septemberTransactions = repository.getTransactionsForPeriod(
            startDate = "2026-09-01",
            endDate = "2026-09-30",
            limit = 100
        )
        assertEquals(10, septemberTransactions.size)
        assertTrue(septemberTransactions.all { it.date.startsWith("2026-09") })
        assertEquals("Belanja September #9", septemberTransactions[0].title)

        // 3. Verifikasi agregasi ringkasan SQL untuk periode September
        val septemberSummary = repository.getFinanceSummary(
            startDate = "2026-09-01",
            endDate = "2026-09-30"
        )
        assertEquals(5000000.0, septemberSummary.totalIncome, 0.001)
        assertEquals(9 * 150000.0, septemberSummary.totalExpense, 0.001)

        // 4. Verifikasi FinanceViewModel dengan fixedClock
        val viewModel = FinanceViewModel(
            repository = repository,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined),
            clock = fixedClock
        )

        // Set preset ke LAST_MONTH (yang via fixedClock 2026-10-15 pasti menyelesaikan 2026-09)
        viewModel.setReportsPeriodPreset("LAST_MONTH")
        val state = viewModel.uiState.filter { it.reportsAnalytics.periodPreset == "LAST_MONTH" }.first()
        assertEquals(5000000.0, state.reportsAnalytics.totalIncome, 0.001)
        assertEquals(1350000.0, state.reportsAnalytics.totalExpense, 0.001)

        // Panggil fetchAiInsight tanpa parameter (harus otomatis mengambil periode preset LAST_MONTH)
        val requestId = viewModel.fetchAiInsight()

        // Tunggu hingga proses insight selesai menggunakan korelasi requestId
        val insightState = viewModel.uiState.first {
            it.completedInsightRequestId == requestId && !it.isAiInsightLoading
        }
        assertEquals(requestId, insightState.completedInsightRequestId)
        assertNull(insightState.aiInsightError)
        assertNotNull(insightState.aiInsightText)
        assertTrue(insightState.aiInsightText!!.isNotBlank())
        // Karena apiKey kosong di unit test, fallback lokal terpanggil dan memuat angka kas periode September
        assertTrue(
            insightState.aiInsightText!!.contains("Bulan Lalu") ||
            insightState.aiInsightText!!.contains("Rp")
        )
    }

    @Test
    fun testAiInsightPeriodAwareBudgetProgressAndMultiMonthHandling() = runBlocking {
        // Bersihkan data lama agar state deterministik
        val existingTxs = repository.getAllTransactions().first()
        for (tx in existingTxs) {
            repository.deleteTransaction(tx.id)
        }
        val existingBudgets = appDb.budgetDao.getAllBudgets().first()
        for (b in existingBudgets) {
            appDb.budgetDao.deleteBudget(b.id)
        }

        val accounts = repository.getAllAccounts().first()
        val accountId = if (accounts.isNotEmpty()) accounts[0].id else {
            appDb.accountDao.insertAccount(
                AccountEntity(
                    name = "BCA Utama",
                    type = "BANK",
                    balance = 10_000_000.0
                )
            )
        }

        // 1. Setup Anggaran Bulanan
        // September (2026-09): Limit Rp 1.000.000
        appDb.budgetDao.insertBudget(
            BudgetEntity(
                category = "Makanan",
                name = "Makanan September",
                limitAmount = 1_000_000.0,
                period = "2026-09"
            )
        )

        // Oktober (2026-10): Limit Rp 1.000.000
        appDb.budgetDao.insertBudget(
            BudgetEntity(
                category = "Makanan",
                name = "Makanan Oktober",
                limitAmount = 1_000_000.0,
                period = "2026-10"
            )
        )

        // 2. Setup Transaksi
        // September: Pemasukan 3.000.000, Pengeluaran Makanan 300.000 (30% -> AMAN / SAFE)
        repository.insertTransaction(
            TransactionEntity(
                title = "Gaji September",
                amount = 3_000_000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-09-05",
                accountId = accountId
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Makan Siang September",
                amount = 300_000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-09-10",
                accountId = accountId
            )
        )

        // Oktober: Pemasukan 3.000.000, Pengeluaran Makanan 1.500.000 (150% -> JEBOL / OVER)
        repository.insertTransaction(
            TransactionEntity(
                title = "Gaji Oktober",
                amount = 3_000_000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-10-05",
                accountId = accountId
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Makan Mewah Oktober",
                amount = 1_500_000.0,
                type = "EXPENSE",
                category = "Makanan",
                date = "2026-10-10",
                accountId = accountId
            )
        )

        // 3. Verifikasi Budget Progress langsung dari Repository
        val sepProgress = repository.getBudgetProgress("2026-09").first()
        assertEquals(1, sepProgress.size)
        assertEquals(300_000.0, sepProgress[0].spentAmount, 0.001)
        assertEquals(30, sepProgress[0].percentage)
        assertFalse(sepProgress[0].isOver)

        val octProgress = repository.getBudgetProgress("2026-10").first()
        assertEquals(1, octProgress.size)
        assertEquals(1_500_000.0, octProgress[0].spentAmount, 0.001)
        assertEquals(150, octProgress[0].percentage)
        assertTrue(octProgress[0].isOver)

        // 4. Inisialisasi ViewModel dengan fixedClock (2026-10-15)
        val viewModel = FinanceViewModel(
            repository = repository,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined),
            clock = fixedClock
        )

        // 5. Kasus A: Minta AI Insight untuk Periode September (LAST_MONTH)
        viewModel.setReportsPeriodPreset("LAST_MONTH")
        val reqIdSep = viewModel.fetchAiInsight()

        val sepInsightState = viewModel.uiState.first {
            it.completedInsightRequestId == reqIdSep && !it.isAiInsightLoading
        }
        val sepInsight = sepInsightState.aiInsightText!!
        // Insight September TIDAK BOLEH mengklaim anggaran Makanan jebol
        assertFalse(
            "AI Insight September tidak boleh mengklaim anggaran jebol karena realisasi hanya 30%",
            sepInsight.contains("jebol") || sepInsight.contains("150%")
        )

        // 6. Kasus B: Minta AI Insight untuk Periode Oktober (THIS_MONTH)
        viewModel.setReportsPeriodPreset("THIS_MONTH")
        val reqIdOct = viewModel.fetchAiInsight()

        val octInsightState = viewModel.uiState.first {
            it.completedInsightRequestId == reqIdOct && !it.isAiInsightLoading
        }
        val octInsight = octInsightState.aiInsightText!!
        // Insight Oktober WAJIB mendeteksi anggaran Makanan jebol
        assertTrue(
            "AI Insight Oktober wajib mendeteksi anggaran Makanan jebol nyentuh 150%",
            octInsight.contains("jebol") && octInsight.contains("Makanan")
        )

        // 7. Kasus C: Minta AI Insight untuk Periode Multi-Bulan (LAST_3_MONTHS)
        viewModel.setReportsPeriodPreset("LAST_3_MONTHS")
        val reqIdMulti = viewModel.fetchAiInsight()

        val multiInsightState = viewModel.uiState.first {
            it.completedInsightRequestId == reqIdMulti && !it.isAiInsightLoading
        }
        val multiInsight = multiInsightState.aiInsightText!!
        // AI Insight untuk multi-bulan TIDAK BOLEH mengklaim anggaran jebol ataupun aman,
        // melainkan wajib menyatakan evaluasi batas anggaran bulanan tidak dihitung
        assertTrue(
            "AI Insight rentang multi-bulan wajib menyatakan evaluasi batas anggaran bulanan tidak dihitung",
            multiInsight.contains("evaluasi batas anggaran bulanan tidak dihitung untuk rentang multi-bulan")
        )
        assertFalse(
            "AI Insight multi-bulan dilarang mengklaim anggaran jebol berantakan",
            multiInsight.contains("Anggaran kamu jebol berantakan")
        )
    }

    @Test
    fun testAiInsightStaleRequestDiscardedOnConcurrentRequests() = runTest(timeout = kotlin.time.Duration.parse("5s")) {
        // 1. Bersihkan data lama agar state terisolasi
        val existingTxs = repository.getAllTransactions().first()
        for (tx in existingTxs) {
            repository.deleteTransaction(tx.id)
        }

        val accounts = repository.getAllAccounts().first()
        val accountId = if (accounts.isNotEmpty()) accounts[0].id else {
            appDb.accountDao.insertAccount(
                AccountEntity(
                    name = "BCA Utama",
                    type = "BANK",
                    balance = 10_000_000.0
                )
            )
        }

        // Siapkan transaksi September 2026 (Request 1 / Lambat)
        repository.insertTransaction(
            TransactionEntity(
                title = "Gaji September",
                amount = 2_000_000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-09-01",
                accountId = accountId
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Belanja September Unik",
                amount = 250_000.0,
                type = "EXPENSE",
                category = "Belanja",
                date = "2026-09-15",
                accountId = accountId
            )
        )

        // Siapkan transaksi Oktober 2026 (Request 2 / Cepat)
        repository.insertTransaction(
            TransactionEntity(
                title = "Gaji Oktober",
                amount = 7_000_000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-10-01",
                accountId = accountId
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Liburan Oktober Unik",
                amount = 1_500_000.0,
                type = "EXPENSE",
                category = "Hiburan",
                date = "2026-10-10",
                accountId = accountId
            )
        )

        // 2. Gunakan CompletableDeferred dan TestDispatcher untuk mengontrol eksekusi secara deterministik
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val req1Started = CompletableDeferred<Unit>()
        val pauseRequest1 = CompletableDeferred<Unit>()

        val slowTransactionDao = object : TransactionDao by appDb.transactionDao {
            override suspend fun getTransactionsForPeriod(
                startDate: String?,
                endDate: String?,
                limit: Int
            ): List<TransactionEntity> {
                if (startDate?.startsWith("2026-09") == true) {
                    req1Started.complete(Unit)
                    pauseRequest1.await()
                }
                return appDb.transactionDao.getTransactionsForPeriod(startDate, endDate, limit)
            }
        }

        val customRepository = TransactionRepository(
            transactionDao = slowTransactionDao,
            accountDao = appDb.accountDao,
            categoryDao = appDb.categoryDao,
            budgetDao = appDb.budgetDao,
            notificationDao = appDb.notificationDao
        )

        val viewModel = FinanceViewModel(
            repository = customRepository,
            scopeOverride = CoroutineScope(Dispatchers.Default),
            clock = fixedClock,
            ioDispatcher = testDispatcher
        )

        val req1CompletedDeferred = CompletableDeferred<Unit>()
        var targetReq1Id: String? = null
        viewModel.onAiInsightCompletedForTest = { completedId ->
            if (completedId == targetReq1Id) {
                req1CompletedDeferred.complete(Unit)
            }
        }

        // 3. Luncurkan Request 1 (September) yang tertahan
        val req1Id = viewModel.fetchAiInsight("2026-09-01", "2026-09-30")
        targetReq1Id = req1Id

        // Majukan test scheduler agar Request 1 aktif dan mencapai gerbang penahanan
        testScheduler.advanceUntilIdle()
        req1Started.await()

        // 4. Luncurkan Request 2 (Oktober) dan jalankan hingga tuntas
        val req2Id = viewModel.fetchAiInsight("2026-10-01", "2026-10-31")
        testScheduler.advanceUntilIdle()

        // 5. Verifikasi bahwa Request 2 selesai lebih dahulu dan terkonfirmasi via completedInsightRequestId
        val req2CompletedState = viewModel.uiState.first {
            it.completedInsightRequestId == req2Id && !it.isAiInsightLoading
        }
        assertEquals(req2Id, req2CompletedState.completedInsightRequestId)
        val octInsightText = req2CompletedState.aiInsightText
        assertNotNull(octInsightText)
        assertTrue(
            "Teks insight wajib merefleksikan transaksi Oktober",
            octInsightText!!.contains("Hiburan") || octInsightText.contains("Liburan") || octInsightText.contains("Oktober") || octInsightText.contains("Rp")
        )

        // 6. Lepaskan Request 1 dan tunggu seluruh coroutine Request 1 selesai secara tuntas
        pauseRequest1.complete(Unit)
        testScheduler.advanceUntilIdle()
        req1CompletedDeferred.await()

        // 7. Buktikan bahwa hasil Request 1 yang stale dibuang dan TIDAK menimpa hasil Request 2
        val finalState = viewModel.uiState.value
        assertEquals("RequestId akhir wajib tetap merupakan Request 2", req2Id, finalState.completedInsightRequestId)
        assertEquals("Teks insight akhir tidak boleh tertimpa oleh Request 1", octInsightText, finalState.aiInsightText)
        assertFalse("State loading wajib tetap false", finalState.isAiInsightLoading)
    }
}
