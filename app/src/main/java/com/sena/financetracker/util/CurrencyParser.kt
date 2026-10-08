package com.sena.financetracker.util

/**
 * Utilitas parsing format mata uang yang ramah locale Indonesia & internasional.
 * Menangani pemisah ribuan (.) dan desimal (,), serta membersihkan prefix seperti "Rp".
 */
object CurrencyParser {

    /**
     * Mem-parsing teks input mata uang menjadi angka [Double] yang presisi.
     *
     * Logika:
     * a. Bersihkan whitespace, prefix "Rp"/"rp", dan karakter selain digit, koma, titik, minus.
     * b. Jika mengandung titik (.) dan koma (,):
     *    - Jika titik sebelum koma (misal "50.000,50"): hapus titik, ganti koma dengan titik desimal.
     *    - Jika koma sebelum titik (misal "50,000.50"): hapus koma, biarkan titik desimal.
     * c. Jika hanya mengandung titik (.):
     *    - Pisahkan berdasarkan titik terakhir. Jika bagian setelah titik terdiri dari tepat 3 digit
     *      (atau jika ada multiple titik seperti "1.000.000"), anggap sebagai pemisah ribuan -> hapus titik.
     *    - Jika bagian setelah titik 1-2 digit: anggap sebagai desimal.
     * d. Jika hanya mengandung koma (,):
     *    - Jika bagian setelah koma terdiri dari 1-2 digit: ganti koma dengan titik desimal.
     *    - Jika 3 digit: anggap sebagai pemisah ribuan -> hapus koma.
     * e. Kembalikan CurrencyMath.roundCurrency(parsed.toDoubleOrNull() ?: 0.0).
     */
    fun parseCurrencyInput(input: String): Double {
        var cleaned = input.trim()
        if (cleaned.isEmpty()) return 0.0

        // Hapus prefix Rp atau rp (case-insensitive)
        if (cleaned.startsWith("rp", ignoreCase = true)) {
            cleaned = cleaned.substring(2).trim()
        }

        // Simpan tanda minus jika ada di awal
        val isNegative = cleaned.startsWith("-")
        if (isNegative) {
            cleaned = cleaned.substring(1).trim()
        }

        // Hanya izinkan digit, '.', dan ','
        cleaned = cleaned.filter { it.isDigit() || it == '.' || it == ',' }
        if (cleaned.isEmpty()) return 0.0

        val hasDot = cleaned.contains('.')
        val hasComma = cleaned.contains(',')

        val normalized: String = when {
            hasDot && hasComma -> {
                val firstDot = cleaned.indexOf('.')
                val firstComma = cleaned.indexOf(',')
                if (firstDot < firstComma) {
                    // Contoh: "50.000,50" -> hapus semua titik, ganti koma dengan titik
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    // Contoh: "50,000.50" -> hapus semua koma
                    cleaned.replace(",", "")
                }
            }
            hasDot -> {
                val lastDotIndex = cleaned.lastIndexOf('.')
                val afterDot = cleaned.substring(lastDotIndex + 1)
                val dotCount = cleaned.count { it == '.' }

                if (dotCount > 1) {
                    // Contoh: "1.000.000" atau "1.500.000"
                    cleaned.replace(".", "")
                } else {
                    // Single dot: misal "50.000" vs "50.5"
                    if (afterDot.length == 3) {
                        // Pemisah ribuan -> hapus titik
                        cleaned.replace(".", "")
                    } else {
                        // 1-2 digit desimal (atau lainnya) -> pertahankan sebagai desimal
                        cleaned
                    }
                }
            }
            hasComma -> {
                val lastCommaIndex = cleaned.lastIndexOf(',')
                val afterComma = cleaned.substring(lastCommaIndex + 1)
                val commaCount = cleaned.count { it == ',' }

                if (commaCount > 1) {
                    cleaned.replace(",", "")
                } else {
                    if (afterComma.length == 3) {
                        // Ribuan misal "50,000"
                        cleaned.replace(",", "")
                    } else {
                        // 1-2 digit desimal misal "50000,50"
                        cleaned.replace(',', '.')
                    }
                }
            }
            else -> cleaned
        }

        val rawDouble = normalized.toDoubleOrNull() ?: return 0.0
        val finalVal = if (isNegative) -rawDouble else rawDouble
        return CurrencyMath.roundCurrency(finalVal)
    }
}
