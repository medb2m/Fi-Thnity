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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import tn.esprit.fithnity.ui.navigation.SearchState
import androidx.navigation.NavGraph.Companion.findStartDestination
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R
import tn.esprit.fithnity.utils.rememberLocationState
import android.widget.Toast
import android.location.Geocoder
import android.location.Address
import java.io.IOException
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import tn.esprit.fithnity.ui.home.VehicleTrackingFAB
import tn.esprit.fithnity.ui.home.PublicTransportManager
import tn.esprit.fithnity.ui.home.PublicTransportSearchDialog
import tn.esprit.fithnity.ui.home.MetroLineSelectionDialog
import tn.esprit.fithnity.ui.home.BusNumberInputDialog
import tn.esprit.fithnity.ui.home.PublicTransportConfirmationDialog
import tn.esprit.fithnity.ui.home.MetroLineConfirmationDialog
import tn.esprit.fithnity.ui.home.BusNumberConfirmationDialog
import tn.esprit.fithnity.services.VehicleWebSocketClient
import tn.esprit.fithnity.data.NetworkModule
import tn.esprit.fithnity.data.BroadcastNotificationRequest

/**
 * Modern Home Screen with MapLibre
 * Welcome banner only shows on first app load
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showWelcomeBanner: Boolean = false,
    onWelcomeBannerDismissed: () -> Unit = {},
    sharedLocationLat: Double? = null,
    sharedLocationLng: Double? = null,
    sharedLocationUserName: String? = null,
    sharedLocationUserPhoto: String? = null,
    showPublicTransportDialog: Boolean = false,
    onPublicTransportDialogShown: () -> Unit = {},
    showPublicTransportConfirmationDialog: Boolean = false
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
    
    // Get auth token for API calls
    val userPreferences = remember { tn.esprit.fithnity.data.UserPreferences(context) }
    val authToken = remember { userPreferences.getAuthToken() }
    val coroutineScope = rememberCoroutineScope()
    
    // Public Transport State - use state-based approach (no navigation)
    var showPublicTransportSearchDialog by remember { 
        mutableStateOf(false) 
    }
    var showPublicTransportConfirmationDialogState by remember { 
        mutableStateOf(false) 
    }
    
    // Show dialog when state changes (no navigation involved)
    LaunchedEffect(showPublicTransportDialog) {
        if (showPublicTransportDialog) {
            showPublicTransportSearchDialog = true
            // Notify that dialog has been shown
            onPublicTransportDialogShown()
        }
    }
    
    // Show confirmation dialog when coming from notification
    LaunchedEffect(showPublicTransportConfirmationDialog) {
        if (showPublicTransportConfirmationDialog) {
            showPublicTransportConfirmationDialogState = true
        }
    }
    
    var showMetroLineSelectionDialog by remember { mutableStateOf(false) }
    var showBusNumberInputDialog by remember { mutableStateOf(false) }
    var showMetroLineConfirmationDialog by remember { mutableStateOf(false) }
    var showBusNumberConfirmationDialog by remember { mutableStateOf(false) }
    
    // Public Transport Manager
    val webSocketClient = remember { VehicleWebSocketClient() }
    var publicTransportManager by remember { mutableStateOf<PublicTransportManager?>(null) }
    var markerManager by remember { mutableStateOf<VehicleMarkerManager?>(null) }
    // Track last location update time
    var lastLocationUpdate by remember { mutableStateOf<Long?>(null) }
    // Track if this is the first location update
    var isFirstLocationUpdate by remember { mutableStateOf(true) }
    // Track when user manually requests to center on location
    var shouldCenterOnLocation by remember { mutableStateOf(0) }
    
    // Search query from global state
    var searchQuery by remember { mutableStateOf(SearchState.searchQuery) }
    
    // Store MapView reference for lifecycle management
    var mapView by remember { mutableStateOf<MapView?>(null) }
    // Store map style for vehicle markers
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    
    // Listen to search state changes for map location search
    LaunchedEffect(Unit) {
        SearchState.setSearchHandler { query ->
            searchQuery = query
            // If query is not empty, try to geocode it
            if (query.isNotBlank() && mapLibreMap != null) {
                try {
                    val geocoder = Geocoder(context, java.util.Locale.getDefault())
                    val allAddresses = geocoder.getFromLocationName(query, 5) ?: emptyList()
                    // Filter to only include addresses in Tunisia
                    val tunisiaAddresses = allAddresses.filter { address ->
                        address.countryName?.equals("Tunisia", ignoreCase = true) == true ||
                        address.countryCode?.equals("TN", ignoreCase = true) == true ||
                        address.countryName?.equals("Tunisie", ignoreCase = true) == true
                    }
                    if (tunisiaAddresses.isNotEmpty()) {
                        val address = tunisiaAddresses[0]
                        val location = LatLng(address.latitude, address.longitude)
                        
                        // Center map on searched location
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(location)
                                .zoom(15.0)
                                .build()
                        )
                        mapLibreMap?.animateCamera(cameraUpdate, 1000)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error geocoding search query", e)
                }
            }
        }
        
        // Listen to address selection from suggestions dropdown
        SearchState.setAddressSelectedHandler { address ->
            if (mapLibreMap != null && mapView != null) {
                try {
                    // Check if lifecycle is in a valid state
                    if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        return@setAddressSelectedHandler
                    }
                    
                    val location = LatLng(address.latitude, address.longitude)
                    
                    // Center map on selected address location
                    val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(location)
                            .zoom(15.0)
                            .build()
                    )
                    mapLibreMap?.animateCamera(cameraUpdate, 1000)
                    
                    // Add a marker for the selected location
                    mapLibreMap?.getStyle { style ->
                        try {
                            addSelectedLocationMarker(style, location)
                        } catch (e: Exception) {
                            android.util.Log.e("HomeScreen", "Error adding selected location marker", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error centering map on selected address", e)
                }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            SearchState.clearSearchHandler()
            SearchState.clearAddressSelectedHandler()
        }
    }
    
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
                                    if (success) {
                                        map.getStyle { style ->
                                            mapStyle = style
                                            // Initialize marker manager and public transport manager
                                            markerManager = VehicleMarkerManager(style, context)
                                            publicTransportManager = PublicTransportManager(
                                                context = context,
                                                mapLibreMap = map,
                                                mapStyle = style,
                                                webSocketClient = webSocketClient,
                                                markerManager = markerManager
                                            )
                                            // Connect WebSocket
                                            webSocketClient.connect()
                                        }
                                    }
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
                // Always request location update
                locationState.requestLocation()
                // If we already have a location, center immediately while waiting for update
                if (locationState.location != null && mapLibreMap != null) {
                    shouldCenterOnLocation++
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 250.dp, end = 20.dp),
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
        
        // Layer 4: Vehicle Tracking FAB (below location FAB)
        VehicleTrackingFAB(
            context = context,
            mapLibreMap = mapLibreMap,
            mapStyle = mapStyle,
            userLocationLat = locationState.location?.latitude,
            userLocationLng = locationState.location?.longitude,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 20.dp)
        )
        
        // Handle location updates and center map
        LaunchedEffect(locationState.location, mapLibreMap, shouldCenterOnLocation) {
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
                    
                    // Update if location is new OR if user manually requested to center
                    val shouldUpdate = lastLocationUpdate == null || 
                        (currentTime - (lastLocationUpdate ?: 0)) > 2000 || // Update at most every 2 seconds for automatic updates
                        shouldCenterOnLocation > 0 // Always update when user manually requests
                    
                    if (shouldUpdate) {
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
        
        // Handle shared location from chat
        LaunchedEffect(sharedLocationLat, sharedLocationLng, mapLibreMap) {
            if (sharedLocationLat != null && sharedLocationLng != null && mapLibreMap != null && mapView != null) {
                // Check if lifecycle is in a valid state
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return@LaunchedEffect
                }
                
                // Use let to ensure map is not null
                mapLibreMap?.let { map ->
                    // Load profile picture in background and add marker
                    withContext(Dispatchers.IO) {
                        val profileBitmap = if (!sharedLocationUserPhoto.isNullOrEmpty() && sharedLocationUserPhoto != "none") {
                            loadProfilePictureBitmap(context, sharedLocationUserPhoto)
                        } else {
                            null
                        }
                        
                        // Switch back to main thread to update map
                        withContext(Dispatchers.Main) {
                            try {
                                val sharedLocation = LatLng(sharedLocationLat, sharedLocationLng)
                                
                                // Center map on shared location
                                val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(sharedLocation)
                                        .zoom(16.0)
                                        .build()
                                )
                                map.animateCamera(cameraUpdate, 1000)
                                
                                // Add shared location marker
                                map.getStyle { style ->
                                    try {
                                        addSharedLocationMarker(
                                            style, 
                                            sharedLocation, 
                                            sharedLocationUserName ?: "User",
                                            profileBitmap
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e("HomeScreen", "Error adding shared location marker", e)
                                    }
                                }
                                
                                // Show toast
                                Toast.makeText(
                                    context,
                                    "${sharedLocationUserName ?: "User"}'s location",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Error displaying shared location", e)
                            }
                        }
                    }
                }
            }
        }
        
        // Public Transport Dialogs
        if (showPublicTransportSearchDialog) {
            PublicTransportSearchDialog(
                onDismiss = { 
                    showPublicTransportSearchDialog = false
                },
                onMetroSelected = { showMetroLineSelectionDialog = true },
                onBusSelected = { showBusNumberInputDialog = true }
            )
        }
        
        if (showMetroLineSelectionDialog) {
            MetroLineSelectionDialog(
                onDismiss = { showMetroLineSelectionDialog = false },
                onLineSelected = { lineNumber ->
                    // Send notification to all users
                    android.util.Log.d("HomeScreen", "User searching for Metro Line $lineNumber")
                    Toast.makeText(context, "Searching for Metro Line $lineNumber", Toast.LENGTH_SHORT).show()
                    
                    // Broadcast notification to all users
                    if (authToken != null) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val response = NetworkModule.notificationApi.broadcastNotification(
                                    bearer = "Bearer $authToken",
                                    request = BroadcastNotificationRequest(
                                        type = "PUBLIC_TRANSPORT_SEARCH",
                                        title = "Someone is looking for Metro Line $lineNumber",
                                        message = "Are you on your way today?",
                                        data = mapOf(
                                            "transportType" to "METRO",
                                            "lineNumber" to lineNumber.toString(),
                                            "searchType" to "METRO_LINE"
                                        )
                                    )
                                )
                                android.util.Log.d("HomeScreen", "Broadcast notification sent: ${response.data?.notifiedCount} users notified")
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Error broadcasting notification", e)
                            }
                        }
                    }
                }
            )
        }
        
        if (showBusNumberInputDialog) {
            BusNumberInputDialog(
                onDismiss = { showBusNumberInputDialog = false },
                onBusSelected = { busNumber ->
                    // Send notification to all users
                    android.util.Log.d("HomeScreen", "User searching for Bus $busNumber")
                    Toast.makeText(context, "Searching for Bus $busNumber", Toast.LENGTH_SHORT).show()
                    
                    // Broadcast notification to all users
                    if (authToken != null) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val response = NetworkModule.notificationApi.broadcastNotification(
                                    bearer = "Bearer $authToken",
                                    request = BroadcastNotificationRequest(
                                        type = "PUBLIC_TRANSPORT_SEARCH",
                                        title = "Someone is looking for Bus $busNumber",
                                        message = "Are you on your way today?",
                                        data = mapOf(
                                            "transportType" to "BUS",
                                            "busNumber" to busNumber,
                                            "searchType" to "BUS_NUMBER"
                                        )
                                    )
                                )
                                android.util.Log.d("HomeScreen", "Broadcast notification sent: ${response.data?.notifiedCount} users notified")
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Error broadcasting notification", e)
                            }
                        }
                    }
                }
            )
        }
        
        if (showPublicTransportConfirmationDialogState) {
            PublicTransportConfirmationDialog(
                onDismiss = { showPublicTransportConfirmationDialogState = false },
                onMetroSelected = { showMetroLineConfirmationDialog = true },
                onBusSelected = { showBusNumberConfirmationDialog = true }
            )
        }
        
        if (showMetroLineConfirmationDialog) {
            MetroLineConfirmationDialog(
                onDismiss = { showMetroLineConfirmationDialog = false },
                onLineConfirmed = { lineNumber ->
                    // Start metro simulation
                    publicTransportManager?.startMetroSimulation(
                        metroLine = lineNumber,
                        userLocationLat = locationState.location?.latitude,
                        userLocationLng = locationState.location?.longitude,
                        onStatusUpdate = { status ->
                            android.util.Log.d("HomeScreen", "Metro simulation: $status")
                        }
                    )
                    Toast.makeText(context, "Sharing Metro Line $lineNumber location", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        if (showBusNumberConfirmationDialog) {
            BusNumberConfirmationDialog(
                onDismiss = { showBusNumberConfirmationDialog = false },
                onBusConfirmed = { busNumber ->
                    // Start bus simulation
                    publicTransportManager?.startBusSimulation(
                        busNumber = busNumber,
                        userLocationLat = locationState.location?.latitude,
                        userLocationLng = locationState.location?.longitude,
                        onStatusUpdate = { status ->
                            android.util.Log.d("HomeScreen", "Bus simulation: $status")
                        }
                    )
                    Toast.makeText(context, "Sharing Bus $busNumber location", Toast.LENGTH_SHORT).show()
                }
            )
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
 * Add or update selected location marker on the map (from search suggestions)
 */
