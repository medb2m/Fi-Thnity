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
import tn.esprit.fithnity.BuildConfig
import tn.esprit.fithnity.ui.components.GlassCard
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R

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
    // MapLibre is initialized in FiThnityApplication.onCreate()
    // Track if map is loading
    var mapLoadError by remember { mutableStateOf(false) }
    // Track welcome banner visibility (local state)
    var bannerVisible by remember(showWelcomeBanner) { mutableStateOf(showWelcomeBanner) }
    
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

        // Bottom Layer: Map (full background)
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    getMapAsync { map ->
                        setupMapStyle(map) { success ->
                            mapLoadError = !success
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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

                    // Location Display
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tunis_tunisia),
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

        }

        // Layer 3: Floating Action Button for Current Location (on top of map)
        FloatingActionButton(
            onClick = {
                // TODO: Center map on current location
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 20.dp),
            containerColor = Surface,
            contentColor = Primary
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location"
            )
        }
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
                    // Style loaded successfully
                    // Set initial camera position (Tunis, Tunisia)
                    val tunisLocation = LatLng(36.8065, 10.1815)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(tunisLocation)
                        .zoom(12.0)
                        .build()
                    onResult(true)
                } catch (e: Exception) {
                    e.printStackTrace()
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
