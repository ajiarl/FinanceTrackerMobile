package com.sena.financetracker.repository

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.util.CurrencyMath.roundCurrency
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

    /**
     * Mentransfer dana antar rekening secara atomik dan mencatat transaksi transfer.
     *
     * @param fromAccount Rekening pengirim (sumber dana).
     * @param toAccount Rekening penerima (tujuan transfer).
     * @param amount Nominal yang akan ditransfer (harus positif dan tidak melebihi saldo pengirim).
     * @param notes Catatan transfer opsional.
     * @param date Tanggal transfer dalam format ISO yyyy-MM-dd.
     * @throws IllegalArgumentException Jika akun asal dan tujuan sama, amount <= 0, atau saldo tidak mencukupi (overdraft).
     */
    suspend fun transferFunds(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String = "",
        date: String
    ) {
        require(fromAccount.id != toAccount.id) { "Akun asal dan akun tujuan tidak boleh sama" }
        require(amount > 0) { "Nominal transfer harus lebih besar dari 0" }

        val safeAmount = roundCurrency(amount)

        val currentFromAccount = accountDao.getAccountById(fromAccount.id) ?: fromAccount
        require(currentFromAccount.balance >= safeAmount) {
            "Saldo rekening asal (${currentFromAccount.name}) tidak mencukupi untuk transfer"
        }

        accountDao.adjustBalance(fromAccount.id, -safeAmount)
        accountDao.adjustBalance(toAccount.id, safeAmount)

        val transferTx = TransactionEntity(
            title = "Transfer ke ${toAccount.name}",
            amount = safeAmount,
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
        val safeInitialBalance = roundCurrency(initialBalance)
        val newAccount = AccountEntity(
            name = name,
            type = type,
            balance = safeInitialBalance
        )
        val newAccountId = accountDao.insertAccount(newAccount)

        if (safeInitialBalance > 0) {
            val initialTx = TransactionEntity(
                title = "Saldo Awal",
                amount = safeInitialBalance,
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
        val safeActualBalance = roundCurrency(actualBalance)
        val diff = roundCurrency(safeActualBalance - account.balance)
        if (kotlin.math.abs(diff) < 0.001) return

        accountDao.updateBalance(account.id, safeActualBalance)

        val adjustmentTx = TransactionEntity(
            title = "Penyesuaian Saldo Sistem",
            amount = kotlin.math.abs(diff),
            type = if (diff > 0) "INCOME" else "EXPENSE",
            category = "Penyesuaian",
            date = date,
            accountId = account.id,
            accountName = account.name,
            notes = "Rekonsiliasi: saldo lama ${account.balance.toLong()}, saldo baru ${safeActualBalance.toLong()}, selisih ${if (diff > 0) "+" else ""}${diff.toLong()}"
        )
        transactionDao.insertTransaction(adjustmentTx)
    }
}
