package com.sena.financetracker.util

import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import org.junit.Assert.assertEquals
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
}
