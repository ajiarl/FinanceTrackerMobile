package com.sena.financetracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.FinanceSummary
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.PagedTransactionsResult
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.repository.TransactionRepository
import com.sena.financetracker.util.CsvImporter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Clock
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ViewModel utama untuk mengelola state dan alur bisnis Finance Tracker Mobile.
 *
 * Menggabungkan berbagai sumber data reaktif (Transactions, Accounts, Categories, Budgets)
 * dengan parameter penyaringan interaktif (Search Query, Category Filter, Date Range Filter, Type Tabs)
 * menggunakan operator [combine] Kotlin Coroutines Flow untuk memancarkan [FinanceUiState] yang konsisten.
 *
 * Menjamin error handling yang aman pada setiap operasi coroutine di [viewModelScope] tanpa membuat aplikasi crash.
 */
class FinanceViewModel(
    private val repository: TransactionRepository,
    private val scopeOverride: CoroutineScope? = null,
    private val context: Context? = null,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val activeScope: CoroutineScope = scopeOverride ?: viewModelScope

    private val _uiState = MutableStateFlow(FinanceUiState(isLoading = true))

    /**
     * StateFlow publik yang diobservasi oleh composable UI layer.
     */
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private val _hasApiKey = MutableStateFlow(false)

    /**
     * StateFlow publik yang mengindikasikan apakah Groq API Key tersimpan dan aktif.
     */
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    /**
     * StateFlow daftar notifikasi sistem aplikasi.
     */
    val notifications: StateFlow<List<NotificationEntity>> = repository.getAllNotifications()
        .stateIn(
            scope = activeScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * StateFlow jumlah notifikasi yang belum dibaca.
     */
    val unreadNotificationCount: StateFlow<Int> = repository.getUnreadNotificationCount()
        .stateIn(
            scope = activeScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _selectedDateFilter = MutableStateFlow("ALL")
    private val _selectedFilterTab = MutableStateFlow("ALL")
    private val _selectedReportsPreset = MutableStateFlow(ReportsPreset.THIS_MONTH)
    val selectedReportsPreset: StateFlow<ReportsPreset> = _selectedReportsPreset.asStateFlow()
    private val _aiInsightText = MutableStateFlow<String?>(null)
    private val _isAiInsightLoading = MutableStateFlow(false)
    private val _aiInsightError = MutableStateFlow<String?>(null)
    private val _completedInsightRequestId = MutableStateFlow<String?>(null)

    @Volatile
    private var activeInsightRequestId: String? = null

    private val _pageSize = MutableStateFlow(50)
    private val _visibleTransactionCount = MutableStateFlow(50)

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val friendlyMsg = throwable.message?.takeIf { it.isNotBlank() }
            ?: "Terjadi kesalahan internal pada operasi data"
        _uiState.value = _uiState.value.copy(
            errorMessage = friendlyMsg,
            isLoading = false
        )
    }

    /**
     * Membersihkan pesan galat/error banner dari UI.
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    init {
        refreshApiKeyStatus()
        observeData()
    }

    private data class AiInsightParams(
        val text: String?,
        val loading: Boolean,
        val error: String?,
        val completedRequestId: String?
    )

    private data class FilterParams(
        val query: String,
        val category: String?,
        val dateFilter: String,
        val typeFilter: String,
        val aiInsightText: String?,
        val isAiInsightLoading: Boolean,
        val aiInsightError: String?,
        val completedInsightRequestId: String?,
        val pageSize: Int,
        val visibleTransactionCount: Int,
        val hasApiKey: Boolean
    )

    private data class CoreData(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val budgets: List<BudgetProgressItem>
    )

    private data class PagedAndSummary(
        val summary: FinanceSummary,
        val pagedResult: PagedTransactionsResult,
        val filter: FilterParams
    )

    private data class NotifData(
        val notifications: List<NotificationEntity>,
        val unreadNotificationCount: Int
    )

    private fun resolveDateRange(dateFilter: String): Pair<String?, String?> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(clock.millis()))
        return when (dateFilter.uppercase(Locale.ROOT)) {
            "TODAY" -> Pair(today, today)
            "THIS_MONTH" -> {
                val cal = Calendar.getInstance().apply { time = Date(clock.millis()) }
                val year = cal.get(Calendar.YEAR)
                val month = String.format(Locale.ROOT, "%02d", cal.get(Calendar.MONTH) + 1)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                Pair("$year-$month-01", "$year-$month-$maxDay")
            }
            else -> Pair(null, null)
        }
    }

    /**
     * Mengobservasi dan menggabungkan aliran basis data Room/SQLite bersama parameter filter.
     *
     * Alur:
     * 1. Menggabungkan StateFlow filter menjadi aliran [FilterParams].
     * 2. Menjalankan query agregasi SQL dan transaksi terpaginasi langsung di basis data SQLite (zero RAM overhead).
     * 3. Menggabungkan hasil agregasi dan paginasi bersama data akun, kategori, dan notifikasi ke [FinanceUiState].
     */
    @OptIn(FlowPreview::class)
    private fun observeData() {
        val debouncedSearchQuery = if (scopeOverride != null) {
            _searchQuery
        } else {
            _searchQuery.debounce { query ->
                if (query.isEmpty()) 0L else 300L
            }
        }

        val pagingFlow = combine(_pageSize, _visibleTransactionCount, _hasApiKey) { size, count, hasKey -> Triple(size, count, hasKey) }
        val aiInsightFlow = combine(
            _aiInsightText,
            _isAiInsightLoading,
            _aiInsightError,
            _completedInsightRequestId
        ) { text, loading, err, reqId ->
            AiInsightParams(text, loading, err, reqId)
        }
        val filterParamsFlow = combine(
            combine(debouncedSearchQuery, _selectedCategoryFilter, _selectedDateFilter) { q, c, d -> Triple(q, c, d) },
            _selectedFilterTab,
            aiInsightFlow,
            pagingFlow
        ) { (query, category, dateFilter), typeFilter, aiState, (pageSize, visibleCount, hasKey) ->
            FilterParams(
                query = query,
                category = category,
                dateFilter = dateFilter,
                typeFilter = typeFilter,
                aiInsightText = aiState.text,
                isAiInsightLoading = aiState.loading,
                aiInsightError = aiState.error,
                completedInsightRequestId = aiState.completedRequestId,
                pageSize = pageSize,
                visibleTransactionCount = visibleCount,
                hasApiKey = hasKey
            )
        }

        val pagedTransactionsAndSummaryFlow: Flow<PagedAndSummary> = combine(
            filterParamsFlow,
            repository.getTransactionUpdateTrigger()
        ) { filter: FilterParams, _: Long ->
            val (startDate, endDate) = resolveDateRange(filter.dateFilter)
            val cleanQuery = filter.query.trim().ifEmpty { null }
            val cleanCategory = if (filter.category.isNullOrBlank() || filter.category.trim().equals("ALL", ignoreCase = true)) null else filter.category.trim()
            val cleanType = when (filter.typeFilter.trim().uppercase(Locale.ROOT)) {
                "EXPENSE" -> "EXPENSE"
                "INCOME" -> "INCOME"
                else -> null
            }

            val summary = repository.getFinanceSummary(
                query = cleanQuery,
                category = cleanCategory,
                startDate = startDate,
                endDate = endDate
            )

            val pagedResult = repository.getFilteredTransactionsPaged(
                query = cleanQuery,
                type = cleanType,
                category = cleanCategory,
                startDate = startDate,
                endDate = endDate,
                limit = filter.visibleTransactionCount,
                offset = 0
            )

            PagedAndSummary(summary, pagedResult, filter)
        }

        val reportsFlow: Flow<ReportsAnalyticsState> = combine(
            _selectedReportsPreset,
            repository.getTransactionUpdateTrigger()
        ) { preset, _ ->
            val (start, end) = resolveReportsDateRange(preset, Date(clock.millis()))
            repository.getReportsAnalytics(start, end).copy(periodPreset = preset.name)
        }

        val coreDataFlow: Flow<CoreData> = combine(
            repository.getAllAccounts(),
            repository.getAllCategories(),
            repository.getBudgetProgress(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(clock.millis())))
        ) { accs, cats, budgets ->
            CoreData(accs, cats, budgets)
        }

        val notifDataFlow: Flow<NotifData> = combine(
            repository.getAllNotifications(),
            repository.getUnreadNotificationCount()
        ) { notifs, unreadCount ->
            NotifData(notifs, unreadCount)
        }

        activeScope.launch(coroutineExceptionHandler) {
            combine(
                pagedTransactionsAndSummaryFlow,
                reportsFlow,
                coreDataFlow,
                notifDataFlow,
                repository.isHapticEnabled()
            ) { pagedSummary: PagedAndSummary, reports: ReportsAnalyticsState, core: CoreData, notif: NotifData, haptic: Boolean ->
                val summary = pagedSummary.summary
                val pagedResult = pagedSummary.pagedResult
                val filter = pagedSummary.filter
                val accounts = core.accounts
                val categories = core.categories
                val budgets = core.budgets

                val totalIncome = summary.totalIncome
                val totalExpense = summary.totalExpense
                val totalBalance = if (accounts.isNotEmpty()) {
                    accounts.sumOf { it.balance }
                } else {
                    totalIncome - totalExpense
                }

                FinanceUiState(
                    transactions = pagedResult.transactions,
                    filteredTransactions = pagedResult.transactions,
                    accounts = accounts,
                    categories = categories,
                    budgets = budgets,
                    notifications = notif.notifications,
                    unreadNotificationCount = notif.unreadNotificationCount,
                    totalBalance = totalBalance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    isLoading = false,
                    errorMessage = null,
                    searchQuery = filter.query,
                    selectedCategoryFilter = filter.category,
                    selectedDateFilter = filter.dateFilter,
                    selectedFilterTab = filter.typeFilter,
                    reportsAnalytics = reports,
                    aiInsightText = filter.aiInsightText,
                    isAiInsightLoading = filter.isAiInsightLoading,
                    aiInsightError = filter.aiInsightError,
                    completedInsightRequestId = filter.completedInsightRequestId,
                    hasApiKey = filter.hasApiKey,
                    isHapticEnabled = haptic,
                    pageSize = filter.pageSize,
                    visibleTransactionCount = filter.visibleTransactionCount,
                    hasMoreTransactions = pagedResult.hasMore
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    /**
     * Memuat lebih banyak transaksi ke tampilan UI (progressive pagination).
     */
    fun loadMoreTransactions() {
        val currentVisible = _visibleTransactionCount.value
        val pageSize = _pageSize.value
        _visibleTransactionCount.value = currentVisible + pageSize
    }

    /**
     * Mengatur ukuran halaman transaksi (pageSize).
     */
    fun setPageSize(size: Int) {
        if (size > 0) {
            _pageSize.value = size
        }
    }

    /**
     * Mengatur preferensi getaran / sensasi taktil Neobrutal (on/off).
     */
    fun setHapticEnabled(enabled: Boolean) {
        repository.setHapticEnabled(enabled)
    }

    /**
     * Memperbarui filter preset waktu analitik laporan ("THIS_MONTH", "LAST_MONTH", "LAST_3_MONTHS", "ALL_TIME").
     */
    fun setReportsPeriodPreset(preset: String) {
        _selectedReportsPreset.value = ReportsPreset.fromString(preset)
    }

    /**
     * Memperbarui filter preset waktu analitik laporan dengan enum [ReportsPreset].
     */
    fun setReportsPeriodPreset(preset: ReportsPreset) {
        _selectedReportsPreset.value = preset
    }

    /**
     * Memperbarui filter preset waktu analitik laporan dengan enum [ReportsPreset].
     */
    fun setReportsPreset(preset: ReportsPreset) {
        _selectedReportsPreset.value = preset
    }

    /**
     * Memperbarui kata kunci pencarian teks pada judul, kategori, dan catatan transaksi.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Memperbarui filter kategori aktif (atau null untuk semua kategori).
     *
     * Mitigasi DAT-07: Melakukan normalisasi whitespace dengan memotong spasi depan/belakang.
     */
    fun setSelectedCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category?.trim()
    }

    // ── NOTIFICATIONS ACTIONS ──────────────────────────────────────────────────
    /**
     * Menandai notifikasi sebagai sudah dibaca berdasarkan ID.
     */
    fun markNotificationAsRead(id: Long) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.markNotificationAsRead(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Gagal menandai notifikasi: ${e.message}")
            }
        }
    }

    /**
     * Menandai seluruh notifikasi yang ada sebagai sudah dibaca.
     */
    fun markAllNotificationsAsRead() {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.markAllNotificationsAsRead()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Gagal menandai semua notifikasi: ${e.message}")
            }
        }
    }

    /**
     * Menghapus seluruh riwayat notifikasi.
     */
    fun clearAllNotifications() {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.clearAllNotifications()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Gagal membersihkan notifikasi: ${e.message}")
            }
        }
    }

    /**
     * Menghapus satu notifikasi berdasarkan ID.
     */
    fun deleteNotification(id: Long) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.deleteNotification(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Gagal menghapus notifikasi: ${e.message}")
            }
        }
    }

    /**
     * Memperbarui filter rentang tanggal ("ALL", "TODAY", atau "THIS_MONTH").
     */
    fun setSelectedDateFilter(dateFilter: String) {
        _selectedDateFilter.value = dateFilter
    }

    /**
     * Memperbarui tab jenis transaksi ("ALL", "EXPENSE", atau "INCOME").
     */
    fun setSelectedFilterTab(tab: String) {
        _selectedFilterTab.value = tab
    }

    /**
     * Mengatur ulang seluruh filter dan pencarian transaksi ke keadaan awal.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _selectedDateFilter.value = "ALL"
        _selectedFilterTab.value = "ALL"
        _visibleTransactionCount.value = _pageSize.value
    }

    /**
     * Mengosongkan seluruh riwayat database transaksi dan mereset filter.
     */
    fun resetTransactions() {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.resetTransactions()
                clearFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Gagal mereset database transaksi: ${e.message}")
            }
        }
    }

    /**
     * Mengimpor daftar transaksi hasil parsing CSV ke dalam basis data Room/SQLite secara batch.
     */
    fun importTransactionsBatch(
        transactions: List<TransactionEntity>,
        onSuccess: (count: Int) -> Unit = {},
        onError: (message: String) -> Unit = {}
    ) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                val insertedIds = repository.insertTransactionsBatch(transactions)
                onSuccess(insertedIds.size)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Gagal mengimpor batch transaksi CSV"
                _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
                onError(errorMsg)
            }
        }
    }

    /**
     * Mengeksekusi impor transaksi CSV secara langsung (suspend) dengan resolusi O(1) in-memory
     * untuk akun dan kategori guna mengeliminasi masalah N+1 Database Queries (PERF-01).
     *
     * Logika Operasi:
     * 1. Pre-fetch seluruh akun dan kategori ke dalam MutableMap in-memory sebelum perulangan baris.
     * 2. Pencocokan nama akun dan kategori secara O(1) berbasis lowercase.
     * 3. Jika nama akun/kategori baru ditemukan, simpan ke database 1x lalu daftarkan ke cache lokal
     *    sehingga baris-baris berikutnya dengan nama yang sama tidak memicu query database tambahan.
     * 4. Melakukan batch insert seluruh entitas transaksi secara atomik melalui [TransactionRepository.insertTransactionsBatch].
     *
     * @param parsedTransactions Daftar transaksi yang telah diparsing dari format CSV.
     * @param defaultAccountId ID akun fallback jika baris CSV tidak memuat nama akun valid.
     * @param defaultAccountName Nama akun fallback jika akun default baru perlu dibuat.
     * @param defaultCategory Kategori fallback jika baris CSV kosong pada kolom kategori.
     * @return Jumlah transaksi yang berhasil dimasukkan ke basis data.
     */
    suspend fun importFromCsvSuspend(
        parsedTransactions: List<CsvImporter.ParsedTransaction>,
        defaultAccountId: Long? = null,
        defaultAccountName: String? = null,
        defaultCategory: String = "Lainnya"
    ): Int {
        val validRows = parsedTransactions.filter { it.isValid }
        if (validRows.isEmpty()) return 0

        // 1. Pre-fetch akun & kategori sekali saja ke dalam memory Map O(1)
        val initialAccounts = repository.getAllAccounts().first()
        val initialCategories = repository.getAllCategories().first()

        val accountsByName = initialAccounts.associateBy { it.name.trim().lowercase(Locale.ROOT) }.toMutableMap()
        val categoriesByName = initialCategories.associateBy { it.name.trim().lowercase(Locale.ROOT) }.toMutableMap()

        // Resolusi fallback akun default
        val fallbackAccount = if (defaultAccountId != null) {
            initialAccounts.find { it.id == defaultAccountId }
        } else {
            initialAccounts.firstOrNull()
        } ?: run {
            val fallbackName = defaultAccountName?.takeIf { it.isNotBlank() } ?: "Dompet Tunai"
            val fallbackKey = fallbackName.lowercase(Locale.ROOT)
            accountsByName.getOrPut(fallbackKey) {
                val newId = repository.addAccount(name = fallbackName, type = "CASH", initialBalance = 0.0)
                AccountEntity(id = newId, name = fallbackName, type = "CASH", balance = 0.0)
            }
        }

        val entitiesToInsert = ArrayList<TransactionEntity>(validRows.size)

        // 2. Iterasi baris CSV dengan in-memory resolution O(1)
        for (row in validRows) {
            // Resolusi Akun
            val resolvedAccount = if (row.rawAccountName.isNotBlank()) {
                val accKey = row.rawAccountName.trim().lowercase(Locale.ROOT)
                accountsByName.getOrPut(accKey) {
                    val accName = row.rawAccountName.trim()
                    val newId = repository.addAccount(name = accName, type = "BANK", initialBalance = 0.0)
                    AccountEntity(id = newId, name = accName, type = "BANK", balance = 0.0)
                }
            } else {
                fallbackAccount
            }

            // Resolusi Kategori
            val rawCat = if (row.category.isNotBlank()) row.category.trim() else defaultCategory
            val catKey = rawCat.lowercase(Locale.ROOT)
            val resolvedCategory = categoriesByName.getOrPut(catKey) {
                val newCatId = repository.insertCategory(name = rawCat, type = row.type, color = "#FAFF00")
                CategoryEntity(id = newCatId, name = rawCat, type = row.type, color = "#FAFF00")
            }

            entitiesToInsert.add(
                TransactionEntity(
                    title = row.title,
                    amount = row.amount,
                    type = row.type,
                    category = resolvedCategory.name,
                    date = row.date,
                    accountId = resolvedAccount.id,
                    accountName = resolvedAccount.name,
                    notes = row.notes
                )
            )
        }

        // 3. Batch insert seluruh transaksi dalam 1 operasi atomik
        val insertedIds = repository.insertTransactionsBatch(entitiesToInsert)
        return insertedIds.size
    }

    /**
     * Overload suspend untuk mengimpor dari teks string CSV mentah.
     */
    suspend fun importFromCsvSuspend(
        csvContent: String,
        defaultAccountId: Long? = null,
        defaultAccountName: String? = null,
        defaultCategory: String = "Lainnya"
    ): Int {
        val parsed = CsvImporter.parseCsv(csvContent, defaultCategory)
        return importFromCsvSuspend(parsed, defaultAccountId, defaultAccountName, defaultCategory)
    }

    /**
     * Mengimpor daftar transaksi hasil parsing CSV dengan resolusi O(1) in-memory untuk
     * akun dan kategori guna mengeliminasi masalah N+1 Database Queries (PERF-01).
     */
    fun importFromCsv(
        parsedTransactions: List<CsvImporter.ParsedTransaction>,
        defaultAccountId: Long? = null,
        defaultAccountName: String? = null,
        defaultCategory: String = "Lainnya",
        onSuccess: (count: Int) -> Unit = {},
        onError: (message: String) -> Unit = {}
    ) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                val count = importFromCsvSuspend(
                    parsedTransactions = parsedTransactions,
                    defaultAccountId = defaultAccountId,
                    defaultAccountName = defaultAccountName,
                    defaultCategory = defaultCategory
                )
                onSuccess(count)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Gagal memproses impor CSV"
                _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
                onError(errorMsg)
            }
        }
    }

    /**
     * Mengimpor transaksi dari string CSV mentah secara asinkron.
     */
    fun importFromCsv(
        csvContent: String,
        defaultAccountId: Long? = null,
        defaultAccountName: String? = null,
        defaultCategory: String = "Lainnya",
        onSuccess: (count: Int) -> Unit = {},
        onError: (message: String) -> Unit = {}
    ) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                val count = importFromCsvSuspend(
                    csvContent = csvContent,
                    defaultAccountId = defaultAccountId,
                    defaultAccountName = defaultAccountName,
                    defaultCategory = defaultCategory
                )
                onSuccess(count)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Gagal memproses impor CSV"
                _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
                onError(errorMsg)
            }
        }
    }

    /**
     * Memicu permintaan analisis finansial cerdas "Pak Hemat · AI Insight"
     * untuk rentang transaksi tertentu atau transaksi periode yang sedang aktif.
     * Menggunakan penyaringan SQL SQLite sebelum LIMIT 100 dan menyertakan ringkasan
     * agregasi periode lengkap dari basis data agar konteks angka tetap utuh dan akurat.
     * Progres anggaran dievaluasi berdasarkan bulan dari rentang periode yang dipilih (startMonth == endMonth).
     * Jika periode mencakup multi-bulan atau semua waktu, daftar anggaran dikirim kosong dan evaluasi
     * batas anggaran bulanan ditandai tidak berlaku untuk mencegah klaim overbudget/safe yang keliru.
     *
     * Menerapkan mekanisme correlation request ID dan stale response discarding:
     * setiap pemanggilan menghasilkan identifier unik [requestId] dan mencatatnya sebagai permintaan aktif.
     * Respon yang selesai ketika permintaan sudah tidak aktif (stale request yang disusul oleh request baru)
     * dibuang secara otomatis dan tidak menimpa [uiState].
     *
     * @param startDate Tanggal awal rentang (format "yyyy-MM-dd", opsional).
     * @param endDate Tanggal akhir rentang (format "yyyy-MM-dd", opsional).
     * @return Identifier korelasi permintaan unik ([requestId]).
     */
    fun fetchAiInsight(startDate: String? = null, endDate: String? = null): String {
        val requestId = UUID.randomUUID().toString()
        activeInsightRequestId = requestId
        _isAiInsightLoading.value = true
        _aiInsightError.value = null

        activeScope.launch(coroutineExceptionHandler) {
            try {
                val (resolvedStart, resolvedEnd) = if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
                    Pair(startDate, endDate)
                } else {
                    resolveReportsDateRange(_selectedReportsPreset.value, Date(clock.millis()))
                }

                // 1. Ambil sampel transaksi periode langsung via SQL filter sebelum LIMIT 100
                val targetTransactions = repository.getTransactionsForPeriod(
                    startDate = resolvedStart,
                    endDate = resolvedEnd,
                    limit = 100
                )

                // 2. Ambil ringkasan agregasi periode utuh dan laporan analitik dari SQLite
                val periodSummary = repository.getFinanceSummary(
                    startDate = resolvedStart,
                    endDate = resolvedEnd
                )
                val reportsAnalytics = repository.getReportsAnalytics(
                    startDate = resolvedStart,
                    endDate = resolvedEnd
                )

                val allAccounts = repository.getAllAccounts().first()

                val startMonth = resolvedStart?.take(7)
                val endMonth = resolvedEnd?.take(7)

                val isMultiMonth = startMonth == null || endMonth == null || startMonth != endMonth
                val periodBudgets = if (startMonth != null && startMonth == endMonth) {
                    repository.getBudgetProgress(startMonth).first()
                } else {
                    emptyList()
                }

                val periodTitle = if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
                    "$startDate s/d $endDate"
                } else {
                    when (_selectedReportsPreset.value) {
                        ReportsPreset.LAST_MONTH -> "Bulan Lalu"
                        ReportsPreset.LAST_3_MONTHS -> "3 Bulan Terakhir"
                        ReportsPreset.ALL_TIME -> "Semua Waktu"
                        ReportsPreset.THIS_MONTH -> "Bulan Ini"
                    }
                }

                val currentApiKey = if (context != null) {
                    com.sena.financetracker.util.SecurityConfig.getGroqApiKey(context)
                } else {
                    com.sena.financetracker.util.SecurityConfig.getGroqApiKey()
                }

                val insight = com.sena.financetracker.service.AiInsightService.getFinancialInsight(
                    accounts = allAccounts,
                    budgets = periodBudgets,
                    transactions = targetTransactions,
                    periodTitle = periodTitle,
                    apiKey = currentApiKey,
                    periodSummary = periodSummary,
                    reportsAnalytics = reportsAnalytics,
                    isMultiMonth = isMultiMonth
                )

                if (activeInsightRequestId != requestId) {
                    return@launch // Stale request, discard
                }

                _aiInsightText.value = insight
                _completedInsightRequestId.value = requestId
            } catch (e: Exception) {
                if (activeInsightRequestId != requestId) {
                    return@launch // Stale request, discard
                }

                _aiInsightError.value = e.message ?: "Gagal mendapatkan analisis AI"
                _completedInsightRequestId.value = requestId
            } finally {
                if (activeInsightRequestId == requestId) {
                    _isAiInsightLoading.value = false
                }
            }
        }
        return requestId
    }

    /**
     * Menambahkan anggaran baru per kategori dan periode bulan.
     */
    fun addBudget(
        name: String,
        category: String,
        limitAmount: Double,
        period: String = ""
    ) {
        val targetPeriod = if (period.isNotBlank()) period else SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(clock.millis()))
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.addBudget(name, category, limitAmount, targetPeriod)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Menghapus anggaran berdasarkan ID.
     */
    fun deleteBudget(id: Long) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.deleteBudget(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Mencatat transaksi baru ke database dan memperbarui saldo rekening akun secara atomik.
     */
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: String = "",
        accountId: Long = 1L,
        accountName: String = "Dompet Tunai",
        notes: String = ""
    ) {
        val targetDate = if (date.isNotBlank()) date else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(clock.millis()))
        activeScope.launch(coroutineExceptionHandler) {
            try {
                val newTx = TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = targetDate,
                    accountId = accountId,
                    accountName = accountName,
                    notes = notes
                )
                repository.insertTransaction(newTx)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Menghapus transaksi dan mengembalikan saldo rekening yang terdampak.
     */
    fun deleteTransaction(transaction: TransactionEntity) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.deleteTransaction(transaction)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Memperbarui informasi transaksi yang sudah ada dan melakukan sinkronisasi saldo.
     */
    fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.updateTransaction(oldTransaction, newTransaction)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Melakukan transfer dana antar rekening bank / dompet.
     */
    fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String = ""
    ) {
        val targetDate = if (date.isNotBlank()) date else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(clock.millis()))
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.transferFunds(fromAccount, toAccount, amount, notes, targetDate)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Membuat rekening akun baru dengan saldo pembuka.
     */
    fun addAccount(
        name: String,
        type: String,
        initialBalance: Double
    ) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.addAccount(name, type, initialBalance)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Menjalankan rekonsiliasi saldo akun ke nilai fisik riil.
     */
    fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = ""
    ) {
        val targetDate = if (date.isNotBlank()) date else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(clock.millis()))
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.reconcileAccount(account, actualBalance, targetDate)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Menambahkan kategori transaksi baru (kustom buatan pengguna).
     */
    fun addCategory(
        name: String,
        type: String,
        color: String = "#FAFF00"
    ) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.insertCategory(name, type, color)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Memperbarui detail nama, tipe, atau warna kategori kustom.
     */
    fun updateCategory(
        id: Long,
        name: String,
        type: String,
        color: String
    ) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.updateCategory(id, name, type, color)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Menghapus kategori kustom berdasarkan ID. Kategori bawaan sistem diproteksi dan ditolak.
     */
    fun deleteCategory(id: Long) {
        activeScope.launch(coroutineExceptionHandler) {
            try {
                repository.deleteCategory(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Memperbarui status ketersediaan API key dari secure storage atau cache in-memory.
     *
     * @param context Context Android opsional untuk mengakses EncryptedSharedPreferences.
     */
    fun refreshApiKeyStatus(context: Context? = null) {
        val targetContext = context ?: this.context
        _hasApiKey.value = if (targetContext != null) {
            com.sena.financetracker.security.ApiKeyStorage.hasCustomApiKey(targetContext)
        } else {
            com.sena.financetracker.security.ApiKeyStorage.hasCustomApiKey()
        }
    }

    /**
     * Menyimpan Groq API Key ke dalam secure storage terenkripsi dan memperbarui status StateFlow.
     *
     * @param key API key mentah yang dimasukkan user.
     * @param context Context Android opsional untuk menulis ke EncryptedSharedPreferences.
     */
    fun saveGroqApiKey(key: String, context: Context? = null) {
        val targetContext = context ?: this.context
        val cleanKey = key.trim()
        if (targetContext != null) {
            com.sena.financetracker.security.ApiKeyStorage.setGroqApiKey(targetContext, cleanKey)
        } else {
            com.sena.financetracker.security.ApiKeyStorage.setGroqApiKey(cleanKey)
        }
        _hasApiKey.value = cleanKey.isNotBlank()
    }

    /**
     * Menghapus Groq API Key dari secure storage dan memperbarui status StateFlow ke false.
     *
     * @param context Context Android opsional untuk menghapus dari EncryptedSharedPreferences.
     */
    fun clearGroqApiKey(context: Context? = null) {
        val targetContext = context ?: this.context
        if (targetContext != null) {
            com.sena.financetracker.security.ApiKeyStorage.clearGroqApiKey(targetContext)
        } else {
            com.sena.financetracker.security.ApiKeyStorage.clearGroqApiKey()
        }
        _hasApiKey.value = false
    }

    /**
     * Factory provider untuk inisialisasi [FinanceViewModel] dengan dependency injection manual.
     */
    class Factory(
        private val repository: TransactionRepository,
        private val context: Context? = null,
        private val clock: Clock = Clock.systemDefaultZone()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(repository, context = context, clock = clock) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
