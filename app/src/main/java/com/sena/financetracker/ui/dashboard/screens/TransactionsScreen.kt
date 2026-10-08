package com.sena.financetracker.ui.dashboard.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.DashboardTransactionsSection
import com.sena.financetracker.util.CsvExporter
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Tab 2 - Transaksi (TransactionsScreen):
 * - Header "RIWAYAT TRANSAKSI" + Tombol Aksi Neobrutal "EKSPOR CSV".
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
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
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

                // Tombol Neobrutal "EKSPOR CSV"
                Box(
                    modifier = Modifier.clickable {
                        com.sena.financetracker.ui.components.NeobrutalHapticEngine.heavyClick(context, uiState.isHapticEnabled)
                        if (uiState.transactions.isEmpty()) {
                            Toast.makeText(context, "Belum ada transaksi untuk diekspor!", Toast.LENGTH_SHORT).show()
                        } else {
                            val csvData = CsvExporter.generateTransactionsCsv(
                                transactions = uiState.filteredTransactions.ifEmpty { uiState.transactions },
                                accounts = uiState.accounts,
                                categories = uiState.categories
                            )
                            CsvExporter.exportAndShareCsv(context, csvData)
                            Toast.makeText(context, "Membuka Share Sheet CSV...", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Row(
                        modifier = Modifier
                            .background(RetroYellow, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor CSV",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "EKSPOR CSV",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
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
                onEditTransaction = onEditTransaction,
                hasMoreTransactions = uiState.hasMoreTransactions,
                onLoadMore = onLoadMore
            )
        }
    }
}
