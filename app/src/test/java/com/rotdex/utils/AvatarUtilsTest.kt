package com.rotdex.utils

import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AvatarUtils
 *
 * TDD Phase: RED
 * Tests avatar utility functions:
 * - getInitials(): Extract 2-letter initials from player names
 * - getColorFromName(): Generate consistent HSL color from name hash
 */
class AvatarUtilsTest {

    // MARK: - getInitials() Tests

    @Test
    fun `getInitials returns first two letters for single word`() {
        // ARRANGE
        val name = "Player"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should return first 2 letters uppercase", "PL", initials)
    }

    @Test
    fun `getInitials returns first letter of each word for two words`() {
        // ARRANGE
        val name = "John Doe"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should return first letter of each word", "JD", initials)
    }

    @Test
    fun `getInitials handles camelCase names`() {
        // ARRANGE
        val name = "JohnDoe"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should extract capital letters from camelCase", "JD", initials)
    }

    @Test
    fun `getInitials handles auto-generated names with hyphens`() {
        // ARRANGE
        val name = "player-abc123"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should return first letter of each part separated by hyphen", "PA", initials)
    }

    @Test
    fun `getInitials handles single character names`() {
        // ARRANGE
        val name = "X"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should return single character twice", "XX", initials)
    }

    @Test
    fun `getInitials handles empty string`() {
        // ARRANGE
        val name = ""

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should return default initials for empty string", "??", initials)
    }

    @Test
    fun `getInitials handles names with numbers`() {
        // ARRANGE
        val name = "Player123"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should extract first 2 letters ignoring numbers", "PL", initials)
    }

    @Test
    fun `getInitials handles names with special characters`() {
        // ARRANGE
        val name = "Player@2024"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should extract letters only", "PL", initials)
    }

    @Test
    fun `getInitials handles three word names`() {
        // ARRANGE
        val name = "John Paul Jones"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should return first letter of first two words", "JP", initials)
    }

    @Test
    fun `getInitials handles lowercase names`() {
        // ARRANGE
        val name = "alice"

        // ACT
        val initials = AvatarUtils.getInitials(name)

        // ASSERT
        assertEquals("Should convert to uppercase", "AL", initials)
    }

    @Test
    fun `getInitials always returns exactly 2 characters`() {
        // ARRANGE
        val testNames = listOf(
            "A",
            "AB",
            "ABC",
            "A B",
            "A B C",
            "player-xyz",
            "CamelCase",
            ""
        )

        // ACT & ASSERT
        testNames.forEach { name ->
            val initials = AvatarUtils.getInitials(name)
            assertEquals(
                "Initials for '$name' should be exactly 2 characters",
                2,
                initials.length
            )
        }
    }

    // MARK: - getColorFromName() Tests

    @Test
    fun `getColorFromName returns consistent color for same name`() {
        // ARRANGE
        val name = "TestPlayer"

        // ACT
        val color1 = AvatarUtils.getColorFromName(name)
        val color2 = AvatarUtils.getColorFromName(name)

        // ASSERT
        assertEquals("Should return identical color for same name", color1, color2)
    }

    @Test
    fun `getColorFromName returns different colors for different names`() {
        // ARRANGE
        val name1 = "Alice"
        val name2 = "Bob"

        // ACT
        val color1 = AvatarUtils.getColorFromName(name1)
        val color2 = AvatarUtils.getColorFromName(name2)

        // ASSERT
        assertNotEquals("Should return different colors for different names", color1, color2)
    }

    @Test
    fun `getColorFromName returns vibrant colors`() {
        // ARRANGE
        val name = "Player123"

        // ACT
        val color = AvatarUtils.getColorFromName(name)

        // ASSERT
        // Vibrant colors should have saturation >= 0.5 and lightness around 0.5-0.7
        // We can't directly test HSL values, but we can check the color is not grayscale
        val red = color.red
        val green = color.green
        val blue = color.blue

        val maxChannel = maxOf(red, green, blue)
        val minChannel = minOf(red, green, blue)
        val saturation = if (maxChannel == minChannel) {
            0f
        } else {
            (maxChannel - minChannel) / maxChannel
        }

        assertTrue(
            "Color should be vibrant (saturation >= 0.3), got $saturation",
            saturation >= 0.3f
        )
    }

    @Test
    fun `getColorFromName handles empty string`() {
        // ARRANGE
        val name = ""

        // ACT
        val color = AvatarUtils.getColorFromName(name)

        // ASSERT
        assertNotNull("Should return a color even for empty string", color)
        // Empty string should produce a consistent color
        val color2 = AvatarUtils.getColorFromName("")
        assertEquals("Should be consistent for empty string", color, color2)
    }

    @Test
    fun `getColorFromName generates diverse palette`() {
        // ARRANGE
        val names = listOf(
            "Alice", "Bob", "Charlie", "Diana", "Eve",
            "Frank", "Grace", "Henry", "Iris", "Jack"
        )

        // ACT
        val colors = names.map { AvatarUtils.getColorFromName(it) }

        // ASSERT
        val uniqueColors = colors.toSet()
        assertTrue(
            "Should generate at least 8 different colors from 10 names",
            uniqueColors.size >= 8
        )
    }

    @Test
    fun `getColorFromName is case sensitive`() {
        // ARRANGE
        val name1 = "player"
        val name2 = "PLAYER"

        // ACT
        val color1 = AvatarUtils.getColorFromName(name1)
        val color2 = AvatarUtils.getColorFromName(name2)

        // ASSERT
        assertNotEquals(
            "Should generate different colors for different cases",
            color1,
            color2
        )
    }

    @Test
    fun `getColorFromName handles very long names`() {
        // ARRANGE
        val longName = "ThisIsAVeryLongPlayerNameThatShouldStillGenerateAConsistentColor"

        // ACT
        val color = AvatarUtils.getColorFromName(longName)

        // ASSERT
        assertNotNull("Should handle long names", color)
        val color2 = AvatarUtils.getColorFromName(longName)
        assertEquals("Should be consistent for same long name", color, color2)
    }

    @Test
    fun `getColorFromName returns valid Color object`() {
        // ARRANGE
        val name = "ValidPlayer"

        // ACT
        val color = AvatarUtils.getColorFromName(name)

        // ASSERT
        // Check color components are in valid range [0, 1]
        assertTrue("Red channel should be in [0, 1]", color.red in 0f..1f)
        assertTrue("Green channel should be in [0, 1]", color.green in 0f..1f)
        assertTrue("Blue channel should be in [0, 1]", color.blue in 0f..1f)
        assertTrue("Alpha channel should be 1 (opaque)", color.alpha == 1f)
    }

    // MARK: - Integration Tests

    @Test
    fun `initials and color work together for typical player names`() {
        // ARRANGE
        val testCases = mapOf(
            "player-abc123" to "PA",
            "JohnDoe" to "JD",
            "Alice" to "AL",
            "X" to "XX"
        )

        // ACT & ASSERT
        testCases.forEach { (name, expectedInitials) ->
            val initials = AvatarUtils.getInitials(name)
            val color = AvatarUtils.getColorFromName(name)

            assertEquals("Initials for '$name'", expectedInitials, initials)
            assertNotNull("Color for '$name'", color)
        }
    }
}
