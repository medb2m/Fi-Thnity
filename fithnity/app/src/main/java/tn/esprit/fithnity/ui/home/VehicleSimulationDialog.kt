package tn.esprit.fithnity.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import tn.esprit.fithnity.data.VehicleType
import tn.esprit.fithnity.ui.theme.*
import tn.esprit.fithnity.utils.VehicleLocationSimulator

/**
 * Dialog for starting vehicle location simulation
 */
@Composable
fun VehicleSimulationDialog(
    onDismiss: () -> Unit,
    onStartSimulation: (VehicleType, Float) -> Unit
) {
    var selectedType by remember { mutableStateOf(VehicleType.CAR) }
    var selectedSpeed by remember { mutableStateOf(50f) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Simulate Vehicle Movement",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Vehicle will follow real Tunisian streets near your location",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Vehicle Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Vehicle type selection
                VehicleType.values().forEach { type ->
                    VehicleTypeItem(
                        type = type,
                        isSelected = type == selectedType,
                        onClick = { selectedType = type }
                    )
                    Spacer(Modifier.height(4.dp))
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Speed: ${selectedSpeed.toInt()} km/h",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Speed slider
                Slider(
                    value = selectedSpeed,
                    onValueChange = { selectedSpeed = it },
                    valueRange = 20f..120f,
                    steps = 9
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("20 km/h", fontSize = 12.sp, color = TextHint)
                    Text("120 km/h", fontSize = 12.sp, color = TextHint)
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            onStartSimulation(selectedType, selectedSpeed)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Start")
                    }
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (isSelected) Primary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = type.displayName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Primary else TextPrimary
            )
            Spacer(Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

