package com.sena.financetracker.ui.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.DashboardAccountsSection
import com.sena.financetracker.ui.dashboard.components.DashboardBalanceSection
import com.sena.financetracker.util.formatRupiah
import com.sena.financetracker.viewmodel.FinanceUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab 1 - Beranda (HomeScreen):
 * - Header & Kartu Ringkasan Keuangan (Total Saldo, Income, Expense).
 * - Ringkasan Rekening Akun (compact horizontal cards).
 * - Preview 5 Transaksi Terakhir + Tombol Neobrutal "LIHAT SEMUA TRANSAKSI" (navigasi ke tab Transaksi).
 */
@Composable
fun HomeScreen(
    uiState: FinanceUiState,
    onTransferClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (AccountEntity) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentMonthText = remember {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date()).uppercase()
    }

    val latestTransactions = remember(uiState.transactions) {
        uiState.transactions.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header with Notification Bell
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RINGKASAN KEUANGAN",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = "HALO, AJI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp,
                            color = Color.Black
                        )
                    )
                }

                // Tombol Lonceng Neobrutal
                Box(
                    modifier = Modifier
                        .clickable(onClick = onNavigateToNotifications)
                ) {
                    // Drop shadow kotak 3.dp
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .offset(x = 3.dp, y = 3.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    // Button body neobrutal
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Pusat Notifikasi",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Badge Merah Unread Count
                    if (uiState.unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 28.dp, y = (-4).dp)
                                .background(Color(0xFFDC2626), RectangleShape)
                                .border(1.5.dp, Color.Black, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (uiState.unreadNotificationCount > 99) "99+" else uiState.unreadNotificationCount.toString(),
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Saldo & Arus Kas Bulanan
        item {
            DashboardBalanceSection(
                totalBalance = uiState.totalBalance,
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
                currentMonthText = currentMonthText
            )
        }

        // Akun & Dompet Ringkas
        item {
            DashboardAccountsSection(
                accounts = uiState.accounts,
                onTransferClick = onTransferClick,
                onAddAccountClick = onAddAccountClick,
                onAccountClick = onAccountClick
            )
        }

        // Preview Transaksi Terbaru
        item {
            RecentTransactionsPreviewCard(
                recentTransactions = latestTransactions,
                onSeeAllClick = onNavigateToTransactions
            )
        }
    }
}

@Composable
private fun RecentTransactionsPreviewCard(
    recentTransactions: List<TransactionEntity>,
    onSeeAllClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Hard drop shadow 6.dp kotak
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, RectangleShape)
        )
        // Kartu Utama Neobrutal
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
                    text = "TRANSAKSI TERAKHIR",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                )
                Box(
                    modifier = Modifier
                        .background(Color.Black, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${recentTransactions.size} TERBARU",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi tercatat.",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentTransactions.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RectangleShape)
                                .border(1.5.dp, Color.Black, RectangleShape)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.Black
                                    )
                                )
                                Text(
                                    text = "${tx.category} • ${tx.accountName}",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                )
                            }
                            val isIncome = tx.type.equals("INCOME", ignoreCase = true)
                            val isExpense = tx.type.equals("EXPENSE", ignoreCase = true)
                            val prefix = if (isIncome) "+Rp " else if (isExpense) "-Rp " else "Rp "
                            val amountColor = if (isIncome) RetroIncomeGreen else if (isExpense) Color(0xFFDC2626) else Color.Black

                            Text(
                                text = prefix + formatRupiah(tx.amount),
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = amountColor
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Neobrutal "LIHAT SEMUA TRANSAKSI"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSeeAllClick)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 3.dp, y = 3.dp)
                        .background(Color.Black, RectangleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetroYellow, RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LIHAT SEMUA TRANSAKSI →",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = Color.Black
                        )
                    )
                }
            }
        }
    }
}
