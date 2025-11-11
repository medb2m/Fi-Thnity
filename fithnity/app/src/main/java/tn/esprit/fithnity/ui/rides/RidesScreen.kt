package tn.esprit.fithnity.ui.rides

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavHostController
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R

/**
 * Rides Screen showing list of offers and demands
 */
@Composable
fun RidesScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(RideFilter.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.active_rides),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(Modifier.height(16.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = selectedFilter == RideFilter.ALL,
                onClick = { selectedFilter = RideFilter.ALL },
                label = { Text(stringResource(R.string.all)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            FilterChip(
                selected = selectedFilter == RideFilter.REQUESTS,
                onClick = { selectedFilter = RideFilter.REQUESTS },
                label = { Text(stringResource(R.string.requests)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            FilterChip(
                selected = selectedFilter == RideFilter.OFFERS,
                onClick = { selectedFilter = RideFilter.OFFERS },
                label = { Text(stringResource(R.string.offers)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        // Rides List
        val rides = remember { emptyList<RideItem>() } // TODO: Get from ViewModel

        if (rides.isEmpty()) {
            // Empty State
            EmptyRidesState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rides) { ride ->
                    RideCard(ride = ride) {
                        // TODO: Navigate to ride detail
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no rides available
 */
@Composable
private fun EmptyRidesState() {
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

/**
 * Ride Card Component
 */
@Composable
private fun RideCard(
    ride: RideItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Type Badge + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (ride.isOffer) Accent.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (ride.isOffer) Icons.Default.Share else Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (ride.isOffer) Accent else Primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (ride.isOffer) stringResource(R.string.offer_ride) else stringResource(R.string.need_ride),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (ride.isOffer) Accent else Primary
                        )
                    }
                }

                // Time
                Text(
                    text = ride.time,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Origin
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ride.origin,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Accent
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ride.destination,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Footer: User + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = ride.userName,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Price (if offer)
                if (ride.isOffer && ride.price != null) {
                    Text(
                        text = "${ride.price} TND",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
        }
    }
}

// Data Models
private enum class RideFilter {
    ALL, REQUESTS, OFFERS
}

private data class RideItem(
    val id: String,
    val isOffer: Boolean,
    val origin: String,
    val destination: String,
    val userName: String,
    val time: String,
    val price: String? = null
)
