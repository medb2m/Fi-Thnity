package tn.esprit.fithnity.ui.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.data.RideResponse
import tn.esprit.fithnity.ui.navigation.FiThnityDetailTopBar
import tn.esprit.fithnity.ui.theme.*
import org.maplibre.android.geometry.LatLng
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import tn.esprit.fithnity.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

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
    val currentUserId = remember { userPreferences.getUserId() }
    val ridesState by viewModel.uiState.collectAsState()
    var selectedRideForEdit by remember { mutableStateOf<RideResponse?>(null) }
    var selectedRideForDetails by remember { mutableStateOf<RideItem?>(null) }
    
    // Load user's rides on first composition
    LaunchedEffect(Unit) {
        viewModel.loadMyRides(authToken, status = null)
    }
    
    // Refresh rides after update
    val createRideState by viewModel.createRideState.collectAsState()
    LaunchedEffect(createRideState) {
        when (val state = createRideState) {
            is CreateRideUiState.Success -> {
                selectedRideForEdit = null
                viewModel.resetCreateRideState()
                viewModel.loadMyRides(authToken, status = null)
            }
            else -> {}
        }
    }
    
    // Filter rides by type and keep full RideResponse for editing
    val filteredRides = when (val state = ridesState) {
        is RideUiState.Success -> {
            state.rides.filter { it.rideType == rideType }
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
                            val rideItem = RideItem(
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
                            EditableRideCard(
                                ride = ride,
                                onEditClick = { selectedRideForEdit = ride },
                                onClick = { selectedRideForDetails = rideItem }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
    
    // Edit Ride Dialog
    selectedRideForEdit?.let { ride ->
        EditRideFormDialog(
            onDismiss = { selectedRideForEdit = null },
            ride = ride,
            viewModel = viewModel,
            authToken = authToken
        )
    }
    
    // Ride Details Dialog
    selectedRideForDetails?.let { ride ->
        RideDetailsDialog(
            ride = ride,
            onDismiss = { selectedRideForDetails = null },
            onApplyToOffer = { /* Not applicable for own rides */ },
            onReplyToRequest = { /* Not applicable for own rides */ },
            currentUserId = currentUserId
        )
    }
}

/**
 * Editable Ride Card with Edit button
 */
@Composable
private fun EditableRideCard(
    ride: RideResponse,
    onEditClick: () -> Unit,
    onClick: () -> Unit
) {
    val rideItem = RideItem(
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Date/Time + Badge + Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Styled Date and Time
                StyledDateTime(rideItem.time)
                
                // Badge and Edit Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Badge (Type + Vehicle)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (rideItem.isOffer) Accent.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (rideItem.vehicleType == VehicleType.TAXI) Icons.Default.LocalTaxi 
                                             else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (rideItem.isOffer) Accent else Primary
                            )
                            Text(
                                text = if (rideItem.isOffer) "Offer" else "Request",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (rideItem.isOffer) Accent else Primary
                            )
                        }
                    }
                    
                    // Edit Button
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Origin and Destination
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            text = rideItem.origin,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = rideItem.destination,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Footer: User + Essential Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = rideItem.userName,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (rideItem.seatsAvailable != null) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Secondary
                        )
                        Text(
                            text = "${rideItem.seatsAvailable}",
                            fontSize = 13.sp,
                            color = Secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (rideItem.vehicleType == VehicleType.TAXI && rideItem.price != null) {
                        Text(
                            text = "${rideItem.price} TND",
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
 * Edit Ride Form Dialog - Pre-filled with existing ride data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRideFormDialog(
    onDismiss: () -> Unit,
    ride: RideResponse,
    viewModel: RideViewModel,
    authToken: String?
) {
    val createRideState by viewModel.createRideState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Pre-fill form with existing ride data
    var selectedVehicleType by remember { 
        mutableStateOf(
            when (ride.transportType) {
                "PRIVATE_CAR" -> VehicleType.PERSONAL_CAR
                "TAXI", "TAXI_COLLECTIF" -> VehicleType.TAXI
                else -> VehicleType.PERSONAL_CAR
            }
        )
    }
    
    var origin by remember { mutableStateOf(ride.origin.address) }
    var originLatLng by remember { mutableStateOf<LatLng?>(LatLng(ride.origin.latitude, ride.origin.longitude)) }
    var showOriginMap by remember { mutableStateOf(false) }
    
    var destination by remember { mutableStateOf(ride.destination.address) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(LatLng(ride.destination.latitude, ride.destination.longitude)) }
    var showDestinationMap by remember { mutableStateOf(false) }
    
    // Parse departure date and time
    val departureDateObj = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(ride.departureDate) ?:
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).parse(ride.departureDate)
    } catch (e: Exception) {
        null
    }
    
    var selectedDate by remember { 
        mutableStateOf<Long?>(departureDateObj?.time)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerDialogShown by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateDisplay = remember(selectedDate) {
        selectedDate?.let { dateFormatter.format(Date(it)) } ?: ""
    }
    
    var time by remember {
        mutableStateOf(
            departureDateObj?.let {
                val calendar = Calendar.getInstance().apply { time = it }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                String.format("%d:%02d", hour, minute)
            } ?: ""
        )
    }
    
    var seatsAvailable by remember { mutableStateOf(ride.availableSeats.toString()) }
    var price by remember { mutableStateOf(ride.price?.toString() ?: "") }
    
    // Validation errors
    var originError by remember { mutableStateOf<String?>(null) }
    var destinationError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var seatsError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    
    // Validation functions (same as AddRideFormDialog)
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
            dateValue < System.currentTimeMillis() - 86400000 -> "Date cannot be in the past"
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
    
    fun formatTimeInput(input: String): String {
        val digitsOnly = input.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""
        val limitedDigits = digitsOnly.take(4)
        val timePart = when (limitedDigits.length) {
            1 -> limitedDigits
            2 -> limitedDigits
            3 -> "${limitedDigits[0]}:${limitedDigits.substring(1)}"
            4 -> "${limitedDigits.substring(0, 2)}:${limitedDigits.substring(2)}"
            else -> ""
        }
        val amPmPart = when {
            input.uppercase().contains("AM") -> " AM"
            input.uppercase().contains("PM") -> " PM"
            else -> ""
        }
        return timePart + amPmPart
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Ride",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
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
                        label = { Text("Car") },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(18.dp))
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
                            Icon(Icons.Default.LocalTaxi, null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                
                // Origin
                OutlinedTextField(
                    value = origin,
                    onValueChange = { },
                    label = { Text("Origin *") },
                    placeholder = { Text("Tap to select on map") },
                    leadingIcon = { Icon(Icons.Default.Place, null) },
                    trailingIcon = {
                        IconButton(onClick = { showOriginMap = true }) {
                            Icon(Icons.Default.Map, "Select on Map", modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOriginMap = true },
                    readOnly = true,
                    singleLine = true,
                    isError = originError != null,
                    supportingText = originError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                )
                
                // Destination
                OutlinedTextField(
                    value = destination,
                    onValueChange = { },
                    label = { Text("Destination *") },
                    placeholder = { Text("Tap to select on map") },
                    leadingIcon = { Icon(Icons.Default.Place, null) },
                    trailingIcon = {
                        IconButton(onClick = { showDestinationMap = true }) {
                            Icon(Icons.Default.Map, "Select on Map", modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDestinationMap = true },
                    readOnly = true,
                    singleLine = true,
                    isError = destinationError != null,
                    supportingText = destinationError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                )
                
                // Date
                OutlinedTextField(
                    value = dateDisplay,
                    onValueChange = { },
                    label = { Text("Date *") },
                    placeholder = { Text("Select date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, "Select Date", modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true,
                    singleLine = true,
                    isError = dateError != null,
                    supportingText = dateError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                )
                
                // Time
                OutlinedTextField(
                    value = time,
                    onValueChange = { 
                        val formatted = formatTimeInput(it)
                        time = formatted
                        timeError = validateTime(formatted)
                    },
                    label = { Text("Time *") },
                    placeholder = { Text("8:30 AM or 14:30") },
                    leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    isError = timeError != null,
                    supportingText = timeError?.let { { Text(it, color = Error, fontSize = 11.sp) } }
                )
                
                // Seats
                OutlinedTextField(
                    value = seatsAvailable,
                    onValueChange = { 
                        seatsAvailable = it.filter { char -> char.isDigit() }
                        seatsError = validateSeats(seatsAvailable)
                    },
                    label = { Text("Available Seats *") },
                    placeholder = { Text("e.g., 3") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
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
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        isError = priceError != null,
                        supportingText = priceError?.let { { Text(it, color = Error) } }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    originError = validateOrigin(origin)
                    destinationError = validateDestination(destination)
                    dateError = validateDate(selectedDate)
                    timeError = validateTime(time)
                    seatsError = validateSeats(seatsAvailable)
                    priceError = validatePrice(price, selectedVehicleType == VehicleType.TAXI)
                    
                    if (originError == null && destinationError == null && dateError == null &&
                        timeError == null && seatsError == null && priceError == null &&
                        originLatLng != null && destinationLatLng != null) {
                        
                        val time24Hour = convertTo24Hour(time)
                        val departureDateTime = selectedDate?.let { dateMillis ->
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                            }
                            val (hour, minute) = parseTime(time24Hour)
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(calendar.time)
                        }
                        
                        val transportType = when (selectedVehicleType) {
                            VehicleType.PERSONAL_CAR -> "PRIVATE_CAR"
                            VehicleType.TAXI -> "TAXI"
                            else -> "PRIVATE_CAR"
                        }
                        
                        viewModel.updateRide(
                            authToken = authToken,
                            rideId = ride._id,
                            transportType = transportType,
                            origin = tn.esprit.fithnity.data.Location(
                                latitude = originLatLng!!.latitude,
                                longitude = originLatLng!!.longitude,
                                address = origin.trim()
                            ),
                            destination = tn.esprit.fithnity.data.Location(
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
                         originLatLng != null && destinationLatLng != null && 
                         selectedDate != null && time.isNotBlank() && 
                         seatsAvailable.isNotBlank() &&
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
                Text(
                    text = if (createRideState is CreateRideUiState.Loading) "Updating..." else "Update"
                )
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
    
    // Date Picker
    LaunchedEffect(showDatePicker) {
        if (showDatePicker && !datePickerDialogShown) {
            datePickerDialogShown = true
            val calendar = Calendar.getInstance()
            selectedDate?.let { calendar.timeInMillis = it }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                R.style.DatePickerDialogTheme,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val newCalendar = Calendar.getInstance()
                    newCalendar.set(selectedYear, selectedMonth, selectedDay)
                    selectedDate = newCalendar.timeInMillis
                    dateError = validateDate(newCalendar.timeInMillis)
                    showDatePicker = false
                    datePickerDialogShown = false
                },
                year, month, day
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 86400000
            datePickerDialog.setOnCancelListener {
                showDatePicker = false
                datePickerDialogShown = false
            }
            datePickerDialog.setOnDismissListener {
                showDatePicker = false
                datePickerDialogShown = false
            }
            datePickerDialog.show()
        } else if (!showDatePicker) {
            datePickerDialogShown = false
        }
    }
    
    // Map dialogs (reuse from RidesScreen)
    if (showOriginMap) {
        DestinationMapDialog(
            onDismiss = { showOriginMap = false },
            onLocationSelected = { latLng, address ->
                originLatLng = latLng
                origin = address
                originError = validateOrigin(address)
                showOriginMap = false
            },
            initialLocation = originLatLng ?: LatLng(36.8065, 10.1815),
            dialogTitle = "Select Origin"
        )
    }
    
    if (showDestinationMap) {
        DestinationMapDialog(
            onDismiss = { showDestinationMap = false },
            onLocationSelected = { latLng, address ->
                destinationLatLng = latLng
                destination = address
                destinationError = validateDestination(address)
                showDestinationMap = false
            },
            initialLocation = destinationLatLng ?: originLatLng ?: LatLng(36.8065, 10.1815)
        )
    }
    
    // Show error message if update failed
    if (createRideState is CreateRideUiState.Error) {
        LaunchedEffect(createRideState) {
            android.widget.Toast.makeText(
                context,
                "Failed to update ride: ${(createRideState as CreateRideUiState.Error).message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}

