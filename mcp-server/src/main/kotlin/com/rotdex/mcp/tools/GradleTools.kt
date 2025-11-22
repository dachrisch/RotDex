package com.rotdex.mcp.tools

import java.io.File

/**
 * Gradle tools for building and testing the RotDex app during development.
 */
object GradleTools {

    private val projectDir: File by lazy {
        // Find the project root (where gradlew is located)
        var dir = File(System.getProperty("user.dir"))
        while (!File(dir, "gradlew").exists() && dir.parentFile != null) {
            dir = dir.parentFile
        }
        dir
    }

    private val gradlew: String by lazy {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) "gradlew.bat" else "./gradlew"
    }

    /**
     * Build debug APK.
     */
    suspend fun assembleDebug(): CommandExecutor.CommandResult {
        return runGradleTask("assembleDebug")
    }

    /**
     * Build release APK.
     */
    suspend fun assembleRelease(): CommandExecutor.CommandResult {
        return runGradleTask("assembleRelease")
    }

    /**
     * Clean the project.
     */
    suspend fun clean(): CommandExecutor.CommandResult {
        return runGradleTask("clean")
    }

    /**
     * Run unit tests.
     */
    suspend fun runTests(): CommandExecutor.CommandResult {
        return runGradleTask("test")
    }

    /**
     * Run a specific test class.
     */
    suspend fun runTestClass(testClass: String): CommandExecutor.CommandResult {
        // Validate test class name to prevent injection
        if (!testClass.matches(Regex("^[a-zA-Z0-9._]+$"))) {
            return CommandExecutor.CommandResult(-1, "", "Invalid test class name: $testClass")
        }
        return runGradleTask("test", "--tests", testClass)
    }

    /**
     * Run instrumented tests (requires connected device/emulator).
     */
    suspend fun runInstrumentedTests(): CommandExecutor.CommandResult {
        return runGradleTask("connectedAndroidTest")
    }

    /**
     * Run Detekt static analysis.
     */
    suspend fun runDetekt(): CommandExecutor.CommandResult {
        return runGradleTask("detekt")
    }

    /**
     * Check for dependency updates.
     */
    suspend fun checkDependencyUpdates(): CommandExecutor.CommandResult {
        return runGradleTask("dependencyUpdates")
    }

    /**
     * Get the path to the debug APK.
     */
    fun getDebugApkPath(): String {
        return File(projectDir, "app/build/outputs/apk/debug/app-debug.apk").absolutePath
    }

    /**
     * Get the path to the release APK.
     */
    fun getReleaseApkPath(): String {
        return File(projectDir, "app/build/outputs/apk/release/app-release.apk").absolutePath
    }

    /**
     * Run an arbitrary Gradle task.
     */
    private suspend fun runGradleTask(vararg tasks: String): CommandExecutor.CommandResult {
        return CommandExecutor.execute(
            gradlew,
            *tasks,
            "--console=plain",
            workingDir = projectDir,
            timeoutSeconds = 600 // 10 minutes for long builds
        )
    }

    /**
     * Build and install debug APK in one command.
     */
    suspend fun buildAndInstall(deviceId: String): CommandExecutor.CommandResult {
        // First build
        val buildResult = assembleDebug()
        if (!buildResult.isSuccess) {
            return buildResult
        }

        // Then install
        val apkPath = getDebugApkPath()
        if (!File(apkPath).exists()) {
            return CommandExecutor.CommandResult(-1, "", "APK not found at $apkPath after build")
        }

        return AdbTools.installApk(deviceId, apkPath)
    }
}
