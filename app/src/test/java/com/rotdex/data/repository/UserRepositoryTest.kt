package com.rotdex.data.repository

import com.rotdex.data.database.SpinHistoryDao
import com.rotdex.data.database.UserProfileDao
import com.rotdex.data.models.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for UserRepository player identity methods
 *
 * TDD Phase: RED
 * These tests verify the new player identity methods:
 * - updatePlayerName(name: String)
 * - updateAvatarImage(imagePath: String?)
 * - getPlayerName(): String
 * - getAvatarImagePath(): String?
 */
class UserRepositoryTest {

    private lateinit var repository: UserRepository
    private lateinit var fakeUserProfileDao: FakeUserProfileDao
    private lateinit var fakeSpinHistoryDao: FakeSpinHistoryDao

    @Before
    fun setup() {
        fakeUserProfileDao = FakeUserProfileDao()
        fakeSpinHistoryDao = FakeSpinHistoryDao()
        repository = UserRepository(fakeUserProfileDao, fakeSpinHistoryDao)
    }

    // MARK: - Player Name Tests

    @Test
    fun `updatePlayerName updates the player name in database`() = runTest {
        // ARRANGE
        val initialProfile = UserProfile(
            userId = "default_user",
            playerName = "player-abc12345"
        )
        fakeUserProfileDao.currentProfile = initialProfile
        val newName = "RotDexChampion"

        // ACT
        repository.updatePlayerName(newName)

        // ASSERT
        assertEquals(
            "Player name should be updated in database",
            newName,
            fakeUserProfileDao.currentProfile?.playerName
        )
    }

    @Test
    fun `getPlayerName returns current player name`() = runTest {
        // ARRANGE
        val expectedName = "MasterPlayer"
        fakeUserProfileDao.currentProfile = UserProfile(playerName = expectedName)

        // ACT
        val actualName = repository.getPlayerName()

        // ASSERT
        assertEquals("Should return current player name", expectedName, actualName)
    }

    @Test
    fun `getPlayerName returns default when profile is null`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = null

        // ACT
        val playerName = repository.getPlayerName()

        // ASSERT
        assertTrue(
            "Should return default generated name when profile is null",
            playerName.startsWith("player-")
        )
    }

    @Test
    fun `updatePlayerName trims whitespace`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = UserProfile()
        val nameWithWhitespace = "  SpacedName  "

        // ACT
        repository.updatePlayerName(nameWithWhitespace)

        // ASSERT
        assertEquals(
            "Player name should be trimmed",
            "SpacedName",
            fakeUserProfileDao.currentProfile?.playerName
        )
    }

    @Test
    fun `updatePlayerName handles empty string by keeping existing name`() = runTest {
        // ARRANGE
        val originalName = "OriginalPlayer"
        fakeUserProfileDao.currentProfile = UserProfile(playerName = originalName)

        // ACT
        repository.updatePlayerName("")

        // ASSERT
        assertEquals(
            "Empty string should not update player name",
            originalName,
            fakeUserProfileDao.currentProfile?.playerName
        )
    }

    @Test
    fun `updatePlayerName handles whitespace-only string by keeping existing name`() = runTest {
        // ARRANGE
        val originalName = "OriginalPlayer"
        fakeUserProfileDao.currentProfile = UserProfile(playerName = originalName)

        // ACT
        repository.updatePlayerName("   ")

        // ASSERT
        assertEquals(
            "Whitespace-only string should not update player name",
            originalName,
            fakeUserProfileDao.currentProfile?.playerName
        )
    }

    // MARK: - Avatar Image Tests

    @Test
    fun `updateAvatarImage updates the avatar path in database`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = UserProfile()
        val avatarPath = "/data/user/0/com.rotdex/files/avatars/avatar_123.jpg"

        // ACT
        repository.updateAvatarImage(avatarPath)

        // ASSERT
        assertEquals(
            "Avatar image path should be updated in database",
            avatarPath,
            fakeUserProfileDao.currentProfile?.avatarImagePath
        )
    }

    @Test
    fun `updateAvatarImage can set null to remove avatar`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = UserProfile(
            avatarImagePath = "/path/to/old/avatar.jpg"
        )

        // ACT
        repository.updateAvatarImage(null)

        // ASSERT
        assertNull(
            "Avatar image path should be set to null",
            fakeUserProfileDao.currentProfile?.avatarImagePath
        )
    }

    @Test
    fun `getAvatarImagePath returns current avatar path`() = runTest {
        // ARRANGE
        val expectedPath = "/path/to/avatar.png"
        fakeUserProfileDao.currentProfile = UserProfile(avatarImagePath = expectedPath)

        // ACT
        val actualPath = repository.getAvatarImagePath()

        // ASSERT
        assertEquals("Should return current avatar path", expectedPath, actualPath)
    }

    @Test
    fun `getAvatarImagePath returns null when no avatar set`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = UserProfile(avatarImagePath = null)

        // ACT
        val avatarPath = repository.getAvatarImagePath()

        // ASSERT
        assertNull("Should return null when no avatar is set", avatarPath)
    }

    @Test
    fun `getAvatarImagePath returns null when profile is null`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = null

        // ACT
        val avatarPath = repository.getAvatarImagePath()

        // ASSERT
        assertNull("Should return null when profile does not exist", avatarPath)
    }

    // MARK: - Integration Tests

    @Test
    fun `can update both player name and avatar together`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = UserProfile()
        val newName = "CoolPlayer"
        val newAvatarPath = "/path/to/cool/avatar.jpg"

        // ACT
        repository.updatePlayerName(newName)
        repository.updateAvatarImage(newAvatarPath)

        // ASSERT
        val profile = fakeUserProfileDao.currentProfile
        assertEquals("Player name should be updated", newName, profile?.playerName)
        assertEquals("Avatar path should be updated", newAvatarPath, profile?.avatarImagePath)
    }

    @Test
    fun `player identity fields persist across multiple operations`() = runTest {
        // ARRANGE
        fakeUserProfileDao.currentProfile = UserProfile(
            playerName = "PersistentPlayer",
            avatarImagePath = "/persistent/avatar.jpg"
        )

        // ACT - Perform other operations (energy, coins)
        repository.spendEnergy(1)
        repository.addCoins(100)

        // Get player identity
        val playerName = repository.getPlayerName()
        val avatarPath = repository.getAvatarImagePath()

        // ASSERT
        assertEquals("Player name should persist", "PersistentPlayer", playerName)
        assertEquals("Avatar path should persist", "/persistent/avatar.jpg", avatarPath)
    }
}

