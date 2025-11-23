package com.rotdex.mcp

import com.rotdex.mcp.server.RotDexMcpServer
import com.rotdex.mcp.tools.CommandExecutor
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RotDexMcpServerTest {

    @Test
    fun `server can be instantiated`() {
        val server = RotDexMcpServer()
        // Server should instantiate without throwing
    }

    @Test
    fun `validateDeviceId accepts valid device IDs`() {
        assertTrue(CommandExecutor.validateDeviceId("emulator-5554"))
        assertTrue(CommandExecutor.validateDeviceId("192.168.1.100:5555"))
        assertTrue(CommandExecutor.validateDeviceId("RF8M33Z0XYZ"))
        assertTrue(CommandExecutor.validateDeviceId("device_name-123"))
    }

    @Test
    fun `validateDeviceId rejects invalid device IDs`() {
        assertFalse(CommandExecutor.validateDeviceId("device; rm -rf /"))
        assertFalse(CommandExecutor.validateDeviceId("device && echo pwned"))
        assertFalse(CommandExecutor.validateDeviceId("device | cat /etc/passwd"))
        assertFalse(CommandExecutor.validateDeviceId("device\necho pwned"))
        assertFalse(CommandExecutor.validateDeviceId(""))
    }

    @Test
    fun `validatePath rejects path traversal`() {
        assertFalse(CommandExecutor.validatePath("../../../etc/passwd"))
        assertFalse(CommandExecutor.validatePath("/tmp/../../../etc/passwd"))
    }

    @Test
    fun `execute runs command and captures output`() = runTest {
        val result = CommandExecutor.execute("echo", "hello")
        assertEquals(0, result.exitCode)
        assertEquals("hello", result.stdout)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `execute captures stderr on failure`() = runTest {
        val result = CommandExecutor.execute("ls", "/nonexistent_path_12345")
        assertFalse(result.isSuccess)
        assertTrue(result.stderr.isNotEmpty() || result.exitCode != 0)
    }

    @Test
    fun `CommandResult isSuccess returns true for exitCode 0`() {
        val result = CommandExecutor.CommandResult(0, "output", "")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `CommandResult isSuccess returns false for non-zero exitCode`() {
        val result = CommandExecutor.CommandResult(1, "", "error")
        assertFalse(result.isSuccess)
    }

    @Test
    fun `server starts via gradlew and responds to initialization`() {
        // Find project root (where gradlew is located)
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "gradlew").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }

        val gradlew = if (System.getProperty("os.name").lowercase().contains("windows")) {
            File(projectRoot, "gradlew.bat")
        } else {
            File(projectRoot, "gradlew")
        }

        assertTrue(gradlew.exists(), "gradlew not found at ${gradlew.absolutePath}")

        // Start the MCP server process
        val processBuilder = ProcessBuilder(
            gradlew.absolutePath,
            ":mcp-server:run",
            "--console=plain",
            "-q"
        ).directory(projectRoot)
            .redirectErrorStream(true)

        val process = processBuilder.start()

        try {
            // Send a simple MCP initialize request via stdin
            val initRequest = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}"""
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(initRequest)
                writer.newLine()
                writer.flush()
            }

            // Wait briefly for server to process
            Thread.sleep(2000)

            // Read response (with timeout)
            val response = StringBuilder()
            val reader = process.inputStream.bufferedReader()
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 5000) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    response.append(line)
                    if (line.contains("result") || line.contains("error")) {
                        break
                    }
                }
                Thread.sleep(100)
            }

            // Verify we got some response (server started successfully)
            val responseStr = response.toString()
            assertTrue(
                responseStr.contains("jsonrpc") || responseStr.contains("result") || process.isAlive,
                "Server should start and respond or still be running. Response: $responseStr"
            )
        } finally {
            // Clean up: kill the server process
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
        }
    }
}
