package com.sena.financetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Strict Neobrutalism Color Palette ─────────────────────────────────────────
val RetroYellow = Color(0xFFFAFF00)
val RetroIncomeGreen = Color(0xFF00E676)
val RetroIncomeDarkGreen = Color(0xFF00A878)
val RetroExpenseRed = Color(0xFFFF3B30)
val RetroExpenseDarkRed = Color(0xFFDC2626)
val RetroTransferBlue = Color(0xFF007AFF)
val RetroAccountBlue = Color(0xFF2563EB)
val RetroBorder = Color(0xFF000000)
val RetroCanvas = Color(0xFFF8F8F5)
val RetroSurface = Color(0xFFFFFFFF)

/**
 * Strict Neobrutalism Box/Card:
 * - Sharp rectangular edges (RectangleShape / 0.dp)
 * - 2.dp or 3.dp pure black border
 * - Hard offset drop shadow without blur
 */
@Composable
fun NeobrutalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = RetroSurface,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = Color.Black,
    fillMaxWidth: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        // Hard drop shadow layer (sharp rectangle, offset, no blur)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, RectangleShape)
        )
        // Foreground container with bold black border
        val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        val widthModifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier

        Box(
            modifier = Modifier
                .then(widthModifier)
                .background(backgroundColor, RectangleShape)
                .border(borderWidth, Color.Black, RectangleShape)
                .then(clickModifier),
            content = content
        )
    }
}

/**
 * Strict Neobrutalism Button:
 * - Yellow #FAFF00 (or custom color)
 * - Sharp rectangular corners
 * - Bold black border
 * - Hard black drop shadow
 */
@Composable
fun NeobrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = RetroYellow,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = Color.Black,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, RectangleShape)
        )
        Box(
            modifier = Modifier
                .background(if (enabled) backgroundColor else Color.LightGray, RectangleShape)
                .border(borderWidth, Color.Black, RectangleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

/**
 * Strict Neobrutalism Badge:
 * - Uppercase bold typography
 * - Solid black border
 * - High-contrast accent background
 */
@Composable
fun NeobrutalBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = RetroYellow,
    textColor: Color = Color.Black,
    borderWidth: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .border(borderWidth, Color.Black, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            ),
            color = textColor
        )
    }
}

/**
 * Strict Neobrutalism Input Field:
 * - Flat sharp input container
 * - 2.dp solid black border
 * - Optional label uppercase
 */
@Composable
fun NeobrutalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isTabularNums: Boolean = false,
    singleLine: Boolean = true,
    prefix: String? = null
) {
    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label.uppercase(),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RectangleShape)
                .border(2.dp, Color.Black, RectangleShape)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!prefix.isNullOrBlank()) {
                    Text(
                        text = prefix,
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontFeatureSettings = if (isTabularNums) "tnum" else null
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Gray,
                                fontFeatureSettings = if (isTabularNums) "tnum" else null
                            )
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        cursorBrush = SolidColor(Color.Black),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        textStyle = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontFeatureSettings = if (isTabularNums) "tnum" else null
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Strict Neobrutalism Toggle Switch:
 * - Thick black border
 * - Hard shadow
 * - Rectangular track & thumb
 * - Vivid RetroYellow when checked, Muted Gray when unchecked
 */
@Composable
fun NeobrutalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkedColor: Color = RetroYellow,
    uncheckedColor: Color = Color(0xFFE5E7EB),
    thumbColor: Color = Color.Black
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = modifier
            .width(54.dp)
            .height(30.dp)
            .clickable {
                try {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {}
                onCheckedChange(!checked)
            }
    ) {
        // Drop shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(Color.Black, RectangleShape)
        )

        // Track container
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(if (checked) checkedColor else uncheckedColor, RectangleShape)
                .border(2.dp, Color.Black, RectangleShape)
                .padding(2.dp)
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(22.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(thumbColor, RectangleShape)
                    .border(1.dp, Color.Black, RectangleShape)
            )
        }
    }
}
