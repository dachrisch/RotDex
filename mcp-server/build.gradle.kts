plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "com.rotdex"
version = "1.0.0"

application {
    mainClass.set("com.rotdex.mcp.server.MainKt")
}

dependencies {
    // MCP Kotlin SDK
    implementation("io.modelcontextprotocol:kotlin-sdk:0.8.0")

    // Ktor for HTTP client (needed by MCP SDK)
    implementation("io.ktor:ktor-client-cio:3.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.21")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.rotdex.mcp.server.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
