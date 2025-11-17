# RotDex Gameplay Mechanics Implementation Plan

## Overview
This document outlines the detailed implementation plan for two core engagement features:
1. **Daily Spin & Streak System** - Reward daily engagement with progressive rewards
2. **Card Upgrade/Fusion System** - Combine cards by chance to create better cards

---

# Part 1: Daily Spin & Streak System 🎡

## 1.1 Feature Overview

### Core Concept
Reward players for consistent daily engagement through:
- **Daily Spin Wheel**: 1 free spin per day with randomized rewards
- **Login Streaks**: Consecutive day bonuses that increase in value
- **Streak Milestones**: Special rewards at 7, 14, 30 days
- **Streak Protection**: Optional gem spending to save broken streaks

### Engagement Goals
- Increase daily active users (DAU)
- Build habit formation (14-30 day streaks)
- Create FOMO for streak breaking
- Provide tangible progression rewards

---

## 1.2 Data Models

### 1.2.1 UserProfile Entity
```kotlin
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val userId: String = "default_user", // Single user for now

    // Energy System
    val currentEnergy: Int = 5,
    val maxEnergy: Int = 5,
    val lastEnergyRefresh: Long = System.currentTimeMillis(),

    // Currency
    val brainrotCoins: Int = 0,
    val gems: Int = 0,

    // Streak System
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastLoginDate: String = "", // Format: "yyyy-MM-dd"
    val lastSpinDate: String = "", // Format: "yyyy-MM-dd"
    val hasUsedSpinToday: Boolean = false,

    // Streak Protection
    val streakProtections: Int = 0, // Number of protections available

    // Statistics
    val totalSpins: Int = 0,
    val totalLoginDays: Int = 0,
    val accountCreatedAt: Long = System.currentTimeMillis()
)
```

### 1.2.2 SpinReward Model
```kotlin
enum class SpinRewardType {
    ENERGY,           // +1-3 Energy
    COINS,            // +50-500 Brainrot Coins
    GEMS,             // +1-10 Gems
    FREE_PACK,        // 1 Free Card Pack
    RARITY_BOOST,     // Next generation has +20% Legendary chance
    STREAK_PROTECTION, // +1 Streak Protection
    JACKPOT           // 1000 coins + 20 gems
}

data class SpinReward(
    val type: SpinRewardType,
    val amount: Int = 1,
    val displayName: String,
    val description: String,
    val weight: Float // Probability weight
)
```

### 1.2.3 SpinHistory Entity
```kotlin
@Entity(tableName = "spin_history")
data class SpinHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rewardType: SpinRewardType,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val streakDayAtSpin: Int
)
```

### 1.2.4 StreakMilestone Model
```kotlin
data class StreakMilestone(
    val day: Int,
    val rewardType: StreakRewardType,
    val amount: Int,
    val displayName: String,
    val description: String
)

enum class StreakRewardType {
    COINS,
    GEMS,
    ENERGY,
    FREE_GENERATION,
    RARE_PACK,
    EPIC_PACK,
    LEGENDARY_PACK,
    CUSTOM_LEGENDARY, // User picks prompt for guaranteed Legendary
    STREAK_PROTECTION
}
```

---

## 1.3 Database Changes

### 1.3.1 New DAOs

#### UserProfileDao
```kotlin
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun getUserProfile(userId: String = "default_user"): Flow<UserProfile>

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun getUserProfileOnce(userId: String = "default_user"): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    // Energy operations
    @Query("UPDATE user_profile SET currentEnergy = :energy, lastEnergyRefresh = :timestamp WHERE userId = :userId")
    suspend fun updateEnergy(energy: Int, timestamp: Long, userId: String = "default_user")

    // Currency operations
    @Query("UPDATE user_profile SET brainrotCoins = brainrotCoins + :amount WHERE userId = :userId")
    suspend fun addCoins(amount: Int, userId: String = "default_user")

    @Query("UPDATE user_profile SET gems = gems + :amount WHERE userId = :userId")
    suspend fun addGems(amount: Int, userId: String = "default_user")

    // Streak operations
    @Query("UPDATE user_profile SET currentStreak = :streak, longestStreak = :longest, lastLoginDate = :date WHERE userId = :userId")
    suspend fun updateStreak(streak: Int, longest: Int, date: String, userId: String = "default_user")

    @Query("UPDATE user_profile SET hasUsedSpinToday = :used, lastSpinDate = :date WHERE userId = :userId")
    suspend fun updateSpinStatus(used: Boolean, date: String, userId: String = "default_user")

    @Query("UPDATE user_profile SET streakProtections = streakProtections + :amount WHERE userId = :userId")
    suspend fun addStreakProtections(amount: Int, userId: String = "default_user")
}
```

#### SpinHistoryDao
```kotlin
@Dao
interface SpinHistoryDao {
    @Query("SELECT * FROM spin_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSpins(limit: Int = 50): Flow<List<SpinHistory>>

    @Insert
    suspend fun insertSpin(spin: SpinHistory)

    @Query("SELECT COUNT(*) FROM spin_history WHERE rewardType = :type")
    suspend fun getCountByRewardType(type: SpinRewardType): Int

    @Query("DELETE FROM spin_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldHistory(cutoffTime: Long)
}
```

### 1.3.2 Update CardDatabase
```kotlin
@Database(
    entities = [Card::class, UserProfile::class, SpinHistory::class],
    version = 2, // Increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CardDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun spinHistoryDao(): SpinHistoryDao
}
```

### 1.3.3 Update Converters
```kotlin
class Converters {
    // Existing converters...

    @TypeConverter
    fun fromSpinRewardType(value: SpinRewardType): String = value.name

    @TypeConverter
    fun toSpinRewardType(value: String): SpinRewardType = SpinRewardType.valueOf(value)
}
```

---

## 1.4 Business Logic

