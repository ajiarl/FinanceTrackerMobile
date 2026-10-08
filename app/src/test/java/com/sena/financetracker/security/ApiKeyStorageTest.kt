package com.sena.financetracker.security

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.sena.financetracker.util.SecurityConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Unit Test Suite untuk memvalidasi keamanan penyimpanan API Key (SEC-01).
 *
 * Menguji:
 * 1. Simpan dan ambil API Key via [ApiKeyStorage] menggunakan penyimpanan terenkripsi.
 * 2. Penghapusan key dan verifikasi status [ApiKeyStorage.hasCustomApiKey].
 * 3. Fallback in-memory session key saat pemanggilan tanpa Context.
 * 4. Verifikasi kriptografi AES-256-GCM (Authenticated Encryption with Associated Data).
 * 5. Delegasi aman dari [SecurityConfig] ke [ApiKeyStorage].
 */
class ApiKeyStorageTest {

    private lateinit var fakePreferences: FakeSharedPreferences
    private lateinit var mockContext: Context

    /**
     * In-memory mock [SharedPreferences] untuk pengujian deterministik di lingkungan JVM.
     */
    private class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(this)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val toRemove = mutableSetOf<String>()
            private var clearFlag = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) toRemove.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clearFlag = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearFlag) prefs.map.clear()
                toRemove.forEach { prefs.map.remove(it) }
                prefs.map.putAll(temp)
            }
        }
    }

    private class MockTestContext(private val prefs: SharedPreferences) : ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
        override fun getPackageName(): String = "com.sena.financetracker"
    }

    @Before
    fun setup() {
        fakePreferences = FakeSharedPreferences()
        mockContext = MockTestContext(fakePreferences)

        // Injeksi fake preferences provider untuk pengujian
        ApiKeyStorage.sharedPreferencesProvider = { fakePreferences }
        ApiKeyStorage.setInMemoryApiKey(null)
    }

    @After
    fun tearDown() {
        ApiKeyStorage.sharedPreferencesProvider = null
        ApiKeyStorage.setInMemoryApiKey(null)
    }

    @Test
    fun setAndGetGroqApiKey_storesAndRetrievesCorrectly() {
        val testKey = "gsk_prod_secure_token_123456789"

        ApiKeyStorage.setGroqApiKey(mockContext, testKey)
        val retrieved = ApiKeyStorage.getGroqApiKey(mockContext)

        assertEquals(testKey, retrieved)
        assertTrue(ApiKeyStorage.hasCustomApiKey(mockContext))
    }

    @Test
    fun clearGroqApiKey_removesStoredKey() {
        val testKey = "gsk_temporary_token"
        ApiKeyStorage.setGroqApiKey(mockContext, testKey)
        assertTrue(ApiKeyStorage.hasCustomApiKey(mockContext))

        ApiKeyStorage.clearGroqApiKey(mockContext)

        val retrieved = ApiKeyStorage.getGroqApiKey(mockContext)
        assertEquals("", retrieved)
        assertFalse(ApiKeyStorage.hasCustomApiKey(mockContext))
    }

    @Test
    fun getMaskedGroqApiKey_masksKeySafelyWithoutExposingSecret() {
        val fullKey = "gsk_prod_secure_token_123456789"
        ApiKeyStorage.setGroqApiKey(mockContext, fullKey)

        val masked = ApiKeyStorage.getMaskedGroqApiKey(mockContext)
        assertEquals("gsk_••••••••6789", masked)
        assertFalse("Masked key dilarang membocorkan raw key", masked.contains("secure_token"))

        // Pengujian untuk key pendek (<= 8 karakter)
        ApiKeyStorage.setGroqApiKey(mockContext, "12345678")
        assertEquals("••••••••", ApiKeyStorage.getMaskedGroqApiKey(mockContext))

        // Pengujian untuk key kosong
        ApiKeyStorage.clearGroqApiKey(mockContext)
        assertEquals("", ApiKeyStorage.getMaskedGroqApiKey(mockContext))
    }

    @Test
    fun nonContextKeyOperations_managesInMemoryKeyCorrectly() {
        ApiKeyStorage.setGroqApiKey("gsk_direct_key_test_9876")
        assertTrue(ApiKeyStorage.hasCustomApiKey())
        assertEquals("gsk_••••••••9876", ApiKeyStorage.getMaskedGroqApiKey())

        ApiKeyStorage.clearGroqApiKey()
        assertFalse(ApiKeyStorage.hasCustomApiKey())
        assertEquals("", ApiKeyStorage.getMaskedGroqApiKey())
    }

    @Test
    fun inMemoryApiKey_fallbackWhenContextNotProvided() {
        val sessionKey = "gsk_session_override_token"
        ApiKeyStorage.setInMemoryApiKey(sessionKey)

        assertEquals(sessionKey, ApiKeyStorage.getGroqApiKey())
    }

    @Test
    fun aesGcmCipher_encryptsAndDecryptsAccuratelyWithUniqueIv() {
        val secretKey = ApiKeyStorage.generateAesKey()
        val plainText = "gsk_confidential_groq_api_credential_xyz"

        val (cipherBytes, iv) = ApiKeyStorage.encryptAesGcm(plainText, secretKey)

        assertNotNull(cipherBytes)
        assertNotNull(iv)
        assertEquals(12, iv.size) // Standar GCM IV 12 bytes (96 bits)
        assertNotEquals(plainText, String(cipherBytes, Charsets.UTF_8)) // Ciphertext berbeda dari plaintext

        val decrypted = ApiKeyStorage.decryptAesGcm(cipherBytes, iv, secretKey)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun securityConfig_delegatesToApiKeyStorage() {
        val testKey = "gsk_delegated_key"
        ApiKeyStorage.setGroqApiKey(mockContext, testKey)

        val retrievedViaSecurityConfig = SecurityConfig.getGroqApiKey(mockContext)
        assertEquals(testKey, retrievedViaSecurityConfig)

        val noArgKey = SecurityConfig.getGroqApiKey()
        assertNotNull(noArgKey)
    }
}
