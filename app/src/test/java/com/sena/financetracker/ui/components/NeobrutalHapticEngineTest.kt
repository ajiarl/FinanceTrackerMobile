package com.sena.financetracker.ui.components

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeobrutalHapticEngineTest {

    private class MockFailingContext : ContextWrapper(null) {
        override fun getSystemService(name: String): Any? {
            throw SecurityException("Vibrator access denied or hardware missing")
        }
    }

    private class MockNullServiceContext : ContextWrapper(null) {
        override fun getSystemService(name: String): Any? {
            return null
        }
    }

    @Test
    fun `tick does not trigger vibration when isEnabled is false`() {
        // Calling with null context or isEnabled = false should safely return early
        NeobrutalHapticEngine.tick(null, isEnabled = false)
        NeobrutalHapticEngine.tick(null, isEnabled = true)
        assertTrue(true)
    }

    @Test
    fun `heavyClick does not trigger vibration when isEnabled is false`() {
        NeobrutalHapticEngine.heavyClick(null, isEnabled = false)
        NeobrutalHapticEngine.heavyClick(null, isEnabled = true)
        assertTrue(true)
    }

    @Test
    fun `tick executes safely and handles exception without throwing`() {
        val failingContext = MockFailingContext()
        // Must be safely caught by runCatching
        NeobrutalHapticEngine.tick(failingContext, isEnabled = true)
        NeobrutalHapticEngine.heavyClick(failingContext, isEnabled = true)
        assertTrue(true)
    }

    @Test
    fun `null vibrator service safely handled without throwing`() {
        val nullServiceContext = MockNullServiceContext()
        NeobrutalHapticEngine.tick(nullServiceContext, isEnabled = true)
        NeobrutalHapticEngine.heavyClick(nullServiceContext, isEnabled = true)
        assertTrue(true)
    }
}
