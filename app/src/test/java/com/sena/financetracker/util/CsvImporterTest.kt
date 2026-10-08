package com.sena.financetracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk memverifikasi parser RFC 4180 [CsvImporter], sanitasi nominal desimal,
 * penanganan escaping tanda kutip ganda, multiline cells, dan normalisasi tanggal.
 */
class CsvImporterTest {

    @Test
    fun parseCsvRows_handlesQuotesCommasAndEscapedQuotes() {
        val q = "\""
        val raw = "Tanggal,Tipe,Judul,Nominal,Kategori,Catatan\n" +
                "2026-03-01,EXPENSE," + q + "Makan Siang, " + q + q + "Spesial" + q + q + q + ",50000,Makanan," + q + "Catatan biasa" + q + "\n" +
                "2026-03-02,INCOME,Gaji Bulanan,5000000,Gaji,Transfer kantor"

        val rows = CsvImporter.parseCsvRows(raw)
        assertEquals(3, rows.size)

        // Verifikasi row 2 (baris dengan koma di dalam teks dan escaped quote "")
        val row2 = rows[1]
        assertEquals("2026-03-01", row2[0])
        assertEquals("EXPENSE", row2[1])
        assertEquals("Makan Siang, \"Spesial\"", row2[2])
        assertEquals("50000", row2[3])
        assertEquals("Makanan", row2[4])
        assertEquals("Catatan biasa", row2[5])
    }

    @Test
    fun parseCsv_parsesStandardTransactionsSuccessfully() {
        val raw = "Tanggal,Tipe,Judul,Nominal,Kategori,Catatan\n" +
                "2026-03-10,EXPENSE,Beli Kopi,25000,Minuman,Kopi susu\n" +
                "2026-03-11,INCOME,Bonus Proyek,1500000,Bonus,Bonus freelance"

        val parsed = CsvImporter.parseCsv(raw)
        assertEquals(2, parsed.size)

        val tx1 = parsed[0]
        assertEquals("Beli Kopi", tx1.title)
        assertEquals(25000.0, tx1.amount, 0.001)
        assertEquals("EXPENSE", tx1.type)
        assertEquals("Minuman", tx1.category)
        assertEquals("2026-03-10", tx1.date)
        assertEquals("Kopi susu", tx1.notes)
        assertTrue(tx1.isValid)

        val tx2 = parsed[1]
        assertEquals("Bonus Proyek", tx2.title)
        assertEquals(1500000.0, tx2.amount, 0.001)
        assertEquals("INCOME", tx2.type)
        assertEquals("Bonus", tx2.category)
        assertEquals("2026-03-11", tx2.date)
        assertTrue(tx2.isValid)
    }

    @Test
    fun sanitizeAmount_cleansIndonesianAndUsCurrencies() {
        // Format biasa
        assertEquals(50000.0, CsvImporter.sanitizeAmount("50000")!!, 0.001)
        // Format dengan simbol Rp dan titik ribuan
        assertEquals(150000.0, CsvImporter.sanitizeAmount("Rp 150.000")!!, 0.001)
        // Format desimal koma Indonesia (150.000,50)
        assertEquals(150000.5, CsvImporter.sanitizeAmount("150.000,50")!!, 0.001)
        // Format standar US koma ribuan (1,500,000.75)
        assertEquals(1500000.75, CsvImporter.sanitizeAmount("1,500,000.75")!!, 0.001)
        // Tanda minus dan kurung kredit
        assertEquals(75000.0, CsvImporter.sanitizeAmount("-75000")!!, 0.001)
        assertEquals(25000.0, CsvImporter.sanitizeAmount("(25000)")!!, 0.001)
        // String kosong atau invalid
        assertNull(CsvImporter.sanitizeAmount(""))
        assertNull(CsvImporter.sanitizeAmount("bukan_angka"))
    }

