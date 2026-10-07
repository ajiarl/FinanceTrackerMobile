package com.sena.financetracker.ui.dashboard.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.AiInsightsPanel
import com.sena.financetracker.util.CsvExporter
import com.sena.financetracker.util.formatRupiah
import com.sena.financetracker.viewmodel.CashflowBarItem
import com.sena.financetracker.viewmodel.CategoryBreakdownItem
import com.sena.financetracker.viewmodel.FinanceUiState
import kotlin.math.max

/**
 * Tab 5 - Laporan & Grafik Analisis Keuangan (ReportsScreen):
 * - Header "LAPORAN KEUANGAN" + Tombol Aksi Neobrutal "BAGIKAN CSV".
 * - Filter Preset Chip Neobrutal (Bulan Ini, Bulan Lalu, 3 Bulan, Semua).
 * - Kartu Neobrutal Ringkasan Rasio Tabungan (Saving Rate % badge: HEMAT / NORMAL / BOROS).
 * - Kartu Diagram Batang Arus Kas (Canvas Neobrutal: Pemasukan Hijau Emerald vs Pengeluaran Merah).
 * - Kartu Komposisi Pengeluaran per Kategori dengan Neobrutal progress strip bar.
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
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
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
                            if (uiState.transactions.isEmpty()) {
                                Toast.makeText(context, "Belum ada transaksi untuk dibagikan!", Toast.LENGTH_SHORT).show()
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
                        val isSelected = reports.periodPreset == key
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

@Composable
private fun NeobrutalPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 2.dp)
                    .background(Color.Black, RectangleShape)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) RetroYellow else Color.White, RectangleShape)
                .border(2.dp, Color.Black, RectangleShape)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.uppercase(),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.Black
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SavingRateSummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    netSavings: Double,
    savingRate: Int,
    savingStatus: String
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, RectangleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RectangleShape)
                .border(3.dp, Color.Black, RectangleShape)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RASIO TABUNGAN (SAVING RATE)",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF64748B)
                    )
                )

                val badgeBg = when (savingStatus) {
                    "HEMAT" -> RetroIncomeGreen
                    "NORMAL" -> RetroYellow
                    else -> Color(0xFFDC2626)
                }
                val badgeText = when (savingStatus) {
                    "HEMAT" -> Color.Black
                    "NORMAL" -> Color.Black
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = savingStatus,
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = badgeText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$savingRate%",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-1).sp,
                        color = Color.Black
                    )
                )
                Text(
                    text = if (netSavings >= 0) "Surplus Rp ${formatRupiah(netSavings)}" else "Defisit Rp ${formatRupiah(netSavings)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (netSavings >= 0) RetroIncomeGreen else Color(0xFFDC2626)
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-box perbandingan Pemasukan vs Pengeluaran
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Income
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF0FDF4), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "PEMASUKAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color(0xFF15803D)
                            )
                        )
                        Text(
                            text = "Rp " + formatRupiah(totalIncome),
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        )
                    }
                }

                // Expense
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFEF2F2), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "PENGELUARAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color(0xFFDC2626)
                            )
                        )
                        Text(
                            text = "Rp " + formatRupiah(totalExpense),
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowChartCard(
    cashflowBars: List<CashflowBarItem>
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, RectangleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RectangleShape)
                .border(3.dp, Color.Black, RectangleShape)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARUS KAS MASUK VS KELUAR",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                )

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(RetroIncomeGreen, RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                        )
                        Text(
                            text = " Masuk",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFDC2626), RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                        )
                        Text(
                            text = " Keluar",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cashflowBars.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF8FAFC), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Data grafik belum tersedia.",
                        style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 11.sp)
                    )
                }
            } else {
                val maxVal = cashflowBars.maxOfOrNull { max(it.income, it.expense) } ?: 1.0
                val safeMax = if (maxVal > 0) maxVal else 1.0

                // Canvas Diagram Batang Neobrutalisme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFFAFAFA), RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height - 24.dp.toPx() // sisakan tempat untuk label bawah
                        val barGroupCount = cashflowBars.size
                        val groupWidth = canvasWidth / barGroupCount
                        val barWidth = 14.dp.toPx()
                        val barSpacing = 4.dp.toPx()

                        // Garis dasar horizontal hitam
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, canvasHeight),
                            end = Offset(canvasWidth, canvasHeight),
                            strokeWidth = 2.dp.toPx()
                        )

                        cashflowBars.forEachIndexed { index, item ->
                            val groupCenterX = (index * groupWidth) + (groupWidth / 2)
                            val incomeBarX = groupCenterX - barWidth - (barSpacing / 2)
                            val expenseBarX = groupCenterX + (barSpacing / 2)

                            val incomeHeight = ((item.income / safeMax) * (canvasHeight - 10.dp.toPx())).toFloat().coerceAtLeast(0f)
                            val expenseHeight = ((item.expense / safeMax) * (canvasHeight - 10.dp.toPx())).toFloat().coerceAtLeast(0f)

                            // 1. Gambar Bar Income (Hijau)
                            if (incomeHeight > 0f) {
                                val topY = canvasHeight - incomeHeight
                                // Shadow bar 2.dp
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(incomeBarX + 2.dp.toPx(), topY + 2.dp.toPx()),
                                    size = Size(barWidth, incomeHeight)
                                )
                                // Isi bar
                                drawRect(
                                    color = Color(0xFF00E676),
                                    topLeft = Offset(incomeBarX, topY),
                                    size = Size(barWidth, incomeHeight)
                                )
                                // Border bar
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(incomeBarX, topY),
                                    size = Size(barWidth, incomeHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // 2. Gambar Bar Expense (Merah)
                            if (expenseHeight > 0f) {
                                val topY = canvasHeight - expenseHeight
                                // Shadow bar 2.dp
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(expenseBarX + 2.dp.toPx(), topY + 2.dp.toPx()),
                                    size = Size(barWidth, expenseHeight)
                                )
                                // Isi bar
                                drawRect(
                                    color = Color(0xFFDC2626),
                                    topLeft = Offset(expenseBarX, topY),
                                    size = Size(barWidth, expenseHeight)
                                )
                                // Border bar
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(expenseBarX, topY),
                                    size = Size(barWidth, expenseHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    // Label teks bulan di bawah diagram
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        cashflowBars.forEach { item ->
                            Text(
                                text = item.label,
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCompositionCard(
    categoryBreakdown: List<CategoryBreakdownItem>,
    totalExpense: Double
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, RectangleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RectangleShape)
                .border(3.dp, Color.Black, RectangleShape)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KOMPOSISI PENGELUARAN",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                )
                Text(
                    text = "${categoryBreakdown.size} KATEGORI",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi pengeluaran pada periode ini.",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categoryBreakdown.forEach { item ->
                        CategoryBreakdownRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(item: CategoryBreakdownItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.category,
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "Rp ${formatRupiah(item.totalAmount)} (${item.percentage}%)",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Neobrutal Progress Strip Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFFF1F5F9), RectangleShape)
                .border(1.5.dp, Color.Black, RectangleShape)
        ) {
            val fillFraction = (item.percentage / 100f).coerceIn(0f, 1f)
            if (fillFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .fillMaxHeight()
                        .background(Color.Black, RectangleShape)
                )
            }
        }
    }
}
