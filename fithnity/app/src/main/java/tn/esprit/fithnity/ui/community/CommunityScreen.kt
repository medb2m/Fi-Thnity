package tn.esprit.fithnity.ui.community

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*

/**
 * Community Screen showing social feed
 */
@Composable
fun CommunityScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val posts = remember { emptyList<Post>() } // TODO: Get from ViewModel

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Text(
                text = "Community",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(16.dp))

            // Posts List
            if (posts.isEmpty()) {
                // Empty State
                EmptyCommunityState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts) { post ->
                        PostCard(post = post) {
                            // TODO: Navigate to post detail
                        }
                    }
                }
            }
        }

        // Floating Action Button for New Post
        FloatingActionButton(
            onClick = {
                navController.navigate(Screen.NewPost.route)
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
            text = "No Posts Yet",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Be the first to share something with the community!",
            fontSize = 15.sp,
            color = TextHint
        )
    }
}

/**
 * Post Card Component
 */
@Composable
private fun PostCard(
    post: Post,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: User Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User Avatar
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

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.userName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = post.time,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                // More Options
                IconButton(onClick = { /* TODO: Show options */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
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

            Spacer(Modifier.height(16.dp))

            // Actions: Like, Comment, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Like
                PostAction(
                    icon = Icons.Default.Favorite,
                    count = post.likes,
                    label = "Like",
                    color = Accent
                )

                // Comment
                PostAction(
                    icon = Icons.Default.Comment,
                    count = post.comments,
                    label = "Comment",
                    color = Primary
                )

                // Share
                PostAction(
                    icon = Icons.Default.Share,
                    count = post.shares,
                    label = "Share",
                    color = Secondary
                )
            }
        }
    }
}

/**
 * Post Action Button Component
 */
@Composable
private fun PostAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    TextButton(
        onClick = { /* TODO: Handle action */ }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (count > 0) count.toString() else label,
            fontSize = 14.sp,
            color = color
        )
    }
}

// Data Model
private data class Post(
    val id: String,
    val userName: String,
    val userAvatar: String? = null,
    val time: String,
    val content: String,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0
)