private fun addSelectedLocationMarker(mapStyle: Style, location: LatLng) {
    try {
        // Create GeoJSON point for selected location
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
        
        // Remove existing selected location layer and source
        mapStyle.getLayer("selected-location-layer")?.let {
            mapStyle.removeLayer(it)
        }
        mapStyle.getSourceAs<GeoJsonSource>("selected-location-source")?.let {
            mapStyle.removeSource(it)
        }
        
        // Add source for selected location
        val source = GeoJsonSource("selected-location-source", featureJson.toString())
        mapStyle.addSource(source)
        
        // Add symbol layer for selected location icon
        val symbolLayer = SymbolLayer("selected-location-layer", "selected-location-source")
            .withProperties(
                org.maplibre.android.style.layers.PropertyFactory.iconImage("selected-location-icon"),
                org.maplibre.android.style.layers.PropertyFactory.iconSize(1.5f),
                org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true),
                org.maplibre.android.style.layers.PropertyFactory.iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM)
            )
        
        // Add a custom icon for selected location (red/orange color to distinguish from user location)
        val bitmap = android.graphics.Bitmap.createBitmap(56, 56, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FF6B6B") // Red color for selected location
            this.style = android.graphics.Paint.Style.FILL
        }
        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            this.style = android.graphics.Paint.Style.STROKE
            strokeWidth = 5f
        }
        // Draw a pin-like shape (circle with a point at bottom)
        canvas.drawCircle(28f, 20f, 16f, paint)
        canvas.drawCircle(28f, 20f, 16f, strokePaint)
        // Draw a small triangle at the bottom to make it look like a pin
        val path = android.graphics.Path()
        path.moveTo(28f, 36f)
        path.lineTo(20f, 50f)
        path.lineTo(36f, 50f)
        path.close()
        canvas.drawPath(path, paint)
        canvas.drawPath(path, strokePaint)
        
        mapStyle.addImage("selected-location-icon", bitmap)
        mapStyle.addLayer(symbolLayer)
        
    } catch (e: Exception) {
        android.util.Log.e("HomeScreen", "Error adding selected location marker", e)
    }
}

