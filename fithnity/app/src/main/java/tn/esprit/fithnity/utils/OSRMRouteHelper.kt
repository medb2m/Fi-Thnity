package tn.esprit.fithnity.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.maplibre.android.geometry.LatLng
import kotlin.math.*

/**
 * Helper for getting real street routes
 * Uses OSRM (Open Source Routing Machine) - free, open-source routing service
 * Excellent coverage for Tunisia using OpenStreetMap data
 */
object OSRMRouteHelper {
    private const val TAG = "OSRMRouteHelper"
    // OSRM public server - free, no API key needed, excellent Tunisia coverage
    private const val OSRM_BASE_URL = "https://router.project-osrm.org"
    
    /**
     * Decode polyline string to list of LatLng points
     * OSRM uses Google's polyline encoding format
     */
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            
            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        
        return poly
    }
    
    /**
     * Get route using OSRM (Open Source Routing Machine)
     * Free, open-source, excellent Tunisia coverage
     */
    suspend fun getRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<LatLng> {
        // OSRM route endpoint - format: /route/v1/driving/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson
        val url = "$OSRM_BASE_URL/route/v1/driving/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson&steps=true"
        
        Log.d(TAG, "Requesting route from OSRM: $url")
        
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FiThnity/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            Log.d(TAG, "OSRM response code: ${response.code}, body length: ${body?.length ?: 0}")
            
            if (response.isSuccessful && body != null) {
                val json = org.json.JSONObject(body)
                
                if (json.has("code") && json.getString("code") == "Ok") {
                    if (json.has("routes") && json.getJSONArray("routes").length() > 0) {
                        val routes = json.getJSONArray("routes")
                        val route = routes.getJSONObject(0)
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        
                        val points = mutableListOf<LatLng>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            points.add(LatLng(lat, lng))
                        }
                        
                        Log.d(TAG, "✅ Got route with ${points.size} points from OSRM (following streets!)")
                        return points
                    } else {
                        Log.w(TAG, "No routes in OSRM response")
                    }
                } else {
                    val code = json.optString("code", "Unknown")
                    val message = json.optString("message", "")
                    Log.w(TAG, "OSRM error: code=$code, message=$message")
                }
            } else {
                Log.w(TAG, "OSRM API request failed: ${response.code}, body: ${body?.take(200)}")
            }
            
            // Fallback to straight line
            Log.w(TAG, "⚠️ Falling back to straight line route")
            listOf(LatLng(startLat, startLng), LatLng(endLat, endLng))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calling OSRM API", e)
            // Fallback to straight line
            listOf(LatLng(startLat, startLng), LatLng(endLat, endLng))
        }
    }
    
    /**
     * Create a circular route using real streets
     * Gets routes between waypoints to follow actual roads
     */
    suspend fun createStreetRoute(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double = 500.0
    ): List<LatLng> {
        // Create waypoints in a circle (8 points)
        val waypoints = mutableListOf<Pair<Double, Double>>()
        val numPoints = 8
        val radiusDegrees = radiusMeters / 111000.0 // Approximate conversion
        
        for (i in 0 until numPoints) {
            val angle = (i * 360.0 / numPoints) * PI / 180.0
            val lat = centerLat + radiusDegrees * cos(angle)
            val lng = centerLng + radiusDegrees * sin(angle) / cos(centerLat * PI / 180.0)
            waypoints.add(Pair(lat, lng))
        }
        
        Log.d(TAG, "Creating street route with ${waypoints.size} waypoints around $centerLat, $centerLng")
        
        // Get routes between waypoints
        val allPoints = mutableListOf<LatLng>()
        var successfulRoutes = 0
        
        for (i in waypoints.indices) {
            val start = waypoints[i]
            val end = waypoints[(i + 1) % waypoints.size]
            
            Log.d(TAG, "Getting route ${i + 1}/${waypoints.size} from ${start.first},${start.second} to ${end.first},${end.second}")
            
            val routePoints = getRoute(start.first, start.second, end.first, end.second)
            
            // Check if we got a real route (more than 2 points) or just a straight line
            if (routePoints.size > 2) {
                successfulRoutes++
                Log.d(TAG, "✅ Got street route with ${routePoints.size} points")
            } else {
                Log.w(TAG, "⚠️ Got straight line (${routePoints.size} points) - route might not follow streets")
            }
            
            // Add points (skip first to avoid duplicates)
            if (i == 0) {
                allPoints.addAll(routePoints)
            } else {
                allPoints.addAll(routePoints.drop(1))
            }
            
            // Small delay to avoid rate limiting
            kotlinx.coroutines.delay(300)
        }
        
        Log.d(TAG, "Created street route with ${allPoints.size} total points ($successfulRoutes/${waypoints.size} routes follow streets)")
        
        if (successfulRoutes == 0) {
            Log.e(TAG, "❌ WARNING: No routes are following streets! All routes are straight lines.")
        }
        
        return allPoints
    }
}

