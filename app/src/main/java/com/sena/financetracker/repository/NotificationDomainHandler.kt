package com.sena.financetracker.repository

import com.sena.financetracker.data.NotificationDao
import com.sena.financetracker.data.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain handler untuk operasi notifikasi sistem.
 */
class NotificationDomainHandler(
    private val notificationDao: NotificationDao
) {
    fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()

    fun getUnreadNotificationCount(): Flow<Int> = notificationDao.getUnreadCount()

    suspend fun markNotificationAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotification(id)
    }

    suspend fun insertNotification(notification: NotificationEntity): Long {
        return notificationDao.insertNotification(notification)
    }
}
