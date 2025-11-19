package com.rotdex.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API service for AI image generation using Freepik Mystic API
 * Uses Freepik's Mystic model for image generation
 */
interface AiApiService {

    @Headers("Content-Type: application/json")
    @POST("v1/ai/mystic")
    suspend fun generateImage(
        @Body request: ImageGenerationRequest
    ): Response<ImageGenerationResponse>

    @GET("v1/ai/mystic/{id}")
    suspend fun checkImageStatus(
        @Path("id") id: String
    ): Response<ImageStatusResponse>
}

/**
 * Request model for Freepik Mystic API
 */
data class ImageGenerationRequest(
    val prompt: String,
    val aspect_ratio: String = "square_1_1",
    val model: String = "realism",
    val filter_nsfw: Boolean = true
) {
    companion object {
        fun fromPrompt(prompt: String): ImageGenerationRequest {
            return ImageGenerationRequest(
                prompt = prompt,
                aspect_ratio = "square_1_1",
                model = "realism",
                filter_nsfw = true
            )
        }
    }
}

/**
 * Image generation job status
 */
enum class ImageJobStatus {
    @SerializedName("IN_PROGRESS")
    IN_PROGRESS,

    @SerializedName("COMPLETED")
    COMPLETED,

    @SerializedName("FAILED")
    FAILED
}

/**
 * Response model from Freepik Mystic API (initial generation request and status check)
 * Both endpoints return the same structure
 */
data class ImageGenerationResponse(
    val data: ImageJobData
)

data class ImageJobData(
    val task_id: String,
    val status: ImageJobStatus,
    val generated: List<String>,  // Array of image URLs
    val has_nsfw: List<Boolean>? = null  // Optional NSFW detection results
)

/**
 * Response model for checking image generation status
 * Same structure as ImageGenerationResponse
 */
typealias ImageStatusResponse = ImageGenerationResponse

/**
 * API error response
 */
data class ApiError(
    val message: String,
    val type: String,
    val code: String?
)
