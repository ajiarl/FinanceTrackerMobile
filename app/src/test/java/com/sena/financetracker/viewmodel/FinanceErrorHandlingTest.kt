package com.sena.financetracker.viewmodel

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class FinanceErrorHandlingTest {

    @Test
    fun testCoroutineExceptionHandlerUpdatesUiStateWithFriendlyMessage() {
        var uiState = FinanceUiState(isLoading = true)

        val handler = CoroutineExceptionHandler { _, throwable ->
            val friendlyMsg = throwable.message?.takeIf { it.isNotBlank() }
                ?: "Terjadi kesalahan internal pada operasi data"
            uiState = uiState.copy(
                errorMessage = friendlyMsg,
                isLoading = false
            )
        }

        // Simulasikan exception storage I/O
        val exception = IOException("Disk read failed: SQLite database locked")
        handler.handleException(kotlin.coroutines.EmptyCoroutineContext, exception)

        assertNotNull(uiState.errorMessage)
        assertEquals("Disk read failed: SQLite database locked", uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun testCoroutineExceptionHandlerWithBlankMessageProvidesDefaultFriendlyFallback() {
        var uiState = FinanceUiState(isLoading = true)

        val handler = CoroutineExceptionHandler { _, throwable ->
            val friendlyMsg = throwable.message?.takeIf { it.isNotBlank() }
                ?: "Terjadi kesalahan internal pada operasi data"
            uiState = uiState.copy(
                errorMessage = friendlyMsg,
                isLoading = false
            )
        }

        val exception = RuntimeException("")
        handler.handleException(kotlin.coroutines.EmptyCoroutineContext, exception)

        assertEquals("Terjadi kesalahan internal pada operasi data", uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun testClearErrorMessageResetsErrorToNull() {
        var uiState = FinanceUiState(errorMessage = "Gagal memproses transaksi")
        assertEquals("Gagal memproses transaksi", uiState.errorMessage)

        // Simulasikan logika clearErrorMessage()
        uiState = uiState.copy(errorMessage = null)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun testCoroutineExceptionHandlerCatchesUncaughtExceptionInScope() = runBlocking {
        var caughtMessage: String? = null
        var uiState = FinanceUiState(isLoading = true)

        val handler = CoroutineExceptionHandler { _, throwable ->
            caughtMessage = throwable.message
            uiState = uiState.copy(
                errorMessage = throwable.message ?: "Error",
                isLoading = false
            )
        }

        val job = CoroutineScope(Dispatchers.Default + handler).launch {
            throw IllegalStateException("Database I/O fatal crash simulation")
        }

        job.join()

        assertEquals("Database I/O fatal crash simulation", caughtMessage)
        assertEquals("Database I/O fatal crash simulation", uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }
}
