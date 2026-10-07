package com.sena.financetracker.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BudgetEntity(
    val id: Long = 0,
    val name: String,
    val category: String,
    val limitAmount: Double,
    val period: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    val isActive: Boolean = true
)

data class BudgetProgressItem(
    val budget: BudgetEntity,
    val spentAmount: Double,
    val percentage: Int,
    val isOver: Boolean,
    val statusLevel: String // "SAFE", "WARNING", "CRITICAL"
)
