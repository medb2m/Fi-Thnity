package tn.esprit.fithnity.ui.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.esprit.fithnity.ui.theme.*

/**
 * Ride Card Component - Simplified and clean design
 */
@Composable
fun RideCard(
    ride: RideItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Styled Date/Time + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Styled Date and Time
                StyledDateTime(ride.time)
                
                // Compact Badge (Type + Vehicle)
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (ride.isOffer) Accent else Primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Origin and Destination - Simplified
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Origin
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = ride.origin,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Destination
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = ride.destination,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Footer: User + Essential Info (Seats/Price)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = ride.userName,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Essential info: Seats or Price
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (ride.isOffer && ride.seatsAvailable != null) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Secondary
                        )
                        Text(
                            text = "${ride.seatsAvailable}",
                            fontSize = 13.sp,
                            color = Secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (ride.vehicleType == VehicleType.TAXI && ride.price != null) {
                        Text(
                            text = "${ride.price} TND",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Styled Date and Time Display
 */
@Composable
fun StyledDateTime(timeString: String) {
    val (dateText, timeText) = formatDateTime(timeString)
    
    Column {
        // Date
        Text(
            text = dateText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(Modifier.height(2.dp))
        // Time - Styled
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Primary
            )
            Text(
                text = timeText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

