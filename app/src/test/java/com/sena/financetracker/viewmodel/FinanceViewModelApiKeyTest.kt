package com.sena.financetracker.viewmodel

import com.sena.financetracker.data.AccountDao
import com.sena.financetracker.data.AccountEntity
import com.sena.financetracker.data.BudgetDao
import com.sena.financetracker.data.BudgetEntity
import com.sena.financetracker.data.CategoryDao
import com.sena.financetracker.data.CategoryEntity
import com.sena.financetracker.data.NotificationDao
import com.sena.financetracker.data.NotificationEntity
import com.sena.financetracker.data.TransactionDao
import com.sena.financetracker.data.TransactionEntity
import com.sena.financetracker.repository.TransactionRepository
import com.sena.financetracker.security.ApiKeyStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk integrasi manajemen Groq API Key pada [FinanceViewModel].
 * Menguji StateFlow [FinanceViewModel.hasApiKey], penyimpanan aman, dan sinkronisasi status.
 */
class FinanceViewModelApiKeyTest {

    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: FinanceViewModel

    private class FakeTransactionDao : TransactionDao {
        private val _flow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _flow.asStateFlow()
        override suspend fun insertTransaction(transaction: TransactionEntity): Long = 1L
        override suspend fun insertTransactionsBatch(transactions: List<TransactionEntity>): List<Long> = emptyList()
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(id: Long) {}
        override suspend fun getTransactionById(id: Long): TransactionEntity? = null
    }

    private class FakeAccountDao : AccountDao {
        private val _flow = MutableStateFlow<List<AccountEntity>>(emptyList())
        override fun getAllAccounts(): Flow<List<AccountEntity>> = _flow.asStateFlow()
        override suspend fun getAccountById(id: Long): AccountEntity? = null
        override suspend fun insertAccount(account: AccountEntity): Long = 1L
        override suspend fun updateBalance(id: Long, newBalance: Double) {}
        override suspend fun adjustBalance(id: Long, delta: Double) {}
        override suspend fun deleteAccount(id: Long) {}
    }

    private class FakeCategoryDao : CategoryDao {
        private val _flow = MutableStateFlow<List<CategoryEntity>>(emptyList())
        override fun getAllCategories(): Flow<List<CategoryEntity>> = _flow.asStateFlow()
        override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun insertCategory(category: CategoryEntity): Long = 1L
        override suspend fun updateCategory(category: CategoryEntity): Int = 1
        override suspend fun deleteCategory(id: Long): Int = 1
        override suspend fun getCategoryById(id: Long): CategoryEntity? = null
    }

    private class StubBudgetDao : BudgetDao {
        override fun getAllBudgets(): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override suspend fun getBudgetById(id: Long): BudgetEntity? = null
        override suspend fun insertBudget(budget: BudgetEntity): Long = 1L
        override suspend fun updateBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(id: Long) {}
    }

    private class StubNotificationDao : NotificationDao {
        override fun getAllNotifications(): Flow<List<NotificationEntity>> = flowOf(emptyList())
        override fun getUnreadCount(): Flow<Int> = flowOf(0)
        override suspend fun insertNotification(notification: NotificationEntity): Long = 1L
        override suspend fun markAsRead(id: Long) {}
        override suspend fun markAllAsRead() {}
        override suspend fun deleteNotification(id: Long) {}
        override suspend fun clearAllNotifications() {}
    }

    @Before
    fun setup() {
        ApiKeyStorage.setInMemoryApiKey(null)

        repository = TransactionRepository(
            transactionDao = FakeTransactionDao(),
            accountDao = FakeAccountDao(),
            categoryDao = FakeCategoryDao(),
            budgetDao = StubBudgetDao(),
            notificationDao = StubNotificationDao()
        )

        viewModel = FinanceViewModel(
            repository = repository,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @After
    fun tearDown() {
        ApiKeyStorage.setInMemoryApiKey(null)
    }

    @Test
    fun initialState_hasApiKeyIsFalse() {
        assertFalse(viewModel.hasApiKey.value)
    }

    @Test
    fun saveGroqApiKey_updatesHasApiKeyAndStorage() {
        val testKey = "gsk_prod_secure_token_12345678"
        viewModel.saveGroqApiKey(testKey)

        assertTrue(viewModel.hasApiKey.value)
        assertTrue(ApiKeyStorage.hasCustomApiKey())
        assertEquals("gsk_••••••••5678", ApiKeyStorage.getMaskedGroqApiKey())
    }

    @Test
    fun clearGroqApiKey_resetsHasApiKeyAndStorage() {
        viewModel.saveGroqApiKey("gsk_temporary_key_9999")
        assertTrue(viewModel.hasApiKey.value)

        viewModel.clearGroqApiKey()
        assertFalse(viewModel.hasApiKey.value)
        assertFalse(ApiKeyStorage.hasCustomApiKey())
        assertEquals("", ApiKeyStorage.getMaskedGroqApiKey())
    }

    @Test
    fun refreshApiKeyStatus_syncsWithUnderlyingStorage() {
        ApiKeyStorage.setInMemoryApiKey("gsk_external_configured_key")
        viewModel.refreshApiKeyStatus()
        assertTrue(viewModel.hasApiKey.value)

        ApiKeyStorage.setInMemoryApiKey(null)
        viewModel.refreshApiKeyStatus()
        assertFalse(viewModel.hasApiKey.value)
    }

    @Test
    fun saveGroqApiKey_trimsWhitespaceAndStoresCleanKey() {
        viewModel.saveGroqApiKey("   gsk_trimmed_key_value   ")
        assertTrue(viewModel.hasApiKey.value)
        assertEquals("gsk_trimmed_key_value", ApiKeyStorage.getGroqApiKey())
    }

    @Test
    fun saveGroqApiKey_emptyStringSetsHasApiKeyToFalse() {
        viewModel.saveGroqApiKey("   ")
        assertFalse(viewModel.hasApiKey.value)
    }
}
