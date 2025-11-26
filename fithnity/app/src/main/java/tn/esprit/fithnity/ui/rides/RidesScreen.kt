package tn.esprit.fithnity.ui.rides

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import android.location.Geocoder
import java.io.IOException
import tn.esprit.fithnity.data.Location
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Rides Screen showing list of offers and demands
 * Supports both personal cars and taxis with matching functionality
 */
@Composable
fun RidesScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: RideViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedFilter by remember { mutableStateOf(RideFilter.ALL) }
    var selectedVehicleType by remember { mutableStateOf(VehicleType.ALL) }
    var showRideTypeSelection by remember { mutableStateOf(false) }
    var selectedRideType by remember { mutableStateOf<RideType?>(null) }

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

        Spacer(Modifier.height(16.dp))

        // Vehicle Type Filter (Personal Car vs Taxi)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = selectedVehicleType == VehicleType.ALL,
                onClick = { selectedVehicleType = VehicleType.ALL },
                label = { Text("All Vehicles") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Secondary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Ride Type Filter (All, Requests, Offers)
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
                        time = ride.departureDate, // ISO format, will be formatted in UI if needed
                        price = ride.price?.toString(),
                        seatsAvailable = ride.availableSeats
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
        
        // Filter rides based on selected filters
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
            matchesFilter && matchesVehicleType
        }

        if (filteredRides.isEmpty()) {
            // Empty State
            EmptyRidesState()
        } else {
            LazyColumn(
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
            viewModel = viewModel
        )
    }
}

/**
 * Ride Type Selection Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideTypeSelectionDialog(
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
 * Add Ride Form Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRideFormDialog(
    onDismiss: () -> Unit,
    rideType: RideType,
    viewModel: RideViewModel
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
    
    // Origin from current location
    val originLocation = locationState.location
    var origin by remember { mutableStateOf("") }
    var originLatLng by remember { mutableStateOf<LatLng?>(null) }
    
    // Update origin when location is available
    LaunchedEffect(originLocation) {
        if (originLocation != null) {
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
        } else if (locationState.isLoading) {
            origin = "Getting your location..."
        } else {
            origin = "Location not available"
        }
    }
    
    // Destination selection
    var showDestinationMap by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf("") }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateDisplay = remember(selectedDate) {
        selectedDate?.let { dateFormatter.format(Date(it)) } ?: ""
    }
    
    var time by remember { mutableStateOf("") }
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
                
                // Origin - Current Location (Read-only)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    supportingText = destinationError?.let { { Text(it, color = Error) } }
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
                    
                    // Time Picker
                    OutlinedTextField(
                        value = time,
                        onValueChange = { 
                            time = it
                            timeError = validateTime(it)
                        },
                        label = { Text("Time *") },
                        placeholder = { Text("8:30 AM") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = timeError != null,
                        supportingText = timeError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                    )
                }
                
                // Date Picker Dialog
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis()
                    )
                    
                    AlertDialog(
                        onDismissRequest = { showDatePicker = false },
                        title = { Text("Select Date") },
                        text = {
                            DatePicker(state = datePickerState)
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let {
                                        selectedDate = it
                                        dateError = validateDate(it)
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    )
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
private fun DestinationMapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (LatLng, String) -> Unit,
    initialLocation: LatLng
) {
    val context = LocalContext.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var shouldCreateMapView by remember { mutableStateOf(false) }
    
    // Ensure MapLibre is initialized before creating MapView
    LaunchedEffect(Unit) {
        val app = context.applicationContext as? tn.esprit.fithnity.FiThnityApplication
        app?.ensureMapLibreInitialized()
        shouldCreateMapView = true
    }
    
    // Get address from coordinates
    fun getAddressFromLocation(latLng: LatLng) {
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
                text = "Select Destination",
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
                                    getMapAsync { map ->
                                        mapLibreMap = map
                                        setupDestinationMapStyle(map, initialLocation) { success ->
                                            if (success) {
                                                // Add marker at center
                                                map.getStyle { style ->
                                                    addDestinationMarker(style, initialLocation)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
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
private fun setupDestinationMapStyle(map: MapLibreMap, initialLocation: LatLng, onResult: (Boolean) -> Unit) {
    val apiKey = BuildConfig.MAPTILER_API_KEY
    val mapId = "streets-v2"
    val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$apiKey"
    
    try {
        map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
            // Set initial camera position
            map.cameraPosition = CameraPosition.Builder()
                .target(initialLocation)
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
private fun addDestinationMarker(mapStyle: Style, location: LatLng) {
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
 * Ride Card Component with enhanced features
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
            // Header: Type Badges + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ride Type Badge (Offer/Request)
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
                    
                    // Vehicle Type Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (ride.vehicleType == VehicleType.TAXI) Accent.copy(alpha = 0.15f) else Secondary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (ride.vehicleType == VehicleType.TAXI) Icons.Default.LocalTaxi else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (ride.vehicleType == VehicleType.TAXI) Accent else Secondary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (ride.vehicleType == VehicleType.TAXI) "Taxi" else "Car",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (ride.vehicleType == VehicleType.TAXI) Accent else Secondary
                            )
                        }
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

            // Matching Info (if available) - Only show savings for taxis, not personal cars
            if (ride.matchingInfo != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Success.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Success
                        )
                        Text(
                            text = "${ride.matchingInfo!!.matchedRiders} people going same direction",
                            fontSize = 12.sp,
                            color = Success,
                            fontWeight = FontWeight.Medium
                        )
                        // Only show savings for taxis, not for personal cars
                        if (ride.vehicleType == VehicleType.TAXI && ride.matchingInfo!!.savings != null) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "Save ${ride.matchingInfo!!.savings} TND",
                                fontSize = 12.sp,
                                color = Success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Footer: User + Seats + Price
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

                // Seats Available (if offer)
                if (ride.isOffer && ride.seatsAvailable != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Secondary
                        )
                        Text(
                            text = "${ride.seatsAvailable} seats",
                            fontSize = 13.sp,
                            color = Secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Price (only for taxis, not for personal cars)
                if (ride.vehicleType == VehicleType.TAXI && ride.price != null) {
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

// Helper functions for time conversion
private fun convertTo24Hour(timeStr: String): String {
    // If already in 24-hour format (e.g., "14:30"), return as is
    if (timeStr.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
        return timeStr
    }
    
    // Parse 12-hour format (e.g., "8:30 AM" or "2:45 PM")
    val pattern = Regex("^(\\d{1,2}):([0-5][0-9])\\s*(AM|PM|am|pm)$")
    val match = pattern.find(timeStr) ?: return "00:00"
    
    var hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2]
    val amPm = match.groupValues[3].uppercase()
    
    if (amPm == "PM" && hour != 12) {
        hour += 12
    } else if (amPm == "AM" && hour == 12) {
        hour = 0
    }
    
    return String.format("%02d:%s", hour, minute)
}

private fun parseTime(timeStr: String): Pair<Int, Int> {
    val parts = timeStr.split(":")
    val hour = parts[0].toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return Pair(hour, minute)
}

// Data Models
private enum class RideFilter {
    ALL, REQUESTS, OFFERS
}

private enum class VehicleType {
    ALL, PERSONAL_CAR, TAXI
}

private enum class RideType {
    REQUEST, OFFER
}

private data class MatchingInfo(
    val matchedRiders: Int,
    val savings: String? = null // Savings in TND
)

private data class RideItem(
    val id: String,
    val isOffer: Boolean,
    val vehicleType: VehicleType,
    val origin: String,
    val destination: String,
    val userName: String,
    val time: String,
    val price: String? = null,
    val seatsAvailable: Int? = null, // For offers
    val matchingInfo: MatchingInfo? = null // Matching information
)

/**
 * Sample rides data for development
 */
