package com.sena.financetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sena.financetracker.data.AccountEntity
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

class FinanceViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState(isLoading = true))
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _selectedDateFilter = MutableStateFlow("ALL")
    private val _selectedFilterTab = MutableStateFlow("ALL")

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val dataFlow = combine(
                repository.getAllTransactions(),
                repository.getAllAccounts(),
                repository.getAllCategories()
            ) { transactions, accounts, categories ->
                Triple(transactions, accounts, categories)
            }

            val filterFlow = combine(
                _searchQuery,
                _selectedCategoryFilter,
                _selectedDateFilter,
                _selectedFilterTab
            ) { query, cat, date, tab ->
                FilterParams(query, cat, date, tab)
            }

            combine(dataFlow, filterFlow) { (transactions, accounts, categories), filter ->
                calculateFinanceTotals(
                    transactions = transactions,
                    accounts = accounts,
                    categories = categories,
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

    private data class FilterParams(
        val query: String,
        val category: String?,
        val dateFilter: String,
        val typeFilter: String
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setSelectedDateFilter(dateFilter: String) {
        _selectedDateFilter.value = dateFilter
    }

    fun setSelectedFilterTab(tab: String) {
        _selectedFilterTab.value = tab
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _selectedDateFilter.value = "ALL"
        _selectedFilterTab.value = "ALL"
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: String,
        accountId: Long = 1,
        accountName: String = "Dompet Tunai",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type.trim().uppercase(),
                category = category.trim(),
                date = date.trim(),
                accountId = accountId,
                accountName = accountName.trim(),
                notes = notes.trim()
            )
            repository.insertTransaction(entity)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(oldTransaction, newTransaction)
        }
    }

    fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            repository.transferFunds(fromAccount, toAccount, amount, notes, date)
        }
    }

    fun addAccount(name: String, type: String, initialBalance: Double) {
        viewModelScope.launch {
            repository.addAccount(name, type, initialBalance)
        }
    }

    fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            repository.reconcileAccount(account, actualBalance, date)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            val tx = _uiState.value.transactions.find { it.id == id }
            if (tx != null) {
                repository.deleteTransaction(tx)
            } else {
                repository.deleteTransaction(id)
            }
        }
    }

    class Factory(
        private val repository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
