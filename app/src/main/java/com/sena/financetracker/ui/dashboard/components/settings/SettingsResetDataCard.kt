package com.sena.financetracker.ui.dashboard.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.dashboard.components.NeobrutalConfirmDialog

@Composable
fun SettingsResetDataCard(
    onResetTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    NeobrutalCard(
        backgroundColor = Color(0xFFFEE2E2),
        borderWidth = 2.dp,
        shadowOffset = 4.dp,
        onClick = { showResetConfirmDialog = true },
        modifier = modifier
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
