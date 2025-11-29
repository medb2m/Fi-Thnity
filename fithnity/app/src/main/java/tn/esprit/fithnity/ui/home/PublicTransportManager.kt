package tn.esprit.fithnity.ui.home

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import tn.esprit.fithnity.data.VehicleType
import tn.esprit.fithnity.services.VehicleWebSocketClient
import tn.esprit.fithnity.utils.MetroRailHelper
import tn.esprit.fithnity.utils.OSRMRouteHelper
import tn.esprit.fithnity.utils.VehicleLocationSimulator
import kotlin.math.*

/**
 * Manages public transport tracking (Metro and Bus)
 * Handles simulation for metro (on rails) and bus (on streets)
 */
class PublicTransportManager(
    private val context: Context,
    private val mapLibreMap: MapLibreMap?,
    private val mapStyle: Style?,
    private val webSocketClient: VehicleWebSocketClient,
    private val markerManager: VehicleMarkerManager?
) {
    private val TAG = "PublicTransportManager"
    private var metroSimulator: VehicleLocationSimulator? = null
    private var busSimulator: VehicleLocationSimulator? = null
    private var simulationJob: Job? = null
    
    /**
     * Start metro simulation on rails
     */
    fun startMetroSimulation(
        metroLine: Int,
        userLocationLat: Double?,
        userLocationLng: Double?,
        onStatusUpdate: ((String) -> Unit)? = null
    ) {
        stopAllSimulations()
        
        val baseLat = userLocationLat ?: 36.8065 // Tunis center
        val baseLng = userLocationLng ?: 10.1815
        
        Log.d(TAG, "Starting Metro Line $metroLine simulation at $baseLat, $baseLng")
        
        metroSimulator = VehicleLocationSimulator()
        simulationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get route along railway tracks
                val route = MetroRailHelper.createMetroRailRoute(
                    centerLat = baseLat,
                    centerLng = baseLng,
                    routeLengthMeters = 5000.0 // Longer route for metro
                )
                
                if (route.isEmpty()) {
                    onStatusUpdate?.invoke("Error: Could not find railway tracks")
                    return@launch
                }
                
                onStatusUpdate?.invoke("Metro Line $metroLine moving on rails...")
                
                // Start simulation following the rail route
                metroSimulator?.startSimulation(
                    vehicleId = "metro_line_${metroLine}_${System.currentTimeMillis()}",
                    vehicleType = VehicleType.METRO,
                    speedKmh = 40f, // Metro speed
                    updateIntervalMs = 2000L,
                    startLat = route.first().latitude,
                    startLng = route.first().longitude,
                    onStatusUpdate = onStatusUpdate
                )
                
                // Manually send position updates along the route (loop continuously)
                var routeIndex = 0
                while (isActive) {
                    val point = route[routeIndex]
                    
                    // Calculate bearing (direction to next point)
                    val nextIndex = (routeIndex + 1) % route.size
                    val nextPoint = route[nextIndex]
                    val bearing = calculateBearing(
                        point.latitude, point.longitude,
                        nextPoint.latitude, nextPoint.longitude
                    )
                    
                    // Send position update via WebSocket
                    sendVehiclePosition(
                        vehicleId = "metro_line_${metroLine}",
                        type = VehicleType.METRO.name,
                        lat = point.latitude,
                        lng = point.longitude,
                        speed = 40f,
                        bearing = bearing
                    )
                    
                    routeIndex = nextIndex
                    delay(2000) // Update every 2 seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in metro simulation", e)
                onStatusUpdate?.invoke("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Start bus simulation on streets
     */
    fun startBusSimulation(
        busNumber: String,
        userLocationLat: Double?,
        userLocationLng: Double?,
        onStatusUpdate: ((String) -> Unit)? = null
    ) {
        stopAllSimulations()
        
        val baseLat = userLocationLat ?: 36.8065
        val baseLng = userLocationLng ?: 10.1815
        
        Log.d(TAG, "Starting Bus $busNumber simulation at $baseLat, $baseLng")
        
        busSimulator = VehicleLocationSimulator()
        simulationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get route along streets (not rails)
                val route = OSRMRouteHelper.createStreetRoute(
                    centerLat = baseLat,
                    centerLng = baseLng,
                    radiusMeters = 1000.0
                )
                
                if (route.isEmpty()) {
                    onStatusUpdate?.invoke("Error: Could not get street route")
                    return@launch
                }
                
                onStatusUpdate?.invoke("Bus $busNumber moving on streets...")
                
                // Start simulation following street route
                busSimulator?.startSimulation(
                    vehicleId = "bus_${busNumber}_${System.currentTimeMillis()}",
                    vehicleType = VehicleType.BUS,
                    speedKmh = 30f, // Bus speed
                    updateIntervalMs = 2000L,
                    startLat = route.first().latitude,
                    startLng = route.first().longitude,
                    onStatusUpdate = onStatusUpdate
                )
                
                // Manually send position updates along the route (loop continuously)
                var routeIndex = 0
                while (isActive) {
                    val point = route[routeIndex]
                    
                    val nextIndex = (routeIndex + 1) % route.size
                    val nextPoint = route[nextIndex]
                    val bearing = calculateBearing(
                        point.latitude, point.longitude,
                        nextPoint.latitude, nextPoint.longitude
                    )
                    
                    sendVehiclePosition(
                        vehicleId = "bus_$busNumber",
                        type = VehicleType.BUS.name,
                        lat = point.latitude,
                        lng = point.longitude,
                        speed = 30f,
                        bearing = bearing
                    )
                    
                    routeIndex = nextIndex
                    delay(2000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in bus simulation", e)
                onStatusUpdate?.invoke("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Send vehicle position via WebSocket
     */
    private suspend fun sendVehiclePosition(
        vehicleId: String,
        type: String,
        lat: Double,
        lng: Double,
        speed: Float,
        bearing: Float
    ) {
        try {
            // Ensure WebSocket is connected
            if (!webSocketClient.isConnected.value) {
                Log.w(TAG, "WebSocket not connected, attempting to connect...")
                webSocketClient.connect()
                // Wait a bit for connection
                delay(500)
            }
            
            // Format matches backend expectation: { event: "update_location", payload: {...} }
            val message = org.json.JSONObject().apply {
                put("event", "update_location")
                put("payload", org.json.JSONObject().apply {
                    put("vehicleId", vehicleId)
                    put("type", type)
                    put("lat", lat)
                    put("lng", lng)
                    put("speed", speed)
                    put("bearing", bearing)
                    put("timestamp", System.currentTimeMillis())
                })
            }
            
            // Send via WebSocket client
            val sent = webSocketClient.sendMessage(message.toString())
            if (sent) {
                Log.d(TAG, "✅ Sent position: $vehicleId at $lat, $lng")
            } else {
                Log.w(TAG, "⚠️ Failed to send position: $vehicleId (WebSocket not connected)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending vehicle position", e)
        }
    }
    
    /**
     * Calculate bearing between two points
     */
    private fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val dLng = Math.toRadians(lng2 - lng1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = sin(dLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLng)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
    
    /**
     * Stop all simulations
     */
    fun stopAllSimulations() {
        metroSimulator?.stopSimulation()
        busSimulator?.stopSimulation()
        simulationJob?.cancel()
        metroSimulator = null
        busSimulator = null
        simulationJob = null
    }
    
    /**
     * Check if any simulation is running
     */
    fun isSimulating(): Boolean {
        return metroSimulator?.isSimulating() == true || busSimulator?.isSimulating() == true
    }
}

