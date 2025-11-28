package tn.esprit.fithnity.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.collectAsState
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import tn.esprit.fithnity.data.VehicleType
import tn.esprit.fithnity.services.VehicleLocationService
import tn.esprit.fithnity.services.VehicleWebSocketClient
import tn.esprit.fithnity.ui.home.VehicleMarkerManager
import tn.esprit.fithnity.ui.theme.*

/**
 * Vehicle tracking FAB and management
 */
@Composable
fun VehicleTrackingFAB(
    context: Context,
    mapLibreMap: MapLibreMap?,
    mapStyle: Style?,
    modifier: Modifier = Modifier
) {
    var isSharing by remember { mutableStateOf(false) }
    var showVehicleTypeDialog by remember { mutableStateOf(false) }
    var selectedVehicleType by remember { mutableStateOf(VehicleType.CAR) }
    
    // Vehicle WebSocket client for receiving positions
    val webSocketClient = remember { VehicleWebSocketClient() }
    val vehiclePositions by webSocketClient.vehiclePositions.collectAsState()
    val isConnected by webSocketClient.isConnected.collectAsState()
    
    // Vehicle marker manager
    var markerManager by remember { mutableStateOf<VehicleMarkerManager?>(null) }
    
    // Initialize marker manager when map style is ready
    LaunchedEffect(mapStyle) {
        mapStyle?.let {
            markerManager = VehicleMarkerManager(it)
        }
    }
    
    // Connect WebSocket when map is ready
    LaunchedEffect(mapLibreMap, isConnected) {
        if (mapLibreMap != null && !isConnected) {
            webSocketClient.connect()
        }
    }
    
    // Update vehicle markers when positions change
    LaunchedEffect(vehiclePositions) {
        markerManager?.let { manager ->
            vehiclePositions.values.forEach { position ->
                manager.updateVehiclePosition(position)
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            webSocketClient.disconnect()
            markerManager?.clearAll()
        }
    }
    
    // FAB Button
    FloatingActionButton(
        onClick = {
            if (isSharing) {
                // Stop sharing
                val stopIntent = Intent(context, VehicleLocationService::class.java).apply {
                    action = VehicleLocationService.ACTION_STOP_TRACKING
                }
                context.stopService(stopIntent)
                isSharing = false
            } else {
                // Show vehicle type selection
                showVehicleTypeDialog = true
            }
        },
        modifier = modifier,
        containerColor = if (isSharing) Error else Primary
    ) {
        Icon(
            imageVector = if (isSharing) Icons.Default.Stop else Icons.Default.Navigation,
            contentDescription = if (isSharing) "Stop sharing" else "Start sharing",
            tint = Color.White
        )
    }
    
    // Vehicle type selection dialog
    if (showVehicleTypeDialog) {
        VehicleTypeSelectionDialog(
            selectedType = selectedVehicleType,
            onTypeSelected = { type ->
                selectedVehicleType = type
                showVehicleTypeDialog = false
                
                // Start location sharing service
                val startIntent = Intent(context, VehicleLocationService::class.java).apply {
                    action = VehicleLocationService.ACTION_START_TRACKING
                    putExtra(VehicleLocationService.EXTRA_VEHICLE_TYPE, type.name)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(startIntent)
                } else {
                    context.startService(startIntent)
                }
                
                isSharing = true
            },
            onDismiss = { showVehicleTypeDialog = false }
        )
    }
    
    // Connection status indicator
    if (!isConnected) {
        Surface(
            modifier = Modifier
                .padding(bottom = 80.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Error.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Connecting to vehicle tracking...",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Vehicle type selection dialog
 */
@Composable
private fun VehicleTypeSelectionDialog(
    selectedType: VehicleType,
    onTypeSelected: (VehicleType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Select Vehicle Type",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(16.dp))
                
                VehicleType.values().forEach { type ->
                    VehicleTypeItem(
                        type = type,
                        isSelected = type == selectedType,
                        onClick = {
                            onTypeSelected(type)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun VehicleTypeItem(
    type: VehicleType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Primary.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (isSelected) Primary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = type.displayName,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Primary else TextPrimary
            )
            Spacer(Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

