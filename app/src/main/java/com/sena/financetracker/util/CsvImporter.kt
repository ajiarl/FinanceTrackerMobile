package com.sena.financetracker.util

import com.sena.financetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitas parser berkas CSV transaksi (RFC 4180 compliant) untuk aplikasi Android.
 *
 * Fitur:
 * - Mendukung header kustom / standar (Tanggal, Tipe, Judul/Deskripsi, Nominal/Amount, Kategori, Catatan, Akun).
 * - Penanganan RFC 4180: pemisah tanda koma (,), tanda kutip ganda pembungkus ("..."),
 *   dan escaped quotes ("").
 * - Penanganan multiline sel CSV yang terbungkus tanda kutip.
 * - Sanitasi nilai nominal (membersihkan Rp, spasi, pemisah ribuan titik/koma, dan normalisasi desimal).
 * - Parsing & normalisasi format tanggal (ISO yyyy-MM-dd, dd/MM/yyyy, yyyy/MM/dd, dll).
 * - Validasi baris transaksi & fallback akun/kategori default.
 */
object CsvImporter {

    data class ParsedTransaction(
        val title: String,
        val amount: Double,
        val type: String,
        val category: String,
        val date: String,
        val notes: String = "",
        val rawAccountName: String = "",
        val isValid: Boolean = true,
        val errorMessage: String? = null
    )

    /**
     * Parsing teks CSV mentah menjadi daftar rekaman terstruktur [ParsedTransaction].
     *
     * @param csvContent Konten berkas CSV dalam bentuk String utuh.
     * @param defaultCategory Nama kategori fallback jika baris tidak menyediakan kategori.
     * @return Daftar baris transaksi hasil parsing.
     */
    fun parseCsv(
        csvContent: String,
        defaultCategory: String = "Lainnya"
    ): List<ParsedTransaction> {
        if (csvContent.isBlank()) return emptyList()

        val rows = parseCsvRows(csvContent)
        if (rows.isEmpty()) return emptyList()

        val headerRow = rows.first()
        val headerIndices = resolveHeaderIndices(headerRow)

        val results = mutableListOf<ParsedTransaction>()
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.all { it.isBlank() }) continue // Abaikan baris kosong

