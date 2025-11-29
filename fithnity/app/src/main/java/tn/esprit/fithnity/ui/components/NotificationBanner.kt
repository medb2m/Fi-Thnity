package tn.esprit.fithnity.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tn.esprit.fithnity.data.NotificationResponse
import tn.esprit.fithnity.ui.theme.Primary
import tn.esprit.fithnity.ui.theme.Surface

/**
 * In-app notification banner that appears at the top of the screen
 * Shows real-time notifications while the user is using the app
 */
@Composable
fun NotificationBanner(
    notification: NotificationResponse?,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after 5 seconds
    LaunchedEffect(notification) {
        if (notification != null) {
            delay(5000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        notification?.let { notif ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clickable {
                        onClick()
                        onDismiss()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon based on notification type
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = when (notif.type) {
                                    "MESSAGE" -> Primary.copy(alpha = 0.1f)
                                    "COMMENT" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    "PUBLIC_TRANSPORT_SEARCH" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                    else -> Color.Gray.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (notif.type) {
                                "MESSAGE" -> Icons.Default.Email
                                "COMMENT" -> Icons.Default.Comment
                                "PUBLIC_TRANSPORT_SEARCH" -> Icons.Default.DirectionsBus
                                "LIKE" -> Icons.Default.ThumbUp
                                else -> Icons.Default.Notifications
                            },
                            contentDescription = null,
                            tint = when (notif.type) {
                                "MESSAGE" -> Primary
                                "COMMENT" -> Color(0xFF4CAF50)
                                "PUBLIC_TRANSPORT_SEARCH" -> Color(0xFFFF9800)
                                else -> Color.Gray
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Title and message
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = notif.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        Text(
                            text = notif.message,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * State holder for in-app notifications
 */
class InAppNotificationState {
    private val _currentNotification = mutableStateOf<NotificationResponse?>(null)
    val currentNotification: State<NotificationResponse?> = _currentNotification

    fun showNotification(notification: NotificationResponse) {
        android.util.Log.d("NotificationBanner", "Showing notification: ${notification.title}")
        _currentNotification.value = notification
    }

    fun dismissNotification() {
        android.util.Log.d("NotificationBanner", "Dismissing notification")
        _currentNotification.value = null
    }
}

/**
 * Remember in-app notification state
 */
@Composable
fun rememberInAppNotificationState(): InAppNotificationState {
    return remember { InAppNotificationState() }
}

