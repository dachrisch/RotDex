package com.rotdex.utils

import android.os.Build

/**
 * Utility object for blur capability detection
 *
 * Provides runtime checks for native blur support in Jetpack Compose.
 * Native Modifier.blur() requires API 31+ (Android 12+).
 *
 * For older devices, fallback to alternative visual effects
 * (e.g., frosted glass overlay with reduced opacity).
 */
object BlurUtils {

    /**
     * Check if the device supports native Modifier.blur()
     *
     * Native blur using RenderEffect was introduced in API 31 (Android 12).
     * Devices below API 31 will need to use fallback blur techniques.
     *
     * @return true if API >= 31, false otherwise
     */
    val supportsNativeBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // S = API 31 (Android 12)
}
