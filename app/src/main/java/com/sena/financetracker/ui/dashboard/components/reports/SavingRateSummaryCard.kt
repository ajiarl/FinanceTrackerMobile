package com.sena.financetracker.ui.dashboard.components.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah

@Composable
fun SavingRateSummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    netSavings: Double,
    savingRate: Int,
    savingStatus: String
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
                    text = "RASIO TABUNGAN (SAVING RATE)",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF64748B)
                    )
                )

                val badgeBg = when (savingStatus) {
                    "HEMAT" -> RetroIncomeGreen
                    "NORMAL" -> RetroYellow
                    else -> Color(0xFFDC2626)
                }
                val badgeText = when (savingStatus) {
                    "HEMAT" -> Color.Black
                    "NORMAL" -> Color.Black
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = savingStatus,
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = badgeText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$savingRate%",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-1).sp,
                        color = Color.Black
                    )
                )
                Text(
                    text = if (netSavings >= 0) "Surplus Rp ${formatRupiah(netSavings)}" else "Defisit Rp ${formatRupiah(netSavings)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (netSavings >= 0) RetroIncomeGreen else Color(0xFFDC2626)
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-box perbandingan Pemasukan vs Pengeluaran
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Income
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF0FDF4), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "PEMASUKAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color(0xFF15803D)
                            )
                        )
                        Text(
                            text = "Rp " + formatRupiah(totalIncome),
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        )
                    }
                }

                // Expense
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFEF2F2), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "PENGELUARAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color(0xFFDC2626)
                            )
                        )
                        Text(
                            text = "Rp " + formatRupiah(totalExpense),
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}
