package com.sena.financetracker.ui.dashboard.screens.importcsv

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
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
@Composable
fun ImportCsvHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clickable(onClick = onNavigateBack)) {
            Box(
                modifier = Modifier
                    .offset(x = 3.dp, y = 3.dp)
                    .size(42.dp)
                    .background(Color.Black, RectangleShape)
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White, RectangleShape)
                    .border(2.dp, Color.Black, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = stringResource(R.string.title_screen_import_csv),
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.Black,
                    letterSpacing = 0.5.sp
                )
            )
            Text(
                text = "Konversi CSV RFC 4180 ke Basis Data",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
fun CsvPreviewHeader(
    totalCount: Int,
    previewCount: Int,
    isEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LANGKAH 3: PRATINJAU DATA ($totalCount BARIS)",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )
            )
            if (totalCount > 0) {
                Text(
                    text = "Menampilkan $previewCount sampel",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }

        if (isEmpty) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .background(Color.Black, RectangleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RectangleShape)
                        .border(3.dp, Color.Black, RectangleShape)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Belum ada berkas CSV yang diunggah",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommitBatchButton(
    validCount: Int,
    isImporting: Boolean,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isImporting, onClick = onCommit)
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
                .background(Color(0xFF00E676), RectangleShape)
                .border(3.dp, Color.Black, RectangleShape)
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isImporting) "MEMPROSES IMPOR BATCH..." else "IMPOR SEKARANG ($validCount TRANSAKSI)",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )
            )
        }
    }
}
