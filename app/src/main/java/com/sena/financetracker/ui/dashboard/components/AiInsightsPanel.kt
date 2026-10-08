package com.sena.financetracker.ui.dashboard.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroYellow

private val AmberNeobrutal = Color(0xFFFCD34D)
private val DarkShadow = Color(0xFF111827)

/**
 * Komponen panel kecerdasan buatan "Pak Hemat · AI Insight" Strict Neobrutalism:
 * - Kartu Neobrutal kuning Amber (Color(0xFFFCD34D)), border 3.dp hitam pekat, hard drop shadow offset 4.dp.
 * - Avatar Neobrutal kotak hitam "PH" (Pak Hemat) beraksen teks kuning.
 * - Header label "PAK HEMAT · AI INSIGHT" + tombol refresh Neobrutal dengan state animasi berputar saat loading.
 * - Konten evaluasi rasio keuangan atau pesan fallback jika offline / belum ada data.
 */
@Composable
fun AiInsightsPanel(
    insightText: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animasi putar ikon refresh saat sedang memuat
    val infiniteTransition = rememberInfiniteTransition(label = "RefreshRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Hard drop shadow Neobrutal offset 4.dp
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(DarkShadow, RectangleShape)
        )

        // Kartu Utama Amber
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmberNeobrutal, RectangleShape)
                .border(3.dp, DarkShadow, RectangleShape)
                .padding(16.dp)
        ) {
            // Header: Avatar PH + Label + Tombol Refresh Neobrutal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar Kotak Neobrutal "PH"
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkShadow, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PH",
                            style = TextStyle(
                                color = RetroYellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = DarkShadow,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "PAK HEMAT",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp,
                                    color = DarkShadow
                                )
                            )
                        }
                        Text(
                            text = "AI INSIGHT · ADVISOR",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                color = DarkShadow.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Tombol Refresh Neobrutal
                val context = androidx.compose.ui.platform.LocalContext.current
                Box(
                    modifier = Modifier.clickable(enabled = !isLoading) {
                        com.sena.financetracker.ui.components.NeobrutalHapticEngine.tick(context, true)
                        onRefresh()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(DarkShadow, RectangleShape)
                    )
                    Row(
                        modifier = Modifier
                            .background(if (isLoading) Color(0xFFE5E7EB) else Color.White, RectangleShape)
                            .border(2.dp, DarkShadow, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Analisis Ulang",
                            tint = DarkShadow,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(if (isLoading) rotationAngle else 0f)
                        )
                        Text(
                            text = if (isLoading) "ANALISIS..." else "REFRESH",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = DarkShadow
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Box untuk Konten Insight AI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RectangleShape)
                    .border(2.dp, DarkShadow, RectangleShape)
                    .padding(12.dp)
            ) {
                when {
                    isLoading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sedang membedah transaksi & kalkulasi rasio kas...",
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = DarkShadow.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                    !errorMessage.isNullOrBlank() -> {
                        Text(
                            text = "Error: $errorMessage",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                        )
                    }
                    !insightText.isNullOrBlank() -> {
                        Text(
                            text = insightText,
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = DarkShadow
                            )
                        )
                    }
                    else -> {
                        Text(
                            text = "Tekan tombol REFRESH untuk meminta Pak Hemat menganalisis kesehatan keuangan periode ini secara matematis.",
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = DarkShadow.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}
