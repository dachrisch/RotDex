package com.rotdex.data.repository

import com.rotdex.data.api.AiApiService
import com.rotdex.data.api.ImageGenerationResponse
import com.rotdex.data.api.ImageStatusResponse
import com.rotdex.data.api.ImageJobData
import com.rotdex.data.api.ImageJobResult
import com.rotdex.data.api.ImageResult
import org.junit.Test
import org.junit.Assert.*

/**
 * Compilation verification tests for CardRepository
 * These tests catch missing imports and type resolution issues at compile time
 */
class CardRepositoryCompilationTest {

    @Test
    fun `verify ImageGenerationResponse type is accessible`() {
        // This test will fail to compile if ImageGenerationResponse is not properly imported
        val response: ImageGenerationResponse? = null
        assertNull(response)
    }

    @Test
    fun `verify ImageStatusResponse type is accessible`() {
        // This test will fail to compile if ImageStatusResponse is not properly imported
        val response: ImageStatusResponse? = null
        assertNull(response)
    }

    @Test
    fun `verify ImageJobData type is accessible`() {
        // This test will fail to compile if ImageJobData is not properly defined
        val jobData = ImageJobData(
            id = "test-id",
            status = "processing",
            created_at = "2025-11-19T00:00:00Z"
        )
        assertEquals("test-id", jobData.id)
        assertEquals("processing", jobData.status)
    }

    @Test
    fun `verify ImageJobResult type is accessible`() {
        // This test will fail to compile if ImageJobResult is not properly defined
        val jobResult = ImageJobResult(
            id = "test-id",
            status = "completed",
            image = ImageResult(url = "https://example.com/image.png")
        )
        assertEquals("test-id", jobResult.id)
        assertEquals("completed", jobResult.status)
        assertNotNull(jobResult.image)
    }

    @Test
    fun `verify ImageResult type is accessible`() {
        // This test will fail to compile if ImageResult is not properly defined
        val imageResult = ImageResult(
            url = "https://example.com/image.png",
            base64 = null
        )
        assertEquals("https://example.com/image.png", imageResult.url)
        assertNull(imageResult.base64)
    }

    @Test
    fun `verify AiApiService interface is accessible`() {
        // This test will fail to compile if AiApiService methods are not properly defined
        val serviceClass = AiApiService::class.java

        // Verify generateImage method exists
        val generateImageMethod = serviceClass.methods.find {
            it.name == "generateImage"
        }
        assertNotNull("generateImage method should exist", generateImageMethod)

        // Verify checkImageStatus method exists
        val checkStatusMethod = serviceClass.methods.find {
            it.name == "checkImageStatus"
        }
        assertNotNull("checkImageStatus method should exist", checkStatusMethod)
    }

    @Test
    fun `verify all Freepik API response types have proper structure`() {
        // Create a full response chain to verify all types work together
        val imageResult = ImageResult(
            url = "https://example.com/image.png",
            base64 = null
        )

        val jobResult = ImageJobResult(
            id = "job-123",
            status = "completed",
            image = imageResult
        )

        val statusResponse = ImageStatusResponse(
            data = jobResult
        )

        val jobData = ImageJobData(
            id = "job-123",
            status = "processing",
            created_at = "2025-11-19T00:00:00Z"
        )

        val generationResponse = ImageGenerationResponse(
            data = jobData
        )

        // Verify the chain
        assertNotNull(statusResponse.data.image)
        assertEquals("https://example.com/image.png", statusResponse.data.image?.url)
        assertEquals("job-123", generationResponse.data.id)
    }
}
