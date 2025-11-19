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
            id = "test-id",
            status = "processing",
            created_at = "2025-11-19"
        )
        val response = ImageGenerationResponse(data = jobData)

        assertNotNull(response)
        assertEquals("test-id", response.data.id)
    }

    @Test
    fun imageStatusResponse_canBeInstantiated() {
        val jobResult = ImageJobResult(
            id = "test-id",
            status = "completed",
            image = ImageResult(url = "https://example.com/image.png")
        )
        val response = ImageStatusResponse(data = jobResult)

        assertNotNull(response)
        assertEquals("completed", response.data.status)
        assertNotNull(response.data.image)
    }

    @Test
    fun imageResult_handlesNullBase64() {
        val imageResult = ImageResult(
            url = "https://example.com/image.png",
            base64 = null
        )

        assertNotNull(imageResult.url)
        assertNull(imageResult.base64)
    }

    @Test
    fun imageStatusResponse_handlesNullImage() {
        val jobResult = ImageJobResult(
            id = "test-id",
            status = "processing",
            image = null
        )
        val response = ImageStatusResponse(data = jobResult)

        assertNotNull(response)
        assertNull(response.data.image)
    }
}
