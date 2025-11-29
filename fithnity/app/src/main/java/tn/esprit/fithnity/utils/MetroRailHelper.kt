package tn.esprit.fithnity.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import kotlin.math.*

/**
 * Helper for finding and routing along metro/railway tracks
 * Uses OpenStreetMap Overpass API to find railway tracks
 */
object MetroRailHelper {
    private const val TAG = "MetroRailHelper"
    // Overpass API endpoint - free, no API key needed
    private const val OVERPASS_API = "https://overpass-api.de/api/interpreter"
    
    /**
     * Find nearby metro/railway stations using Overpass API
     */
    suspend fun findNearbyStations(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double = 3000.0
    ): List<Pair<LatLng, String>> {
        val radiusDegrees = radiusMeters / 111000.0
        
        val query = """
            [out:json][timeout:25];
            (
              node["railway"="station"]["public_transport"="station"]
                (${centerLat - radiusDegrees},${centerLng - radiusDegrees},${centerLat + radiusDegrees},${centerLng + radiusDegrees});
              node["railway"="halt"]
                (${centerLat - radiusDegrees},${centerLng - radiusDegrees},${centerLat + radiusDegrees},${centerLng + radiusDegrees});
            );
            out center;
        """.trimIndent()
        
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val requestBody = okhttp3.FormBody.Builder()
                .add("data", query)
                .build()
            
            val request = Request.Builder()
                .url(OVERPASS_API)
                .post(requestBody)
                .header("User-Agent", "FiThnity/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val elements = json.getJSONArray("elements")
                val stations = mutableListOf<Pair<LatLng, String>>()
                
                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)
                    if (element.getString("type") == "node") {
                        val lat = element.getDouble("lat")
                        val lng = element.getDouble("lon")
                        val name = element.optString("name", "Station")
                        stations.add(Pair(LatLng(lat, lng), name))
                    }
                }
                
