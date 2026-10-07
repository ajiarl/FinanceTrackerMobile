package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalBadge
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.RetroExpenseDarkRed
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeDarkGreen
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroTransferBlue
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah

@Composable
fun DashboardTransactionsSection(
    transactions: List<TransactionEntity>,
    selectedFilterTab: String,
    onFilterTabSelected: (String) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedDateFilter: String = "ALL",
    onDateFilterSelected: (String) -> Unit = {},
    categories: List<CategoryEntity> = emptyList(),
    selectedCategoryFilter: String? = null,
    onCategoryFilterSelected: (String?) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAnyFilterActive = searchQuery.isNotBlank() ||
            selectedCategoryFilter != null ||
            selectedDateFilter != "ALL" ||
            selectedFilterTab != "ALL"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Section Header ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
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
                text = "${transactions.size} TRANSAKSI",
                backgroundColor = Color.White
            )
        }

        // ── 2. Interactive Search Bar ─────────────────────────────────────
        NeobrutalSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )

        // ── 3. Filter Tabs (Semua / Pengeluaran / Pemasukan) ───────────────
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
                            .padding(vertical = 9.dp),
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
                                .height(36.dp)
                                .background(Color.Black)
                        )
                    }
                }
            }
        }

        // ── 4. Date Range Filters (Semua / Hari Ini / Bulan Ini) ───────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateOptions = listOf(
                Pair("ALL", "SEMUA WAKTU"),
                Pair("TODAY", "HARI INI"),
                Pair("THIS_MONTH", "BULAN INI")
            )
            dateOptions.forEach { (key, label) ->
                val isSelected = selectedDateFilter == key
                Box(
                    modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) RetroTransferBlue else Color.White, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .clickable { onDateFilterSelected(key) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        )
                    }
                }
            }
        }

        // ── 5. Category Chips Horizontal Scroll ───────────────────────────
        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "SEMUA KATEGORI" Chip
                val isAllCategoriesSelected = selectedCategoryFilter == null
                Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Box(
                        modifier = Modifier
                            .background(if (isAllCategoriesSelected) RetroYellow else Color.White, RectangleShape)
                            .border(1.5.dp, Color.Black, RectangleShape)
                            .clickable { onCategoryFilterSelected(null) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SEMUA KATEGORI",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = Color.Black
                            )
                        )
                    }
                }

                // Distinct Category Chips
                val categoryNames = categories.map { it.name }.distinct()
                categoryNames.forEach { catName ->
                    val isCatSelected = selectedCategoryFilter.equals(catName, ignoreCase = true)
                    Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 2.dp, y = 2.dp)
                                .background(Color.Black, RectangleShape)
                        )
                        Box(
                            modifier = Modifier
                                .background(if (isCatSelected) RetroYellow else Color(0xFFF8FAFC), RectangleShape)
                                .border(1.5.dp, Color.Black, RectangleShape)
                                .clickable {
                                    if (isCatSelected) {
                                        onCategoryFilterSelected(null)
                                    } else {
                                        onCategoryFilterSelected(catName)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = catName.uppercase(),
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

        // ── 6. Transactions List or Empty State ───────────────────────────
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
                        text = if (isAnyFilterActive) "TIDAK ADA TRANSAKSI DITEMUKAN" else "BELUM ADA TRANSAKSI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            color = Color.Black
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAnyFilterActive) {
                            "Tidak ada transaksi yang cocok dengan filter atau kata kunci pencarian."
                        } else {
                            "Tekan tombol (+) kuning di kanan bawah untuk mencatat transaksi baru."
                        },
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    )

                    if (isAnyFilterActive) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .background(RetroYellow, RectangleShape)
                                    .border(2.dp, Color.Black, RectangleShape)
                                    .clickable { onResetFilters() }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "RESET FILTER",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }
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
                        onEdit = { onEditTransaction(tx) },
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
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type.equals("INCOME", ignoreCase = true)
    val isTransfer = transaction.type.equals("TRANSFER", ignoreCase = true)

    val accentColor = when {
        isTransfer -> RetroTransferBlue
        isIncome -> RetroIncomeDarkGreen
        else -> RetroExpenseDarkRed
    }
    val amountPrefix = when {
        isTransfer -> "⇄ "
        isIncome -> "+"
        else -> "-"
    }
    val badgeBgColor = when {
        isTransfer -> RetroTransferBlue
        isIncome -> RetroIncomeGreen
        else -> RetroExpenseRed
    }
    val iconVector = when {
        isTransfer -> Icons.Default.SwapHoriz
        isIncome -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }

    NeobrutalCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
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
                        badgeBgColor,
                        RectangleShape
                    )
                    .border(2.dp, Color.Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
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
