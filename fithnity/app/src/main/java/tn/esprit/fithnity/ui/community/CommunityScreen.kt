package tn.esprit.fithnity.ui.community

import androidx.compose.foundation.Image
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import tn.esprit.fithnity.data.CommunityPostResponse
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.navigation.SearchState
import tn.esprit.fithnity.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Community Screen showing social feed (Facebook-style)
 */
@Composable
fun CommunityScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences,
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Search query from global state
    var searchQuery by remember { mutableStateOf(SearchState.searchQuery) }
    
    // Listen to search state changes
    LaunchedEffect(Unit) {
        SearchState.setSearchHandler { query ->
            searchQuery = query
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            SearchState.clearSearchHandler()
        }
    }
    val authToken = userPreferences.getAuthToken()
    
    // State for showing new post dialog
    var showNewPostDialog by remember { mutableStateOf(false) }

    // Load posts on first composition
    LaunchedEffect(Unit) {
        viewModel.loadPosts(authToken = authToken, sort = "score")
    }
    
    // Reload posts when dialog closes (in case a post was created)
    LaunchedEffect(showNewPostDialog) {
        if (!showNewPostDialog) {
            viewModel.loadPosts(authToken = authToken, sort = "score")
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is CommunityUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CommunityUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error loading posts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadPosts(sort = "score") }) {
                        Text("Retry")
                    }
                }
            }
            is CommunityUiState.Success -> {
                // Filter posts by search query
                val filteredPosts = if (searchQuery.isBlank()) {
                    state.posts
                } else {
                    val queryLower = searchQuery.lowercase()
                    state.posts.filter { post ->
                        post.content.lowercase().contains(queryLower) ||
                        (post.user.name?.lowercase()?.contains(queryLower) == true)
                    }
                }
                
                if (filteredPosts.isEmpty()) {
                    EmptyCommunityState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = UiConstants.ContentBottomPadding + 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPosts) { post ->
                            PostCard(
                                post = post,
                                viewModel = viewModel,
                                authToken = authToken,
                                onCommentClick = { postId ->
                                    // TODO: Navigate to comments screen or show dialog
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button for New Post
        FloatingActionButton(
            onClick = {
                showNewPostDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Post"
            )
        }
        
        // New Post Dialog
        if (showNewPostDialog) {
            NewPostDialog(
                onDismiss = { showNewPostDialog = false },
                onPostCreated = {
                    // Post was created, dialog will close automatically
                },
                userPreferences = userPreferences,
                viewModel = viewModel
            )
        }
    }
}

/**
 * Empty state when no posts available
 */
@Composable
private fun EmptyCommunityState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = TextHint
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "No posts yet",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Be the first to share something!",
            fontSize = 15.sp,
            color = TextHint
        )
    }
}

/**
 * Facebook-style Post Card Component
 */
@Composable
private fun PostCard(
    post: CommunityPostResponse,
    viewModel: CommunityViewModel,
    authToken: String?,
    onCommentClick: (String) -> Unit
) {
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
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
            // Header: User Info (Facebook-style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar with profile picture
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

                // More Options
                IconButton(onClick = { /* TODO: Show options */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        modifier = Modifier.size(20.dp),
                        tint = TextHint
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

            // Post Image (if available)
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

            // Reddit-style Voting Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Upvote/Downvote buttons (Reddit-style)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Upvote button
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

                    // Score display
                    Text(
                        text = "${post.score}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Downvote button
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

                // Right: Comment button
                Row(
                    modifier = Modifier.clickable { showComments = !showComments },
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

            // Comments Section
            if (showComments) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                // Existing comments
                post.comments?.forEach { comment ->
                    CommentItem(comment = comment)
                    Spacer(Modifier.height(8.dp))
                }

                // Add comment input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a comment...", fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(authToken = authToken, postId = post._id, content = commentText)
                                commentText = ""
                            }
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send comment",
                            tint = if (commentText.isNotBlank()) Primary else TextHint
                        )
                    }
                }
            }
        }
    }
}

/**
 * Comment Item Component
 */
@Composable
private fun CommentItem(comment: tn.esprit.fithnity.data.CommentResponse) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Commenter avatar
        val commenterImageUrl = comment.user.photoUrl?.let { url ->
            if (url.startsWith("http")) url else "http://72.61.145.239:9090$url"
        }
        if (commenterImageUrl != null) {
            AsyncImage(
                model = commenterImageUrl,
                contentDescription = "Commenter avatar",
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
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFF0F2F5), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = comment.user.name ?: "User",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
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
}

/**
 * Format time ago string
 */
private fun formatTimeAgo(dateString: String?): String {
    if (dateString == null) return "Just now"
    
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(dateString)
        val now = Date()
        val diff = now.time - (date?.time ?: 0)
        
        when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val displayFormat = SimpleDateFormat("MMM d", Locale.US)
                displayFormat.format(date ?: now)
            }
        }
    } catch (e: Exception) {
        "Just now"
    }
}
