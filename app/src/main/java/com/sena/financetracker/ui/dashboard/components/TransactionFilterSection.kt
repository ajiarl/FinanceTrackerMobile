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
import androidx.compose.foundation.rememberScrollState
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
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.ui.components.RetroYellow

@Composable
fun TransactionFilterTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabOptions = listOf(
        "ALL" to stringResource(R.string.filter_type_all),
        "EXPENSE" to stringResource(R.string.filter_type_expense),
        "INCOME" to stringResource(R.string.filter_type_income)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabOptions.forEach { (key, label) ->
            val isSelected = selectedTab.equals(key, ignoreCase = true)
            NeobrutalFilterChip(
                label = label,
                isSelected = isSelected,
                selectedBg = RetroYellow,
                onClick = { onTabSelected(key) }
            )
        }
    }
}

@Composable
fun TransactionDateFilters(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateOptions = listOf(
        "ALL" to stringResource(R.string.filter_date_all),
        "TODAY" to stringResource(R.string.filter_date_today),
        "THIS_MONTH" to stringResource(R.string.filter_date_month)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dateOptions.forEach { (key, label) ->
            val isSelected = selectedFilter.equals(key, ignoreCase = true)
            NeobrutalFilterChip(
                label = label,
                isSelected = isSelected,
                selectedBg = RetroTransferBlue,
                selectedTextColor = Color.White,
                onClick = { onFilterSelected(key) }
            )
        }
    }
}

@Composable
fun TransactionCategoryChips(
    categories: List<CategoryEntity>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeobrutalFilterChip(
            label = "SEMUA KATEGORI",
            isSelected = selectedCategory == null,
            selectedBg = RetroYellow,
            onClick = { onCategorySelected(null) }
        )
        categories.forEach { cat ->
            val isSelected = selectedCategory?.equals(cat.name, ignoreCase = true) == true
            NeobrutalFilterChip(
                label = cat.name.uppercase(),
                isSelected = isSelected,
                selectedBg = RetroYellow,
                onClick = { onCategorySelected(if (isSelected) null else cat.name) }
            )
        }
    }
}

@Composable
private fun NeobrutalFilterChip(
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    selectedTextColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
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
                .background(if (isSelected) selectedBg else Color.White, RectangleShape)
                .border(if (isSelected) 2.dp else 1.5.dp, Color.Black, RectangleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = if (isSelected) selectedTextColor else Color.Black
                )
            )
        }
    }
}
