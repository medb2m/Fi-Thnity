package tn.esprit.fithnity.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import tn.esprit.fithnity.data.ConversationResponse
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.navigation.SearchState
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.runtime.DisposableEffect

/**
 * ChatListScreen - Shows list of conversations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences,
    viewModel: ChatViewModel = viewModel()
) {
    val conversationsState by viewModel.conversationsState.collectAsState()
    val authToken = userPreferences.getAuthToken()
    val currentUserId = userPreferences.getUserId()
    var showNewChatDialog by remember { mutableStateOf(false) }
    
    // Get search query from global SearchState
    var searchQuery by remember { mutableStateOf(SearchState.searchQuery) }
    
    // Listen to search state changes
    LaunchedEffect(SearchState.searchQuery) {
        searchQuery = SearchState.searchQuery
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            // Clear search when leaving chat screen
            SearchState.updateQuery("")
        }
    }

    LaunchedEffect(Unit) {
        // Small delay to avoid blocking initial render
        kotlinx.coroutines.delay(100)
        viewModel.loadConversations(authToken)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Surface(
                color = Surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Messages",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    // Circular yellow + button
                    IconButton(
                        onClick = { showNewChatDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFFFC107), // Yellow color
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Conversations List
            when (val state = conversationsState) {
                is ConversationsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                is ConversationsUiState.Success -> {
                    // Filter conversations by search query
                    val filteredConversations = if (searchQuery.isBlank()) {
                        state.conversations
                    } else {
                        val queryLower = searchQuery.lowercase()
                        state.conversations.filter { conversation ->
                            conversation.otherUser.name?.lowercase()?.contains(queryLower) == true
                        }
                    }
                    
                    if (state.conversations.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = TextHint
                                )
                                Text(
                                    text = "No conversations yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Tap the + button to start a new chat",
                                    fontSize = 14.sp,
                                    color = TextHint
                                )
                            }
                        }
                    } else if (filteredConversations.isEmpty() && searchQuery.isNotEmpty()) {
                        // No results for search
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = TextHint
                                )
                                Text(
                                    text = "No users found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Try searching with a different name",
                                    fontSize = 14.sp,
                                    color = TextHint
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = UiConstants.ContentBottomPadding + 80.dp)
                        ) {
                            items(filteredConversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    currentUserId = currentUserId,
                                    onClick = {
                                        val userName = conversation.otherUser.name ?: "User"
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
                                        navController.navigate("chat_detail/${conversation._id}/${conversation.otherUser._id}/$encodedName/$encodedPhoto")
                                    }
                                )
                            }
                        }
                    }
                }

                is ConversationsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Error loading conversations",
                                fontSize = 16.sp,
                                color = Error
                            )
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Button(
                                onClick = { viewModel.loadConversations(authToken) },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {}
            }
        }

        // Floating Action Button for New Chat
        // New Chat Dialog
        if (showNewChatDialog) {
            UserSelectionDialog(
                userPreferences = userPreferences,
                viewModel = viewModel,
                onDismiss = { showNewChatDialog = false },
                onUserSelected = { user ->
                    showNewChatDialog = false
                    viewModel.getOrCreateConversation(authToken, user._id) { conversation ->
                        // Use conversation's otherUser data (from API response)
                        val userName = conversation.otherUser.name ?: "User"
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
                        navController.navigate("chat_detail/${conversation._id}/${conversation.otherUser._id}/$encodedName/$encodedPhoto")
                    }
                }
            )
        }
    }
}

/**
 * Single conversation item in the list
 */
@Composable
fun ConversationItem(
    conversation: ConversationResponse,
    onClick: () -> Unit,
    currentUserId: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (conversation.otherUser.photoUrl != null && conversation.otherUser.photoUrl.isNotEmpty()) {
                    val fullPhotoUrl = if (conversation.otherUser.photoUrl.startsWith("http")) {
                        conversation.otherUser.photoUrl
                    } else {
                        "http://72.61.145.239:9090${conversation.otherUser.photoUrl}"
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
                        modifier = Modifier.size(28.dp),
                        tint = Primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Conversation Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.otherUser.name ?: "User",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (conversation.lastMessageTime != null) {
                        Text(
                            text = formatTimeAgo(conversation.lastMessageTime),
                            fontSize = 12.sp,
                            color = TextHint
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Format last message preview
                    val lastMessagePreview = when {
                        conversation.lastMessage == null -> "No messages yet"
                        conversation.lastMessage.messageType == "IMAGE" -> {
                            // Check if current user sent the message
                            val isSentByMe = conversation.lastMessage.sender == currentUserId
                            val senderName = if (isSentByMe) "You" else (conversation.otherUser.name ?: "User")
                            "$senderName sent a photo"
                        }
                        conversation.lastMessage.messageType == "AUDIO" -> {
                            // Check if current user sent the message
                            val isSentByMe = conversation.lastMessage.sender == currentUserId
                            val senderName = if (isSentByMe) "You" else (conversation.otherUser.name ?: "User")
                            "$senderName sent a voice message"
                        }
                        conversation.lastMessage.content.isBlank() -> "No messages yet"
                        else -> conversation.lastMessage.content
                    }
                    
                    Text(
                        text = lastMessagePreview,
                        fontSize = 14.sp,
                        color = if (conversation.unreadCount > 0) TextPrimary else TextSecondary,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    Divider(
        modifier = Modifier.padding(start = 84.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}
