package tn.esprit.fithnity.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.esprit.fithnity.data.MessageResponse
import tn.esprit.fithnity.data.LocationData
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.components.ToastManager
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.net.URLEncoder

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
 * Helper function to download audio from URL to local file
 */
suspend fun downloadAudioFile(context: android.content.Context, audioUrl: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("ChatScreen", "Downloading audio from: $audioUrl")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(audioUrl)
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("ChatScreen", "Failed to download audio: HTTP ${response.code} - ${response.message}")
                return@withContext null
            }
            
            val body = response.body ?: run {
                Log.e("ChatScreen", "Response body is null")
                return@withContext null
            }
            
            // Determine file extension from URL or use .3gp as default
            val extension = when {
                audioUrl.contains(".m4a") -> ".m4a"
                audioUrl.contains(".3gp") -> ".3gp"
                audioUrl.contains(".mp3") -> ".mp3"
                audioUrl.contains(".wav") -> ".wav"
                audioUrl.contains(".aac") -> ".aac"
                else -> ".3gp" // Default to 3gp for maximum compatibility
            }
            
            val file = File(context.cacheDir, "chat_audio_${System.currentTimeMillis()}$extension")
            val outputStream = FileOutputStream(file)
            
            var bytesCopied = 0L
            body.byteStream().use { input ->
                outputStream.use { output ->
                    bytesCopied = input.copyTo(output)
                }
            }
            
            if (file.exists() && file.length() > 0) {
                Log.d("ChatScreen", "Audio downloaded successfully: ${file.absolutePath}, size: ${file.length()} bytes")
                file
            } else {
                Log.e("ChatScreen", "Downloaded file is empty or doesn't exist")
                file.delete()
                null
            }
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error downloading audio: ${e.message}", e)
            null
        }
    }
}

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
    
    // Voice recording states
    var isRecordingMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    
    // Location sharing states
    var isGettingLocation by remember { mutableStateOf(false) }
    var fusedLocationClient by remember { mutableStateOf<FusedLocationProviderClient?>(null) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Initialize location client
    LaunchedEffect(Unit) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }
    
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
            coroutineScope.launch(Dispatchers.IO) {
                selectedImageFile = uriToFile(context, it)
            }
        }
    }
    
    // Function to check and request camera permission
    val requestCameraPermission: () -> Unit = remember {
        {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                    createCameraFile()?.let { uri ->
                        cameraLauncher.launch(uri)
                    }
                }
                else -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
    
    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecordingMode = true
        } else {
            ToastManager.showError("Microphone permission is required to record audio")
        }
    }
    
    // Function to request audio permission and start recording mode
    val requestAudioPermission: () -> Unit = remember {
        {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                    isRecordingMode = true
                }
                else -> {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
    
    // Function to get current location and share
    fun getCurrentLocationAndShare() {
        if (fusedLocationClient != null && authToken != null) {
            isGettingLocation = true
            try {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient!!.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            // Send location message
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                address = null
                            )
                            viewModel.sendMessage(
                                authToken,
                                conversationId,
                                "Shared live location",
                                location = locationData
                            )
                            ToastManager.showSuccess("Location shared")
                        } else {
                            ToastManager.showError("Could not get current location")
                        }
                        isGettingLocation = false
                    }.addOnFailureListener { e ->
                        Log.e("ChatScreen", "Failed to get location", e)
                        ToastManager.showError("Failed to get location: ${e.message}")
                        isGettingLocation = false
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error getting location", e)
                ToastManager.showError("Error getting location")
                isGettingLocation = false
            }
        }
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, get location
            getCurrentLocationAndShare()
        } else {
            ToastManager.showError("Location permission is required to share location")
            isGettingLocation = false
        }
    }
    
    // Function to request location permission
    val requestLocationPermission: () -> Unit = remember {
        {
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val coarseLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (fineLocationGranted || coarseLocationGranted) {
                getCurrentLocationAndShare()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    // Function to start recording
    val startRecording: () -> Unit = remember {
        {
            try {
                // Use 3GP format with AMR encoder for maximum Android compatibility
                val outputFile = File(context.cacheDir, "voice_message_${System.currentTimeMillis()}.3gp")
                audioFile = outputFile
                
                Log.d("ChatScreen", "Starting recording to: ${outputFile.absolutePath}")
                
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    // Use 3GP format with AMR encoder - most compatible on Android
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }
                
                Log.d("ChatScreen", "Recording started successfully")
                isRecording = true
                recordingDuration = 0
                
                coroutineScope.launch {
                    while (isRecording) {
                        delay(1000)
                        recordingDuration++
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Failed to start recording: ${e.message}", e)
                ToastManager.showError("Failed to start recording: ${e.message}")
                isRecording = false
                mediaRecorder?.release()
                mediaRecorder = null
            }
        }
    }
    
    // Function to stop recording
    val stopRecording: () -> Unit = remember {
        {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
            } catch (e: Exception) {
                ToastManager.showError("Failed to stop recording")
            }
        }
    }
    
    // Function to cancel recording
    val cancelRecording: () -> Unit = remember {
        {
            try {
                if (isRecording) {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null
                    isRecording = false
                }
                audioFile?.delete()
                audioFile = null
                recordingDuration = 0
                isRecordingMode = false
            } catch (e: Exception) {
                isRecordingMode = false
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

                    // Clickable profile section - navigates to user profile preview
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                navController.navigate(
                                    Screen.ChatUserProfile.createRoute(
                                        userId = otherUserId,
                                        userName = displayUserName,
                                        userPhoto = displayUserPhoto
                                    )
                                )
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = displayUserName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            
                            Spacer(Modifier.height(2.dp))
                            
                            // Active now indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)) // Green for active
                                )
                                Text(
                                    text = "Active now",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
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
                                
                                IconButton(
                                    onClick = {
                                        selectedImageUri = null
                                        selectedImageFile = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecordingMode) {
                            // Recording mode UI
                            IconButton(
                                onClick = { cancelRecording() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = Error,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isRecording) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Error)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isRecording) {
                                        "%02d:%02d".format(recordingDuration / 60, recordingDuration % 60)
                                    } else {
                                        "Tap to record"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isRecording) Error else TextSecondary
                                )
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            if (isRecording) {
                                IconButton(
                                    onClick = { stopRecording() },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Primary)
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop recording",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else if (audioFile != null) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            audioFile?.let { file ->
                                                // Verify file exists and has content
                                                if (!file.exists() || file.length() == 0L) {
                                                    ToastManager.showError("Audio file is invalid")
                                                    Log.e("ChatScreen", "Audio file invalid: exists=${file.exists()}, size=${file.length()}")
                                                    return@launch
                                                }
                                                
                                                Log.d("ChatScreen", "Uploading audio file: ${file.absolutePath}, size: ${file.length()} bytes")
                                                
                                                if (authToken != null) {
                                                    isUploadingImage = true
                                                    val audioUrl = withContext(Dispatchers.IO) {
                                                        viewModel.uploadChatAudio(authToken, file)
                                                    }
                                                    isUploadingImage = false
                                                    
                                                    if (audioUrl != null) {
                                                        Log.d("ChatScreen", "Audio uploaded successfully, sending message")
                                                        viewModel.sendMessage(
                                                            authToken,
                                                            conversationId,
                                                            "",
                                                            null,
                                                            audioUrl,
                                                            recordingDuration
                                                        )
                                                        isRecordingMode = false
                                                        audioFile = null
                                                        recordingDuration = 0
                                                    } else {
                                                        ToastManager.showError("Failed to upload audio")
                                                        Log.e("ChatScreen", "Audio upload failed")
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isUploadingImage,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Primary)
                                ) {
                                    if (isUploadingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = { startRecording() },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Primary)
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Start recording",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        } else {
                            // Normal mode UI (text input)
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
                                
                                DropdownMenu(
                                    expanded = showImageOptionsMenu,
                                    onDismissRequest = { showImageOptionsMenu = false },
                                    offset = androidx.compose.ui.unit.DpOffset(0.dp, (-48).dp),
                                    modifier = Modifier
                                        .background(Color.White)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
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
                                                Text("Gallery", fontSize = 15.sp, color = TextPrimary)
                                            }
                                        },
                                        onClick = {
                                            showImageOptionsMenu = false
                                            galleryLauncher.launch("image/*")
                                        }
                                    )
                                    
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
                                                Text("Camera", fontSize = 15.sp, color = TextPrimary)
                                            }
                                        },
                                        onClick = {
                                            showImageOptionsMenu = false
                                            requestCameraPermission()
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = Primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text("Voice Message", fontSize = 15.sp, color = TextPrimary)
                                            }
                                        },
                                        onClick = {
                                            showImageOptionsMenu = false
                                            requestAudioPermission()
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = Primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text("Share Live Location", fontSize = 15.sp, color = TextPrimary)
                                            }
                                        },
                                        onClick = {
                                            showImageOptionsMenu = false
                                            requestLocationPermission()
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
                                    isOwnMessage = message.sender._id == currentUserId,
                                    onLocationClick = { locationData ->
                                        // Navigate to home screen with location - ENCODE parameters properly
                                        val encodedUserName = URLEncoder.encode(message.sender.name ?: "User", "UTF-8")
                                        val encodedUserPhoto = URLEncoder.encode(message.sender.photoUrl ?: "none", "UTF-8")
                                        
                                        navController.navigate(
                                            Screen.Home.route +
                                            "?lat=${locationData.latitude}" +
                                            "&lng=${locationData.longitude}" +
                                            "&userName=$encodedUserName" +
                                            "&userPhoto=$encodedUserPhoto"
                                        )
                                    }
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
    isOwnMessage: Boolean,
    onLocationClick: ((LocationData) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Audio playback state
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0f) }
    var currentPosition by remember { mutableStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var localAudioFile by remember { mutableStateOf<File?>(null) }
    var isDownloadingAudio by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Download audio file when message is first displayed
    LaunchedEffect(message._id, message.audioUrl) {
        if (message.audioUrl != null && localAudioFile == null && !isDownloadingAudio) {
            val fullAudioUrl = if (message.audioUrl!!.startsWith("http")) {
                message.audioUrl
            } else {
                "http://72.61.145.239:9090${message.audioUrl}"
            }
            isDownloadingAudio = true
            Log.d("ChatScreen", "Starting audio download for message ${message._id}")
            val downloadedFile = downloadAudioFile(context, fullAudioUrl)
            if (downloadedFile != null) {
                localAudioFile = downloadedFile
                Log.d("ChatScreen", "Audio downloaded and ready for playback")
            } else {
                Log.e("ChatScreen", "Failed to download audio from: $fullAudioUrl")
                ToastManager.showError("Failed to download audio message")
            }
            isDownloadingAudio = false
        }
    }
    
    // Cleanup MediaPlayer and local file when composable is disposed
    DisposableEffect(message._id) {
        onDispose {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }
                mediaPlayer = null
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error releasing MediaPlayer: ${e.message}")
            }
            
            // Clean up local audio file after a delay to allow playback
            val fileToDelete = localAudioFile
            if (fileToDelete != null) {
                coroutineScope.launch {
                    delay(10000) // Keep file for 10 seconds after disposal to allow playback
                    try {
                        if (fileToDelete.exists()) {
                            fileToDelete.delete()
                            Log.d("ChatScreen", "Cleaned up audio file: ${fileToDelete.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Error deleting audio file: ${e.message}")
                    }
                }
            }
        }
    }
    
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
                
                // Audio if message type is AUDIO
                if (message.messageType == "AUDIO" && message.audioUrl != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isDownloadingAudio && localAudioFile != null) {
                                if (isPlaying) {
                                    // Stop playback
                                    mediaPlayer?.pause()
                                    isPlaying = false
                                } else {
                                    // Start playback
                                    if (mediaPlayer == null && localAudioFile != null) {
                                        val audioFile = localAudioFile!!
                                        if (!audioFile.exists() || audioFile.length() == 0L) {
                                            Log.e("ChatScreen", "Audio file doesn't exist or is empty: ${audioFile.absolutePath}")
                                            ToastManager.showError("Audio file not found")
                                            return@clickable
                                        }
                                        
                                        try {
                                            Log.d("ChatScreen", "Starting playback from: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")
                                            mediaPlayer = MediaPlayer().apply {
                                                setDataSource(audioFile.absolutePath)
                                                
                                                setOnPreparedListener {
                                                    Log.d("ChatScreen", "MediaPlayer prepared, starting playback")
                                                    try {
                                                        start()
                                                        isPlaying = true
                                                        Log.d("ChatScreen", "Playback started, duration: ${duration}ms")
                                                        
                                                        // Update progress
                                                        coroutineScope.launch {
                                                            while (isPlaying && mediaPlayer != null) {
                                                                try {
                                                                    val mp = mediaPlayer
                                                                    if (mp != null && mp.isPlaying) {
                                                                        currentPosition = mp.currentPosition / 1000
                                                                        val duration = mp.duration
                                                                        if (duration > 0) {
                                                                            playbackProgress = mp.currentPosition.toFloat() / duration.toFloat()
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e("ChatScreen", "Error updating progress: ${e.message}")
                                                                }
                                                                delay(100)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("ChatScreen", "Error starting playback: ${e.message}", e)
                                                        ToastManager.showError("Failed to start playback")
                                                        isPlaying = false
                                                    }
                                                }
                                                
                                                setOnCompletionListener {
                                                    Log.d("ChatScreen", "Playback completed")
                                                    isPlaying = false
                                                    playbackProgress = 0f
                                                    currentPosition = 0
                                                    mediaPlayer?.release()
                                                    mediaPlayer = null
                                                }
                                                
                                                setOnErrorListener { _, what, extra ->
                                                    Log.e("ChatScreen", "MediaPlayer error: what=$what, extra=$extra")
                                                    isPlaying = false
                                                    ToastManager.showError("Failed to play audio (error: $what)")
                                                    try {
                                                        mediaPlayer?.release()
                                                    } catch (e: Exception) {
                                                        Log.e("ChatScreen", "Error releasing MediaPlayer: ${e.message}")
                                                    }
                                                    mediaPlayer = null
                                                    true
                                                }
                                                
                                                prepareAsync()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ChatScreen", "Error creating MediaPlayer: ${e.message}", e)
                                            ToastManager.showError("Failed to play audio: ${e.message}")
                                            isPlaying = false
                                            try {
                                                mediaPlayer?.release()
                                            } catch (ex: Exception) {
                                                Log.e("ChatScreen", "Error releasing MediaPlayer: ${ex.message}")
                                            }
                                            mediaPlayer = null
                                        }
                                    } else if (mediaPlayer != null) {
                                        mediaPlayer?.start()
                                        isPlaying = true
                                        coroutineScope.launch {
                                            while (isPlaying && mediaPlayer != null) {
                                                try {
                                                    val mp = mediaPlayer
                                                    if (mp != null && mp.isPlaying) {
                                                        currentPosition = mp.currentPosition / 1000
                                                        playbackProgress = mp.currentPosition.toFloat() / mp.duration.toFloat()
                                                    }
                                                } catch (e: Exception) {
                                                    // Ignore
                                                }
                                                delay(100)
                                            }
                                        }
                                    }
                                }
                            }
                            .padding(4.dp)
                    ) {
                        // Play/Pause button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOwnMessage) Color.White.copy(alpha = 0.2f) 
                                    else Primary.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloadingAudio) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isOwnMessage) Color.White else Primary
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = if (isOwnMessage) Color.White else Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { playbackProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (isOwnMessage) Color.White else Primary,
                                trackColor = if (isOwnMessage) Color.White.copy(alpha = 0.3f) else Primary.copy(alpha = 0.2f)
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            // Duration
                            Text(
                                text = if (isPlaying || currentPosition > 0) {
                                    "%02d:%02d".format(currentPosition / 60, currentPosition % 60)
                                } else {
                                    message.audioDuration?.let { 
                                        "%02d:%02d".format(it / 60, it % 60) 
                                    } ?: "Voice message"
                                },
                                fontSize = 12.sp,
                                color = if (isOwnMessage) Color.White.copy(alpha = 0.8f) else TextSecondary
                            )
                        }
                    }
                }
                
                // Location if message type is LOCATION
                if (message.messageType == "LOCATION" && message.location != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onLocationClick?.invoke(message.location)
                            }
                            .background(
                                if (isOwnMessage) Color.White.copy(alpha = 0.1f)
                                else Color(0xFFF0F0F0)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Location icon and label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isOwnMessage) Color.White.copy(alpha = 0.2f)
                                        else Primary.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = if (isOwnMessage) Color.White else Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = "Live Location",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOwnMessage) Color.White else TextPrimary
                                )
                                Text(
                                    text = "${message.sender.name ?: "User"} shared their location",
                                    fontSize = 12.sp,
                                    color = if (isOwnMessage) Color.White.copy(alpha = 0.8f) else TextSecondary
                                )
                            }
                        }
                        
                        // Coordinates
                        Text(
                            text = "Lat: ${String.format("%.4f", message.location.latitude)}, " +
                                   "Lng: ${String.format("%.4f", message.location.longitude)}",
                            fontSize = 11.sp,
                            color = if (isOwnMessage) Color.White.copy(alpha = 0.7f) else TextHint,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        
                        // View on map button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isOwnMessage) Color.White.copy(alpha = 0.2f)
                                    else Primary.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = if (isOwnMessage) Color.White else Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tap to view on map",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOwnMessage) Color.White else Primary
                            )
                        }
                    }
                    
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

/**
 * Format timestamp to relative time
 */
fun formatTimeAgo(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        
        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            duration.toDays() < 7 -> "${duration.toDays()}d ago"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    .withZone(java.time.ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        ""
    }
}
