package com.sena.financetracker.ui.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.dashboard.components.DashboardTransactionsSection
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Tab 2 - Transaksi (TransactionsScreen):
 * - Header "RIWAYAT TRANSAKSI".
 * - NeobrutalSearchBar & filter chips (Tanggal, Tipe, Kategori).
 * - List transaksi lengkap yang terkelompok per tanggal (group by date) dengan aksi edit & konfirmasi hapus.
 */
@Composable
fun TransactionsScreen(
    uiState: FinanceUiState,
    onFilterTabSelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDateFilterSelected: (String) -> Unit,
    onCategoryFilterSelected: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "CATATAN ARUS KAS",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "RIWAYAT TRANSAKSI",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color.Black
                    )
                )
            }
        }

        item {
            DashboardTransactionsSection(
                transactions = uiState.filteredTransactions,
                selectedFilterTab = uiState.selectedFilterTab,
                onFilterTabSelected = onFilterTabSelected,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                selectedDateFilter = uiState.selectedDateFilter,
                onDateFilterSelected = onDateFilterSelected,
                categories = uiState.categories,
                selectedCategoryFilter = uiState.selectedCategoryFilter,
                onCategoryFilterSelected = onCategoryFilterSelected,
                onResetFilters = onResetFilters,
                onDeleteTransaction = onDeleteTransaction,
                onEditTransaction = onEditTransaction
            )
        }
    }
}