### 1.4.1 StreakManager
```kotlin
class StreakManager(
    private val userProfileDao: UserProfileDao,
    private val context: Context
) {
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }

    suspend fun checkAndUpdateStreak(): StreakUpdateResult {
        val profile = userProfileDao.getUserProfileOnce() ?: createDefaultProfile()
        val today = getTodayDate()
        val lastLogin = profile.lastLoginDate

        return when {
            lastLogin == today -> {
                // Already logged in today
                StreakUpdateResult.AlreadyLoggedIn(profile.currentStreak)
            }
            isConsecutiveDay(lastLogin, today) -> {
                // Streak continues
                val newStreak = profile.currentStreak + 1
                val newLongest = maxOf(newStreak, profile.longestStreak)

                userProfileDao.updateStreak(newStreak, newLongest, today)

                // Check for milestone rewards
                val milestone = getStreakMilestone(newStreak)
                StreakUpdateResult.StreakIncreased(newStreak, milestone)
            }
            else -> {
                // Streak broken - check for protection
                if (profile.streakProtections > 0) {
                    StreakUpdateResult.ProtectionAvailable(profile.currentStreak)
                } else {
                    userProfileDao.updateStreak(1, profile.longestStreak, today)
                    StreakUpdateResult.StreakBroken(profile.currentStreak)
                }
            }
        }
    }

    suspend fun useStreakProtection(): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.streakProtections <= 0) return false

        val today = getTodayDate()
        userProfileDao.updateProfile(
            profile.copy(
                streakProtections = profile.streakProtections - 1,
                lastLoginDate = today
            )
        )
        return true
    }

    private fun isConsecutiveDay(lastDate: String, currentDate: String): Boolean {
        if (lastDate.isEmpty()) return false

        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
        val last = formatter.parse(lastDate) ?: return false
        val current = formatter.parse(currentDate) ?: return false

        val diffInMillis = current.time - last.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return diffInDays == 1L
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
    }

    private fun getStreakMilestone(day: Int): StreakMilestone? {
        return STREAK_MILESTONES.find { it.day == day }
    }
}

sealed class StreakUpdateResult {
    data class AlreadyLoggedIn(val currentStreak: Int) : StreakUpdateResult()
    data class StreakIncreased(val newStreak: Int, val milestone: StreakMilestone?) : StreakUpdateResult()
    data class StreakBroken(val previousStreak: Int) : StreakUpdateResult()
    data class ProtectionAvailable(val currentStreak: Int) : StreakUpdateResult()
}
```

### 1.4.2 SpinWheelManager
```kotlin
class SpinWheelManager(
    private val userProfileDao: UserProfileDao,
    private val spinHistoryDao: SpinHistoryDao
) {
    suspend fun canSpinToday(): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        val today = getTodayDate()
        return profile.lastSpinDate != today || !profile.hasUsedSpinToday
    }

    suspend fun performSpin(): SpinResult {
        if (!canSpinToday()) {
            return SpinResult.AlreadySpunToday
        }

        val profile = userProfileDao.getUserProfileOnce() ?: return SpinResult.Error("Profile not found")

        // Weighted random selection
        val reward = selectRandomReward(profile.currentStreak)

        // Apply reward
        applyReward(reward, profile)

        // Record spin
        val today = getTodayDate()
        userProfileDao.updateSpinStatus(true, today)
        spinHistoryDao.insertSpin(
            SpinHistory(
                rewardType = reward.type,
                amount = reward.amount,
                streakDayAtSpin = profile.currentStreak
            )
        )

        return SpinResult.Success(reward)
    }

    private fun selectRandomReward(streakDay: Int): SpinReward {
        // Bonus weights for higher streaks
        val streakBonus = (streakDay / 7) * 0.1f // +10% better rewards per week

        val rewards = SPIN_REWARDS.map {
            it.copy(weight = it.weight * (1f + streakBonus))
        }

        val totalWeight = rewards.sumOf { it.weight.toDouble() }.toFloat()
        val random = Random.nextFloat() * totalWeight

        var currentWeight = 0f
        for (reward in rewards) {
            currentWeight += reward.weight
            if (random <= currentWeight) {
                return reward
            }
        }

        return rewards.last() // Fallback
    }

    private suspend fun applyReward(reward: SpinReward, profile: UserProfile) {
        when (reward.type) {
            SpinRewardType.ENERGY -> {
                val newEnergy = minOf(profile.currentEnergy + reward.amount, profile.maxEnergy)
                userProfileDao.updateEnergy(newEnergy, System.currentTimeMillis())
            }
            SpinRewardType.COINS -> {
                userProfileDao.addCoins(reward.amount)
            }
            SpinRewardType.GEMS -> {
                userProfileDao.addGems(reward.amount)
            }
            SpinRewardType.STREAK_PROTECTION -> {
                userProfileDao.addStreakProtections(reward.amount)
            }
            SpinRewardType.JACKPOT -> {
                userProfileDao.addCoins(1000)
                userProfileDao.addGems(20)
            }
            // Other types handled in UI layer or repository
            else -> { }
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

sealed class SpinResult {
    data class Success(val reward: SpinReward) : SpinResult()
    object AlreadySpunToday : SpinResult()
    data class Error(val message: String) : SpinResult()
}
```

