package com.sena.financetracker.util

import com.sena.financetracker.BuildConfig

/**
 * Runtime Security Configuration & In-Memory De-obfuscation Engine.
 * Conceals API keys from static DEX analysis by storing them as XOR-masked byte arrays
 * and de-obfuscating dynamically at runtime.
 */
object SecurityConfig {

    /**
     * De-obfuscates the masked GROQ API Key using the compile-time XOR salt.
     * Returns an empty string safely if the payload is empty or invalid.
     */
    fun getGroqApiKey(): String {
        return deobfuscate(BuildConfig.GROQ_KEY_MASKED, BuildConfig.GROQ_KEY_SALT)
    }

    /**
     * Pure de-obfuscation algorithm for XOR-masked byte buffers.
     */
    fun deobfuscate(maskedBytes: ByteArray?, salt: Byte): String {
        if (maskedBytes == null || maskedBytes.isEmpty()) {
            return ""
        }
        return try {
            val decoded = ByteArray(maskedBytes.size) { i ->
                (maskedBytes[i].toInt() xor salt.toInt()).toByte()
            }
            String(decoded, Charsets.UTF_8).trim()
        } catch (_: Throwable) {
            ""
        }
    }
}
