package com.rotdex.mcp

import com.rotdex.mcp.server.RotDexMcpServer
import com.rotdex.mcp.tools.CommandExecutor
import kotlinx.coroutines.test.runTest
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
}