### 1.4.3 Reward Configuration
```kotlin
object RewardConfig {
    val SPIN_REWARDS = listOf(
        SpinReward(
            type = SpinRewardType.ENERGY,
            amount = 1,
            displayName = "+1 Energy",
            description = "Instant energy refill",
            weight = 30f
        ),
        SpinReward(
            type = SpinRewardType.ENERGY,
            amount = 3,
            displayName = "+3 Energy",
            description = "Major energy boost!",
            weight = 10f
        ),
        SpinReward(
            type = SpinRewardType.COINS,
            amount = 50,
            displayName = "50 Coins",
            description = "Some pocket change",
            weight = 25f
        ),
        SpinReward(
            type = SpinRewardType.COINS,
            amount = 200,
            displayName = "200 Coins",
            description = "Nice chunk of coins!",
            weight = 15f
        ),
        SpinReward(
            type = SpinRewardType.COINS,
            amount = 500,
            displayName = "500 Coins",
            description = "Big money!",
            weight = 5f
        ),
        SpinReward(
            type = SpinRewardType.GEMS,
            amount = 1,
            displayName = "+1 Gem",
            description = "Premium currency",
            weight = 10f
        ),
        SpinReward(
            type = SpinRewardType.GEMS,
            amount = 5,
            displayName = "+5 Gems",
            description = "Rare premium drop!",
            weight = 3f
        ),
        SpinReward(
            type = SpinRewardType.STREAK_PROTECTION,
            amount = 1,
            displayName = "Streak Shield",
            description = "Protect your streak once",
            weight = 8f
        ),
        SpinReward(
            type = SpinRewardType.RARITY_BOOST,
            amount = 1,
            displayName = "Legendary Luck",
            description = "+20% Legendary chance next gen",
            weight = 7f
        ),
        SpinReward(
            type = SpinRewardType.FREE_PACK,
            amount = 1,
            displayName = "Free Card Pack",
            description = "3 random cards!",
            weight = 12f
        ),
        SpinReward(
            type = SpinRewardType.JACKPOT,
            amount = 1,
            displayName = "JACKPOT!",
            description = "1000 coins + 20 gems!",
            weight = 1f
        )
    )

    val STREAK_MILESTONES = listOf(
        // Early milestones (Day 1-7)
        StreakMilestone(
            day = 1,
            rewardType = StreakRewardType.ENERGY,
            amount = 2,
            displayName = "First Day!",
            description = "+2 Energy"
        ),
        StreakMilestone(
            day = 3,
            rewardType = StreakRewardType.COINS,
            amount = 100,
            displayName = "3 Day Streak",
            description = "+100 Coins"
        ),
        StreakMilestone(
            day = 7,
            rewardType = StreakRewardType.RARE_PACK,
            amount = 1,
            displayName = "Week Warrior",
            description = "Free Rare Pack (3 cards, 1 guaranteed Rare+)"
        ),

        // Mid milestones (Day 8-21)
        StreakMilestone(
            day = 14,
            rewardType = StreakRewardType.EPIC_PACK,
            amount = 1,
            displayName = "Two Weeks Strong",
            description = "Free Epic Pack (5 cards, 1 guaranteed Epic+)"
        ),
        StreakMilestone(
            day = 21,
            rewardType = StreakRewardType.STREAK_PROTECTION,
            amount = 2,
            displayName = "Triple Week",
            description = "+2 Streak Protections"
        ),

        // Major milestones (Day 30+)
        StreakMilestone(
            day = 30,
            rewardType = StreakRewardType.CUSTOM_LEGENDARY,
            amount = 1,
            displayName = "Month Master",
            description = "Create your own Legendary card!"
        ),
        StreakMilestone(
            day = 60,
            rewardType = StreakRewardType.LEGENDARY_PACK,
            amount = 1,
            displayName = "Two Month Titan",
            description = "Legendary Pack (10 cards, 1 guaranteed Legendary)"
        ),
        StreakMilestone(
            day = 100,
            rewardType = StreakRewardType.CUSTOM_LEGENDARY,
            amount = 3,
            displayName = "Century Club",
            description = "3 Custom Legendary cards + Special badge"
        )
    )
}
```

---

## 1.5 Repository Layer

### 1.5.1 UserRepository
```kotlin
class UserRepository(
    private val userProfileDao: UserProfileDao,
    private val spinHistoryDao: SpinHistoryDao,
    context: Context
) {
    private val streakManager = StreakManager(userProfileDao, context)
    private val spinWheelManager = SpinWheelManager(userProfileDao, spinHistoryDao)

    val userProfile: Flow<UserProfile> = userProfileDao.getUserProfile()

    suspend fun initializeUser() {
        val existing = userProfileDao.getUserProfileOnce()
        if (existing == null) {
            userProfileDao.insertProfile(
                UserProfile(
                    currentEnergy = 5,
                    brainrotCoins = 100, // Starting bonus
                    gems = 5 // Starting gems
                )
            )
        }
    }

    // Streak operations
    suspend fun updateDailyStreak() = streakManager.checkAndUpdateStreak()
    suspend fun useStreakProtection() = streakManager.useStreakProtection()

    // Spin operations
    suspend fun canSpinToday() = spinWheelManager.canSpinToday()
    suspend fun performDailySpin() = spinWheelManager.performSpin()

    // Energy operations
    suspend fun regenerateEnergy() {
        val profile = userProfileDao.getUserProfileOnce() ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - profile.lastEnergyRefresh
        val hoursElapsed = TimeUnit.MILLISECONDS.toHours(elapsed)

        if (hoursElapsed >= 4) {
            val energyToAdd = (hoursElapsed / 4).toInt()
            val newEnergy = minOf(profile.currentEnergy + energyToAdd, profile.maxEnergy)
            userProfileDao.updateEnergy(newEnergy, now)
        }
    }

    suspend fun spendEnergy(amount: Int): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.currentEnergy < amount) return false

        userProfileDao.updateEnergy(
            profile.currentEnergy - amount,
            System.currentTimeMillis()
        )
        return true
    }

    // Currency operations
    suspend fun spendCoins(amount: Int): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.brainrotCoins < amount) return false

        userProfileDao.addCoins(-amount)
        return true
    }

    suspend fun spendGems(amount: Int): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.gems < amount) return false

        userProfileDao.addGems(-amount)
        return true
    }
}
```

---

## 1.6 ViewModel Layer

