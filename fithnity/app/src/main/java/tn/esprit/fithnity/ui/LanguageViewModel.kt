package tn.esprit.fithnity.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.utils.LocaleManager
import java.util.Locale

/**
 * ViewModel for managing app language changes
 * Provides a centralized way to handle language switching
 */
class LanguageViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "LanguageViewModel"
    private val userPreferences = UserPreferences.getInstance(application)
    
    private val _currentLanguage = MutableStateFlow(userPreferences.getCurrentLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    
    init {
        Log.d(TAG, "LanguageViewModel initialized")
        Log.d(TAG, "Current language from preferences: ${userPreferences.getCurrentLanguage()}")
        Log.d(TAG, "StateFlow current language: ${_currentLanguage.value}")
    }
    
    /**
     * Change the app language
     * @param languageCode "en", "fr", or "auto" for system default
     * @return true if language was changed, false if already set to that language
     */
    fun changeLanguage(languageCode: String): Boolean {
        Log.d(TAG, "=== changeLanguage called ===")
        Log.d(TAG, "Requested language: $languageCode")
        Log.d(TAG, "Current language: ${_currentLanguage.value}")
        
        if (_currentLanguage.value == languageCode) {
            Log.d(TAG, "Language already set to $languageCode, no change needed")
            Toast.makeText(getApplication(), "Already using $languageCode", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Save to preferences and update state
        Log.d(TAG, "Saving language to preferences: $languageCode")
        userPreferences.saveLanguage(languageCode)
        _currentLanguage.value = languageCode
        Log.d(TAG, "StateFlow updated to: ${_currentLanguage.value}")

        return true
    }

    /**
     * Get the Locale object for the current language
     */
    fun getLocale(): Locale {
        val langCode = _currentLanguage.value
        return LocaleManager.getLocaleForLanguageCode(langCode)
    }

    /**
     * Get display name for language code
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "fr" -> "Français"
            "auto" -> "System Default"
            else -> "English"
        }
    }
}

