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
            task_id = "test-task-id",
            status = "IN_PROGRESS",
            generated = emptyList()
        )
        assertEquals("test-task-id", jobData.task_id)
        assertEquals("IN_PROGRESS", jobData.status)
        assertTrue(jobData.generated.isEmpty())
    }

    @Test
    fun `verify GeneratedImage type is accessible`() {
        // This test will fail to compile if GeneratedImage is not properly defined
        val generatedImage = GeneratedImage(
            url = "https://example.com/image.png",
            base64 = null
        )
        assertEquals("https://example.com/image.png", generatedImage.url)
        assertNull(generatedImage.base64)
    }

    @Test
    fun `verify ImageJobData with generated images is accessible`() {
        // This test will fail to compile if the structure is not properly defined
        val jobData = ImageJobData(
            task_id = "test-task-id",
            status = "COMPLETED",
            generated = listOf(
                GeneratedImage(url = "https://example.com/image.png")
            )
        )
        assertEquals("COMPLETED", jobData.status)
        assertEquals(1, jobData.generated.size)
        assertEquals("https://example.com/image.png", jobData.generated.first().url)
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
        val generatedImage = GeneratedImage(
            url = "https://example.com/image.png",
            base64 = null
        )

        val jobData = ImageJobData(
            task_id = "task-123",
            status = "COMPLETED",
            generated = listOf(generatedImage)
        )

        val generationResponse = ImageGenerationResponse(
            data = jobData
        )

        // Status response uses the same structure (typealias)
        val statusResponse: ImageStatusResponse = generationResponse

        // Verify the chain
        assertEquals("task-123", generationResponse.data.task_id)
        assertEquals("COMPLETED", generationResponse.data.status)
        assertEquals(1, generationResponse.data.generated.size)
        assertEquals("https://example.com/image.png", generationResponse.data.generated.first().url)

        // Verify typealias works
        assertEquals("task-123", statusResponse.data.task_id)
    }
}
