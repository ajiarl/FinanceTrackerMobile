package com.sena.financetracker.data

/**
 * Entitas lokal tabel `notifications` pada Room/SQLite.
 *
 * Digunakan untuk mencatat riwayat pesan & peringatan sistem dalam aplikasi
 * (contoh: peringatan mendekati batas anggaran atau overbudget alert).
 *
 * @property id Kunci utama unik berpenomoran otomatis (auto-increment).
 * @property title Judul notifikasi yang mencerminkan inti pesan (misal: "PERINGATAN ANGGARAN").
 * @property message Isi pesan detail mengenai peristiwa keuangan yang terjadi.
 * @property type Kategori/tingkat keparahan notifikasi: "WARNING", "DANGER", atau "INFO".
 * @property isRead Status apakah notifikasi sudah dibaca oleh pengguna (true) atau belum (false).
 * @property createdAt Timestamp waktu pencatatan notifikasi dalam milidetik Epoch.
 */
data class NotificationEntity(
    val id: Long = 0L,
    val title: String,
    val message: String,
    val type: String, // "WARNING", "DANGER", "INFO"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
