package com.sena.financetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.sena.financetracker.ui.components.NeobrutalButton
import com.sena.financetracker.ui.components.NeobrutalInputField
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroTransferBlue
import com.sena.financetracker.ui.components.RetroYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NeobrutalTransferDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onTransfer: (
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String,
        date: String
    ) -> Unit
) {
    var fromAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var toAccountId by remember {
        mutableStateOf(
            if (accounts.size > 1) accounts[1].id else (accounts.firstOrNull()?.id ?: 2L)
        )
    }

    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val fromAccount = accounts.find { it.id == fromAccountId } ?: accounts.firstOrNull()
    val toAccount = accounts.find { it.id == toAccountId } ?: accounts.getOrNull(1)

    val isSameAccount = fromAccountId == toAccountId
    val amountVal = amountText.toDoubleOrNull() ?: 0.0
    val isValid = !isSameAccount && amountVal > 0.0 && fromAccount != null && toAccount != null

    val presets = listOf(
        Pair("+20k", 20000.0),
        Pair("+50k", 50000.0),
        Pair("+100k", 100000.0),
        Pair("+200k", 200000.0),
        Pair("+500k", 500000.0)
    )

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
                // Header Bar (Solid Black Bar dengan ikon Transfer)
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
                                .background(RetroTransferBlue, RectangleShape)
                                .border(1.5.dp, Color.White, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "TRANSFER ANTAR REKENING",
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
                    // 1. Selector Akun Asal (Pengirim)
                    Column {
                        Text(
                            text = "DARI REKENING (PENGIRIM)",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSelected = acc.id == fromAccountId
                                Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .offset(x = 3.dp, y = 3.dp)
                                                .background(Color.Black, RectangleShape)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) RetroYellow else Color.White,
                                                RectangleShape
                                            )
                                            .border(2.dp, Color.Black, RectangleShape)
                                            .clickable { fromAccountId = acc.id }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = acc.name.uppercase(),
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
                    }

                    // 2. Selector Akun Tujuan (Penerima)
                    Column {
                        Text(
                            text = "KE REKENING (PENERIMA)",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSelected = acc.id == toAccountId
                                Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .offset(x = 3.dp, y = 3.dp)
                                                .background(Color.Black, RectangleShape)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) RetroTransferBlue else Color.White,
                                                RectangleShape
                                            )
                                            .border(2.dp, Color.Black, RectangleShape)
                                            .clickable { toAccountId = acc.id }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = acc.name.uppercase(),
                                            style = TextStyle(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Peringatan jika Akun Asal & Tujuan Sama
                    if (isSameAccount) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RectangleShape)
                                .border(2.dp, RetroExpenseRed, RectangleShape)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "⚠️ PERINGATAN: Rekening asal dan rekening tujuan tidak boleh sama!",
                                style = TextStyle(
                                    color = RetroExpenseRed,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // 3. Nominal Input & Presets Quick Button
                    Column {
                        NeobrutalInputField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    amountText = input
                                }
                            },
                            label = "Nominal Transfer (IDR)",
                            placeholder = "0",
                            prefix = "Rp ",
                            keyboardType = KeyboardType.Number,
                            isTabularNums = true
                        )

                        // Presets Row (+20k, +50k, +100k, +200k, +500k)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (label, addAmount) ->
                                Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .offset(x = 3.dp, y = 3.dp)
                                            .background(Color.Black, RectangleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(RetroYellow, RectangleShape)
                                            .border(2.dp, Color.Black, RectangleShape)
                                            .clickable {
                                                val current = amountText.toDoubleOrNull() ?: 0.0
                                                val updated = current + addAmount
                                                amountText = updated.toLong().toString()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
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
                    }

                    // 4. Catatan Opsional (Notes)
                    NeobrutalInputField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Catatan Transfer (Opsional)",
                        placeholder = "Keterangan keperluan transfer..."
                    )
                }

                // Footer Action Buttons (Batal & Transfer Dana)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tombol Batal
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

                    // Tombol Transfer Dana
                    NeobrutalButton(
                        onClick = {
                            if (isValid && fromAccount != null && toAccount != null) {
                                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                onTransfer(fromAccount, toAccount, amountVal, notes.trim(), currentDate)
                            }
                        },
                        enabled = isValid,
                        backgroundColor = RetroTransferBlue,
                        borderWidth = 3.dp,
                        shadowOffset = 4.dp,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(
                            text = "TRANSFER DANA",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
