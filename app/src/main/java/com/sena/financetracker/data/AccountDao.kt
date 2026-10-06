package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow

interface AccountDao {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateBalance(id: Long, newBalance: Double)
    suspend fun adjustBalance(id: Long, delta: Double)
    suspend fun deleteAccount(id: Long)
}
