package com.sena.financetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState(isLoading = true))

    /**
     * StateFlow publik yang diobservasi oleh composable UI layer.
     */
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    /**
     * StateFlow daftar notifikasi sistem aplikasi.
     */
    val notifications: StateFlow<List<NotificationEntity>> = repository.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * StateFlow jumlah notifikasi yang belum dibaca.
     */
    val unreadNotificationCount: StateFlow<Int> = repository.getUnreadNotificationCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _selectedDateFilter = MutableStateFlow("ALL")
    private val _selectedFilterTab = MutableStateFlow("ALL")
    private val _reportsPeriodPreset = MutableStateFlow("THIS_MONTH")
    private val _aiInsightText = MutableStateFlow<String?>(null)
    private val _isAiInsightLoading = MutableStateFlow(false)
    private val _aiInsightError = MutableStateFlow<String?>(null)

    init {
        observeData()
    }

    private data class FilterParams(
        val query: String,
        val category: String?,
        val dateFilter: String,
        val typeFilter: String,
        val reportsPreset: String,
        val aiInsightText: String?,
        val isAiInsightLoading: Boolean,
        val aiInsightError: String?
    )

    private data class CoreData(
        val transactions: List<TransactionEntity>,
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val budgets: List<BudgetProgressItem>
    )

    private data class NotifData(
        val notifications: List<NotificationEntity>,
        val unreadNotificationCount: Int
    )

    private data class DataBundle(
        val transactions: List<TransactionEntity>,
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val budgets: List<BudgetProgressItem>,
        val notifications: List<NotificationEntity>,
        val unreadNotificationCount: Int
    )

    /**
     * Mengobservasi dan menggabungkan aliran basis data Room/SQLite bersama parameter filter.
     *
     * Alur:
     * 1. Menggabungkan 5 StateFlow filter menjadi aliran [FilterParams].
     * 2. Menggabungkan 4 Flow database dari repository menjadi [DataBundle].
     * 3. Mengkalkulasi total keuangan riil, menyaring [FinanceUiState.filteredTransactions], serta kalkulasi [ReportsAnalyticsState].
     */
    private fun observeData() {
        val filterParamsFlow = combine(
            combine(_searchQuery, _selectedCategoryFilter, _selectedDateFilter) { q, c, d -> Triple(q, c, d) },
            combine(_selectedFilterTab, _reportsPeriodPreset) { t, r -> Pair(t, r) },
            combine(_aiInsightText, _isAiInsightLoading, _aiInsightError) { text, loading, err -> Triple(text, loading, err) }
        ) { (query, category, dateFilter), (typeFilter, reportsPreset), (aiText, aiLoading, aiErr) ->
            FilterParams(
                query = query,
                category = category,
                dateFilter = dateFilter,
                typeFilter = typeFilter,
                reportsPreset = reportsPreset,
                aiInsightText = aiText,
                isAiInsightLoading = aiLoading,
                aiInsightError = aiErr
            )
        }

        val coreDataFlow = combine(
            repository.getAllTransactions(),
            repository.getAllAccounts(),
            repository.getAllCategories(),
            repository.getBudgetProgress()
        ) { txs, accs, cats, budgets ->
            CoreData(txs, accs, cats, budgets)
        }

        val notifDataFlow = combine(
            repository.getAllNotifications(),
            repository.getUnreadNotificationCount()
        ) { notifs, unreadCount ->
            NotifData(notifs, unreadCount)
        }

        val dataFlow = combine(coreDataFlow, notifDataFlow) { core, notif ->
            DataBundle(
                transactions = core.transactions,
                accounts = core.accounts,
                categories = core.categories,
                budgets = core.budgets,
                notifications = notif.notifications,
                unreadNotificationCount = notif.unreadNotificationCount
            )
        }

        viewModelScope.launch {
            combine(dataFlow, filterParamsFlow) { data, filter ->
                calculateFinanceTotals(
                    transactions = data.transactions,
                    accounts = data.accounts,
                    categories = data.categories,
                    budgets = data.budgets,
                    notifications = data.notifications,
                    unreadNotificationCount = data.unreadNotificationCount,
                    searchQuery = filter.query,
                    selectedCategoryFilter = filter.category,
                    selectedDateFilter = filter.dateFilter,
                    selectedFilterTab = filter.typeFilter,
                    reportsPreset = filter.reportsPreset,
                    aiInsightText = filter.aiInsightText,
                    isAiInsightLoading = filter.isAiInsightLoading,
                    aiInsightError = filter.aiInsightError
                ).copy(isLoading = false)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * Memperbarui filter preset waktu analitik laporan ("THIS_MONTH", "LAST_MONTH", "LAST_3_MONTHS", "ALL_TIME").
     */
    fun setReportsPeriodPreset(preset: String) {
        _reportsPeriodPreset.value = preset
    }

    /**
     * Memperbarui kata kunci pencarian teks pada judul, kategori, dan catatan transaksi.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Memperbarui filter kategori aktif (atau null untuk semua kategori).
     */
    fun setSelectedCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    // ── NOTIFICATIONS ACTIONS ──────────────────────────────────────────────────
    /**
     * Menandai notifikasi sebagai sudah dibaca berdasarkan ID.
     */
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
    }

    /**
     * Mengosongkan seluruh riwayat database transaksi dan mereset filter.
     */
    fun resetTransactions() {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
     * Memicu permintaan analisis finansial cerdas "Pak Hemat · AI Insight"
     * untuk rentang transaksi tertentu atau transaksi periode yang sedang aktif.
     */
    fun fetchAiInsight(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _isAiInsightLoading.value = true
            _aiInsightError.value = null
            try {
                val allTx = repository.getAllTransactions().first()
                val targetTransactions = if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
                    allTx.filter { tx ->
                        val date = tx.date.take(10)
                        date in startDate..endDate
                    }
                } else {
                    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    val thisMonthStr = sdfMonth.format(cal.time)
                    cal.add(Calendar.MONTH, -1)
                    val lastMonthStr = sdfMonth.format(cal.time)

                    val last3MonthsSet = mutableSetOf<String>()
                    val cal3 = Calendar.getInstance()
                    for (i in 0..2) {
                        last3MonthsSet.add(sdfMonth.format(cal3.time))
                        cal3.add(Calendar.MONTH, -1)
                    }

                    when (_reportsPeriodPreset.value) {
                        "THIS_MONTH" -> allTx.filter { it.date.take(7) == thisMonthStr }
                        "LAST_MONTH" -> allTx.filter { it.date.take(7) == lastMonthStr }
                        "LAST_3_MONTHS" -> allTx.filter { last3MonthsSet.contains(it.date.take(7)) }
                        else -> allTx
                    }
                }

                val periodTitle = when (_reportsPeriodPreset.value) {
                    "LAST_MONTH" -> "Bulan Lalu"
                    "LAST_3_MONTHS" -> "3 Bulan Terakhir"
                    "ALL_TIME" -> "Semua Waktu"
                    else -> "Bulan Ini"
                }

                val insight = com.sena.financetracker.service.AiInsightService.getFinancialInsight(
                    transactions = targetTransactions,
                    periodTitle = periodTitle
                )
                _aiInsightText.value = insight
            } catch (e: Exception) {
                _aiInsightError.value = e.message ?: "Gagal mendapatkan analisis AI"
            } finally {
                _isAiInsightLoading.value = false
            }
        }
    }

    /**
     * Menambahkan anggaran baru per kategori dan periode bulan.
     */
    fun addBudget(
        name: String,
        category: String,
        limitAmount: Double,
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            try {
                repository.addBudget(name, category, limitAmount, period)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Menghapus anggaran berdasarkan ID.
     */
    fun deleteBudget(id: Long) {
        viewModelScope.launch {
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
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        accountId: Long = 1L,
        accountName: String = "Dompet Tunai",
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val newTx = TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            try {
                repository.transferFunds(fromAccount, toAccount, amount, notes, date)
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
        viewModelScope.launch {
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
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            try {
                repository.reconcileAccount(account, actualBalance, date)
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            try {
                repository.deleteCategory(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * Factory provider untuk inisialisasi [FinanceViewModel] dengan dependency injection manual.
     */
    class Factory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
