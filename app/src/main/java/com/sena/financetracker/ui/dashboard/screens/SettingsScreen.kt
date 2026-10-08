package com.sena.financetracker.ui.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.dashboard.components.settings.GroqApiKeySettingsCard
import com.sena.financetracker.ui.dashboard.components.settings.HapticSettingsCard
import com.sena.financetracker.ui.dashboard.components.settings.SettingsNavigationCard
import com.sena.financetracker.ui.dashboard.components.settings.SettingsResetDataCard
import com.sena.financetracker.ui.dashboard.components.settings.SettingsSystemInfoCard
import com.sena.financetracker.ui.dashboard.components.settings.SettingsTopBarHeader
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Layar Pengaturan & Preferensi Neobrutal (SettingsScreen).
 * Menyusun sub-komponen kartu navigasi, preferensi haptic, info sistem, dan reset database.
 */
@Composable
fun SettingsScreen(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToImport: () -> Unit,
    onResetTransactions: () -> Unit,
    onToggleHaptic: (Boolean) -> Unit = {},
    onSaveGroqApiKey: (String) -> Unit = {},
    onClearGroqApiKey: () -> Unit = {},
    hasApiKey: Boolean = uiState.hasApiKey,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar Navigation Header
        item {
            SettingsTopBarHeader(onNavigateBack = onNavigateBack)
        }

        // Section: Menu Navigasi Neobrutal
        item {
            Text(
                text = "NAVIGASI & PREFERENSI",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Menu 1: KELOLA KATEGORI (Retro Yellow #FAFF00)
        item {
            SettingsNavigationCard(
                title = "KELOLA KATEGORI",
                subtitle = "Atur kategori pengeluaran dan pemasukan kustom",
                icon = Icons.Default.Category,
                badgeColor = RetroYellow,
                onClick = onNavigateToCategories
            )
        }

        // Menu 2: PUSAT NOTIFIKASI (Electric Blue #007AFF)
        item {
            SettingsNavigationCard(
                title = "PUSAT NOTIFIKASI",
                subtitle = "Peringatan overbudget & histori pemberitahuan sistem",
                icon = Icons.Default.Notifications,
                badgeColor = Color(0xFF007AFF),
                iconTintColor = Color.White,
                badgeTextColor = Color.White,
                badgeCount = uiState.unreadNotificationCount,
                onClick = onNavigateToNotifications
            )
        }

        // Menu 3: IMPOR TRANSAKSI (CSV) (Emerald Green #00E676)
        item {
            SettingsNavigationCard(
                title = "IMPOR TRANSAKSI (CSV)",
                subtitle = "Unggah & konversi berkas CSV riwayat transaksi ke database",
                icon = Icons.Default.FileUpload,
                badgeColor = Color(0xFF00E676),
                iconTintColor = Color.Black,
                onClick = onNavigateToImport
            )
        }

        // Section: Sensasi Taktil & Haptic
        item {
            Text(
                text = "RESPON TAKTIL & INTERAKSI",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Kartu Neobrutal: RESPON TAKTIL / GETAR (Vibration / Haptic Switch)
        item {
            HapticSettingsCard(
                isHapticEnabled = uiState.isHapticEnabled,
                onToggleHaptic = onToggleHaptic
            )
        }

        // Section: Integrasi AI & Pak Hemat
        item {
            Text(
                text = "KECERDASAN BUATAN & ANALITIK",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Kartu Neobrutal: Konfigurasi Groq API Key
        item {
            GroqApiKeySettingsCard(
                hasApiKey = hasApiKey,
                onSaveApiKey = onSaveGroqApiKey,
                onClearApiKey = onClearGroqApiKey
            )
        }

        // Section: Informasi Sistem & Basis Data
        item {
            Text(
                text = "INFORMASI SISTEM & DATABASE",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Kartu Info Neobrutal: TENTANG APLIKASI & DATABASE
        item {
            SettingsSystemInfoCard(
                totalTransactions = uiState.transactions.size,
                totalAccounts = uiState.accounts.size
            )
        }

        // Section: Tindakan Berisiko / Reset Data
        item {
            Text(
                text = "ZONA TINDAKAN BERISIKO",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFDC2626)
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Tombol Reset Database Transaksi & Dialog
        item {
            SettingsResetDataCard(
                onResetTransactions = onResetTransactions
            )
        }
    }
}
