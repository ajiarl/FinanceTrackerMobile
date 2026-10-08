package com.sena.financetracker.ui.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sena.financetracker.ui.dashboard.components.AiInsightsPanel
import com.sena.financetracker.ui.dashboard.components.reports.CashflowChartCard
import com.sena.financetracker.ui.dashboard.components.reports.CategoryCompositionCard
import com.sena.financetracker.ui.dashboard.components.reports.ReportsHeaderFilterSection
import com.sena.financetracker.ui.dashboard.components.reports.SavingRateSummaryCard
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Tab 5 - Laporan & Grafik Analisis Keuangan (ReportsScreen):
 * Layout orchestrator untuk ringkasan rasio tabungan, chart arus kas, dan komposisi pengeluaran.
 */
@Composable
fun ReportsScreen(
    uiState: FinanceUiState,
    onPresetSelected: (String) -> Unit,
    onRefreshAiInsight: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reports = uiState.reportsAnalytics
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Period Preset Filter + Export Button
        item {
            ReportsHeaderFilterSection(
                uiState = uiState,
                selectedPreset = reports.periodPreset,
                onPresetSelected = onPresetSelected,
                context = context
            )
        }

        // Panel AI "Pak Hemat · AI Insight"
        item {
            AiInsightsPanel(
                insightText = uiState.aiInsightText,
                isLoading = uiState.isAiInsightLoading,
                errorMessage = uiState.aiInsightError,
                onRefresh = onRefreshAiInsight
            )
        }

        // Kartu Ringkasan Rasio Tabungan (Saving Rate %)
        item {
            SavingRateSummaryCard(
                totalIncome = reports.totalIncome,
                totalExpense = reports.totalExpense,
                netSavings = reports.netSavings,
                savingRate = reports.savingRate,
                savingStatus = reports.savingStatus
            )
        }

        // Kartu Diagram Batang Arus Kas (Cashflow Bar Chart)
        item {
            CashflowChartCard(
                cashflowBars = reports.cashflowBars
            )
        }

        // Kartu Komposisi Pengeluaran per Kategori
        item {
            CategoryCompositionCard(
                categoryBreakdown = reports.categoryBreakdown,
                totalExpense = reports.totalExpense
            )
        }
    }
}