### 1.6.1 DailyRewardsViewModel
```kotlin
class DailyRewardsViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val userProfile = userRepository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _streakState = MutableStateFlow<StreakState>(StreakState.Loading)
    val streakState: StateFlow<StreakState> = _streakState.asStateFlow()

    private val _spinState = MutableStateFlow<SpinState>(SpinState.Idle)
    val spinState: StateFlow<SpinState> = _spinState.asStateFlow()

    init {
        checkDailyStreak()
    }

    fun checkDailyStreak() {
        viewModelScope.launch {
            try {
                when (val result = userRepository.updateDailyStreak()) {
                    is StreakUpdateResult.AlreadyLoggedIn -> {
                        _streakState.value = StreakState.AlreadyCheckedIn(result.currentStreak)
                    }
                    is StreakUpdateResult.StreakIncreased -> {
                        _streakState.value = StreakState.StreakIncreased(
                            newStreak = result.newStreak,
                            milestone = result.milestone
                        )
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

    fun useStreakProtection() {
        viewModelScope.launch {
            val success = userRepository.useStreakProtection()
            if (success) {
                checkDailyStreak()
            }
        }
    }

    fun performSpin() {
        viewModelScope.launch {
            _spinState.value = SpinState.Spinning

            // Simulate spin animation duration
            delay(2000)

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

    fun resetSpinState() {
        _spinState.value = SpinState.Idle
    }
}

sealed class StreakState {
    object Loading : StreakState()
    data class AlreadyCheckedIn(val streak: Int) : StreakState()
    data class StreakIncreased(val newStreak: Int, val milestone: StreakMilestone?) : StreakState()
    data class StreakBroken(val previousStreak: Int) : StreakState()
    data class ProtectionOffered(val currentStreak: Int) : StreakState()
    data class Error(val message: String) : StreakState()
}

sealed class SpinState {
    object Idle : SpinState()
    object Spinning : SpinState()
    data class Result(val reward: SpinReward) : SpinState()
    object AlreadySpun : SpinState()
    data class Error(val message: String) : SpinState()
}
```

---

## 1.7 UI Screens

### 1.7.1 DailyRewardsScreen
```kotlin
@Composable
fun DailyRewardsScreen(
    viewModel: DailyRewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val streakState by viewModel.streakState.collectAsState()
    val spinState by viewModel.spinState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Daily Rewards",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Streak Display
        StreakCard(
            streakState = streakState,
            currentStreak = userProfile?.currentStreak ?: 0,
            longestStreak = userProfile?.longestStreak ?: 0,
            onUseProtection = { viewModel.useStreakProtection() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Spin Wheel
        SpinWheelCard(
            spinState = spinState,
            canSpin = userProfile?.let { !it.hasUsedSpinToday } ?: false,
            onSpin = { viewModel.performSpin() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Streak Milestones
        StreakMilestonesSection(
            currentStreak = userProfile?.currentStreak ?: 0
        )
    }
}
```

### 1.7.2 SpinWheelCard Component
```kotlin
@Composable
fun SpinWheelCard(
    spinState: SpinState,
    canSpin: Boolean,
    onSpin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (spinState) {
                is SpinState.Idle, is SpinState.AlreadySpun -> {
                    SpinWheelDisplay()

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSpin,
                        enabled = canSpin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (canSpin) "SPIN NOW!" else "Come back tomorrow!",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                is SpinState.Spinning -> {
                    SpinningAnimation()
                }

                is SpinState.Result -> {
                    RewardDisplay(reward = spinState.reward)
                }

                is SpinState.Error -> {
                    Text(
                        text = "Error: ${spinState.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SpinWheelDisplay() {
    // Animated wheel with reward segments
    val rotation = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .size(250.dp)
            .rotate(rotation.value),
        contentAlignment = Alignment.Center
    ) {
        // Draw wheel segments
        Canvas(modifier = Modifier.fillMaxSize()) {
            val segmentAngle = 360f / SPIN_REWARDS.size
            SPIN_REWARDS.forEachIndexed { index, reward ->
                drawArc(
                    color = getColorForReward(reward.type),
                    startAngle = index * segmentAngle,
                    sweepAngle = segmentAngle,
                    useCenter = true
                )
            }
        }

        // Center indicator
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Spin indicator",
            modifier = Modifier.size(48.dp),
            tint = Color.Yellow
        )
    }
}

@Composable
fun SpinningAnimation() {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 1800f, // 5 full rotations
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .size(250.dp)
            .rotate(rotation.value)
    ) {
        SpinWheelDisplay()
    }

    Text(
        text = "Spinning...",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp)
    )
}
```

### 1.7.3 StreakCard Component
```kotlin
@Composable
fun StreakCard(
    streakState: StreakState,
    currentStreak: Int,
    longestStreak: Int,
    onUseProtection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakStatColumn("Current", currentStreak)
                StreakStatColumn("Best", longestStreak)
            }

            when (streakState) {
                is StreakState.StreakIncreased -> {
                    if (streakState.milestone != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MilestoneReward(streakState.milestone)
                    }
                }
                is StreakState.StreakBroken -> {
                    Text(
                        text = "Streak broken! Start fresh today.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is StreakState.ProtectionOffered -> {
                    Button(onClick = onUseProtection) {
                        Text("Use Streak Protection")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun StreakStatColumn(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
```

---

# Part 2: Card Upgrade/Fusion System ⚗️

## 2.1 Feature Overview

### Core Concept
Allow players to combine duplicate or unwanted cards to create better cards through:
- **Fusion System**: Combine 2-5 cards of same rarity → chance for higher rarity
- **Success Rates**: Based on rarity and number of cards used
- **Fusion Recipes**: Special combinations guarantee specific outcomes
- **Fusion History**: Track all fusion attempts and results

