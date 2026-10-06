package com.sena.financetracker.data

data class TransactionEntity(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val date: String
)
