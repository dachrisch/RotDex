package com.rotdex.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rotdex.data.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for regenerating energy over time
 * Runs every 15 minutes to check if energy should be regenerated
 */
@HiltWorker
class EnergyRegenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userRepository: UserRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Regenerate energy if needed
            val energyAdded = userRepository.regenerateEnergy()

            if (energyAdded > 0) {
                // Energy was regenerated successfully
                Result.success()
            } else {
                // No energy needed to be regenerated (already at max)
                Result.success()
            }
        } catch (e: Exception) {
            // Log error and retry
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "energy_regeneration"
    }
}
