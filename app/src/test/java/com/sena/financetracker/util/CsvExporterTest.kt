package com.sena.financetracker.util

import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testEmptyTransactionsExport() {
        val csv = CsvExporter.generateTransactionsCsv(emptyList())
        val lines = csv.trim().split("\n")

        assertEquals(1, lines.size)
        assertEquals("ID,Tanggal,Tipe,Judul,Nominal,Kategori,Akun,Catatan", lines[0])
    }

    @Test
    fun testStandardTransactionRowFormat() {
        val transactions = listOf(
            TransactionEntity(
                id = 101,
                title = "Gaji Bulanan",
                amount = 12500000.0,
                type = "INCOME",
                category = "Gaji",
                date = "2026-10-01",
                accountId = 1,
                accountName = "BCA",
                notes = "Bonus proyek"
            )
        )

        val csv = CsvExporter.generateTransactionsCsv(transactions)
        val lines = csv.trim().split("\n")

        assertEquals(2, lines.size)
        assertEquals("ID,Tanggal,Tipe,Judul,Nominal,Kategori,Akun,Catatan", lines[0])
        assertEquals("101,2026-10-01,INCOME,Gaji Bulanan,12500000,Gaji,BCA,Bonus proyek", lines[1])
    }

    @Test
    fun testCsvEscapingForCommasAndQuotesRFC4180() {
        val transactions = listOf(
            TransactionEntity(
                id = 102,
                title = "Makan Siang, Kopi & Snack",
                amount = 75500.5,
                type = "EXPENSE",
                category = "Makanan & Minuman",
                date = "2026-10-02T12:30:00",
                accountId = 2,
                accountName = "GoPay",
                notes = "Beli di \"Kafe Senja\", enak banget\nrekomendasi teman"
            )
        )

        val csv = CsvExporter.generateTransactionsCsv(transactions)
        val lines = csv.trim().split("\n")

        // Baris header
        assertEquals("ID,Tanggal,Tipe,Judul,Nominal,Kategori,Akun,Catatan", lines[0])

        // Judul harus dibungkus tanda kutip karena ada koma
        assertTrue(csv.contains("\"Makan Siang, Kopi & Snack\""))

        // Catatan harus dibungkus tanda kutip dan tanda kutip ganda di-escape menjadi ganda ("")
        assertTrue(csv.contains("\"Beli di \"\"Kafe Senja\"\", enak banget\nrekomendasi teman\""))

        // Nominal dengan desimal bersih (75500.50)
        assertTrue(csv.contains("75500.50"))

        // Tanggal terpotong 10 karakter pertama ISO (2026-10-02)
        assertTrue(csv.contains("2026-10-02"))
    }

    @Test
    fun testFallbackAccountNameFromAccountsList() {
        val accounts = listOf(
            AccountEntity(id = 5, name = "Mandiri Tabungan", type = "bank", balance = 3000000.0)
        )
        val transactions = listOf(
            TransactionEntity(
                id = 103,
                title = "Beli Buku",
                amount = 150000.0,
                type = "EXPENSE",
                category = "Pendidikan",
                date = "2026-10-05",
                accountId = 5,
                accountName = "", // Nama kosong, fallback ke list accounts
                notes = ""
            )
        )

        val csv = CsvExporter.generateTransactionsCsv(transactions, accounts)
        val lines = csv.trim().split("\n")

        assertEquals(2, lines.size)
        assertEquals("103,2026-10-05,EXPENSE,Beli Buku,150000,Pendidikan,Mandiri Tabungan,", lines[1])
    }

    @Test
    fun testEscapeCsvCellDirectly() {
        assertEquals("Normal Text", CsvExporter.escapeCsvCell("Normal Text"))
        assertEquals("\"Text, with comma\"", CsvExporter.escapeCsvCell("Text, with comma"))
        assertEquals("\"Text with \"\"quote\"\"\"", CsvExporter.escapeCsvCell("Text with \"quote\""))
        assertEquals("\"Line1\nLine2\"", CsvExporter.escapeCsvCell("Line1\nLine2"))
    }

    @Test
    fun testCsvFormulaInjectionSanitization() {
        // DDE injection prefixes: =, +, -, @, \t, 
        assertEquals("'=CMD|' /C calc'!A0", CsvExporter.escapeCsvCell("=CMD|' /C calc'!A0"))
        assertEquals("'+1234", CsvExporter.escapeCsvCell("+1234"))
        assertEquals("'-5678", CsvExporter.escapeCsvCell("-5678"))
        assertEquals("'@SUM(A1:A10)", CsvExporter.escapeCsvCell("@SUM(A1:A10)"))
        assertEquals("'\tTAB_VAL", CsvExporter.escapeCsvCell("\tTAB_VAL"))
        
        // Formula injection yang memuat tanda koma juga harus di-quote RFC 4180
        assertEquals("\"'=SUM(A1, B1)\"", CsvExporter.escapeCsvCell("=SUM(A1, B1)"))
    }

    @Test
    fun testExportTransactionsWithFormulaInjectionPayloads() {
        val maliciousTransactions = listOf(
            TransactionEntity(
                id = 999,
                title = "=CMD|' /C calc'!A0",
                amount = 100000.0,
                type = "EXPENSE",
                category = "@AdminAction",
                date = "2026-10-08",
                accountId = 1,
                accountName = "BCA",
                notes = "+628123456789"
            )
        )

        val csv = CsvExporter.generateTransactionsCsv(maliciousTransactions)
        val lines = csv.trim().split("\n")

        assertEquals(2, lines.size)
        // Kolom Judul (=CMD...), Kategori (@AdminAction), dan Catatan (+628...) harus diawali petik tunggal (')
        assertTrue(lines[1].contains("'=CMD|' /C calc'!A0"))
        assertTrue(lines[1].contains("'@AdminAction"))
        assertTrue(lines[1].contains("'+628123456789"))
    }

    @Test
    fun testIsActivityContextDetection() {
        val dummyActivity = object : Activity() {}
        val dummyContext = object : ContextWrapper(dummyActivity) {
            override fun getBaseContext(): Context = dummyActivity
        }
        val nonActivityContext = object : ContextWrapper(null) {
            override fun getBaseContext(): Context? = null
        }

        assertTrue(CsvExporter.isActivityContext(dummyActivity))
        assertTrue(CsvExporter.isActivityContext(dummyContext))
        assertFalse(CsvExporter.isActivityContext(nonActivityContext))
        assertFalse(CsvExporter.isActivityContext(null))
    }

    @Test
    fun testCalculateChooserFlagsOmitsNewTaskFlagForActivityContext() {
        val dummyActivity = object : Activity() {}
        val flags = CsvExporter.calculateChooserFlags(dummyActivity)

        // Verifikasi mitigasi SEC-04: FLAG_ACTIVITY_NEW_TASK TIDAK boleh disertakan pada context Activity
        val hasNewTask = (flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        assertFalse("Flags chooser tidak boleh memiliki FLAG_ACTIVITY_NEW_TASK pada context Activity", hasNewTask)

        // Verifikasi FLAG_GRANT_READ_URI_PERMISSION tetap aktif
        val hasGrantUri = (flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        assertTrue("Flags chooser harus mempertahankan FLAG_GRANT_READ_URI_PERMISSION", hasGrantUri)
    }

    @Test
    fun testCalculateChooserFlagsAddsNewTaskFlagForNonActivityContext() {
        val nonActivityContext = object : ContextWrapper(null) {
            override fun getBaseContext(): Context? = null
        }
        val flags = CsvExporter.calculateChooserFlags(nonActivityContext)

        // Pada non-Activity context (seperti ApplicationContext), FLAG_ACTIVITY_NEW_TASK diperlukan agar tidak crash
        val hasNewTask = (flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        assertTrue("Flags chooser harus memiliki FLAG_ACTIVITY_NEW_TASK pada non-Activity context", hasNewTask)

        val hasGrantUri = (flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        assertTrue("Flags chooser harus mempertahankan FLAG_GRANT_READ_URI_PERMISSION", hasGrantUri)
    }
}
