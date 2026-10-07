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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.NeobrutalCard

/**
 * Kartu Info Neobrutal: TENTANG APLIKASI & DATABASE
 */
@Composable
fun SettingsSystemInfoCard(
    totalTransactions: Int,
    totalAccounts: Int,
    modifier: Modifier = Modifier
) {
    NeobrutalCard(
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp,
        modifier = modifier
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
            InfoRowItem(label = "TOTAL TRANSAKSI", value = "$totalTransactions Tercatat")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRowItem(label = "TOTAL REKENING", value = "$totalAccounts Akun Aktif")
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
