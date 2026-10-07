package com.sena.financetracker.service

import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk AiInsightService:
 * - Pembentukan user prompt & grounding rincian 3 kategori teratas
 * - Pembentukan payload JSON untuk model openai/gpt-oss-120b dengan persona Pak Hemat & panggilan 'Aji'
 * - Parsing respons JSON completion
 * - Validasi aturan generator local fallback (boncos zero income, defisit, saving rate, sapaan 'Ji')
 * - Eksekusi getFinancialInsight dengan fallback saat api key kosong
 */
class AiInsightServiceTest {

    @Test
    fun buildGroqPayloadJson_createsValidChatCompletionsStructure() {
        val userPrompt = "Evaluasi keuangan Aji ini, Pak!"
        val jsonString = AiInsightService.buildGroqPayloadJson(userPrompt)

        assertTrue(jsonString.contains("\"model\":\"openai/gpt-oss-120b\""))
        assertTrue(jsonString.contains("\"temperature\":0.6"))
        assertTrue(jsonString.contains("\"max_tokens\":350"))
        assertTrue(jsonString.contains("\"role\":\"system\""))
        assertTrue(jsonString.contains("Pak Hemat"))
        assertTrue(jsonString.contains("Aji"))
        assertTrue(jsonString.contains("GROUNDING"))
        assertTrue(jsonString.contains("\"role\":\"user\""))
        assertTrue(jsonString.contains("Evaluasi keuangan Aji ini, Pak!"))
    }

    @Test
    fun parseGroqResponse_extractsContentSuccessfully() {
        val mockApiResponse = """
            {
              "id": "chatcmpl-test",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Pengeluaran kopi kamu 800 ribu? Itu lambung aman, dompet kritis Ji!"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
        """.trimIndent()

        val parsed = AiInsightService.parseGroqResponse(mockApiResponse)
        assertEquals("Pengeluaran kopi kamu 800 ribu? Itu lambung aman, dompet kritis Ji!", parsed)
    }

    @Test
    fun parseGroqResponse_whenInvalidOrEmptyJson_returnsEmptyString() {
        val emptyChoices = """{"choices": []}"""
        assertEquals("", AiInsightService.parseGroqResponse(emptyChoices))

        val noChoices = """{"error": "rate_limit_exceeded"}"""
        assertEquals("", AiInsightService.parseGroqResponse(noChoices))
    }

    @Test
    fun buildUserPrompt_containsTopThreeCategoriesAndGroundedFigures() {
        val topCategories = listOf(
            "Makanan" to 1_500_000.0,
            "Tagihan" to 800_000.0,
            "Hiburan" to 400_000.0
        )

        val prompt = AiInsightService.buildUserPrompt(
            totalIncome = 5_000_000.0,
            totalExpense = 2_700_000.0,
            topCategories = topCategories,
            txCount = 20,
            periodTitle = "Bulan Ini"
        )

        assertTrue(prompt.contains("Ringkasan Keuangan Aji (Bulan Ini):"))
        assertTrue(prompt.contains("5.000.000") || prompt.contains("5000000"))
        assertTrue(prompt.contains("2.700.000") || prompt.contains("2700000"))
        assertTrue(prompt.contains("46%")) // (5jt - 2.7jt) / 5jt = 46%
        assertTrue(prompt.contains("Makanan"))
        assertTrue(prompt.contains("Tagihan"))
        assertTrue(prompt.contains("Hiburan"))
        assertTrue(prompt.contains("20 transaksi"))
    }

    @Test
    fun generateLocalFallbackInsight_whenZeroIncomeAndHasExpense_returnsWarningWithAjiPersona() {
        val topCategories = listOf(
            "Hiburan" to 500_000.0,
            "Kopi" to 150_000.0
        )

        val insight = AiInsightService.generateLocalFallbackInsight(
            totalIncome = 0.0,
            totalExpense = 650_000.0,
            topCategories = topCategories,
            periodTitle = "Bulan Ini"
        )

        assertTrue(insight.contains("Ji,"))
        assertTrue(insight.contains("boncos"))
        assertTrue(insight.contains("Hiburan"))
    }

    @Test
    fun generateLocalFallbackInsight_whenDeficit_returnsDeficitWarningWithAjiPersona() {
        val topCategories = listOf(
            "Elektronik" to 3_000_000.0,
            "Hobi" to 1_500_000.0
        )

        val insight = AiInsightService.generateLocalFallbackInsight(
            totalIncome = 3_000_000.0,
            totalExpense = 4_500_000.0,
            topCategories = topCategories,
            periodTitle = "Bulan Ini"
        )

        assertTrue(insight.contains("Defisit jebol, Ji!"))
        assertTrue(insight.contains("Elektronik"))
    }

    @Test
    fun generateLocalFallbackInsight_whenHighSavingRate_returnsPraiseWithAjiPersona() {
        val topCategories = listOf(
            "Kebutuhan Pokok" to 1_500_000.0
        )

        val insight = AiInsightService.generateLocalFallbackInsight(
            totalIncome = 10_000_000.0,
            totalExpense = 1_500_000.0,
            topCategories = topCategories,
            periodTitle = "Bulan Ini"
        )

        assertTrue(insight.contains("Gokil Ji,"))
        assertTrue(insight.contains("85%"))
    }

    @Test
    fun getFinancialInsight_whenApiKeyIsBlank_usesLocalFallbackGracefully() = runBlocking {
        val txs = listOf(
            TransactionEntity(id = 1, title = "Gaji", amount = 8_000_000.0, type = "INCOME", category = "Gaji", date = "2026-10-01", accountId = 1),
            TransactionEntity(id = 2, title = "Makan", amount = 1_500_000.0, type = "EXPENSE", category = "Makanan", date = "2026-10-02", accountId = 1)
        )

        val result = AiInsightService.getFinancialInsight(
            transactions = txs,
            periodTitle = "Bulan Ini",
            apiKey = ""
        )

        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertTrue(result.contains("Ji") || result.contains("Makanan"))
    }

    @Test
    fun getFinancialInsight_whenEmptyTransactions_returnsNotice() = runBlocking {
        val result = AiInsightService.getFinancialInsight(
            transactions = emptyList(),
            periodTitle = "Bulan Ini",
            apiKey = ""
        )

        assertTrue(result.contains("Belum ada transaksi"))
        assertTrue(result.contains("Ji"))
    }
}
