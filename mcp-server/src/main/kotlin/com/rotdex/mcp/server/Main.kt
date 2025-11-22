package com.rotdex.mcp.server

import kotlinx.coroutines.runBlocking

/**
 * Entry point for the RotDex MCP Server.
 *
 * This server exposes Android development tools (ADB, Gradle, Logcat) via MCP,
 * allowing AI assistants like Claude Code to control the RotDex app during development.
 *
 * Usage:
 *   java -jar rotdex-mcp-server.jar
 *
 * Or via Gradle:
 *   ./gradlew :mcp-server:run
 */
fun main() = runBlocking {
    val server = RotDexMcpServer()
    server.start()
}