    @Test
    fun sanitizeAmount_handlesVariedThousandAndDecimalFormatsAccurately() {
        // Mitigasi DAT-08: Format Rp dengan titik (sebelumnya terpotong menjadi 0.5 karena "Rp.")
        assertEquals(50000.0, CsvImporter.sanitizeAmount("Rp. 50000")!!, 0.001)
        assertEquals(50000.0, CsvImporter.sanitizeAmount("Rp. 50.000")!!, 0.001)
        assertEquals(1000000.5, CsvImporter.sanitizeAmount("Rp. 1.000.000,50")!!, 0.001)

        // Akhiran sen bulat khas Indonesia (,- atau ,--)
        assertEquals(50000.0, CsvImporter.sanitizeAmount("Rp 50.000,-")!!, 0.001)
        assertEquals(50000.0, CsvImporter.sanitizeAmount("50000,-")!!, 0.001)
        assertEquals(1500000.0, CsvImporter.sanitizeAmount("1.500.000,--")!!, 0.001)

        // Variasi pemisah ribuan murni (titik dan koma multi-kelompok & miliaran)
        assertEquals(1000000.0, CsvImporter.sanitizeAmount("1.000.000")!!, 0.001)
        assertEquals(1000000000.0, CsvImporter.sanitizeAmount("1.000.000.000")!!, 0.001)
        assertEquals(1000000.0, CsvImporter.sanitizeAmount("1,000,000")!!, 0.001)
        assertEquals(1000000000.0, CsvImporter.sanitizeAmount("1,000,000,000")!!, 0.001)

        // Pemisah spasi standar akuntansi internasional (1 000 000)
        assertEquals(1000000.0, CsvImporter.sanitizeAmount("1 000 000")!!, 0.001)
        assertEquals(1000000.5, CsvImporter.sanitizeAmount("1 000 000,50")!!, 0.001)
        assertEquals(1000000.5, CsvImporter.sanitizeAmount("1 000 000.50")!!, 0.001)

        // Berbagai simbol mata uang asing dan kode IDR
        assertEquals(50000.0, CsvImporter.sanitizeAmount("IDR 50.000")!!, 0.001)
        assertEquals(50000.0, CsvImporter.sanitizeAmount("IDR. 50000")!!, 0.001)
        assertEquals(1500.5, CsvImporter.sanitizeAmount("$ 1,500.50")!!, 0.001)
        assertEquals(2500.25, CsvImporter.sanitizeAmount("€ 2.500,25")!!, 0.001)
        assertEquals(50000.0, CsvImporter.sanitizeAmount("¥ 50,000")!!, 0.001)

        // Format CSV hasil mitigasi Formula Injection (diawali kutip ')
        assertEquals(50000.0, CsvImporter.sanitizeAmount("'+50.000")!!, 0.001)
        assertEquals(1000000.0, CsvImporter.sanitizeAmount("'-1.000.000")!!, 0.001)
        assertEquals(50000.0, CsvImporter.sanitizeAmount("'50000")!!, 0.001)

        // Desimal murni dan angka kecil
        assertEquals(0.5, CsvImporter.sanitizeAmount("0,5")!!, 0.001)
        assertEquals(0.5, CsvImporter.sanitizeAmount("0.5")!!, 0.001)
        assertEquals(0.75, CsvImporter.sanitizeAmount("0,75")!!, 0.001)
        assertEquals(0.75, CsvImporter.sanitizeAmount("0.75")!!, 0.001)
        assertEquals(1000.0, CsvImporter.sanitizeAmount("1000.00")!!, 0.001)
        assertEquals(1000.0, CsvImporter.sanitizeAmount("1000,00")!!, 0.001)
        assertEquals(1000.5, CsvImporter.sanitizeAmount("1000.5")!!, 0.001)
        assertEquals(1000.5, CsvImporter.sanitizeAmount("1000,5")!!, 0.001)

        // Input tidak valid
        assertNull(CsvImporter.sanitizeAmount("   "))
        assertNull(CsvImporter.sanitizeAmount("Rp. "))
        assertNull(CsvImporter.sanitizeAmount("NaN"))
        assertNull(CsvImporter.sanitizeAmount("Infinity"))
    }

