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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
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
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalBadge
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.RetroExpenseDarkRed
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeDarkGreen
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah

@Composable
fun DashboardTransactionsSection(
    transactions: List<TransactionEntity>,
    selectedFilterTab: String,
    onFilterTabSelected: (String) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Section Header ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RIWAYAT TRANSAKSI",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )
            )
            NeobrutalBadge(
                text = "${transactions.size} TOTAL",
                backgroundColor = Color.White
            )
        }

        // ── 2. Filter Tabs (Semua / Keluar / Masuk) ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .background(Color.Black, RectangleShape)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RectangleShape)
                    .border(2.dp, Color.Black, RectangleShape)
            ) {
                val filterTabs = listOf(
                    Pair("ALL", "SEMUA"),
                    Pair("EXPENSE", "PENGELUARAN"),
                    Pair("INCOME", "PEMASUKAN")
                )
                filterTabs.forEachIndexed { index, (key, label) ->
                    val isSelected = selectedFilterTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSelected) RetroYellow else Color.White)
                            .clickable { onFilterTabSelected(key) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                        )
                    }
                    if (index < filterTabs.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(38.dp)
                                .background(Color.Black)
                        )
                    }
                }
            }
        }

        // ── 3. Transactions List or Empty State ───────────────────────────
        if (transactions.isEmpty()) {
            NeobrutalCard(
                backgroundColor = Color.White,
                borderWidth = 2.dp,
                shadowOffset = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BELUM ADA TRANSAKSI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tekan tombol (+) kuning di kanan bawah untuk mencatat transaksi baru.",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                transactions.forEach { tx ->
                    TransactionNeobrutalItem(
                        transaction = tx,
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionNeobrutalItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type.equals("INCOME", ignoreCase = true)
    val accentColor = if (isIncome) RetroIncomeDarkGreen else RetroExpenseDarkRed
    val amountPrefix = if (isIncome) "+" else "-"

    NeobrutalCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon Badge Box (Square 38x38 with 2.dp black border)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isIncome) RetroIncomeGreen else RetroExpenseRed,
                        RectangleShape
                    )
                    .border(2.dp, Color.Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Description, Category & Account Badges
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.Black
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalBadge(
                        text = transaction.category,
                        backgroundColor = Color(0xFFF1F5F9),
                        textColor = Color.Black
                    )

                    NeobrutalBadge(
                        text = transaction.accountName,
                        backgroundColor = RetroYellow.copy(alpha = 0.5f),
                        textColor = Color.Black
                    )

                    Text(
                        text = transaction.date,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    )
                }
            }

            // Amount & Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$amountPrefix${formatRupiah(transaction.amount)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = accentColor,
                        fontFeatureSettings = "tnum"
                    )
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(RetroExpenseRed, RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
