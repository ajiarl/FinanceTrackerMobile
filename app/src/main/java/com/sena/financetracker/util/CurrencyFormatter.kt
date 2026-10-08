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
