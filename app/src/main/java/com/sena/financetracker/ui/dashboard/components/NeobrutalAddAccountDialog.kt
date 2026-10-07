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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.sena.financetracker.ui.components.NeobrutalButton
import com.sena.financetracker.ui.components.NeobrutalInputField
import com.sena.financetracker.ui.components.RetroYellow

@Composable
fun NeobrutalAddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, initialBalance: Double) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("bank") }
    var initialBalanceText by remember { mutableStateOf("0") }

    val accountTypes = listOf(
        Pair("bank", "BANK"),
        Pair("cash", "TUNAI / DOMPET"),
        Pair("e-wallet", "E-WALLET")
    )

    val balanceVal = initialBalanceText.toDoubleOrNull() ?: 0.0
    val isValid = accountName.isNotBlank() && balanceVal >= 0.0

    val presets = listOf(
        Pair("Nol", 0.0),
        Pair("+50k", 50000.0),
        Pair("+100k", 100000.0),
        Pair("+500k", 500000.0),
        Pair("+1 Jt", 1000000.0)
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
                // Header Bar (Solid Black Bar dengan ikon Wallet)
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
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "TAMBAH DOMPET / AKUN",
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
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Input Nama Akun
                    NeobrutalInputField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = "Nama Rekening / Akun",
                        placeholder = "Contoh: SeaBank, Bibit, BCA, Kasir"
                    )

                    // 2. Selector Tipe Akun
                    Column {
                        Text(
                            text = "TIPE AKUN",
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
                            accountTypes.forEach { (typeKey, label) ->
                                val isSelected = selectedType == typeKey
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
                                            .clickable { selectedType = typeKey }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
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

                    // 3. Saldo Awal
                    Column {
                        NeobrutalInputField(
                            value = initialBalanceText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    initialBalanceText = input
                                }
                            },
                            label = "Saldo Awal (IDR)",
                            placeholder = "0",
                            prefix = "Rp ",
                            keyboardType = KeyboardType.Number,
                            isTabularNums = true
                        )

                        // Quick presets
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
                                            .background(Color(0xFFF1F5F9), RectangleShape)
                                            .border(2.dp, Color.Black, RectangleShape)
                                            .clickable {
                                                if (addAmount == 0.0) {
                                                    initialBalanceText = "0"
                                                } else {
                                                    val cur = initialBalanceText.toDoubleOrNull() ?: 0.0
                                                    initialBalanceText = (cur + addAmount).toLong().toString()
                                                }
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
                                onSave(accountName.trim(), selectedType, balanceVal)
                            }
                        },
                        enabled = isValid,
                        backgroundColor = RetroYellow,
                        borderWidth = 3.dp,
                        shadowOffset = 4.dp,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(
                            text = "SIMPAN AKUN",
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
