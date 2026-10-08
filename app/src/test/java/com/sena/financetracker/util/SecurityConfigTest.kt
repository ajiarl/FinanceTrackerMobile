package com.sena.financetracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityConfigTest {

    @Test
    fun `deobfuscate returns exact original string when masked with salt`() {
        val originalKey = "gsk_test_api_key_1234567890abcdef"
        val salt: Byte = 0x5A
        val maskedBytes = originalKey.toByteArray(Charsets.UTF_8).map {
            (it.toInt() xor salt.toInt()).toByte()
        }.toByteArray()

        val decoded = SecurityConfig.deobfuscate(maskedBytes, salt)
        assertEquals(originalKey, decoded)
    }

    @Test
    fun `deobfuscate returns empty string when input is null or empty`() {
        val salt: Byte = 0x5A
        assertEquals("", SecurityConfig.deobfuscate(null, salt))
        assertEquals("", SecurityConfig.deobfuscate(byteArrayOf(), salt))
    }

    @Test
    fun `getGroqApiKey returns non-null string without exception`() {
        // BuildConfig.GROQ_KEY_MASKED either contains the local.properties key or empty
        val key = SecurityConfig.getGroqApiKey()
        // Must execute safely and return a valid String instance
        org.junit.Assert.assertNotNull(key)
    }
}
