package com.sena.financetracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyParserTest {

    @Test
    fun testIndonesianThousandsDot() {
        assertEquals(50000.0, CurrencyParser.parseCurrencyInput("50.000"), 0.001)
        assertEquals(1500000.0, CurrencyParser.parseCurrencyInput("1.500.000"), 0.001)
        assertEquals(10000000.0, CurrencyParser.parseCurrencyInput("10.000.000"), 0.001)
    }

    @Test
    fun testIndonesianDecimalComma() {
        assertEquals(50000.50, CurrencyParser.parseCurrencyInput("50000,50"), 0.001)
        assertEquals(50.5, CurrencyParser.parseCurrencyInput("50,5"), 0.001)
        assertEquals(50000.50, CurrencyParser.parseCurrencyInput("50.000,50"), 0.001)
    }

    @Test
    fun testInternationalFormat() {
        assertEquals(50000.50, CurrencyParser.parseCurrencyInput("50,000.50"), 0.001)
        assertEquals(50000.0, CurrencyParser.parseCurrencyInput("50,000"), 0.001)
    }

    @Test
    fun testPrefixAndWhitespace() {
        assertEquals(25000.0, CurrencyParser.parseCurrencyInput("Rp 25.000"), 0.001)
        assertEquals(25000.0, CurrencyParser.parseCurrencyInput("rp 25.000"), 0.001)
        assertEquals(10000.0, CurrencyParser.parseCurrencyInput("  10000  "), 0.001)
        assertEquals(10000.0, CurrencyParser.parseCurrencyInput("Rp10.000"), 0.001)
    }

    @Test
    fun testDecimalsWithDot() {
        assertEquals(50.5, CurrencyParser.parseCurrencyInput("50.5"), 0.001)
        assertEquals(50.25, CurrencyParser.parseCurrencyInput("50.25"), 0.001)
    }

    @Test
    fun testInvalidAndEmpty() {
        assertEquals(0.0, CurrencyParser.parseCurrencyInput(""), 0.001)
        assertEquals(0.0, CurrencyParser.parseCurrencyInput("   "), 0.001)
        assertEquals(0.0, CurrencyParser.parseCurrencyInput("abc"), 0.001)
        assertEquals(0.0, CurrencyParser.parseCurrencyInput("Rp -"), 0.001)
    }
}
