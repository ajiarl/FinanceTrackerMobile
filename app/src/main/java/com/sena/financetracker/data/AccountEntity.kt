package com.sena.financetracker.data

data class AccountEntity(
    val id: Long = 0,
    val name: String,
    val type: String, // "cash", "bank", "e-wallet"
    val balance: Double = 0.0
)
