package tn.esprit.fithnity.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.json.JSONObject
import org.json.JSONArray
import tn.esprit.fithnity.BuildConfig
import tn.esprit.fithnity.ui.components.GlassCard
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R
import tn.esprit.fithnity.utils.rememberLocationState
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

/**
 * Modern Home Screen with MapLibre
 * Welcome banner only shows on first app load
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showWelcomeBanner: Boolean = false,
    onWelcomeBannerDismissed: () -> Unit = {}
) {
    // Track if map is loading
    var mapLoadError by remember { mutableStateOf(false) }
    // Track welcome banner visibility (local state)
    var bannerVisible by remember(showWelcomeBanner) { mutableStateOf(showWelcomeBanner) }
    // Store reference to the map for location updates
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    // Track if MapView should be created (deferred to prevent blocking)
    var shouldCreateMapView by remember { mutableStateOf(false) }
    // Location state
    val locationState = rememberLocationState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Track if we should follow user location
    var isFollowingLocation by remember { mutableStateOf(false) }
    // Track last location update time
    var lastLocationUpdate by remember { mutableStateOf<Long?>(null) }
    // Track if this is the first location update
    var isFirstLocationUpdate by remember { mutableStateOf(true) }
    // Store MapView reference for lifecycle management
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Initialize MapLibre lazily and defer MapView creation to prevent ANR
    LaunchedEffect(Unit) {
        // Ensure MapLibre is initialized before creating MapView
        val app = context.applicationContext as? tn.esprit.fithnity.FiThnityApplication
        app?.ensureMapLibreInitialized()
        
        // Small delay to allow UI to render first, then create MapView
        kotlinx.coroutines.delay(50)
        shouldCreateMapView = true
    }
    
    // Manage MapView lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            mapView?.let { view ->
                try {
                    when (event) {
                        Lifecycle.Event.ON_START -> view.onStart()
                        Lifecycle.Event.ON_RESUME -> view.onResume()
                        Lifecycle.Event.ON_PAUSE -> view.onPause()
                        Lifecycle.Event.ON_STOP -> view.onStop()
                        Lifecycle.Event.ON_DESTROY -> {
                            view.onDestroy()
                            mapView = null
                            mapLibreMap = null
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error in MapView lifecycle: ${event.name}", e)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Clean up MapView on dispose
            mapView?.let { view ->
                try {
                    view.onPause()
                    view.onStop()
                    view.onDestroy()
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error destroying MapView", e)
                }
            }
            mapView = null
            mapLibreMap = null
        }
    }
    
    // Auto-dismiss welcome banner after 3 seconds if it should be shown
    LaunchedEffect(showWelcomeBanner) {
        if (showWelcomeBanner) {
            kotlinx.coroutines.delay(3000L)
            bannerVisible = false
            // Notify parent that banner has been dismissed
            onWelcomeBannerDismissed()
        }
    }

    // Map is the bottom layer - takes entire background
    Box(modifier = modifier.fillMaxSize()) {
        // Bottom Layer: Map (full background)
        // Fallback background when map fails to load
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.1f),
                            Background
                        )
                    )
                )
        )

        // Bottom Layer: Map (full background) - created after initial render to prevent ANR
        if (shouldCreateMapView) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapView = this
                        // Call onStart immediately after creation
                        try {
                            onStart()
                            onResume()
                        } catch (e: Exception) {
                            android.util.Log.e("HomeScreen", "Error starting MapView", e)
                        }
                        getMapAsync { map ->
                            try {
                                mapLibreMap = map
                                setupMapStyle(map) { success ->
                                    mapLoadError = !success
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Error in getMapAsync callback", e)
                                mapLoadError = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Update callback - ensure lifecycle is maintained
                    try {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            view.onStart()
                        }
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            view.onResume()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeScreen", "Error updating MapView lifecycle", e)
                    }
                }
            )
        }

        // Layer 1: Map loading error indicator (on top of map)
        if (mapLoadError) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF6B6B).copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Map offline - Check internet connection",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Layer 2: Welcome banner (on top of map)
        // Top Glass Card with Welcome Message (with auto-dismiss animation)
        // Only shows on first app load
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            AnimatedVisibility(
                visible = bannerVisible,
                enter = fadeIn(animationSpec = tween(300)) + 
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + 
                       shrinkVertically(animationSpec = tween(300))
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Welcome Message
                    Text(
                        text = stringResource(R.string.welcome_back),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(8.dp))

                    // Location Display - Show actual location if available
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (locationState.location != null) Primary else TextSecondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (locationState.location != null) {
                                "Lat: ${String.format("%.4f", locationState.location.latitude)}, " +
                                "Lng: ${String.format("%.4f", locationState.location.longitude)}"
                            } else {
                                stringResource(R.string.tunis_tunisia)
                            },
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                    
                    // Show loading indicator if location is being fetched
                    if (locationState.isLoading) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Getting your location...",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

        }

        // Layer 3: Floating Action Button for Current Location (on top of map)
        FloatingActionButton(
            onClick = {
                isFollowingLocation = true
                locationState.requestLocation()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 20.dp),
            containerColor = if (isFollowingLocation && locationState.location != null) Primary else Surface,
            contentColor = if (isFollowingLocation && locationState.location != null) Color.White else Primary
        ) {
            if (locationState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = if (isFollowingLocation) Color.White else Primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location"
                )
            }
        }
        
        // Handle location updates and center map
        LaunchedEffect(locationState.location, mapLibreMap) {
            val location = locationState.location
            val map = mapLibreMap
            
            // Only proceed if map is valid and lifecycle is active
            if (location != null && map != null && mapView != null) {
                // Check if lifecycle is in a valid state
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return@LaunchedEffect
                }
                
                try {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    val currentTime = System.currentTimeMillis()
                    
                    // Only update if location is new (avoid unnecessary updates)
                    if (lastLocationUpdate == null || 
                        (currentTime - (lastLocationUpdate ?: 0)) > 2000) { // Update at most every 2 seconds
                        
                        lastLocationUpdate = currentTime
                        
                        // Verify map is still valid before using
                        if (mapView == null || mapLibreMap == null) {
                            return@LaunchedEffect
                        }
                        
                        // Center map on user location with smooth animation
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(userLocation)
                                .zoom(if (isFollowingLocation) 16.0 else 15.0) // Zoom closer when following
                                .build()
                        )
                        
                        // Use animateCamera for smooth transition
                        map.animateCamera(cameraUpdate, 1000) // 1 second animation
                        
                        // Add or update user location marker
                        map.getStyle { style ->
                            try {
                                addUserLocationMarker(style, userLocation, location.accuracy)
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Error adding user location marker", e)
                            }
                        }
                        
                        // Show success message on first location
                        if (isFirstLocationUpdate) {
                            isFirstLocationUpdate = false
                            Toast.makeText(
                                context,
                                "Location found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error centering map on location", e)
                    // Don't show toast for native crashes, just log
                    if (e !is UnsatisfiedLinkError && e !is NoClassDefFoundError) {
                        Toast.makeText(
                            context,
                            "Error updating location on map: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        
        // Auto-request location when map is ready and permission is granted
        LaunchedEffect(mapLibreMap, locationState.hasPermission) {
            // Only proceed if lifecycle is active
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                return@LaunchedEffect
            }
            
            if (mapLibreMap != null && mapView != null && locationState.hasPermission && locationState.location == null) {
                try {
                    // Auto-request location when map loads (optional - can be removed if not desired)
                    kotlinx.coroutines.delay(500) // Small delay to ensure map is fully loaded
                    // Verify map is still valid before proceeding
                    if (mapLibreMap != null && mapView != null && !locationState.isLoading) {
                        locationState.requestLocation()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error requesting location", e)
                }
            }
        }
        
        // Show error message if location request fails
        LaunchedEffect(locationState.error) {
            locationState.error?.let { errorMessage ->
                isFollowingLocation = false
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
        
        // Show indicator when location is being tracked
        if (isFollowingLocation && locationState.location != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp, 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Tracking location",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add or update user location marker on the map
 */
