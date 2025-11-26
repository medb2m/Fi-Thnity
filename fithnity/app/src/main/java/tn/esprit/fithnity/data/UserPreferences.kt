package tn.esprit.fithnity.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User Preferences Manager
 * Handles persistent storage of auth tokens and app preferences
 */
class UserPreferences(context: Context) {
    
    private val TAG = "UserPreferences"
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "fithnity_prefs",
        Context.MODE_PRIVATE
    )

    private val _authToken = MutableStateFlow(getAuthToken())
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _currentLanguage = MutableStateFlow(getCurrentLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    
    init {
        Log.d(TAG, "UserPreferences initialized")
        Log.d(TAG, "Initial language: ${getCurrentLanguage()}")
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO_URL = "user_photo_url"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_NEEDS_VERIFICATION = "needs_verification"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Save authentication token and user info
     */
    fun saveAuthData(token: String, userId: String?, userName: String?, userEmail: String?, needsVerification: Boolean = false) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            putBoolean(KEY_NEEDS_VERIFICATION, needsVerification)
            apply()
        }
        _authToken.value = token
    }

    /**
     * Save user photo URL
     */
    fun savePhotoUrl(photoUrl: String?) {
        prefs.edit().apply {
            if (photoUrl != null) {
                putString(KEY_USER_PHOTO_URL, photoUrl)
            } else {
                remove(KEY_USER_PHOTO_URL)
            }
            apply()
        }
    }

    /**
     * Get stored user photo URL
     */
    fun getPhotoUrl(): String? {
        return prefs.getString(KEY_USER_PHOTO_URL, null)
    }

    /**
     * Get stored authentication token
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Get stored user ID
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Get stored user name
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Get stored user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Check if user needs email verification
     */
    fun needsEmailVerification(): Boolean {
        return prefs.getBoolean(KEY_NEEDS_VERIFICATION, false)
    }

    /**
     * Check if user is logged in
     * Checks both stored token and Firebase current user
     * 
     * WARNING: This method calls Firebase synchronously which can block the main thread.
     * Avoid calling this during composition or on the main thread.
     * Prefer checking getAuthToken() first, then verify Firebase asynchronously.
     */
    fun isLoggedIn(): Boolean {
        // Check if we have a stored token (for email auth) - this is fast
        val hasStoredToken = getAuthToken() != null
        if (hasStoredToken) {
            return true
        }
        
        // Only check Firebase if no stored token exists
        // WARNING: This can block if Firebase is initializing
        val hasFirebaseUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Firebase currentUser", e)
            false
        }
        
        return hasFirebaseUser
    }

    /**
     * Clear all authentication data (logout)
     */
    fun clearAuthData() {
        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PHOTO_URL)
            remove(KEY_NEEDS_VERIFICATION)
            apply()
        }
        _authToken.value = null
    }

    /**
     * Save language preference
     */
    fun saveLanguage(languageCode: String) {
        Log.d(TAG, "=== saveLanguage called ===")
        Log.d(TAG, "Saving language: $languageCode")
        
        val editor = prefs.edit()
        editor.putString(KEY_LANGUAGE, languageCode)
        val saved = editor.commit() // Use commit() instead of apply() to ensure immediate write
        
        Log.d(TAG, "SharedPreferences commit result: $saved")
        
        // Verify it was saved
        val readBack = prefs.getString(KEY_LANGUAGE, null)
        Log.d(TAG, "Read back from prefs: $readBack")
        
        _currentLanguage.value = languageCode
        Log.d(TAG, "StateFlow updated to: ${_currentLanguage.value}")
    }

    /**
     * Get current language preference
     * Returns saved language or "auto" for system default
     */
    fun getCurrentLanguage(): String {
        val lang = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
        Log.d(TAG, "getCurrentLanguage returning: $lang")
        return lang
    }

    /**
     * Check if language is set to auto (system default)
     */
    fun isAutoLanguage(): Boolean {
        return getCurrentLanguage() == "auto"
    }
}

