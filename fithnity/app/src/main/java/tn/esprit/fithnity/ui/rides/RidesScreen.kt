package tn.esprit.fithnity.ui.rides

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R
import java.text.SimpleDateFormat
import java.util.*
import tn.esprit.fithnity.utils.rememberLocationState
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.SymbolLayer
import org.json.JSONObject
import org.json.JSONArray
import tn.esprit.fithnity.BuildConfig
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import android.location.Geocoder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.io.IOException
import tn.esprit.fithnity.data.Location
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.fithnity.ui.navigation.SearchState
import tn.esprit.fithnity.data.UserPreferences
import android.app.DatePickerDialog
import kotlinx.coroutines.delay
import tn.esprit.fithnity.ui.chat.ChatViewModel

/**
 * Rides Screen showing list of offers and demands
 * Supports both personal cars and taxis with matching functionality
 */
@Composable
fun RidesScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences,
    viewModel: RideViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    autoOpenRideType: String? = null
) {
    val authToken = userPreferences.getAuthToken()
    val currentUserId = userPreferences.getUserId()
    var selectedFilter by remember { mutableStateOf(RideFilter.ALL) }
    var selectedVehicleType by remember { mutableStateOf(VehicleType.ALL) }
    var showRideTypeSelection by remember { mutableStateOf(false) }
    var selectedRideType by remember { mutableStateOf<RideType?>(null) }
    var selectedRide by remember { mutableStateOf<RideItem?>(null) }
    
    // Search query from global state
    var searchQuery by remember { mutableStateOf(SearchState.searchQuery) }
    
    // Update local state when global state changes
    LaunchedEffect(SearchState.searchQuery) {
        searchQuery = SearchState.searchQuery
    }
    
    // Listen to search state changes (for real-time updates)
    LaunchedEffect(Unit) {
        SearchState.setSearchHandler { query ->
            searchQuery = query
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            SearchState.clearSearchHandler()
        }
    }
    
    // Auto-open dialog if autoOpenRideType is provided
    LaunchedEffect(autoOpenRideType) {
        when (autoOpenRideType?.uppercase()) {
            "OFFER" -> {
                selectedRideType = RideType.OFFER
            }
            "REQUEST" -> {
                selectedRideType = RideType.REQUEST
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header with Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.active_rides),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            // Add Ride Button
            FloatingActionButton(
                onClick = { showRideTypeSelection = true },
                modifier = Modifier.size(48.dp),
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Ride",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Compact Filters - Two Rows Design
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Vehicle Type Filters - First Row
            val vehicleScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(vehicleScrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactFilterChip(
                    selected = selectedVehicleType == VehicleType.ALL,
                    onClick = { selectedVehicleType = VehicleType.ALL },
                    label = "All",
                    icon = Icons.Default.DirectionsCar,
                    color = Primary
                )
                CompactFilterChip(
                    selected = selectedVehicleType == VehicleType.PERSONAL_CAR,
                    onClick = { selectedVehicleType = VehicleType.PERSONAL_CAR },
                    label = "Car",
                    icon = Icons.Default.DirectionsCar,
                    color = Secondary
                )
                CompactFilterChip(
                    selected = selectedVehicleType == VehicleType.TAXI,
                    onClick = { selectedVehicleType = VehicleType.TAXI },
                    label = "Taxi",
                    icon = Icons.Default.LocalTaxi,
                    color = Accent
                )
            }

            // Ride Type Filters - Second Row
            val rideScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rideScrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactFilterChip(
                    selected = selectedFilter == RideFilter.ALL,
                    onClick = { selectedFilter = RideFilter.ALL },
                    label = stringResource(R.string.all),
                    icon = Icons.Default.List,
                    color = Primary
                )
                CompactFilterChip(
                    selected = selectedFilter == RideFilter.REQUESTS,
                    onClick = { selectedFilter = RideFilter.REQUESTS },
                    label = stringResource(R.string.requests),
                    icon = Icons.Default.Search,
                    color = Primary
                )
                CompactFilterChip(
                    selected = selectedFilter == RideFilter.OFFERS,
                    onClick = { selectedFilter = RideFilter.OFFERS },
                    label = "Offers",
                    icon = Icons.Default.Share,
                    color = Accent
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Rides List
        val ridesState by viewModel.uiState.collectAsState()
        
        // Load rides on first composition
        LaunchedEffect(Unit) {
            viewModel.loadRides()
        }
        
        val allRides = when (val state = ridesState) {
            is RideUiState.Success -> {
                // Convert RideResponse to RideItem for display
                state.rides.map { ride ->
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
                        userName = ride.user?.name ?: "Unknown",
                        userId = ride.user?._id,
                        userPhoto = ride.user?.photoUrl,
                        time = ride.departureDate, // ISO format, will be formatted in UI if needed
                        price = ride.price?.toString(),
                        seatsAvailable = ride.availableSeats,
                        matchedWithUserId = ride.matchedWith?._id,
                        matchedWithUserName = ride.matchedWith?.name,
                        matchedWithUserPhoto = ride.matchedWith?.photoUrl,
                        passengers = ride.passengers?.map { passenger ->
                            PassengerInfo(
                                userId = passenger._id ?: "",
                                userName = passenger.name ?: "Unknown",
                                userPhoto = passenger.photoUrl
                            )
                        }
                    )
                }
            }
            is RideUiState.Loading -> emptyList()
            is RideUiState.Error -> {
                // Show error, fallback to sample data for now
                getSampleRides()
            }
            else -> getSampleRides()
        }
        
        // Filter rides based on selected filters and search query
        val filteredRides = allRides.filter { ride ->
            val matchesFilter = when (selectedFilter) {
                RideFilter.ALL -> true
                RideFilter.REQUESTS -> !ride.isOffer
                RideFilter.OFFERS -> ride.isOffer
            }
            val matchesVehicleType = when (selectedVehicleType) {
                VehicleType.ALL -> true
                VehicleType.PERSONAL_CAR -> ride.vehicleType == VehicleType.PERSONAL_CAR
                VehicleType.TAXI -> ride.vehicleType == VehicleType.TAXI
            }
            // Search filter: match origin, destination, or user name
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val queryLower = searchQuery.lowercase()
                ride.origin.lowercase().contains(queryLower) ||
                ride.destination.lowercase().contains(queryLower) ||
                ride.userName.lowercase().contains(queryLower)
            }
            matchesFilter && matchesVehicleType && matchesSearch
        }

        if (filteredRides.isEmpty()) {
            // Empty State
            EmptyRidesState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = UiConstants.ContentBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRides) { ride ->
                    RideCard(ride = ride) {
                        selectedRide = ride
                    }
                }
            }
        }
    }
    
    // Add Ride Form Dialog
    val createRideState by viewModel.createRideState.collectAsState()
    
    // Handle create ride success/error
    LaunchedEffect(createRideState) {
        when (val state = createRideState) {
            is CreateRideUiState.Success -> {
                selectedRideType = null
                viewModel.resetCreateRideState()
                // Refresh rides list
                viewModel.loadRides()
            }
            is CreateRideUiState.Error -> {
                // Error is shown in dialog
            }
            else -> {}
        }
    }
    
    // Ride Type Selection Dialog
    if (showRideTypeSelection) {
        RideTypeSelectionDialog(
            onDismiss = { showRideTypeSelection = false },
            onRideTypeSelected = { rideType ->
                selectedRideType = rideType
                showRideTypeSelection = false
            }
        )
    }
    
    // Add Ride Form Dialog (shown after type selection)
    selectedRideType?.let { rideType ->
        AddRideFormDialog(
            onDismiss = { 
                selectedRideType = null
                viewModel.resetCreateRideState()
            },
            rideType = rideType,
            viewModel = viewModel,
            authToken = authToken
        )
    }
    
    // Ride Details Dialog
    selectedRide?.let { ride ->
        RideDetailsDialog(
            ride = ride,
            onDismiss = { selectedRide = null },
            currentUserId = currentUserId,
            onApplyToOffer = {
                // Navigate to chat with the user who created the offer
                val otherUserId = ride.userId
                if (otherUserId != null && otherUserId != currentUserId) {
                    chatViewModel.getOrCreateConversation(authToken, otherUserId) { conversation ->
                        val userName = conversation.otherUser.name ?: ride.userName
                        val userPhoto = conversation.otherUser.photoUrl
                        
                        // Extract relative path from full URL if needed
                        val photoPath = if (userPhoto != null && userPhoto.isNotEmpty()) {
                            if (userPhoto.startsWith("http://72.61.145.239:9090")) {
                                userPhoto.substring("http://72.61.145.239:9090".length)
                            } else if (userPhoto.startsWith("http")) {
                                "none" // External URL, use placeholder
                            } else {
                                userPhoto // Already a relative path
                            }
                        } else {
                            "none" // Placeholder for null/empty
                        }
                        
                        val encodedName = java.net.URLEncoder.encode(userName, "UTF-8")
                        val encodedPhoto = java.net.URLEncoder.encode(photoPath, "UTF-8")
                        selectedRide = null
                        navController.navigate("chat_detail/${conversation._id}/${conversation.otherUser._id}/$encodedName/$encodedPhoto")
                    }
                } else {
                    selectedRide = null
                }
            },
            onReplyToRequest = {
                // Navigate to chat with the user who created the request
                val otherUserId = ride.userId
                if (otherUserId != null && otherUserId != currentUserId) {
                    chatViewModel.getOrCreateConversation(authToken, otherUserId) { conversation ->
                        val userName = conversation.otherUser.name ?: ride.userName
                        val userPhoto = conversation.otherUser.photoUrl
                        
                        // Extract relative path from full URL if needed
                        val photoPath = if (userPhoto != null && userPhoto.isNotEmpty()) {
                            if (userPhoto.startsWith("http://72.61.145.239:9090")) {
                                userPhoto.substring("http://72.61.145.239:9090".length)
                            } else if (userPhoto.startsWith("http")) {
                                "none" // External URL, use placeholder
                            } else {
                                userPhoto // Already a relative path
                            }
                        } else {
                            "none" // Placeholder for null/empty
                        }
                        
                        val encodedName = java.net.URLEncoder.encode(userName, "UTF-8")
                        val encodedPhoto = java.net.URLEncoder.encode(photoPath, "UTF-8")
                        selectedRide = null
                        navController.navigate("chat_detail/${conversation._id}/${conversation.otherUser._id}/$encodedName/$encodedPhoto")
                    }
                } else {
                    selectedRide = null
                }
            }
        )
    }
}


