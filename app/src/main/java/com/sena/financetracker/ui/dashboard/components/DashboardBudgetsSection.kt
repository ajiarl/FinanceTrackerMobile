package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.BudgetProgressItem
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah

private val RetroSafeGreen = Color(0xFF00E676)
private val RetroWarningYellow = Color(0xFFFAFF00)
private val RetroCriticalRed = Color(0xFFDC2626)

@Composable
fun DashboardBudgetsSection(
    budgets: List<BudgetProgressItem>,
    onAddBudgetClick: () -> Unit = {},
    onDeleteBudgetClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Overall status level
    val worstStatus = when {
        budgets.any { it.statusLevel == "CRITICAL" } -> "CRITICAL"
        budgets.any { it.statusLevel == "WARNING" } -> "WARNING"
        budgets.isNotEmpty() -> "SAFE"
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .padding(end = 6.dp, bottom = 6.dp)
    ) {
        // Hard drop shadow 6.dp kotak (RectangleShape tanpa blur)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, RectangleShape)
        )

        // Main Container Neobrutal
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RectangleShape)
                .border(3.dp, Color.Black, RectangleShape)
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ANGGARAN BULAN INI",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            color = Color.Black
                        )
                    )

                    if (worstStatus != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val (statusBadgeBg, statusBadgeText, statusTextColor) = when (worstStatus) {
                            "CRITICAL" -> Triple(RetroCriticalRed, "KRITIS", Color.White)
                            "WARNING" -> Triple(RetroWarningYellow, "WASPADA", Color.Black)
                            else -> Triple(RetroSafeGreen, "AMAN", Color.Black)
                        }

                        Box(
                            modifier = Modifier
                                .background(statusBadgeBg, RectangleShape)
                                .border(1.5.dp, Color.Black, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusBadgeText,
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    color = statusTextColor
                                )
                            )
                        }
                    }
                }

                // Compact button + ANGGARAN
                Box(
                    modifier = Modifier
                        .background(RetroYellow, RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .clickable { onAddBudgetClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Anggaran",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "ANGGARAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: List or Empty Banner
            if (budgets.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BELUM ADA ANGGARAN BULAN INI",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tetapkan batas pengeluaran kategori untuk mengontrol keuangan.",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .background(RetroYellow, RectangleShape)
                                .border(1.5.dp, Color.Black, RectangleShape)
                                .clickable { onAddBudgetClick() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ BUAT ANGGARAN PERTAMA",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            } else {
                // Budgets List
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    budgets.forEach { item ->
                        BudgetItemRow(
                            item = item,
                            onDeleteClick = { onDeleteBudgetClick(item.budget.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetItemRow(
    item: BudgetProgressItem,
    onDeleteClick: () -> Unit
) {
    val progressColor = when (item.statusLevel) {
        "CRITICAL" -> RetroCriticalRed
        "WARNING" -> RetroWarningYellow
        else -> RetroSafeGreen
    }

    val progressFraction = (item.percentage / 100f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RectangleShape)
            .border(2.dp, Color.Black, RectangleShape)
            .padding(10.dp)
    ) {
        Column {
            // Header Row: Category/Name + Percentage Badge + Delete Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.budget.name,
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = item.budget.category,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Percentage Badge
                    Box(
                        modifier = Modifier
                            .background(progressColor, RectangleShape)
                            .border(1.5.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.percentage}%",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (item.statusLevel == "CRITICAL") Color.White else Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Delete Action
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White, RectangleShape)
                            .border(1.dp, Color.Black, RectangleShape)
                            .clickable { onDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Anggaran",
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Neobrutal Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Color(0xFFE2E8F0), RectangleShape)
                    .border(2.dp, Color.Black, RectangleShape)
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .fillMaxHeight()
                            .background(progressColor, RectangleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Nominal details: Terpakai vs Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terpakai: ${formatRupiah(item.spentAmount)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (item.isOver) RetroCriticalRed else Color.Black
                    )
                )

                Text(
                    text = "Limit: ${formatRupiah(item.budget.limitAmount)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = Color.Black
                    )
                )
            }
        }
    }
}
