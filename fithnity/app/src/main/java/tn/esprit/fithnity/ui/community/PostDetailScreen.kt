package tn.esprit.fithnity.ui.community

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun PostDetailScreen(
    navController: NavHostController,
    postId: String,
    userPreferences: UserPreferences,
    viewModel: CommunityViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val authToken = remember { userPreferences.getAuthToken() }
    val userId = remember { userPreferences.getUserId() }
    val postDetailState by viewModel.postDetailState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    var editContent by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }
    var removeImage by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            removeImage = false
            coroutineScope.launch {
                selectedImageFile = uriToFile(context, it)
            }
        }
    }
    
    // Load the specific post by ID
    LaunchedEffect(postId) {
        viewModel.loadPostById(authToken, postId)
    }
    
    // Get the post from state
    val post = when (val state = postDetailState) {
        is PostDetailState.Success -> state.post
        else -> null
    }
    
    val isOwnPost = post?.user?._id == userId
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Post",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Primary
                        )
                    }
                },
                actions = {
                    if (isOwnPost && post != null) {
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = TextPrimary
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
                .background(Surface)
        ) {
            when (postDetailState) {
                is PostDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is PostDetailState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                tint = Error
                            )
                            Text(
                                text = (postDetailState as PostDetailState.Error).message,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                is PostDetailState.Success -> {
                    val successPost = (postDetailState as PostDetailState.Success).post
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Post Card
                        item {
                            PostDetailCard(
                                post = successPost,
                                viewModel = viewModel,
                                authToken = authToken
                            )
                        }
                        
                        // Comments Section
                        item {
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Comments",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        
                        // Comments List
                        if (successPost.comments.isNullOrEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments yet. Be the first to comment!",
                                        fontSize = 14.sp,
                                        color = TextHint,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(successPost.comments ?: emptyList()) { comment ->
                                CommentItem(
                                    comment = comment,
                                    currentUserId = userId,
                                    authToken = authToken,
                                    postId = postId,
                                    viewModel = viewModel
                                )
                            }
                        }
                        
                        // Comment Input
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Write a comment...", color = TextHint) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = TextHint.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank() && authToken != null) {
                                            viewModel.addComment(authToken, postId, commentText)
                                            commentText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (commentText.isNotBlank()) Primary else Primary.copy(alpha = 0.3f)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        
                        // Extra space at bottom
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
    
    // Edit Post Dialog
    if (showEditDialog && post != null) {
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
                        postId = postId,
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
                viewModel.deletePost(authToken = authToken, postId = postId)
                showDeleteDialog = false
                ToastManager.showSuccess("Post deleted successfully")
                navController.popBackStack()
            }
        )
    }
}

@Composable
private fun PostDetailCard(
    post: CommunityPostResponse,
    viewModel: CommunityViewModel,
    authToken: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Header: User Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            // Voting Section
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
}

@Composable
private fun CommentItem(
    comment: tn.esprit.fithnity.data.CommentResponse,
    currentUserId: String?,
    authToken: String?,
    postId: String,
    viewModel: CommunityViewModel
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf(comment.content) }
    
    val isOwnComment = comment.user._id == currentUserId
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        val profileImageUrl = comment.user.photoUrl?.let { url ->
            if (url.startsWith("http")) url else "http://72.61.145.239:9090$url"
        }
        if (profileImageUrl != null) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Surface.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comment.user.name ?: "User",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        
                        if (isOwnComment) {
                            Box {
                                IconButton(
                                    onClick = { showOptionsMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        modifier = Modifier.size(18.dp),
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
                                            editContent = comment.content
                                            showEditDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = Error) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = comment.content,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatTimeAgo(comment.createdAt),
                fontSize = 11.sp,
                color = TextHint
            )
        }
    }
    
    // Edit Comment Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Comment") },
            text = {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Edit your comment...", color = TextHint) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = TextHint.copy(alpha = 0.3f)
                    ),
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editContent.isNotBlank() && authToken != null) {
                            viewModel.updateComment(authToken, postId, comment._id ?: "", editContent)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextHint)
                }
            }
        )
    }
    
    // Delete Comment Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Comment") },
            text = { Text("Are you sure you want to delete this comment?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (authToken != null) {
                            viewModel.deleteComment(authToken, postId, comment._id ?: "")
                            showDeleteDialog = false
                        }
                    }
                ) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextHint)
                }
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

