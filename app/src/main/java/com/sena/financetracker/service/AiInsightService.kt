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
        "Kamu adalah Pak Hemat, Peer Savage untuk Aji. Panggil user 'Aji', atau sesekali sindir 'Bos' saat kondisi kas minus atau boncos parah. ATURAN GROUNDING MUTLAK: Kamu WAJIB mengacu 100% pada angka riil yang diberikan (Pemasukan, Pengeluaran, Sisa Kas, Saving Rate %, dan Kategori Pengeluaran Terbesar). DILARANG KERAS mengarang, mengubah nominal, atau menyebut kategori fiktif di luar data! ROASTING KONTEKSTUAL: Kaitkan nama kategori terbesar dengan sindiran gaya hidup nyata (misal Makanan & Minuman = ngopi aesthetic/jajan, Belanja = kalap diskon e-commerce). REAKSI KONDISI: Jika defisit atau boros, semprot savage tanpa basa-basi pembuka, tampar dengan fakta minusnya, dan beri 1 instruksi konkret ngerem jajan. Jika surplus atau hemat, puji skeptis-waspada ('Wih tumben waras'), ingatkan kunci sisa saldo ke tabungan atau investasi sebelum nafsu belanja kumat. FORMAT: Tulis langsung dalam 2-3 kalimat padat mengalir dalam satu paragraf tunggal (bukan bullet points). DILARANG memakai salam formal pembuka ('Halo Aji', 'Berdasarkan data') dan DILARANG memakai tanda em dash."

    /**
     * Meminta analisis AI dari Groq Cloud secara background IO dengan fallback aturan lokal jika gagal / offline.
     */
    suspend fun getFinancialInsight(
        transactions: List<TransactionEntity>,
        periodTitle: String = "Periode Ini",
        apiKey: String = BuildConfig.GROQ_API_KEY
    ): String = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext "Belum ada transaksi di $periodTitle nih, Ji. Catat dulu pengeluaran dan pemasukanmu biar Pak Hemat bisa bedah kas kamu!"
        }

        val totalIncome = transactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
        val categoryBreakdown = transactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .groupBy { it.category.ifBlank { "Lainnya" } }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }

        val topCategories = categoryBreakdown.take(3)
        val highestExpenseCategory = topCategories.firstOrNull()?.first ?: "Tidak ada"
        val highestExpenseAmount = topCategories.firstOrNull()?.second ?: 0.0

        // Jika API Key kosong / tidak diset, langsung gunakan fallback aturan lokal
        if (apiKey.isBlank()) {
            return@withContext generateLocalFallbackInsight(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                topCategories = topCategories,
                periodTitle = periodTitle
            )
        }

        try {
            val userPrompt = buildUserPrompt(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                topCategories = topCategories,
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
                    topCategories = topCategories,
                    periodTitle = periodTitle
                )
            }
        } catch (e: Exception) {
            // Fail-safe: fallback lokal tanpa crash
            generateLocalFallbackInsight(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                topCategories = topCategories,
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

        return """{"model":"$GROQ_MODEL","temperature":0.6,"max_tokens":1000,"messages":[{"role":"system","content":"$escapedSys"},{"role":"user","content":"$escapedUser"}]}""".trimIndent()
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
     * Membentuk prompt ringkasan data transaksi untuk AI dengan rincian 3 kategori teratas.
     */
    fun buildUserPrompt(
        totalIncome: Double,
        totalExpense: Double,
        topCategories: List<Pair<String, Double>>,
        txCount: Int,
        periodTitle: String
    ): String {
        val rupiahFormat = NumberFormat.getNumberInstance(Locale.GERMANY)
        val netSavings = totalIncome - totalExpense
        val savingRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0

        val categoryLines = if (topCategories.isNotEmpty()) {
            topCategories.mapIndexed { idx, pair ->
                "  ${idx + 1}. ${pair.first}: Rp ${rupiahFormat.format(pair.second.toLong())}"
            }.joinToString("\n")
        } else {
            "  (Belum ada pengeluaran)"
        }

        return """
            Ringkasan Keuangan Aji ($periodTitle):
            - Total Pemasukan: Rp ${rupiahFormat.format(totalIncome.toLong())}
            - Total Pengeluaran: Rp ${rupiahFormat.format(totalExpense.toLong())}
            - Sisa Kas / Tabungan Bersih: Rp ${rupiahFormat.format(netSavings.toLong())}
            - Rasio Tabungan (Saving Rate): $savingRate%
            - Top Kategori Pengeluaran:
            $categoryLines
            - Total Catatan Transaksi: $txCount transaksi

            Evaluasi keuangan Aji secara blak-blakan, matematis, sarkas-kocak, dan tanpa basa-basi formal sesuai persona Pak Hemat. Roasting kategori pengeluaran terbesar di atas secara kontekstual dan kasih instruksi konkret agar dompet Aji selamat!
        """.trimIndent()
    }

    /**
     * Generator analisis aturan lokal berbasis kalkulasi rasio finansial deterministik dengan persona Pak Hemat.
     * Dipanggil saat koneksi offline, kuota habis, atau terjadi error jaringan.
     */
    fun generateLocalFallbackInsight(
        totalIncome: Double,
        totalExpense: Double,
        topCategories: List<Pair<String, Double>>,
        periodTitle: String
    ): String {
        val rupiahFormat = NumberFormat.getNumberInstance(Locale.GERMANY)
        val netSavings = totalIncome - totalExpense
        val savingRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else -100

        val topCategoryName = topCategories.firstOrNull()?.first ?: "pos belanja"
        val topCategoryAmount = topCategories.firstOrNull()?.second ?: 0.0

        val categoryRoast = when {
            topCategoryName.contains("makan", ignoreCase = true) || topCategoryName.contains("kuliner", ignoreCase = true) ->
                "Kebanyakan ngopi aesthetic sama jajan delivery bikin dompet gepeng."
            topCategoryName.contains("belanja", ignoreCase = true) || topCategoryName.contains("shop", ignoreCase = true) ->
                "Kalap diskon e-commerce lagi kan lu?"
            topCategoryName.contains("hiburan", ignoreCase = true) || topCategoryName.contains("game", ignoreCase = true) ->
                "Self-reward berlebihan itu aslinya bunuh diri finansial pelan-pelan."
            else ->
                "Pos belanja ini jelas-jelas nyedot porsi kas paling rakus."
        }

        val breakdownSnippet = if (topCategories.size > 1) {
            " Tiga pos penyedot utama: " + topCategories.joinToString(", ") {
                "${it.first} (Rp ${rupiahFormat.format(it.second.toLong())})"
            } + "."
        } else {
            ""
        }

        return when {
            totalIncome <= 0 && totalExpense > 0 -> {
                "Waduh Bos, kamu boncos Rp ${rupiahFormat.format(totalExpense.toLong())} di $periodTitle padahal pemasukan masih nol melompong! Kategori '$topCategoryName' nembus Rp ${rupiahFormat.format(topCategoryAmount.toLong())}, $categoryRoast$breakdownSnippet Rem darurat jajan lu hari ini juga sebelum kas sekarat total!"
            }
            totalIncome > 0 && totalExpense > totalIncome -> {
                "Defisit parah, Bos! Pengeluaranmu tembus Rp ${rupiahFormat.format(totalExpense.toLong())} numpahin pemasukan sampai minus Rp ${rupiahFormat.format((-netSavings).toLong())}. Kategori '$topCategoryName' (Rp ${rupiahFormat.format(topCategoryAmount.toLong())}) jadi biang keroknya, $categoryRoast$breakdownSnippet Pangkas pengeluaran sekunder detik ini juga, jangan sok sultan!"
            }
            savingRate in 0..19 -> {
                "Saving rate kamu cuma $savingRate% di $periodTitle, tipis banget kayak tisu basah, Ji! Duitmu habis disedot '$topCategoryName' sebesar Rp ${rupiahFormat.format(topCategoryAmount.toLong())}, $categoryRoast$breakdownSnippet Evaluasi kebiasaan impulsif ini biar ada dana darurat yang waras."
            }
            savingRate in 20..49 -> {
                "Cashflow kamu masih napas aman dengan saving rate $savingRate% dan sisa kas Rp ${rupiahFormat.format(netSavings.toLong())}, Ji. Tapi jangan santai dulu karena '$topCategoryName' udah nelan Rp ${rupiahFormat.format(topCategoryAmount.toLong())}.$breakdownSnippet Kunci sisa saldo ke tabungan sebelum nafsu belanja kumat lagi!"
            }
            else -> {
                "Wih tumben waras, Ji! Saving rate kamu tembus $savingRate% di $periodTitle dengan surplus Rp ${rupiahFormat.format(netSavings.toLong())}, pengeluaran terbesar di '$topCategoryName' juga terkontrol rapi. Segera amankan sisa saldo ke tabungan atau investasi sebelum godaan promo merusak kedisiplinan ini!"
            }
        }
    }
}
