package com.sena.financetracker.repository

import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain handler untuk validasi dan manajemen kategori transaksi.
 */
class CategoryDomainHandler(
    private val categoryDao: CategoryDao
) {
    val systemCategoryNames = setOf(
        "Makanan & Minuman",
        "Transportasi",
        "Belanja",
        "Tagihan & Utilitas",
        "Hiburan",
        "Gaji",
        "Freelance",
        "Investasi",
        "Bonus",
        "Lainnya",
        "Transfer",
        "Penyesuaian",
        "Saldo Awal"
    )

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)

    fun isSystemCategory(name: String): Boolean {
        return systemCategoryNames.any { it.equals(name.trim(), ignoreCase = true) }
    }

    suspend fun insertCategory(name: String, type: String, color: String = "#FAFF00"): Long {
        require(name.isNotBlank()) { "Nama kategori tidak boleh kosong" }
        val upperType = type.trim().uppercase()
        require(upperType == "EXPENSE" || upperType == "INCOME") {
            "Tipe kategori harus EXPENSE atau INCOME"
        }
        val entity = CategoryEntity(
            name = name.trim(),
            type = upperType,
            color = if (color.isNotBlank()) color else "#FAFF00"
        )
        return categoryDao.insertCategory(entity)
    }

    suspend fun updateCategory(id: Long, name: String, type: String, color: String): Int {
        require(name.isNotBlank()) { "Nama kategori tidak boleh kosong" }
        val upperType = type.trim().uppercase()
        require(upperType == "EXPENSE" || upperType == "INCOME") {
            "Tipe kategori harus EXPENSE atau INCOME"
        }
        val entity = CategoryEntity(
            id = id,
            name = name.trim(),
            type = upperType,
            color = if (color.isNotBlank()) color else "#FAFF00"
        )
        return categoryDao.updateCategory(entity)
    }

    suspend fun deleteCategory(id: Long): Int {
        val existing = categoryDao.getCategoryById(id)
        if (existing != null && isSystemCategory(existing.name)) {
            throw IllegalStateException("Kategori bawaan sistem '${existing.name}' tidak dapat dihapus!")
        }
        return categoryDao.deleteCategory(id)
    }
}
