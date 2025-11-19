package com.rotdex.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for Freepik API service
 * Verifies that dependency injection and API setup works correctly
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FreepikApiIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var aiApiService: AiApiService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun aiApiService_isInjectedCorrectly() {
        assertNotNull("AiApiService should be injected by Hilt", aiApiService)
    }

    @Test
    fun aiApiService_hasCorrectType() {
        assertTrue(
            "AiApiService should be the correct interface",
            aiApiService is AiApiService
        )
    }

    @Test
    fun imageGenerationRequest_canBeCreatedFromPrompt() {
        val prompt = "test prompt"
        val request = ImageGenerationRequest.fromPrompt(prompt)

        assertNotNull(request)
        assertEquals(prompt, request.prompt)
        assertEquals("square_1_1", request.aspect_ratio)
        assertEquals("realism", request.model)
        assertTrue(request.filter_nsfw)
    }

    @Test
    fun imageGenerationResponse_canBeInstantiated() {
        val jobData = ImageJobData(
            task_id = "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
            status = "IN_PROGRESS",
            generated = emptyList()
        )
        val response = ImageGenerationResponse(data = jobData)

        assertNotNull(response)
        assertEquals("046b6c7f-0b8a-43b9-b35d-6489e6daee91", response.data.task_id)
        assertEquals("IN_PROGRESS", response.data.status)
    }

    @Test
    fun imageStatusResponse_canBeInstantiated() {
        val jobData = ImageJobData(
            task_id = "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
            status = "COMPLETED",
            generated = listOf("https://example.com/image.png"),
            has_nsfw = listOf(false)
        )
        val response: ImageStatusResponse = ImageGenerationResponse(data = jobData)

        assertNotNull(response)
        assertEquals("COMPLETED", response.data.status)
        assertEquals(1, response.data.generated.size)
        assertEquals("https://example.com/image.png", response.data.generated.first())
    }

    @Test
    fun imageJobData_handlesMultipleGeneratedUrls() {
        val jobData = ImageJobData(
            task_id = "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
            status = "COMPLETED",
            generated = listOf(
                "https://example.com/image1.png",
                "https://example.com/image2.png"
            )
        )

        assertEquals(2, jobData.generated.size)
        assertEquals("https://example.com/image1.png", jobData.generated[0])
        assertEquals("https://example.com/image2.png", jobData.generated[1])
    }

    @Test
    fun imageStatusResponse_handlesEmptyGenerated() {
        val jobData = ImageJobData(
            task_id = "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
            status = "IN_PROGRESS",
            generated = emptyList()
        )
        val response: ImageStatusResponse = ImageGenerationResponse(data = jobData)

        assertNotNull(response)
        assertTrue(response.data.generated.isEmpty())
    }

    @Test
    fun imageJobData_handlesOptionalHasNsfw() {
        val jobDataWithNsfw = ImageJobData(
            task_id = "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
            status = "COMPLETED",
            generated = listOf("https://example.com/image.png"),
            has_nsfw = listOf(false)
        )
        assertNotNull(jobDataWithNsfw.has_nsfw)

        val jobDataWithoutNsfw = ImageJobData(
            task_id = "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
            status = "IN_PROGRESS",
            generated = emptyList()
        )
        assertNull(jobDataWithoutNsfw.has_nsfw)
    }
}
