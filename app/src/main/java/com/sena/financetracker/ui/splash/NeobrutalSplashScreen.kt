package com.sena.financetracker.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroCyberMint
import com.sena.financetracker.ui.components.RetroYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Layar pembuka (Splash Screen) dengan Strict Neobrutalism UI saat aplikasi pertama kali dimuat.
 *
 * Elemen visual & fungsional:
 * - Latar belakang RetroYellow (#FAFF00) solid kontras tinggi.
 * - Logo Neobrutal CyberMint (#00F0FF) kotak tajam (RectangleShape) berborder hitam 3.dp,
 *   dilengkapi hard drop shadow 6.dp hitam pekat dan simbol mata uang '$' hitam tebal.
 * - Judul aplikasi "FinanceTracker" tebal (FontWeight.Black) dan badge status offline-first.
 * - Animasi pop masuk (spring scale) dan transisi mulus ke dashboard setelah jeda waktu selesai.
 * - Mendukung tap layar untuk melewati (skip) splash screen secara instan tanpa menunggu durasi.
 *
 * @param onSplashFinished Callback yang dipanggil saat durasi splash selesai atau pengguna mengetuk layar.
 * @param modifier Modifier penataan tata letak Compose.
 * @param splashDurationMillis Durasi tayang splash screen dalam milidetik sebelum transisi (default: 1000L).
 */
@Composable
fun NeobrutalSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    splashDurationMillis: Long = 1000L
) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300)
            )
        }
        delay(splashDurationMillis)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RetroYellow)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            // ── Neobrutal Logo Card: CyberMint ($) ──────────────────────────
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .padding(bottom = 6.dp, end = 6.dp)
            ) {
                // Hard offset shadow hitam pekat (6.dp)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 6.dp, y = 6.dp)
                        .background(Color.Black, RectangleShape)
                )

                // Kartu utama Neobrutal CyberMint
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(RetroCyberMint, RectangleShape)
                        .border(3.dp, Color.Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$",
                        style = TextStyle(
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Title: FinanceTracker Bold ───────────────────────────────────
            Text(
                text = "FinanceTracker",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Neobrutal Subtitle Badge ─────────────────────────────────────
            Box(
                modifier = Modifier.padding(bottom = 3.dp, end = 3.dp)
            ) {
                // Shadow badge hitam pekat (3.dp)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 3.dp, y = 3.dp)
                        .background(Color.Black, RectangleShape)
                )
                // Badge container
                Box(
                    modifier = Modifier
                        .background(Color.White, RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "OFFLINE-FIRST PERSONAL FINANCE",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    )
                }
            }
        }

        // ── Bottom Architecture Watermark ─────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "LEVEL 3 • 100% PRIVATE & LOCAL ROOM DB",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            )
        }
    }
}
