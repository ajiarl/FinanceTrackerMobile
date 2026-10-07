package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RetroExpenseRed = Color(0xFFEF4444)
private val RetroExpenseDarkRed = Color(0xFFDC2626)
private val RetroIncomeGreen = Color(0xFF22C55E)
private val RetroIncomeDarkGreen = Color(0xFF16A34A)
private val RetroTransferBlue = Color(0xFF3B82F6)
private val RetroDateHeaderBg = Color(0xFFF1F5F9)

@Composable
fun DashboardTransactionsSection(
    transactions: List<TransactionEntity>,
    selectedFilterTab: String = "ALL",
    onFilterTabSelected: (String) -> Unit = {},
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
            selectedFilterTab != "ALL" ||
            selectedDateFilter != "ALL" ||
            selectedCategoryFilter != null

    // Grouping transactions by date ("YYYY-MM-DD") sorted descending
    val groupedTransactions = remember(transactions) {
        transactions
            .groupBy { it.date.take(10) }
            .toList()
            .sortedByDescending { it.first }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RIWAYAT TRANSAKSI",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )
            )

            // Filter Tabs: SEMUA, KELUAR, MASUK
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "ALL" to "SEMUA",
                    "EXPENSE" to "KELUAR",
                    "INCOME" to "MASUK"
                ).forEach { (typeKey, label) ->
                    val isSelected = selectedFilterTab.equals(typeKey, ignoreCase = true)
                    Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) RetroYellow else Color.White,
                                    RectangleShape
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = Color.Black,
                                    shape = RectangleShape
                                )
                                .clickable { onFilterTabSelected(typeKey) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        // Search Bar Neobrutalisme
        NeobrutalSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )

        // Date Range Filters: SEMUA WAKTU, HARI INI, BULAN INI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "ALL" to "SEMUA WAKTU",
                "TODAY" to "HARI INI",
                "THIS_MONTH" to "BULAN INI"
            ).forEach { (key, label) ->
                val isSelected = selectedDateFilter.equals(key, ignoreCase = true)
                Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 2.dp, y = 2.dp)
                                .background(Color.Black, RectangleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) RetroTransferBlue else Color.White,
                                RectangleShape
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.5.dp,
                                color = Color.Black,
                                shape = RectangleShape
                            )
                            .clickable { onDateFilterSelected(key) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Horizontal Category Chips Scroll
        val categoryScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoryScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Semua Kategori" Chip
            val isAllCategoriesSelected = selectedCategoryFilter == null
            Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                if (isAllCategoriesSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(Color.Black, RectangleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (isAllCategoriesSelected) RetroYellow else Color.White,
                            RectangleShape
                        )
                        .border(
                            width = if (isAllCategoriesSelected) 2.dp else 1.5.dp,
                            color = Color.Black,
                            shape = RectangleShape
                        )
                        .clickable { onCategoryFilterSelected(null) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SEMUA KATEGORI",
                        style = TextStyle(
                            fontWeight = if (isAllCategoriesSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    )
                }
            }

            // Category Items
            categories.forEach { cat ->
                val isSelected = selectedCategoryFilter?.equals(cat.name, ignoreCase = true) == true
                Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 2.dp, y = 2.dp)
                                .background(Color.Black, RectangleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) RetroYellow else Color.White,
                                RectangleShape
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.5.dp,
                                color = Color.Black,
                                shape = RectangleShape
                            )
                            .clickable {
                                if (isSelected) onCategoryFilterSelected(null)
                                else onCategoryFilterSelected(cat.name)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.name.uppercase(),
                            style = TextStyle(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Transactions List or Empty State
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedTransactions.forEach { (dateKey, itemsInDate) ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sub-header tanggal Neobrutal
                        NeobrutalDateHeader(dateStr = dateKey)

                        // Transaksi pada tanggal ini
                        itemsInDate.forEach { tx ->
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
    }
}

/**
 * Sub-header tanggal Neobrutal kecil dengan format rapi (matching web Transactions.jsx)
 */
@Composable
private fun NeobrutalDateHeader(
    dateStr: String,
    modifier: Modifier = Modifier
) {
    val formattedLabel = remember(dateStr) {
        formatNeobrutalDate(dateStr)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(RetroDateHeaderBg, RectangleShape)
                .border(1.5.dp, Color.Black, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = formattedLabel,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .background(Color.Black.copy(alpha = 0.25f))
        )
    }
}

private fun formatNeobrutalDate(dateStr: String): String {
    return try {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (dateStr == todayStr) {
            return "HARI INI • $dateStr"
        }

        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        if (parsed != null) {
            val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
            displayFormat.format(parsed).uppercase()
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RectangleShape)
                            .border(1.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.category.uppercase(),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                    }

                    // Account Source Badge
                    if (transaction.accountName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2E8F0), RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.accountName.uppercase(),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // Amount & Actions (Edit + Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$amountPrefix${formatRupiah(transaction.amount)}",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = accentColor
                        )
                    )
                    Text(
                        text = transaction.date,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    )
                }

                // Edit Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaksi",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Transaksi",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
