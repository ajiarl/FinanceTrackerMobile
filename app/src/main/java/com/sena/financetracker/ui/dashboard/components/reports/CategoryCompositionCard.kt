package com.sena.financetracker.ui.dashboard.components.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.sena.financetracker.viewmodel.CategoryBreakdownItem
import com.sena.financetracker.util.formatRupiah

@Composable
fun CategoryCompositionCard(
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
fun CategoryBreakdownRow(item: CategoryBreakdownItem) {
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
