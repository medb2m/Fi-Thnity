package tn.esprit.fithnity.ui.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import tn.esprit.fithnity.ui.theme.*

/**
 * Chat User Profile Screen - Messenger-style user profile preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatUserProfileScreen(
    navController: NavHostController,
    userId: String,
    userName: String,
    userPhoto: String?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.1f),
                        Surface,
                        Surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Large profile picture
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Primary, Primary.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (userPhoto != null && userPhoto.isNotEmpty() && userPhoto != "none") {
                    val fullPhotoUrl = if (userPhoto.startsWith("http")) {
                        userPhoto
                    } else {
                        "http://72.61.145.239:9090$userPhoto"
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
                        modifier = Modifier.size(80.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // User name
            Text(
                text = userName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status indicator (online/offline - placeholder for now)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)) // Green for online
                )
                Text(
                    text = "Active now",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionCircleButton(
                    icon = Icons.Default.PersonAdd,
                    label = "Add Friend",
                    onClick = { /* TODO: Implement add friend */ }
                )
                
                ActionCircleButton(
                    icon = Icons.Default.DirectionsCar,
                    label = "Add to Ride",
                    onClick = { /* TODO: Implement add to ride */ }
                )
                
                ActionCircleButton(
                    icon = Icons.Default.Notifications,
                    label = "Mute",
                    onClick = { /* TODO: Implement mute */ }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Options section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    ProfileOptionItem(
                        icon = Icons.Default.Image,
                        title = "Media, Links & Docs",
                        subtitle = "View shared media",
                        onClick = { /* TODO: Implement shared media */ }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = DividerColor
                    )
                    
                    ProfileOptionItem(
                        icon = Icons.Default.Search,
                        title = "Search in Conversation",
                        subtitle = "Find messages",
                        onClick = { /* TODO: Implement search */ }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = DividerColor
                    )
                    
                    ProfileOptionItem(
                        icon = Icons.Default.Palette,
                        title = "Customize Chat",
                        subtitle = "Themes, emoji, nicknames",
                        onClick = { /* TODO: Implement customize */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy & Support section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    ProfileOptionItem(
                        icon = Icons.Default.Block,
                        title = "Block",
                        subtitle = "Block this user",
                        iconTint = Error,
                        onClick = { /* TODO: Implement block */ }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = DividerColor
                    )
                    
                    ProfileOptionItem(
                        icon = Icons.Default.Report,
                        title = "Report",
                        subtitle = "Report a problem",
                        iconTint = Error,
                        onClick = { /* TODO: Implement report */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ActionCircleButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Primary.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = Primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

