package com.sena.financetracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sena.financetracker.viewmodel.FinanceViewModel
import com.sena.financetracker.ui.dashboard.FinanceDashboardScreen as ModularDashboardScreen

/**
 * Backward-compatibility delegator pointing to modular [ModularDashboardScreen].
 */
@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    ModularDashboardScreen(viewModel = viewModel, modifier = modifier)
}
