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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah

@Composable
fun NeobrutalAddBudgetDialog(
    categories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveBudget: (name: String, category: String, limitAmount: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val defaultCategories = remember(categories) {
        val expenseCats = categories.filter { it.type.equals("EXPENSE", ignoreCase = true) }.map { it.name }
        if (expenseCats.isNotEmpty()) expenseCats else listOf(
            "Makanan & Minuman",
            "Transportasi",
            "Belanja",
            "Tagihan & Utilitas",
            "Hiburan",
            "Lainnya"
        )
    }
    var selectedCategory by remember { mutableStateOf(defaultCategories.firstOrNull() ?: "Makanan & Minuman") }
    var limitInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetLimits = listOf(500000.0, 1000000.0, 2000000.0, 3000000.0, 5000000.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(bottom = 12.dp, end = 12.dp)
        ) {
            // Hard Drop Shadow 6.dp kotak
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(Color.Black, RectangleShape)
            )

            // Dialog Window Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RectangleShape)
                    .border(3.dp, Color.Black, RectangleShape)
            ) {
                // Header Bar (Solid Black)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = RetroYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BUAT ANGGARAN BARU",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, RectangleShape)
                            .border(1.dp, Color.Black, RectangleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Form Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Field 1: Nama Anggaran
                    Text(
                        text = "NAMA ANGGARAN",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Contoh: Makan Siang Harian", fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RectangleShape
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Field 2: Kategori Anggaran
                    Text(
                        text = "KATEGORI PENGELUARAN",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        defaultCategories.forEach { cat ->
                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) RetroYellow else Color.White,
                                        RectangleShape
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.5.dp,
                                        Color.Black,
                                        RectangleShape
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    style = TextStyle(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Field 3: Nominal Limit Anggaran Bulanan
                    Text(
                        text = "LIMIT MAKSIMAL (BULANAN)",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("Contoh: 1000000", fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RectangleShape
                    )

                    // Quick Nominal Chips
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetLimits.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF1F5F9), RectangleShape)
                                    .border(1.dp, Color.Black, RectangleShape)
                                    .clickable { limitInput = preset.toLong().toString() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = formatRupiah(preset),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    // Error message if any
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.Red
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // BATAL
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF1F5F9), RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape)
                                .clickable { onDismiss() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BATAL",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.Black
                                )
                            )
                        }

                        // SIMPAN
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .background(RetroYellow, RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape)
                                .clickable {
                                    val parsedLimit = limitInput.toDoubleOrNull() ?: 0.0
                                    when {
                                        name.isBlank() -> {
                                            errorMessage = "Nama anggaran wajib diisi"
                                        }
                                        parsedLimit <= 0.0 -> {
                                            errorMessage = "Nominal limit harus lebih besar dari Rp 0"
                                        }
                                        else -> {
                                            onSaveBudget(name.trim(), selectedCategory, parsedLimit)
                                            onDismiss()
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SIMPAN ANGGARAN",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
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
}
