package tn.esprit.fithnity.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * Locale Manager for handling app language
 */
object LocaleManager {
    
    private const val TAG = "LocaleManager"

    /**
     * Set the app locale
     * @param context Application context
     * @param languageCode Language code ("en", "fr", or "auto" for system default)
     */
    fun setLocale(context: Context, languageCode: String): Context {
        Log.d(TAG, "=== setLocale called ===")
        Log.d(TAG, "Language code: $languageCode")
        Log.d(TAG, "Context type: ${context.javaClass.simpleName}")
        
        val locale = when (languageCode) {
            "auto" -> {
                val sysLocale = getSystemLocale()
                Log.d(TAG, "Using system locale: ${sysLocale.language}")
                sysLocale
            }
            else -> {
                Log.d(TAG, "Creating locale for: $languageCode")
                Locale(languageCode)
            }
        }

        Log.d(TAG, "Setting default locale to: ${locale.language}")
        Locale.setDefault(locale)
        Log.d(TAG, "Default locale set. Current default: ${Locale.getDefault().language}")
        
        val newContext = updateResources(context, locale)
        Log.d(TAG, "Resources updated. Returning context")
        return newContext
    }

    /**
     * Get system default locale
     */
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.content.res.Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources.getSystem().configuration.locale
        }
    }

    /**
     * Update app resources with new locale
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Log.d(TAG, "updateResources called with locale: ${locale.language}")
        
        var contextResult = context
        val configuration = context.resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "Using Android N+ method")
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            contextResult = context.createConfigurationContext(configuration)
        } else {
            Log.d(TAG, "Using legacy method")
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        }
        
        // Force update the main application resources as well
        context.applicationContext.resources.updateConfiguration(configuration, context.applicationContext.resources.displayMetrics)
        
        Log.d(TAG, "Resources updated. New config locale: ${contextResult.resources.configuration.locales[0].language}")
        return contextResult
    }
    
    /**
     * Apply locale change to current activity
     * This should be called before recreating the activity
     */
    fun applyLocaleChange(activity: Activity, languageCode: String) {
        Log.d(TAG, "=== applyLocaleChange called ===")
        Log.d(TAG, "Activity: ${activity.javaClass.simpleName}, Language: $languageCode")
        
        val locale = getLocaleForLanguageCode(languageCode)
        Log.d(TAG, "Locale resolved to: ${locale.language}")
        
        Locale.setDefault(locale)
        Log.d(TAG, "Default locale set")
        
        val configuration = Configuration(activity.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "Applying locale with Android N+ method")
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
        } else {
            Log.d(TAG, "Applying locale with legacy method")
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(configuration, activity.resources.displayMetrics)
        Log.d(TAG, "Activity resources updated successfully")
    }

    /**
     * Get current app locale
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    /**
     * Get supported language code from locale
     * Returns "en" or "fr" based on locale, defaults to "en"
     */
    fun getLanguageCode(locale: Locale): String {
        return when (locale.language) {
            "fr" -> "fr"
            else -> "en"
        }
    }

    /**
     * Get locale for language code
     */
    fun getLocaleForLanguageCode(languageCode: String): Locale {
        return when (languageCode) {
            "auto" -> getSystemLocale()
            else -> Locale(languageCode)
        }
    }
}

