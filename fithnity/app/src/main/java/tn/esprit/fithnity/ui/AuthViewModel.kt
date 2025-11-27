package tn.esprit.fithnity.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.*

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserInfo, val needsEmailVerification: Boolean = false) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class OTPSent(val phoneNumber: String) : AuthUiState() // New state for OTP sent
}

class AuthViewModel : ViewModel() {
    private val api = NetworkModule.authApi
    private val TAG = "AuthViewModel"

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Store auth token for persistence
    private var authToken: String? = null
    
    // DEV: Magic credentials to bypass backend authentication
    companion object {
        private const val MAGIC_EMAIL = "dev@fithnity.com"
        private const val MAGIC_PASSWORD = "dev123"
    }
    
    fun getAuthToken(): String? = authToken

    fun registerWithEmail(name: String, email: String, password: String, userPreferences: UserPreferences) = viewModelScope.launch {
        Log.d(TAG, "registerWithEmail: Starting registration for email: $email")
        _uiState.value = AuthUiState.Loading
        try {
            val resp = api.register(RegisterRequest(name, email, password))
            Log.d(TAG, "registerWithEmail: Response received - success: ${resp.success}")
            if (resp.success && resp.data != null) {
                // Save authentication token and user data
                authToken = resp.data.token
                val user = resp.data.user
                val needsVerification = resp.data.needsVerification ?: !resp.data.emailVerified
                
                userPreferences.saveAuthData(
                    token = resp.data.token,
                    userId = user._id,
                    userName = user.name ?: name,
                    userEmail = user.email ?: email,
                    needsVerification = needsVerification
                )
                
                Log.d(TAG, "registerWithEmail: Registration successful - userId: ${user._id}, emailVerified: ${resp.data.emailVerified}")
                
                _uiState.value = AuthUiState.Success(
                    user,
                    needsEmailVerification = needsVerification
                )
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
        
        // DEV: Check for magic credentials to bypass backend
        if (email.equals(MAGIC_EMAIL, ignoreCase = true) && password == MAGIC_PASSWORD) {
            Log.d(TAG, "loginWithEmail: Magic credentials detected - bypassing backend")
            kotlinx.coroutines.delay(500)
            
            authToken = "dev_magic_token_${System.currentTimeMillis()}"
            val mockUser = UserInfo(
                _id = "dev_user_id",
                name = "Dev User",
                email = email,
                phoneNumber = null,
                photoUrl = null,
                isVerified = true,
                emailVerified = true
            )
            Log.d(TAG, "loginWithEmail: Magic login successful - navigating to home")
            _uiState.value = AuthUiState.Success(mockUser, needsEmailVerification = false)
            return@launch
        }
        
        // Normal authentication flow
        try {
            val resp = api.login(LoginRequest(email, password))
            Log.d(TAG, "loginWithEmail: Response received - success: ${resp.success}")
            if (resp.success && resp.data != null) {
                Log.d(TAG, "loginWithEmail: Login successful for user: ${resp.data.user._id}")
                authToken = resp.data.token
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

    /**
     * Send OTP code to phone number
     */
    fun sendOTP(phoneNumber: String) = viewModelScope.launch {
        Log.d(TAG, "sendOTP: Sending OTP to $phoneNumber")
        _uiState.value = AuthUiState.Loading
        try {
            val resp = api.sendOTP(SendOTPRequest(phoneNumber))
            Log.d(TAG, "sendOTP: Response received - success: ${resp.success}")
            if (resp.success && resp.data != null) {
                Log.d(TAG, "sendOTP: OTP sent successfully")
                _uiState.value = AuthUiState.OTPSent(phoneNumber)
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Failed to send OTP"
                Log.e(TAG, "sendOTP: Failed - $errorMsg")
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendOTP: Exception occurred", e)
            _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Verify OTP code and login/register
     */
    fun verifyOTP(phoneNumber: String, code: String, name: String? = null) = viewModelScope.launch {
        Log.d(TAG, "verifyOTP: Verifying OTP for $phoneNumber")
        _uiState.value = AuthUiState.Loading
        try {
            val resp = api.verifyOTP(VerifyOTPRequest(phoneNumber, code, name))
            Log.d(TAG, "verifyOTP: Response received - success: ${resp.success}")
            if (resp.success && resp.data != null) {
                Log.d(TAG, "verifyOTP: OTP verified successfully for user: ${resp.data.user._id}")
                authToken = resp.data.token
                _uiState.value = AuthUiState.Success(resp.data.user, needsEmailVerification = false)
            } else {
                val errorMsg = resp.message ?: resp.error ?: "Invalid OTP code"
                Log.e(TAG, "verifyOTP: Failed - $errorMsg")
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyOTP: Exception occurred", e)
            _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Resend OTP code
     */
    fun resendOTP(phoneNumber: String) = viewModelScope.launch {
        Log.d(TAG, "resendOTP: Resending OTP to $phoneNumber")
        sendOTP(phoneNumber) // Reuse sendOTP logic
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
        authToken = null
    }
}
