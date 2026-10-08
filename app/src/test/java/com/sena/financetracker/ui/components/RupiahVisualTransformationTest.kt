package com.sena.financetracker.ui.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class RupiahVisualTransformationTest {

    private val transformation = RupiahVisualTransformation()

    @Test
    fun testEmptyInput() {
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun testSingleDigits() {
        val result = transformation.filter(AnnotatedString("5"))
        assertEquals("5", result.text.text)
        assertEquals(1, result.offsetMapping.originalToTransformed(1))
        assertEquals(1, result.offsetMapping.transformedToOriginal(1))
    }

    @Test
    fun testThreeDigitsNoSeparator() {
        val result = transformation.filter(AnnotatedString("500"))
        assertEquals("500", result.text.text)
        assertEquals(3, result.offsetMapping.originalToTransformed(3))
        assertEquals(3, result.offsetMapping.transformedToOriginal(3))
    }

    @Test
    fun testFourDigitsOneDot() {
        val result = transformation.filter(AnnotatedString("1000"))
        assertEquals("1.000", result.text.text)
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(2, result.offsetMapping.originalToTransformed(1)) // "1."
        assertEquals(3, result.offsetMapping.originalToTransformed(2)) // "1.0"
        assertEquals(4, result.offsetMapping.originalToTransformed(3)) // "1.00"
        assertEquals(5, result.offsetMapping.originalToTransformed(4)) // "1.000"

        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(1))
        assertEquals(1, result.offsetMapping.transformedToOriginal(2)) // kursor di setelah '.' memetakan ke 1 digit original
        assertEquals(2, result.offsetMapping.transformedToOriginal(3))
        assertEquals(3, result.offsetMapping.transformedToOriginal(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun testMillionsWithMultipleDots() {
        val result = transformation.filter(AnnotatedString("5000000"))
        assertEquals("5.000.000", result.text.text)
        assertEquals(9, result.offsetMapping.originalToTransformed(7))
        assertEquals(7, result.offsetMapping.transformedToOriginal(9))
    }

    @Test
    fun testBillions() {
        val result = transformation.filter(AnnotatedString("1234567890"))
        assertEquals("1.234.567.890", result.text.text)
        assertEquals(13, result.offsetMapping.originalToTransformed(10))
        assertEquals(10, result.offsetMapping.transformedToOriginal(13))
    }

    @Test
    fun testWithDecimalComma() {
        val result = transformation.filter(AnnotatedString("50000,50"))
        assertEquals("50.000,50", result.text.text)
        assertEquals(9, result.offsetMapping.originalToTransformed(8))
        assertEquals(8, result.offsetMapping.transformedToOriginal(9))
    }

    @Test
    fun testWithDecimalDot() {
        val result = transformation.filter(AnnotatedString("50000.50"))
        assertEquals("50.000.50", result.text.text)
        assertEquals(9, result.offsetMapping.originalToTransformed(8))
        assertEquals(8, result.offsetMapping.transformedToOriginal(9))
    }
}
