package com.sena.financetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.sena.financetracker.data.FinanceDatabaseHelper
import com.sena.financetracker.repository.TransactionRepository
import com.sena.financetracker.ui.FinanceDashboardScreen
import com.sena.financetracker.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dbHelper = FinanceDatabaseHelper(this)
        val repository = TransactionRepository(dbHelper)
        val viewModel = ViewModelProvider(
            this,
            FinanceViewModel.Factory(repository)
        )[FinanceViewModel::class.java]

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinanceDashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
