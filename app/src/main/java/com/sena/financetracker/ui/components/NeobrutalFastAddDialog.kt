package com.sena.financetracker.ui.components

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
import androidx.compose.material.icons.automirrored.filled.List
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NeobrutalFastAddDialog(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onManageCategoriesClick: (() -> Unit)? = null,
    onSave: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: String,
        accountId: Long,
        accountName: String,
        notes: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME", "TRANSFER"
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val defaultAccountId = accounts.firstOrNull()?.id ?: 1L
    val defaultAccountName = accounts.firstOrNull()?.name ?: "Dompet Tunai"
    var selectedAccountId by remember { mutableStateOf(defaultAccountId) }
    var selectedAccountName by remember { mutableStateOf(defaultAccountName) }

    val filteredCategories = categories.filter {
        it.type.equals(selectedType, ignoreCase = true)
    }.ifEmpty { categories }

    var selectedCategory by remember {
        mutableStateOf(filteredCategories.firstOrNull()?.name ?: "Lainnya")
    }

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
            // Drop shadow
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
                // Header Bar (Web replica: solid black bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.title_fast_add),
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
                            contentDescription = stringResource(R.string.action_close),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Scrollable Form content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Type Selector (Web replica: Keluar / Masuk / Transfer segmented box)
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
                                val isSelected = selectedType == typeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) activeColor else Color.White)
                                        .clickable {
                                            selectedType = typeKey
                                            // Reset category if not matching
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
                                if (input.all { it.isDigit() || it == '.' }) {
                                    amountText = input
                                }
                            },
                            label = "Jumlah (IDR)",
                            placeholder = "0",
                            prefix = "Rp ",
                            keyboardType = KeyboardType.Number,
                            isTabularNums = true
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

                    // 3. Title / Description
                    NeobrutalInputField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Deskripsi / Judul",
                        placeholder = stringResource(R.string.placeholder_title)
                    )

                    // 4. Account Selection (Chips with border 2.dp & hard shadow)
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

                    // 5. Category Selection
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KATEGORI",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.Black
                                )
                            )
                            onManageCategoriesClick?.let { manageClick ->
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFE2E8F0), RectangleShape)
                                        .border(1.dp, Color.Black, RectangleShape)
                                        .clickable {
                                            onDismiss()
                                            manageClick()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "+ KELOLA",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = Color.Black
                                        )
                                    )
                                }
                            }
                        }
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

                    // 6. Notes (Optional)
                    NeobrutalInputField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Catatan (Opsional)",
                        placeholder = stringResource(R.string.placeholder_notes)
                    )
                }

                // Footer Save Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    val amountVal = amountText.toDoubleOrNull() ?: 0.0
                    val isValid = amountVal > 0.0 && title.isNotBlank()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    NeobrutalButton(
                        onClick = {
                            if (isValid) {
                                NeobrutalHapticEngine.heavyClick(context, true)
                                onSave(
                                    title.trim(),
                                    amountVal,
                                    selectedType,
                                    selectedCategory,
                                    todayDate,
                                    selectedAccountId,
                                    selectedAccountName,
                                    notes.trim()
                                )
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        backgroundColor = RetroYellow,
                        borderWidth = 3.dp,
                        shadowOffset = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SIMPAN TRANSAKSI",
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
