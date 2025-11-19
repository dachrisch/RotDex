package com.rotdex.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * API service for AI image generation using Google Gemini native image generation
 * Uses Gemini 2.5 Flash Image model
 */
interface AiApiService {

    @Headers("Content-Type: application/json")
    @POST("models/gemini-2.5-flash-image:generateContent")
    suspend fun generateImage(
        @Body request: ImageGenerationRequest
    ): Response<ImageGenerationResponse>
}

/**
 * Request model for Gemini generateContent API
 */
data class ImageGenerationRequest(
    val contents: String
) {
    companion object {
        fun fromPrompt(prompt: String): ImageGenerationRequest {
            return ImageGenerationRequest(contents = prompt)
        }
    }
}

/**
 * Response model from Gemini generateContent API
 */
data class ImageGenerationResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: Content
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String  // Base64-encoded image
)

/**
 * API error response
 */
data class ApiError(
    val message: String,
    val type: String,
    val code: String?
)
