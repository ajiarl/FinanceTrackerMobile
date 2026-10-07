package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow

interface BudgetDao {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>>
    suspend fun getBudgetById(id: Long): BudgetEntity?
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(id: Long)
}
