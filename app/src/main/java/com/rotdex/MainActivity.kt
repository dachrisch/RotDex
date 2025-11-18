package com.rotdex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.rotdex.ui.navigation.NavGraph
import com.rotdex.ui.theme.RotDexTheme
import com.rotdex.ui.viewmodel.DailyRewardsViewModel
import com.rotdex.workers.EnergyRegenerationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize user profile and check streak on app start
        lifecycleScope.launch {
            initializeApp()
        }

        // Schedule energy regeneration worker
        scheduleEnergyRegeneration()

        setContent {
            RotDexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    /**
     * Initialize app state on startup
     */
    private suspend fun initializeApp() {
        // User profile initialization and streak check will happen automatically
        // when the DailyRewardsViewModel is created via Hilt
    }

    /**
     * Schedule periodic energy regeneration worker
     */
    private fun scheduleEnergyRegeneration() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val energyWorkRequest = PeriodicWorkRequestBuilder<EnergyRegenerationWorker>(
            repeatInterval = 15, // Run every 15 minutes
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // First run after 1 minute
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "energy_regeneration",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            energyWorkRequest
        )
    }
}
