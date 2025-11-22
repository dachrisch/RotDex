package com.rotdex.mcp.server

import com.rotdex.mcp.tools.AdbTools
import com.rotdex.mcp.tools.GradleTools
import com.rotdex.mcp.tools.LogcatTools
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Server
import io.modelcontextprotocol.kotlin.sdk.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * MCP Server for RotDex Android development.
 * Exposes ADB, Gradle, and Logcat tools for AI-assisted development.
 */
class RotDexMcpServer {

    private val server = Server(
        Implementation(
            name = "rotdex-mcp-server",
            version = "1.0.0"
        ),
        ServerOptions(
            capabilities = io.modelcontextprotocol.kotlin.sdk.ServerCapabilities(
                tools = io.modelcontextprotocol.kotlin.sdk.ServerCapabilities.Tools(listChanged = true)
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
        ) { _ ->
            runBlocking {
                val result = AdbTools.listDevices()
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val apkPath = args["apk_path"]?.jsonPrimitive?.content ?: GradleTools.getDebugApkPath()
                val result = AdbTools.installApk(deviceId, apkPath)
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = AdbTools.launchApp(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) "App launched" else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = AdbTools.stopApp(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) "App stopped" else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = AdbTools.clearAppData(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) "App data cleared" else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val outputPath = args["output_path"]?.jsonPrimitive?.content ?: ""
                val result = AdbTools.takeScreenshot(deviceId, outputPath)
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = AdbTools.getDeviceInfo(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
        }

        // Gradle Tools
        server.addTool(
            name = "build_debug",
            description = "Build the debug APK using Gradle"
        ) { _ ->
            runBlocking {
                val result = GradleTools.assembleDebug()
                toolResult(result.isSuccess, if (result.isSuccess) "Debug APK built at ${GradleTools.getDebugApkPath()}" else result.stderr)
            }
        }

        server.addTool(
            name = "build_release",
            description = "Build the release APK using Gradle"
        ) { _ ->
            runBlocking {
                val result = GradleTools.assembleRelease()
                toolResult(result.isSuccess, if (result.isSuccess) "Release APK built at ${GradleTools.getReleaseApkPath()}" else result.stderr)
            }
        }

        server.addTool(
            name = "clean",
            description = "Clean the Gradle build"
        ) { _ ->
            runBlocking {
                val result = GradleTools.clean()
                toolResult(result.isSuccess, if (result.isSuccess) "Build cleaned" else result.stderr)
            }
        }

        server.addTool(
            name = "run_tests",
            description = "Run all unit tests"
        ) { _ ->
            runBlocking {
                val result = GradleTools.runTests()
                toolResult(result.isSuccess, result.stdout.ifBlank { result.stderr })
            }
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
        ) { args ->
            runBlocking {
                val testClass = args["test_class"]?.jsonPrimitive?.content ?: ""
                val result = GradleTools.runTestClass(testClass)
                toolResult(result.isSuccess, result.stdout.ifBlank { result.stderr })
            }
        }

        server.addTool(
            name = "run_instrumented_tests",
            description = "Run instrumented tests on a connected device"
        ) { _ ->
            runBlocking {
                val result = GradleTools.runInstrumentedTests()
                toolResult(result.isSuccess, result.stdout.ifBlank { result.stderr })
            }
        }

        server.addTool(
            name = "run_detekt",
            description = "Run Detekt static analysis"
        ) { _ ->
            runBlocking {
                val result = GradleTools.runDetekt()
                toolResult(result.isSuccess, result.stdout.ifBlank { result.stderr })
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = GradleTools.buildAndInstall(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) "Built and installed successfully" else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val lines = args["lines"]?.jsonPrimitive?.intOrNull ?: 100
                val result = LogcatTools.getAppLogs(deviceId, lines)
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = LogcatTools.getErrorLogs(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = LogcatTools.getCrashLogs(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val pattern = args["pattern"]?.jsonPrimitive?.content ?: ""
                val result = LogcatTools.searchLogs(deviceId, pattern)
                toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
            }
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
        ) { args ->
            runBlocking {
                val deviceId = args["device_id"]?.jsonPrimitive?.content ?: ""
                val result = LogcatTools.clearLogs(deviceId)
                toolResult(result.isSuccess, if (result.isSuccess) "Logs cleared" else result.stderr)
            }
        }
    }

    private fun toolResult(success: Boolean, message: String): io.modelcontextprotocol.kotlin.sdk.CallToolResult {
        return io.modelcontextprotocol.kotlin.sdk.CallToolResult(
            content = listOf(
                io.modelcontextprotocol.kotlin.sdk.TextContent(text = message)
            ),
            isError = !success
        )
    }

    suspend fun start() {
        val transport = StdioServerTransport()
        server.connect(transport)
    }
}
