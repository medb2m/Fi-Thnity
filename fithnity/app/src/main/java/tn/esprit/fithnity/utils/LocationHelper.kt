package tn.esprit.fithnity.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.tasks.await

/**
 * Data class to hold location information
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
)

/**
 * Composable function to request location permission and get current location
 * Returns the current location or null if permission is denied or location unavailable
 */
@Composable
fun rememberLocationState(): LocationState {
    val context = LocalContext.current
    var location by remember { mutableStateOf<LocationData?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Check initial permission state
    LaunchedEffect(Unit) {
        hasPermission = checkLocationPermission(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasPermission = fineLocationGranted || coarseLocationGranted
        
        if (hasPermission) {
            getCurrentLocation(context) { loc, err ->
                location = loc
                error = err
                isLoading = false
            }
        } else {
            error = "Location permission denied"
            isLoading = false
        }
    }

    fun requestLocation() {
        if (hasPermission) {
            isLoading = true
            error = null
            getCurrentLocation(context) { loc, err ->
                location = loc
                error = err
                isLoading = false
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            isLoading = true
        }
    }

    return LocationState(
        location = location,
        hasPermission = hasPermission,
        isLoading = isLoading,
        error = error,
        requestLocation = ::requestLocation
    )
}

/**
 * State class for location
 */
class LocationState(
    val location: LocationData?,
    val hasPermission: Boolean,
    val isLoading: Boolean,
    val error: String?,
    val requestLocation: () -> Unit
)

/**
 * Check if location permission is granted
 */
private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Get current location using Fused Location Provider
 */
private fun getCurrentLocation(
    context: Context,
    callback: (LocationData?, String?) -> Unit
) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        // First try to get last known location (faster)
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                callback(
                    LocationData(
                        latitude = lastLocation.latitude,
                        longitude = lastLocation.longitude,
                        accuracy = lastLocation.accuracy
                    ),
                    null
                )
            } else {
                // If last location is not available, request a fresh location update
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000L // 5 seconds
                ).setMaxUpdateDelayMillis(10000L).build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location = locationResult.lastLocation
                        fusedLocationClient.removeLocationUpdates(this)
                        if (location != null) {
                            callback(
                                LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy
                                ),
                                null
                            )
                        } else {
                            callback(null, "Unable to get location")
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                
                // Timeout after 15 seconds
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    callback(null, "Location request timed out")
                }, 15000)
            }
        }.addOnFailureListener { exception ->
            callback(null, "Error getting location: ${exception.message}")
        }

    } catch (e: SecurityException) {
        callback(null, "Location permission not granted")
    } catch (e: Exception) {
        callback(null, "Error: ${e.message}")
    }
}

