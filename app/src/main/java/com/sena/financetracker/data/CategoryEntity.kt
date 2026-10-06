package com.sena.financetracker.data

data class CategoryEntity(
    val id: Long = 0,
    val name: String,
    val type: String, // "EXPENSE", "INCOME"
    val color: String = "#FAFF00"
)
