package com.rotdex.utils

/**
 * Utility functions for formatting dates and timestamps
 */
object DateUtils {
    /**
     * Format a timestamp to a human-readable relative time string
     *
     * @param timestamp The timestamp in milliseconds
     * @return A formatted string like "2 days ago", "3 hours ago", etc.
     */
    fun formatTimestamp(timestamp: Long): String {
        // Handle invalid timestamps
        if (timestamp <= 0) return "Unknown date"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        // Handle future timestamps (clock skew or invalid data)
        if (diff < 0) return "Just now"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 365 -> {
                val years = days / 365
                "$years year${if (years > 1) "s" else ""} ago"
            }
            days > 30 -> {
                val months = days / 30
                "$months month${if (months > 1) "s" else ""} ago"
            }
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
}
