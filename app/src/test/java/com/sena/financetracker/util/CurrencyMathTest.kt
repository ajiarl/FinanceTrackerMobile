package com.sena.financetracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyMathTest {

    @Test
    fun testFloatingPointDriftRounding() {
        // Cacat representasi IEEE 754: 0.1 + 0.2 = 0.30000000000000004
        val rawSum = 0.1 + 0.2
        val rounded = CurrencyMath.roundCurrency(rawSum)
        assertEquals(0.30, rounded, 0.0)
    }

    @Test
    fun testToRupiahLongRoundingThreshold() {
        // Memastikan tidak ada truncation bias dari desimal mikro
        val amountJustBelow = 49999.999
        val amountRounded = CurrencyMath.toRupiahLong(amountJustBelow)
        assertEquals(50000L, amountRounded)

        val amountExactHalf = 50000.5
        val halfRounded = CurrencyMath.toRupiahLong(amountExactHalf)
        assertEquals(50001L, halfRounded)
    }

    @Test
    fun testSymmetricNegativeRounding() {
        val negativeAmount = -49999.999
        val roundedLong = CurrencyMath.toRupiahLong(negativeAmount)
        assertEquals(-50000L, roundedLong)

        val negativeFloat = -0.1 - 0.2
        val roundedFloat = CurrencyMath.roundCurrency(negativeFloat)
        assertEquals(-0.30, roundedFloat, 0.0)
    }

    @Test
    fun testStandardFinancialDecimals() {
        assertEquals(1234.56, CurrencyMath.roundCurrency(1234.556), 0.0)
        assertEquals(1234.55, CurrencyMath.roundCurrency(1234.554), 0.0)
        assertEquals(0.0, CurrencyMath.roundCurrency(0.0), 0.0)
    }
}