private fun addUserLocationMarker(mapStyle: Style, location: LatLng, accuracy: Float?) {
    try {
        // Create GeoJSON point for user location
        val pointJson = JSONObject().apply {
            put("type", "Point")
            put("coordinates", JSONArray().apply {
                put(location.longitude)
                put(location.latitude)
            })
        }
        
        val featureJson = JSONObject().apply {
            put("type", "Feature")
            put("geometry", pointJson)
        }
        
        // Remove existing layers first (must remove layers before sources)
        mapStyle.getLayer("user-location-accuracy-layer")?.let {
            mapStyle.removeLayer(it)
        }
        mapStyle.getLayer("user-location-layer")?.let {
            mapStyle.removeLayer(it)
        }
        
        // Remove existing sources
        mapStyle.getSourceAs<GeoJsonSource>("user-location-accuracy-source")?.let {
            mapStyle.removeSource(it)
        }
        mapStyle.getSourceAs<GeoJsonSource>("user-location-source")?.let {
            mapStyle.removeSource(it)
        }
        
        // Add source for user location
        val source = GeoJsonSource("user-location-source", featureJson.toString())
        mapStyle.addSource(source)
        
        // Add accuracy circle if accuracy is available
        if (accuracy != null && accuracy > 0) {
            // Create a circle around the location (accuracy in meters)
            // Approximate: 1 degree latitude ≈ 111 km, so accuracy/111000 gives degrees
            val radiusInDegrees = accuracy / 111000.0
            
            val circleCoordinates = JSONArray()
            for (i in 0..64) {
                val angle = (i * 360.0 / 64) * Math.PI / 180.0
                val lat = location.latitude + radiusInDegrees * Math.cos(angle)
                val lon = location.longitude + radiusInDegrees * Math.sin(angle) / Math.cos(location.latitude * Math.PI / 180.0)
                circleCoordinates.put(JSONArray().apply {
                    put(lon)
                    put(lat)
                })
            }
            // Close the circle
            val firstPoint = circleCoordinates.getJSONArray(0)
            circleCoordinates.put(firstPoint)
            
            val circleJson = JSONObject().apply {
                put("type", "Feature")
                put("geometry", JSONObject().apply {
                    put("type", "Polygon")
                    put("coordinates", JSONArray().apply {
                        put(circleCoordinates)
                    })
                })
            }
            
            val accuracySource = GeoJsonSource("user-location-accuracy-source", circleJson.toString())
            mapStyle.addSource(accuracySource)
            
            // Add accuracy circle layer
            val accuracyLayer = org.maplibre.android.style.layers.FillLayer(
                "user-location-accuracy-layer",
                "user-location-accuracy-source"
            ).withProperties(
                org.maplibre.android.style.layers.PropertyFactory.fillColor(android.graphics.Color.parseColor("#3D8BFD").let { 
                    android.graphics.Color.argb(30, android.graphics.Color.red(it), android.graphics.Color.green(it), android.graphics.Color.blue(it))
                }),
                org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor(android.graphics.Color.parseColor("#3D8BFD").let {
                    android.graphics.Color.argb(100, android.graphics.Color.red(it), android.graphics.Color.green(it), android.graphics.Color.blue(it))
                })
            )
            mapStyle.addLayer(accuracyLayer)
        }
        
        // Add symbol layer for user location icon
        // We'll use a built-in icon or create a custom one
        val symbolLayer = SymbolLayer("user-location-layer", "user-location-source")
            .withProperties(
                org.maplibre.android.style.layers.PropertyFactory.iconImage("user-location-icon"),
                org.maplibre.android.style.layers.PropertyFactory.iconSize(1.2f),
                org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true),
                org.maplibre.android.style.layers.PropertyFactory.iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM)
            )
        
        // Add a custom icon for user location
        // Using a simple circle bitmap
        val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#3D8BFD")
            this.style = android.graphics.Paint.Style.FILL
        }
        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            this.style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(24f, 24f, 18f, paint)
        canvas.drawCircle(24f, 24f, 18f, strokePaint)
        
        mapStyle.addImage("user-location-icon", bitmap)
        mapStyle.addLayer(symbolLayer)
        
    } catch (e: Exception) {
        android.util.Log.e("HomeScreen", "Error adding user location marker", e)
    }
}