            val parsed = parseSingleRow(row, headerIndices, defaultCategory)
            results.add(parsed)
        }

        return results
    }

    /**
     * Memecah string CSV menjadi baris-baris token sel sesuai spesifikasi RFC 4180.
     * Menangani koma di dalam kutip dan baris baru di dalam kutip.
     */
    fun parseCsvRows(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var insideQuote = false
        var i = 0
        val length = csvText.length

        while (i < length) {
            val c = csvText[i]

            if (insideQuote) {
                if (c == '"') {
                    // Cek escaped quote ("")
                    if (i + 1 < length && csvText[i + 1] == '"') {
                        currentCell.append('"')
                        i++ // Lompat 1 karakter
                    } else {
                        insideQuote = false
                    }
                } else {
                    currentCell.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        insideQuote = true
                    }
                    ',' -> {
                        currentRow.add(currentCell.toString().trim())
                        currentCell.clear()
                    }
                    '\r' -> {
                        // Cek \r\n
                        if (i + 1 < length && csvText[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentCell.toString().trim())
                        currentCell.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentCell.toString().trim())
                        currentCell.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> {
                        currentCell.append(c)
                    }
                }
            }
            i++
        }

        // Tambahkan sel terakhir jika ada sisa
        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString().trim())
            rows.add(currentRow.toList())
        }

        return rows
    }

    private data class HeaderIndices(
        val titleIdx: Int = -1,
        val amountIdx: Int = -1,
        val typeIdx: Int = -1,
        val categoryIdx: Int = -1,
        val dateIdx: Int = -1,
        val notesIdx: Int = -1,
        val accountIdx: Int = -1
    )

    private fun resolveHeaderIndices(headerRow: List<String>): HeaderIndices {
        var titleIdx = -1
        var amountIdx = -1
        var typeIdx = -1
        var categoryIdx = -1
        var dateIdx = -1
        var notesIdx = -1
        var accountIdx = -1

        headerRow.forEachIndexed { index, rawHeader ->
            val h = rawHeader.trim().lowercase(Locale.ROOT)
            when {
                h.contains("tanggal") || h.contains("date") || h.contains("waktu") -> if (dateIdx == -1) dateIdx = index
                h.contains("nominal") || h.contains("amount") || h.contains("jumlah") || h.contains("total") -> if (amountIdx == -1) amountIdx = index
                h.contains("tipe") || h.contains("type") || h.contains("jenis") -> if (typeIdx == -1) typeIdx = index
                h.contains("judul") || h.contains("title") || h.contains("deskripsi") || h.contains("description") || h.contains("nama") -> if (titleIdx == -1) titleIdx = index
                h.contains("kategori") || h.contains("category") -> if (categoryIdx == -1) categoryIdx = index
                h.contains("catatan") || h.contains("notes") || h.contains("keterangan") || h.contains("memo") -> if (notesIdx == -1) notesIdx = index
                h.contains("akun") || h.contains("account") || h.contains("rekening") -> if (accountIdx == -1) accountIdx = index
            }
        }

        // Fallback urutan standar jika tidak cocok: Tanggal, Tipe, Judul, Nominal, Kategori, Akun, Catatan
        if (dateIdx == -1 && headerRow.isNotEmpty()) dateIdx = 0
        if (amountIdx == -1 && headerRow.size > 3) amountIdx = 3

        return HeaderIndices(titleIdx, amountIdx, typeIdx, categoryIdx, dateIdx, notesIdx, accountIdx)
    }

    private fun parseSingleRow(
        row: List<String>,
        indices: HeaderIndices,
        defaultCategory: String
    ): ParsedTransaction {
        val rawDate = row.getOrNull(indices.dateIdx)?.trim().orEmpty()
        val rawTitle = row.getOrNull(indices.titleIdx)?.trim().orEmpty()
        val rawAmount = row.getOrNull(indices.amountIdx)?.trim().orEmpty()
        val rawType = row.getOrNull(indices.typeIdx)?.trim().orEmpty()
        val rawCategory = row.getOrNull(indices.categoryIdx)?.trim().orEmpty()
        val rawNotes = row.getOrNull(indices.notesIdx)?.trim().orEmpty()
        val rawAccount = row.getOrNull(indices.accountIdx)?.trim().orEmpty()

        val cleanAmount = sanitizeAmount(rawAmount)
        val cleanDate = normalizeDate(rawDate)
        val cleanType = normalizeType(rawType, cleanAmount)
        val cleanTitle = if (rawTitle.isNotBlank()) rawTitle else "Transaksi Tanpa Judul"
        val cleanCategory = if (rawCategory.isNotBlank()) rawCategory else defaultCategory

        var isValid = true
        var errorMsg: String? = null

        if (cleanAmount == null || cleanAmount <= 0.0) {
            isValid = false
            errorMsg = "Nominal tidak valid atau kosong ($rawAmount)"
        }

        return ParsedTransaction(
            title = cleanTitle,
            amount = cleanAmount ?: 0.0,
            type = cleanType,
            category = cleanCategory,
            date = cleanDate,
            notes = rawNotes,
            rawAccountName = rawAccount,
            isValid = isValid,
            errorMessage = errorMsg
        )
    }

    /**
     * Membersihkan dan menormalisasi teks nominal ke Double.
     * Menangani simbol mata uang (Rp, IDR, $), pemisah ribuan (titik atau koma),
     * dan format angka desimal.
     */
    fun sanitizeAmount(raw: String): Double? {
        if (raw.isBlank()) return null

        var cleaned = raw.trim()
            .replace("Rp", "", ignoreCase = true)
            .replace("IDR", "", ignoreCase = true)
            .replace("$", "")
            .replace(" ", "")

        // Tangani tanda minus atau kurung kredit misal (50000)
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length - 1)
        }
        cleaned = cleaned.replace("-", "")

        if (cleaned.isEmpty()) return null

        // Cek pola pemisah ribuan dan desimal:
        // Pola Indo/Eropa: 50.000,50 atau 50.000
        // Pola US: 50,000.50 atau 50,000
        return try {
            if (cleaned.contains(",") && cleaned.contains(".")) {
                val lastComma = cleaned.lastIndexOf(',')
                val lastDot = cleaned.lastIndexOf('.')
                if (lastComma > lastDot) {
                    // Koma adalah pemisah desimal (misal 50.000,00)
                    cleaned = cleaned.replace(".", "").replace(",", ".")
                } else {
                    // Titik adalah pemisah desimal (misal 50,000.00)
                    cleaned = cleaned.replace(",", "")
                }
            } else if (cleaned.contains(",")) {
                // Hanya koma: jika koma diikuti 1-2 digit di akhir dan panjangnya <= 2 (misal 50,5 atau 50,50)
                val parts = cleaned.split(",")
                if (parts.size == 2 && parts[1].length in 1..2) {
                    cleaned = cleaned.replace(",", ".")
                } else {
                    // Koma adalah pemisah ribuan
                    cleaned = cleaned.replace(",", "")
                }
            } else if (cleaned.contains(".")) {
                // Hanya titik: jika bagian setelah titik ada 3 digit (misal 50.000), ini kemungkinan ribuan
                val parts = cleaned.split(".")
                if (parts.size > 2 || (parts.size == 2 && parts[1].length == 3)) {
                    cleaned = cleaned.replace(".", "")
                }
                // Jika parts[1].length 1-2, biarkan sebagai desimal
            }

            cleaned.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Menormalisasi tanggal ke format ISO (yyyy-MM-dd).
     */
    fun normalizeDate(rawDate: String): String {
        if (rawDate.isBlank()) {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        val trimmed = rawDate.trim()
        // Jika sudah ISO yyyy-MM-dd
        if (trimmed.matches(Regex("^\\d{4}-\\d{2}-\\d{2}.*"))) {
            return trimmed.take(10)
        }

        val patterns = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "d/M/yyyy",
            "d-M-yyyy",
            "dd MMMM yyyy",
            "dd MMM yyyy"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale("id", "ID"))
                sdf.isLenient = false
                val date = sdf.parse(trimmed)
                if (date != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                }
            } catch (_: Exception) {
                // Coba dengan locale English
                try {
                    val sdfEn = SimpleDateFormat(pattern, Locale.US)
                    sdfEn.isLenient = false
                    val date = sdfEn.parse(trimmed)
                    if (date != null) {
                        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                    }
                } catch (_: Exception) {
                    // Lanjut pola berikutnya
                }
            }
        }

        // Fallback tanggal hari ini jika gagal diparsing
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Menentukan tipe transaksi (INCOME atau EXPENSE).
     */
    fun normalizeType(rawType: String, amount: Double?): String {
        val t = rawType.trim().uppercase(Locale.ROOT)
        return when {
            t.contains("INCOME") || t.contains("PEMASUKAN") || t.contains("MASUK") || t.contains("KREDIT") || t.contains("CR") -> "INCOME"
            t.contains("EXPENSE") || t.contains("PENGELUARAN") || t.contains("KELUAR") || t.contains("DEBIT") || t.contains("DB") -> "EXPENSE"
            else -> "EXPENSE" // Default transaksi pengeluaran
        }
    }

    /**
     * Konversi daftar [ParsedTransaction] yang valid ke entitas Room [TransactionEntity].
     */
    fun toTransactionEntities(
        parsedList: List<ParsedTransaction>,
        targetAccountId: Long,
        targetAccountName: String
    ): List<TransactionEntity> {
        return parsedList
            .filter { it.isValid }
            .map { p ->
                TransactionEntity(
                    title = p.title,
                    amount = p.amount,
                    type = p.type,
                    category = p.category,
                    date = p.date,
                    accountId = targetAccountId,
                    accountName = targetAccountName,
                    notes = p.notes
                )
            }
    }
}
