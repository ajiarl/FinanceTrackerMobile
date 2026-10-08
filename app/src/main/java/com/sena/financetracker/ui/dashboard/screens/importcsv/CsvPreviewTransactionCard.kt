package com.sena.financetracker.ui.dashboard.screens.importcsv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.CsvImporter
import java.text.NumberFormat
import java.util.Locale

/**
 * Kartu baris pratinjau transaksi CSV bergaya Neobrutalisme.
 */
@Composable
fun CsvPreviewTransactionCard(
    index: Int,
    item: CsvImporter.ParsedTransaction,
    accountName: String,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")) }
    val formattedAmount = rupiahFormatter.format(item.amount)

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(Color.Black, RectangleShape)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RectangleShape)
                .border(2.dp, Color.Black, RectangleShape)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Badge Status Valid / Invalid
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(if (item.isValid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isValid) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Valid",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Invalid",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#$index ${item.title}",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .background(if (item.type == "INCOME") RetroIncomeGreen else RetroYellow, RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.type,
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }

                    Text(
                        text = "${item.date} • ${item.category} • $accountName",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280)
                        )
                    )

                    if (!item.isValid && item.errorMessage != null) {
                        Text(
                            text = "Peringatan: ${item.errorMessage}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color(0xFFDC2626)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Rp $formattedAmount",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = if (item.type == "INCOME") Color(0xFF16A34A) else Color(0xFFDC2626)
                )
            )
        }
    }
}
