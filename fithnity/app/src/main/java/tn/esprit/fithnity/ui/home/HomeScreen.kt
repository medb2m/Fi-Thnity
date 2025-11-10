package tn.esprit.fithnity.ui.home

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

/**
 * Modern Home Screen with MapLibre and Quick Actions
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // MapLibre is initialized in FiThnityApplication.onCreate()
    // Track if map is loading
    var mapLoadError by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
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

        // Background Map using MapLibre
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

        // Map loading error indicator
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

        // Top Glass Card with Welcome Message
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Welcome Message
                Text(
                    text = "Welcome back! 👋",
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
                        text = "Tunis, Tunisia",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Quick Actions Card at Bottom
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quick Actions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Need a Ride Button
                    QuickActionButton(
                        title = "I Need a Ride",
                        icon = Icons.Default.Search,
                        color = Primary,
                        onClick = {
                            navController.navigate(Screen.DemandRide.route)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Offer a Ride Button
                    QuickActionButton(
                        title = "I Offer a Ride",
                        icon = Icons.Default.Share,
                        color = Accent,
                        onClick = {
                            navController.navigate(Screen.OfferRide.route)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Active Rides Indicator
                Text(
                    text = "No active rides",
                    fontSize = 14.sp,
                    color = TextTertiary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Floating Action Button for Current Location
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
