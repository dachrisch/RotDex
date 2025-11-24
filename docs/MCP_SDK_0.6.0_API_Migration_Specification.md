# MCP Kotlin SDK 0.6.0 API Migration Specification

## Document Overview

This document specifies the required changes to migrate the RotDex MCP server from incorrect API usage to the correct MCP Kotlin SDK 0.6.0 API patterns.

**Target File**: `/home/cda/dev/playground/RotDex/mcp-server/src/main/kotlin/com/rotdex/mcp/server/RotDexMcpServer.kt`

**SDK Version**: `io.modelcontextprotocol:kotlin-sdk:0.6.0`

---

## 1. Import Statement Corrections

### Current (Incorrect) Imports

```kotlin
import io.modelcontextprotocol.kotlin.sdk.Server                    // WRONG: Server is in .server subpackage
import io.modelcontextprotocol.kotlin.sdk.ServerOptions             // WRONG: ServerOptions is in .server subpackage
import io.modelcontextprotocol.kotlin.sdk.Implementation            // OK: Core package
import io.modelcontextprotocol.kotlin.sdk.Tool                      // OK: Core package
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport  // OK: Correct location
```

### Required (Correct) Imports

```kotlin
// Server-side classes (from .server subpackage)
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport

// Core types (from root .sdk package)
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent

// kotlinx.io for stream conversion (required for StdioServerTransport)
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
```

### Complete Import Block for RotDexMcpServer.kt

```kotlin
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

// Coroutines
import kotlinx.coroutines.runBlocking

// JSON serialization
import kotlinx.serialization.json.*
```

---

## 2. Server Instantiation Pattern

### Current (Incorrect) Pattern

```kotlin
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
```

**Issues:**
- Constructor parameters are positional without named arguments
- Missing explicit parameter names required by SDK

### Required (Correct) Pattern

```kotlin
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
```

**Key Changes:**
- Use named parameters: `serverInfo = ` and `options = `
- Import `ServerCapabilities` properly so fully qualified names are not needed

---

## 3. Tool Registration API

### 3.1 Simple Tools (No Input Parameters)

#### Current (Incorrect) Pattern

```kotlin
server.addTool(
    name = "list_devices",
    description = "List all connected Android devices via ADB"
) { _ ->
    runBlocking {
        val result = AdbTools.listDevices()
        toolResult(result.isSuccess, if (result.isSuccess) result.stdout else result.stderr)
    }
}
```

**Issues:**
- Lambda parameter is typed incorrectly (using `_` as JsonObject)
- The handler lambda must be a suspending function or properly handle the request object

#### Required (Correct) Pattern

```kotlin
server.addTool(
    name = "list_devices",
    description = "List all connected Android devices via ADB"
) {
    // Handler is already in a coroutine context, no need for runBlocking
    val result = AdbTools.listDevices()
    CallToolResult(
        content = listOf(
            TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
        ),
        isError = !result.isSuccess
    )
}
```

**Key Changes:**
- Remove `runBlocking` - the handler is already within a coroutine context
- Return `CallToolResult` directly (no custom helper method needed)
- Use `TextContent` for the response content
- Handler lambda has no explicit parameter when not using arguments

### 3.2 Tools with Input Parameters

#### Current (Incorrect) Pattern

```kotlin
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
```

**Issues:**
- Lambda parameter `args` is incorrectly typed
- The handler receives a `request` object, not a `JsonObject` directly
- Arguments are accessed via `request.arguments`, not directly

#### Required (Correct) Pattern

```kotlin
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
```

**Key Changes:**
- Lambda parameter is `request` (not `args`)
- Access arguments via `request.arguments["paramName"]`
- Remove `runBlocking` wrapper
- Return `CallToolResult` directly

---

## 4. Tool Handler Lambda Typing

### Handler Signature

The `addTool` method accepts a handler with this effective signature:

```kotlin
suspend (CallToolRequest) -> CallToolResult
```

Where:
- `CallToolRequest` contains:
  - `arguments: Map<String, JsonElement>?` - The tool invocation arguments
  - Other request metadata

### Accessing Arguments

```kotlin
// For string parameters
val stringParam = request.arguments["paramName"]?.jsonPrimitive?.content ?: "default"

// For integer parameters
val intParam = request.arguments["paramName"]?.jsonPrimitive?.intOrNull ?: 0

// For boolean parameters
val boolParam = request.arguments["paramName"]?.jsonPrimitive?.booleanOrNull ?: false

// Null check with early return
val requiredParam = request.arguments["paramName"]?.jsonPrimitive?.content
    ?: return@addTool CallToolResult(
        content = listOf(TextContent(text = "Required parameter 'paramName' is missing")),
        isError = true
    )
```

---

