package tn.esprit.fithnity.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.MessageResponse
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.theme.*

/**
 * ChatScreen - Individual conversation view (WhatsApp/Messenger style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    conversationId: String,
    otherUserId: String,
    otherUserName: String = "User",
    otherUserPhoto: String? = null,
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences,
    viewModel: ChatViewModel = viewModel()
) {
    val messagesState by viewModel.messagesState.collectAsState()
    val sendMessageState by viewModel.sendMessageState.collectAsState()
    val authToken = userPreferences.getAuthToken()
    val currentUserId = userPreferences.getUserId()
    
    var messageText by remember { mutableStateOf("") }
    var displayUserName by remember { mutableStateOf(otherUserName) }
    var displayUserPhoto by remember { mutableStateOf(otherUserPhoto) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Load messages
    LaunchedEffect(conversationId) {
        viewModel.loadMessages(authToken, conversationId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messagesState) {
        if (messagesState is MessagesUiState.Success) {
            val messages = (messagesState as MessagesUiState.Success).messages
            if (messages.isNotEmpty()) {
                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size - 1)
                }
                // Update photo from messages if not already set (fallback)
                if (displayUserPhoto == null) {
                    messages.firstOrNull()?.let { msg ->
                        if (msg.sender._id != currentUserId) {
                            displayUserPhoto = msg.sender.photoUrl
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Primary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Profile Picture
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (displayUserPhoto != null && displayUserPhoto!!.isNotEmpty()) {
                            val fullPhotoUrl = if (displayUserPhoto!!.startsWith("http")) {
                                displayUserPhoto
                            } else {
                                "http://72.61.145.239:9090$displayUserPhoto"
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
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = displayUserName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = Surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 120.dp),
                        placeholder = { Text("Message", color = TextHint) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(authToken, conversationId, messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() && sendMessageState !is SendMessageUiState.Sending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (messageText.isNotBlank()) Primary else Primary.copy(alpha = 0.3f))
                    ) {
                        if (sendMessageState is SendMessageUiState.Sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ChatBackground)
        ) {
            when (val state = messagesState) {
                is MessagesUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                is MessagesUiState.Success -> {
                    if (state.messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No messages yet\nSend a message to start the conversation",
                                fontSize = 16.sp,
                                color = TextHint,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.messages) { message ->
                                MessageBubble(
                                    message = message,
                                    isOwnMessage = message.sender._id == currentUserId
                                )
                            }
                        }
                    }
                }

                is MessagesUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Error loading messages",
                                fontSize = 16.sp,
                                color = Error
                            )
                            Button(
                                onClick = { viewModel.loadMessages(authToken, conversationId) },
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
    }
}

/**
 * Message bubble (WhatsApp/Messenger style)
 */
@Composable
fun MessageBubble(
    message: MessageResponse,
    isOwnMessage: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                        bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                    )
                ),
            color = if (isOwnMessage) Primary else Color.White,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    color = if (isOwnMessage) Color.White else TextPrimary,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = message.timeAgo ?: formatTimeAgo(message.createdAt ?: ""),
                    fontSize = 11.sp,
                    color = if (isOwnMessage) Color.White.copy(alpha = 0.7f) else TextHint,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

