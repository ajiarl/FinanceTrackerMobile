package com.sena.financetracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitas ekspor data transaksi ke berkas CSV yang sesuai dengan spesifikasi standar RFC 4180
 * dan integrasi native Android Share Sheet melalui FileProvider.
 */
object CsvExporter {

    /**
     * Menghasilkan konten teks CSV murni (RFC 4180) dari daftar entitas transaksi.
     *
     * Header CSV:
     * `ID,Tanggal,Tipe,Judul,Nominal,Kategori,Akun,Catatan`
     *
     * Spesifikasi:
     * - Tanggal: Format ISO (yyyy-MM-dd).
     * - Nominal: Angka numerik murni tanpa simbol mata uang (contoh: 50000 atau 125000.5).
     * - Escape: Nilai yang memuat koma (,), tanda kutip ganda ("), atau karakter baris baru (\n)
     *   akan dibungkus dengan tanda kutip ganda serta tanda kutip internal di-escape menjadi ganda ("").
     *
     * @param transactions Daftar riwayat transaksi yang akan diekspor.
     * @param accounts Daftar akun opsional untuk pemetaan akun jika diperlukan.
     * @param categories Daftar kategori opsional untuk pemetaan kategori jika diperlukan.
     * @return String representasi berkas CSV lengkap dengan header dan baris rekaman.
     */
    fun generateTransactionsCsv(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList()
    ): String {
        val accountMap = accounts.associate { it.id to it.name }
        val categoryMap = categories.associate { it.id to it.name }

        val sb = StringBuilder()
        // Header CSV
        sb.append("ID,Tanggal,Tipe,Judul,Nominal,Kategori,Akun,Catatan\n")

        transactions.forEach { tx ->
            val accountDisplay = if (tx.accountName.isNotBlank()) {
                tx.accountName
            } else {
                accountMap[tx.accountId] ?: "Akun #${tx.accountId}"
            }

            // Format nominal bersih: jika desimal nol (.0), buang desimalnya agar bersih di Excel
            val formattedAmount = if (tx.amount % 1.0 == 0.0) {
                tx.amount.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", tx.amount)
            }

            val row = listOf(
                tx.id.toString(),
                escapeCsvCell(tx.date.take(10)),
                escapeCsvCell(tx.type),
                escapeCsvCell(tx.title),
                formattedAmount,
                escapeCsvCell(tx.category),
                escapeCsvCell(accountDisplay),
                escapeCsvCell(tx.notes)
            )
            sb.append(row.joinToString(",")).append("\n")
        }

        return sb.toString()
    }

    /**
     * Meng-escape string nilai sel CSV sesuai standar RFC 4180.
     * Jika memuat tanda koma, kutip ganda, atau baris baru, sel akan dibungkus kutip ganda
     * dan tanda kutip di dalamnya digandakan (" -> "").
     */
    fun escapeCsvCell(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Menulis konten CSV ke cache direktori `exports/` dan meluncurkan native Android Share Sheet
     * menggunakan [FileProvider].
     *
     * @param context Context Android aktif.
     * @param csvContent Konten teks CSV yang telah digenerate.
     * @param fileName Nama file target (misal: FinanceTracker_Transactions_20261007.csv).
     * @return Berkas [File] hasil penyimpanan jika berhasil ditulis.
     */
    fun exportAndShareCsv(
        context: Context,
        csvContent: String,
        fileName: String = "FinanceTracker_Transactions_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val targetFile = File(exportDir, fileName)
        FileOutputStream(targetFile).use { fos ->
            fos.write(csvContent.toByteArray(Charsets.UTF_8))
            fos.flush()
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, targetFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Ekspor Transaksi Finance Tracker")
            putExtra(Intent.EXTRA_TEXT, "Terlampir cadangan data transaksi keuangan (format CSV).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Bagikan / Simpan Laporan CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)

        return targetFile
    }
}
