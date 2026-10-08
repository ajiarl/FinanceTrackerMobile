package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.R
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.RetroYellow

/**
 * Container modular riwayat transaksi dashboard dengan filter, date grouping, pagination, dan dialog aksi.
 */
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
    hasMoreTransactions: Boolean = false,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAnyFilterActive = searchQuery.isNotBlank() ||
            selectedFilterTab != "ALL" ||
            selectedDateFilter != "ALL" ||
            selectedCategoryFilter != null

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
        // Filter Tabs: SEMUA, KELUAR, MASUK
        TransactionFilterTabs(
            selectedTab = selectedFilterTab,
            onTabSelected = onFilterTabSelected
        )

        // Search Bar Neobrutalisme
        NeobrutalSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )

        // Date Range Filters: SEMUA WAKTU, HARI INI, BULAN INI
        TransactionDateFilters(
            selectedFilter = selectedDateFilter,
            onFilterSelected = onDateFilterSelected
        )

        // Horizontal Category Chips Scroll
        TransactionCategoryChips(
            categories = categories,
            selectedCategory = selectedCategoryFilter,
            onCategorySelected = onCategoryFilterSelected
        )

        // Transactions List or Empty State
        if (transactions.isEmpty()) {
            EmptyTransactionsCard(
                isAnyFilterActive = isAnyFilterActive,
                onResetFilters = onResetFilters
            )
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
                        NeobrutalDateHeader(dateStr = dateKey)

                        itemsInDate.forEach { tx ->
                            TransactionNeobrutalItem(
                                transaction = tx,
                                onEdit = { onEditTransaction(tx) },
                                onDelete = { onDeleteTransaction(tx) }
                            )
                        }
                    }
                }

                if (hasMoreTransactions) {
                    LoadMorePaginationButton(onLoadMore = onLoadMore)
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard(
    isAnyFilterActive: Boolean,
    onResetFilters: () -> Unit
) {
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
                style = TextStyle(fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
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
}

@Composable
private fun LoadMorePaginationButton(
    onLoadMore: () -> Unit
) {
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(Color.Black, RectangleShape)
            )
            Box(
                modifier = Modifier
                    .background(RetroYellow, RectangleShape)
                    .border(2.dp, Color.Black, RectangleShape)
                    .clickable { onLoadMore() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.action_load_more),
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
