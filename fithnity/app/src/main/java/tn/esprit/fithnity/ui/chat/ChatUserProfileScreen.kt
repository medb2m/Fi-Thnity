package tn.esprit.fithnity.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.data.RideResponse
import tn.esprit.fithnity.ui.rides.RideViewModel
import tn.esprit.fithnity.ui.rides.RideUiState
import tn.esprit.fithnity.ui.friends.FriendViewModel
import tn.esprit.fithnity.ui.theme.*
import tn.esprit.fithnity.ui.components.ToastManager
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Chat User Profile Screen - Messenger-style user profile preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatUserProfileScreen(
    navController: NavHostController,
    userId: String,
    userName: String,
    userPhoto: String?,
    userPreferences: UserPreferences,
    rideViewModel: RideViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRideSelectionDialog by remember { mutableStateOf(false) }
    val authToken = remember { userPreferences.getAuthToken() }
    val currentUserId = remember { userPreferences.getUserId() }
    val ridesState by rideViewModel.uiState.collectAsState()
    val friendViewModel: FriendViewModel = viewModel()
    val friendStatusState by friendViewModel.friendStatusState.collectAsState()
    val chatViewModel: ChatViewModel = viewModel()
    var conversationId by remember { mutableStateOf<String?>(null) }
    
    // Load friend status (with delay to avoid blocking initial render)
    LaunchedEffect(userId) {
        if (authToken != null) {
            kotlinx.coroutines.delay(200)
            friendViewModel.getFriendStatus(authToken, userId)
        }
    }
    
    // Get or create conversation when needed
    fun getOrCreateConversation(onSuccess: (String) -> Unit) {
        if (conversationId != null) {
            onSuccess(conversationId!!)
            return
        }
        
        if (authToken == null) return
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = tn.esprit.fithnity.data.NetworkModule.chatApi.getOrCreateConversation(
                    bearer = "Bearer $authToken",
                    request = tn.esprit.fithnity.data.CreateConversationRequest(otherUserId = userId)
                )
                if (response.success && response.data != null) {
                    conversationId = response.data._id
                    withContext(Dispatchers.Main) {
                        onSuccess(response.data._id)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ToastManager.showToast(
                        type = tn.esprit.fithnity.ui.components.ToastType.ERROR,
                        message = "Failed to load conversation"
                    )
                }
            }
        }
    }
    
    // Load user's offers when dialog opens
    LaunchedEffect(showRideSelectionDialog) {
        if (showRideSelectionDialog && authToken != null) {
            rideViewModel.loadMyRides(authToken, status = "ACTIVE")
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.1f),
                        Surface,
                        Surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Large profile picture
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Primary, Primary.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (userPhoto != null && userPhoto.isNotEmpty() && userPhoto != "none") {
                    val fullPhotoUrl = if (userPhoto.startsWith("http")) {
                        userPhoto
                    } else {
                        "http://72.61.145.239:9090$userPhoto"
                    }
                    AsyncImage(
                        model = fullPhotoUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // User name
            Text(
                text = userName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status indicator (online/offline - placeholder for now)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)) // Green for online
                )
                Text(
                    text = "Active now",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val friendStatus = friendStatusState
                val friendButtonLabel = when (friendStatus?.status) {
                    "ACCEPTED" -> "Friends"
                    "PENDING" -> if (friendStatus.isRequester == true) "Request Sent" else "Accept Request"
                    else -> "Add Friend"
                }
                
                ActionCircleButton(
                    icon = Icons.Default.PersonAdd,
                    label = friendButtonLabel,
                    onClick = {
                        when (friendStatus?.status) {
                            "ACCEPTED" -> {
                                ToastManager.showToast(
                                    type = tn.esprit.fithnity.ui.components.ToastType.INFO,
                                    message = "You are already friends with $userName"
                                )
                            }
                            "PENDING" -> {
                                if (friendStatus.isRequester == false && friendStatus.requestId != null) {
                                    // Accept the request
                                    friendViewModel.acceptFriendRequest(authToken, friendStatus.requestId)
                                    ToastManager.showToast(
                                        type = tn.esprit.fithnity.ui.components.ToastType.SUCCESS,
                                        message = "Friend request accepted"
                                    )
                                    // Reload friend status
                                    friendViewModel.getFriendStatus(authToken, userId)
                                } else {
                                    ToastManager.showToast(
                                        type = tn.esprit.fithnity.ui.components.ToastType.INFO,
                                        message = "Friend request already sent"
                                    )
                                }
                            }
                            else -> {
                                // Send friend request
                                friendViewModel.sendFriendRequest(authToken, userId)
                                ToastManager.showToast(
                                    type = tn.esprit.fithnity.ui.components.ToastType.SUCCESS,
                                    message = "Friend request sent to $userName"
                                )
                                // Note: ViewModel automatically updates state and refreshes friend status
                            }
                        }
                    }
                )
                
                ActionCircleButton(
                    icon = Icons.Default.DirectionsCar,
                    label = "Add to Ride",
                    onClick = { showRideSelectionDialog = true }
                )
                
                ActionCircleButton(
                    icon = Icons.Default.Notifications,
                    label = "Mute",
                    onClick = { /* TODO: Implement mute */ }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Options section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    ProfileOptionItem(
                        icon = Icons.Default.Image,
                        title = "Media, Links & Docs",
                        subtitle = "View shared media",
                        onClick = {
                            getOrCreateConversation { convId ->
                                navController.navigate(tn.esprit.fithnity.ui.navigation.Screen.SharedMedia.createRoute(convId))
                            }
                        }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = DividerColor
                    )
                    
                    ProfileOptionItem(
                        icon = Icons.Default.Search,
                        title = "Search in Conversation",
                        subtitle = "Find messages",
                        onClick = { /* TODO: Implement search */ }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = DividerColor
                    )
                    
                    ProfileOptionItem(
                        icon = Icons.Default.Palette,
                        title = "Customize Chat",
                        subtitle = "Themes, emoji, nicknames",
                        onClick = { /* TODO: Implement customize */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy & Support section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    ProfileOptionItem(
                        icon = Icons.Default.Block,
                        title = "Block",
                        subtitle = "Block this user",
                        iconTint = Error,
                        onClick = { /* TODO: Implement block */ }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = DividerColor
                    )
                    
                    ProfileOptionItem(
                        icon = Icons.Default.Report,
                        title = "Report",
                        subtitle = "Report a problem",
                        iconTint = Error,
                        onClick = { /* TODO: Implement report */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    
    // Ride Selection Dialog
    if (showRideSelectionDialog) {
        RideSelectionDialog(
            onDismiss = { showRideSelectionDialog = false },
            ridesState = ridesState,
            userIdToAdd = userId,
            authToken = authToken,
            rideViewModel = rideViewModel,
            onUserAdded = {
                showRideSelectionDialog = false
                android.widget.Toast.makeText(
                    context,
                    "$userName has been added to the ride",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun ActionCircleButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Primary.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = Primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Dialog to select a ride offer to add a user to
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideSelectionDialog(
    onDismiss: () -> Unit,
    ridesState: RideUiState,
    userIdToAdd: String,
    authToken: String?,
    rideViewModel: RideViewModel,
    onUserAdded: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    
    // Filter only OFFER rides that are active and have available seats
    val availableOffers = when (ridesState) {
        is RideUiState.Success -> {
            ridesState.rides
                .filter { it.rideType == "OFFER" && it.status == "ACTIVE" && it.availableSeats > 0 }
        }
        else -> emptyList()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select a Ride Offer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            when (ridesState) {
                is RideUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is RideUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Error loading rides",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Error
                        )
                        Text(
                            text = ridesState.message,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
                is RideUiState.Success -> {
                    if (availableOffers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = TextHint
                                )
                                Text(
                                    text = "No available offers",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "You don't have any active ride offers with available seats",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableOffers) { ride ->
                                RideOfferItem(
                                    ride = ride,
                                    dateFormatter = dateFormatter,
                                    onClick = {
                                        if (authToken != null) {
                                            rideViewModel.addUserToRide(
                                                authToken = authToken,
                                                rideId = ride._id,
                                                userId = userIdToAdd
                                            )
                                            onUserAdded()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Ride offer item in the selection dialog
 */
@Composable
private fun RideOfferItem(
    ride: RideResponse,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Origin and Destination
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                        Text(
                            text = ride.origin.address,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Accent
                        )
                        Text(
                            text = ride.destination.address,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            HorizontalDivider(color = DividerColor)
            
            // Date and Seats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Text(
                        text = try {
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                .parse(ride.departureDate) ?: Date()
                            dateFormatter.format(date)
                        } catch (e: Exception) {
                            ride.departureDate
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Text(
                        text = "${ride.availableSeats} seat${if (ride.availableSeats > 1) "s" else ""} available",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Price if available
            if (ride.price != null && ride.price > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Accent
                    )
                    Text(
                        text = "${ride.price} TND",
                        fontSize = 12.sp,
                        color = Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

