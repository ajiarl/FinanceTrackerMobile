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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.ui.components.NeobrutalBadge
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.RetroTransferBlue
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.formatRupiah

@Composable
fun DashboardAccountsSection(
    accounts: List<AccountEntity>,
    onTransferClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AKUN & DOMPET",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Transfer Neobrutal
                Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Box(
                        modifier = Modifier
                            .background(RetroTransferBlue, RectangleShape)
                            .border(2.dp, Color.Black, RectangleShape)
                            .clickable(onClick = onTransferClick)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TRANSFER",
                            style = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                NeobrutalBadge(
                    text = "${accounts.size} AKUN",
                    backgroundColor = RetroYellow
                )
            }
        }

        // Horizontal Scrollable Accounts List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            accounts.forEach { account ->
                AccountNeobrutalItem(account = account)
            }
        }
    }
}

@Composable
fun AccountNeobrutalItem(
    account: AccountEntity,
    modifier: Modifier = Modifier
) {
    val badgeConfig = when (account.type.lowercase()) {
        "bank" -> Pair("BANK", Color(0xFF93C5FD))
        "e-wallet" -> Pair("E-WALLET", Color(0xFFFDE047))
        else -> Pair("TUNAI", Color(0xFF86EFAC))
    }

    NeobrutalCard(
        modifier = modifier.width(160.dp),
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp,
        fillMaxWidth = false
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeobrutalBadge(
                    text = badgeConfig.first,
                    backgroundColor = badgeConfig.second
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = account.name.uppercase(),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.Black
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SALDO",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black.copy(alpha = 0.5f)
                )
            )

            Text(
                text = formatRupiah(account.balance),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.Black,
                    fontFeatureSettings = "tnum"
                )
            )
        }
    }
}
