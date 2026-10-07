package com.sena.financetracker.ui.dashboard.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
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
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.NeobrutalSwitch
import com.sena.financetracker.ui.components.RetroYellow

@Composable
fun HapticSettingsCard(
    isHapticEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    NeobrutalCard(
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isHapticEnabled) RetroYellow else Color.LightGray, RectangleShape)
                    .border(2.dp, Color.Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Haptic Feedback",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RESPON TAKTIL / GETAR",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                )
                Text(
                    text = if (isHapticEnabled)
                        "Getaran taktil mekanik aktif pada tombol & dialog"
                    else
                        "Umpan balik getaran dinonaktifkan",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            NeobrutalSwitch(
                checked = isHapticEnabled,
                onCheckedChange = onToggleHaptic
            )
        }
    }
}
