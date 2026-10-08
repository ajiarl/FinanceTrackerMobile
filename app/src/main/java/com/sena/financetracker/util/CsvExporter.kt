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
     * Karakter berisiko eksekusi formula spreadsheet / Dynamic Data Exchange (DDE).
     */
     private val FORMULA_CHARS = charArrayOf('=', '+', '-', '@', '\u0009', '\u000D')

    /**
     * Meng-escape string nilai sel CSV sesuai standar RFC 4180 serta memitigasi
     * celah keamanan CSV Formula Injection (DDE / OWASP CSV Injection).
     *
     * Jika string diawali dengan karakter risiko formula (`=`, `+`, `-`, `@`, tab, CR),
     * nilai akan diawali dengan tanda petik tunggal (`'`) agar spreadsheet (Excel/Calc)
     * memperlakukannya secara ketat sebagai teks polos alih-alih mengeksekusi formula atau macro.
     * Selanjutnya, jika memuat tanda koma, kutip ganda, atau baris baru, sel akan dibungkus
     * tanda kutip ganda dan tanda kutip internal di dalamnya digandakan (`" -> ""`).
     *
     * @param value Nilai mentah sel teks yang akan diekspor.
     * @return String nilai sel yang telah disanitasi dan di-escape.
      */
     fun escapeCsvCell(value: String): String {
         val sanitized = if (value.isNotEmpty() && value.first() in FORMULA_CHARS) {
             "'$value"
         } else {
             value
         }
         val needsQuotes = sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\u000D")
         return if (needsQuotes) {
             "\"" + sanitized.replace("\"", "\"\"") + "\""
         } else {
             sanitized
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

        val chooser = createChooserIntent(context, shareIntent, "Bagikan / Simpan Laporan CSV")
        context.startActivity(chooser)

        return targetFile
    }

    /**
     * Menghitung bitmask flags yang aman untuk Intent Chooser berdasarkan konteks pemanggil.
     *
     * Mitigasi SEC-04:
     * - Pada konteks [Activity], [Intent.FLAG_ACTIVITY_NEW_TASK] TIDAK disertakan untuk mencegah
     *   window / task hijacking dan anomali navigasi back-stack.
     * - Pada konteks non-Activity (misal Application Context), [Intent.FLAG_ACTIVITY_NEW_TASK]
     *   disertakan agar pemanggilan startActivity tidak melempar crash.
     * - Flag [Intent.FLAG_GRANT_READ_URI_PERMISSION] selalu disertakan.
     *
     * @param context Konteks Android pemanggil.
     * @return Bitmask flags yang aman.
     */
    fun calculateChooserFlags(context: Context): Int {
        var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (!isActivityContext(context)) {
            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return flags
    }

    /**
     * Membangun Intent Chooser untuk berbagi dokumen atau berkas CSV.
     *
     * Mitigasi SEC-04:
     * - Menetapkan flags aman melalui [calculateChooserFlags].
     *
     * @param context Context Android aktif pemanggil.
     * @param targetIntent Intent target yang akan dibungkus chooser (misal ACTION_SEND).
     * @param chooserTitle Judul dialog pemilih aplikasi (chooser).
     * @return Intent chooser yang telah dikonfigurasi secara aman.
     */
    fun createChooserIntent(
        context: Context,
        targetIntent: Intent,
        chooserTitle: CharSequence = "Bagikan / Simpan Laporan CSV"
    ): Intent {
        val flags = calculateChooserFlags(context)
        val chooser = (Intent.createChooser(targetIntent, chooserTitle) ?: Intent(Intent.ACTION_CHOOSER)).apply {
            addFlags(flags)
        }
        return chooser
    }

    /**
     * Memeriksa apakah [context] merupakan instance dari [android.app.Activity],
     * termasuk jika terbungkus dalam hierarki [android.content.ContextWrapper].
     *
     * @param context Context yang akan diperiksa.
     * @return `true` jika context adalah Activity, `false` jika bukan.
     */
    fun isActivityContext(context: Context?): Boolean {
        if (context == null) return false
        var current: Context? = context
        val visited = HashSet<Context>()
        while (current != null) {
            if (current is android.app.Activity) {
                return true
            }
            if (current is android.content.ContextWrapper) {
                if (!visited.add(current)) break
                val base = try {
                    current.baseContext
                } catch (e: Exception) {
                    null
                }
                if (base == null || base == current) break
                current = base
            } else {
                break
            }
        }
        return current is android.app.Activity
    }
}
