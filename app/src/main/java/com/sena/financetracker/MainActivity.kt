package com.sena.financetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import com.sena.financetracker.data.AppDatabase
import com.sena.financetracker.repository.TransactionRepository
import com.sena.financetracker.ui.dashboard.FinanceDashboardScreen
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appDatabase = AppDatabase.getInstance(this)
        val repository = TransactionRepository(appDatabase)
        val viewModel = ViewModelProvider(
            this,
            FinanceViewModel.Factory(repository)
        )[FinanceViewModel::class.java]

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = RetroCanvas
            ) {
                FinanceDashboardScreen(viewModel = viewModel)
            }
        }
    }
}
