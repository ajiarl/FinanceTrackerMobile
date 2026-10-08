package com.sena.financetracker.repository

import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.data.NotificationDao
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.util.CurrencyMath.roundCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain handler untuk kalkulasi pemakaian anggaran dan peringatan overbudget.
 */
class BudgetDomainHandler(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val notificationDao: NotificationDao
) {
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    fun getBudgetProgress(
        period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ): Flow<List<BudgetProgressItem>> {
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getAllTransactions()
        ) { budgets, transactions ->
            val activeBudgets = budgets.filter { it.isActive && (it.period.isEmpty() || it.period == period) }
            activeBudgets.map { budget ->
                val spentAmount = roundCurrency(
                    transactions
                        .filter { tx ->
                            tx.type.equals("EXPENSE", ignoreCase = true) &&
                            tx.category.equals(budget.category, ignoreCase = true) &&
                            tx.date.startsWith(period)
                        }
                        .sumOf { it.amount }
                )

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

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudget(id)
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.updateBudget(budget)
    }

    suspend fun checkAndTriggerBudgetAlert(transaction: TransactionEntity) {
        val period = if (transaction.date.length >= 7) {
            transaction.date.substring(0, 7)
        } else {
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        }

        val allBudgets = budgetDao.getAllBudgets().first()
        val matchingBudgets = allBudgets.filter { budget ->
            budget.isActive &&
            (budget.period.isEmpty() || budget.period == period) &&
            budget.category.equals(transaction.category, ignoreCase = true)
        }

        if (matchingBudgets.isEmpty()) return

        val allTransactions = transactionDao.getAllTransactions().first()
        val totalSpentInCategory = roundCurrency(
            allTransactions
                .filter { tx ->
                    tx.type.equals("EXPENSE", ignoreCase = true) &&
                    tx.category.equals(transaction.category, ignoreCase = true) &&
                    tx.date.startsWith(period)
                }
                .sumOf { it.amount }
        )

        for (budget in matchingBudgets) {
            if (budget.limitAmount <= 0) continue

            val percentage = ((totalSpentInCategory / budget.limitAmount) * 100).toInt()
            if (percentage >= 100) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        title = "ANGGARAN TERLAMPAUI (100%+)",
                        message = "Pengeluaran '${budget.category}' mencapai Rp ${totalSpentInCategory.toLong()} (${percentage}% dari anggaran Rp ${budget.limitAmount.toLong()}). Segera evaluasi!",
                        type = "DANGER",
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else if (percentage >= 80) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        title = "PERINGATAN ANGGARAN (80%+)",
                        message = "Pengeluaran '${budget.category}' telah mencapai Rp ${totalSpentInCategory.toLong()} (${percentage}% dari batas anggaran).",
                        type = "WARNING",
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
