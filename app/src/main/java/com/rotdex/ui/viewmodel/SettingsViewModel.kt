package com.rotdex.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.models.UserProfile
import com.rotdex.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 *
 * Manages:
 * - Player name updates
 * - Avatar image selection and processing
 * - User profile state
 *
 * Avatar images are:
 * - Resized to 256x256 for consistency
 * - Saved to app's private storage (/files/avatars/)
 * - Stored as JPEG with 90% quality
 * - Old avatars are automatically deleted when replaced
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * Current user profile state
     * Exposed as StateFlow for Compose observation
     */
    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
        .map { it as UserProfile? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Update the player's display name
     *
     * @param name New player name (will be trimmed and validated by repository)
     */
    fun updatePlayerName(name: String) {
        viewModelScope.launch {
            userRepository.updatePlayerName(name)
        }
    }

    /**
     * Update the player's avatar image from a URI
     *
     * Process:
     * 1. Load image from URI using ContentResolver
     * 2. Resize to 256x256 for performance and storage efficiency
     * 3. Delete old avatar if exists
     * 4. Save new avatar to /files/avatars/ directory
     * 5. Update user profile with new file path
     *
     * Error handling:
     * - Silently fails if URI is invalid or I/O error occurs
     * - Logs error for debugging (in production, consider user notification)
     *
     * @param uri Content URI of the selected image (from photo picker)
     */
    fun updateAvatar(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load bitmap from URI
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Resize to consistent size (256x256)
                val resized = Bitmap.createScaledBitmap(bitmap, 256, 256, true)

                // Ensure avatars directory exists
                val avatarsDir = File(context.filesDir, "avatars")
                avatarsDir.mkdirs()

                // Delete old avatar if exists
                val oldPath = userRepository.getAvatarImagePath()
                oldPath?.let { File(it).delete() }

                // Save new avatar with timestamp in filename
                val filename = "avatar_${System.currentTimeMillis()}.jpg"
                val file = File(avatarsDir, filename)
                FileOutputStream(file).use { out ->
                    resized.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                // Update user profile with new path
                userRepository.updateAvatarImage(file.absolutePath)
            } catch (e: Exception) {
                // TODO: Log error for debugging
                // In production, consider showing error message to user
            }
        }
    }
}