                // Sort by distance from center
                stations.sortedBy { station ->
                    calculateDistance(centerLat, centerLng, station.first.latitude, station.first.longitude)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding stations", e)
            emptyList()
        }
    }
    
    /**
     * Find nearby railway tracks using Overpass API
     * Returns a list of track segments near the given location
     */
    suspend fun findNearbyRailwayTracks(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double = 2000.0 // Search within 2km
    ): List<List<LatLng>> {
        // Convert radius to degrees (approximate)
        val radiusDegrees = radiusMeters / 111000.0
        
        // Overpass QL query to find railway tracks
        val query = """
            [out:json][timeout:25];
            (
              way["railway"~"^(rail|light_rail|tram|subway)$"]["railway"!="abandoned"]["railway"!="disused"]
                (${centerLat - radiusDegrees},${centerLng - radiusDegrees},${centerLat + radiusDegrees},${centerLng + radiusDegrees});
            );
            out geom;
        """.trimIndent()
        
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            // Use FormBody for form-encoded data (proper OkHttp way)
            val requestBody = okhttp3.FormBody.Builder()
                .add("data", query)
                .build()
            
            val request = Request.Builder()
                .url(OVERPASS_API)
                .post(requestBody)
                .header("User-Agent", "FiThnity/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            Log.d(TAG, "Overpass API response code: ${response.code}")
            
            if (response.isSuccessful && body != null) {
                parseRailwayTracks(body, centerLat, centerLng)
            } else {
                Log.w(TAG, "Overpass API request failed: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Overpass API for railway tracks", e)
            emptyList()
        }
    }
    
    /**
     * Parse Overpass API response and extract railway track coordinates
     */
    private fun parseRailwayTracks(
        jsonString: String,
        centerLat: Double,
        centerLng: Double
    ): List<List<LatLng>> {
        val tracks = mutableListOf<List<LatLng>>()
        
        try {
            val json = JSONObject(jsonString)
            val elements = json.getJSONArray("elements")
            
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                if (element.getString("type") == "way") {
                    val geometry = element.optJSONArray("geometry")
                    if (geometry != null) {
                        val trackPoints = mutableListOf<LatLng>()
                        for (j in 0 until geometry.length()) {
                            val node = geometry.getJSONObject(j)
                            val lat = node.getDouble("lat")
                            val lng = node.getDouble("lon")
                            trackPoints.add(LatLng(lat, lng))
                        }
                        if (trackPoints.size >= 2) {
                            tracks.add(trackPoints)
                        }
                    }
                }
            }
            
            Log.d(TAG, "Found ${tracks.size} railway track segments")
            
            // Sort tracks by length (longest first) - longer tracks are more reliable
            // Also filter out very short tracks (< 5 points) as they're likely incomplete
            return tracks
                .filter { it.size >= 5 } // Only keep tracks with at least 5 points
                .sortedByDescending { it.size } // Longest tracks first
                .take(5) // Return up to 5 longest track segments
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing railway tracks from Overpass response", e)
            return emptyList()
        }
    }
    
    /**
     * Create a route along railway tracks for metro simulation
     * Reads the FULL track trajectory first, then creates a continuous route
     * Ensures the route stays on ONE continuous track segment (no random jumping)
     */
    suspend fun createMetroRailRoute(
        centerLat: Double,
        centerLng: Double,
        routeLengthMeters: Double = 2000.0
    ): List<LatLng> {
        Log.d(TAG, "Creating metro rail route near $centerLat, $centerLng")
        
        // Find nearby railway tracks FIRST - read all available tracks
        val tracks = findNearbyRailwayTracks(centerLat, centerLng, radiusMeters = 3000.0)
        
        if (tracks.isEmpty()) {
            Log.w(TAG, "No railway tracks found nearby, using fallback route")
            return createFallbackMetroRoute(centerLat, centerLng, routeLengthMeters)
        }
        
        Log.d(TAG, "Found ${tracks.size} track segments - analyzing for continuous route")
        
        // Find the LONGEST continuous track segment (most reliable)
        val longestTrack = tracks.maxByOrNull { it.size } ?: tracks.first()
        Log.d(TAG, "Using longest track segment with ${longestTrack.size} points")
        
        // Find the point on this track closest to center location
        var closestPointIndex = 0
        var minDistance = Double.MAX_VALUE
        
        longestTrack.forEachIndexed { index, point ->
            val distance = calculateDistance(centerLat, centerLng, point.latitude, point.longitude)
            if (distance < minDistance) {
                minDistance = distance
                closestPointIndex = index
            }
        }
        
        Log.d(TAG, "Closest point on track is at index $closestPointIndex (distance: ${minDistance.toInt()}m)")
        
        // Read the FULL trajectory from the track FIRST
        // Then create route that follows this continuous trajectory
        val route = mutableListOf<LatLng>()
        
        // Strategy: Always go FORWARD along the track (toward end of track segment)
        // This simulates metro heading to next station
        // Calculate how many points we need based on route length (approx 50m per point)
        val pointsNeeded = (routeLengthMeters / 50).toInt().coerceAtLeast(10)
        
        // Start from closest point and go forward
        var pointsAdded = 0
        for (i in closestPointIndex until longestTrack.size) {
            route.add(longestTrack[i])
            pointsAdded++
            if (pointsAdded >= pointsNeeded) break
        }
        
        // If we haven't reached the end of track and need more points, continue forward
        // But if we're at the end, we can extend backward a bit for context
        if (pointsAdded < pointsNeeded && closestPointIndex > 0) {
            // Add some backward points for context, but keep going forward as primary direction
            val backwardPoints = minOf(5, closestPointIndex, pointsNeeded - pointsAdded)
            for (i in 1..backwardPoints) {
                route.add(0, longestTrack[closestPointIndex - i])
            }
        }
        
        // Ensure route is continuous and valid
        if (route.size < 5) {
            Log.w(TAG, "Route too short, extending with more track points")
            // Extend forward more aggressively
            for (i in route.size until minOf(pointsNeeded, longestTrack.size)) {
                if (i < longestTrack.size) {
                    route.add(longestTrack[i])
                }
            }
        }
        
        // Verify route continuity - check distances between consecutive points
        var discontinuityCount = 0
        for (i in 1 until route.size) {
            val dist = calculateDistance(
                route[i-1].latitude, route[i-1].longitude,
                route[i].latitude, route[i].longitude
            )
            // If distance > 500m, likely a discontinuity (jump between segments)
            if (dist > 500) {
                discontinuityCount++
                Log.w(TAG, "Potential discontinuity detected at index $i (distance: ${dist.toInt()}m)")
            }
        }
        
        if (discontinuityCount > 0) {
            Log.w(TAG, "Found $discontinuityCount discontinuities - route may jump between segments")
        }
        
        Log.d(TAG, "Created continuous metro rail route with ${route.size} points (${discontinuityCount} discontinuities)")
        return route
    }
    
    /**
     * Find route between two stations along railway tracks
     */
    private fun findRouteBetweenStations(
        startStation: LatLng,
        endStation: LatLng,
        tracks: List<List<LatLng>>,
        centerLat: Double,
        centerLng: Double
    ): List<LatLng> {
        // Find track segments closest to each station
        val startTrack = tracks.minByOrNull { track ->
            track.minOfOrNull { point ->
                calculateDistance(startStation.latitude, startStation.longitude, point.latitude, point.longitude)
            } ?: Double.MAX_VALUE
        }
        
        val endTrack = tracks.minByOrNull { track ->
            track.minOfOrNull { point ->
                calculateDistance(endStation.latitude, endStation.longitude, point.latitude, point.longitude)
            } ?: Double.MAX_VALUE
        }
        
        if (startTrack == null || endTrack == null) {
            return emptyList()
        }
        
        // If same track, use it
        if (startTrack == endTrack) {
            val route = mutableListOf<LatLng>()
            var startIndex = 0
            var endIndex = startTrack.size - 1
            var minStartDist = Double.MAX_VALUE
            var minEndDist = Double.MAX_VALUE
            
            startTrack.forEachIndexed { index, point ->
                val distToStart = calculateDistance(startStation.latitude, startStation.longitude, point.latitude, point.longitude)
                val distToEnd = calculateDistance(endStation.latitude, endStation.longitude, point.latitude, point.longitude)
                
                if (distToStart < minStartDist) {
                    minStartDist = distToStart
                    startIndex = index
                }
                if (distToEnd < minEndDist) {
                    minEndDist = distToEnd
                    endIndex = index
                }
            }
            
            // Ensure we go forward (startIndex < endIndex)
            if (startIndex < endIndex) {
                for (i in startIndex..endIndex) {
                    route.add(startTrack[i])
                }
            } else {
                // Reverse direction
                for (i in endIndex..startIndex) {
                    route.add(startTrack[i])
                }
            }
            
            return route
        }
        
        // Different tracks - connect them
        val route = mutableListOf<LatLng>()
        
        // Find closest point to center on start track
        var startIndex = 0
        var minDist = Double.MAX_VALUE
        startTrack.forEachIndexed { index, point ->
            val dist = calculateDistance(centerLat, centerLng, point.latitude, point.longitude)
            if (dist < minDist) {
                minDist = dist
                startIndex = index
            }
        }
        
        // Add points forward from start
        for (i in startIndex until startTrack.size) {
            route.add(startTrack[i])
        }
        
        // Add points from end track
        var endIndex = 0
        minDist = Double.MAX_VALUE
        endTrack.forEachIndexed { index, point ->
            val dist = calculateDistance(centerLat, centerLng, point.latitude, point.longitude)
            if (dist < minDist) {
                minDist = dist
                endIndex = index
            }
        }
        
        for (i in endIndex until endTrack.size) {
            route.add(endTrack[i])
        }
        
        return route
    }
    
    /**
     * Create route toward a station along railway tracks
     */
    private fun createRouteTowardStation(
        centerLat: Double,
        centerLng: Double,
        station: LatLng,
        tracks: List<List<LatLng>>,
        routeLengthMeters: Double
    ): List<LatLng> {
        // Find track closest to center
        val closestTrack = tracks.minByOrNull { track ->
            track.minOfOrNull { point ->
                calculateDistance(centerLat, centerLng, point.latitude, point.longitude)
            } ?: Double.MAX_VALUE
        } ?: return emptyList()
        
        // Find point on track closest to center
        var closestIndex = 0
        var minDist = Double.MAX_VALUE
        closestTrack.forEachIndexed { index, point ->
            val dist = calculateDistance(centerLat, centerLng, point.latitude, point.longitude)
            if (dist < minDist) {
                minDist = dist
                closestIndex = index
            }
        }
        
        // Find point on track closest to station
        var stationIndex = 0
        var minStationDist = Double.MAX_VALUE
        closestTrack.forEachIndexed { index, point ->
            val dist = calculateDistance(station.latitude, station.longitude, point.latitude, point.longitude)
            if (dist < minStationDist) {
                minStationDist = dist
                stationIndex = index
            }
        }
        
        val route = mutableListOf<LatLng>()
        
        // Determine direction: go toward station
        if (closestIndex < stationIndex) {
            // Go forward
            for (i in closestIndex until minOf(stationIndex + 5, closestTrack.size)) {
                route.add(closestTrack[i])
                if (route.size * 100 > routeLengthMeters) break
            }
        } else {
            // Go backward
            for (i in closestIndex downTo maxOf(stationIndex - 5, 0)) {
                route.add(closestTrack[i])
                if (route.size * 100 > routeLengthMeters) break
            }
        }
        
        return route
    }
    
    /**
     * Fallback route if no railway tracks are found
     * Creates a straight line route (not ideal, but better than nothing)
     */
    private fun createFallbackMetroRoute(
        centerLat: Double,
        centerLng: Double,
        routeLengthMeters: Double
    ): List<LatLng> {
        val radiusDegrees = routeLengthMeters / 111000.0
        val numPoints = 20
        
        return (0 until numPoints).map { i ->
            val progress = i.toDouble() / (numPoints - 1)
            val lat = centerLat + radiusDegrees * (progress - 0.5) * 2
            LatLng(lat, centerLng)
        }
    }
    
    /**
     * Calculate distance between two points in meters (Haversine formula)
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}

