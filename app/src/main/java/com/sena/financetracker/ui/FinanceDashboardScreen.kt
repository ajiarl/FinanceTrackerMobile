package com.sena.financetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sena.financetracker.ui.components.NeobrutalBadge
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.NeobrutalFastAddDialog
import com.sena.financetracker.ui.components.RetroAccountBlue
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroExpenseDarkRed
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeDarkGreen
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah
import com.sena.financetracker.viewmodel.FinanceUiState
import com.sena.financetracker.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilterTab by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"

    val currentMonthText = remember {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date()).uppercase()
    }

    val filteredTransactions = remember(uiState.transactions, selectedFilterTab) {
        when (selectedFilterTab) {
            "EXPENSE" -> uiState.transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            "INCOME" -> uiState.transactions.filter { it.type.equals("INCOME", ignoreCase = true) }
            else -> uiState.transactions
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RetroCanvas,
        floatingActionButton = {
            // Web FAB Replica (#FAFF00, 3.dp border black, 4.dp hard drop shadow)
            Box(
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .background(Color.Black, RectangleShape)
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(RetroYellow, RectangleShape)
                        .border(3.dp, Color.Black, RectangleShape)
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Transaksi",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Header Sapaan ──────────────────────────────────────────────
            item {
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
            }

            // ── 2. Total Saldo Card (Web Replica: Hitam Kuning Retro) ─────────
            item {
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
                            text = formatRupiah(uiState.totalBalance),
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
            }

            // ── 3. Pemasukan & Pengeluaran Grid ──────────────────────────────
            item {
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
                                text = formatRupiah(uiState.totalIncome),
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
                                text = formatRupiah(uiState.totalExpense),
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

            // ── 4. Section Ringkasan Accounts / Dompet ─────────────────────────
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AKUN & DOMPET",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                        )
                        NeobrutalBadge(
                            text = "${uiState.accounts.size} AKUN",
                            backgroundColor = RetroYellow
                        )
                    }

                    // Horizontal Accounts List
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.accounts.forEach { account ->
                            AccountNeobrutalItem(account = account)
                        }
                    }
                }
            }

            // ── 5. Section Riwayat Transaksi ──────────────────────────────────
            item {
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
                        text = "${filteredTransactions.size} TOTAL",
                        backgroundColor = Color.White
                    )
                }
            }

            // Filter Tabs (Semua / Keluar / Masuk)
            item {
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
                                    .clickable { selectedFilterTab = key }
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
            }

            // List of Transactions or Empty State
            if (filteredTransactions.isEmpty()) {
                item {
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
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionNeobrutalItem(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        NeobrutalFastAddDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, type, category, date, accountId, accountName, notes ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
                    accountId = accountId,
                    accountName = accountName,
                    notes = notes
                )
            }
        )
    }
}

@Composable
private fun AccountNeobrutalItem(
    account: AccountEntity,
    modifier: Modifier = Modifier
) {
    val badgeConfig = when (account.type.lowercase()) {
        "bank" -> Pair("BANK", Color(0xFF93C5FD))
        "e-wallet" -> Pair("E-WALLET", Color(0xFFFDE047))
        else -> Pair("TUNAI", Color(0xFF86EFAC))
    }

    NeobrutalCard(
        modifier = modifier.width(160.dp),
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp,
        fillMaxWidth = false
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeobrutalBadge(
                    text = badgeConfig.first,
                    backgroundColor = badgeConfig.second
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = account.name.uppercase(),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.Black
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SALDO",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black.copy(alpha = 0.5f)
                )
            )

            Text(
                text = formatRupiah(account.balance),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.Black,
                    fontFeatureSettings = "tnum"
                )
            )
        }
    }
}

@Composable
private fun TransactionNeobrutalItem(
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
