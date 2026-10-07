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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroIncomeGreen
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
                        .height(160.dp)
                        .background(Color(0xFFFAFAFA), RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height - 24.dp.toPx() // sisakan tempat untuk label bawah
                        val barGroupCount = cashflowBars.size
                        val groupWidth = canvasWidth / barGroupCount
                        val barWidth = 14.dp.toPx()
                        val barSpacing = 4.dp.toPx()

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

                            val incomeHeight = ((item.income / safeMax) * (canvasHeight - 10.dp.toPx())).toFloat().coerceAtLeast(0f)
                            val expenseHeight = ((item.expense / safeMax) * (canvasHeight - 10.dp.toPx())).toFloat().coerceAtLeast(0f)

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
