package com.rotdex.mcp.server

import com.rotdex.mcp.tools.AdbTools
import com.rotdex.mcp.tools.GradleTools
import com.rotdex.mcp.tools.LogcatTools

// MCP SDK - Server module
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport

// MCP SDK - Core types
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent

// kotlinx.io for stdio stream conversion
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

// JSON serialization
import kotlinx.serialization.json.*

/**
 * MCP Server for RotDex Android development.
 * Exposes ADB, Gradle, and Logcat tools for AI-assisted development.
 */
class RotDexMcpServer {

    private val server = Server(
        serverInfo = Implementation(
            name = "rotdex-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    init {
        registerTools()
    }

    private fun registerTools() {
        // ADB Tools
        server.addTool(
            name = "list_devices",
            description = "List all connected Android devices via ADB"
        ) {
            val result = AdbTools.listDevices()
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "install_apk",
            description = "Install the RotDex APK to a connected device",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID from list_devices")
                    )),
                    "apk_path" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("Path to the APK file (optional, defaults to debug APK)")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val apkPath = request.arguments["apk_path"]?.jsonPrimitive?.content ?: GradleTools.getDebugApkPath()
            val result = AdbTools.installApk(deviceId, apkPath)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "launch_app",
            description = "Launch the RotDex app on a device",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = AdbTools.launchApp(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "App launched" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "stop_app",
            description = "Force stop the RotDex app on a device",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = AdbTools.stopApp(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "App stopped" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "clear_app_data",
            description = "Clear all data for the RotDex app (resets to fresh install state)",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = AdbTools.clearAppData(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "App data cleared" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "take_screenshot",
            description = "Take a screenshot from the device",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    )),
                    "output_path" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("Where to save the screenshot")
                    ))
                )),
                required = listOf("device_id", "output_path")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val outputPath = request.arguments["output_path"]?.jsonPrimitive?.content ?: ""
            val result = AdbTools.takeScreenshot(deviceId, outputPath)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "device_info",
            description = "Get information about a connected device",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = AdbTools.getDeviceInfo(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        // Gradle Tools
        server.addTool(
            name = "build_debug",
            description = "Build the debug APK using Gradle"
        ) {
            val result = GradleTools.assembleDebug()
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "Debug APK built at ${GradleTools.getDebugApkPath()}" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "build_release",
            description = "Build the release APK using Gradle"
        ) {
            val result = GradleTools.assembleRelease()
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "Release APK built at ${GradleTools.getReleaseApkPath()}" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "clean",
            description = "Clean the Gradle build"
        ) {
            val result = GradleTools.clean()
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "Build cleaned" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "run_tests",
            description = "Run all unit tests"
        ) {
            val result = GradleTools.runTests()
            CallToolResult(
                content = listOf(
                    TextContent(text = result.stdout.ifBlank { result.stderr })
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "run_test_class",
            description = "Run a specific test class",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "test_class" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("Fully qualified test class name (e.g., com.rotdex.data.api.FreepikApiModelsTest)")
                    ))
                )),
                required = listOf("test_class")
            )
        ) { request ->
            val testClass = request.arguments["test_class"]?.jsonPrimitive?.content ?: ""
            val result = GradleTools.runTestClass(testClass)
            CallToolResult(
                content = listOf(
                    TextContent(text = result.stdout.ifBlank { result.stderr })
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "run_instrumented_tests",
            description = "Run instrumented tests on a connected device"
        ) {
            val result = GradleTools.runInstrumentedTests()
            CallToolResult(
                content = listOf(
                    TextContent(text = result.stdout.ifBlank { result.stderr })
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "run_detekt",
            description = "Run Detekt static analysis"
        ) {
            val result = GradleTools.runDetekt()
            CallToolResult(
                content = listOf(
                    TextContent(text = result.stdout.ifBlank { result.stderr })
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "build_and_install",
            description = "Build debug APK and install it on a device in one step",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID to install to")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = GradleTools.buildAndInstall(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "Built and installed successfully" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        // Logcat Tools
        server.addTool(
            name = "get_app_logs",
            description = "Get recent logs from the RotDex app",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    )),
                    "lines" to JsonObject(mapOf(
                        "type" to JsonPrimitive("integer"),
                        "description" to JsonPrimitive("Number of log lines to retrieve (default 100)")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val lines = request.arguments["lines"]?.jsonPrimitive?.intOrNull ?: 100
            val result = LogcatTools.getAppLogs(deviceId, lines)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "get_error_logs",
            description = "Get error-level logs from the device",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = LogcatTools.getErrorLogs(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "get_crash_logs",
            description = "Get crash logs (AndroidRuntime errors)",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = LogcatTools.getCrashLogs(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "search_logs",
            description = "Search logs for a specific pattern",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    )),
                    "pattern" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The text pattern to search for")
                    ))
                )),
                required = listOf("device_id", "pattern")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val pattern = request.arguments["pattern"]?.jsonPrimitive?.content ?: ""
            val result = LogcatTools.searchLogs(deviceId, pattern)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        server.addTool(
            name = "clear_logs",
            description = "Clear the logcat buffer",
            inputSchema = Tool.Input(
                properties = JsonObject(mapOf(
                    "device_id" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("The device ID")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val result = LogcatTools.clearLogs(deviceId)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) "Logs cleared" else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }
    }

    suspend fun start() {
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
        server.connect(transport)
    }
}