    @Test
    fun normalizeDate_convertsVariousFormatsToIso() {
        assertEquals("2026-03-15", CsvImporter.normalizeDate("2026-03-15"))
        assertEquals("2026-03-15", CsvImporter.normalizeDate("15/03/2026"))
        assertEquals("2026-03-15", CsvImporter.normalizeDate("15-03-2026"))
        assertEquals("2026-03-15", CsvImporter.normalizeDate("2026/03/15"))
    }

    @Test
    fun normalizeDate_rejectsInvalidDatesStrictly() {
        // Tanggal 30 Februari (tidak valid)
        assertNull(CsvImporter.normalizeDate("2026-02-30"))
        assertNull(CsvImporter.normalizeDate("30/02/2026"))
        assertNull(CsvImporter.normalizeDate("30-02-2026"))
        // Tanggal 31 April (April hanya ada 30 hari)
        assertNull(CsvImporter.normalizeDate("2026-04-31"))
        assertNull(CsvImporter.normalizeDate("31/04/2026"))
        // Format dan nilai tidak masuk akal
        assertNull(CsvImporter.normalizeDate("2026-13-45"))
        assertNull(CsvImporter.normalizeDate("bukan-tanggal"))
    }

    @Test
    fun parseCsv_flagsInvalidDateAsNotValid() {
        val raw = "Tanggal,Tipe,Judul,Nominal,Kategori\n" +
                "2026-02-30,EXPENSE,Tanggal Februari Salah,50000,Makanan\n" +
                "31/04/2026,EXPENSE,Tanggal April Salah,25000,Makanan"

        val parsed = CsvImporter.parseCsv(raw)
        assertEquals(2, parsed.size)
        assertFalse(parsed[0].isValid)
        assertTrue(parsed[0].errorMessage?.contains("Tanggal tidak valid") == true)
        assertFalse(parsed[1].isValid)
        assertTrue(parsed[1].errorMessage?.contains("Tanggal tidak valid") == true)
    }

    @Test
    fun normalizeType_identifiesIncomeAndExpense() {
        assertEquals("INCOME", CsvImporter.normalizeType("INCOME", 1000.0))
        assertEquals("INCOME", CsvImporter.normalizeType("Pemasukan", 1000.0))
        assertEquals("INCOME", CsvImporter.normalizeType("Kredit", 1000.0))
        assertEquals("EXPENSE", CsvImporter.normalizeType("EXPENSE", 1000.0))
        assertEquals("EXPENSE", CsvImporter.normalizeType("Pengeluaran", 1000.0))
        assertEquals("EXPENSE", CsvImporter.normalizeType("Debit", 1000.0))
        assertEquals("EXPENSE", CsvImporter.normalizeType("", 1000.0)) // Default fallback
    }

    @Test
    fun parseCsv_flagsInvalidAmountAsNotValid() {
        val raw = "Tanggal,Tipe,Judul,Nominal,Kategori\n" +
                "2026-03-10,EXPENSE,Uji Salah,NOL,Lainnya"

        val parsed = CsvImporter.parseCsv(raw)
        assertEquals(1, parsed.size)
        assertFalse(parsed[0].isValid)
        assertNotNull(parsed[0].errorMessage)
    }

    @Test
    fun toTransactionEntities_mapsValidRowsCorrectly() {
        val raw = "Tanggal,Tipe,Judul,Nominal,Kategori\n" +
                "2026-03-10,EXPENSE,Makan Malam,45000,Makanan\n" +
                "2026-03-11,EXPENSE,Gagal,KOSONG,Makanan"

        val parsed = CsvImporter.parseCsv(raw)
        val entities = CsvImporter.toTransactionEntities(
            parsedList = parsed,
            targetAccountId = 2L,
            targetAccountName = "Bank BCA"
        )

        assertEquals(1, entities.size)
        val entity = entities[0]
        assertEquals("Makan Malam", entity.title)
        assertEquals(45000.0, entity.amount, 0.001)
        assertEquals("EXPENSE", entity.type)
        assertEquals("Makanan", entity.category)
        assertEquals("2026-03-10", entity.date)
        assertEquals(2L, entity.accountId)
        assertEquals("Bank BCA", entity.accountName)
    }
}
