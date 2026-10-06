package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.RetroExpenseDarkRed
import com.sena.financetracker.ui.components.RetroIncomeDarkGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah

@Composable
fun DashboardBalanceSection(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currentMonthText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Total Saldo Card (Retro Hitam Kuning) ─────────────────────────
        NeobrutalCard(
            backgroundColor = Color.Black,
            borderWidth = 3.dp,
            shadowOffset = 5.dp,
            shadowColor = RetroYellow
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL SALDO",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Wallet,
                        contentDescription = null,
                        tint = RetroYellow.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatRupiah(totalBalance),
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-1).sp,
                        color = RetroYellow,
                        fontFeatureSettings = "tnum"
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "PER HARI INI • $currentMonthText",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // ── 2. Pemasukan & Pengeluaran Grid ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pemasukan Card
            NeobrutalCard(
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFF0FDF4),
                borderWidth = 2.dp,
                shadowOffset = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PEMASUKAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = RetroIncomeDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRupiah(totalIncome),
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = RetroIncomeDarkGreen,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }

            // Pengeluaran Card
            NeobrutalCard(
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFFEF2F2),
                borderWidth = 2.dp,
                shadowOffset = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PENGELUARAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = RetroExpenseDarkRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRupiah(totalExpense),
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = RetroExpenseDarkRed,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
        }
    }
}
