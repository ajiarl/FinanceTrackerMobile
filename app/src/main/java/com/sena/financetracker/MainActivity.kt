package com.sena.financetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.sena.financetracker.data.AppDatabase
import com.sena.financetracker.repository.TransactionRepository
import com.sena.financetracker.ui.components.RetroCanvas
import com.sena.financetracker.ui.components.RetroYellow
import com.sena.financetracker.ui.dashboard.FinanceDashboardScreen
import com.sena.financetracker.ui.splash.NeobrutalSplashScreen
import com.sena.financetracker.viewmodel.FinanceViewModel

/**
 * Main Activity aplikasi Finance Tracker.
 * Menginisialisasi komponen database Room, repository, ViewModel,
 * serta menyajikan Neobrutal Splash Screen saat pembukaan app dengan transisi mulus ke Dashboard.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appDatabase = AppDatabase.getInstance(this)
        val prefs = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        val repository = TransactionRepository(appDatabase, prefs)
        val viewModel = ViewModelProvider(
            this,
            FinanceViewModel.Factory(repository, applicationContext)
        )[FinanceViewModel::class.java]

        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (showSplash) RetroYellow else RetroCanvas
            ) {
                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 350)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 300))
                    },
                    label = "SplashScreenTransition"
                ) { isSplash ->
                    if (isSplash) {
                        NeobrutalSplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        FinanceDashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
