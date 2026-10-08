package com.sena.financetracker.data

import android.content.ContentValues
import android.database.Cursor
import com.sena.financetracker.data.DatabaseSchema.COL_ACC_BALANCE
import com.sena.financetracker.data.DatabaseSchema.COL_ACC_ID
import com.sena.financetracker.data.DatabaseSchema.COL_ACC_NAME
import com.sena.financetracker.data.DatabaseSchema.COL_ACC_TYPE
import com.sena.financetracker.data.DatabaseSchema.COL_BUDGET_CATEGORY
import com.sena.financetracker.data.DatabaseSchema.COL_BUDGET_ID
import com.sena.financetracker.data.DatabaseSchema.COL_BUDGET_IS_ACTIVE
import com.sena.financetracker.data.DatabaseSchema.COL_BUDGET_LIMIT
import com.sena.financetracker.data.DatabaseSchema.COL_BUDGET_NAME
import com.sena.financetracker.data.DatabaseSchema.COL_BUDGET_PERIOD
import com.sena.financetracker.data.DatabaseSchema.COL_CAT_COLOR
import com.sena.financetracker.data.DatabaseSchema.COL_CAT_ID
import com.sena.financetracker.data.DatabaseSchema.COL_CAT_NAME
import com.sena.financetracker.data.DatabaseSchema.COL_CAT_TYPE
import com.sena.financetracker.data.DatabaseSchema.COL_NOTIF_CREATED_AT
import com.sena.financetracker.data.DatabaseSchema.COL_NOTIF_ID
import com.sena.financetracker.data.DatabaseSchema.COL_NOTIF_IS_READ
import com.sena.financetracker.data.DatabaseSchema.COL_NOTIF_MESSAGE
import com.sena.financetracker.data.DatabaseSchema.COL_NOTIF_TITLE
import com.sena.financetracker.data.DatabaseSchema.COL_NOTIF_TYPE
import com.sena.financetracker.data.DatabaseSchema.COL_TX_ACCOUNT_ID
import com.sena.financetracker.data.DatabaseSchema.COL_TX_ACCOUNT_NAME
import com.sena.financetracker.data.DatabaseSchema.COL_TX_AMOUNT
import com.sena.financetracker.data.DatabaseSchema.COL_TX_CATEGORY
import com.sena.financetracker.data.DatabaseSchema.COL_TX_DATE
import com.sena.financetracker.data.DatabaseSchema.COL_TX_ID
import com.sena.financetracker.data.DatabaseSchema.COL_TX_NOTES
import com.sena.financetracker.data.DatabaseSchema.COL_TX_TITLE
import com.sena.financetracker.data.DatabaseSchema.COL_TX_TYPE

/**
 * Helper pemetaan Cursor SQLite ke domain entities dan ContentValues.
 */
object DatabaseMappers {

    fun mapTransaction(c: Cursor): TransactionEntity {
        val accIdIdx = c.getColumnIndex(COL_TX_ACCOUNT_ID)
        val accNameIdx = c.getColumnIndex(COL_TX_ACCOUNT_NAME)
        val notesIdx = c.getColumnIndex(COL_TX_NOTES)

        return TransactionEntity(
            id = c.getLong(c.getColumnIndexOrThrow(COL_TX_ID)),
            title = c.getString(c.getColumnIndexOrThrow(COL_TX_TITLE)),
            amount = c.getDouble(c.getColumnIndexOrThrow(COL_TX_AMOUNT)),
            type = c.getString(c.getColumnIndexOrThrow(COL_TX_TYPE)),
            category = c.getString(c.getColumnIndexOrThrow(COL_TX_CATEGORY)),
            date = c.getString(c.getColumnIndexOrThrow(COL_TX_DATE)),
            accountId = if (accIdIdx != -1) c.getLong(accIdIdx) else 1L,
            accountName = if (accNameIdx != -1) c.getString(accNameIdx) else "Dompet Tunai",
            notes = if (notesIdx != -1) c.getString(notesIdx) ?: "" else ""
        )
    }