### Engagement Goals
- Give value to duplicate cards
- Create strategic decision-making
- Add progression system beyond generation
- Encourage collection completion
- Risk/reward gambling mechanics

---

## 2.2 Data Models

### 2.2.1 FusionRecipe Model
```kotlin
data class FusionRecipe(
    val id: String,
    val name: String,
    val description: String,
    val requiredCards: List<FusionCardRequirement>,
    val guaranteedRarity: CardRarity,
    val guaranteedTags: List<String> = emptyList(),
    val isSecret: Boolean = false // Discovered through experimentation
)

data class FusionCardRequirement(
    val rarity: CardRarity,
    val count: Int,
    val tagRequired: String? = null // Optional tag requirement
)
```

### 2.2.2 FusionHistory Entity
```kotlin
@Entity(tableName = "fusion_history")
data class FusionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputCardIds: List<Long>, // IDs of cards used
    val inputRarities: List<CardRarity>,
    val resultCardId: Long,
    val resultRarity: CardRarity,
    val fusionType: FusionType,
    val wasSuccessful: Boolean, // True if rarity upgraded
    val recipeUsed: String? = null, // Recipe ID if special recipe
    val timestamp: Long = System.currentTimeMillis()
)

enum class FusionType {
    STANDARD,        // Normal fusion
    RECIPE,          // Special recipe
    SUPER_FUSION     // 5+ cards
}
```

### 2.2.3 FusionResult Model
```kotlin
data class FusionResult(
    val success: Boolean,
    val resultCard: Card,
    val rarityUpgraded: Boolean,
    val bonusApplied: String? = null,
    val recipeDiscovered: FusionRecipe? = null
)
```

---

## 2.3 Fusion Algorithm

### 2.3.1 Success Rate Calculation
```kotlin
object FusionRules {
    // Base success rates for rarity upgrade
    private val BASE_SUCCESS_RATES = mapOf(
        CardRarity.COMMON to 0.30f,      // 30% → Rare
        CardRarity.RARE to 0.20f,        // 20% → Epic
        CardRarity.EPIC to 0.10f,        // 10% → Legendary
        CardRarity.LEGENDARY to 0.0f     // Cannot upgrade Legendary
    )

    // Bonus per additional card (after 2 cards minimum)
    private const val BONUS_PER_CARD = 0.05f // +5% per card

    // Maximum cards that can be fused
    private const val MAX_FUSION_CARDS = 5

    fun calculateSuccessRate(
        cards: List<Card>,
        recipeBonus: Float = 0f
    ): Float {
        if (cards.isEmpty()) return 0f

        // Must be same rarity
        val rarity = cards.first().rarity
        if (!cards.all { it.rarity == rarity }) return 0f

        val baseRate = BASE_SUCCESS_RATES[rarity] ?: 0f
        val cardCountBonus = maxOf(0, cards.size - 2) * BONUS_PER_CARD

        return minOf(1f, baseRate + cardCountBonus + recipeBonus)
    }

    fun getNextRarity(current: CardRarity): CardRarity {
        return when (current) {
            CardRarity.COMMON -> CardRarity.RARE
            CardRarity.RARE -> CardRarity.EPIC
            CardRarity.EPIC -> CardRarity.LEGENDARY
            CardRarity.LEGENDARY -> CardRarity.LEGENDARY
        }
    }

    fun canFuse(cards: List<Card>): FusionValidation {
        return when {
            cards.size < 2 -> FusionValidation.Error("Need at least 2 cards")
            cards.size > MAX_FUSION_CARDS -> FusionValidation.Error("Maximum 5 cards")
            !cards.all { it.rarity == cards.first().rarity } ->
                FusionValidation.Error("All cards must be same rarity")
            cards.first().rarity == CardRarity.LEGENDARY && cards.size < 3 ->
                FusionValidation.Error("Legendary fusion requires 3+ cards")
            else -> FusionValidation.Valid(calculateSuccessRate(cards))
        }
    }
}

sealed class FusionValidation {
    data class Valid(val successRate: Float) : FusionValidation()
    data class Error(val message: String) : FusionValidation()
}
```

### 2.3.2 Special Recipes
```kotlin
object FusionRecipes {
    val ALL_RECIPES = listOf(
        // Beginner recipes
        FusionRecipe(
            id = "triple_common",
            name = "Triple Threat",
            description = "Fuse 3 Common cards for guaranteed Rare",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 3)
            ),
            guaranteedRarity = CardRarity.RARE,
            isSecret = false
        ),

        FusionRecipe(
            id = "quad_common",
            name = "Four Leaf Clover",
            description = "Fuse 4 Common cards for 50% Epic chance",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 4)
            ),
            guaranteedRarity = CardRarity.RARE, // Guaranteed, with 50% Epic chance
            isSecret = false
        ),

        // Advanced recipes
        FusionRecipe(
            id = "double_epic",
            name = "Epic Gamble",
            description = "Fuse 2 Epic cards for 50% Legendary chance",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.EPIC, 2)
            ),
            guaranteedRarity = CardRarity.EPIC,
            isSecret = false
        ),

        // Secret recipes (discovered by experimentation)
        FusionRecipe(
            id = "cursed_fusion",
            name = "Cursed Ritual",
            description = "Fuse 3 'cursed' cards for guaranteed Epic",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 3, tagRequired = "cursed")
            ),
            guaranteedRarity = CardRarity.EPIC,
            guaranteedTags = listOf("cursed", "fusion"),
            isSecret = true
        ),

        FusionRecipe(
            id = "sigma_fusion",
            name = "Sigma Grindset",
            description = "Fuse 5 cards with 'sigma' tag",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 5, tagRequired = "sigma")
            ),
            guaranteedRarity = CardRarity.LEGENDARY,
            guaranteedTags = listOf("sigma", "based", "legendary"),
            isSecret = true
        ),

        FusionRecipe(
            id = "rainbow_fusion",
            name = "Rainbow Road",
            description = "One of each rarity = guaranteed Legendary",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 1),
                FusionCardRequirement(CardRarity.RARE, 1),
                FusionCardRequirement(CardRarity.EPIC, 1),
                FusionCardRequirement(CardRarity.LEGENDARY, 1)
            ),
            guaranteedRarity = CardRarity.LEGENDARY,
            guaranteedTags = listOf("rainbow", "special"),
            isSecret = true
        )
    )

    fun findMatchingRecipe(cards: List<Card>): FusionRecipe? {
        return ALL_RECIPES.find { recipe ->
            matchesRecipe(cards, recipe)
        }
    }

    private fun matchesRecipe(cards: List<Card>, recipe: FusionRecipe): Boolean {
        val cardsByRarity = cards.groupBy { it.rarity }

        return recipe.requiredCards.all { requirement ->
            val matchingCards = cardsByRarity[requirement.rarity] ?: emptyList()

            if (requirement.tagRequired != null) {
                matchingCards.count { card ->
                    card.tags.contains(requirement.tagRequired)
                } >= requirement.count
            } else {
                matchingCards.size >= requirement.count
            }
        }
    }
}
```