/**
 * Add Ride Form Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRideFormDialog(
    onDismiss: () -> Unit,
    rideType: RideType,
    viewModel: RideViewModel,
    authToken: String?
) {
    val createRideState by viewModel.createRideState.collectAsState()
    val context = LocalContext.current
    val locationState = rememberLocationState()
    
    // Auto-request location when dialog opens
    LaunchedEffect(Unit) {
        // Always try to request location (will request permission if needed)
        locationState.requestLocation()
    }
    
    // Re-request location when permission is granted
    LaunchedEffect(locationState.hasPermission) {
        if (locationState.hasPermission && locationState.location == null && !locationState.isLoading) {
            locationState.requestLocation()
        }
    }
    
    var selectedVehicleType by remember { mutableStateOf(VehicleType.PERSONAL_CAR) }
    
    // Origin selection
    var useCurrentLocation by remember { mutableStateOf(true) } // Checkbox state - checked by default
    val originLocation = locationState.location
    var origin by remember { mutableStateOf("") }
    var originLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showOriginMap by remember { mutableStateOf(false) }
    
    // Update origin when location is available (only if useCurrentLocation is true)
    LaunchedEffect(originLocation, useCurrentLocation) {
        if (useCurrentLocation && originLocation != null) {
            originLatLng = LatLng(originLocation.latitude, originLocation.longitude)
            // Try to get address from coordinates
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(originLocation.latitude, originLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    origin = addresses[0].getAddressLine(0) ?: "${originLocation.latitude}, ${originLocation.longitude}"
                } else {
                    origin = "${String.format("%.4f", originLocation.latitude)}, ${String.format("%.4f", originLocation.longitude)}"
                }
            } catch (e: IOException) {
                origin = "${String.format("%.4f", originLocation.latitude)}, ${String.format("%.4f", originLocation.longitude)}"
            }
        } else if (useCurrentLocation) {
            if (locationState.isLoading) {
                origin = "Getting your location..."
            } else {
                origin = "Location not available"
            }
        }
    }
    
    // Destination selection
    var showDestinationMap by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf("") }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var datePickerDialogShown by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateDisplay = remember(selectedDate) {
        selectedDate?.let { dateFormatter.format(Date(it)) } ?: ""
    }
    
    var time by remember { mutableStateOf("") }
    
    // Function to format time input automatically
    fun formatTimeInput(input: String): String {
        // If input already contains ":" and AM/PM, preserve them
        val hasColon = input.contains(":")
        val hasAmPm = input.uppercase().contains("AM") || input.uppercase().contains("PM")
        
        // Extract digits only
        val digitsOnly = input.filter { it.isDigit() }
        
        if (digitsOnly.isEmpty()) {
            // If user is deleting, allow partial deletion but preserve AM/PM if present
            if (hasAmPm) {
                val amPmPart = if (input.uppercase().contains("AM")) " AM" else if (input.uppercase().contains("PM")) " PM" else ""
                return input.filter { it.isDigit() || it == ':' } + amPmPart
            }
            return ""
        }
        
        // Limit to 4 digits for time (HHMM)
        val limitedDigits = digitsOnly.take(4)
        
        // Format the time part
        val timePart = when (limitedDigits.length) {
            1 -> limitedDigits // Just one digit: "8"
            2 -> limitedDigits // Two digits: "83"
            3 -> "${limitedDigits[0]}:${limitedDigits.substring(1)}" // "8:30"
            4 -> "${limitedDigits.substring(0, 2)}:${limitedDigits.substring(2)}" // "08:30"
            else -> ""
        }
        
        // Preserve AM/PM if user typed it
        val amPmPart = when {
            input.uppercase().contains("AM") -> " AM"
            input.uppercase().contains("PM") -> " PM"
            else -> ""
        }
        
        return timePart + amPmPart
    }
    var seatsAvailable by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    
    // Validation errors
    var originError by remember { mutableStateOf<String?>(null) }
    var destinationError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var seatsError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    
    // Validation functions
    fun validateOrigin(value: String): String? {
        return when {
            value.isBlank() -> "Origin is required"
            value.length < 3 -> "Origin must be at least 3 characters"
            value.length > 100 -> "Origin must be less than 100 characters"
            else -> null
        }
    }
    
    fun validateDestination(value: String): String? {
        return when {
            value.isBlank() -> "Destination is required"
            value.length < 3 -> "Destination must be at least 3 characters"
            value.length > 100 -> "Destination must be less than 100 characters"
            value.equals(origin, ignoreCase = true) -> "Destination must be different from origin"
            else -> null
        }
    }
    
    fun validateDate(dateValue: Long?): String? {
        return when {
            dateValue == null -> "Date is required"
            dateValue < System.currentTimeMillis() - 86400000 -> "Date cannot be in the past" // Allow today
            else -> null
        }
    }
    
    fun validateTime(value: String): String? {
        val timePattern = Regex("^(0?[1-9]|1[0-2]):[0-5][0-9]\\s?(AM|PM|am|pm)$|^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        return when {
            value.isBlank() -> "Time is required"
            !timePattern.matches(value) -> "Invalid time format (e.g., 8:30 AM or 14:30)"
            else -> null
        }
    }
    
    fun validateSeats(value: String): String? {
        val seats = value.toIntOrNull()
        return when {
            value.isBlank() -> "Number of seats is required"
            seats == null -> "Please enter a valid number"
            seats < 1 -> "At least 1 seat is required"
            seats > 8 -> "Maximum 8 seats allowed"
            else -> null
        }
    }
    
    fun validatePrice(value: String, isTaxi: Boolean): String? {
        if (!isTaxi) return null
        val priceValue = value.toDoubleOrNull()
        return when {
            value.isBlank() -> "Price is required for taxi rides"
            priceValue == null -> "Please enter a valid price"
            priceValue < 0 -> "Price cannot be negative"
            priceValue > 1000 -> "Price cannot exceed 1000 TND"
            else -> null
        }
    }
    
    val isOffer = rideType == RideType.OFFER
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isOffer) "Add New Ride Offer" else "Add New Ride Request",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vehicle Type Selection
                Text(
                    text = "Vehicle Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedVehicleType == VehicleType.PERSONAL_CAR,
                        onClick = { selectedVehicleType = VehicleType.PERSONAL_CAR },
                        label = { Text("Personal Car") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedVehicleType == VehicleType.TAXI,
                        onClick = { selectedVehicleType = VehicleType.TAXI },
                        label = { Text("Taxi") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocalTaxi,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                
                // Origin - Current Location or Map Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Checkbox for using current location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = useCurrentLocation,
                            onCheckedChange = { 
                                useCurrentLocation = it
                                if (!it) {
                                    // Clear origin when unchecking
                                    origin = ""
                                    originLatLng = null
                                }
                            }
                        )
                        Text(
                            text = "Use current location",
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { useCurrentLocation = !useCurrentLocation }
                        )
                    }
                    
                    if (useCurrentLocation) {
                        // Show current location field
                        OutlinedTextField(
                            value = origin,
                            onValueChange = { }, // Read-only
                            label = { Text("Origin * (Your Current Location)") },
                            placeholder = { Text("Getting your location...") },
                            leadingIcon = {
                                if (locationState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Primary
                                    )
                                } else {
                                    Icon(Icons.Default.MyLocation, null, tint = Primary)
                                }
                            },
                            trailingIcon = {
                                if (originLocation != null) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        "Location found",
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = true,
                            singleLine = true,
                            isError = originError != null || (originLocation == null && !locationState.isLoading && !locationState.hasPermission),
                            supportingText = {
                                when {
                                    originError != null -> Text(originError!!, color = Error)
                                    locationState.isLoading -> Text("Getting your location...", color = TextSecondary)
                                    !locationState.hasPermission -> Text("Location permission required", color = Error)
                                    originLocation == null && locationState.hasPermission -> Text("Unable to get location. Tap button to retry.", color = Error)
                                    originLocation == null -> Text("Please enable location permission", color = Error)
                                    else -> Text("Using your current location", color = Primary)
                                }
                            }
                        )
                        
                        // Button to request location/permission if not available
                        if (originLocation == null) {
                            Button(
                                onClick = { locationState.requestLocation() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (locationState.hasPermission) Secondary else Primary
                                ),
                                enabled = !locationState.isLoading
                            ) {
                                if (locationState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(
                                    imageVector = if (locationState.hasPermission) Icons.Default.Refresh else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (locationState.hasPermission) "Retry Location" else "Enable Location",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // Show map selection field (similar to destination)
                        OutlinedTextField(
                            value = origin,
                            onValueChange = { }, // Read-only, opens map
                            label = { Text("Origin *") },
                            placeholder = { Text("Tap to select on map") },
                            leadingIcon = {
                                Icon(Icons.Default.Place, null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showOriginMap = true }) {
                                    Icon(Icons.Default.Map, "Select on Map", modifier = Modifier.size(20.dp))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOriginMap = true },
                            readOnly = true,
                            enabled = true,
                            singleLine = true,
                            isError = originError != null,
                            supportingText = originError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                        )
                    }
                }
                
                // Destination - Map Selection
                OutlinedTextField(
                    value = destination,
                    onValueChange = { }, // Read-only, opens map
                    label = { Text("Destination *") },
                    placeholder = { Text("Tap to select on map") },
                    leadingIcon = {
                        Icon(Icons.Default.Place, null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showDestinationMap = true }) {
                            Icon(Icons.Default.Map, "Select on Map", modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDestinationMap = true },
                    readOnly = true,
                    enabled = true,
                    singleLine = true,
                    isError = destinationError != null,
                    supportingText = destinationError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                )
                
                // Date and Time Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Picker Button
                    OutlinedTextField(
                        value = dateDisplay,
                        onValueChange = { }, // Read-only, opens picker
                        label = { Text("Date *") },
                        placeholder = { Text("Select date") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, "Select Date", modifier = Modifier.size(20.dp))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        readOnly = true,
                        enabled = true,
                        singleLine = true,
                        isError = dateError != null,
                        supportingText = dateError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                    )
                    
                    // Time Picker with automatic formatting
                    OutlinedTextField(
                        value = time,
                        onValueChange = { newValue ->
                            // Format the input automatically
                            val formatted = formatTimeInput(newValue)
                            time = formatted
                            timeError = validateTime(formatted)
                        },
                        label = { Text("Time *") },
                        placeholder = { Text("8:30 AM or 14:30") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text // Allow text for AM/PM
                        ),
                        isError = timeError != null,
                        supportingText = {
                            when {
                                timeError != null -> Text(timeError!!, color = Error, fontSize = 11.sp)
                                time.isNotEmpty() && time.length >= 4 -> Text("Use 12h format (e.g., 8:30 AM) or 24h format (e.g., 14:30)", color = TextSecondary, fontSize = 11.sp)
                                else -> Text("Type time (e.g., 830 becomes 8:30)", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    )
                }
                
                // Date Picker Dialog - Using native Android DatePickerDialog for better mobile compatibility
                // This provides better screen size adaptation than Material3 DatePicker
                LaunchedEffect(showDatePicker) {
                    if (showDatePicker && !datePickerDialogShown) {
                        datePickerDialogShown = true
                        val calendar = Calendar.getInstance()
                        selectedDate?.let {
                            calendar.timeInMillis = it
                        }
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        
                        val datePickerDialog = DatePickerDialog(
                            context,
                            R.style.DatePickerDialogTheme, // Use custom theme with primary blue color
                            { _, selectedYear, selectedMonth, selectedDay ->
                                val newCalendar = Calendar.getInstance()
                                newCalendar.set(selectedYear, selectedMonth, selectedDay)
                                selectedDate = newCalendar.timeInMillis
                                dateError = validateDate(newCalendar.timeInMillis)
                                showDatePicker = false
                                datePickerDialogShown = false
                            },
                            year,
                            month,
                            day
                        )
                        
                        // Set minimum date to today
                        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 86400000 // Allow today
                        
                        // Handle dialog cancellation
                        datePickerDialog.setOnCancelListener {
                            showDatePicker = false
                            datePickerDialogShown = false
                        }
                        
                        // Handle dialog dismissal (back button)
                        datePickerDialog.setOnDismissListener {
                            showDatePicker = false
                            datePickerDialogShown = false
                        }
                        
                        datePickerDialog.show()
                    } else if (!showDatePicker) {
                        datePickerDialogShown = false
                    }
                }
                
                // Seats Available (for offers) or Number of Passengers (for requests)
                OutlinedTextField(
                    value = seatsAvailable,
                    onValueChange = { 
                        seatsAvailable = it.filter { char -> char.isDigit() }
                        seatsError = validateSeats(seatsAvailable)
                    },
                    label = { Text(if (isOffer) "Available Seats *" else "Number of Passengers *") },
                    placeholder = { Text("e.g., 3") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    isError = seatsError != null,
                    supportingText = seatsError?.let { { Text(it, color = Error) } }
                )
                
                // Price (only for taxis)
                if (selectedVehicleType == VehicleType.TAXI) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { 
                            price = it.filter { char -> char.isDigit() || char == '.' }
                            priceError = validatePrice(price, true)
                        },
                        label = { Text("Price per person (TND) *") },
                        placeholder = { Text("e.g., 5") },
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        isError = priceError != null,
                        supportingText = priceError?.let { { Text(it, color = Error) } }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate all fields
                    originError = validateOrigin(origin)
                    destinationError = validateDestination(destination)
                    dateError = validateDate(selectedDate)
                    timeError = validateTime(time)
                    seatsError = validateSeats(seatsAvailable)
                    priceError = validatePrice(price, selectedVehicleType == VehicleType.TAXI)
                    
                    // Only submit if all validations pass
                    if (originError == null && destinationError == null && dateError == null && 
                        timeError == null && seatsError == null && priceError == null &&
                        originLocation != null && destinationLatLng != null) {
                        
                        // Convert time to 24-hour format if needed
                        val time24Hour = convertTo24Hour(time)
                        
                        // Combine date and time into ISO 8601 format
                        val departureDateTime = selectedDate?.let { dateMillis ->
                            val calendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                            }
                            // Parse time string (e.g., "8:30 AM" or "14:30")
                            val (hour, minute) = parseTime(time24Hour)
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                            calendar.set(java.util.Calendar.MINUTE, minute)
                            calendar.set(java.util.Calendar.SECOND, 0)
                            
                            // Format as ISO 8601 (backend will parse as local time)
                            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(calendar.time)
                        }
                        
                        // Map vehicle type to backend transport type
                        val transportType = when (selectedVehicleType) {
                            VehicleType.PERSONAL_CAR -> "PRIVATE_CAR"
                            VehicleType.TAXI -> "TAXI"
                            else -> "PRIVATE_CAR"
                        }
                        
                        // Create API request
                        viewModel.createRide(
                            authToken = authToken,
                            rideType = if (isOffer) "OFFER" else "REQUEST",
                            transportType = transportType,
                            origin = Location(
                                latitude = originLocation!!.latitude,
                                longitude = originLocation!!.longitude,
                                address = origin.trim()
                            ),
                            destination = Location(
                                latitude = destinationLatLng!!.latitude,
                                longitude = destinationLatLng!!.longitude,
                                address = destination.trim()
                            ),
                            availableSeats = seatsAvailable.toIntOrNull(),
                            notes = null,
                            departureDate = departureDateTime,
                            price = if (selectedVehicleType == VehicleType.TAXI && price.isNotBlank()) {
                                price.toDoubleOrNull()
                            } else null
                        )
                    }
                },
                enabled = createRideState !is CreateRideUiState.Loading &&
                         originLocation != null && destination.isNotBlank() && selectedDate != null && 
                         time.isNotBlank() && seatsAvailable.isNotBlank() && 
                         (selectedVehicleType != VehicleType.TAXI || price.isNotBlank())
            ) {
                if (createRideState is CreateRideUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (createRideState is CreateRideUiState.Loading) {
                    if (isOffer) "Publishing..." else "Submitting..."
                } else {
                    if (isOffer) "Publish" else "Submit Request"
                })
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = createRideState !is CreateRideUiState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
    
    // Show error message if create failed
    if (createRideState is CreateRideUiState.Error) {
        LaunchedEffect(createRideState) {
            android.widget.Toast.makeText(
                context,
                "Failed to create ride: ${(createRideState as CreateRideUiState.Error).message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Origin Map Selection Dialog
    if (showOriginMap) {
        val defaultLocation = LatLng(36.8065, 10.1815) // Default to Tunis
        DestinationMapDialog(
            onDismiss = { showOriginMap = false },
            onLocationSelected = { latLng, address ->
                originLatLng = latLng
                origin = address
                originError = validateOrigin(address)
                showOriginMap = false
            },
            initialLocation = originLatLng ?: originLocation?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation,
            dialogTitle = "Select Origin"
        )
    }
    
    // Destination Map Selection Dialog
    if (showDestinationMap) {
        val defaultLocation = LatLng(36.8065, 10.1815) // Default to Tunis
        DestinationMapDialog(
            onDismiss = { showDestinationMap = false },
            onLocationSelected = { latLng, address ->
                destinationLatLng = latLng
                destination = address
                destinationError = validateDestination(address)
                showDestinationMap = false
            },
            initialLocation = originLatLng ?: defaultLocation
        )
    }
}

/**
 * Dialog for selecting destination on map
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationMapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (LatLng, String) -> Unit,
    initialLocation: LatLng,
    dialogTitle: String = "Select Destination"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var shouldCreateMapView by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Search functionality
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    
    // Ensure MapLibre is initialized before creating MapView
    LaunchedEffect(Unit) {
        val app = context.applicationContext as? tn.esprit.fithnity.FiThnityApplication
        app?.ensureMapLibreInitialized()
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
                    android.util.Log.e("RidesScreen", "Error in MapView lifecycle: ${event.name}", e)
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
                    android.util.Log.e("RidesScreen", "Error destroying MapView", e)
                }
            }
            mapView = null
            mapLibreMap = null
        }
    }
    
    // Search for address - Limited to Tunisia only
    fun searchAddress(query: String) {
        if (query.isBlank() || query.length < 3) {
            searchResults = emptyList()
            return
        }
        
        isSearching = true
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            // Tunisia geographic bounds
            // North: ~37.5°N, South: ~30.2°N, East: ~11.6°E, West: ~7.5°E
            val tunisiaNorthEast = android.location.Location("").apply {
                latitude = 37.5
                longitude = 11.6
            }
            val tunisiaSouthWest = android.location.Location("").apply {
                latitude = 30.2
                longitude = 7.5
            }
            
            // Get more results to filter (get 20, filter to 5)
            val allAddresses = geocoder.getFromLocationName(
                query, 
                20, // Get more results for better filtering
                tunisiaSouthWest.latitude,
                tunisiaSouthWest.longitude,
                tunisiaNorthEast.latitude,
                tunisiaNorthEast.longitude
            ) ?: emptyList()
            
            // Filter to only include addresses in Tunisia
            val tunisiaAddresses = allAddresses.filter { address ->
                address.countryName?.equals("Tunisia", ignoreCase = true) == true ||
                address.countryCode?.equals("TN", ignoreCase = true) == true ||
                address.countryName?.equals("Tunisie", ignoreCase = true) == true ||
                // Also check if coordinates are within Tunisia bounds
                (address.latitude >= 30.2 && address.latitude <= 37.5 &&
                 address.longitude >= 7.5 && address.longitude <= 11.6)
            }
            
            // Limit to 5 best results
            searchResults = tunisiaAddresses.take(5)
        } catch (e: Exception) {
            // Fallback: try without bounds and filter by country
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val allAddresses = geocoder.getFromLocationName(query, 20) ?: emptyList()
                val tunisiaAddresses = allAddresses.filter { address ->
                    address.countryName?.equals("Tunisia", ignoreCase = true) == true ||
                    address.countryCode?.equals("TN", ignoreCase = true) == true ||
                    address.countryName?.equals("Tunisie", ignoreCase = true) == true
                }
                searchResults = tunisiaAddresses.take(5)
            } catch (e2: Exception) {
                searchResults = emptyList()
            }
        } finally {
            isSearching = false
        }
    }
    
    // Handle search with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            delay(500) // Wait 500ms after last keystroke
            if (searchQuery.length >= 3) {
                searchAddress(searchQuery)
            }
        } else {
            searchResults = emptyList()
        }
    }
    
    // Function to display search result on map (without selecting)
    fun selectSearchResult(address: android.location.Address) {
        val latLng = org.maplibre.android.geometry.LatLng(address.latitude, address.longitude)
        val addressText = address.getAddressLine(0) ?: "${address.latitude}, ${address.longitude}"
        
        // Update selected location and address
        selectedLocation = latLng
        selectedAddress = addressText
        searchQuery = addressText
        searchResults = emptyList()
        
        // Move map to selected location and add marker
        mapLibreMap?.let { map ->
            map.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                    latLng,
                    15.0
                )
            )
            map.getStyle { style ->
                addDestinationMarker(style, latLng)
            }
        }
    }
    
    // Get address from coordinates
    fun getAddressFromLocation(latLng: org.maplibre.android.geometry.LatLng) {
        isLoadingAddress = true
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                selectedAddress = addresses[0].getAddressLine(0) ?: 
                    "${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)}"
            } else {
                selectedAddress = "${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)}"
            }
        } catch (e: IOException) {
            selectedAddress = "${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)}"
        } finally {
            isLoadingAddress = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search address") },
                    placeholder = { Text("Type address to search...") },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                        } else {
                            Icon(Icons.Default.Search, null)
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                searchResults = emptyList()
                            }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Search Results - Dynamic height based on number of results
                if (searchResults.isNotEmpty()) {
                    // Calculate dynamic height: min 60dp per item, max 200dp total
                    val itemHeight = 60.dp // Approximate height per item (with padding)
                    val calculatedHeight = (searchResults.size * itemHeight.value).dp.coerceAtMost(200.dp)
                    val finalHeight = calculatedHeight.coerceAtLeast(60.dp) // Minimum height for 1 item
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(finalHeight),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Surface
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(searchResults) { address ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectSearchResult(address) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Place,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = address.getAddressLine(0) ?: "Unknown address",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (address.getAddressLine(1) != null) {
                                            Text(
                                                text = address.getAddressLine(1) ?: "",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
                
                // Map View - created after MapLibre initialization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
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
                                        android.util.Log.e("RidesScreen", "Error starting MapView", e)
                                    }
                                    getMapAsync { map ->
                                        try {
                                            mapLibreMap = map
                                            setupDestinationMapStyle(map, org.maplibre.android.geometry.LatLng(initialLocation.latitude, initialLocation.longitude)) { success ->
                                                if (success && mapLibreMap != null && mapView != null) {
                                                    try {
                                                        // Add marker at center
                                                        map.getStyle { style ->
                                                            try {
                                                                addDestinationMarker(style, org.maplibre.android.geometry.LatLng(initialLocation.latitude, initialLocation.longitude))
                                                            } catch (e: Exception) {
                                                                android.util.Log.e("RidesScreen", "Error adding destination marker", e)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("RidesScreen", "Error getting map style", e)
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("RidesScreen", "Error in getMapAsync callback", e)
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
                                    android.util.Log.e("RidesScreen", "Error updating MapView lifecycle", e)
                                }
                            }
                        )
                    } else {
                        // Show loading indicator while MapLibre initializes
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    // Center crosshair indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Selected location",
                            modifier = Modifier
                                .size(40.dp)
                                .offset(y = (-20).dp),
                            tint = Primary
                        )
                    }
                    
                    // Loading indicator
                    if (isLoadingAddress) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp, 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Getting address...", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                
                // Selected location info
                if (selectedAddress != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedAddress ?: "",
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Tap and drag the map to select destination",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedLocation?.let { location ->
                        val address = selectedAddress ?: 
                            "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                        onLocationSelected(location, address)
                    }
                },
                enabled = selectedLocation != null && selectedAddress != null
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Listen to map camera movements to update selected location
    LaunchedEffect(mapLibreMap) {
        mapLibreMap?.let { map ->
            // Update location when camera stops moving
            map.addOnCameraIdleListener {
                val center = map.cameraPosition.target
                if (center != null) {
                    selectedLocation = center
                    getAddressFromLocation(center)
                    
                    // Update marker position
                    map.getStyle { style ->
                        addDestinationMarker(style, center)
                    }
                }
            }
        }
    }
}

/**
 * Setup map style for destination selection
 */
