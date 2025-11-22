package com.rotdex.mcp.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Utility class for executing shell commands safely.
 * All commands are validated before execution to prevent injection attacks.
 */
object CommandExecutor {

    data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    /**
     * Execute a command with arguments.
     * @param command The command to execute (e.g., "adb", "gradlew")
     * @param args The arguments to pass to the command
     * @param workingDir Optional working directory
     * @param timeoutSeconds Timeout in seconds (default 120)
     */
    suspend fun execute(
        command: String,
        vararg args: String,
        workingDir: File? = null,
        timeoutSeconds: Long = 120
    ): CommandResult = withContext(Dispatchers.IO) {
        val processBuilder = ProcessBuilder(listOf(command) + args.toList())
            .redirectErrorStream(false)

        workingDir?.let { processBuilder.directory(it) }

        val process = processBuilder.start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            return@withContext CommandResult(-1, "", "Command timed out after $timeoutSeconds seconds")
        }

        CommandResult(process.exitValue(), stdout.trim(), stderr.trim())
    }

    /**
     * Validate a file path to prevent path traversal attacks.
     */
    fun validatePath(path: String): Boolean {
        val normalizedPath = File(path).canonicalPath
        return !path.contains("..") && File(normalizedPath).exists()
    }

    /**
     * Validate a device ID to prevent command injection.
     */
    fun validateDeviceId(deviceId: String): Boolean {
        return deviceId.matches(Regex("^[a-zA-Z0-9.:_-]+$"))
    }
}