/**
 * Load profile picture from URL and convert to circular bitmap
 */
private suspend fun loadProfilePictureBitmap(context: android.content.Context, photoUrl: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val fullPhotoUrl = if (photoUrl.startsWith("http")) {
                photoUrl
            } else {
                "http://72.61.145.239:9090$photoUrl"
            }
            
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(fullPhotoUrl)
                .allowHardware(false) // Disable hardware bitmaps for manipulation
                .build()
            
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                bitmap?.let { makeCircularBitmap(it, 64) } // 64px circular profile picture
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error loading profile picture", e)
            null
        }
    }
}

/**
 * Convert bitmap to circular shape
 */
private fun makeCircularBitmap(bitmap: Bitmap, size: Int): Bitmap {
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    
    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    
    // Draw circle
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Apply source-in mode to clip the bitmap to the circle
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    
    // Scale and center the bitmap
    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
    val dstRect = Rect(0, 0, size, size)
    canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    
    return output
}

/**
 * Add or update shared location marker on the map (from chat)
 * Shows the user's profile picture and name
 */
private fun addSharedLocationMarker(
    mapStyle: Style, 
    location: LatLng, 
    userName: String,
    profileBitmap: Bitmap?
) {
    try {
        // Create GeoJSON point for shared location
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
            put("properties", JSONObject().apply {
                put("userName", userName)
            })
        }
        
        // Remove existing shared location layer and source
        mapStyle.getLayer("shared-location-layer")?.let {
            mapStyle.removeLayer(it)
        }
        mapStyle.getSourceAs<GeoJsonSource>("shared-location-source")?.let {
            mapStyle.removeSource(it)
        }
        
        // Add source for shared location
        val source = GeoJsonSource("shared-location-source", featureJson.toString())
        mapStyle.addSource(source)
        
        // Add symbol layer for shared location icon
        val symbolLayer = SymbolLayer("shared-location-layer", "shared-location-source")
            .withProperties(
                org.maplibre.android.style.layers.PropertyFactory.iconImage("shared-location-icon"),
                org.maplibre.android.style.layers.PropertyFactory.iconSize(1.0f),
                org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true),
                org.maplibre.android.style.layers.PropertyFactory.iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM)
            )
        
        // Create custom marker with name drawn directly into bitmap (no MapLibre text layer needed!)
        val bitmap = createSharedLocationMarkerBitmap(profileBitmap, userName)
        
        mapStyle.addImage("shared-location-icon", bitmap)
        mapStyle.addLayer(symbolLayer)
        
    } catch (e: Exception) {
        android.util.Log.e("HomeScreen", "Error adding shared location marker", e)
    }
}

