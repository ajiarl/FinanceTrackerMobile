package com.sena.financetracker.ui.dashboard.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.R
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah
import com.sena.financetracker.viewmodel.FinanceUiState

/**
 * Tab 4 - Akun & Rekening (AccountsScreen):
 * - Header "DOMPET & REKENING", total kekayaan di seluruh akun, tombol "+ AKUN", dan tombol "TRANSFER".
 * - List kartu akun dengan rincian saldo dan tombol aksi rekonsiliasi saldo riil.
 */
@Composable
fun AccountsScreen(
    uiState: FinanceUiState,
    onAddAccountClick: () -> Unit,
    onTransferClick: () -> Unit,
    onAccountClick: (AccountEntity) -> Unit,
    onManageCategoriesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "MANAJEMEN REKENING",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = stringResource(R.string.title_screen_accounts),
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color.Black
                    )
                )
            }
        }

        // Kartu Akumulasi Saldo Total Akun
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 6.dp, y = 6.dp)
                        .background(Color.Black, RectangleShape)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RectangleShape)
                        .border(3.dp, Color.Black, RectangleShape)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TOTAL KEKAYAAN TERVERIFIKASI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFF64748B)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rp " + formatRupiah(uiState.totalBalance),
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = (-1).sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: + AKUN dan TRANSFER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tombol Tambah Akun
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onAddAccountClick)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RetroYellow, RectangleShape)
                                    .border(2.dp, Color.Black, RectangleShape)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ AKUN BARU",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.Black
                                    )
                                )
                            }
                        }

                        // Tombol Transfer
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onTransferClick)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(Color.Black, RectangleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RectangleShape)
                                    .border(2.dp, Color.Black, RectangleShape)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⇄ TRANSFER DANA",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tombol Kelola Kategori
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onManageCategoriesClick)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 3.dp, y = 3.dp)
                                .background(Color.Black, RectangleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0), RectangleShape)
                                .border(2.dp, Color.Black, RectangleShape)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "🏷️ KELOLA KATEGORI",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daftar Akun Individual
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "DAFTAR AKUN AKTIF (${uiState.accounts.size})",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                )

                if (uiState.accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada rekening/dompet terdaftar.",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        )
                    }
                } else {
                    uiState.accounts.forEach { acc ->
                        AccountItemCard(
                            account = acc,
                            onReconcileClick = { onAccountClick(acc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountItemCard(
    account: AccountEntity,
    onReconcileClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
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
                .border(2.dp, Color.Black, RectangleShape)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = account.name,
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE2E8F0), RectangleShape)
                            .border(1.dp, Color.Black, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = account.type.uppercase(),
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rp " + formatRupiah(account.balance),
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color.Black
                    )
                )
            }

            // Tombol Rekonsiliasi Neobrutal
            Box(
                modifier = Modifier.clickable(onClick = onReconcileClick)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 2.dp, y = 2.dp)
                        .background(Color.Black, RectangleShape)
                )
                Box(
                    modifier = Modifier
                        .background(Color.White, RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REKONSILIASI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    )
                }
            }
        }
    }
}