    fun toContentValues(tx: TransactionEntity): ContentValues = ContentValues().apply {
        put(COL_TX_TITLE, tx.title)
        put(COL_TX_AMOUNT, tx.amount)
        put(COL_TX_TYPE, tx.type)
        put(COL_TX_CATEGORY, tx.category)
        put(COL_TX_DATE, tx.date)
        put(COL_TX_ACCOUNT_ID, tx.accountId)
        put(COL_TX_ACCOUNT_NAME, tx.accountName)
        put(COL_TX_NOTES, tx.notes)
    }

    fun mapAccount(c: Cursor): AccountEntity {
        return AccountEntity(
            id = c.getLong(c.getColumnIndexOrThrow(COL_ACC_ID)),
            name = c.getString(c.getColumnIndexOrThrow(COL_ACC_NAME)),
            type = c.getString(c.getColumnIndexOrThrow(COL_ACC_TYPE)),
            balance = c.getDouble(c.getColumnIndexOrThrow(COL_ACC_BALANCE))
        )
    }

    fun toContentValues(acc: AccountEntity): ContentValues = ContentValues().apply {
        put(COL_ACC_NAME, acc.name)
        put(COL_ACC_TYPE, acc.type)
        put(COL_ACC_BALANCE, acc.balance)
    }

    fun mapCategory(c: Cursor): CategoryEntity {
        return CategoryEntity(
            id = c.getLong(c.getColumnIndexOrThrow(COL_CAT_ID)),
            name = c.getString(c.getColumnIndexOrThrow(COL_CAT_NAME)),
            type = c.getString(c.getColumnIndexOrThrow(COL_CAT_TYPE)),
            color = c.getString(c.getColumnIndexOrThrow(COL_CAT_COLOR))
        )
    }

    fun toContentValues(cat: CategoryEntity): ContentValues = ContentValues().apply {
        put(COL_CAT_NAME, cat.name)
        put(COL_CAT_TYPE, cat.type)
        put(COL_CAT_COLOR, cat.color)
    }

    fun mapBudget(c: Cursor): BudgetEntity {
        return BudgetEntity(
            id = c.getLong(c.getColumnIndexOrThrow(COL_BUDGET_ID)),
            name = c.getString(c.getColumnIndexOrThrow(COL_BUDGET_NAME)),
            category = c.getString(c.getColumnIndexOrThrow(COL_BUDGET_CATEGORY)),
            limitAmount = c.getDouble(c.getColumnIndexOrThrow(COL_BUDGET_LIMIT)),
            period = c.getString(c.getColumnIndexOrThrow(COL_BUDGET_PERIOD)),
            isActive = c.getInt(c.getColumnIndexOrThrow(COL_BUDGET_IS_ACTIVE)) == 1
        )
    }

    fun toContentValues(b: BudgetEntity): ContentValues = ContentValues().apply {
        put(COL_BUDGET_NAME, b.name)
        put(COL_BUDGET_CATEGORY, b.category)
        put(COL_BUDGET_LIMIT, b.limitAmount)
        put(COL_BUDGET_PERIOD, b.period)
        put(COL_BUDGET_IS_ACTIVE, if (b.isActive) 1 else 0)
    }

    fun mapNotification(c: Cursor): NotificationEntity {
        return NotificationEntity(
            id = c.getLong(c.getColumnIndexOrThrow(COL_NOTIF_ID)),
            title = c.getString(c.getColumnIndexOrThrow(COL_NOTIF_TITLE)),
            message = c.getString(c.getColumnIndexOrThrow(COL_NOTIF_MESSAGE)),
            type = c.getString(c.getColumnIndexOrThrow(COL_NOTIF_TYPE)),
            isRead = c.getInt(c.getColumnIndexOrThrow(COL_NOTIF_IS_READ)) == 1,
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_NOTIF_CREATED_AT))
        )
    }

    fun toContentValues(notif: NotificationEntity): ContentValues = ContentValues().apply {
        put(COL_NOTIF_TITLE, notif.title)
        put(COL_NOTIF_MESSAGE, notif.message)
        put(COL_NOTIF_TYPE, notif.type)
        put(COL_NOTIF_IS_READ, if (notif.isRead) 1 else 0)
        put(COL_NOTIF_CREATED_AT, notif.createdAt)
    }
}
