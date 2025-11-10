package tn.esprit.fithnity.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserInfo, val needsEmailVerification: Boolean = false) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {
    private val api = NetworkModule.authApi
    private val TAG = "AuthViewModel"

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun registerWithEmail(name: String, email: String, password: String) = viewModelScope.launch {
        Log.d(TAG, "registerWithEmail: Starting registration for email: $email")
        _uiState.value = AuthUiState.Loading
        try {
            val resp = api.register(RegisterRequest(name, email, password))
            Log.d(TAG, "registerWithEmail: Response received - success: ${resp.success}")
            if (resp.success && resp.data != null) {
                Log.d(TAG, "registerWithEmail: Registration successful for userId: ${resp.data.userId}")
                _uiState.value = AuthUiState.Success(UserInfo(resp.data.userId, name, email, null, null, null, false))
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Registration failed"
                Log.e(TAG, "registerWithEmail: Registration failed - $errorMsg")
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerWithEmail: Exception occurred", e)
            _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun loginWithEmail(email: String, password: String) = viewModelScope.launch {
        Log.d(TAG, "loginWithEmail: Starting login for email: $email")
        _uiState.value = AuthUiState.Loading
        try {
            val resp = api.login(LoginRequest(email, password))
            Log.d(TAG, "loginWithEmail: Response received - success: ${resp.success}")
            if (resp.success && resp.data != null) {
                Log.d(TAG, "loginWithEmail: Login successful for user: ${resp.data.user._id}")
                val needsVerification = resp.data.needsVerification ?: false
                Log.d(TAG, "loginWithEmail: Email verification needed: $needsVerification")
                _uiState.value = AuthUiState.Success(resp.data.user, needsVerification)
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Login failed"
                Log.e(TAG, "loginWithEmail: Login failed - $errorMsg")
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginWithEmail: Exception occurred", e)
            _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun syncFirebaseUser(firebaseUser: FirebaseUser, name: String, email: String? = null, photoUrl: String? = null) = viewModelScope.launch {
        Log.d(TAG, "syncFirebaseUser: Starting Firebase sync for uid: ${firebaseUser.uid}")
        _uiState.value = AuthUiState.Loading
        try {
            // Get Firebase ID token on a suspendCoroutine for better coroutine support
            val idToken = suspendCoroutine<String?> { cont ->
                firebaseUser.getIdToken(true)
                    .addOnSuccessListener { result -> cont.resume(result.token) }
                    .addOnFailureListener { ex -> cont.resumeWithException(ex) }
            }
            if (idToken == null) {
                Log.e(TAG, "syncFirebaseUser: Firebase token unavailable")
                _uiState.value = AuthUiState.Error("Firebase token unavailable")
                return@launch
            }
            Log.d(TAG, "syncFirebaseUser: Firebase token obtained")
            val request = FirebaseRegisterRequest(
                firebaseUid = firebaseUser.uid,
                phoneNumber = firebaseUser.phoneNumber ?: "",
                name = name,
                email = email,
                photoUrl = photoUrl
            )
            val resp = api.syncFirebaseUser(bearer = "Bearer $idToken", request = request)
            Log.d(TAG, "syncFirebaseUser: Response received - success: ${resp.success}")
            if (resp.success && resp.data != null) {
                Log.d(TAG, "syncFirebaseUser: Sync successful for user: ${resp.data._id}")
                _uiState.value = AuthUiState.Success(resp.data)
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Firebase user sync failed"
                Log.e(TAG, "syncFirebaseUser: Sync failed - $errorMsg")
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncFirebaseUser: Exception occurred", e)
            _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