---

## 2.4 Database Changes

### 2.4.1 FusionHistoryDao
```kotlin
@Dao
interface FusionHistoryDao {
    @Query("SELECT * FROM fusion_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFusions(limit: Int = 50): Flow<List<FusionHistory>>

    @Query("SELECT * FROM fusion_history WHERE wasSuccessful = 1 ORDER BY timestamp DESC")
    fun getSuccessfulFusions(): Flow<List<FusionHistory>>

    @Insert
    suspend fun insertFusion(fusion: FusionHistory)

    @Query("SELECT COUNT(*) FROM fusion_history WHERE wasSuccessful = 1")
    suspend fun getSuccessfulFusionCount(): Int

    @Query("SELECT COUNT(*) FROM fusion_history WHERE resultRarity = :rarity")
    suspend fun getFusionCountByRarity(rarity: CardRarity): Int

    @Query("SELECT DISTINCT recipeUsed FROM fusion_history WHERE recipeUsed IS NOT NULL")
    suspend fun getDiscoveredRecipes(): List<String>
}
```

### 2.4.2 Update CardDatabase
```kotlin
@Database(
    entities = [Card::class, UserProfile::class, SpinHistory::class, FusionHistory::class],
    version = 3, // Increment version again
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CardDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun spinHistoryDao(): SpinHistoryDao
    abstract fun fusionHistoryDao(): FusionHistoryDao
}
```

### 2.4.3 Update Converters
```kotlin
class Converters {
    // Existing converters...

    @TypeConverter
    fun fromLongList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        if (value.isEmpty()) emptyList()
        else value.split(",").map { it.toLong() }

    @TypeConverter
    fun fromCardRarityList(value: List<CardRarity>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toCardRarityList(value: String): List<CardRarity> =
        if (value.isEmpty()) emptyList()
        else value.split(",").map { CardRarity.valueOf(it) }

    @TypeConverter
    fun fromFusionType(value: FusionType): String = value.name

    @TypeConverter
    fun toFusionType(value: String): FusionType = FusionType.valueOf(value)
}
```

---

## 2.5 Repository Layer

### 2.5.1 FusionRepository
```kotlin
class FusionRepository(
    private val cardDao: CardDao,
    private val fusionHistoryDao: FusionHistoryDao,
    private val userRepository: UserRepository,
    private val aiApiService: AiApiService
) {
    companion object {
        private const val FUSION_COST_COINS = 50
    }

    suspend fun fuseCards(cards: List<Card>): Result<FusionResult> {
        return try {
            // Validate fusion
            val validation = FusionRules.canFuse(cards)
            if (validation is FusionValidation.Error) {
                return Result.failure(Exception(validation.message))
            }

            // Check if user has enough coins
            if (!userRepository.spendCoins(FUSION_COST_COINS)) {
                return Result.failure(Exception("Not enough coins (need $FUSION_COST_COINS)"))
            }

            val successRate = (validation as FusionValidation.Valid).successRate

            // Check for special recipe
            val matchingRecipe = FusionRecipes.findMatchingRecipe(cards)
            val finalSuccessRate = if (matchingRecipe != null) {
                1.0f // Recipes guarantee success
            } else {
                successRate
            }

            // Determine outcome
            val isSuccess = Random.nextFloat() < finalSuccessRate
            val baseRarity = cards.first().rarity
            val resultRarity = if (isSuccess) {
                matchingRecipe?.guaranteedRarity ?: FusionRules.getNextRarity(baseRarity)
            } else {
                baseRarity
            }

            // Generate new card
            val fusionPrompt = generateFusionPrompt(cards, matchingRecipe)
            val resultCard = generateFusionCard(fusionPrompt, resultRarity, matchingRecipe)

            // Save card
            val cardId = cardDao.insertCard(resultCard)

            // Delete input cards
            cards.forEach { cardDao.deleteCard(it) }

            // Record fusion history
            fusionHistoryDao.insertFusion(
                FusionHistory(
                    inputCardIds = cards.map { it.id },
                    inputRarities = cards.map { it.rarity },
                    resultCardId = cardId,
                    resultRarity = resultRarity,
                    fusionType = determineFusionType(cards, matchingRecipe),
                    wasSuccessful = isSuccess,
                    recipeUsed = matchingRecipe?.id
                )
            )

            Result.success(
                FusionResult(
                    success = isSuccess,
                    resultCard = resultCard.copy(id = cardId),
                    rarityUpgraded = resultRarity > baseRarity,
                    bonusApplied = matchingRecipe?.name,
                    recipeDiscovered = if (matchingRecipe?.isSecret == true) matchingRecipe else null
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateFusionPrompt(cards: List<Card>, recipe: FusionRecipe?): String {
        val prompts = cards.map { it.prompt }

        return if (recipe != null) {
            // Use recipe theme
            "fusion of: ${prompts.joinToString(" + ")}, style: ${recipe.name}"
        } else {
            // Combine prompts
            when (prompts.size) {
                2 -> "${prompts[0]} merged with ${prompts[1]}"
                3 -> "${prompts[0]}, ${prompts[1]} and ${prompts[2]} combined"
                else -> "chaotic fusion of: ${prompts.joinToString(", ")}"
            }
        }
    }

    private suspend fun generateFusionCard(
        prompt: String,
        rarity: CardRarity,
        recipe: FusionRecipe?
    ): Card {
        // Generate AI image
        val enhancedPrompt = "fusion art: $prompt, epic style, merged elements"

        val response = aiApiService.generateImage(
            ImageGenerationRequest(
                prompt = enhancedPrompt,
                n = 1,
                size = "512x512"
            )
        )

        val imageUrl = response.data.firstOrNull()?.url ?: ""

        val tags = mutableListOf("fusion").apply {
            recipe?.guaranteedTags?.let { addAll(it) }
        }

        return Card(
            prompt = prompt,
            imageUrl = imageUrl,
            rarity = rarity,
            tags = tags
        )
    }

    private fun determineFusionType(cards: List<Card>, recipe: FusionRecipe?): FusionType {
        return when {
            recipe != null -> FusionType.RECIPE
            cards.size >= 5 -> FusionType.SUPER_FUSION
            else -> FusionType.STANDARD
        }
    }

    fun getDiscoveredRecipes(): Flow<List<FusionRecipe>> {
        return fusionHistoryDao.getDiscoveredRecipes().map { discoveredIds ->
            FusionRecipes.ALL_RECIPES.filter { recipe ->
                !recipe.isSecret || recipe.id in discoveredIds
            }
        }
    }
}
```

