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
}
