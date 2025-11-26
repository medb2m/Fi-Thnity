package tn.esprit.fithnity.ui.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tn.esprit.fithnity.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * New Post Screen - Create a new community post
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: CommunityViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
                // Post created successfully, navigate back
                navController.popBackStack()
            }
            is CreatePostUiState.Error -> {
                // Error will be shown in UI
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (postContent.isNotBlank()) {
                                viewModel.createPost(
                                    content = postContent,
                                    postType = selectedPostType,
                                    imageFile = imageFile
                                )
                            }
                        },
                        enabled = postContent.isNotBlank() && createPostState !is CreatePostUiState.Loading
                    ) {
                        if (createPostState is CreatePostUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Post Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostTypeChip(
                    label = "General",
                    selected = selectedPostType == "GENERAL",
                    onClick = { selectedPostType = "GENERAL" }
                )
                PostTypeChip(
                    label = "Accident",
                    selected = selectedPostType == "ACCIDENT",
                    onClick = { selectedPostType = "ACCIDENT" }
                )
                PostTypeChip(
                    label = "Delay",
                    selected = selectedPostType == "DELAY",
                    onClick = { selectedPostType = "DELAY" }
                )
                PostTypeChip(
                    label = "Road Closure",
                    selected = selectedPostType == "ROAD_CLOSURE",
                    onClick = { selectedPostType = "ROAD_CLOSURE" }
                )
            }
            
            // Content Input
            OutlinedTextField(
                value = postContent,
                onValueChange = { 
                    if (it.length <= 500) {
                        postContent = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("What's on your mind?") },
                maxLines = 15,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            
            // Character count
            Text(
                text = "${postContent.length}/500",
                fontSize = 12.sp,
                color = TextHint,
                modifier = Modifier.align(Alignment.End)
            )
            
            // Selected Image Preview
            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Add Image Button
            if (selectedImageUri == null) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, "Add Image")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Image")
                }
            }
            
            // Error Message
            if (createPostState is CreatePostUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Text(
                        text = (createPostState as CreatePostUiState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

/**
 * Post Type Chip Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary,
            selectedLabelColor = Color.White,
            containerColor = Surface,
            labelColor = TextPrimary
        )
    )
}

/**
 * Convert URI to File
 */
private fun getFileFromUri(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.let {
            val file = File(context.cacheDir, "post_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            
            it.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            file
        }
    } catch (e: Exception) {
        android.util.Log.e("NewPostScreen", "Error converting URI to File", e)
        null
    }
}

