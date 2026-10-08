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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.R
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.dashboard.components.DashboardBudgetsSection
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Tab 3 - Anggaran (BudgetsScreen):
 * - Header "ANGGARAN BULAN INI", overall alert badge (AMAN/WASPADA/KRITIS), dan tombol "+ ANGGARAN".
 * - List kartu anggaran bulanan dengan Neobrutal progress bar dan aksi hapus berproteksi dialog.
 */
@Composable
fun BudgetsScreen(
    uiState: FinanceUiState,
    onAddBudgetClick: () -> Unit,
    onDeleteBudgetClick: (Long) -> Unit,
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
                    text = "KONTROL PENGELUARAN",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = stringResource(R.string.title_screen_budgets),
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
            DashboardBudgetsSection(
                budgets = uiState.budgets,
                onAddBudgetClick = onAddBudgetClick,
                onDeleteBudgetClick = onDeleteBudgetClick
            )
        }
    }
}
