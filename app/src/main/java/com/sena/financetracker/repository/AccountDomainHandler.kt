package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain handler untuk pengelolaan rekening, transfer dana atomik, dan rekonsiliasi saldo akun.
 */
class AccountDomainHandler(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String
    ) {
        require(fromAccount.id != toAccount.id) { "Akun asal dan akun tujuan tidak boleh sama" }
        require(amount > 0) { "Nominal transfer harus lebih besar dari 0" }

        accountDao.adjustBalance(fromAccount.id, -amount)
        accountDao.adjustBalance(toAccount.id, amount)

        val transferTx = TransactionEntity(
            title = "Transfer ke ${toAccount.name}",
            amount = amount,
            type = "TRANSFER",
            category = "Transfer",
            date = date,
            accountId = fromAccount.id,
            accountName = fromAccount.name,
            notes = if (notes.isNotBlank()) notes else "Transfer dari ${fromAccount.name} ke ${toAccount.name}",
            toAccountId = toAccount.id,
            toAccountName = toAccount.name
        )
        transactionDao.insertTransaction(transferTx)
    }

    suspend fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Long {
        val newAccount = AccountEntity(
            name = name,
            type = type,
            balance = initialBalance
        )
        val newAccountId = accountDao.insertAccount(newAccount)

        if (initialBalance > 0) {
            val initialTx = TransactionEntity(
                title = "Saldo Awal",
                amount = initialBalance,
                type = "INCOME",
                category = "Saldo Awal",
                date = date,
                accountId = newAccountId,
                accountName = name,
                notes = "Saldo awal saat pembuatan akun"
            )
            transactionDao.insertTransaction(initialTx)
        }
        return newAccountId
    }

    suspend fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        val diff = actualBalance - account.balance
        if (kotlin.math.abs(diff) < 0.001) return

        accountDao.updateBalance(account.id, actualBalance)

        val adjustmentTx = TransactionEntity(
            title = "Penyesuaian Saldo Sistem",
            amount = kotlin.math.abs(diff),
            type = if (diff > 0) "INCOME" else "EXPENSE",
            category = "Penyesuaian",
            date = date,
            accountId = account.id,
            accountName = account.name,
            notes = "Rekonsiliasi: saldo lama ${account.balance.toLong()}, saldo baru ${actualBalance.toLong()}, selisih ${if (diff > 0) "+" else ""}${diff.toLong()}"
        )
        transactionDao.insertTransaction(adjustmentTx)
    }
}
