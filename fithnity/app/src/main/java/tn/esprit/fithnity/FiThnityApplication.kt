package tn.esprit.fithnity

import android.app.Application
import org.maplibre.android.MapLibre
import tn.esprit.fithnity.data.UserPreferences
import java.util.Locale

/**
 * Application class for Fi Thnity
 * Initializes MapLibre and app language on startup
 */
class FiThnityApplication : Application() {

    private val TAG = "FiThnityApplication"
    
    lateinit var userPreferences: UserPreferences
        private set

    private var mapLibreInitialized = false
    private val mapLibreLock = Any()

    override fun onCreate() {
        android.util.Log.d(TAG, "=== FiThnityApplication onCreate ===")
        super.onCreate()

        // Initialize user preferences (fast, synchronous)
        android.util.Log.d(TAG, "Initializing UserPreferences")
        userPreferences = UserPreferences.getInstance(this)
        android.util.Log.d(TAG, "UserPreferences created")

        // Set up app language based on user preference or system default
        // This is fast - just reading SharedPreferences
        val languageCode = userPreferences.getCurrentLanguage()
        android.util.Log.d(TAG, "Setting app locale to: $languageCode")
        
        // Defer heavy locale operations to avoid blocking startup
        // Just set the default locale, full resource update happens in attachBaseContext
        try {
            val locale = when (languageCode) {
                "auto" -> {
                    // Get system locale safely
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            resources.configuration.locales[0]
                        } else {
                            @Suppress("DEPRECATION")
                            resources.configuration.locale
                        }
                    } catch (e: Exception) {
                        Locale.getDefault()
                    }
                }
                else -> Locale.forLanguageTag(languageCode)
            }
            Locale.setDefault(locale)
            android.util.Log.d(TAG, "Default locale set to: ${locale.language}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error setting locale", e)
            // Continue with default locale
        }

        // MapLibre initialization is deferred until first MapView is created
        // This prevents blocking Application.onCreate() which can cause ANR
        android.util.Log.d(TAG, "MapLibre initialization deferred")
    }

    /**
     * Initialize MapLibre lazily when first MapView is created
     * This prevents blocking Application.onCreate() and reduces ANR risk
     */
    fun ensureMapLibreInitialized() {
        if (!mapLibreInitialized) {
            synchronized(mapLibreLock) {
                if (!mapLibreInitialized) {
                    android.util.Log.d(TAG, "Lazy initializing MapLibre")
                    val apiKey = BuildConfig.MAPTILER_API_KEY
                    MapLibre.getInstance(
                        this,
                        apiKey,
                        org.maplibre.android.WellKnownTileServer.MapTiler
                    )
                    mapLibreInitialized = true
                    android.util.Log.d(TAG, "MapLibre initialized successfully")
                }
            }
        }
    }
}
