package com.sena.financetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Representasi rute destinasi tab pada Bottom Navigation Bar aplikasi Finance Tracker.
 *
 * @property route Kunci navigasi unik string untuk NavHost.
 * @property title Label teks bahasa Indonesia yang dirender pada UI tab.
 * @property icon Ikon Material untuk tab bar.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Beranda",
        icon = Icons.Default.Home
    )

    data object Transactions : Screen(
        route = "transactions",
        title = "Transaksi",
        icon = Icons.AutoMirrored.Filled.ReceiptLong
    )

    data object Budgets : Screen(
        route = "budgets",
        title = "Anggaran",
        icon = Icons.Default.PieChart
    )

    data object Accounts : Screen(
        route = "accounts",
        title = "Akun",
        icon = Icons.Default.AccountBalanceWallet
    )

    companion object {
        /**
         * Seluruh daftar layar tab bottom navigation utama.
         */
        val bottomNavItems: List<Screen> by lazy {
            listOf(
                Dashboard,
                Transactions,
                Budgets,
                Accounts
            )
        }
    }
}
