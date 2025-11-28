package tn.esprit.fithnity.ui.rides

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.data.RideResponse
import tn.esprit.fithnity.ui.navigation.FiThnityDetailTopBar
import tn.esprit.fithnity.ui.theme.*

/**
 * Screen to display user's own rides (offers or requests)
 */
@Composable
fun MyRidesScreen(
    navController: NavHostController,
    userPreferences: UserPreferences,
    rideType: String, // "OFFER" or "REQUEST"
    viewModel: RideViewModel = viewModel()
) {
    val authToken = remember { userPreferences.getAuthToken() }
    val ridesState by viewModel.uiState.collectAsState()
    
    // Load user's rides on first composition
    LaunchedEffect(Unit) {
        viewModel.loadMyRides(authToken, status = null)
    }
    
    // Filter rides by type
    val filteredRides = when (val state = ridesState) {
        is RideUiState.Success -> {
            state.rides
                .filter { it.rideType == rideType }
                .map { ride ->
                    RideItem(
                        id = ride._id,
                        isOffer = ride.rideType == "OFFER",
                        vehicleType = when (ride.transportType) {
                            "PRIVATE_CAR" -> VehicleType.PERSONAL_CAR
                            "TAXI", "TAXI_COLLECTIF" -> VehicleType.TAXI
                            else -> VehicleType.PERSONAL_CAR
                        },
                        origin = ride.origin.address,
                        destination = ride.destination.address,
                        userName = ride.user?.name ?: "You",
                        userId = ride.user?._id,
                        userPhoto = ride.user?.photoUrl,
                        time = ride.departureDate,
                        price = ride.price?.toString(),
                        seatsAvailable = ride.availableSeats
                    )
                }
        }
        else -> emptyList()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        FiThnityDetailTopBar(
            title = if (rideType == "OFFER") "My Offers" else "My Requests",
            navController = navController
        )
        
        // Content
        when (val state = ridesState) {
            is RideUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is RideUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Error loading rides",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { viewModel.loadMyRides(authToken, status = null) }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is RideUiState.Success -> {
                if (filteredRides.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (rideType == "OFFER") 
                                    "No offers yet" 
                                else 
                                    "No requests yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (rideType == "OFFER")
                                    "Create your first ride offer to get started"
                                else
                                    "Create your first ride request to get started",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRides) { ride ->
                            RideCard(ride = ride) {
                                // TODO: Navigate to ride detail
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

