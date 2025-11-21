package tn.esprit.fithnity.ui.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
        // Try Firebase token first, then fallback to stored token
        val token = getFirebaseIdToken() ?: authToken
        
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
        // Try Firebase token first, then fallback to stored token
        val token = getFirebaseIdToken() ?: authToken
        
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
                        firebaseUid = null,
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

    fun getFirebaseAuthToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.let {
            // This is a suspend function, so we'll handle it in the calling code
            null
        }
    }

    suspend fun getFirebaseIdToken(): String? {
        return suspendCoroutine { cont ->
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                cont.resume(null)
                return@suspendCoroutine
            }
            user.getIdToken(true)
                .addOnSuccessListener { result -> cont.resume(result.token) }
                .addOnFailureListener { ex -> cont.resumeWithException(ex) }
        }
    }
}

