package tn.esprit.fithnity.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
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
class VehicleMarkerManager(
    private val mapStyle: Style,
    private val context: android.content.Context
) {
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
        try {
            VehicleType.values().forEach { type ->
                try {
                    val bitmap = createVehicleIcon(type)
                    val iconName = "vehicle_${type.iconName}"
                    
                    // Remove existing icon if it exists
                    if (mapStyle.getImage(iconName) != null) {
                        mapStyle.removeImage(iconName)
                    }
                    
                    mapStyle.addImage(iconName, bitmap)
                    Log.d(TAG, "Created vehicle icon: $iconName")
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating icon for ${type.name}", e)
                }
            }
            Log.d(TAG, "All vehicle icons created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating vehicle icons", e)
        }
    }
    
    /**
     * Create a bitmap icon for vehicle type using drawable resources
     */
    private fun createVehicleIcon(type: VehicleType): Bitmap {
        val size = 64
        
        // Get drawable resource ID based on vehicle type
        // For PNG files, use R.drawable directly
        val drawableResId = when (type) {
            VehicleType.CAR -> tn.esprit.fithnity.R.drawable.ic_vehicle_car
            VehicleType.BUS -> tn.esprit.fithnity.R.drawable.ic_vehicle_bus
            VehicleType.MINIBUS -> tn.esprit.fithnity.R.drawable.ic_vehicle_minibus
            VehicleType.METRO -> tn.esprit.fithnity.R.drawable.ic_vehicle_metro
            VehicleType.TAXI -> tn.esprit.fithnity.R.drawable.ic_vehicle_taxi
            VehicleType.MOTORCYCLE -> tn.esprit.fithnity.R.drawable.ic_vehicle_motorcycle
        }
        
        return try {
            // Load drawable (works for both PNG and XML vector drawables)
            val drawable = ContextCompat.getDrawable(context, drawableResId)
                ?: throw IllegalArgumentException("Drawable not found for ${type.name}")
            
            // Create bitmap from drawable
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Set bounds and draw
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading vehicle icon for ${type.name}, using fallback", e)
            // Fallback: create simple colored circle
            createFallbackIcon(type, size)
        }
    }
    
    /**
     * Fallback icon creation if vector drawable fails
     */
    private fun createFallbackIcon(type: VehicleType, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val color = when (type) {
            VehicleType.CAR -> android.graphics.Color.parseColor("#3D8BFD")
            VehicleType.BUS -> android.graphics.Color.parseColor("#FF6B6B")
            VehicleType.MINIBUS -> android.graphics.Color.parseColor("#FFA500")
            VehicleType.METRO -> android.graphics.Color.parseColor("#9B59B6")
            VehicleType.TAXI -> android.graphics.Color.parseColor("#F1C40F")
            VehicleType.MOTORCYCLE -> android.graphics.Color.parseColor("#E74C3C")
        }
        
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = color
            this.style = Paint.Style.FILL
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
        
        return bitmap
    }
    
    /**
     * Update vehicle position with smooth animation
     */
    fun updateVehiclePosition(position: VehiclePosition) {
        try {
            Log.d(TAG, "updateVehiclePosition called for ${position.vehicleId}: ${position.lat}, ${position.lng}")
            
            val vehicleType = try {
                VehicleType.valueOf(position.type)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown vehicle type: ${position.type}, defaulting to CAR")
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
                Log.d(TAG, "Updated existing marker for ${position.vehicleId}")
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
                Log.d(TAG, "Created new marker for ${position.vehicleId}")
            }
            
            updateMapMarkers()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vehicle position", e)
            e.printStackTrace()
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
     * Path lines are added first (below), then markers (on top)
     */
    private fun updateMapMarkers() {
        // Update path lines FIRST (so they appear below markers)
        updatePathLines()
        
        // Then update markers (so they appear on top of path lines)
        vehicleMarkers.values.forEach { marker ->
            updateMapMarker(marker)
        }
    }
    
    /**
     * Update single vehicle marker on map
     */
    private fun updateMapMarker(marker: VehicleMarkerData) {
        try {
            val sourceId = "vehicle_source_${marker.vehicleId}"
            val layerId = "vehicle_layer_${marker.vehicleId}"
            val iconName = "vehicle_${marker.type.iconName}"
            
            // Ensure icon exists
            if (mapStyle.getImage(iconName) == null) {
                Log.w(TAG, "Icon $iconName not found, recreating icons")
                createVehicleIcons()
            }
            
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
                    // CAR and METRO always point down (0 rotation), others rotate with bearing
                    val rotation = when (marker.type) {
                        VehicleType.CAR, VehicleType.METRO -> 0f
                        else -> marker.bearing
                    }
                    put("bearing", rotation)
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
            
            // Add marker layer ABOVE its corresponding path line (so icons appear on top)
            val pathLayerId = "vehicle_path_layer_${marker.vehicleId}"
            val pathLayer = mapStyle.getLayer(pathLayerId)
            
            if (pathLayer != null) {
                // Add marker above its path line
                mapStyle.addLayerAbove(symbolLayer, pathLayer.id)
            } else {
                // If no path line exists yet, add marker normally (it will be on top)
                mapStyle.addLayer(symbolLayer)
            }
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
            
            // Add line layer (path lines should be below markers)
            val lineLayer = org.maplibre.android.style.layers.LineLayer(pathLayerId, pathSourceId)
                .withProperties(
                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#3D8BFD")),
                    PropertyFactory.lineWidth(3f),
                    PropertyFactory.lineOpacity(0.6f)
                )
            
            // Always add path lines at the bottom (before any marker layers)
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

