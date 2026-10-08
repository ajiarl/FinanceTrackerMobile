package com.sena.financetracker.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

fun formatRupiah(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,###", symbols)
    val absAmount = kotlin.math.round(abs(amount)).toLong()
    val formattedNumber = formatter.format(absAmount)

    return if (amount < 0) {
        "-Rp $formattedNumber"
    } else {
        "Rp $formattedNumber"
    }
}

/**
 * Memformat nominal angka numerik ke representasi ringkas/kompak mata uang Rupiah
 * untuk visualisasi chart dan label ruang terbatas (contoh: "1.2jt", "750rb", "1M", "0").
 *
 * Mengeliminasi trailing zero (tanpa ".0") serta menangani skala ribuan ('rb'),
 * jutaan ('jt'), dan miliaran ('M') secara presisi.
 *
 * @param amount Nilai nominal moneter (Double).
 * @return String format ringkas tanpa desimal redundan.
 */
fun formatCompactAmount(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = abs(amount)

    if (absAmount < 1.0) {
        return "0"
    }

    val (value, suffix) = when {
        absAmount >= 1_000_000_000.0 -> (absAmount / 1_000_000_000.0) to "M"
        absAmount >= 1_000_000.0 -> (absAmount / 1_000_000.0) to "jt"
        absAmount >= 1_000.0 -> (absAmount / 1_000.0) to "rb"
        else -> absAmount to ""
    }

    val symbols = DecimalFormatSymbols(Locale.US)
    val formatter = DecimalFormat("0.#", symbols)
    val formatted = formatter.format(value)

    val result = "$formatted$suffix"
    return if (isNegative && result != "0") "-$result" else result
}
