package com.rotdex.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * API service for AI image generation using DeepSeek API
 * Uses DeepSeek's image generation endpoint
 */
interface AiApiService {

    @Headers("Content-Type: application/json")
    @POST("images/generations")
    suspend fun generateImage(
        @Body request: ImageGenerationRequest
    ): Response<ImageGenerationResponse>
}

/**
 * Request model for image generation using DeepSeek API
 */
data class ImageGenerationRequest(
    val prompt: String,
    val model: String = "deepseek-image",
    val size: String = "1024x1024",
    val quality: String = "standard",
    val n: Int = 1,
    val response_format: String = "url"
)

/**
 * Response model from AI API
 */
data class ImageGenerationResponse(
    val created: Long,
    val data: List<GeneratedImage>
)

data class GeneratedImage(
    val url: String,
    val revisedPrompt: String? = null
)

/**
 * API error response
 */
data class ApiError(
    val message: String,
    val type: String,
    val code: String?
)