/**
 * Fake implementation of UserProfileDao for testing
 * Uses in-memory storage instead of actual database
 */
private class FakeUserProfileDao : UserProfileDao {
    var currentProfile: UserProfile? = null

    override fun getUserProfile(userId: String): Flow<UserProfile> {
        return flowOf(currentProfile ?: UserProfile())
    }

    override suspend fun getUserProfileOnce(userId: String): UserProfile? {
        return currentProfile
    }

    override suspend fun insertProfile(profile: UserProfile) {
        currentProfile = profile
    }

    override suspend fun updateProfile(profile: UserProfile) {
        currentProfile = profile
    }

    override suspend fun updateEnergy(energy: Int, timestamp: Long, userId: String) {
        currentProfile = currentProfile?.copy(currentEnergy = energy, lastEnergyRefresh = timestamp)
    }

    override suspend fun updateMaxEnergy(maxEnergy: Int, userId: String) {
        currentProfile = currentProfile?.copy(maxEnergy = maxEnergy)
    }

    override suspend fun addCoins(amount: Int, userId: String) {
        currentProfile = currentProfile?.copy(
            brainrotCoins = (currentProfile?.brainrotCoins ?: 0) + amount
        )
    }

    override suspend fun addGems(amount: Int, userId: String) {
        currentProfile = currentProfile?.copy(
            gems = (currentProfile?.gems ?: 0) + amount
        )
    }

    override suspend fun addEnergy(amount: Int, userId: String) {
        currentProfile = currentProfile?.copy(
            currentEnergy = (currentProfile?.currentEnergy ?: 0) + amount
        )
    }

    override suspend fun setCoins(amount: Int, userId: String) {
        currentProfile = currentProfile?.copy(brainrotCoins = amount)
    }

    override suspend fun setGems(amount: Int, userId: String) {
        currentProfile = currentProfile?.copy(gems = amount)
    }

    override suspend fun updateStreak(streak: Int, longest: Int, date: String, userId: String) {
        currentProfile = currentProfile?.copy(
            currentStreak = streak,
            longestStreak = longest,
            lastLoginDate = date,
            totalLoginDays = (currentProfile?.totalLoginDays ?: 0) + 1
        )
    }

    override suspend fun updateSpinStatus(used: Boolean, date: String, userId: String) {
        currentProfile = currentProfile?.copy(
            hasUsedSpinToday = used,
            lastSpinDate = date,
            totalSpins = (currentProfile?.totalSpins ?: 0) + 1
        )
    }

    override suspend fun addStreakProtections(amount: Int, userId: String) {
        currentProfile = currentProfile?.copy(
            streakProtections = (currentProfile?.streakProtections ?: 0) + amount
        )
    }

    override suspend fun useStreakProtection(userId: String): Int {
        val protections = currentProfile?.streakProtections ?: 0
        if (protections > 0) {
            currentProfile = currentProfile?.copy(streakProtections = protections - 1)
            return 1
        }
        return 0
    }

    override suspend fun getTotalSpins(userId: String): Int {
        return currentProfile?.totalSpins ?: 0
    }

    override suspend fun getTotalLoginDays(userId: String): Int {
        return currentProfile?.totalLoginDays ?: 0
    }

    override suspend fun getLongestStreak(userId: String): Int {
        return currentProfile?.longestStreak ?: 0
    }

    override suspend fun updatePlayerName(name: String, userId: String) {
        currentProfile = currentProfile?.copy(playerName = name)
    }

    override suspend fun updateAvatarImagePath(imagePath: String?, userId: String) {
        currentProfile = currentProfile?.copy(avatarImagePath = imagePath)
    }

    override suspend fun getPlayerName(userId: String): String? {
        return currentProfile?.playerName
    }

    override suspend fun getAvatarImagePath(userId: String): String? {
        return currentProfile?.avatarImagePath
    }
}

/**
 * Fake implementation of SpinHistoryDao for testing
 */
private class FakeSpinHistoryDao : SpinHistoryDao {
    override fun getRecentSpins(limit: Int) = flowOf(emptyList<com.rotdex.data.models.SpinHistory>())
    override suspend fun insertSpin(spin: com.rotdex.data.models.SpinHistory): Long = 0L
    override suspend fun getLastSpin() = null
    override suspend fun getSpinById(id: Long) = null
    override suspend fun getTotalSpinCount() = 0
    override suspend fun getCountByRewardType(type: com.rotdex.data.models.SpinRewardType) = 0
    override suspend fun getTotalAmountByType(type: com.rotdex.data.models.SpinRewardType) = null
    override fun getSpinsSince(startTime: Long) = flowOf(emptyList<com.rotdex.data.models.SpinHistory>())
    override suspend fun deleteOldHistory(cutoffTime: Long) = 0
    override suspend fun deleteAllHistory() {}
}
