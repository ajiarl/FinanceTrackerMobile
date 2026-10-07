package com.sena.financetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _selectedDateFilter = MutableStateFlow("ALL")
    private val _selectedFilterTab = MutableStateFlow("ALL")

    init {
        observeData()
    }

    private data class FilterParams(
        val query: String,
        val category: String?,
        val dateFilter: String,
        val typeFilter: String
    )

    private data class DataBundle(
        val transactions: List<TransactionEntity>,
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val budgets: List<BudgetProgressItem>
    )

    /**
     * Mengobservasi dan menggabungkan aliran basis data Room/SQLite bersama parameter filter.
     *
     * Alur:
     * 1. Menggabungkan 4 StateFlow filter menjadi aliran [FilterParams].
     * 2. Menggabungkan 4 Flow database dari repository menjadi [DataBundle].
     * 3. Mengkalkulasi total keuangan riil serta menyaring [FinanceUiState.filteredTransactions].
     */
    private fun observeData() {
        val filterParamsFlow = combine(
            _searchQuery,
            _selectedCategoryFilter,
            _selectedDateFilter,
            _selectedFilterTab
        ) { query, category, dateFilter, typeFilter ->
            FilterParams(query, category, dateFilter, typeFilter)
        }

        val dataFlow = combine(
            repository.getAllTransactions(),
            repository.getAllAccounts(),
            repository.getAllCategories(),
            repository.getBudgetProgress()
        ) { txs, accs, cats, budgets ->
            DataBundle(txs, accs, cats, budgets)
        }

        viewModelScope.launch {
            combine(dataFlow, filterParamsFlow) { data, filter ->
                calculateFinanceTotals(
                    transactions = data.transactions,
                    accounts = data.accounts,
                    categories = data.categories,
                    budgets = data.budgets,
                    searchQuery = filter.query,
                    selectedCategoryFilter = filter.category,
                    selectedDateFilter = filter.dateFilter,
                    selectedFilterTab = filter.typeFilter
                ).copy(isLoading = false)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
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
