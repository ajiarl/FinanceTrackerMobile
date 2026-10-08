package com.sena.financetracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testFormatRupiahZero() {
        val result = formatRupiah(0.0)
        assertEquals("Rp 0", result)
    }

    @Test
    fun testFormatRupiahPositiveStandard() {
        val result = formatRupiah(50000.0)
        assertEquals("Rp 50.000", result)
    }

    @Test
    fun testFormatRupiahMillions() {
        val result = formatRupiah(1250000.0)
        assertEquals("Rp 1.250.000", result)
    }

    @Test
    fun testFormatRupiahNegative() {
        val result = formatRupiah(-25000.0)
        assertEquals("-Rp 25.000", result)
    }

    @Test
    fun testFormatRupiahPrecisionSanitizer() {
        // Angka 49999.9999 seharusnya dibulatkan menjadi 50.000 bukan terpotong ke 49.999
        val result = formatRupiah(49999.9999)
        assertEquals("Rp 50.000", result)
    }

    @Test
    fun testFormatCompactAmountZero() {
        assertEquals("0", formatCompactAmount(0.0))
        assertEquals("0", formatCompactAmount(-0.0))
        assertEquals("0", formatCompactAmount(0.4))
    }

    @Test
    fun testFormatCompactAmountThousands() {
        assertEquals("750rb", formatCompactAmount(750000.0))
        assertEquals("10rb", formatCompactAmount(10000.0))
        assertEquals("1rb", formatCompactAmount(1000.0))
        assertEquals("1.5rb", formatCompactAmount(1500.0))
    }

    @Test
    fun testFormatCompactAmountMillions() {
        assertEquals("1.2jt", formatCompactAmount(1200000.0))
        assertEquals("1jt", formatCompactAmount(1000000.0))
        assertEquals("2.5jt", formatCompactAmount(2500000.0))
        assertEquals("50jt", formatCompactAmount(50000000.0))
    }

    @Test
    fun testFormatCompactAmountBillions() {
        assertEquals("1M", formatCompactAmount(1000000000.0))
        assertEquals("1.2M", formatCompactAmount(1200000000.0))
        assertEquals("2.5M", formatCompactAmount(2500000000.0))
    }

    @Test
    fun testFormatCompactAmountBelowThousand() {
        assertEquals("500", formatCompactAmount(500.0))
        assertEquals("999", formatCompactAmount(999.0))
        assertEquals("50", formatCompactAmount(50.0))
    }

    @Test
    fun testFormatCompactAmountNegative() {
        assertEquals("-1.2jt", formatCompactAmount(-1200000.0))
        assertEquals("-750rb", formatCompactAmount(-750000.0))
        assertEquals("-1M", formatCompactAmount(-1000000000.0))
    }

    @Test
    fun testFormatCompactAmountNoTrailingZero() {
        assertEquals("5jt", formatCompactAmount(5000000.0))
        assertEquals("3rb", formatCompactAmount(3000.0))
        assertEquals("4M", formatCompactAmount(4000000000.0))
    }
}
