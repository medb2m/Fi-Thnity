package tn.esprit.fithnity.ui.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.esprit.fithnity.R
import tn.esprit.fithnity.ui.rides.formatDateTime
import tn.esprit.fithnity.ui.theme.*

/**
 * Ride Type Selection Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideTypeSelectionDialog(
    onDismiss: () -> Unit,
    onRideTypeSelected: (RideType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Ride Type",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "What would you like to do?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                // Offer Option
                Card(
                    onClick = { onRideTypeSelected(RideType.OFFER) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Accent.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Accent
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.offer_ride),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Share your ride with others",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
                
                // Request Option
                Card(
                    onClick = { onRideTypeSelected(RideType.REQUEST) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.need_ride),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Find a ride for your trip",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Ride Details Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailsDialog(
    ride: RideItem,
    onDismiss: () -> Unit,
    onApplyToOffer: () -> Unit,
    onReplyToRequest: () -> Unit,
    currentUserId: String? = null
) {
    val (dateText, timeText) = formatDateTime(ride.time)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ride Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (ride.isOffer) Accent.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (ride.vehicleType == VehicleType.TAXI) Icons.Default.LocalTaxi 
                                         else Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (ride.isOffer) Accent else Primary
                        )
                        Text(
                            text = if (ride.isOffer) "Offer" else "Request",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (ride.isOffer) Accent else Primary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date and Time Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Primary
                        )
                        Column {
                            Text(
                                text = dateText,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = timeText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
                
                // Route Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Origin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "From",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = ride.origin,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                    
                    // Line between origin and destination
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .offset(x = 5.dp)
                            .background(TextHint.copy(alpha = 0.3f))
                    )
                    
                    // Destination
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Accent)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "To",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = ride.destination,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
                
                Divider()
                
                // Driver/Requester Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TextSecondary
                    )
                    Column {
                        Text(
                            text = if (ride.isOffer) "Driver" else "Requester",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = ride.userName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
                
                // Additional Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // First row: Vehicle Type and Seats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Vehicle Type
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (ride.vehicleType == VehicleType.TAXI) Icons.Default.LocalTaxi 
                                             else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TextSecondary
                            )
                            Text(
                                text = if (ride.vehicleType == VehicleType.TAXI) "Taxi" else "Personal Car",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                        
                        // Seats
                        if (ride.seatsAvailable != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Secondary
                                )
                                Text(
                                    text = if (ride.isOffer) {
                                        "${ride.seatsAvailable} seats available"
                                    } else {
                                        "${ride.seatsAvailable} passenger${if (ride.seatsAvailable > 1) "s" else ""}"
                                    },
                                    fontSize = 14.sp,
                                    color = Secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Second row: Price (for taxis) - on a new line
                    if (ride.vehicleType == VehicleType.TAXI && ride.price != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Primary
                            )
                            Text(
                                text = "${ride.price} TND per person",
                                fontSize = 14.sp,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Only show button if the ride is not owned by the current user
            if (ride.userId != null && ride.userId != currentUserId) {
                Button(
                    onClick = if (ride.isOffer) onApplyToOffer else onReplyToRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ride.isOffer) Accent else Primary
                    )
                ) {
                    Text(
                        text = if (ride.isOffer) "Apply to Offer" else "Reply to Request"
                    )
                }
            } else {
                // Show a close button if it's the user's own ride
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextSecondary
                    )
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Empty state when no rides available
 */
@Composable
fun EmptyRidesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = TextHint
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.no_active_rides),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.be_first_to_ride),
            fontSize = 15.sp,
            color = TextHint
        )
    }
}