## 5. CallToolResult Construction

### Structure

```kotlin
CallToolResult(
    content = listOf(
        TextContent(text = "Response message here")
    ),
    isError = false  // or true for error responses
)
```

### For Success Responses

```kotlin
CallToolResult(
    content = listOf(TextContent(text = "Operation completed successfully")),
    isError = false
)
```

### For Error Responses

```kotlin
CallToolResult(
    content = listOf(TextContent(text = "Error: Operation failed - details here")),
    isError = true
)
```

### Multiple Content Items

```kotlin
CallToolResult(
    content = listOf(
        TextContent(text = "Line 1"),
        TextContent(text = "Line 2"),
        TextContent(text = "Line 3")
    ),
    isError = false
)
```

---

## 6. Server Startup with StdioServerTransport

### Current (Incorrect) Pattern

```kotlin
suspend fun start() {
    val transport = StdioServerTransport()
    server.connect(transport)
}
```

**Issues:**
- `StdioServerTransport` requires input/output stream parameters in SDK 0.6.0

### Required (Correct) Pattern

```kotlin
suspend fun start() {
    val transport = StdioServerTransport(
        input = System.`in`.asSource().buffered(),
        output = System.out.asSink().buffered()
    )
    server.connect(transport)
}
```

**Key Changes:**
- Provide explicit `input` and `output` parameters
- Use kotlinx.io extension functions to convert Java streams:
  - `System.in.asSource().buffered()` for input
  - `System.out.asSink().buffered()` for output

### Required Dependencies in build.gradle.kts

Ensure the kotlinx-io dependency is present:

```kotlin
dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.4")  // Required for asSource/asSink
}
```

---

## 7. Complete Refactored Code Example

Here is a representative subset of the refactored `RotDexMcpServer.kt`:

```kotlin
package com.rotdex.mcp.server

import com.rotdex.mcp.tools.AdbTools
import com.rotdex.mcp.tools.GradleTools
import com.rotdex.mcp.tools.LogcatTools

// MCP SDK imports
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent

// kotlinx.io imports for stream conversion
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

// JSON serialization
import kotlinx.serialization.json.*

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
        // Simple tool without parameters
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

        // Tool with input parameters
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
                        "description" to JsonPrimitive("Path to the APK file (optional)")
                    ))
                )),
                required = listOf("device_id")
            )
        ) { request ->
            val deviceId = request.arguments["device_id"]?.jsonPrimitive?.content ?: ""
            val apkPath = request.arguments["apk_path"]?.jsonPrimitive?.content
                ?: GradleTools.getDebugApkPath()
            val result = AdbTools.installApk(deviceId, apkPath)
            CallToolResult(
                content = listOf(
                    TextContent(text = if (result.isSuccess) result.stdout else result.stderr)
                ),
                isError = !result.isSuccess
            )
        }

        // ... additional tools follow the same pattern
    }

    suspend fun start() {
        val transport = StdioServerTransport(
            input = System.`in`.asSource().buffered(),
            output = System.out.asSink().buffered()
        )
        server.connect(transport)
    }
}
```

---

## 8. Summary of All Required Changes

| Component | Current (Incorrect) | Required (Correct) |
|-----------|--------------------|--------------------|
| Server import | `io.modelcontextprotocol.kotlin.sdk.Server` | `io.modelcontextprotocol.kotlin.sdk.server.Server` |
| ServerOptions import | `io.modelcontextprotocol.kotlin.sdk.ServerOptions` | `io.modelcontextprotocol.kotlin.sdk.server.ServerOptions` |
| Server constructor | Positional parameters | Named: `serverInfo = `, `options = ` |
| Tool handler (no args) | `{ _ -> runBlocking { ... } }` | `{ ... }` (no runBlocking) |
| Tool handler (with args) | `{ args -> args["key"] }` | `{ request -> request.arguments["key"] }` |
| Result construction | `toolResult(success, message)` | `CallToolResult(content = listOf(TextContent(text = ...)), isError = ...)` |
| StdioServerTransport | `StdioServerTransport()` | `StdioServerTransport(input = ..., output = ...)` |

---

## 9. Additional Dependencies Required

Add to `mcp-server/build.gradle.kts`:

```kotlin
dependencies {
    // MCP Kotlin SDK (already present)
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")

    // kotlinx-io for stream conversion (MAY be needed if not transitively included)
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.4")
}
```

---

## 10. References

- [MCP Kotlin SDK GitHub Repository](https://github.com/modelcontextprotocol/kotlin-sdk)
- [MCP Kotlin SDK Documentation](https://modelcontextprotocol.github.io/kotlin-sdk/)
- [kotlinx-io Documentation](https://github.com/Kotlin/kotlinx-io)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
