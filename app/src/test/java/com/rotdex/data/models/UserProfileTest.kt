package com.rotdex.data.models

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for UserProfile data class
 * Tests player identity fields and default values
 *
 * TDD Phase: RED
 * These tests verify the new player identity fields:
 * - playerName with auto-generated default
 * - avatarImagePath optional field
 */
class UserProfileTest {

    @Test
    fun `UserProfile has playerName field with auto-generated default`() {
        // ARRANGE & ACT
        val profile = UserProfile()

        // ASSERT
        assertNotNull("playerName should not be null", profile.playerName)
        assertTrue(
            "playerName should start with 'player-'",
            profile.playerName.startsWith("player-")
        )
        // Format: "player-XXXXXXXX" where X is alphanumeric
        assertEquals(
            "playerName should be 'player-' + 8 characters",
            15,
            profile.playerName.length
        )
    }

    @Test
    fun `playerName default follows correct format`() {
        // ARRANGE & ACT
        val profile1 = UserProfile()
        val profile2 = UserProfile()

        // ASSERT
        // Each profile should have unique generated name
        assertNotEquals(
            "Different profiles should have different generated playerNames",
            profile1.playerName,
            profile2.playerName
        )

        // Both should follow the pattern
        val playerNamePattern = "^player-[a-zA-Z0-9]{8}$".toRegex()
        assertTrue(
            "playerName should match pattern 'player-XXXXXXXX'",
            playerNamePattern.matches(profile1.playerName)
        )
        assertTrue(
            "playerName should match pattern 'player-XXXXXXXX'",
            playerNamePattern.matches(profile2.playerName)
        )
    }

    @Test
    fun `UserProfile has avatarImagePath field defaulting to null`() {
        // ARRANGE & ACT
        val profile = UserProfile()

        // ASSERT
        assertNull("avatarImagePath should default to null", profile.avatarImagePath)
    }

    @Test
    fun `UserProfile can be created with custom playerName`() {
        // ARRANGE
        val customName = "BrainrotMaster"

        // ACT
        val profile = UserProfile(playerName = customName)

        // ASSERT
        assertEquals("playerName should be set to custom value", customName, profile.playerName)
    }

    @Test
    fun `UserProfile can be created with avatarImagePath`() {
        // ARRANGE
        val avatarPath = "/data/user/0/com.rotdex/files/avatars/avatar_123.jpg"

        // ACT
        val profile = UserProfile(avatarImagePath = avatarPath)

        // ASSERT
        assertEquals("avatarImagePath should be set", avatarPath, profile.avatarImagePath)
    }

    @Test
    fun `UserProfile copy preserves player identity fields`() {
        // ARRANGE
        val original = UserProfile(
            playerName = "TestPlayer",
            avatarImagePath = "/path/to/avatar.jpg"
        )

        // ACT
        val copy = original.copy(brainrotCoins = 500)

        // ASSERT
        assertEquals("playerName should be preserved in copy", original.playerName, copy.playerName)
        assertEquals("avatarImagePath should be preserved in copy", original.avatarImagePath, copy.avatarImagePath)
        assertEquals("brainrotCoins should be updated", 500, copy.brainrotCoins)
    }

    @Test
    fun `UserProfile with all fields populated is valid`() {
        // ARRANGE & ACT
        val profile = UserProfile(
            userId = "test_user",
            currentEnergy = 3,
            maxEnergy = 5,
            lastEnergyRefresh = System.currentTimeMillis(),
            brainrotCoins = 250,
            gems = 10,
            currentStreak = 7,
            longestStreak = 15,
            lastLoginDate = "2025-11-26",
            lastSpinDate = "2025-11-26",
            hasUsedSpinToday = true,
            streakProtections = 2,
            totalSpins = 42,
            totalLoginDays = 30,
            accountCreatedAt = System.currentTimeMillis(),
            playerName = "EpicGamer",
            avatarImagePath = "/path/to/custom/avatar.png"
        )

        // ASSERT
        assertEquals("userId should match", "test_user", profile.userId)
        assertEquals("currentEnergy should match", 3, profile.currentEnergy)
        assertEquals("playerName should match", "EpicGamer", profile.playerName)
        assertEquals("avatarImagePath should match", "/path/to/custom/avatar.png", profile.avatarImagePath)
    }

    @Test
    fun `multiple UserProfile instances have unique generated playerNames`() {
        // ARRANGE & ACT
        val profiles = List(10) { UserProfile() }

        // ASSERT
        val uniqueNames = profiles.map { it.playerName }.toSet()
        assertEquals(
            "All 10 profiles should have unique generated playerNames",
            10,
            uniqueNames.size
        )
    }
}
