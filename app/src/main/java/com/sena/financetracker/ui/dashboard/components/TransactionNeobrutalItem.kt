package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.util.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val RetroExpenseRed = Color(0xFFEF4444)
val RetroExpenseDarkRed = Color(0xFFDC2626)
val RetroIncomeGreen = Color(0xFF22C55E)
val RetroIncomeDarkGreen = Color(0xFF16A34A)
val RetroTransferBlue = Color(0xFF3B82F6)
val RetroDateHeaderBg = Color(0xFFF1F5F9)

/**
 * Kartu transaksi individual neobrutal dengan badge tipe, nominal berwarna, dan tombol edit/delete.
 */
@Composable
fun TransactionNeobrutalItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type.equals("INCOME", ignoreCase = true)
    val isTransfer = transaction.type.equals("TRANSFER", ignoreCase = true)

    val accentColor = when {
        isTransfer -> RetroTransferBlue
        isIncome -> RetroIncomeDarkGreen
        else -> RetroExpenseDarkRed
    }
    val amountPrefix = when {
        isTransfer -> "⇄ "
        isIncome -> "+"
        else -> "-"
    }
    val badgeBgColor = when {
        isTransfer -> RetroTransferBlue
        isIncome -> RetroIncomeGreen
        else -> RetroExpenseRed
    }
    val iconVector = when {
        isTransfer -> Icons.Default.SwapHoriz
        isIncome -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }

    NeobrutalCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon Badge Box (Square 38x38 with 2.dp black border)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        badgeBgColor,
                        RectangleShape
                    )
                    .border(2.dp, Color.Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Description, Category & Account Badges
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RectangleShape)
                            .border(1.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.category.uppercase(),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                    }

                    // Account Source Badge
                    if (transaction.accountName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2E8F0), RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.accountName.uppercase(),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                        }
                    }
                }
            }

            // Amount & Actions (Edit + Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$amountPrefix${formatRupiah(transaction.amount)}",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = accentColor
                        )
                    )
                    Text(
                        text = transaction.date,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    )
                }

                // Edit Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaksi",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Transaksi",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sub-header tanggal Neobrutal dengan badge tanggal dan garis horizontal.
 */
@Composable
fun NeobrutalDateHeader(
    dateStr: String,
    modifier: Modifier = Modifier
) {
    val formattedLabel = remember(dateStr) {
        formatNeobrutalDate(dateStr)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(RetroDateHeaderBg, RectangleShape)
                .border(1.5.dp, Color.Black, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = formattedLabel,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .background(Color.Black.copy(alpha = 0.25f))
        )
    }
}

/**
 * Helper pemformat tanggal neobrutal ("HARI INI • YYYY-MM-DD" atau "DD MMM YYYY").
 */
fun formatNeobrutalDate(dateStr: String): String {
    return try {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (dateStr == todayStr) {
            return "HARI INI • $dateStr"
        }

        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        if (parsed != null) {
            val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
            displayFormat.format(parsed).uppercase()
        } else {
            dateStr
        }
    } catch (e: Exception) {
        android.util.Log.e("FinanceTracker", "Gagal memformat tanggal transaksi: $dateStr", e)
        dateStr
    }
}
