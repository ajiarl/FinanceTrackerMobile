package com.sena.financetracker.data

import kotlinx.coroutines.flow.Flow

interface CategoryDao {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun deleteCategory(id: Long)
}
