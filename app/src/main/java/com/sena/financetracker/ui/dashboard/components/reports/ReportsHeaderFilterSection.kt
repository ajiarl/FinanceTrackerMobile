package com.sena.financetracker.ui.dashboard.components.reports

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.CsvExporter
import com.sena.financetracker.viewmodel.FinanceUiState

@Composable
fun ReportsHeaderFilterSection(
    uiState: FinanceUiState,
    selectedPreset: String,
    onPresetSelected: (String) -> Unit,
    context: Context,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "ANALISIS ARUS KAS",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "LAPORAN KEUANGAN",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color.Black
                    )
                )
            }

            // Tombol Neobrutal "BAGIKAN CSV"
            Box(
                modifier = Modifier.clickable {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    if (uiState.transactions.isEmpty()) {
                        Toast.makeText(context, "Belum ada transaksi untuk diekspor!", Toast.LENGTH_SHORT).show()
                    } else {
                        val csvData = CsvExporter.generateTransactionsCsv(
                            transactions = uiState.transactions,
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
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Bagikan CSV",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "BAGIKAN CSV",
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

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                "THIS_MONTH" to "Bulan Ini",
                "LAST_MONTH" to "Bulan Lalu",
                "LAST_3_MONTHS" to "3 Bulan",
                "ALL_TIME" to "Semua"
            )
            presets.forEach { (key, label) ->
                val isSelected = selectedPreset == key
                NeobrutalPresetChip(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onPresetSelected(key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