---

## 2.6 ViewModel Layer

### 2.6.1 FusionViewModel
```kotlin
class FusionViewModel(
    private val fusionRepository: FusionRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    val allCards = cardRepository.getAllCards().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCards = MutableStateFlow<List<Card>>(emptyList())
    val selectedCards: StateFlow<List<Card>> = _selectedCards.asStateFlow()

    private val _fusionState = MutableStateFlow<FusionState>(FusionState.Idle)
    val fusionState: StateFlow<FusionState> = _fusionState.asStateFlow()

    private val _successRate = MutableStateFlow(0f)
    val successRate: StateFlow<Float> = _successRate.asStateFlow()

    val discoveredRecipes = fusionRepository.getDiscoveredRecipes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleCardSelection(card: Card) {
        val current = _selectedCards.value
        _selectedCards.value = if (card in current) {
            current - card
        } else {
            if (current.size < 5) current + card else current
        }
        updateSuccessRate()
    }

    fun clearSelection() {
        _selectedCards.value = emptyList()
        _successRate.value = 0f
    }

    private fun updateSuccessRate() {
        val cards = _selectedCards.value
        if (cards.isEmpty()) {
            _successRate.value = 0f
            return
        }

        val validation = FusionRules.canFuse(cards)
        _successRate.value = if (validation is FusionValidation.Valid) {
            validation.successRate
        } else {
            0f
        }
    }

    fun performFusion() {
        val cards = _selectedCards.value
        if (cards.size < 2) return

        viewModelScope.launch {
            _fusionState.value = FusionState.Fusing

            // Animation delay
            delay(2000)

            when (val result = fusionRepository.fuseCards(cards)) {
                is Result.Success -> {
                    _fusionState.value = FusionState.Success(result.getOrNull()!!)
                    _selectedCards.value = emptyList()
                }
                is Result.Failure -> {
                    _fusionState.value = FusionState.Error(
                        result.exceptionOrNull()?.message ?: "Fusion failed"
                    )
                }
            }
        }
    }

    fun resetFusionState() {
        _fusionState.value = FusionState.Idle
    }
}

sealed class FusionState {
    object Idle : FusionState()
    object Fusing : FusionState()
    data class Success(val result: FusionResult) : FusionState()
    data class Error(val message: String) : FusionState()
}
```

---

## 2.7 UI Screens

### 2.7.1 CardFusionScreen
```kotlin
@Composable
fun CardFusionScreen(
    viewModel: FusionViewModel,
    onNavigateBack: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState()
    val selectedCards by viewModel.selectedCards.collectAsState()
    val fusionState by viewModel.fusionState.collectAsState()
    val successRate by viewModel.successRate.collectAsState()
    val discoveredRecipes by viewModel.discoveredRecipes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Card Fusion",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected cards display
        SelectedCardsRow(
            selectedCards = selectedCards,
            successRate = successRate,
            onClear = { viewModel.clearSelection() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Fusion button
        FusionButton(
            enabled = selectedCards.size >= 2 && fusionState !is FusionState.Fusing,
            successRate = successRate,
            onClick = { viewModel.performFusion() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tabs: Card Selection / Recipes
        var selectedTab by remember { mutableStateOf(0) }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Select Cards") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Recipes") }
            )
        }

        when (selectedTab) {
            0 -> CardSelectionGrid(
                cards = allCards,
                selectedCards = selectedCards,
                onCardClick = { viewModel.toggleCardSelection(it) }
            )
            1 -> RecipesList(recipes = discoveredRecipes)
        }
    }

    // Fusion result dialog
    if (fusionState is FusionState.Success) {
        FusionResultDialog(
            result = (fusionState as FusionState.Success).result,
            onDismiss = { viewModel.resetFusionState() }
        )
    }
}

@Composable
fun SelectedCardsRow(
    selectedCards: List<Card>,
    successRate: Float,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected: ${selectedCards.size}/5",
                    style = MaterialTheme.typography.titleMedium
                )

                if (selectedCards.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
            }

            if (selectedCards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCards.forEach { card ->
                        MiniCardDisplay(card = card)
                    }
                }

                if (selectedCards.size >= 2) {
                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = successRate,
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            successRate >= 0.5f -> Color.Green
                            successRate >= 0.3f -> Color.Yellow
                            else -> Color.Red
                        }
                    )

                    Text(
                        text = "Success rate: ${(successRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun FusionButton(
    enabled: Boolean,
    successRate: Float,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null
            )
            Text(
                text = "FUSE CARDS (50 coins)",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun FusionResultDialog(
    result: FusionResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (result.rarityUpgraded) "SUCCESS!" else "Fusion Complete",
                color = if (result.rarityUpgraded) Color.Green else Color.White
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show result card
                CardDisplay(card = result.resultCard)

                Spacer(modifier = Modifier.height(16.dp))

                if (result.rarityUpgraded) {
                    Text(
                        text = "Rarity upgraded to ${result.resultCard.rarity}!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Green
                    )
                }

                if (result.bonusApplied != null) {
                    Text(
                        text = "Recipe: ${result.bonusApplied}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (result.recipeDiscovered != null) {
                    Text(
                        text = "New recipe discovered: ${result.recipeDiscovered.name}!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Cyan
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
```

