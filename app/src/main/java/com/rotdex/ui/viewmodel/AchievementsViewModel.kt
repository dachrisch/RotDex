package com.rotdex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.manager.AchievementManager
import com.rotdex.data.models.Achievement
import com.rotdex.data.models.AchievementProgress
import com.rotdex.data.models.AchievementType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the achievements screen
 */
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementManager: AchievementManager
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<AchievementType?>(null)
    val selectedFilter: StateFlow<AchievementType?> = _selectedFilter.asStateFlow()

    /**
     * All achievements with their progress
     * Reactive flow that updates when progress changes
     */
    val achievementsWithProgress: StateFlow<List<AchievementWithProgress>> =
        achievementManager.getAllProgress().map { progressList ->
            progressList.mapNotNull { progress ->
                val achievement = com.rotdex.data.models.Achievements.ALL_ACHIEVEMENTS
                    .find { it.id == progress.achievementId }
                achievement?.let {
                    AchievementWithProgress(
                        achievement = it,
                        progress = progress
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Filtered achievements based on selected type
     */
    val filteredAchievements: StateFlow<List<AchievementWithProgress>> =
        achievementsWithProgress.map { list ->
            val filter = _selectedFilter.value
            if (filter != null) {
                list.filter { it.achievement.type == filter }
            } else {
                list
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Achievement statistics
     */
    val stats: StateFlow<AchievementStats> = achievementsWithProgress.map { list ->
        val unlocked = list.count { it.progress.isUnlocked }
        val total = list.size
        val progressPercentage = if (total > 0) (unlocked.toFloat() / total * 100).toInt() else 0

        AchievementStats(
            totalAchievements = total,
            unlockedCount = unlocked,
            progressPercentage = progressPercentage,
            collectionCount = list.count { it.achievement.type == AchievementType.COLLECTION && it.progress.isUnlocked },
            rarityCount = list.count { it.achievement.type == AchievementType.RARITY && it.progress.isUnlocked },
            fusionCount = list.count { it.achievement.type == AchievementType.FUSION && it.progress.isUnlocked },
            generationCount = list.count { it.achievement.type == AchievementType.GENERATION && it.progress.isUnlocked },
            streakCount = list.count { it.achievement.type == AchievementType.STREAK && it.progress.isUnlocked }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AchievementStats(0, 0, 0, 0, 0, 0, 0, 0)
    )

    /**
     * Filter achievements by type
     */
    fun filterByType(type: AchievementType?) {
        _selectedFilter.value = type
    }

    /**
     * Load achievement details with progress
     */
    suspend fun getAchievementDetails(achievementId: String): Pair<Achievement, AchievementProgress>? {
        return achievementManager.getAchievementWithProgress(achievementId)
    }
}

/**
 * Achievement combined with its progress
 */
data class AchievementWithProgress(
    val achievement: Achievement,
    val progress: AchievementProgress
) {
    /**
     * Progress percentage (0-100)
     */
    val progressPercentage: Float
        get() = if (achievement.requirement > 0) {
            (progress.currentProgress.toFloat() / achievement.requirement * 100).coerceIn(0f, 100f)
        } else {
            0f
        }

    /**
     * Is the achievement completed?
     */
    val isCompleted: Boolean
        get() = progress.isUnlocked
}

/**
 * Achievement statistics
 */
data class AchievementStats(
    val totalAchievements: Int,
    val unlockedCount: Int,
    val progressPercentage: Int,
    val collectionCount: Int,
    val rarityCount: Int,
    val fusionCount: Int,
    val generationCount: Int,
    val streakCount: Int
)
