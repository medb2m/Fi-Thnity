package tn.esprit.fithnity

import android.app.Application
import org.maplibre.android.MapLibre

/**
 * Application class for Fi Thnity
 * Initializes MapLibre on app startup
 */
class FiThnityApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize MapLibre with MapTiler API key
        // This MUST happen before any MapView is created
        val apiKey = BuildConfig.MAPTILER_API_KEY
        MapLibre.getInstance(
            this,
            apiKey,
            org.maplibre.android.WellKnownTileServer.MapTiler
        )
    }
}
