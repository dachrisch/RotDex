package com.rotdex.mcp.tools

/**
 * Logcat tools for viewing and filtering logs from the RotDex app during development.
 */
object LogcatTools {

    private const val ROTDEX_PACKAGE = "com.rotdex"

    /**
     * Get recent logs from the device (last 100 lines).
     */
    suspend fun getRecentLogs(deviceId: String, lines: Int = 100): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "logcat",
            "-d", "-t", lines.toString()
        )
    }

    /**
     * Get logs filtered by RotDex package.
     */
    suspend fun getAppLogs(deviceId: String, lines: Int = 100): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }

        // First get the PID of RotDex
        val pidResult = CommandExecutor.execute(
            "adb", "-s", deviceId, "shell",
            "pidof", ROTDEX_PACKAGE
        )

        return if (pidResult.isSuccess && pidResult.stdout.isNotBlank()) {
            val pid = pidResult.stdout.trim()
            CommandExecutor.execute(
                "adb", "-s", deviceId, "logcat",
                "-d", "-t", lines.toString(),
                "--pid=$pid"
            )
        } else {
            // Fallback: filter by tag pattern
            CommandExecutor.execute(
                "adb", "-s", deviceId, "logcat",
                "-d", "-t", lines.toString(),
                "-s", "RotDex:*", "AndroidRuntime:E"
            )
        }
    }

    /**
     * Get error logs only.
     */
    suspend fun getErrorLogs(deviceId: String, lines: Int = 50): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "logcat",
            "-d", "-t", lines.toString(),
            "*:E"
        )
    }

    /**
     * Get crash logs (AndroidRuntime errors).
     */
    suspend fun getCrashLogs(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "logcat",
            "-d", "-t", "200",
            "-s", "AndroidRuntime:E", "FATAL:E"
        )
    }

    /**
     * Filter logs by a custom tag.
     */
    suspend fun filterByTag(deviceId: String, tag: String, lines: Int = 100): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        // Validate tag to prevent injection
        if (!tag.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return CommandExecutor.CommandResult(-1, "", "Invalid tag name: $tag")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "logcat",
            "-d", "-t", lines.toString(),
            "-s", "$tag:*"
        )
    }

    /**
     * Search logs for a specific text pattern.
     */
    suspend fun searchLogs(deviceId: String, pattern: String, lines: Int = 200): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }

        val logsResult = CommandExecutor.execute(
            "adb", "-s", deviceId, "logcat",
            "-d", "-t", lines.toString()
        )

        if (!logsResult.isSuccess) return logsResult

        // Filter locally to avoid shell injection risks
        val filteredLines = logsResult.stdout
            .lines()
            .filter { it.contains(pattern, ignoreCase = true) }
            .joinToString("\n")

        return CommandExecutor.CommandResult(
            0,
            if (filteredLines.isBlank()) "No matches found for: $pattern" else filteredLines,
            ""
        )
    }

    /**
     * Clear the logcat buffer.
     */
    suspend fun clearLogs(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "logcat", "-c"
        )
    }

    /**
     * Get ViewModel-specific logs (useful for debugging state changes).
     */
    suspend fun getViewModelLogs(deviceId: String, viewModelName: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        // Validate viewModelName
        if (!viewModelName.matches(Regex("^[a-zA-Z0-9]+$"))) {
            return CommandExecutor.CommandResult(-1, "", "Invalid ViewModel name: $viewModelName")
        }
        return searchLogs(deviceId, viewModelName, 200)
    }
}
