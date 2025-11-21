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
    fun `ImageGenerationResponse deserializes correctly when in progress`() {
        val json = """
            {
                "data": {
                    "task_id": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
                    "status": "IN_PROGRESS",
                    "generated": []
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ImageGenerationResponse::class.java)

        assertNotNull(response)
        assertEquals("046b6c7f-0b8a-43b9-b35d-6489e6daee91", response.data.task_id)
        assertEquals(ImageJobStatus.IN_PROGRESS, response.data.status)
        assertTrue(response.data.generated.isEmpty())
    }

    @Test
    fun `ImageStatusResponse deserializes correctly when completed`() {
        val json = """
            {
                "data": {
                    "task_id": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
                    "status": "COMPLETED",
                    "generated": ["https://example.com/image.png"],
                    "has_nsfw": [false]
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ImageStatusResponse::class.java)

        assertNotNull(response)
        assertEquals("046b6c7f-0b8a-43b9-b35d-6489e6daee91", response.data.task_id)
        assertEquals(ImageJobStatus.COMPLETED, response.data.status)
        assertEquals(1, response.data.generated.size)
        assertEquals("https://example.com/image.png", response.data.generated.first())
        assertEquals(false, response.data.has_nsfw?.first())
    }

    @Test
    fun `ImageStatusResponse deserializes correctly when failed`() {
        val json = """
            {
                "data": {
                    "task_id": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
                    "status": "FAILED",
                    "generated": []
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ImageStatusResponse::class.java)

        assertNotNull(response)
        assertEquals("046b6c7f-0b8a-43b9-b35d-6489e6daee91", response.data.task_id)
        assertEquals(ImageJobStatus.FAILED, response.data.status)
        assertTrue(response.data.generated.isEmpty())
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
