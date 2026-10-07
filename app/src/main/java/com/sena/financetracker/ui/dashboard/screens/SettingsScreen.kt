package com.sena.financetracker.ui.dashboard.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.NeobrutalConfirmDialog
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Layar Pengaturan & Preferensi Neobrutal (SettingsScreen).
 *
 * Mengadopsi styling Strict Neobrutalism murni sesuai paritas web (Settings.jsx)
 * sebagai pusat navigasi preferensi, diagnostik basis data lokal, dan utilitas aplikasi.
 *
 * @param uiState State reaktif UI keuangan.
 * @param onNavigateBack Callback kembali ke layar sebelumnya.
 * @param onNavigateToCategories Callback navigasi ke layar Kelola Kategori (Screen.Categories).
 * @param onNavigateToNotifications Callback navigasi ke Pusat Notifikasi (Screen.Notifications).
 * @param onResetTransactions Callback eksekusi pembersihan seluruh riwayat transaksi setelah dikonfirmasi.
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
    modifier: Modifier = Modifier
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Back Neobrutal
                Box(
                    modifier = Modifier.clickable(onClick = onNavigateBack)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = 3.dp, y = 3.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "PREFERENSI & SISTEM",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = "PENGATURAN",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp,
                            color = Color.Black
                        )
                    )
                }
            }
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
            NeobrutalCard(
                backgroundColor = Color.White,
                borderWidth = 2.dp,
                shadowOffset = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (uiState.isHapticEnabled) RetroYellow else Color.LightGray, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Haptic Feedback",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RESPON TAKTIL / GETAR",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                        Text(
                            text = if (uiState.isHapticEnabled)
                                "Getaran taktil mekanik aktif pada tombol & dialog"
                            else
                                "Umpan balik getaran dinonaktifkan",
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    com.sena.financetracker.ui.components.NeobrutalSwitch(
                        checked = uiState.isHapticEnabled,
                        onCheckedChange = onToggleHaptic
                    )
                }
            }
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
            NeobrutalCard(
                backgroundColor = Color.White,
                borderWidth = 2.dp,
                shadowOffset = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEC4899), RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Tentang Aplikasi",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "TENTANG APLIKASI & DATABASE",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.Black
                                )
                            )
                            Text(
                                text = "Spesifikasi arsitektur & penyimpanan lokal",
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detail Grid Baris Info
                    InfoRowItem(label = "VERSI APLIKASI", value = "v1.0.0-neobrutal")
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowItem(label = "SQLITE ENGINE", value = "v4 (Reactive StateFlow)")
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowItem(label = "PENYIMPANAN", value = "SQLite / Room Local Storage")
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowItem(label = "TOTAL TRANSAKSI", value = "${uiState.transactions.size} Tercatat")
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowItem(label = "TOTAL REKENING", value = "${uiState.accounts.size} Akun Aktif")
                }
            }
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

        // Tombol Reset Database Transaksi
        item {
            NeobrutalCard(
                backgroundColor = Color(0xFFFEE2E2),
                borderWidth = 2.dp,
                shadowOffset = 4.dp,
                onClick = { showResetConfirmDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDC2626), RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Reset Database",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "RESET DATABASE TRANSAKSI",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFFDC2626)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hapus seluruh catatan riwayat transaksi dari basis data",
                                style = TextStyle(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    // Badge Aksi Bahaya
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDC2626), RectangleShape)
                            .border(1.5.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "RESET",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }

    // Modal Proteksi NeobrutalConfirmDialog
    if (showResetConfirmDialog) {
        NeobrutalConfirmDialog(
            title = "RESET DATABASE TRANSAKSI?",
            message = "Tindakan ini akan mengosongkan seluruh riwayat transaksi yang tersimpan di SQLite secara permanen. Data yang telah dihapus tidak dapat dipulihkan.",
            confirmButtonText = "RESET SEMUA",
            cancelButtonText = "BATALKAN",
            confirmButtonColor = Color(0xFFDC2626),
            onConfirm = {
                showResetConfirmDialog = false
                onResetTransactions()
            },
            onDismiss = {
                showResetConfirmDialog = false
            }
        )
    }
}

/**
 * Komponen kartu item navigasi Neobrutal dengan aksen warna tebal dan panah navigasi.
 */
@Composable
private fun SettingsNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    iconTintColor: Color = Color.Black,
    badgeTextColor: Color = Color.Black,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    NeobrutalCard(
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kotak Ikon Aksen
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(badgeColor, RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                        if (badgeCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDC2626), RectangleShape)
                                    .border(1.dp, Color.Black, RectangleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // Tombol Panah Neobrutal
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, RectangleShape)
                    .border(1.5.dp, Color.Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Buka",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Baris informasi label-value Neobrutal dengan kontras tinggi.
 */
@Composable
private fun InfoRowItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RectangleShape)
            .border(1.dp, Color.Black, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = Color.Black.copy(alpha = 0.7f)
            )
        )
        Text(
            text = value,
            style = TextStyle(
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = Color.Black
            )
        )
    }
}
