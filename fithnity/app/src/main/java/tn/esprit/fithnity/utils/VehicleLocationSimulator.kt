package tn.esprit.fithnity.utils

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import tn.esprit.fithnity.data.VehicleType
import tn.esprit.fithnity.utils.OSRMRouteHelper
import tn.esprit.fithnity.utils.MetroRailHelper
import java.util.concurrent.TimeUnit

/**
 * Simulator for testing vehicle location tracking
 * Simulates a vehicle moving along a predefined route
 */
class VehicleLocationSimulator {
    private val TAG = "VehicleLocationSimulator"
    private val WS_URL = "ws://72.61.145.239:9090/ws/vehicle-location"
    
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    private var simulationJob: Job? = null
    private var isSimulating = false
    private val connectionLock = java.lang.Object()
    private var isConnected = false
    
    /**
     * Generate a small circular route around a starting point
     * Creates a visible route that's close to the user (about 200m radius)
     * Small enough to see clearly on map, large enough to see movement
     * Starts slightly offset from user location so both are visible
     * Returns MapLibre LatLng to match OSRMRouteHelper
     */
    private fun generateRouteAroundPoint(startLat: Double, startLng: Double): List<LatLng> {
        // Start point is slightly offset from user (about 100m to the east)
        // This ensures user's blue dot and vehicle marker are both visible
        val startOffset = 0.001 // ~100 meters east
        val routeCenterLat = startLat
        val routeCenterLng = startLng + startOffset
        
        // Create a small square route around the offset starting point
        // Each point is about 0.002 degrees away (roughly 200 meters) - visible but close
        val offset = 0.002 // ~200 meters - small enough to see clearly
        
        return listOf(
            LatLng(routeCenterLat, routeCenterLng), // Start point (slightly east of user)
            LatLng(routeCenterLat + offset, routeCenterLng), // North
            LatLng(routeCenterLat + offset, routeCenterLng + offset), // North-East
            LatLng(routeCenterLat, routeCenterLng + offset), // East
            LatLng(routeCenterLat - offset, routeCenterLng + offset), // South-East
            LatLng(routeCenterLat - offset, routeCenterLng), // South
            LatLng(routeCenterLat - offset, routeCenterLng - offset), // South-West
            LatLng(routeCenterLat, routeCenterLng - offset), // West
            LatLng(routeCenterLat + offset, routeCenterLng - offset), // North-West
            LatLng(routeCenterLat, routeCenterLng) // Return to start
        )
    }
    
    data class LatLngPoint(val lat: Double, val lng: Double)
    
