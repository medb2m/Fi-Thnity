package tn.esprit.fithnity.ui.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * New Post Dialog - Create a new community post as a popup dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostDialog(
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit,
    userPreferences: UserPreferences,
    viewModel: CommunityViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authToken = userPreferences.getAuthToken()
    
    var postContent by remember { mutableStateOf("") }
    var selectedPostType by remember { mutableStateOf("GENERAL") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    
    val createPostState by viewModel.createPostState.collectAsState()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Convert URI to File
            scope.launch {
                imageFile = getFileFromUri(context, it)
            }
        }
    }
    
    // Handle create post result
    LaunchedEffect(createPostState) {
        when (val state = createPostState) {
            is CreatePostUiState.Success -> {
                // Post created successfully, close dialog and refresh
                onPostCreated()
                onDismiss()
                // Reset form
                postContent = ""
                selectedImageUri = null
                imageFile = null
                selectedPostType = "GENERAL"
                viewModel.resetCreatePostState()
            }
            is CreatePostUiState.Error -> {
                // Error will be shown in UI
            }
            else -> {}
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Primary,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Create Post",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("Cancel", fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                if (postContent.isNotBlank()) {
                                    viewModel.createPost(
                                        authToken = authToken,
                                        content = postContent,
                                        postType = selectedPostType,
                                        imageFile = imageFile
                                    )
                                }
                            },
                            enabled = postContent.isNotBlank() && createPostState !is CreatePostUiState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Primary,
                                disabledContainerColor = Color.White.copy(alpha = 0.5f),
                                disabledContentColor = Primary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (createPostState is CreatePostUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary
                                )
                            } else {
                                Text("Post", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Post Type Selector - Compact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PostTypeChip(
                            label = "General",
                            selected = selectedPostType == "GENERAL",
                            onClick = { selectedPostType = "GENERAL" },
                            modifier = Modifier.weight(1f)
                        )
                        PostTypeChip(
                            label = "Accident",
                            selected = selectedPostType == "ACCIDENT",
                            onClick = { selectedPostType = "ACCIDENT" },
                            modifier = Modifier.weight(1f)
                        )
                        PostTypeChip(
                            label = "Delay",
                            selected = selectedPostType == "DELAY",
                            onClick = { selectedPostType = "DELAY" },
                            modifier = Modifier.weight(1f)
                        )
                        PostTypeChip(
                            label = "Closure",
                            selected = selectedPostType == "ROAD_CLOSURE",
                            onClick = { selectedPostType = "ROAD_CLOSURE" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Content Input - Always visible, fixed height
                    OutlinedTextField(
                        value = postContent,
                        onValueChange = { 
                            if (it.length <= 500) {
                                postContent = it
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        placeholder = { 
                            Text(
                                "What's on your mind?",
                                color = TextHint,
                                fontSize = 14.sp
                            ) 
                        },
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Character count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${postContent.length}/500",
                            fontSize = 11.sp,
                            color = TextHint,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        
                        // Add Image Button - Compact
                        if (selectedImageUri == null) {
                            TextButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Primary
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Add Image",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Add Image", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    // Selected Image Preview - Compact
                    selectedImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Remove image button
                            IconButton(
                                onClick = {
                                    selectedImageUri = null
                                    imageFile = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Error Message
                    if (createPostState is CreatePostUiState.Error) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = (createPostState as CreatePostUiState.Error).message,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Post Type Chip Component - Compact and responsive
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            Text(
                label, 
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        modifier = modifier.height(32.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary,
            selectedLabelColor = Color.White,
            containerColor = SurfaceVariant,
            labelColor = if (selected) Color.White else TextSecondary
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

/**
 * Convert URI to File
 */
private fun getFileFromUri(context: android.content.Context, uri: Uri): File? {
    return try {
        android.util.Log.d("NewPostDialog", "Converting URI to File: $uri")
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.let {
            val file = File(context.cacheDir, "post_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            
            val bytesCopied = it.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            android.util.Log.d("NewPostDialog", "File created: ${file.absolutePath}, size: ${file.length()} bytes ($bytesCopied bytes copied)")
            file
        }
    } catch (e: Exception) {
        android.util.Log.e("NewPostDialog", "Error converting URI to File", e)
        null
    }
}
