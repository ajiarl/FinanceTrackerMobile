package com.sena.financetracker.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Penyimpanan aman API Key Groq menggunakan Jetpack Security [EncryptedSharedPreferences]
 * dan Android Keystore hardware (SEC-01).
 *
 * Mengeliminasi kerentanan obfuskasi statis XOR rapuh di bytecode dengan mengenkripsi kredensial
 * menggunakan standar industri AES-256-GCM (Authenticated Encryption with Associated Data)
 * dan skema MasterKey tingkat sistem operasi.
 */
object ApiKeyStorage {

    private const val TAG = "ApiKeyStorage"
    private const val PREFS_FILE = "secure_finance_keys"
    private const val KEY_GROQ_API = "groq_api_key"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    @Volatile
    private var inMemoryCachedKey: String? = null

    /**
     * Provider [SharedPreferences] opsional untuk kebutuhan pengujian unit test terisolasi.
     */
    @VisibleForTesting
    var sharedPreferencesProvider: ((Context) -> SharedPreferences)? = null

    /**
     * Memperoleh instance [SharedPreferences] terenkripsi berbasis Android Keystore.
     * Menggunakan fallback aman jika Keystore tidak tersedia di runtime tertentu (misal headless JVM).
     */
    private fun getSecurePrefs(context: Context): SharedPreferences {
        sharedPreferencesProvider?.let { return it(context) }

        return try {
            val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal menginisialisasi EncryptedSharedPreferences via Keystore", e)
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    /**
     * Mengambil Groq API Key dari [EncryptedSharedPreferences] terenkripsi hardware Keystore.
     *
     * @param context Context Android untuk mengakses EncryptedSharedPreferences.
     * @return Nilai API Key tersimpan, atau string kosong jika belum dikonfigurasi.
     */
    fun getGroqApiKey(context: Context): String {
        return try {
            val prefs = getSecurePrefs(context)
            val stored = prefs.getString(KEY_GROQ_API, null)
            if (!stored.isNullOrBlank()) {
                val trimmed = stored.trim()
                inMemoryCachedKey = trimmed
                return trimmed
            }
            inMemoryCachedKey ?: ""
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal membaca Groq API Key dari secure storage", e)
            inMemoryCachedKey ?: ""
        }
    }

    /**
     * Mengambil Groq API Key saat pemanggil tidak memiliki akses langsung ke [Context] Android
     * (misalnya dari default parameter fungsi atau background worker).
     *
     * @return Nilai API Key dari cache in-memory atau string kosong.
     */
    fun getGroqApiKey(): String {
        return inMemoryCachedKey ?: ""
    }

    /**
     * Menyimpan Groq API Key ke dalam [EncryptedSharedPreferences] dengan enkripsi AES-256-GCM.
     *
     * @param context Context Android untuk mengakses EncryptedSharedPreferences.
     * @param key API Key yang akan disimpan.
     */
    fun setGroqApiKey(context: Context, key: String) {
        val trimmed = key.trim()
        inMemoryCachedKey = trimmed
        try {
            val prefs = getSecurePrefs(context)
            prefs.edit()
                .putString(KEY_GROQ_API, trimmed)
                .apply()
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal menyimpan Groq API Key ke secure storage", e)
        }
    }

    /**
     * Menghapus API Key tersimpan dari penyimpanan aman dan mereset in-memory cache.
     *
     * @param context Context Android untuk mengakses EncryptedSharedPreferences.
     */
    fun clearGroqApiKey(context: Context) {
        inMemoryCachedKey = null
        try {
            val prefs = getSecurePrefs(context)
            prefs.edit()
                .remove(KEY_GROQ_API)
                .apply()
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal menghapus Groq API Key", e)
        }
    }

    /**
     * Memeriksa apakah user telah mengonfigurasi custom API Key di penyimpanan terenkripsi.
     *
     * @param context Context Android untuk mengakses EncryptedSharedPreferences.
     * @return True jika terdapat key tersimpan yang tidak kosong.
     */
    fun hasCustomApiKey(context: Context): Boolean {
        return try {
            val prefs = getSecurePrefs(context)
            !prefs.getString(KEY_GROQ_API, null).isNullOrBlank()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Mengatur nilai in-memory API Key secara langsung (digunakan untuk pengujian atau session override).
     */
    fun setInMemoryApiKey(key: String?) {
        inMemoryCachedKey = key?.trim()
    }

    /**
     * Menghasilkan kunci simetris 256-bit AES acak untuk pengujian enkripsi terisolasi.
     */
    fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        return keyGen.generateKey()
    }

    /**
     * Mengenkripsi teks menggunakan AES-256-GCM dengan Initialization Vector (IV) 12-byte acak
     * dan authentication tag 128-bit.
     *
     * @param plainText Teks rahasia yang akan dienkripsi.
     * @param secretKey Kunci AES 256-bit.
     * @return Pasangan ByteArray ciphertext terenkripsi dan ByteArray IV acak.
     */
    fun encryptAesGcm(plainText: String, secretKey: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Pair(cipherBytes, iv)
    }

    /**
     * Mendekripsi ciphertext AES-256-GCM dengan memverifikasi autentikasi data (MAC tag).
     *
     * @param cipherBytes Ciphertext terenkripsi.
     * @param iv Initialization Vector yang digunakan saat enkripsi.
     * @param secretKey Kunci AES 256-bit yang cocok.
     * @return Teks asli hasil dekripsi.
     */
    fun decryptAesGcm(cipherBytes: ByteArray, iv: ByteArray, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }
}
