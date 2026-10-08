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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sena.financetracker.R
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.NeobrutalButton
import com.sena.financetracker.ui.components.NeobrutalInputField
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroTransferBlue
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.components.RupiahVisualTransformation
import com.sena.financetracker.util.CurrencyParser

@Composable
fun NeobrutalEditTransactionDialog(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var selectedType by remember { mutableStateOf(transaction.type) }
    var title by remember { mutableStateOf(transaction.title) }
    var amountText by remember {
        mutableStateOf(
            if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString()
            else transaction.amount.toString()
        )
    }
    var notes by remember { mutableStateOf(transaction.notes) }

    var selectedAccountId by remember {
        mutableStateOf(if (transaction.accountId != 0L) transaction.accountId else (accounts.firstOrNull()?.id ?: 1L))
    }
    var selectedAccountName by remember {
        mutableStateOf(
            transaction.accountName.ifBlank { accounts.find { it.id == selectedAccountId }?.name ?: "Dompet Tunai" }
        )
    }

    val filteredCategories = categories.filter {
        it.type.equals(selectedType, ignoreCase = true)
    }.ifEmpty { categories }

    var selectedCategory by remember {
        mutableStateOf(transaction.category.ifBlank { filteredCategories.firstOrNull()?.name ?: "Lainnya" })
    }

    val presets = listOf(
        Pair("+5k", 5000.0),
        Pair("+10k", 10000.0),
        Pair("+25k", 25000.0),
        Pair("+50k", 50000.0),
        Pair("+100k", 100000.0)
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
            // Drop shadow kotak hitam
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
                // Header Bar (Solid Black Bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDIT TRANSAKSI",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 2.sp
                        )
                    )

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
                    // 1. Type Selector (EXPENSE / INCOME / TRANSFER)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 4.dp, y = 4.dp)
                                .background(Color.Black, RectangleShape)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RectangleShape)
                                .border(3.dp, Color.Black, RectangleShape)
                        ) {
                            val types = listOf(
                                Triple("EXPENSE", stringResource(R.string.filter_type_expense), RetroExpenseRed),
                                Triple("INCOME", stringResource(R.string.filter_type_income), RetroIncomeGreen),
                                Triple("TRANSFER", "TRF", RetroTransferBlue)
                            )
                            types.forEachIndexed { index, (typeKey, typeLabel, activeColor) ->
                                val isSelected = selectedType.equals(typeKey, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) activeColor else Color.White)
                                        .clickable {
                                            selectedType = typeKey
                                            val newFiltered = categories.filter {
                                                it.type.equals(typeKey, ignoreCase = true)
                                            }
                                            if (newFiltered.isNotEmpty()) {
                                                selectedCategory = newFiltered.first().name
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = typeLabel,
                                        style = TextStyle(
                                            color = if (isSelected) Color.White else Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                                if (index < types.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(44.dp)
                                            .background(Color.Black)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Nominal Input & Presets
                    Column {
                        NeobrutalInputField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                                    amountText = input
                                }
                            },
                            label = "Jumlah (IDR)",
                            placeholder = "0",
                            prefix = "Rp ",
                            keyboardType = KeyboardType.Number,
                            isTabularNums = true,
                            visualTransformation = RupiahVisualTransformation()
                        )

                        // Presets Row (#FAFF00 with 2.dp black border and hard shadow)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (label, addAmount) ->
                                Box(
                                    modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)
                                ) {
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
                                                val current = CurrencyParser.parseCurrencyInput(amountText)
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

                    // 3. Deskripsi / Judul
                    NeobrutalInputField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Deskripsi / Judul",
                        placeholder = stringResource(R.string.placeholder_title)
                    )

                    // 4. Pilih Akun Rekening
                    Column {
                        Text(
                            text = "PILIH AKUN",
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
                                val isSelected = acc.id == selectedAccountId
                                Box(
                                    modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)
                                ) {
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
                                            .clickable {
                                                selectedAccountId = acc.id
                                                selectedAccountName = acc.name
                                            }
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

                    // 5. Pilih Kategori
                    Column {
                        Text(
                            text = "KATEGORI",
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
                            filteredCategories.forEach { cat ->
                                val isSelected = cat.name.equals(selectedCategory, ignoreCase = true)
                                Box(
                                    modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)
                                ) {
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
                                            .clickable {
                                                selectedCategory = cat.name
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = cat.name.uppercase(),
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

                    // 6. Catatan (Notes)
                    NeobrutalInputField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Catatan (Opsional)",
                        placeholder = stringResource(R.string.placeholder_notes)
                    )
                }

                // Footer Action Buttons (Batal & Simpan Perubahan)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val amountVal = CurrencyParser.parseCurrencyInput(amountText)
                    val isValid = amountVal > 0.0 && title.isNotBlank()

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

                    // Tombol Simpan Perubahan
                    NeobrutalButton(
                        onClick = {
                            if (isValid) {
                                val updated = transaction.copy(
                                    title = title.trim(),
                                    amount = amountVal,
                                    type = selectedType.trim().uppercase(),
                                    category = selectedCategory.trim(),
                                    accountId = selectedAccountId,
                                    accountName = selectedAccountName.trim(),
                                    notes = notes.trim()
                                )
                                onSave(updated)
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        backgroundColor = RetroYellow,
                        borderWidth = 3.dp,
                        shadowOffset = 4.dp,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(
                            text = "SIMPAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}
