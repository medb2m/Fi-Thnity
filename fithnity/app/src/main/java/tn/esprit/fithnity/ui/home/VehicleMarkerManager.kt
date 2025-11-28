package tn.esprit.fithnity.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.expressions.Expression
import tn.esprit.fithnity.data.VehiclePosition
import tn.esprit.fithnity.data.VehicleType

/**
 * Manages vehicle markers on MapLibre map with smooth animation
 */
class VehicleMarkerManager(private val mapStyle: Style) {
    private val TAG = "VehicleMarkerManager"
    private val vehicleMarkers = mutableMapOf<String, VehicleMarkerData>()
    
    data class VehicleMarkerData(
        val vehicleId: String,
        val currentLocation: LatLng,
        val previousLocation: LatLng?,
        val bearing: Float,
        val type: VehicleType,
        val pathPoints: MutableList<LatLng> = mutableListOf()
    )
    
    init {
        createVehicleIcons()
    }
    
    /**
     * Create icons for different vehicle types
     */
    private fun createVehicleIcons() {
        VehicleType.values().forEach { type ->
            val bitmap = createVehicleIcon(type)
            mapStyle.addImage("vehicle_${type.iconName}", bitmap)
        }
    }
    
    /**
     * Create a bitmap icon for vehicle type
     */
    private fun createVehicleIcon(type: VehicleType): Bitmap {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Base color based on vehicle type
        val color = when (type) {
            VehicleType.CAR -> android.graphics.Color.parseColor("#3D8BFD") // Blue
            VehicleType.BUS -> android.graphics.Color.parseColor("#FF6B6B") // Red
            VehicleType.MINIBUS -> android.graphics.Color.parseColor("#FFA500") // Orange
            VehicleType.METRO -> android.graphics.Color.parseColor("#9B59B6") // Purple
            VehicleType.TAXI -> android.graphics.Color.parseColor("#F1C40F") // Yellow
            VehicleType.MOTORCYCLE -> android.graphics.Color.parseColor("#E74C3C") // Dark Red
        }
        
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = color
            this.style = Paint.Style.FILL
        }
        
        val strokePaint = Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.WHITE
            this.style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        
        // Draw vehicle shape (simplified car icon)
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Main body (rounded rectangle)
        val bodyRect = android.graphics.RectF(centerX - 20f, centerY - 12f, centerX + 20f, centerY + 12f)
        canvas.drawRoundRect(bodyRect, 8f, 8f, paint)
        canvas.drawRoundRect(bodyRect, 8f, 8f, strokePaint)
        
