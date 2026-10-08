package com.sena.financetracker.ui.dashboard.components.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.util.formatCompactAmount
import com.sena.financetracker.viewmodel.CashflowBarItem
import kotlin.math.max

/**
 * Komponen kartu grafik arus kas (Cashflow) bulanan dengan gaya Neobrutalisme.
 *
 * Menampilkan perbandingan pemasukan (income) dan pengeluaran (expense) dalam bentuk diagram
 * batang bersebelahan, dilengkapi dengan header bertumpuk anti-truncation (judul di atas dan
 * legenda di bawah), garis batas maksimum (horizontal dashed gridline), indikator teks skala
 * tertinggi (safeMax), serta label nominal ringkas terkoordinasi warna (hijau gelap untuk income
 * dan merah gelap untuk expense) dengan pemisahan ruang horizontal individual per batang
 * untuk mengeliminasi sepenuhnya angka bertumpuk atau dobel.
 *
 * @param cashflowBars Daftar data batang arus kas ([CashflowBarItem]) per bulan/periode.
 */
@Composable
fun CashflowChartCard(
    cashflowBars: List<CashflowBarItem>
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, RectangleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RectangleShape)
                .border(3.dp, Color.Black, RectangleShape)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ARUS KAS MASUK VS KELUAR",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                )

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(RetroIncomeGreen, RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                        )
                        Text(
                            text = "Masuk",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFDC2626), RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                        )
                        Text(
                            text = "Keluar",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cashflowBars.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF8FAFC), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Data grafik belum tersedia.",
                        style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 11.sp)
                    )
                }
            } else {
                val maxVal = cashflowBars.maxOfOrNull { max(it.income, it.expense) } ?: 1.0
                val safeMax = if (maxVal > 0) maxVal else 1.0

                // Canvas Diagram Batang Neobrutalisme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .background(Color(0xFFFAFAFA), RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val bottomLabelHeight = 24.dp.toPx() // sisakan tempat untuk label bawah
                        val canvasHeight = size.height - bottomLabelHeight
                        val topLabelPadding = 24.dp.toPx() // ruang atas agar teks nominal dan gridline tidak terpotong (clip)
                        val maxBarHeight = (canvasHeight - topLabelPadding).coerceAtLeast(10f)

                        val barGroupCount = cashflowBars.size
                        val groupWidth = canvasWidth / barGroupCount
                        val barWidth = 13.dp.toPx()
                        // Ruang antar batang dalam satu bulan (cukup lebar agar label masing-masing batang memiliki ruang horizontal sendiri)
                        val barSpacing = (groupWidth * 0.18f).coerceIn(12.dp.toPx(), 18.dp.toPx())

                        // Paint teks Neobrutalisme untuk label pemasukan (Income - deep green emerald)
                        val incomeTextPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#065F46")
                            textSize = 8.5.sp.toPx()
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        // Paint teks Neobrutalisme untuk label pengeluaran (Expense - deep retro dark red)
                        val expenseTextPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#991B1B")
                            textSize = 8.5.sp.toPx()
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        // Paint teks Neobrutalisme untuk label skala nilai maksimum
                        val scaleTextPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 8.sp.toPx()
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }

                        // Garis dasar horizontal hitam
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, canvasHeight),
                            end = Offset(canvasWidth, canvasHeight),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Garis batas nilai maksimum (gridline horizontal dashed line)
                        val maxLineY = canvasHeight - maxBarHeight
                        drawLine(
                            color = Color.Black.copy(alpha = 0.25f),
                            start = Offset(0f, maxLineY),
                            end = Offset(canvasWidth, maxLineY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Teks label skala maksimum di pojok kanan atas di atas gridline
                        val maxScaleText = "Maks: " + formatCompactAmount(safeMax)
                        drawContext.canvas.nativeCanvas.drawText(
                            maxScaleText,
                            canvasWidth - 2.dp.toPx(),
                            maxLineY - 4.dp.toPx(),
                            scaleTextPaint
                        )

                        // Ambang batas nominal minimal (abaikan nilai < 1% dari nilai maksimum agar tidak membuat label hantu / tumpukan 0)
                        val minLabelThreshold = safeMax * 0.01

                        cashflowBars.forEachIndexed { index, item ->
                            val groupCenterX = (index * groupWidth) + (groupWidth / 2)
                            val incomeBarX = groupCenterX - barWidth - (barSpacing / 2)
                            val expenseBarX = groupCenterX + (barSpacing / 2)

                            val incomeHeight = ((item.income / safeMax) * maxBarHeight).toFloat().coerceAtLeast(0f)
                            val expenseHeight = ((item.expense / safeMax) * maxBarHeight).toFloat().coerceAtLeast(0f)

                            val hasIncome = item.income > 0 && incomeHeight > 0f
                            val hasExpense = item.expense > 0 && expenseHeight > 0f

                            val incomeTopY = if (hasIncome) canvasHeight - incomeHeight else canvasHeight
                            val expenseTopY = if (hasExpense) canvasHeight - expenseHeight else canvasHeight

                            // 1. Gambar Bar Income (Hijau)
                            if (hasIncome) {
                                // Shadow bar 2.dp
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(incomeBarX + 2.dp.toPx(), incomeTopY + 2.dp.toPx()),
                                    size = Size(barWidth, incomeHeight)
                                )
                                // Isi bar
                                drawRect(
                                    color = Color(0xFF00E676),
                                    topLeft = Offset(incomeBarX, incomeTopY),
                                    size = Size(barWidth, incomeHeight)
                                )
                                // Border bar
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(incomeBarX, incomeTopY),
                                    size = Size(barWidth, incomeHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // 2. Gambar Bar Expense (Merah)
                            if (hasExpense) {
                                // Shadow bar 2.dp
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(expenseBarX + 2.dp.toPx(), expenseTopY + 2.dp.toPx()),
                                    size = Size(barWidth, expenseHeight)
                                )
                                // Isi bar
                                drawRect(
                                    color = Color(0xFFDC2626),
                                    topLeft = Offset(expenseBarX, expenseTopY),
                                    size = Size(barWidth, expenseHeight)
                                )
                                // Border bar
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(expenseBarX, expenseTopY),
                                    size = Size(barWidth, expenseHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            val incomeBarCenterX = incomeBarX + (barWidth / 2f)
                            val expenseBarCenterX = expenseBarX + (barWidth / 2f)

                            val canShowIncomeLabel = hasIncome && item.income >= minLabelThreshold
                            val canShowExpenseLabel = hasExpense && item.expense >= minLabelThreshold

                            if (canShowIncomeLabel && canShowExpenseLabel) {
                                val incomeText = formatCompactAmount(item.income)
                                val expenseText = formatCompactAmount(item.expense)
                                val incomeTextWidth = incomeTextPaint.measureText(incomeText)
                                val expenseTextWidth = expenseTextPaint.measureText(expenseText)

                                val incomeRightEdge = incomeBarCenterX + (incomeTextWidth / 2f)
                                val expenseLeftEdge = expenseBarCenterX - (expenseTextWidth / 2f)
                                val isHorizontalColliding = (incomeRightEdge + 3.dp.toPx()) > expenseLeftEdge

                                if (isHorizontalColliding) {
                                    // Jika bertabrakan secara horizontal karena ruang sempit, tampilkan HANYA nilai yang dominan (lebih besar)
                                    // tepat di atas batangnya sendiri untuk mengeliminasi sepenuhnya kesan angka dobel / menumpuk vertikal.
                                    if (item.income >= item.expense) {
                                        drawContext.canvas.nativeCanvas.drawText(
                                            incomeText,
                                            incomeBarCenterX,
                                            incomeTopY - 4.dp.toPx(),
                                            incomeTextPaint
                                        )
                                    } else {
                                        drawContext.canvas.nativeCanvas.drawText(
                                            expenseText,
                                            expenseBarCenterX,
                                            expenseTopY - 4.dp.toPx(),
                                            expenseTextPaint
                                        )
                                    }
                                } else {
                                    // Ruang horizontal cukup: gambar masing-masing label tepat di atas batangnya sendiri
                                    drawContext.canvas.nativeCanvas.drawText(
                                        incomeText,
                                        incomeBarCenterX,
                                        incomeTopY - 4.dp.toPx(),
                                        incomeTextPaint
                                    )
                                    drawContext.canvas.nativeCanvas.drawText(
                                        expenseText,
                                        expenseBarCenterX,
                                        expenseTopY - 4.dp.toPx(),
                                        expenseTextPaint
                                    )
                                }
                            } else if (canShowIncomeLabel) {
                                val incomeText = formatCompactAmount(item.income)
                                drawContext.canvas.nativeCanvas.drawText(
                                    incomeText,
                                    incomeBarCenterX,
                                    incomeTopY - 4.dp.toPx(),
                                    incomeTextPaint
                                )
                            } else if (canShowExpenseLabel) {
                                val expenseText = formatCompactAmount(item.expense)
                                drawContext.canvas.nativeCanvas.drawText(
                                    expenseText,
                                    expenseBarCenterX,
                                    expenseTopY - 4.dp.toPx(),
                                    expenseTextPaint
                                )
                            }
                        }
                    }

                    // Label teks bulan di bawah diagram
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        cashflowBars.forEach { item ->
                            Text(
                                text = item.label,
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
