package com.rotdex.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * API service for AI image generation using Google Gemini Imagen API
 * Uses Google's Imagen model for image generation
 */
interface AiApiService {

    @Headers("Content-Type: application/json")
    @POST("models/imagen-3.0-generate-001:predict")
    suspend fun generateImage(
        @Body request: ImageGenerationRequest
    ): Response<ImageGenerationResponse>
}

/**
 * Request model for Google Imagen API
 */
data class ImageGenerationRequest(
    val instances: List<ImagePromptInstance>,
    val parameters: ImageGenerationParameters? = ImageGenerationParameters()
) {
    companion object {
        fun fromPrompt(prompt: String): ImageGenerationRequest {
            return ImageGenerationRequest(
                instances = listOf(ImagePromptInstance(prompt = prompt)),
                parameters = ImageGenerationParameters()
            )
        }
    }
}

data class ImagePromptInstance(
    val prompt: String
)

data class ImageGenerationParameters(
    val sampleCount: Int = 1,
    val aspectRatio: String = "1:1",
    val compressionQuality: String? = null,
    val negativePrompt: String? = null,
    val language: String = "en"
)

/**
 * Response model from Google Imagen API
 */
data class ImageGenerationResponse(
    val predictions: List<ImagePrediction>? = null,
    val metadata: ResponseMetadata? = null
)

data class ImagePrediction(
    val bytesBase64Encoded: String,
    val mimeType: String? = "image/png"
)

data class ResponseMetadata(
    val tokenMetadata: Any? = null
)

/**
 * API error response
 */
data class ApiError(
    val message: String,
    val type: String,
    val code: String?
)
