package com.sena.financetracker.data

data class TransactionEntity(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", "TRANSFER"
    val category: String,
    val date: String,
    val accountId: Long = 1,
    val accountName: String = "Dompet Tunai",
    val notes: String = "",
    val toAccountId: Long? = null,
    val toAccountName: String? = null
)
