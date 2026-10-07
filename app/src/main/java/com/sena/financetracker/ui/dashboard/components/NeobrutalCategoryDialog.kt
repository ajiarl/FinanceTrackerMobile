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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.ui.components.NeobrutalButton
import com.sena.financetracker.ui.components.NeobrutalInputField
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow

/**
 * Modal Dialog Neobrutalisme untuk Tambah dan Edit Kategori Transaksi.
 *
 * Mengakomodasi:
 * - Input nama kategori.
 * - Pilihan tipe transaksi: PENGELUARAN (Merah) vs PEMASUKAN (Hijau).
 * - Color swatch picker Neobrutal: Retro Yellow, Emerald, Coral Red, Sky Blue, Purple, Pink.
 */
@Composable
fun NeobrutalCategoryDialog(
    categoryToEdit: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, color: String) -> Unit
) {
    var categoryName by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var selectedType by remember { mutableStateOf(categoryToEdit?.type ?: "EXPENSE") }
    var selectedColor by remember { mutableStateOf(categoryToEdit?.color ?: "#FAFF00") }

    val colorOptions = listOf(
        "#FAFF00", // Retro Yellow
        "#00E676", // Emerald Green
        "#DC2626", // Coral Red
        "#007AFF", // Sky Blue
        "#A855F7", // Purple
        "#EC4899"  // Pink
    )

    val isValid = categoryName.isNotBlank()
    val isEditMode = categoryToEdit != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
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

            // Main Dialog Content Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RectangleShape)
                    .border(3.dp, Color.Black, RectangleShape)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val parsedHeaderColor = try {
                        Color(android.graphics.Color.parseColor(selectedColor))
                    } catch (e: Exception) {
                        RetroYellow
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(parsedHeaderColor, RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (isEditMode) "EDIT KATEGORI" else "KATEGORI BARU",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Black
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Nama Kategori
                Text(
                    text = "NAMA KATEGORI",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                NeobrutalInputField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    placeholder = "Misal: Langganan SaaS, Bensin...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tipe Transaksi: PENGELUARAN vs PEMASUKAN
                Text(
                    text = "TIPE KATEGORI",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isExpense = selectedType == "EXPENSE"
                    val isIncome = selectedType == "INCOME"

                    // Tombol Tipe Pengeluaran
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = "EXPENSE" }
                    ) {
                        if (isExpense) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isExpense) RetroExpenseRed else Color.White,
                                    RectangleShape
                                )
                                .border(
                                    if (isExpense) 2.dp else 1.5.dp,
                                    Color.Black,
                                    RectangleShape
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PENGELUARAN",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (isExpense) Color.White else Color.Black
                                )
                            )
                        }
                    }

                    // Tombol Tipe Pemasukan
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = "INCOME" }
                    ) {
                        if (isIncome) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isIncome) RetroIncomeGreen else Color.White,
                                    RectangleShape
                                )
                                .border(
                                    if (isIncome) 2.dp else 1.5.dp,
                                    Color.Black,
                                    RectangleShape
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PEMASUKAN",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Picker Mini (Pilihan Warna Neobrutal)
                Text(
                    text = "PILIHAN WARNA NEOBRUTAL",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { hexColor ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hexColor))
                        } catch (e: Exception) {
                            RetroYellow
                        }
                        val isColorSelected = selectedColor.equals(hexColor, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedColor = hexColor }
                        ) {
                            if (isColorSelected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = 2.dp, y = 2.dp)
                                        .background(Color.Black, RectangleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(parsedColor, RectangleShape)
                                    .border(
                                        if (isColorSelected) 3.dp else 1.5.dp,
                                        Color.Black,
                                        RectangleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Terpilih",
                                        tint = if (hexColor == "#DC2626" || hexColor == "#007AFF") Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Simpan
                NeobrutalButton(
                    onClick = {
                        if (isValid) {
                            onSave(categoryName.trim(), selectedType, selectedColor)
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    backgroundColor = RetroYellow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEditMode) "SIMPAN PERUBAHAN" else "TAMBAH KATEGORI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    )
                }
            }
        }
    }
}