        // Windows
        val windowPaint = Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.parseColor("#87CEEB")
            this.style = Paint.Style.FILL
        }
        canvas.drawRect(centerX - 15f, centerY - 8f, centerX - 5f, centerY + 2f, windowPaint)
        canvas.drawRect(centerX + 5f, centerY - 8f, centerX + 15f, centerY + 2f, windowPaint)
        
        // Wheels
        val wheelPaint = Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.BLACK
            this.style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX - 12f, centerY + 12f, 4f, wheelPaint)
        canvas.drawCircle(centerX + 12f, centerY + 12f, 4f, wheelPaint)
        
        return bitmap
    }
    
    /**
     * Update vehicle position with smooth animation
     */
    fun updateVehiclePosition(position: VehiclePosition) {
        try {
            val vehicleType = try {
                VehicleType.valueOf(position.type)
            } catch (e: Exception) {
                VehicleType.CAR
            }
            
            val newLocation = LatLng(position.lat, position.lng)
            val existing = vehicleMarkers[position.vehicleId]
            
            if (existing != null) {
                // Update existing marker
                val updated = existing.copy(
                    previousLocation = existing.currentLocation,
                    currentLocation = newLocation,
                    bearing = position.bearing,
                    pathPoints = existing.pathPoints.apply {
                        add(newLocation)
                        // Keep only last 50 points for path
                        if (size > 50) removeAt(0)
                    }
                )
                vehicleMarkers[position.vehicleId] = updated
            } else {
                // Create new marker
                vehicleMarkers[position.vehicleId] = VehicleMarkerData(
                    vehicleId = position.vehicleId,
                    currentLocation = newLocation,
                    previousLocation = null,
                    bearing = position.bearing,
                    type = vehicleType,
                    pathPoints = mutableListOf(newLocation)
                )
            }
            
            updateMapMarkers()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vehicle position", e)
        }
    }
    
    /**
     * Remove vehicle marker
     */
    fun removeVehicle(vehicleId: String) {
        vehicleMarkers.remove(vehicleId)
        removeMapMarker(vehicleId)
    }
    
    /**
     * Update all vehicle markers on map
     */
    private fun updateMapMarkers() {
        vehicleMarkers.values.forEach { marker ->
            updateMapMarker(marker)
        }
        
        // Update path lines
        updatePathLines()
    }
    
    /**
     * Update single vehicle marker on map
     */
    private fun updateMapMarker(marker: VehicleMarkerData) {
        try {
            val sourceId = "vehicle_source_${marker.vehicleId}"
            val layerId = "vehicle_layer_${marker.vehicleId}"
            
            // Remove existing
            mapStyle.getLayer(layerId)?.let { mapStyle.removeLayer(it) }
            mapStyle.getSourceAs<GeoJsonSource>(sourceId)?.let { mapStyle.removeSource(it) }
            
            // Create GeoJSON point
            val pointJson = JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(marker.currentLocation.longitude)
                    put(marker.currentLocation.latitude)
                })
            }
            
            val featureJson = JSONObject().apply {
                put("type", "Feature")
                put("geometry", pointJson)
                put("properties", JSONObject().apply {
                    put("bearing", marker.bearing)
                })
            }
            
            // Add source
            val source = GeoJsonSource(sourceId, featureJson.toString())
            mapStyle.addSource(source)
            
            // Add layer with rotation based on bearing
            val symbolLayer = SymbolLayer(layerId, sourceId)
                .withProperties(
                    PropertyFactory.iconImage("vehicle_${marker.type.iconName}"),
                    PropertyFactory.iconSize(1.0f),
                    PropertyFactory.iconRotate(Expression.get("bearing")),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
                )
            
            mapStyle.addLayer(symbolLayer)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating map marker for ${marker.vehicleId}", e)
        }
    }
    
    /**
     * Update path lines showing vehicle movement trail
     */
    private fun updatePathLines() {
        vehicleMarkers.values.forEach { marker ->
            if (marker.pathPoints.size < 2) return@forEach
            
            val pathSourceId = "vehicle_path_source_${marker.vehicleId}"
            val pathLayerId = "vehicle_path_layer_${marker.vehicleId}"
            
            // Remove existing
            mapStyle.getLayer(pathLayerId)?.let { mapStyle.removeLayer(it) }
            mapStyle.getSourceAs<GeoJsonSource>(pathSourceId)?.let { mapStyle.removeSource(it) }
            
            // Create line string from path points
            val coordinates = JSONArray()
            marker.pathPoints.forEach { point ->
                coordinates.put(JSONArray().apply {
                    put(point.longitude)
                    put(point.latitude)
                })
            }
            
            val lineJson = JSONObject().apply {
                put("type", "Feature")
                put("geometry", JSONObject().apply {
                    put("type", "LineString")
                    put("coordinates", coordinates)
                })
            }
            
            val pathSource = GeoJsonSource(pathSourceId, lineJson.toString())
            mapStyle.addSource(pathSource)
            
            // Add line layer
            val lineLayer = org.maplibre.android.style.layers.LineLayer(pathLayerId, pathSourceId)
                .withProperties(
                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#3D8BFD")),
                    PropertyFactory.lineWidth(3f),
                    PropertyFactory.lineOpacity(0.6f)
                )
            
            mapStyle.addLayer(lineLayer)
        }
    }
    
    /**
     * Remove vehicle marker from map
     */
    private fun removeMapMarker(vehicleId: String) {
        try {
            val sourceId = "vehicle_source_$vehicleId"
            val layerId = "vehicle_layer_$vehicleId"
            val pathSourceId = "vehicle_path_source_$vehicleId"
            val pathLayerId = "vehicle_path_layer_$vehicleId"
            
            mapStyle.getLayer(layerId)?.let { mapStyle.removeLayer(it) }
            mapStyle.getLayer(pathLayerId)?.let { mapStyle.removeLayer(it) }
            mapStyle.getSourceAs<GeoJsonSource>(sourceId)?.let { mapStyle.removeSource(it) }
            mapStyle.getSourceAs<GeoJsonSource>(pathSourceId)?.let { mapStyle.removeSource(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing map marker for $vehicleId", e)
        }
    }
    
    /**
     * Clear all vehicle markers
     */
    fun clearAll() {
        vehicleMarkers.keys.forEach { vehicleId ->
            removeMapMarker(vehicleId)
        }
        vehicleMarkers.clear()
    }
}

