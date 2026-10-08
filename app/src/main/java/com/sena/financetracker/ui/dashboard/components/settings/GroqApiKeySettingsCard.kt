package com.sena.financetracker.ui.dashboard.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sena.financetracker.security.ApiKeyStorage
import com.sena.financetracker.ui.components.NeobrutalBadge
import com.sena.financetracker.ui.components.NeobrutalButton
import com.sena.financetracker.ui.components.NeobrutalCard
import com.sena.financetracker.ui.components.NeobrutalInputField
import com.sena.financetracker.ui.components.RetroExpenseRed
import com.sena.financetracker.ui.components.RetroIncomeGreen
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.components.NeobrutalConfirmDialog

/**
 * Komponen kartu pengaturan Neobrutalism untuk konfigurasi Groq API Key (Pak Hemat AI).
 *
 * Fitur:
 * 1. Menampilkan status integrasi AI: Mode Cloud Aktif (Llama-3/Groq) vs Mode Fallback Lokal (Offline).
 * 2. Input/paste API Key dengan toggle visibilitas (masking password) yang aman.
 * 3. Menampilkan preview masked key (contoh: `gsk_••••••••xxxx`) tanpa mengekspos kredensial mentah.
 * 4. Menyimpan key ke secure hardware Keystore via [ApiKeyStorage.setGroqApiKey].
 * 5. Menghapus key dengan konfirmasi [NeobrutalConfirmDialog] sesuai standar keamanan aplikasi.
 *
 * @param hasApiKey Menandakan apakah API Key saat ini aktif/tersimpan di sistem.
 * @param onSaveApiKey Callback saat pengguna menekan tombol simpan dengan input API Key valid.
 * @param onClearApiKey Callback saat pengguna mengonfirmasi penghapusan API Key.
 * @param modifier Modifier penyesuaian layout Compose.
 */
@Composable
fun GroqApiKeySettingsCard(
    hasApiKey: Boolean,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    val maskedKey = remember(hasApiKey) {
        ApiKeyStorage.getMaskedGroqApiKey(context)
    }

    NeobrutalCard(
        backgroundColor = Color.White,
        borderWidth = 2.dp,
        shadowOffset = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header: Ikon + Judul + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (hasApiKey) RetroIncomeGreen else RetroYellow,
                            RectangleShape
                        )
                        .border(2.dp, Color.Black, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Groq API Key",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GROQ API KEY · AI",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = "Model Llama-3 / Groq Cloud untuk analisis finansial",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasApiKey) {
                    NeobrutalBadge(
                        text = "AI CLOUD AKTIF",
                        backgroundColor = RetroIncomeGreen,
                        textColor = Color.Black
                    )
                } else {
                    NeobrutalBadge(
                        text = "MODE FALLBACK LOKAL",
                        backgroundColor = Color(0xFFE5E7EB),
                        textColor = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Status Description Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (hasApiKey) Color(0xFFF0FDF4) else Color(0xFFF9FAFB),
                        RectangleShape
                    )
                    .border(1.5.dp, Color.Black, RectangleShape)
                    .padding(10.dp)
            ) {
                Column {
                    if (hasApiKey) {
                        Text(
                            text = "API Key tersimpan aman di Android Keystore (AES-256-GCM).",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF15803D)
                            )
                        )
                        if (maskedKey.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Key Aktif: $maskedKey",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Pak Hemat berjalan dalam mode generator lokal deterministik offline tanpa kuota cloud.",
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Input Field
            NeobrutalInputField(
                value = keyInput,
                onValueChange = {
                    keyInput = it
                    saveSuccessMessage = null
                },
                label = if (hasApiKey) "GANTI DENGAN KEY BARU" else "INPUT GROQ API KEY",
                placeholder = "gsk_...",
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(
                        onClick = { isKeyVisible = !isKeyVisible },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isKeyVisible) "Sembunyikan Key" else "Tampilkan Key",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Dapatkan API Key gratis di console.groq.com. Key disimpan terenkripsi di hardware HP.",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            )

            // Feedback Pesan Sukses
            if (saveSuccessMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFDCFCE7), RectangleShape)
                        .border(1.dp, Color(0xFF16A34A), RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sukses",
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = saveSuccessMessage ?: "",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF15803D)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Tombol Aksi: SIMPAN & HAPUS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeobrutalButton(
                    onClick = {
                        val cleanKey = keyInput.trim()
                        if (cleanKey.isNotBlank()) {
                            onSaveApiKey(cleanKey)
                            keyInput = ""
                            saveSuccessMessage = "API Key berhasil disimpan dengan aman!"
                        }
                    },
                    backgroundColor = RetroYellow,
                    modifier = if (hasApiKey) Modifier.weight(1f) else Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SIMPAN KEY",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    )
                }

                if (hasApiKey) {
                    Spacer(modifier = Modifier.width(8.dp))
                    NeobrutalButton(
                        onClick = {
                            showConfirmClearDialog = true
                        },
                        backgroundColor = RetroExpenseRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "HAPUS KEY",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Hapus Key
    if (showConfirmClearDialog) {
        NeobrutalConfirmDialog(
            title = "HAPUS API KEY?",
            message = "Apakah kamu yakin ingin menghapus Groq API Key dari penyimpanan terenkripsi? Pak Hemat akan beralih ke mode fallback lokal.",
            confirmButtonText = "HAPUS KEY",
            cancelButtonText = "BATAL",
            onConfirm = {
                onClearApiKey()
                saveSuccessMessage = null
                showConfirmClearDialog = false
            },
            onDismiss = {
                showConfirmClearDialog = false
            }
        )
    }
}
