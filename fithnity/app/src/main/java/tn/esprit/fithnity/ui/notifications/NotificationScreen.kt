package tn.esprit.fithnity.ui.notifications

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.runtime.DisposableEffect
import tn.esprit.fithnity.data.*
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavHostController,
    userPreferences: UserPreferences,
    viewModel: NotificationViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val authToken = remember { userPreferences.getAuthToken() }
    val uiState by viewModel.uiState.collectAsState()

    // Load notifications and connect WebSocket on mount
    LaunchedEffect(Unit) {
        if (authToken != null) {
            viewModel.loadNotifications(authToken)
            viewModel.connectWebSocket(authToken)
        }
    }
    
    // Disconnect WebSocket on unmount
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectWebSocket()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = Primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotificationsUiState.Idle -> {
                    // Show empty state or loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is NotificationsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is NotificationsUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        EmptyNotificationsView()
                    } else {
                        NotificationsList(
                            notifications = state.notifications,
                            authToken = authToken,
                            viewModel = viewModel,
                            navController = navController,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is NotificationsUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = {
                            if (authToken != null) {
                                viewModel.loadNotifications(authToken)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsList(
    notifications: List<NotificationResponse>,
    authToken: String?,
    viewModel: NotificationViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications) { notification ->
            NotificationItem(
                notification = notification,
                authToken = authToken,
                viewModel = viewModel,
                navController = navController,
                onClick = {
                    // Mark as read if unread
                    if (!notification.read && authToken != null) {
                        viewModel.markAsRead(authToken, notification._id)
                    }
                }
            )
        }

        // "See older notifications" text at the end
        item {
            Text(
                text = "See older notifications",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationResponse,
    authToken: String?,
    viewModel: NotificationViewModel,
    navController: NavHostController,
    onClick: () -> Unit
) {
    val isUnread = !notification.read

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
                
                // Handle navigation based on notification type
                when (notification.type) {
                    "MESSAGE" -> {
                        // Extract conversation data from notification.data
                        val conversationId = notification.data?.get("conversationId") as? String
                        val senderId = notification.data?.get("senderId") as? String
                        val senderName = notification.data?.get("senderName") as? String ?: "User"
                        
                        if (conversationId != null && senderId != null) {
                            // Navigate to chat detail screen
                            // Use "none" for photo since we don't have it in notification data
                            navController.navigate(
                                Screen.ChatDetail.createRoute(
                                    conversationId = conversationId,
                                    otherUserId = senderId,
                                    otherUserName = senderName,
                                    otherUserPhoto = null // Will be encoded as "none" in createRoute
                                )
                            )
                        }
                    }
                    "COMMENT" -> {
                        // Navigate to community post
                        val postId = notification.data?.get("postId") as? String
                        if (postId != null) {
                            navController.navigate("community/post/$postId")
                        }
                    }
                    "PUBLIC_TRANSPORT_SEARCH" -> {
                        // Navigate to home with public transport confirmation dialog
                        navController.navigate("home?showPublicTransportConfirmation=true")
                    }
                    // TODO: Handle other notification types (RIDE_REQUEST, etc.)
                    else -> {
                        // No navigation for other types yet
                    }
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = if (isUnread) {
            Primary.copy(alpha = 0.05f) // Light background for unread
        } else {
            Color.White // White background for read
        },
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon based on notification type
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            "MESSAGE" -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                            "RIDE_REQUEST" -> Color(0xFF10B981).copy(alpha = 0.1f)
                            "RIDE_ACCEPTED" -> Color(0xFF10B981).copy(alpha = 0.1f)
                            "COMMENT" -> Color(0xFF8B5CF6).copy(alpha = 0.1f)
                            "LIKE" -> Color(0xFFEF4444).copy(alpha = 0.1f)
                            else -> Primary.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "MESSAGE" -> Icons.Default.Chat
                        "RIDE_REQUEST", "RIDE_ACCEPTED" -> Icons.Default.DirectionsCar
                        "COMMENT" -> Icons.Default.Comment
                        "LIKE" -> Icons.Default.Favorite
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = when (notification.type) {
                        "MESSAGE" -> Color(0xFF3B82F6)
                        "RIDE_REQUEST", "RIDE_ACCEPTED" -> Color(0xFF10B981)
                        "COMMENT" -> Color(0xFF8B5CF6)
                        "LIKE" -> Color(0xFFEF4444)
                        else -> Primary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isUnread) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                    color = if (isUnread) TextPrimary else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Time ago
                notification.timeAgo?.let { timeAgo ->
                    Text(
                        text = timeAgo,
                        fontSize = 12.sp,
                        color = TextHint,
                        fontWeight = FontWeight.Normal
                    )
                } ?: notification.createdAt?.let { createdAt ->
                    Text(
                        text = formatDate(createdAt),
                        fontSize = 12.sp,
                        color = TextHint,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Unread indicator dot
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    // Empty - just a colored dot
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextHint
            )
            Text(
                text = "No notifications yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = "You'll see notifications here when you receive them",
                fontSize = 14.sp,
                color = TextHint,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFEF4444)
            )
            Text(
                text = "Error loading notifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextHint,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                )
            ) {
                Text("Retry")
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

