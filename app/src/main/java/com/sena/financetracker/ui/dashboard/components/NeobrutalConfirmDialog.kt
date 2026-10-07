package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog konfirmasi Neobrutal modal untuk tindakan berisiko/destruktif seperti
 * menghapus transaksi atau anggaran.
 *
 * Mengikuti spesifikasi Strict Neobrutalism:
 * - Border tebal 3.dp hitam pekat
 * - Hard drop shadow kotak 6.dp tanpa blur
 * - Tombol Batal bergaya netral & Tombol Konfirmasi merah tegas
 */
@Composable
fun NeobrutalConfirmDialog(
    title: String,
    message: String,
    confirmButtonText: String = "HAPUS",
    cancelButtonText: String = "BATAL",
    confirmButtonColor: Color = Color(0xFFDC2626), // Merah retro tegas
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            // Hard drop shadow kotak
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(Color.Black, RectangleShape)
            )

            // Kontainer Utama Dialog
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RectangleShape)
                    .border(3.dp, Color.Black, RectangleShape)
                    .padding(20.dp)
            ) {
                // Header Judul
                Text(
                    text = title.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pesan Deskripsi
                Text(
                    text = message,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Aksi (Batal & Konfirmasi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol BATAL
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .background(Color(0xFFE2E8F0), RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = cancelButtonText.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Tombol KONFIRMASI (Merah)
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    Box {
                        // Shadow tombol
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 3.dp, y = 3.dp)
                                .background(Color.Black, RectangleShape)
                        )

                        Box(
                            modifier = Modifier
                                .clickable {
                                    try {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                    onConfirm()
                                }
                                .background(confirmButtonColor, RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape)
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = confirmButtonText.uppercase(),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
