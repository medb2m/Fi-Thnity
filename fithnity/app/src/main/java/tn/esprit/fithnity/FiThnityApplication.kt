package tn.esprit.fithnity

import android.app.Application
import org.maplibre.android.MapLibre
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.utils.LocaleManager

/**
 * Application class for Fi Thnity
 * Initializes MapLibre and app language on startup
 */
class FiThnityApplication : Application() {

    private val TAG = "FiThnityApplication"
    
    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        android.util.Log.d(TAG, "=== FiThnityApplication onCreate ===")
        super.onCreate()

        // Initialize user preferences
        android.util.Log.d(TAG, "Initializing UserPreferences")
        userPreferences = UserPreferences.getInstance(this)
        android.util.Log.d(TAG, "UserPreferences created")

        // Set up app language based on user preference or system default
        val languageCode = userPreferences.getCurrentLanguage()
        android.util.Log.d(TAG, "Setting app locale to: $languageCode")
        LocaleManager.setLocale(this, languageCode)
        android.util.Log.d(TAG, "App locale set successfully")

        // Initialize MapLibre with MapTiler API key
        // This MUST happen before any MapView is created
        android.util.Log.d(TAG, "Initializing MapLibre")
        val apiKey = BuildConfig.MAPTILER_API_KEY
        MapLibre.getInstance(
            this,
            apiKey,
            org.maplibre.android.WellKnownTileServer.MapTiler
        )
        android.util.Log.d(TAG, "MapLibre initialized successfully")
    }
}
