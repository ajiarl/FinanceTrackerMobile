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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(RetroIncomeGreen, RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                        )
                        Text(
                            text = " Masuk",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFDC2626), RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                        )
                        Text(
                            text = " Keluar",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        val topLabelPadding = 20.dp.toPx() // ruang atas agar teks nominal tidak terpotong (clip)
                        val maxBarHeight = (canvasHeight - topLabelPadding).coerceAtLeast(10f)

                        val barGroupCount = cashflowBars.size
                        val groupWidth = canvasWidth / barGroupCount
                        val barWidth = 14.dp.toPx()
                        val barSpacing = 4.dp.toPx()

                        // Paint teks Neobrutalisme untuk label nilai ringkas di atas batang
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 8.5.sp.toPx()
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        // Garis dasar horizontal hitam
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, canvasHeight),
                            end = Offset(canvasWidth, canvasHeight),
                            strokeWidth = 2.dp.toPx()
                        )

                        cashflowBars.forEachIndexed { index, item ->
                            val groupCenterX = (index * groupWidth) + (groupWidth / 2)
                            val incomeBarX = groupCenterX - barWidth - (barSpacing / 2)
                            val expenseBarX = groupCenterX + (barSpacing / 2)

                            val incomeHeight = ((item.income / safeMax) * maxBarHeight).toFloat().coerceAtLeast(0f)
                            val expenseHeight = ((item.expense / safeMax) * maxBarHeight).toFloat().coerceAtLeast(0f)

                            // 1. Gambar Bar Income (Hijau)
                            if (incomeHeight > 0f) {
                                val topY = canvasHeight - incomeHeight
                                // Shadow bar 2.dp
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(incomeBarX + 2.dp.toPx(), topY + 2.dp.toPx()),
                                    size = Size(barWidth, incomeHeight)
                                )
                                // Isi bar
                                drawRect(
                                    color = Color(0xFF00E676),
                                    topLeft = Offset(incomeBarX, topY),
                                    size = Size(barWidth, incomeHeight)
                                )
                                // Border bar
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(incomeBarX, topY),
                                    size = Size(barWidth, incomeHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Teks angka nominal ringkas di atas batang income jika nilai > 0
                                if (item.income > 0) {
                                    val incomeText = formatCompactAmount(item.income)
                                    val incomeBarCenterX = incomeBarX + (barWidth / 2f)
                                    drawContext.canvas.nativeCanvas.drawText(
                                        incomeText,
                                        incomeBarCenterX,
                                        topY - 4.dp.toPx(),
                                        textPaint
                                    )
                                }
                            }

                            // 2. Gambar Bar Expense (Merah)
                            if (expenseHeight > 0f) {
                                val topY = canvasHeight - expenseHeight
                                // Shadow bar 2.dp
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(expenseBarX + 2.dp.toPx(), topY + 2.dp.toPx()),
                                    size = Size(barWidth, expenseHeight)
                                )
                                // Isi bar
                                drawRect(
                                    color = Color(0xFFDC2626),
                                    topLeft = Offset(expenseBarX, topY),
                                    size = Size(barWidth, expenseHeight)
                                )
                                // Border bar
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(expenseBarX, topY),
                                    size = Size(barWidth, expenseHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Teks angka nominal ringkas di atas batang expense jika nilai > 0
                                if (item.expense > 0) {
                                    val expenseText = formatCompactAmount(item.expense)
                                    val expenseBarCenterX = expenseBarX + (barWidth / 2f)
                                    drawContext.canvas.nativeCanvas.drawText(
                                        expenseText,
                                        expenseBarCenterX,
                                        topY - 4.dp.toPx(),
                                        textPaint
                                    )
                                }
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
