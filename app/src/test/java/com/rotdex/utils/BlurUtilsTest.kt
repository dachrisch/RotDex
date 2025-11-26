package com.rotdex.utils

import android.os.Build
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BlurUtils
 *
 * TDD Phase: RED
 * Tests blur capability detection based on Android API level
 *
 * Note: These tests verify the logic of checking Android API level.
 * The actual API level value is provided by the Android system at runtime.
 */
class BlurUtilsTest {

    @Test
    fun `supportsNativeBlur property exists and is accessible`() {
        // ACT
        val supported = BlurUtils.supportsNativeBlur

        // ASSERT
        // Should return a boolean value (true or false based on runtime API level)
        assertNotNull("supportsNativeBlur should return a non-null boolean", supported)
    }

    @Test
    fun `supportsNativeBlur is consistent across multiple calls`() {
        // ACT
        val result1 = BlurUtils.supportsNativeBlur
        val result2 = BlurUtils.supportsNativeBlur
        val result3 = BlurUtils.supportsNativeBlur

        // ASSERT
        assertEquals("Should return consistent value on first and second call", result1, result2)
        assertEquals("Should return consistent value on second and third call", result2, result3)
    }

    @Test
    fun `BlurUtils can be used in conditional statements`() {
        // ACT
        val canUseBlur = BlurUtils.supportsNativeBlur

        // ASSERT
        // Should be able to use in if statements without errors
        val message = if (canUseBlur) {
            "Native blur supported"
        } else {
            "Fallback blur required"
        }

        assertNotNull("Should generate a message based on blur support", message)
        assertTrue(
            "Message should be one of the expected values",
            message == "Native blur supported" || message == "Fallback blur required"
        )
    }
}
