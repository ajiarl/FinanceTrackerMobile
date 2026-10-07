package com.sena.financetracker.ui.dashboard.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.util.CsvImporter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.NumberFormat
import java.util.Locale

/**
 * Layar Impor Berkas CSV Transaksi (ImportCsvScreen) bergaya Strict Neobrutalism.
 *
 * Alur 3 Langkah:
 * 1. Unggah berkas CSV via Android SAF (Storage Access Framework).
 * 2. Pemetaan Akun Target (menentukan rekening default penampung transaksi).
 * 3. Pratinjau Tabel Neobrutal (5-10 baris data valid/invalid).
 * 4. Komit Batch Eksekusi ke SQLite/Room dengan feedback reaktif.
 */
@Composable
fun ImportCsvScreen(
    accounts: List<AccountEntity>,
    onNavigateBack: () -> Unit,
    onImportBatch: (List<TransactionEntity>, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var rawCsvText by remember { mutableStateOf<String?>(null) }
    var parsedList by remember { mutableStateOf<List<CsvImporter.ParsedTransaction>>(emptyList()) }
    var selectedAccountId by remember {
        mutableLongStateOf(accounts.firstOrNull()?.id ?: 1L)
    }
    var isImporting by remember { mutableStateOf(false) }

    // Launcher SAF untuk memilih berkas CSV / Plaintext
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.use { it.readText() }

                // Ekstrak nama file sederhana
                val pathSegments = uri.pathSegments
                val fileName = pathSegments?.lastOrNull() ?: "transaksi.csv"
                selectedFileName = fileName
                rawCsvText = content

                // Parsing langsung teks CSV
                val parsed = CsvImporter.parseCsv(content)
                parsedList = parsed

                if (parsed.isEmpty()) {
                    Toast.makeText(context, "Berkas kosong atau format tidak sesuai", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Berhasil memuat ${parsed.size} baris data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca berkas: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
    val validTransactions = parsedList.filter { it.isValid }
    val invalidCount = parsedList.size - validTransactions.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RetroCanvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── TOP BAR HEADER ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Back Neobrutal
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
                        text = "IMPOR TRANSAKSI",
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

        // ── LANGKAH 1: PILIH BERKAS CSV ──────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LANGKAH 1: PILIH BERKAS CSV",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color.Black
                    )
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RectangleShape)
                            .border(3.dp, Color.Black, RectangleShape)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF00E676), RectangleShape)
                                    .border(2.dp, Color.Black, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (selectedFileName != null) selectedFileName!! else "Belum ada berkas dipilih",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (parsedList.isNotEmpty()) "${parsedList.size} baris ditemukan (${validTransactions.size} valid, $invalidCount invalid)" else "Format didukung: text/csv, RFC 4180",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                )
                            }
                        }

                        // Tombol Pilih Berkas Neobrutal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filePickerLauncher.launch("*/*")
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
                                    .fillMaxWidth()
                                    .background(RetroYellow, RectangleShape)
                                    .border(2.dp, Color.Black, RectangleShape)
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedFileName != null) "GANTI BERKAS CSV" else "PILIH BERKAS DARI STORAGE",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
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

        // ── LANGKAH 2: PEMETAAN AKUN PENAMPUNG ────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LANGKAH 2: AKUN PENAMPUNG TRANSAKSI",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color.Black
                    )
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(Color.Black, RectangleShape)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RectangleShape)
                            .border(3.dp, Color.Black, RectangleShape)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pilih akun/rekening penampung jika data CSV tidak memuat kolom akun:",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF4B5563)
                            )
                        )

                        // Horizontal list Akun Neobrutal Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSelected = acc.id == selectedAccountId
                                Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .offset(x = 3.dp, y = 3.dp)
                                                .background(Color.Black, RectangleShape)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) Color(0xFF007AFF) else Color.White,
                                                RectangleShape
                                            )
                                            .border(2.dp, Color.Black, RectangleShape)
                                            .clickable { selectedAccountId = acc.id }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = acc.name.uppercase(),
                                                style = TextStyle(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) Color.White else Color.Black
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── LANGKAH 3: PRATINJAU TABEL NEOBRUTAL ──────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LANGKAH 3: PRATINJAU DATA (${parsedList.size} BARIS)",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = Color.Black
                        )
                    )
                    if (parsedList.isNotEmpty()) {
                        Text(
                            text = "Menampilkan ${parsedList.take(10).size} sampel",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }

                if (parsedList.isEmpty()) {
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

        // List Preview Baris Data (Maksimal 10 Baris Pratinjau)
        itemsIndexed(parsedList.take(10)) { index, row ->
            PreviewTransactionCard(
                index = index + 1,
                item = row,
                accountName = selectedAccount?.name ?: "Dompet Tunai"
            )
        }

        // ── TOMBOL KOMIT EKSEKUSI BATCH ──────────────────────────────────────
        item {
            if (validTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isImporting) {
                            if (selectedAccount == null) {
                                Toast.makeText(context, "Pilih akun penampung terlebih dahulu", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }

                            isImporting = true
                            val entities = CsvImporter.toTransactionEntities(
                                parsedList = validTransactions,
                                targetAccountId = selectedAccount.id,
                                targetAccountName = selectedAccount.name
                            )

                            onImportBatch(entities) {
                                isImporting = false
                                Toast.makeText(
                                    context,
                                    "Berhasil mengimpor ${entities.size} transaksi ke database!",
                                    Toast.LENGTH_LONG
                                ).show()
                                onNavigateBack()
                            }
                        }
                ) {
                    // Hard Drop Shadow 4dp
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
                            text = if (isImporting) "MEMPROSES IMPOR BATCH..." else "IMPOR SEKARANG (${validTransactions.size} TRANSAKSI)",
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
        }
    }
}

/**
 * Kartu baris pratinjau transaksi CSV Neobrutal.
 */
@Composable
private fun PreviewTransactionCard(
    index: Int,
    item: CsvImporter.ParsedTransaction,
    accountName: String
) {
    val rupiahFormatter = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    val formattedAmount = rupiahFormatter.format(item.amount)

    Box(modifier = Modifier.fillMaxWidth()) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Badge Status Valid / Invalid
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(if (item.isValid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), RectangleShape)
                        .border(1.5.dp, Color.Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isValid) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Valid",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Invalid",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#$index ${item.title}",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .background(if (item.type == "INCOME") RetroIncomeGreen else RetroYellow, RectangleShape)
                                .border(1.dp, Color.Black, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.type,
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }

                    Text(
                        text = "${item.date} • ${item.category} • $accountName",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280)
                        )
                    )

                    if (!item.isValid && item.errorMessage != null) {
                        Text(
                            text = "Peringatan: ${item.errorMessage}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color(0xFFDC2626)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Rp $formattedAmount",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = if (item.type == "INCOME") Color(0xFF16A34A) else Color(0xFFDC2626)
                )
            )
        }
    }
}
