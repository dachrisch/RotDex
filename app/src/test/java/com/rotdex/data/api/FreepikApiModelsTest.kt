package com.rotdex.data.api

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Freepik API models
 * These tests catch serialization issues and verify model structure
 */
class FreepikApiModelsTest {

    private val gson = Gson()

    @Test
    fun `ImageGenerationRequest serializes correctly`() {
        val request = ImageGenerationRequest.fromPrompt("test prompt")
        val json = gson.toJson(request)

        assertTrue(json.contains("\"prompt\":\"test prompt\""))
        assertTrue(json.contains("\"aspect_ratio\":\"square_1_1\""))
        assertTrue(json.contains("\"model\":\"realism\""))
        assertTrue(json.contains("\"filter_nsfw\":true"))
    }

    @Test
    fun `ImageGenerationResponse deserializes correctly`() {
        val json = """
            {
                "data": {
                    "id": "test-job-123",
                    "status": "processing",
                    "created_at": "2025-11-19T00:00:00Z"
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ImageGenerationResponse::class.java)

        assertNotNull(response)
        assertEquals("test-job-123", response.data.id)
        assertEquals("processing", response.data.status)
        assertEquals("2025-11-19T00:00:00Z", response.data.created_at)
    }

    @Test
    fun `ImageStatusResponse deserializes correctly when processing`() {
        val json = """
            {
                "data": {
                    "id": "test-job-123",
                    "status": "processing",
                    "image": null
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ImageStatusResponse::class.java)

        assertNotNull(response)
        assertEquals("test-job-123", response.data.id)
        assertEquals("processing", response.data.status)
        assertNull(response.data.image)
    }

    @Test
    fun `ImageStatusResponse deserializes correctly when completed`() {
        val json = """
            {
                "data": {
                    "id": "test-job-123",
                    "status": "completed",
                    "image": {
                        "url": "https://example.com/image.png",
                        "base64": null
                    }
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ImageStatusResponse::class.java)

        assertNotNull(response)
        assertEquals("test-job-123", response.data.id)
        assertEquals("completed", response.data.status)
        assertNotNull(response.data.image)
        assertEquals("https://example.com/image.png", response.data.image?.url)
    }

    @Test
    fun `ImageStatusResponse deserializes correctly when failed`() {
        val json = """
            {
                "data": {
                    "id": "test-job-123",
                    "status": "failed",
                    "image": null
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ImageStatusResponse::class.java)

        assertNotNull(response)
        assertEquals("test-job-123", response.data.id)
        assertEquals("failed", response.data.status)
        assertNull(response.data.image)
    }

    @Test
    fun `ApiError deserializes correctly`() {
        val json = """
            {
                "message": "Invalid API key",
                "type": "authentication_error",
                "code": "401"
            }
        """.trimIndent()

        val error = gson.fromJson(json, ApiError::class.java)

        assertNotNull(error)
        assertEquals("Invalid API key", error.message)
        assertEquals("authentication_error", error.type)
        assertEquals("401", error.code)
    }

    @Test
    fun `fromPrompt creates valid request`() {
        val prompt = "a cat with laser eyes"
        val request = ImageGenerationRequest.fromPrompt(prompt)

        assertEquals(prompt, request.prompt)
        assertEquals("square_1_1", request.aspect_ratio)
        assertEquals("realism", request.model)
        assertTrue(request.filter_nsfw)
    }
}
