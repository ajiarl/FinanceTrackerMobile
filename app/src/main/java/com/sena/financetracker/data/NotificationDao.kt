package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk entitas [NotificationEntity].
 *
 * Menyediakan antarmuka terstruktur untuk operasi baca, tulis, pembaruan status baca,
 * dan pembersihan notifikasi dalam penyimpanan lokal.
 */
interface NotificationDao {
    /**
     * Mengambil seluruh aliran notifikasi secara reaktif, diurutkan dari yang terbaru.
     */
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    /**
     * Mengambil jumlah notifikasi yang belum dibaca secara reaktif.
     */
    fun getUnreadCount(): Flow<Int>

    /**
     * Memasukkan entitas notifikasi baru ke dalam basis data.
     *
     * @param notification Objek notifikasi yang akan disimpan.
     * @return ID unik baris data yang baru disisipkan.
     */
    suspend fun insertNotification(notification: NotificationEntity): Long

    /**
     * Menandai satu notifikasi tertentu sebagai telah dibaca berdasarkan [id].
     */
    suspend fun markAsRead(id: Long)

    /**
     * Menandai seluruh notifikasi yang ada sebagai telah dibaca.
     */
    suspend fun markAllAsRead()

    /**
     * Menghapus seluruh riwayat notifikasi dari basis data.
     */
    suspend fun clearAllNotifications()

    /**
     * Menghapus satu notifikasi tertentu berdasarkan [id].
     */
    suspend fun deleteNotification(id: Long)
}
