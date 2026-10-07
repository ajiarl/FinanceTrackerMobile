package com.sena.financetracker.ui.dashboard.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.NeobrutalCategoryDialog
import com.sena.financetracker.ui.dashboard.components.NeobrutalConfirmDialog
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Layar Kelola Kategori Kustom (CategoriesScreen) dengan UI Strict Neobrutalism.
 *
 * Fitur:
 * - Header navigasi kembali ke layar sebelumnya.
 * - Filter Tab Neobrutal: SEMUA, PENGELUARAN, PEMASUKAN.
 * - Tombol "+ KATEGORI BARU" Neobrutal RetroYellow.
 * - List kartu kategori: swatch warna, badge jenis, nama kategori, dan proteksi hapus kategori bawaan sistem.
 */
@Composable
fun CategoriesScreen(
    uiState: FinanceUiState,
    onNavigateBack: () -> Unit,
    onAddCategory: (name: String, type: String, color: String) -> Unit,
    onUpdateCategory: (id: Long, name: String, type: String, color: String) -> Unit,
    onDeleteCategory: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilterType by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"

    var showDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val systemCategoryNames = setOf(
        "Makanan & Minuman",
        "Transportasi",
        "Belanja",
        "Tagihan & Utilitas",
        "Hiburan",
        "Gaji",
        "Freelance",
        "Investasi",
        "Bonus",
        "Lainnya",
        "Transfer",
        "Penyesuaian",
        "Saldo Awal"
    )

    val filteredCategories = remember(uiState.categories, selectedFilterType) {
        when (selectedFilterType) {
            "EXPENSE" -> uiState.categories.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            "INCOME" -> uiState.categories.filter { it.type.equals("INCOME", ignoreCase = true) }
            else -> uiState.categories
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar & Navigasi Kembali
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PENGATURAN",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                color = Color.Black.copy(alpha = 0.5f)
                            )
                        )
                        Text(
                            text = "KELOLA KATEGORI",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp,
                                color = Color.Black
                            )
                        )
                    }
                }

                // Tombol + Kategori Baru
                Box(
                    modifier = Modifier.clickable {
                        categoryToEdit = null
                        showDialog = true
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 3.dp, y = 3.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Row(
                        modifier = Modifier
                            .background(RetroYellow, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "BARU",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Filter Tab Neobrutal: SEMUA / PENGELUARAN / PEMASUKAN
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    Triple("ALL", "SEMUA", Color.White),
                    Triple("EXPENSE", "PENGELUARAN", RetroExpenseRed),
                    Triple("INCOME", "PEMASUKAN", RetroIncomeGreen)
                )

                tabs.forEach { (typeKey, label, accentColor) ->
                    val isSelected = selectedFilterType == typeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilterType = typeKey }
                    ) {
                        if (isSelected) {
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
                                    if (isSelected) (if (typeKey == "ALL") RetroYellow else accentColor) else Color.White,
                                    RectangleShape
                                )
                                .border(
                                    if (isSelected) 2.dp else 1.5.dp,
                                    Color.Black,
                                    RectangleShape
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = if (isSelected && typeKey == "EXPENSE") Color.White else Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        // Count Info
        item {
            Text(
                text = "TOTAL KATEGORI: ${filteredCategories.size}",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            )
        }

        // Empty State
        if (filteredCategories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BELUM ADA KATEGORI PADA FILTER INI",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        } else {
            // Daftar Kategori Card
            items(filteredCategories, key = { it.id }) { cat ->
                val isSystem = systemCategoryNames.any { it.equals(cat.name.trim(), ignoreCase = true) }
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(cat.color))
                } catch (e: Exception) {
                    RetroYellow
                }
                val isExpense = cat.type.equals("EXPENSE", ignoreCase = true)

                Box(modifier = Modifier.fillMaxWidth()) {
                    // Hard shadow kotak hitam 3.dp
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
                        // Info Kiri: Swatch Warna & Nama & Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Swatch Warna
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(parsedColor, RectangleShape)
                                    .border(2.dp, Color.Black, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = if (cat.color == "#DC2626" || cat.color == "#007AFF") Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = cat.name.uppercase(),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Badge Tipe
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isExpense) RetroExpenseRed.copy(alpha = 0.15f) else RetroIncomeGreen.copy(alpha = 0.25f),
                                                RectangleShape
                                            )
                                            .border(1.dp, Color.Black, RectangleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isExpense) "PENGELUARAN" else "PEMASUKAN",
                                            style = TextStyle(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = if (isExpense) RetroExpenseRed else Color(0xFF0F5132)
                                            )
                                        )
                                    }

                                    // Badge Sistem vs Kustom
                                    if (isSystem) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE2E8F0), RectangleShape)
                                                .border(1.dp, Color.Black, RectangleShape)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SISTEM",
                                                style = TextStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF475569)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tombol Aksi Kanan: Edit & Hapus
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Tombol Edit
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(RetroYellow, RectangleShape)
                                    .border(1.5.dp, Color.Black, RectangleShape)
                                    .clickable {
                                        categoryToEdit = cat
                                        showDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Kategori",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Tombol Hapus (Nonaktif atau ditolak jika kategori sistem)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isSystem) Color(0xFFE2E8F0) else RetroExpenseRed,
                                        RectangleShape
                                    )
                                    .border(1.5.dp, Color.Black, RectangleShape)
                                    .clickable {
                                        if (isSystem) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Kategori sistem bawaan '${cat.name}' tidak dapat dihapus!",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        } else {
                                            categoryToDelete = cat
                                            showDeleteConfirm = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Kategori",
                                    tint = if (isSystem) Color(0xFF94A3B8) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog Tambah / Edit Kategori
    if (showDialog) {
        NeobrutalCategoryDialog(
            categoryToEdit = categoryToEdit,
            onDismiss = {
                showDialog = false
                categoryToEdit = null
            },
            onSave = { name, type, color ->
                val editTarget = categoryToEdit
                if (editTarget != null) {
                    onUpdateCategory(editTarget.id, name, type, color)
                    Toast.makeText(context, "Kategori '$name' berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    onAddCategory(name, type, color)
                    Toast.makeText(context, "Kategori '$name' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                }
                showDialog = false
                categoryToEdit = null
            }
        )
    }

    // Modal Konfirmasi Hapus Kategori Kustom
    if (showDeleteConfirm && categoryToDelete != null) {
        val targetCat = categoryToDelete!!
        NeobrutalConfirmDialog(
            title = "HAPUS KATEGORI?",
            message = "Kategori '${targetCat.name}' akan dihapus secara permanen dari database. Lanjutkan?",
            confirmButtonText = "HAPUS SEKARANG",
            cancelButtonText = "BATALKAN",
            confirmButtonColor = RetroExpenseRed,
            onConfirm = {
                onDeleteCategory(targetCat.id)
                Toast.makeText(context, "Kategori '${targetCat.name}' berhasil dihapus!", Toast.LENGTH_SHORT).show()
                showDeleteConfirm = false
                categoryToDelete = null
            },
            onDismiss = {
                showDeleteConfirm = false
                categoryToDelete = null
            }
        )
    }
}
