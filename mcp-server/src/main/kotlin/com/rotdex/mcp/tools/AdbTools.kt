package com.rotdex.mcp.tools

import java.io.File

/**
 * ADB tools for controlling Android devices and the RotDex app during development.
 */
object AdbTools {

    private const val ROTDEX_PACKAGE = "com.rotdex"

    /**
     * List all connected Android devices.
     */
    suspend fun listDevices(): CommandExecutor.CommandResult {
        return CommandExecutor.execute("adb", "devices", "-l")
    }

    /**
     * Install the RotDex APK to a specific device.
     */
    suspend fun installApk(deviceId: String, apkPath: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        if (!File(apkPath).exists()) {
            return CommandExecutor.CommandResult(-1, "", "APK file not found: $apkPath")
        }
        return CommandExecutor.execute("adb", "-s", deviceId, "install", "-r", apkPath)
    }

    /**
     * Uninstall RotDex from a device.
     */
    suspend fun uninstallApp(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute("adb", "-s", deviceId, "uninstall", ROTDEX_PACKAGE)
    }

    /**
     * Launch the RotDex app on a device.
     */
    suspend fun launchApp(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "shell",
            "am", "start", "-n", "$ROTDEX_PACKAGE/.MainActivity"
        )
    }

    /**
     * Stop the RotDex app on a device.
     */
    suspend fun stopApp(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "shell",
            "am", "force-stop", ROTDEX_PACKAGE
        )
    }

    /**
     * Clear app data for RotDex.
     */
    suspend fun clearAppData(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "shell",
            "pm", "clear", ROTDEX_PACKAGE
        )
    }

    /**
     * Take a screenshot from the device.
     */
    suspend fun takeScreenshot(deviceId: String, outputPath: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }

        val remotePath = "/sdcard/screenshot.png"

        // Capture screenshot on device
        val captureResult = CommandExecutor.execute(
            "adb", "-s", deviceId, "shell",
            "screencap", "-p", remotePath
        )
        if (!captureResult.isSuccess) return captureResult

        // Pull screenshot to local machine
        val pullResult = CommandExecutor.execute(
            "adb", "-s", deviceId, "pull", remotePath, outputPath
        )
        if (!pullResult.isSuccess) return pullResult

        // Clean up remote file
        CommandExecutor.execute("adb", "-s", deviceId, "shell", "rm", remotePath)

        return CommandExecutor.CommandResult(0, "Screenshot saved to $outputPath", "")
    }

    /**
     * Get device info.
     */
    suspend fun getDeviceInfo(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }

        val model = CommandExecutor.execute("adb", "-s", deviceId, "shell", "getprop", "ro.product.model")
        val sdk = CommandExecutor.execute("adb", "-s", deviceId, "shell", "getprop", "ro.build.version.sdk")
        val version = CommandExecutor.execute("adb", "-s", deviceId, "shell", "getprop", "ro.build.version.release")

        val info = buildString {
            appendLine("Device: ${model.stdout}")
            appendLine("Android Version: ${version.stdout}")
            appendLine("SDK Level: ${sdk.stdout}")
        }

        return CommandExecutor.CommandResult(0, info, "")
    }

    /**
     * Check if RotDex is installed on the device.
     */
    suspend fun isAppInstalled(deviceId: String): CommandExecutor.CommandResult {
        if (!CommandExecutor.validateDeviceId(deviceId)) {
            return CommandExecutor.CommandResult(-1, "", "Invalid device ID: $deviceId")
        }
        return CommandExecutor.execute(
            "adb", "-s", deviceId, "shell",
            "pm", "list", "packages", ROTDEX_PACKAGE
        )
    }
}
