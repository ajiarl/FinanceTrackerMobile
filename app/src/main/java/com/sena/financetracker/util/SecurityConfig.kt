package com.sena.financetracker.util

import android.content.Context
import com.sena.financetracker.security.ApiKeyStorage

/**
 * Konfigurasi keamanan dan gerbang kredensial aplikasi.
 *
 * Meneruskan panggilan pengambilan API Key ke [ApiKeyStorage] yang memanfaatkan
 * [androidx.security.crypto.EncryptedSharedPreferences] dan Android Keystore hardware (SEC-01).
 */
object SecurityConfig {

    /**
     * Mengambil Groq API Key dari [ApiKeyStorage].
     *
     * @param context Context Android opsional untuk membaca EncryptedSharedPreferences.
     * @return Groq API Key dalam bentuk String.
     */
    fun getGroqApiKey(context: Context? = null): String {
        return if (context != null) {
            ApiKeyStorage.getGroqApiKey(context)
        } else {
            ApiKeyStorage.getGroqApiKey()
        }
    }

    /**
     * De-obfuscation algoritma untuk XOR-masked byte array (dipertahankan untuk backwards compatibility pengujian).
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
        } catch (e: Throwable) {
            android.util.Log.e("FinanceTracker", "Gagal de-obfuscate masked key in-memory", e)
            ""
        }
    }
}
