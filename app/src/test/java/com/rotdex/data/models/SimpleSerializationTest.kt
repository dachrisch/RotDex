package com.rotdex.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*

/**
 * Simple test to verify kotlinx.serialization is working
 */
class SimpleSerializationTest {

    @Serializable
    data class SimpleState(
        val id: String = "test",
        val version: Int = 0
    )

    // Configure Json to encode default values
    private val json = Json { encodeDefaults = true }

    @Test
    fun `simple serialization works`() {
        val state = SimpleState()
        // Try manual serialization without catching
        val jsonString = json.encodeToString(SimpleState.serializer(), state)

        println("Simple JSON: $jsonString")
        assertTrue("JSON should not be empty", jsonString.isNotEmpty())
        assertTrue("JSON should contain 'test'", jsonString.contains("test"))
    }
}
