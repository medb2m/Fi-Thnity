package tn.esprit.fithnity.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tn.esprit.fithnity.ui.theme.*

enum class AlertType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

/**
 * Custom Alert Banner with auto-dismiss and manual close
 *
 * @param message The message to display
 * @param type The type of alert (INFO, WARNING, ERROR, SUCCESS)
 * @param isVisible Whether the alert is visible
 * @param onDismiss Callback when alert is dismissed
 * @param autoDismissMillis Time in milliseconds before auto-dismiss (default 2000)
 */
@Composable
fun AlertBanner(
    message: String,
    type: AlertType = AlertType.INFO,
    isVisible: Boolean = true,
    onDismiss: () -> Unit = {},
    autoDismissMillis: Long = 2000L,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(isVisible) }

    // Get the color for this alert type
    val alertColor = when (type) {
        AlertType.INFO -> Primary
        AlertType.WARNING -> Secondary
        AlertType.ERROR -> Error
        AlertType.SUCCESS -> Success
    }

    // Auto-dismiss after specified time
    LaunchedEffect(visible) {
        if (visible) {
            delay(autoDismissMillis)
            visible = false
            onDismiss()
        }
    }

    // Update visibility when isVisible prop changes
    LaunchedEffect(isVisible) {
        visible = isVisible
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Main alert container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = alertColor.copy(alpha = 0.5f),
                        ambientColor = alertColor.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        color = alertColor.copy(alpha = 0.12f)
                    )
                    .border(
                        width = 2.dp,
                        color = alertColor.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Icon + Text
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (type) {
                                AlertType.INFO -> Icons.Default.Info
                                AlertType.WARNING -> Icons.Default.Warning
                                AlertType.ERROR -> Icons.Default.Warning
                                AlertType.SUCCESS -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = alertColor,
                            modifier = Modifier.size(26.dp)
                        )

                        Spacer(Modifier.width(14.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = alertColor
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Right side: Close button (custom clickable without Material components)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable(
                                onClick = {
                                    visible = false
                                    onDismiss()
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = alertColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Alert Banner Host - Place this at the top of your screen to show alerts
 */
@Composable
fun AlertBannerHost(
    alertMessage: String?,
    alertType: AlertType = AlertType.INFO,
    onDismiss: () -> Unit = {}
) {
    if (alertMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            AlertBanner(
                message = alertMessage,
                type = alertType,
                isVisible = true,
                onDismiss = onDismiss
            )
        }
    }
}
