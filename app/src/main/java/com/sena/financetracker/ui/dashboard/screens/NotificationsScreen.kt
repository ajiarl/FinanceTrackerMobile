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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.R
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.NeobrutalConfirmDialog
import com.sena.financetracker.viewmodel.FinanceUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Pusat Notifikasi & Overbudget Alert (NotificationsScreen).
 *
 * Mengadopsi prinsip Strict Neobrutalism murni:
 * - Border tebal 2.dp / 3.dp hitam pekat.
 * - Hard drop shadow kotak 2.dp / 4.dp tanpa blur.
 * - Kontras visual tegas dengan palet warna fungsional:
 *   * DANGER: Merah lembut (#FEE2E2 / Border #DC2626)
 *   * WARNING: Kuning pastel / Retro Yellow (#FEF9C3 / #FAFF00)
 *   * INFO: Biru pastel / Putih (#E0F2FE / #FFFFFF)
 *
 * @param uiState State reaktif UI keuangan yang membawa daftar notifikasi dan unread count.
 * @param onNavigateBack Callback kembali ke layar sebelumnya.
 * @param onMarkAsRead Callback menandai notifikasi telah dibaca berdasarkan ID.
 * @param onMarkAllAsRead Callback menandai seluruh notifikasi telah dibaca.
 * @param onClearAllNotifications Callback menghapus semua data notifikasi.
 * @param onDeleteNotification Callback menghapus 1 notifikasi berdasarkan ID.
 */
@Composable
fun NotificationsScreen(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onMarkAsRead: (Long) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAllNotifications: () -> Unit,
    onDeleteNotification: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Header
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

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "SISTEM PERINGATAN",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = stringResource(R.string.title_screen_notifications),
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp,
                            color = Color.Black
                        )
                    )
                }
            }
        }

        // Action Bar (Tandai Semua Dibaca & Hapus Semua)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tombol Tandai Semua Dibaca
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = uiState.unreadNotificationCount > 0) {
                            onMarkAllAsRead()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 3.dp, y = 3.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (uiState.unreadNotificationCount > 0) RetroYellow else Color(0xFFE5E7EB),
                                RectangleShape
                            )
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TANDAI SEMUA DIBACA",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                        )
                    }
                }

                // Tombol Hapus Semua
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = uiState.notifications.isNotEmpty()) {
                            showClearConfirmDialog = true
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 3.dp, y = 3.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (uiState.notifications.isNotEmpty()) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                                RectangleShape
                            )
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (uiState.notifications.isNotEmpty()) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HAPUS SEMUA",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (uiState.notifications.isNotEmpty()) Color.White else Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Empty State jika tidak ada notifikasi sama sekali
        if (uiState.notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(RetroYellow, RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "KOTAK MASUK BERSIH!",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Belum ada peringatan overbudget atau pesan sistem baru.",
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        } else {
            // List Kartu Notifikasi Neobrutal
            items(uiState.notifications, key = { it.id }) { notif ->
                NotificationNeobrutalCard(
                    notification = notif,
                    onCardClick = {
                        if (!notif.isRead) {
                            onMarkAsRead(notif.id)
                        }
                    },
                    onDeleteClick = {
                        onDeleteNotification(notif.id)
                    }
                )
            }
        }
    }

    // Modal Konfirmasi Hapus Semua Notifikasi
    if (showClearConfirmDialog) {
        NeobrutalConfirmDialog(
            title = "Hapus Semua Pesan?",
            message = "Tindakan ini akan mengosongkan seluruh riwayat pesan dan peringatan anggaran. Tindakan tidak dapat dibatalkan.",
            confirmButtonText = "HAPUS SEMUA",
            confirmButtonColor = Color(0xFFDC2626),
            onConfirm = {
                onClearAllNotifications()
                showClearConfirmDialog = false
            },
            onDismiss = {
                showClearConfirmDialog = false
            }
        )
    }
}

/**
 * Komponen kartu Neobrutal untuk merender sebuah entitas [NotificationEntity].
 */
@Composable
private fun NotificationNeobrutalCard(
    notification: NotificationEntity,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardBackground = when (notification.type.uppercase()) {
        "DANGER" -> Color(0xFFFEE2E2) // Merah pastel tegas
        "WARNING" -> Color(0xFFFEF9C3) // Kuning pastel tegas
        else -> Color.White // Biru / putih netral
    }

    val badgeColor = when (notification.type.uppercase()) {
        "DANGER" -> Color(0xFFDC2626)
        "WARNING" -> Color(0xFFEAB308)
        else -> Color(0xFF0284C7)
    }

    val typeIcon = when (notification.type.uppercase()) {
        "DANGER" -> Icons.Default.NotificationsActive
        "WARNING" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    val dateFormatted = remember(notification.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")).format(Date(notification.createdAt))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        // Hard drop shadow kotak 2.dp
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(Color.Black, RectangleShape)
        )

        // Card container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground, RectangleShape)
                .border(2.dp, Color.Black, RectangleShape)
                .padding(14.dp)
        ) {
            // Header Row: Type Badge, Unread Indicator, Timestamp, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge Type
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RectangleShape)
                            .border(1.5.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = notification.type.uppercase(),
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    // Indikator Belum Dibaca (Bulatan Merah Tebal)
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFDC2626), CircleShape)
                                .border(1.dp, Color.Black, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BARU",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color(0xFFDC2626)
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormatted,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Hapus per Item
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, RectangleShape)
                            .border(1.dp, Color.Black, RectangleShape)
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Judul Notifikasi
            Text(
                text = notification.title.uppercase(),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Pesan Notifikasi
            Text(
                text = notification.message,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color.Black.copy(alpha = 0.85f)
                )
            )
        }
    }
}
