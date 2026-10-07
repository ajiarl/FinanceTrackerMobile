package com.sena.financetracker.service

import com.sena.financetracker.BuildConfig
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetProgressItem
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
        "Kamu adalah Pak Hemat, Peer Savage untuk Aji. Panggil user 'Aji', atau sesekali sindir 'Bos' saat kondisi kas minus, boncos parah, atau saldo sekarat. ATURAN GROUNDING MUTLAK: Kamu WAJIB mengacu 100% pada angka riil yang diberikan dari 3 pilar konteks: [1] Ringkasan Saldo & Rekening, [2] Arus Kas Periode Ini (Pemasukan, Pengeluaran, Sisa Kas, Saving Rate %, dan Kategori Pengeluaran Terbesar), dan [3] Status Anggaran / Budget (anggaran jebol atau kritis). DILARANG KERAS mengarang, mengubah nominal, atau menyebut kategori/rekening fiktif di luar data! ANALISIS HUBUNGAN: Analisis korelasi antara saldo rekening, kebocoran pos belanja, dan budget yang jebol (misal: saldo rekening masih ada tapi budget makan jebol, atau saldo rekening sekarat dan pengeluaran defisit). ROASTING KONTEKSTUAL: Kaitkan nama kategori terbesar dan budget jebol dengan sindiran gaya hidup nyata (misal Makanan & Minuman = ngopi aesthetic/jajan, Belanja = kalap diskon e-commerce). REAKSI KONDISI: Jika defisit, boros, atau budget jebol, semprot savage tanpa basa-basi pembuka, tampar dengan fakta minus atau anggaran jebolnya, dan beri 1 instruksi konkret ngerem jajan. Jika surplus, hemat, dan budget terkendali, puji skeptis-waspada ('Wih tumben waras'), ingatkan kunci sisa saldo ke tabungan atau investasi sebelum nafsu belanja kumat. FORMAT: Tulis langsung dalam 2-3 kalimat padat mengalir dalam satu paragraf tunggal (bukan bullet points). DILARANG memakai salam formal pembuka ('Halo Aji', 'Berdasarkan data') dan DILARANG memakai tanda em dash."

    /**
     * Meminta analisis AI dari Groq Cloud secara background IO dengan fallback aturan lokal jika gagal / offline.
     * Menerima konteks menyeluruh: Accounts, Budgets, dan Transactions.
     */
    suspend fun getFinancialInsight(
        accounts: List<AccountEntity> = emptyList(),
        budgets: List<BudgetProgressItem> = emptyList(),
        transactions: List<TransactionEntity> = emptyList(),
        periodTitle: String = "Periode Ini",
        apiKey: String = BuildConfig.GROQ_API_KEY
    ): String = withContext(Dispatchers.IO) {
        val totalNetWorth = accounts.sumOf { it.balance }
        val overBudgets = budgets.filter { it.isOver || it.percentage >= 100 }
        val criticalBudgets = budgets.filter { !it.isOver && it.percentage in 80..99 }

        if (transactions.isEmpty() && accounts.isEmpty()) {
            return@withContext "Belum ada catatan rekening dan transaksi di $periodTitle nih, Ji. Catat dulu biar Pak Hemat bisa bedah kas kamu!"
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

        // Jika API Key kosong / tidak diset, langsung gunakan fallback aturan lokal
        if (apiKey.isBlank()) {
            return@withContext generateLocalFallbackInsight(
                totalNetWorth = totalNetWorth,
                accounts = accounts,
                overBudgets = overBudgets,
                criticalBudgets = criticalBudgets,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                topCategories = topCategories,
                periodTitle = periodTitle
            )
        }

        try {
            val userPrompt = buildUserPrompt(
                totalNetWorth = totalNetWorth,
                accounts = accounts,
                overBudgets = overBudgets,
                criticalBudgets = criticalBudgets,
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
                    totalNetWorth = totalNetWorth,
                    accounts = accounts,
                    overBudgets = overBudgets,
                    criticalBudgets = criticalBudgets,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    topCategories = topCategories,
                    periodTitle = periodTitle
                )
            }
        } catch (e: Exception) {
            // Fail-safe: fallback lokal tanpa crash
            generateLocalFallbackInsight(
                totalNetWorth = totalNetWorth,
                accounts = accounts,
                overBudgets = overBudgets,
                criticalBudgets = criticalBudgets,
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
     * Membentuk prompt ringkasan data finansial menyeluruh untuk AI (3 Pilar: Rekening, Arus Kas, dan Anggaran).
     */
    fun buildUserPrompt(
        totalNetWorth: Double,
        accounts: List<AccountEntity>,
        overBudgets: List<BudgetProgressItem>,
        criticalBudgets: List<BudgetProgressItem>,
        totalIncome: Double,
        totalExpense: Double,
        topCategories: List<Pair<String, Double>>,
        txCount: Int,
        periodTitle: String
    ): String {
        val rupiahFormat = NumberFormat.getNumberInstance(Locale.GERMANY)
        val netSavings = totalIncome - totalExpense
        val savingRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0

        // Pilar 1: Rekening & Saldo
        val accountLines = if (accounts.isNotEmpty()) {
            accounts.joinToString("\n") { acc ->
                val criticalTag = if (acc.balance < 50_000) " [SALDO KRITIS < 50rb!]" else ""
                "  - ${acc.name} (${acc.type}): Rp ${rupiahFormat.format(acc.balance.toLong())}$criticalTag"
            }
        } else {
            "  - (Belum ada rekening terdaftar)"
        }

        // Pilar 2: Pengeluaran Teratas
        val categoryLines = if (topCategories.isNotEmpty()) {
            topCategories.mapIndexed { idx, pair ->
                "  ${idx + 1}. ${pair.first}: Rp ${rupiahFormat.format(pair.second.toLong())}"
            }.joinToString("\n")
        } else {
            "  (Belum ada pengeluaran tercatat)"
        }

        // Pilar 3: Anggaran / Budget Status
        val budgetStatusText = when {
            overBudgets.isNotEmpty() -> {
                val listOver = overBudgets.joinToString(", ") {
                    "${it.budget.name ?: it.budget.category} (Terpakai Rp ${rupiahFormat.format(it.spentAmount.toLong())} / Limit Rp ${rupiahFormat.format(it.budget.limitAmount.toLong())} - ${it.percentage}%)"
                }
                "JEBOL! Ada ${overBudgets.size} anggaran jebol: $listOver."
            }
            criticalBudgets.isNotEmpty() -> {
                val listCrit = criticalBudgets.joinToString(", ") {
                    "${it.budget.name ?: it.budget.category} (${it.percentage}% terpakai)"
                }
                "WASPADA! Ada ${criticalBudgets.size} anggaran kritis di atas 80%: $listCrit."
            }
            else -> {
                "Aman terkendali (tidak ada anggaran jebol atau kritis)."
            }
        }

        return """
            Konteks Finansial Menyeluruh Aji ($periodTitle):

            [1] Ringkasan Saldo & Rekening:
            - Total Kas Bersih (Net Worth): Rp ${rupiahFormat.format(totalNetWorth.toLong())}
            - Rincian Akun:
            $accountLines

            [2] Arus Kas Periode Ini:
            - Total Pemasukan: Rp ${rupiahFormat.format(totalIncome.toLong())}
            - Total Pengeluaran: Rp ${rupiahFormat.format(totalExpense.toLong())}
            - Sisa Kas / Tabungan Bersih: Rp ${rupiahFormat.format(netSavings.toLong())}
            - Rasio Tabungan (Saving Rate): $savingRate%
            - Top 3 Pos Pengeluaran:
            $categoryLines
            - Total Catatan Transaksi: $txCount transaksi

            [3] Status Anggaran / Budget:
            - $budgetStatusText

            Instruksi Pak Hemat:
            Evaluasi keuangan Aji secara blak-blakan, matematis, sarkas-kocak, dan tanpa basa-basi formal sesuai persona Pak Hemat. Analisis korelasi antara total saldo rekening, kebocoran pos belanja, dan status anggaran jebol. Roasting secara kontekstual dan kasih 1 instruksi konkret penyelamatan kas!
        """.trimIndent()
    }

    /**
     * Generator analisis aturan lokal berbasis kalkulasi holistik (Saldo, Budget, Kas) dengan persona Pak Hemat.
     * Dipanggil saat koneksi offline, kuota habis, atau terjadi error jaringan.
     */
    fun generateLocalFallbackInsight(
        totalNetWorth: Double,
        accounts: List<AccountEntity>,
        overBudgets: List<BudgetProgressItem>,
        criticalBudgets: List<BudgetProgressItem>,
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

        val budgetJebolNotice = if (overBudgets.isNotEmpty()) {
            val b = overBudgets.first()
            " Anggaran '${b.budget.name ?: b.budget.category}' jebol parah nyentuh ${b.percentage}% (Rp ${rupiahFormat.format(b.spentAmount.toLong())})."
        } else if (criticalBudgets.isNotEmpty()) {
            val b = criticalBudgets.first()
            " Anggaran '${b.budget.name ?: b.budget.category}' udah sekarat di ${b.percentage}%."
        } else {
            ""
        }

        val accountWarning = if (totalNetWorth < 100_000 && accounts.isNotEmpty()) {
            " Total sisa saldomu cuma Rp ${rupiahFormat.format(totalNetWorth.toLong())}, lampu merah menyala!"
        } else {
            ""
        }

        return when {
            totalIncome <= 0 && totalExpense > 0 -> {
                "Waduh Bos, kamu boncos Rp ${rupiahFormat.format(totalExpense.toLong())} di $periodTitle padahal pemasukan masih nol melompong! Kategori '$topCategoryName' nembus Rp ${rupiahFormat.format(topCategoryAmount.toLong())}, $categoryRoast$budgetJebolNotice$accountWarning Rem darurat jajan lu hari ini juga sebelum kas sekarat total!"
            }
            totalIncome > 0 && totalExpense > totalIncome -> {
                "Defisit parah, Bos! Pengeluaranmu tembus Rp ${rupiahFormat.format(totalExpense.toLong())} numpahin pemasukan sampai minus Rp ${rupiahFormat.format((-netSavings).toLong())}. Kategori '$topCategoryName' (Rp ${rupiahFormat.format(topCategoryAmount.toLong())}) jadi biang keroknya, $categoryRoast$budgetJebolNotice$accountWarning Pangkas pengeluaran sekunder detik ini juga, jangan sok sultan!"
            }
            overBudgets.isNotEmpty() -> {
                "Anggaran kamu jebol berantakan, Ji! Kategori '$topCategoryName' nelan Rp ${rupiahFormat.format(topCategoryAmount.toLong())}.$budgetJebolNotice Total saldo semua rekening tinggal Rp ${rupiahFormat.format(totalNetWorth.toLong())}. Setop gesek kartu atau checkout aplikasi sekarang juga!"
            }
            savingRate in 0..19 -> {
                "Saving rate kamu cuma $savingRate% di $periodTitle, tipis banget kayak tisu basah, Ji! Duitmu habis disedot '$topCategoryName' sebesar Rp ${rupiahFormat.format(topCategoryAmount.toLong())}, $categoryRoast$budgetJebolNotice Evaluasi kebiasaan impulsif ini biar ada dana darurat yang waras."
            }
            savingRate in 20..49 -> {
                "Cashflow kamu masih napas aman dengan saving rate $savingRate% dan sisa kas Rp ${rupiahFormat.format(netSavings.toLong())}, Ji. Tapi jangan santai dulu karena '$topCategoryName' udah nelan Rp ${rupiahFormat.format(topCategoryAmount.toLong())}.$budgetJebolNotice Kunci sisa saldo ke tabungan sebelum nafsu belanja kumat lagi!"
            }
            else -> {
                "Wih tumben waras, Ji! Saving rate kamu tembus $savingRate% di $periodTitle dengan surplus Rp ${rupiahFormat.format(netSavings.toLong())}, total kas bersih Rp ${rupiahFormat.format(totalNetWorth.toLong())}, dan pengeluaran terbesar di '$topCategoryName' terkontrol rapi. Segera amankan sisa saldo ke tabungan atau investasi sebelum godaan promo merusak kedisiplinan ini!"
            }
        }
    }
}
