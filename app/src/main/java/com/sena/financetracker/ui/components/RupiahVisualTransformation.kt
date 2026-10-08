package com.sena.financetracker.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation untuk memformat angka dengan pemisah ribuan titik (.) khas Indonesia.
 * Contoh: "1000" -> "1.000", "5000000" -> "5.000.000".
 * Menangani input desimal dengan koma atau titik secara aman tanpa out-of-bounds offset cursor.
 */
class RupiahVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Tentukan apakah terdapat tanda minus di awal
        val isNegative = original.startsWith("-")
        val contentWithoutSign = if (isNegative) original.substring(1) else original
        val signPrefix = if (isNegative) "-" else ""

        // Pisahkan bagian integer dan desimal (mendukung koma atau titik sebagai pemisah desimal jika ada)
        val decimalIndex = contentWithoutSign.indexOfAny(charArrayOf(',', '.'))
        val integerPart = if (decimalIndex != -1) contentWithoutSign.substring(0, decimalIndex) else contentWithoutSign
        val decimalPartWithSep = if (decimalIndex != -1) contentWithoutSign.substring(decimalIndex) else ""

        // Format bagian integer dengan titik ribuan
        val formattedInt = StringBuilder()
        val intLen = integerPart.length

        // Array mapping index posisi kursor:
        // origToTrans[i] = offset transformed setelah i karakter original
        val origToTrans = IntArray(original.length + 1)
        // transToOrig[j] = offset original setelah j karakter transformed
        // Hitung dulu formatted string
        var curOrig = 0
        var curTrans = 0

        origToTrans[0] = 0

        if (isNegative) {
            curOrig++
            curTrans++
            origToTrans[1] = 1
        }

        for (i in 0 until intLen) {
            val char = integerPart[i]
            formattedInt.append(char)
            curTrans++
            val digitsRemaining = intLen - 1 - i
            if (digitsRemaining > 0 && digitsRemaining % 3 == 0) {
                formattedInt.append('.')
                curTrans++
            }
            curOrig++
            origToTrans[curOrig] = curTrans
        }

        // Tambahkan decimal part
        for (i in decimalPartWithSep.indices) {
            curTrans++
            curOrig++
            origToTrans[curOrig] = curTrans
        }

        val formattedText = signPrefix + formattedInt.toString() + decimalPartWithSep

        val transToOrig = IntArray(formattedText.length + 1)
        transToOrig[0] = 0
        for (origOffset in 0..original.length) {
            val transOffset = origToTrans[origOffset]
            if (transOffset in transToOrig.indices) {
                transToOrig[transOffset] = origOffset
            }
        }
        // Fill gaps in transToOrig (misalnya titik pemisah ribuan)
        for (j in 1..formattedText.length) {
            if (transToOrig[j] == 0 && j > 0) {
                // Jika posisi titik pemisah, petakan ke posisi original karakter sebelum titik
                transToOrig[j] = transToOrig[j - 1]
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, original.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, formattedText.length)]
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
