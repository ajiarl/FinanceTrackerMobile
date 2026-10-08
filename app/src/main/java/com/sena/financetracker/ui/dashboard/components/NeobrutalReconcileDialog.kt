package com.sena.financetracker.ui.dashboard.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.ui.components.NeobrutalBadge
import com.sena.financetracker.ui.components.NeobrutalButton
import com.sena.financetracker.ui.components.NeobrutalInputField
import com.sena.financetracker.ui.components.RetroExpenseDarkRed
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeDarkGreen
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.CurrencyParser
import com.sena.financetracker.util.formatRupiah
import kotlin.math.abs

@Composable
fun NeobrutalReconcileDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onReconcile: (account: AccountEntity, actualBalance: Double) -> Unit
) {
    var actualBalanceText by remember {
        mutableStateOf(
            if (account.balance % 1.0 == 0.0) account.balance.toLong().toString() else account.balance.toString()
        )
    }

    val actualBalance = CurrencyParser.parseCurrencyInput(actualBalanceText)
    val diff = actualBalance - account.balance
    val isValid = actualBalanceText.isNotBlank() && actualBalance >= 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .imePadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Hard drop shadow kotak hitam 6.dp
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(Color.Black, RectangleShape)
            )

            // Dialog body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RectangleShape)
                    .border(3.dp, Color.Black, RectangleShape)
            ) {
                // Header Bar (Solid Black Bar dengan ikon Balance)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(RetroYellow, RectangleShape)
                                .border(1.5.dp, Color.White, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Balance,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "REKONSILIASI SALDO",
                            style = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(2.dp, Color.White, RectangleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Scrollable Form content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Kartu Info Akun & Saldo Sistem Saat Ini
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = account.name.uppercase(),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                )
                                NeobrutalBadge(
                                    text = account.type.uppercase(),
                                    backgroundColor = RetroYellow
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "SALDO SISTEM TERCATAT",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                            )
                            Text(
                                text = formatRupiah(account.balance),
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.Black,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }

                    // 2. Input Saldo Fisik Riil Sekarang
                    Column {
                        NeobrutalInputField(
                            value = actualBalanceText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                                    actualBalanceText = input
                                }
                            },
                            label = "Saldo Fisik Riil Sekarang (IDR)",
                            placeholder = "0",
                            prefix = "Rp ",
                            keyboardType = KeyboardType.Number,
                            isTabularNums = true
                        )

                        // Quick Button Sesuai Saldo Sistem
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = 2.dp, y = 2.dp)
                                        .background(Color.Black, RectangleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE2E8F0), RectangleShape)
                                        .border(2.dp, Color.Black, RectangleShape)
                                        .clickable {
                                            actualBalanceText = if (account.balance % 1.0 == 0.0) {
                                                account.balance.toLong().toString()
                                            } else {
                                                account.balance.toString()
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Reset Sesuai Sistem",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Realtime Selisih Status Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    diff > 0.001 -> Color(0xFFDCFCE7)
                                    diff < -0.001 -> Color(0xFFFEE2E2)
                                    else -> Color(0xFFF1F5F9)
                                },
                                RectangleShape
                            )
                            .border(
                                2.dp,
                                when {
                                    diff > 0.001 -> RetroIncomeDarkGreen
                                    diff < -0.001 -> RetroExpenseDarkRed
                                    else -> Color.Black
                                },
                                RectangleShape
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        diff > 0.001 -> "SURPLUS (LEBIH)"
                                        diff < -0.001 -> "DEFISIT (KURANG)"
                                        else -> "SALDO COCOK"
                                    },
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        color = when {
                                            diff > 0.001 -> RetroIncomeDarkGreen
                                            diff < -0.001 -> RetroExpenseDarkRed
                                            else -> Color.Black
                                        }
                                    )
                                )

                                Text(
                                    text = when {
                                        diff > 0.001 -> "+${formatRupiah(diff)}"
                                        diff < -0.001 -> "-${formatRupiah(abs(diff))}"
                                        else -> "Rp 0"
                                    },
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = when {
                                            diff > 0.001 -> RetroIncomeDarkGreen
                                            diff < -0.001 -> RetroExpenseDarkRed
                                            else -> Color.Black
                                        },
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }

                            Text(
                                text = when {
                                    diff > 0.001 -> "Sistem akan mencatat pemasukan penyesuaian (Income) sebesar ${formatRupiah(diff)}."
                                    diff < -0.001 -> "Sistem akan mencatat pengeluaran penyesuaian (Expense) sebesar ${formatRupiah(abs(diff))}."
                                    else -> "Saldo fisik riil identik dengan catatan sistem. Tidak ada transaksi yang perlu disesuaikan."
                                },
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeobrutalButton(
                        onClick = onDismiss,
                        backgroundColor = Color(0xFFF1F5F9),
                        borderWidth = 2.dp,
                        shadowOffset = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "BATAL",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                        )
                    }

                    NeobrutalButton(
                        onClick = {
                            if (isValid) {
                                onReconcile(account, actualBalance)
                            }
                        },
                        enabled = isValid,
                        backgroundColor = RetroYellow,
                        borderWidth = 3.dp,
                        shadowOffset = 4.dp,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(
                            text = "REKONSILIASI",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}
