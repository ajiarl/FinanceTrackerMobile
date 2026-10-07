package com.sena.financetracker.service

import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk AiInsightService:
 * - Pembentukan user prompt & format string
 * - Pembentukan payload JSON untuk model openai/gpt-oss-120b
 * - Parsing respons JSON completion
 * - Validasi aturan generator local fallback (defisit, saving rate, zero income)
 * - Eksekusi getFinancialInsight dengan fallback saat api key kosong
 */
class AiInsightServiceTest {

    @Test
    fun buildGroqPayloadJson_createsValidChatCompletionsStructure() {
        val userPrompt = "Evaluasi transaksi ini, Pak!"
        val jsonString = AiInsightService.buildGroqPayloadJson(userPrompt)

        assertTrue(jsonString.contains("\"model\":\"openai/gpt-oss-120b\""))
        assertTrue(jsonString.contains("\"temperature\":0.6"))
        assertTrue(jsonString.contains("\"max_tokens\":350"))
        assertTrue(jsonString.contains("\"role\":\"system\""))
        assertTrue(jsonString.contains("Pak Hemat"))
        assertTrue(jsonString.contains("\"role\":\"user\""))
        assertTrue(jsonString.contains("Evaluasi transaksi ini, Pak!"))
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
                    "content": "Pengeluaran kopi kamu 800 ribu? Itu lambung aman, dompet kritis bos!"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
        """.trimIndent()

        val parsed = AiInsightService.parseGroqResponse(mockApiResponse)
        assertEquals("Pengeluaran kopi kamu 800 ribu? Itu lambung aman, dompet kritis bos!", parsed)
    }

    @Test
    fun parseGroqResponse_whenInvalidOrEmptyJson_returnsEmptyString() {
        val emptyChoices = """{"choices": []}"""
        assertEquals("", AiInsightService.parseGroqResponse(emptyChoices))

        val noChoices = """{"error": "rate_limit_exceeded"}"""
        assertEquals("", AiInsightService.parseGroqResponse(noChoices))
    }

    @Test
    fun buildUserPrompt_containsAccurateFinancialFigures() {
        val prompt = AiInsightService.buildUserPrompt(
            totalIncome = 5_000_000.0,
            totalExpense = 2_000_000.0,
            highestCategory = "Makanan",
            highestCategoryAmount = 1_200_000.0,
            txCount = 15,
            periodTitle = "Bulan Ini"
        )

        assertTrue(prompt.contains("Bulan Ini"))
        assertTrue(prompt.contains("5.000.000") || prompt.contains("5000000"))
        assertTrue(prompt.contains("2.000.000") || prompt.contains("2000000"))
        assertTrue(prompt.contains("60%")) // 3jt / 5jt = 60% saving rate
        assertTrue(prompt.contains("Makanan"))
        assertTrue(prompt.contains("15 transaksi"))
    }

    @Test
    fun generateLocalFallbackInsight_whenZeroIncomeAndHasExpense_returnsWarning() {
        val insight = AiInsightService.generateLocalFallbackInsight(
            totalIncome = 0.0,
            totalExpense = 750_000.0,
            highestExpenseCategory = "Hiburan",
            highestExpenseAmount = 500_000.0,
            periodTitle = "Bulan Ini"
        )

        assertTrue(insight.contains("belum ada pemasukan"))
        assertTrue(insight.contains("Hiburan"))
    }

    @Test
    fun generateLocalFallbackInsight_whenDeficit_returnsDeficitWarning() {
        val insight = AiInsightService.generateLocalFallbackInsight(
            totalIncome = 3_000_000.0,
            totalExpense = 4_500_000.0,
            highestExpenseCategory = "Elektronik",
            highestExpenseAmount = 3_000_000.0,
            periodTitle = "Bulan Ini"
        )

        assertTrue(insight.contains("Defisit") || insight.contains("jebol"))
        assertTrue(insight.contains("Elektronik"))
    }

    @Test
    fun generateLocalFallbackInsight_whenHighSavingRate_returnsPraise() {
        val insight = AiInsightService.generateLocalFallbackInsight(
            totalIncome = 10_000_000.0,
            totalExpense = 2_000_000.0,
            highestExpenseCategory = "Kebutuhan Pokok",
            highestExpenseAmount = 1_500_000.0,
            periodTitle = "Bulan Ini"
        )

        assertTrue(insight.contains("Mantap") || insight.contains("surplus"))
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
        assertTrue(result.contains("Makanan") || result.contains("saving rate") || result.contains("Mantap"))
    }

    @Test
    fun getFinancialInsight_whenEmptyTransactions_returnsNotice() = runBlocking {
        val result = AiInsightService.getFinancialInsight(
            transactions = emptyList(),
            periodTitle = "Bulan Ini",
            apiKey = ""
        )

        assertTrue(result.contains("Belum ada transaksi"))
    }
}