---

## 2.8 Implementation Summary

### Cost System
- **Standard Fusion**: 50 Brainrot Coins
- **Recipe Fusion**: 100 Brainrot Coins (guaranteed success)
- **Super Fusion** (5 cards): 75 Coins (higher success rate)

### Success Rates
| Rarity Input | 2 Cards | 3 Cards | 4 Cards | 5 Cards |
|-------------|---------|---------|---------|---------|
| Common → Rare | 30% | 35% | 40% | 45% |
| Rare → Epic | 20% | 25% | 30% | 35% |
| Epic → Legendary | 10% | 15% | 20% | 25% |

### Recipe Success Rates
All recipes guarantee the stated rarity, with special recipes potentially offering higher rarities.

---

# Part 3: Implementation Roadmap

## Phase 1: Daily Spin & Streak (Week 1-2)

### Week 1: Data Layer
- [ ] Create UserProfile, SpinHistory entities
- [ ] Create UserProfileDao, SpinHistoryDao
- [ ] Update CardDatabase to version 2
- [ ] Create Converters for new types
- [ ] Write unit tests for DAOs

### Week 2: Business Logic & UI
- [ ] Implement StreakManager
- [ ] Implement SpinWheelManager
- [ ] Create UserRepository
- [ ] Create DailyRewardsViewModel
- [ ] Build DailyRewardsScreen UI
- [ ] Add spin wheel animation
- [ ] Test streak calculation logic
- [ ] Integration testing

## Phase 2: Card Fusion (Week 3-4)

### Week 3: Data Layer
- [ ] Create FusionHistory entity
- [ ] Create FusionHistoryDao
- [ ] Update CardDatabase to version 3
- [ ] Update Converters for fusion types
- [ ] Define FusionRecipes
- [ ] Implement FusionRules algorithm

### Week 4: Business Logic & UI
- [ ] Create FusionRepository
- [ ] Create FusionViewModel
- [ ] Build CardFusionScreen UI
- [ ] Add fusion animation
- [ ] Implement recipe discovery system
- [ ] Add fusion history screen
- [ ] Integration testing
- [ ] Balance testing (success rates)

## Phase 3: Polish & Integration (Week 5)

- [ ] Connect daily rewards to main navigation
- [ ] Add fusion access from collection screen
- [ ] Implement notification system for streak reminders
- [ ] Add analytics tracking
- [ ] Performance optimization
- [ ] UI polish and animations
- [ ] Comprehensive testing
- [ ] Bug fixes

---

# Part 4: Dependencies & Technical Notes

## Additional Dependencies Needed

```kotlin
// build.gradle.kts (app)
dependencies {
    // Animation
    implementation("androidx.compose.animation:animation:1.5.4")

    // WorkManager for background tasks (energy regeneration)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

## Background Work (Energy Regeneration)

```kotlin
class EnergyRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val userRepository = // Inject via DI
        userRepository.regenerateEnergy()
        return Result.success()
    }
}

// Schedule in Application.onCreate()
val energyWork = PeriodicWorkRequestBuilder<EnergyRefreshWorker>(
    repeatInterval = 1,
    repeatIntervalTimeUnit = TimeUnit.HOURS
).build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "energy_refresh",
    ExistingPeriodicWorkPolicy.KEEP,
    energyWork
)
```

---

# Part 5: Success Metrics

## Key Performance Indicators

### Daily Spin & Streak
- **Daily Active Users (DAU)**: Target 30% increase
- **Streak Retention**:
  - Day 7: 50% of users
  - Day 30: 20% of users
- **Avg Session Length**: +2 minutes
- **Spin Engagement**: 80% of daily users spin

### Card Fusion
- **Fusion Rate**: 3-5 fusions per user per week
- **Recipe Discovery**: Avg 2-3 recipes discovered per user
- **Duplicate Satisfaction**: Reduce "useless duplicate" complaints by 70%
- **Coin Spending**: 60% of earned coins spent on fusion

---

# Conclusion

This plan provides a complete roadmap for implementing both the Daily Spin & Streak System and the Card Upgrade/Fusion System. Both features are designed to:

1. **Increase Engagement**: Daily rewards create habit formation
2. **Add Depth**: Fusion adds strategic gameplay
3. **Reduce Frustration**: Duplicates have value
4. **Monetization Potential**: Gems for streak protection, coin bundles

The implementation is structured in phases, allowing for iterative development and testing. Each phase builds on the previous one, ensuring a solid foundation before adding complexity.

Next steps: Begin Phase 1 implementation or request clarification on any section.
