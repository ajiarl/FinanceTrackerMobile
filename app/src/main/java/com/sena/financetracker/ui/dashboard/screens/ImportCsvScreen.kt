package com.sena.financetracker.ui.dashboard.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.dashboard.screens.importcsv.CommitBatchButton
import com.sena.financetracker.ui.dashboard.screens.importcsv.CsvAccountMappingSection
import com.sena.financetracker.ui.dashboard.screens.importcsv.CsvFilePickerSection
import com.sena.financetracker.ui.dashboard.screens.importcsv.CsvPreviewHeader
import com.sena.financetracker.ui.dashboard.screens.importcsv.CsvPreviewTransactionCard
import com.sena.financetracker.ui.dashboard.screens.importcsv.ImportCsvHeader
import com.sena.financetracker.util.CsvImporter
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Layar Impor Berkas CSV Transaksi (ImportCsvScreen) bergaya Strict Neobrutalism.
 * Bertindak sebagai orchestrator alur SAF, parsing CsvImporter, dan batch commit.
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

                val fileName = uri.pathSegments?.lastOrNull() ?: "transaksi.csv"
                selectedFileName = fileName

                val parsed = CsvImporter.parseCsv(content)
                parsedList = parsed

                if (parsed.isEmpty()) {
                    Toast.makeText(context, "Berkas kosong atau format tidak sesuai", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Berhasil memuat ${parsed.size} baris data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceTracker", "Gagal membaca berkas CSV dari Uri: $uri", e)
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
        // TOP BAR HEADER
        item {
            ImportCsvHeader(onNavigateBack = onNavigateBack)
        }

        // LANGKAH 1: PILIH BERKAS CSV
        item {
            CsvFilePickerSection(
                selectedFileName = selectedFileName,
                parsedCount = parsedList.size,
                validCount = validTransactions.size,
                invalidCount = invalidCount,
                onPickFileClick = { filePickerLauncher.launch("*/*") }
            )
        }

        // LANGKAH 2: PEMETAAN AKUN PENAMPUNG
        item {
            CsvAccountMappingSection(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onAccountSelected = { selectedAccountId = it }
            )
        }

        // LANGKAH 3: PRATINJAU TABEL NEOBRUTAL
        item {
            CsvPreviewHeader(
                totalCount = parsedList.size,
                previewCount = parsedList.take(10).size,
                isEmpty = parsedList.isEmpty()
            )
        }

        // List Preview Baris Data (Maksimal 10 Baris Pratinjau)
        itemsIndexed(parsedList.take(10)) { index, row ->
            CsvPreviewTransactionCard(
                index = index + 1,
                item = row,
                accountName = selectedAccount?.name ?: "Dompet Tunai"
            )
        }

        // TOMBOL KOMIT EKSEKUSI BATCH
        item {
            if (validTransactions.isNotEmpty()) {
                CommitBatchButton(
                    validCount = validTransactions.size,
                    isImporting = isImporting,
                    onCommit = {
                        if (selectedAccount == null) {
                            Toast.makeText(context, "Pilih akun penampung terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@CommitBatchButton
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
                )
            }
        }
    }
}
