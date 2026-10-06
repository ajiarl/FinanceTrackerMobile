package com.sena.financetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FinanceViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState(isLoading = true))
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            repository.getAllTransactions().collect { list ->
                val calculated = calculateFinanceTotals(list)
                _uiState.value = calculated.copy(isLoading = false)
            }
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: String
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type.trim().uppercase(),
                category = category.trim(),
                date = date.trim()
            )
            repository.insertTransaction(entity)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
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