    /**
     * Start simulating vehicle movement
     */
    fun startSimulation(
        vehicleId: String = "test_vehicle_${System.currentTimeMillis()}",
        vehicleType: VehicleType = VehicleType.CAR,
        speedKmh: Float = 50f, // Simulated speed in km/h
        updateIntervalMs: Long = 2000L, // Update every 2 seconds
        startLat: Double? = null, // Starting latitude (user's location)
        startLng: Double? = null, // Starting longitude (user's location)
        onStatusUpdate: ((String) -> Unit)? = null
    ) {
        if (isSimulating) {
            Log.w(TAG, "Simulation already running")
            return
        }
        
        // Use provided location or default to Tunis center
        val baseLat = startLat ?: 36.8065
        val baseLng = startLng ?: 10.1815
        
        isSimulating = true
        
        simulationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Connect WebSocket and wait for connection
                onStatusUpdate?.invoke("Connecting to server...")
                val connected = connectWebSocket()
                
                if (!connected || webSocket == null) {
                    Log.e(TAG, "Failed to connect WebSocket")
                    onStatusUpdate?.invoke("Error: Could not connect to server")
                    isSimulating = false
                    return@launch
                }
                
                Log.d(TAG, "WebSocket connected, starting simulation")
                onStatusUpdate?.invoke("Connected! Getting route...")
                
                // For METRO, use railway tracks; for others, use streets
                val route = withContext(Dispatchers.IO) {
                    try {
                        if (vehicleType == VehicleType.METRO) {
                            onStatusUpdate?.invoke("Finding railway tracks...")
                            // Get route along actual railway tracks
                            MetroRailHelper.createMetroRailRoute(baseLat, baseLng, routeLengthMeters = 2000.0)
                        } else {
                            onStatusUpdate?.invoke("Getting route from streets...")
                            // Get real street route for other vehicles
                            OSRMRouteHelper.createStreetRoute(baseLat, baseLng, radiusMeters = 500.0)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting route, using simple route", e)
                        // Fallback to simple route
                        generateRouteAroundPoint(baseLat, baseLng)
                    }
                }
                
                Log.d(TAG, "Starting simulation at: $baseLat, $baseLng with ${route.size} street waypoints")
                
                if (route.isEmpty()) {
                    onStatusUpdate?.invoke("Error: Could not get route")
                    isSimulating = false
                    return@launch
                }
                
                var currentRouteIndex = 0
                var progress = 0f // Progress between current and next point (0.0 to 1.0)
                
                onStatusUpdate?.invoke("Vehicle moving on streets...")
                
                while (isSimulating && currentRouteIndex < route.size) {
                    val currentPoint = route[currentRouteIndex]
                    val nextPoint = route[(currentRouteIndex + 1) % route.size]
                    
                    // Calculate interpolated position
                    // MapLibre LatLng uses latitude/longitude properties
                    val lat = currentPoint.latitude + (nextPoint.latitude - currentPoint.latitude) * progress
                    val lng = currentPoint.longitude + (nextPoint.longitude - currentPoint.longitude) * progress
                    
                    // Calculate bearing (direction)
                    val bearing = calculateBearing(
                        currentPoint.latitude, currentPoint.longitude,
                        nextPoint.latitude, nextPoint.longitude
                    )
                    
                    // Send location update
                    sendLocationUpdate(
                        vehicleId = vehicleId,
                        type = vehicleType.name,
                        lat = lat,
                        lng = lng,
                        speed = speedKmh,
                        bearing = bearing
                    )
                    
                    onStatusUpdate?.invoke("Moving: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}")
                    
                    // Move forward
                    val distancePerUpdate = (speedKmh / 3.6f) * (updateIntervalMs / 1000f) // meters per update
                    val distanceToNext = calculateDistance(
                        currentPoint.latitude, currentPoint.longitude,
                        nextPoint.latitude, nextPoint.longitude
                    )
                    
                    progress += (distancePerUpdate / distanceToNext)
                    
                    if (progress >= 1.0f) {
                        progress = 0f
                        currentRouteIndex++
                        
                        // Loop back to start if reached end
                        if (currentRouteIndex >= route.size) {
                            currentRouteIndex = 0
                            onStatusUpdate?.invoke("Route completed, looping on streets...")
                        }
                    }
                    
                    delay(updateIntervalMs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Simulation error", e)
                onStatusUpdate?.invoke("Error: ${e.message}")
            } finally {
                isSimulating = false
            }
        }
    }
    
    /**
     * Stop simulation
     */
    fun stopSimulation() {
        isSimulating = false
        simulationJob?.cancel()
        disconnectWebSocket()
    }
    
    private suspend fun connectWebSocket(): Boolean {
        synchronized(connectionLock) {
            if (webSocket != null && isConnected) {
                Log.d(TAG, "WebSocket already connected")
                return true
            }
            
            isConnected = false
            webSocket = null
            
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(WS_URL)
                .build()
            
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                    Log.d(TAG, "Simulator WebSocket connected successfully")
                    synchronized(connectionLock) {
                        this@VehicleLocationSimulator.webSocket = webSocket
                        isConnected = true
                        connectionLock.notifyAll()
                    }
                    
                    // Subscribe to receive vehicle positions
                    val subscribeMessage = JSONObject().apply {
                        put("event", "subscribe")
                    }
                    webSocket.send(subscribeMessage.toString())
                }
                
                override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "Simulator WebSocket closed: $reason")
                    synchronized(connectionLock) {
                        this@VehicleLocationSimulator.webSocket = null
                        isConnected = false
                        connectionLock.notifyAll()
                    }
                }
                
                override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                    Log.d(TAG, "Simulator received: $text")
                }
                
                override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Simulator WebSocket failure", t)
                    synchronized(connectionLock) {
                        this@VehicleLocationSimulator.webSocket = null
                        isConnected = false
                        connectionLock.notifyAll()
                    }
                }
            }
            
            webSocket = okHttpClient?.newWebSocket(request, listener)
            
            // Wait for connection with timeout
            var waited = 0
            while (!isConnected && waited < 5000) {
                connectionLock.wait(500)
                waited += 500
            }
            
            return isConnected
        }
    }
    
    private fun disconnectWebSocket() {
        synchronized(connectionLock) {
            webSocket?.close(1000, "Simulation stopped")
            webSocket = null
            isConnected = false
            okHttpClient = null
        }
    }
    
    private fun sendLocationUpdate(
        vehicleId: String,
        type: String,
        lat: Double,
        lng: Double,
        speed: Float,
        bearing: Float
    ) {
        val json = JSONObject().apply {
            put("event", "update_location")
            put("data", JSONObject().apply {
                put("vehicleId", vehicleId)
                put("type", type)
                put("lat", lat)
                put("lng", lng)
                put("speed", speed)
                put("bearing", bearing)
                put("timestamp", System.currentTimeMillis())
            })
        }
        
        val message = json.toString()
        Log.d(TAG, "Sending location update: $message")
        
        if (webSocket != null) {
            val sent = webSocket?.send(message) ?: false
            if (!sent) {
                Log.e(TAG, "Failed to send WebSocket message")
            }
        } else {
            Log.e(TAG, "WebSocket is null, cannot send update")
        }
    }
    
    /**
     * Calculate bearing (direction) between two points in degrees
     */
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = Math.sin(dLon) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - 
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon)
        
        var bearing = Math.toDegrees(Math.atan2(y, x))
        bearing = (bearing + 360) % 360
        
        return bearing.toFloat()
    }
    
    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return (R * c).toFloat()
    }
    
    fun isSimulating(): Boolean = isSimulating
}

