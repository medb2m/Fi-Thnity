package tn.esprit.fithnity.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.esprit.fithnity.data.MessageResponse
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.components.ToastManager
import tn.esprit.fithnity.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageOptionsMenu by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Function to create camera file URI
    val createCameraFile: () -> Uri? = remember {
        {
            try {
                val imageFile = File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.jpg")
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                ).also {
                    cameraUri = it
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            selectedImageUri = cameraUri
            cameraUri?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    selectedImageFile = uriToFile(context, uri)
                }
            }
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            createCameraFile()?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            ToastManager.showError("Camera permission is required to take photos")
        }
    }
    
    // Image picker for gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Convert URI to File
            coroutineScope.launch(Dispatchers.IO) {
                selectedImageFile = uriToFile(context, it)
            }
        }
    }
    
    // Function to check and request camera permission
    val requestCameraPermission: () -> Unit = remember {
        {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) -> {
                    // Permission already granted
                    createCameraFile()?.let { uri ->
                        cameraLauncher.launch(uri)
                    }
                }
                else -> {
                    // Request permission
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

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
                Column {
                    // Image preview
                    if (selectedImageUri != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.1f)
                        ) {
                            Box {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Remove button
                                IconButton(
                                    onClick = {
                                        selectedImageUri = null
                                        selectedImageFile = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Add image button with dropdown menu
                        Box {
                            IconButton(
                                onClick = { showImageOptionsMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add image",
                                    tint = Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            // Dropdown menu with image options
                            DropdownMenu(
                                expanded = showImageOptionsMenu,
                                onDismissRequest = { showImageOptionsMenu = false },
                                offset = androidx.compose.ui.unit.DpOffset(0.dp, (-48).dp), // Offset to appear above button
                                modifier = Modifier
                                    .background(Color.White)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                // Gallery option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                "Gallery",
                                                fontSize = 15.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        showImageOptionsMenu = false
                                        galleryLauncher.launch("image/*")
                                    }
                                )
                                
                                // Camera option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                "Camera",
                                                fontSize = 15.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        showImageOptionsMenu = false
                                        requestCameraPermission()
                                    }
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
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
                                coroutineScope.launch {
                                    if (selectedImageFile != null && authToken != null) {
                                        isUploadingImage = true
                                        val imageUrl = withContext(Dispatchers.IO) {
                                            viewModel.uploadChatImage(authToken, selectedImageFile!!)
                                        }
                                        isUploadingImage = false
                                        
                                        if (imageUrl != null) {
                                            viewModel.sendMessage(
                                                authToken,
                                                conversationId,
                                                messageText,
                                                imageUrl
                                            )
                                            messageText = ""
                                            selectedImageUri = null
                                            selectedImageFile = null
                                        }
                                    } else if (messageText.isNotBlank()) {
                                        viewModel.sendMessage(authToken, conversationId, messageText)
                                        messageText = ""
                                    }
                                }
                            },
                            enabled = (messageText.isNotBlank() || selectedImageUri != null) 
                                && sendMessageState !is SendMessageUiState.Sending 
                                && !isUploadingImage,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (messageText.isNotBlank() || selectedImageUri != null) 
                                        Primary 
                                    else 
                                        Primary.copy(alpha = 0.3f)
                                )
                        ) {
                            if (sendMessageState is SendMessageUiState.Sending || isUploadingImage) {
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
 * Helper function to convert URI to File
 */
suspend fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            file
        } catch (e: Exception) {
            null
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
                // Image if message type is IMAGE
                if (message.messageType == "IMAGE" && message.imageUrl != null) {
                    val fullImageUrl = if (message.imageUrl!!.startsWith("http")) {
                        message.imageUrl
                    } else {
                        "http://72.61.145.239:9090${message.imageUrl}"
                    }
                    
                    AsyncImage(
                        model = fullImageUrl,
                        contentDescription = "Message image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (message.content.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                
                // Text content
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        fontSize = 15.sp,
                        color = if (isOwnMessage) Color.White else TextPrimary,
                        lineHeight = 20.sp
                    )
                }
                
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

