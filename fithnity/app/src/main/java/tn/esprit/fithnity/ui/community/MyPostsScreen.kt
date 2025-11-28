package tn.esprit.fithnity.ui.community

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.esprit.fithnity.data.CommunityPostResponse
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.components.ToastManager
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    navController: NavHostController,
    userPreferences: UserPreferences,
    viewModel: CommunityViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val authToken = remember { userPreferences.getAuthToken() }
    val userId = remember { userPreferences.getUserId() }
    val uiState by viewModel.uiState.collectAsState()

    // Load all posts and filter by current user
    LaunchedEffect(Unit) {
        if (authToken != null) {
            viewModel.loadPosts(authToken)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Posts",
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
                is CommunityUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is CommunityUiState.Success -> {
                    // Filter posts to show only current user's posts
                    val myPosts = state.posts.filter { post ->
                        post.user._id == userId
                    }

                    if (myPosts.isEmpty()) {
                        EmptyMyPostsView()
                    } else {
                        MyPostsList(
                            posts = myPosts,
                            authToken = authToken,
                            userId = userId,
                            navController = navController,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is CommunityUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = {
                            if (authToken != null) {
                                viewModel.loadPosts(authToken)
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
private fun MyPostsList(
    posts: List<CommunityPostResponse>,
    authToken: String?,
    userId: String?,
    navController: NavHostController,
    viewModel: CommunityViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(posts) { post ->
            MyPostCard(
                post = post,
                viewModel = viewModel,
                authToken = authToken,
                userId = userId,
                navController = navController,
                onCommentClick = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                }
            )
        }

        // Extra space at bottom
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmptyMyPostsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PostAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextHint
            )
            Text(
                text = "No posts yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = "Share your thoughts with the community!",
                fontSize = 14.sp,
                color = TextHint,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFEF4444)
            )
            Text(
                text = "Error loading posts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextHint,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

@Composable
private fun MyPostCard(
    post: CommunityPostResponse,
    viewModel: CommunityViewModel,
    authToken: String?,
    userId: String?,
    navController: NavHostController,
    onCommentClick: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    var editContent by remember { mutableStateOf(post.content) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }
    var removeImage by remember { mutableStateOf(false) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            removeImage = false
            // Convert URI to File
            coroutineScope.launch {
                selectedImageFile = uriToFile(context, it)
            }
        }
    }
    
    // Use the existing PostCard but with custom header
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable {
                navController.navigate(Screen.PostDetail.createRoute(post._id))
            },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header: User Info with dropdown menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                val profileImageUrl = post.user.photoUrl?.let { url ->
                    if (url.startsWith("http")) url else "http://72.61.145.239:9090$url"
                }
                if (profileImageUrl != null) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.user.name ?: "User",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = post.timeAgo ?: formatTimeAgo(post.createdAt),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // More Options Dropdown
                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(20.dp),
                            tint = TextHint
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showOptionsMenu = false
                                editContent = post.content
                                // Set existing image URI for preview (don't convert to file yet)
                                selectedImageUri = post.imageUrl?.let { 
                                    Uri.parse(if (it.startsWith("http")) it else "http://72.61.145.239:9090$it")
                                }
                                selectedImageFile = null
                                removeImage = false
                                showEditDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Error) },
                            onClick = {
                                showOptionsMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Error)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Post Content
            Text(
                text = post.content,
                fontSize = 15.sp,
                color = TextPrimary,
                lineHeight = 22.sp
            )

            // Post Image
            post.imageUrl?.let { imageUrl ->
                Spacer(Modifier.height(12.dp))
                val fullImageUrl = if (imageUrl.startsWith("http")) {
                    imageUrl
                } else {
                    "http://72.61.145.239:9090$imageUrl"
                }
                AsyncImage(
                    model = fullImageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(12.dp))

            // Voting Section (reuse from PostCard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newVote = if (post.userVote == "up") null else "up"
                            viewModel.votePost(authToken = authToken, postId = post._id, vote = newVote)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upvote",
                            modifier = Modifier.size(20.dp),
                            tint = if (post.userVote == "up") Color(0xFFFF4500) else TextHint
                        )
                    }

                    Text(
                        text = "${post.score}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            val newVote = if (post.userVote == "down") null else "down"
                            viewModel.votePost(authToken = authToken, postId = post._id, vote = newVote)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Downvote",
                            modifier = Modifier.size(20.dp),
                            tint = if (post.userVote == "down") Color(0xFF6A5ACD) else TextHint
                        )
                    }
                }

                Row(
                    modifier = Modifier.clickable { onCommentClick(post._id) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comment",
                        modifier = Modifier.size(20.dp),
                        tint = TextHint
                    )
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 14.sp,
                        color = TextHint
                    )
                }
            }
        }
    }
    
    // Edit Post Dialog
    if (showEditDialog) {
        EditPostDialog(
            post = post,
            content = editContent,
            onContentChange = { editContent = it },
            imageUri = selectedImageUri,
            onImageChange = { uri ->
                selectedImageUri = uri
                removeImage = false
                coroutineScope.launch {
                    uri?.let { selectedImageFile = uriToFile(context, it) }
                }
            },
            onRemoveImage = {
                selectedImageUri = null
                selectedImageFile = null
                removeImage = true
            },
            onDismiss = { showEditDialog = false },
            onSave = {
                coroutineScope.launch {
                    viewModel.updatePost(
                        authToken = authToken,
                        postId = post._id,
                        content = editContent,
                        imageFile = selectedImageFile,
                        removeImage = removeImage
                    )
                    showEditDialog = false
                    ToastManager.showSuccess("Post updated successfully")
                }
            },
            onSelectImage = { galleryLauncher.launch("image/*") }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeletePostDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deletePost(authToken = authToken, postId = post._id)
                showDeleteDialog = false
                ToastManager.showSuccess("Post deleted successfully")
            }
        )
    }
}

@Composable
private fun EditPostDialog(
    post: CommunityPostResponse,
    content: String,
    onContentChange: (String) -> Unit,
    imageUri: Uri?,
    onImageChange: (Uri?) -> Unit,
    onRemoveImage: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onSelectImage: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Post",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    placeholder = { Text("What's on your mind?") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = TextHint.copy(alpha = 0.3f)
                    )
                )
                
                // Image preview
                if (imageUri != null) {
                    Box {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = onRemoveImage,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Image selection button
                if (imageUri == null) {
                    Button(
                        onClick = onSelectImage,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Change Image")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletePostDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Post",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Are you sure you want to delete this post? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Format timestamp to relative time
 */
private fun formatTimeAgo(timestamp: String?): String {
    if (timestamp == null) return ""
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

