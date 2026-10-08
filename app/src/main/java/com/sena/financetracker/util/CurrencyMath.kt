package com.sena.financetracker.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

/**
 * Utilitas presisi kalkulasi finansial untuk mencegah binary floating-point drift.
 */
object CurrencyMath {

    /**
     * Membulatkan angka ke 2 tempat desimal standar finansial menggunakan RoundingMode.HALF_UP.
     */
    fun roundCurrency(amount: Double): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        return BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Mengonversi Double ke nominal bulat Rupiah (Long) tanpa truncation bias.
     */
    fun toRupiahLong(amount: Double): Long {
        if (amount.isNaN() || amount.isInfinite()) return 0L
        return amount.roundToLong()
    }
}