private fun setupDestinationMapStyle(map: MapLibreMap, initialLocation: org.maplibre.android.geometry.LatLng, onResult: (Boolean) -> Unit) {
    val apiKey = BuildConfig.MAPTILER_API_KEY
    val mapId = "streets-v2"
    val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$apiKey"
    
    try {
        map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
            // Set initial camera position
            val mapLibreLatLng = org.maplibre.android.geometry.LatLng(initialLocation.latitude, initialLocation.longitude)
            map.cameraPosition = CameraPosition.Builder()
                .target(mapLibreLatLng)
                .zoom(14.0)
                .build()
            onResult(true)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false)
    }
}

/**
 * Add destination marker on map
 */
private fun addDestinationMarker(mapStyle: Style, location: org.maplibre.android.geometry.LatLng) {
    try {
        // Remove existing marker
        mapStyle.getLayer("destination-marker-layer")?.let {
            mapStyle.removeLayer(it)
        }
        mapStyle.getSourceAs<GeoJsonSource>("destination-marker-source")?.let {
            mapStyle.removeSource(it)
        }
        
        // Create GeoJSON point
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
        
        // Add source
        val source = GeoJsonSource("destination-marker-source", featureJson.toString())
        mapStyle.addSource(source)
        
        // Add symbol layer
        val symbolLayer = SymbolLayer("destination-marker-layer", "destination-marker-source")
            .withProperties(
                org.maplibre.android.style.layers.PropertyFactory.iconImage("destination-icon"),
                org.maplibre.android.style.layers.PropertyFactory.iconSize(1.5f),
                org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true),
                org.maplibre.android.style.layers.PropertyFactory.iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM)
            )
        
        // Create custom icon (red pin)
        val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FF3D3D")
            this.style = android.graphics.Paint.Style.FILL
        }
        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            this.style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(24f, 24f, 18f, paint)
        canvas.drawCircle(24f, 24f, 18f, strokePaint)
        
        mapStyle.addImage("destination-icon", bitmap)
        mapStyle.addLayer(symbolLayer)
        
    } catch (e: Exception) {
        android.util.Log.e("DestinationMapDialog", "Error adding destination marker", e)
    }
}

/**
 * Compact Filter Chip - Optimized for space efficiency
 */
@Composable
private fun CompactFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color else Surface.copy(alpha = 0.7f),
        border = if (!selected) {
            BorderStroke(1.dp, TextHint.copy(alpha = 0.3f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) Color.White else TextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else TextSecondary,
                maxLines = 1
            )
        }
    }
}