/**
 * Create marker bitmap with profile picture and NAME DRAWN DIRECTLY INTO BITMAP
 * Optimized for performance - GREEN marker for shared location (vs blue for user location)
 * NO dependency on MapLibre text layers - works even when MapTiler fonts fail!
 */
private fun createSharedLocationMarkerBitmap(profileBitmap: Bitmap?, userName: String): Bitmap {
    // Create larger bitmap to include name label above marker
    val bitmap = Bitmap.createBitmap(120, 150, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Use GREEN color for shared location (different from user's blue)
    val markerColor = android.graphics.Color.parseColor("#22C55E") // Green
    
    // Draw pulsing circle background
    val pulsePaint = Paint().apply {
        isAntiAlias = true
        color = markerColor
        alpha = 60
        this.style = Paint.Style.FILL
    }
    canvas.drawCircle(60f, 60f, 38f, pulsePaint)
    
    // Draw main pin background circle
    val pinPaint = Paint().apply {
        isAntiAlias = true
        color = markerColor
        this.style = Paint.Style.FILL
    }
    val strokePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        this.style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    
    // Draw circular head of pin
    canvas.drawCircle(60f, 60f, 28f, pinPaint)
    canvas.drawCircle(60f, 60f, 28f, strokePaint)
    
    // Draw profile picture if available, otherwise draw person icon
    if (profileBitmap != null) {
        // Create a smaller white background circle for the profile picture
        val innerCirclePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            this.style = Paint.Style.FILL
        }
        canvas.drawCircle(60f, 60f, 22f, innerCirclePaint)
        
        // Draw the profile picture as a circle
        val profilePaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // Save layer for circular clip
        val saveCount = canvas.saveLayer(0f, 0f, 120f, 150f, null)
        
        // Create circular clip for profile picture
        val clipPaint = Paint().apply {
            isAntiAlias = true
        }
        canvas.drawCircle(60f, 60f, 20f, clipPaint)
        
        // Apply source-in mode
        profilePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        // Draw profile picture centered
        val left = 60f - 20f
        val top = 60f - 20f
        val right = 60f + 20f
        val bottom = 60f + 20f
        canvas.drawBitmap(
            profileBitmap,
            null,
            RectF(left, top, right, bottom),
            profilePaint
        )
        
        canvas.restoreToCount(saveCount)
    } else {
        // Draw inner white circle for default icon
        val innerPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            this.style = Paint.Style.FILL
        }
        canvas.drawCircle(60f, 60f, 20f, innerPaint)
        
        // Draw person icon in the center
        val iconPaint = Paint().apply {
            isAntiAlias = true
            color = markerColor
            this.style = Paint.Style.FILL
        }
        // Simple person icon - head
        canvas.drawCircle(60f, 55f, 7f, iconPaint)
        // Body (simplified)
        val bodyPath = android.graphics.Path()
        bodyPath.moveTo(52f, 68f)
        bodyPath.lineTo(60f, 60f)
        bodyPath.lineTo(68f, 68f)
        bodyPath.close()
        canvas.drawPath(bodyPath, iconPaint)
    }
    
    // Draw pin point (triangle at bottom)
    val pinPath = android.graphics.Path()
    pinPath.moveTo(60f, 88f)
    pinPath.lineTo(52f, 108f)
    pinPath.lineTo(68f, 108f)
    pinPath.close()
    canvas.drawPath(pinPath, pinPaint)
    canvas.drawPath(pinPath, strokePaint)
    
    // ===== DRAW NAME LABEL ABOVE THE PIN =====
    // This is the KEY FIX - name drawn directly into bitmap!
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(4f, 0f, 0f, markerColor) // Green shadow/halo effect
    }
    
    // Truncate name if too long (max 10 chars)
    val displayName = if (userName.length > 10) {
        userName.substring(0, 9) + "…"
    } else {
        userName
    }
    
    // Draw name label above the marker
    canvas.drawText(displayName, 60f, 28f, textPaint)
    
    return bitmap
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
