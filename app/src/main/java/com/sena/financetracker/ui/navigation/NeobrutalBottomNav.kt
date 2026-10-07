package com.sena.financetracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.ui.components.RetroYellow

/**
 * Bottom Navigation Bar dengan Strict Neobrutalism UI (border hitam pekat, hard drop shadow kotak).
 *
 * @param currentRoute Rute destinasi tab yang sedang aktif.
 * @param onTabSelected Callback saat pengguna mengklik salah satu item tab.
 */
@Composable
fun NeobrutalBottomNav(
    currentRoute: String,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(Color.White)
            .drawBehind {
                // Top border 3.dp hitam pekat
                val strokeWidth = 3.dp.toPx()
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = Screen.bottomNavItems
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                BottomNavItem(
                    screen = screen,
                    isSelected = isSelected,
                    onClick = { onTabSelected(screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Container aktif dengan hard drop shadow kotak 2.dp
            Box {
                // Shadow kotak hitam
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 2.dp, y = 2.dp)
                        .background(Color.Black, RectangleShape)
                )
                // Card utama aktif warna RetroYellow #FAFF00
                Row(
                    modifier = Modifier
                        .background(RetroYellow, RectangleShape)
                        .border(2.dp, Color.Black, RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " " + screen.title.uppercase(),
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    )
                }
            }
        } else {
            // Container tidak aktif: ikon dan label minimalis vertikal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.title,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = screen.title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                )
            }
        }
    }
}
