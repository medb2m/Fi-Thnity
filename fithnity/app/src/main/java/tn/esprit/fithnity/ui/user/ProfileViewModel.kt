package tn.esprit.fithnity.ui.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Firebase removed - using JWT tokens instead
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val user: tn.esprit.fithnity.data.UserInfo) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val api = NetworkModule.userApi
    private val TAG = "ProfileViewModel"

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uploadState: StateFlow<ProfileUiState> = _uploadState.asStateFlow()

    fun loadProfile(authToken: String?) = viewModelScope.launch {
        // Use stored JWT token
        val token = authToken
        
        if (token == null) {
            _uiState.value = ProfileUiState.Error("Not authenticated")
            return@launch
        }

        _uiState.value = ProfileUiState.Loading
        try {
            val resp = api.getProfile("Bearer $token")
            if (resp.success && resp.data != null) {
                _uiState.value = ProfileUiState.Success(resp.data)
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Failed to load profile"
                _uiState.value = ProfileUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile", e)
            _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun uploadProfilePicture(imageFile: File, authToken: String?) = viewModelScope.launch {
        // Use stored JWT token
        val token = authToken
        
        if (token == null) {
            _uploadState.value = ProfileUiState.Error("Not authenticated")
            return@launch
        }

        _uploadState.value = ProfileUiState.Loading
        try {
            // Detect MIME type from file extension
            val mimeType = when (imageFile.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "image/jpeg" // Default to JPEG
            }
            
            // Create request body for file with proper MIME type
            val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val picturePart = MultipartBody.Part.createFormData("picture", imageFile.name, requestFile)

            val resp = api.uploadProfilePicture("Bearer $token", picturePart)
            if (resp.success && resp.data != null) {
                // Reload profile to get updated user info
                loadProfile(authToken)
                _uploadState.value = ProfileUiState.Success(
                    tn.esprit.fithnity.data.UserInfo(
                        _id = null,
                        name = null,
                        email = null,
                        phoneNumber = null,
                        photoUrl = resp.data.photoUrl,
                        isVerified = null,
                        emailVerified = null
                    )
                )
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Failed to upload picture"
                _uploadState.value = ProfileUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile picture", e)
            _uploadState.value = ProfileUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun updateProfile(
        authToken: String?,
        name: String? = null,
        email: String? = null,
        phoneNumber: String? = null,
        bio: String? = null
    ) = viewModelScope.launch {
        val token = authToken
        
        if (token == null) {
            _uiState.value = ProfileUiState.Error("Not authenticated")
            return@launch
        }

        _uiState.value = ProfileUiState.Loading
        try {
            val request = UpdateProfileRequest(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                bio = bio
            )
            val resp = api.updateProfile("Bearer $token", request)
            if (resp.success && resp.data != null) {
                _uiState.value = ProfileUiState.Success(resp.data)
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Failed to update profile"
                _uiState.value = ProfileUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
        }
    }

    private val _resendVerificationState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val resendVerificationState: StateFlow<ProfileUiState> = _resendVerificationState.asStateFlow()

    fun resendVerificationEmail(authToken: String?) = viewModelScope.launch {
        val token = authToken
        
        if (token == null) {
            _resendVerificationState.value = ProfileUiState.Error("Not authenticated")
            return@launch
        }

        _resendVerificationState.value = ProfileUiState.Loading
        try {
            val resp = api.resendVerificationEmail("Bearer $token")
            if (resp.success) {
                _resendVerificationState.value = ProfileUiState.Success(
                    tn.esprit.fithnity.data.UserInfo(
                        _id = null,
                        name = null,
                        email = null,
                        phoneNumber = null,
                        photoUrl = null,
                        isVerified = null,
                        emailVerified = null
                    )
                )
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Failed to send verification email"
                _resendVerificationState.value = ProfileUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resending verification email", e)
            _resendVerificationState.value = ProfileUiState.Error(e.message ?: "Unknown error")
        }
    }

    // Firebase methods removed - using JWT tokens from UserPreferences instead
}