private fun getSampleRides(): List<RideItem> {
    return listOf(
        RideItem(
            id = "1",
            isOffer = true,
            vehicleType = VehicleType.PERSONAL_CAR,
            origin = "Lac 1, Tunis",
            destination = "Ariana",
            userName = "Ahmed B.",
            time = "Today, 8:30 AM",
            price = null, // No price for personal cars
            seatsAvailable = 3,
            matchingInfo = MatchingInfo(matchedRiders = 2, savings = "3")
        ),
        RideItem(
            id = "2",
            isOffer = false,
            vehicleType = VehicleType.TAXI,
            origin = "Centre-ville, Tunis",
            destination = "La Marsa",
            userName = "Sara M.",
            time = "Today, 9:15 AM",
            price = "8", // Price for taxi
            matchingInfo = MatchingInfo(matchedRiders = 1, savings = "4")
        ),
        RideItem(
            id = "3",
            isOffer = true,
            vehicleType = VehicleType.TAXI,
            origin = "Ben Arous",
            destination = "Tunis Centre",
            userName = "Mohamed K.",
            time = "Today, 10:00 AM",
            price = "6", // Price for taxi
            seatsAvailable = 2,
            matchingInfo = MatchingInfo(matchedRiders = 1, savings = "2")
        ),
        RideItem(
            id = "4",
            isOffer = true,
            vehicleType = VehicleType.PERSONAL_CAR,
            origin = "Sidi Bou Said",
            destination = "Carthage",
            userName = "Fatma L.",
            time = "Today, 11:30 AM",
            price = null, // No price for personal cars
            seatsAvailable = 4
        ),
        RideItem(
            id = "5",
            isOffer = false,
            vehicleType = VehicleType.PERSONAL_CAR,
            origin = "Manouba",
            destination = "Tunis",
            userName = "Youssef A.",
            time = "Today, 2:00 PM",
            price = null // No price for personal cars
        )
    )
}
