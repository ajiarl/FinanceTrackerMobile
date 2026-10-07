package com.sena.financetracker.service

import com.sena.financetracker.BuildConfig
import com.sena.financetracker.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

/**
 * Service client untuk analisis finansial cerdas "Pak Hemat · AI Insight"
 * menggunakan API Groq Cloud (model: openai/gpt-oss-120b) dengan fallback aturan lokal matematis.
 */
object AiInsightService {

    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val GROQ_MODEL = "openai/gpt-oss-120b"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    private const val SYSTEM_PROMPT =
        "Kamu adalah Pak Hemat, konsultan keuangan pribadi Neobrutalisme yang santai, sarkas-kocak, to-the-point, dan matematis tajam. Evaluasi rasio cashflow, saving rate, dan kebocoran dana dalam 3-4 kalimat padat."

    /**
     * Meminta analisis AI dari Groq Cloud secara background IO dengan fallback aturan lokal jika gagal / offline.
     */
    suspend fun getFinancialInsight(
        transactions: List<TransactionEntity>,
        periodTitle: String = "Periode Ini",
        apiKey: String = BuildConfig.GROQ_API_KEY
    ): String = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext "Belum ada transaksi di $periodTitle nih, bos. Catat dulu pengeluaran dan pemasukanmu biar Pak Hemat bisa bedah kas kamu!"
        }

        val totalIncome = transactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
        val categoryBreakdown = transactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .groupBy { it.category.ifBlank { "Lainnya" } }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }

        val highestExpenseCategory = categoryBreakdown.firstOrNull()?.first ?: "Tidak ada"
        val highestExpenseAmount = categoryBreakdown.firstOrNull()?.second ?: 0.0

        // Jika API Key kosong / tidak diset, langsung gunakan fallback aturan lokal
        if (apiKey.isBlank()) {
            return@withContext generateLocalFallbackInsight(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                highestExpenseCategory = highestExpenseCategory,
                highestExpenseAmount = highestExpenseAmount,
                periodTitle = periodTitle
            )
        }

        try {
            val userPrompt = buildUserPrompt(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                highestCategory = highestExpenseCategory,
                highestCategoryAmount = highestExpenseAmount,
                txCount = transactions.size,
                periodTitle = periodTitle
            )

            val payloadJson = buildGroqPayloadJson(userPrompt)
            val responseText = executeHttpRequest(payloadJson, apiKey)
            val parsedContent = parseGroqResponse(responseText)

            if (parsedContent.isNotBlank()) {
                parsedContent
            } else {
                generateLocalFallbackInsight(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    highestExpenseCategory = highestExpenseCategory,
                    highestExpenseAmount = highestExpenseAmount,
                    periodTitle = periodTitle
                )
            }
        } catch (e: Exception) {
            // Fail-safe: fallback lokal tanpa crash
            generateLocalFallbackInsight(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                highestExpenseCategory = highestExpenseCategory,
                highestExpenseAmount = highestExpenseAmount,
                periodTitle = periodTitle
            )
        }
    }

    /**
     * Membentuk payload JSON untuk Groq completions.
     * Dibuat secara deterministik tanpa dependensi android.json agar kebal di JVM unit test.
     */
    fun buildGroqPayloadJson(userPrompt: String): String {
        fun escape(s: String): String {
            val sb = StringBuilder()
            for (c in s) {
                when (c) {
                    '\\' -> sb.append("\\\\")
                    '"' -> sb.append("\\\"")
                    '\b' -> sb.append("\\b")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> {
                        if (c.code in 0..0x1f) {
                            sb.append(String.format("\\u%04x", c.code))
                        } else {
                            sb.append(c)
                        }
                    }
                }
            }
            return sb.toString()
        }

        val escapedSys = escape(SYSTEM_PROMPT)
        val escapedUser = escape(userPrompt)

        return """{"model":"$GROQ_MODEL","temperature":0.6,"max_tokens":350,"messages":[{"role":"system","content":"$escapedSys"},{"role":"user","content":"$escapedUser"}]}""".trimIndent()
    }

    /**
     * Eksekusi HTTP POST request via HttpURLConnection murni.
     */
    private fun executeHttpRequest(jsonBody: String, apiKey: String): String {
        val url = URL(GROQ_ENDPOINT)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.doOutput = true
        conn.doInput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            ?: throw IllegalStateException("HTTP code $responseCode")

        val response = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
        conn.disconnect()

        if (responseCode !in 200..299) {
            throw IllegalStateException("HTTP $responseCode: $response")
        }

        return response
    }

    /**
     * Parsing respons JSON Groq / OpenAI-compatible chat completion.
     * Menggunakan regex extraction untuk ketahanan di unit-test JVM tanpa stub org.json.
     */
    fun parseGroqResponse(jsonString: String): String {
        // Cari pola "content"\s*:\s*"..."
        val contentRegex = Regex("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val match = contentRegex.findAll(jsonString).lastOrNull() ?: return ""
        val rawContent = match.groupValues[1]

        // Unescape string JSON standar
        val sb = StringBuilder()
        var i = 0
        while (i < rawContent.length) {
            val c = rawContent[i]
            if (c == '\\' && i + 1 < rawContent.length) {
                when (val next = rawContent[i + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 5 < rawContent.length) {
                            val hex = rawContent.substring(i + 2, i + 6)
                            try {
                                sb.append(hex.toInt(16).toChar())
                                i += 4
                            } catch (e: Exception) {
                                sb.append("\\u")
                            }
                        } else {
                            sb.append("\\u")
                        }
                    }
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString().trim()
    }

    /**
     * Membentuk prompt ringkasan data transaksi untuk AI.
     */
    fun buildUserPrompt(
        totalIncome: Double,
        totalExpense: Double,
        highestCategory: String,
        highestCategoryAmount: Double,
        txCount: Int,
        periodTitle: String
    ): String {
        val rupiahFormat = NumberFormat.getNumberInstance(Locale.GERMANY)
        val netSavings = totalIncome - totalExpense
        val savingRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0

        return """
            Ringkasan Keuangan ($periodTitle):
            - Total Pemasukan: Rp ${rupiahFormat.format(totalIncome.toLong())}
            - Total Pengeluaran: Rp ${rupiahFormat.format(totalExpense.toLong())}
            - Sisa Kas / Tabungan Bersih: Rp ${rupiahFormat.format(netSavings.toLong())}
            - Rasio Tabungan (Saving Rate): $savingRate%
            - Kategori Pengeluaran Terbesar: $highestCategory (Rp ${rupiahFormat.format(highestCategoryAmount.toLong())})
            - Total Catatan Transaksi: $txCount transaksi

            Berikan penilaian tajam, evaluasi kebocoran dana, dan instruksi penghematan konkret gaya Neobrutalisme Pak Hemat.
        """.trimIndent()
    }

    /**
     * Generator analisis aturan lokal berbasis kalkulasi rasio finansial deterministik.
     * Dipanggil saat koneksi offline, kuota habis, atau terjadi error jaringan.
     */
    fun generateLocalFallbackInsight(
        totalIncome: Double,
        totalExpense: Double,
        highestExpenseCategory: String,
        highestExpenseAmount: Double,
        periodTitle: String
    ): String {
        val rupiahFormat = NumberFormat.getNumberInstance(Locale.GERMANY)
        val netSavings = totalIncome - totalExpense
        val savingRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else -100

        return when {
            totalIncome <= 0 && totalExpense > 0 -> {
                "Waduh bos, di $periodTitle kamu keluar uang Rp ${rupiahFormat.format(totalExpense.toLong())} tapi belum ada pemasukan sama sekali! Kategori '$highestExpenseCategory' jadi biang keladi kebocoran. Rem dulu jajan kamu sebelum dompet amblas!"
            }
            totalIncome > 0 && totalExpense > totalIncome -> {
                "Defisit parah! Pengeluaranmu (Rp ${rupiahFormat.format(totalExpense.toLong())}) jebol melebihi pemasukan dengan minus Rp ${rupiahFormat.format((-netSavings).toLong())}. Kebocoran terbesar di pos '$highestExpenseCategory' (Rp ${rupiahFormat.format(highestExpenseAmount.toLong())}). Pangkas kebutuhan sekunder sekarang juga!"
            }
            savingRate in 0..19 -> {
                "Saving rate kamu cuma $savingRate% di $periodTitle, tipis banget kayak tisu basah! Pos '$highestExpenseCategory' menyedot Rp ${rupiahFormat.format(highestExpenseAmount.toLong())}. Evaluasi pos belanja ini biar ada sisa dana darurat yang waras."
            }
            savingRate in 20..49 -> {
                "Arus kas tergolong aman dengan saving rate $savingRate% (tabungan bersih Rp ${rupiahFormat.format(netSavings.toLong())}). Namun waspadai pos '$highestExpenseCategory' yang sudah tembus Rp ${rupiahFormat.format(highestExpenseAmount.toLong())}. Pertahankan disiplin ini bos!"
            }
            else -> {
                "Mantap jiwa! Saving rate kamu tembus $savingRate% di $periodTitle dengan surplus Rp ${rupiahFormat.format(netSavings.toLong())}. Pengeluaran terbesar di '$highestExpenseCategory' masih dalam batas wajar. Tetap konsisten begini, bos!"
            }
        }
    }
}
