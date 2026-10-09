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
 * Interface untuk mengeksekusi blok kode di dalam transaksi database atomik (ACID).
 */
interface TransactionRunner {
    /**
     * Menjalankan [block] operasi di dalam transaksi atomik database.
     * Jika terjadi kegagalan atau exception, seluruh operasi di dalam blok dibatalkan (rollback).
     */
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

/**
 * Domain handler untuk pengelolaan rekening, transfer dana atomik, dan rekonsiliasi saldo akun.
 * Mengelola integritas transaksional (ACID) untuk seluruh mutasi rekening dan pencatatan transaksi terkait.
 *
 * @param accountDao Data access object untuk entitas rekening.
 * @param transactionDao Data access object untuk entitas transaksi.
 * @param transactionRunner Runner transaksi atomik database (default mendelegasikan ke transactionDao.runInTransaction).
 */
class AccountDomainHandler(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val transactionRunner: TransactionRunner = object : TransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T =
            transactionDao.runInTransaction(block)
    }
) {
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    /**
     * Menjalankan operasi di dalam transaksi database atomik via [transactionRunner].
     *
     * @param block Blok kode suspend yang dieksekusi di dalam transaksi.
     * @return Hasil pengembalian dari [block].
     */
    suspend fun <T> runInTransaction(block: suspend () -> T): T = transactionRunner.runInTransaction(block)

    /**
     * Mentransfer dana antar rekening secara atomik dan mencatat transaksi transfer.
     * Seluruh operasi mutasi saldo rekening pengirim, penerima, dan pencatatan riwayat transfer
     * dibungkus ke dalam [runInTransaction] agar menjamin konsistensi ACID (zero partial state).
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
    ) = runInTransaction {
        require(fromAccount.id != toAccount.id) { "Akun asal dan akun tujuan tidak boleh sama" }
        require(amount > 0) { "Nominal transfer harus lebih besar dari 0" }

        val safeAmount = roundCurrency(amount)

        val sender = accountDao.getAccountById(fromAccount.id)
            ?: throw IllegalArgumentException("Rekening pengirim tidak ditemukan atau sudah dihapus")
        val receiver = accountDao.getAccountById(toAccount.id)
            ?: throw IllegalArgumentException("Rekening penerima tidak ditemukan atau sudah dihapus")

        require(sender.balance >= safeAmount) {
            "Saldo rekening asal (${sender.name}) tidak mencukupi untuk transfer"
        }

        accountDao.adjustBalance(sender.id, -safeAmount)
        accountDao.adjustBalance(receiver.id, safeAmount)

        val transferTx = TransactionEntity(
            title = "Transfer ke ${receiver.name}",
            amount = safeAmount,
            type = "TRANSFER",
            category = "Transfer",
            date = date,
            accountId = sender.id,
            accountName = sender.name,
            notes = if (notes.isNotBlank()) notes else "Transfer dari ${sender.name} ke ${receiver.name}",
            toAccountId = receiver.id,
            toAccountName = receiver.name
        )
        transactionDao.insertTransaction(transferTx)
    }

    /**
     * Menambahkan rekening baru ke dalam database dan mencatat transaksi saldo awal secara atomik.
     * Jika saldo awal > 0, pembuatan akun dan pencatatan transaksi saldo awal dieksekusi di dalam
     * [runInTransaction] agar akun tidak terbuat tanpa riwayat transaksi jika terjadi kegagalan.
     *
     * @param name Nama akun/rekening (contoh: "BCA", "Dompet Tunai").
     * @param type Jenis akun ("bank", "e-wallet", "cash").
     * @param initialBalance Saldo awal saat pembukaan rekening.
     * @param date Tanggal pencatatan saldo awal (default hari ini yyyy-MM-dd).
     * @return ID unik akun yang baru dibuat.
     */
    suspend fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Long = runInTransaction {
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
        newAccountId
    }

    /**
     * Merekonsiliasi saldo rekening dengan membaca data saldo segar (fresh) langsung dari database
     * di dalam transaksi atomik, menghitung selisih (diff = actualBalance - freshBalance),
     * memperbarui saldo akun, dan mencatat transaksi penyesuaian (INCOME/EXPENSE).
     *
     * Rumus bisnis selisih:
     * - diff = actualBalance - freshAccount.balance
     * - diff > 0 -> Penyesuaian bertipe INCOME (surplus)
     * - diff < 0 -> Penyesuaian bertipe EXPENSE (defisit)
     *
     * @param account Objek referensi akun dari UI (ID digunakan untuk membaca record terbaru).
     * @param actualBalance Saldo fisik nyata yang dimasukkan pengguna.
     * @param date Tanggal rekonsiliasi (default hari ini yyyy-MM-dd).
     * @throws IllegalArgumentException Jika akun tidak ditemukan di database.
     */
    suspend fun reconcileAccount(
        account: AccountEntity,
        actualBalance: Double,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) = runInTransaction {
        val freshAccount = accountDao.getAccountById(account.id)
            ?: throw IllegalArgumentException("Rekening dengan ID ${account.id} tidak ditemukan")

        val safeActualBalance = roundCurrency(actualBalance)
        val diff = roundCurrency(safeActualBalance - freshAccount.balance)
        if (kotlin.math.abs(diff) < 0.001) return@runInTransaction

        accountDao.updateBalance(freshAccount.id, safeActualBalance)

        val adjustmentTx = TransactionEntity(
            title = "Penyesuaian Saldo Sistem",
            amount = kotlin.math.abs(diff),
            type = if (diff > 0) "INCOME" else "EXPENSE",
            category = "Penyesuaian",
            date = date,
            accountId = freshAccount.id,
            accountName = freshAccount.name,
            notes = "Rekonsiliasi: saldo lama ${freshAccount.balance.toLong()}, saldo baru ${safeActualBalance.toLong()}, selisih ${if (diff > 0) "+" else ""}${diff.toLong()}"
        )
        transactionDao.insertTransaction(adjustmentTx)
    }
}