/**
 * Setup MapLibre map style with MapTiler
 */
private fun setupMapStyle(map: MapLibreMap, onResult: (Boolean) -> Unit) {
    val apiKey = BuildConfig.MAPTILER_API_KEY
    val mapId = "streets-v2"
    val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$apiKey"

    var callbackCalled = false

    try {
        // Set a timeout to detect if map never loads (network issue)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!callbackCalled) {
                callbackCalled = true
                println("MapLibre: Style loading timed out (likely no internet)")
                onResult(false)
            }
        }, 10000) // 10 second timeout

        map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
            if (!callbackCalled) {
                callbackCalled = true
                try {
                    // Verify map is still valid before using
                    // Style loaded successfully
                    // Set initial camera position (Tunis, Tunisia)
                    val tunisLocation = LatLng(36.8065, 10.1815)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(tunisLocation)
                        .zoom(12.0)
                        .build()
                    onResult(true)
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error setting map style", e)
                    // Don't crash on native errors
                    if (e !is UnsatisfiedLinkError && e !is NoClassDefFoundError) {
                        e.printStackTrace()
                    }
                    onResult(false)
                }
            }
        }
    } catch (e: Exception) {
        if (!callbackCalled) {
            callbackCalled = true
            e.printStackTrace()
            onResult(false)
        }
    }
}

/**
 * Quick Action Button Component
 */
@Composable
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 18.sp
            )
        }
    }
}
