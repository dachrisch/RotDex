package com.rotdex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.manager.SpinResult
import com.rotdex.data.manager.StreakUpdateResult
import com.rotdex.data.models.SpinReward
import com.rotdex.data.models.StreakMilestone
import com.rotdex.data.models.UserProfile
import com.rotdex.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for daily rewards (spin wheel and streak)
 */
@HiltViewModel
class DailyRewardsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _streakState = MutableStateFlow<StreakState>(StreakState.Loading)
    val streakState: StateFlow<StreakState> = _streakState.asStateFlow()

    private val _spinState = MutableStateFlow<SpinState>(SpinState.Idle)
    val spinState: StateFlow<SpinState> = _spinState.asStateFlow()

    private val _nextMilestone = MutableStateFlow<StreakMilestone?>(null)
    val nextMilestone: StateFlow<StreakMilestone?> = _nextMilestone.asStateFlow()

    private val _lastSpinReward = MutableStateFlow<SpinReward?>(null)
    val lastSpinReward: StateFlow<SpinReward?> = _lastSpinReward.asStateFlow()

    init {
        checkDailyStreak()
        loadNextMilestone()
        loadLastSpinReward()
    }

    /**
     * Load the last spin reward for display
     */
    private fun loadLastSpinReward() {
        viewModelScope.launch {
            _lastSpinReward.value = userRepository.getLastSpinReward()
        }
    }

    /**
     * Check and update the user's daily streak
     */
    fun checkDailyStreak() {
        viewModelScope.launch {
            try {
                _streakState.value = StreakState.Loading

                when (val result = userRepository.updateDailyStreak()) {
                    is StreakUpdateResult.AlreadyLoggedIn -> {
                        _streakState.value = StreakState.AlreadyCheckedIn(result.currentStreak)
                    }
                    is StreakUpdateResult.StreakIncreased -> {
                        _streakState.value = StreakState.StreakIncreased(
                            newStreak = result.newStreak,
                            milestone = result.milestone
                        )
                        loadNextMilestone() // Update next milestone
                    }
                    is StreakUpdateResult.StreakBroken -> {
                        _streakState.value = StreakState.StreakBroken(result.previousStreak)
                    }
                    is StreakUpdateResult.ProtectionAvailable -> {
                        _streakState.value = StreakState.ProtectionOffered(result.currentStreak)
                    }
                }
            } catch (e: Exception) {
                _streakState.value = StreakState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Use a streak protection to save the streak
     */
    fun useStreakProtection() {
        viewModelScope.launch {
            val success = userRepository.useStreakProtection()
            if (success) {
                checkDailyStreak()
            } else {
                _streakState.value = StreakState.Error("No streak protections available")
            }
        }
    }

    /**
     * Decline streak protection and reset to day 1
     */
    fun declineProtection() {
        viewModelScope.launch {
            // Streak is already broken, just update UI
            checkDailyStreak()
        }
    }

    /**
     * Perform a spin on the daily wheel
     */
    fun performSpin() {
        viewModelScope.launch {
            _spinState.value = SpinState.Spinning

            // Simulate spin animation duration
            delay(2500)

            when (val result = userRepository.performDailySpin()) {
                is SpinResult.Success -> {
                    _spinState.value = SpinState.Result(result.reward)
                }
                is SpinResult.AlreadySpunToday -> {
                    _spinState.value = SpinState.AlreadySpun
                }
                is SpinResult.Error -> {
                    _spinState.value = SpinState.Error(result.message)
                }
            }
        }
    }

    /**
     * Reset spin state to idle (after viewing result)
     */
    fun resetSpinState() {
        _spinState.value = SpinState.Idle
    }

    /**
     * Load the next streak milestone
     */
    private fun loadNextMilestone() {
        viewModelScope.launch {
            _nextMilestone.value = userRepository.getNextMilestone()
        }
    }

    /**
     * Get time until next energy refill
     */
    fun getTimeUntilNextEnergy(): Flow<Long> = flow {
        while (true) {
            val time = userRepository.getTimeUntilNextEnergy()
            emit(time)
            delay(1000) // Update every second
        }
    }
}

/**
 * UI state for streak system
 */
sealed class StreakState {
    object Loading : StreakState()
    data class AlreadyCheckedIn(val streak: Int) : StreakState()
    data class StreakIncreased(val newStreak: Int, val milestone: StreakMilestone?) : StreakState()
    data class StreakBroken(val previousStreak: Int) : StreakState()
    data class ProtectionOffered(val currentStreak: Int) : StreakState()
    data class Error(val message: String) : StreakState()
}

/**
 * UI state for spin wheel
 */
sealed class SpinState {
    object Idle : SpinState()
    object Spinning : SpinState()
    data class Result(val reward: SpinReward) : SpinState()
    object AlreadySpun : SpinState()
    data class Error(val message: String) : SpinState()
}
