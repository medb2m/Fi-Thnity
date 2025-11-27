package tn.esprit.fithnity.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tn.esprit.fithnity.ui.theme.*

/**
 * Toast message types
 */
enum class ToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    NOTIFICATION
}

/**
 * Toast data class
 */
data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val duration: Long = 3000L, // Auto-dismiss duration in milliseconds
    val actionLabel: String? = null, // Optional action button label
    val onAction: (() -> Unit)? = null // Optional action callback
)

/**
 * Global Toast State Manager
 */
object ToastManager {
    private val _toastState = mutableStateOf<ToastMessage?>(null)
    val toastState: State<ToastMessage?> = _toastState

    fun showToast(
        message: String,
        type: ToastType = ToastType.INFO,
        duration: Long = 3000L,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _toastState.value = ToastMessage(
            message = message,
            type = type,
            duration = duration,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun dismissToast() {
        _toastState.value = null
    }

    // Convenience methods
    fun showSuccess(message: String, duration: Long = 3000L) {
        showToast(message, ToastType.SUCCESS, duration)
    }

    fun showError(message: String, duration: Long = 4000L) {
        showToast(message, ToastType.ERROR, duration)
    }

    fun showWarning(message: String, duration: Long = 3500L) {
        showToast(message, ToastType.WARNING, duration)
    }

    fun showInfo(message: String, duration: Long = 3000L) {
        showToast(message, ToastType.INFO, duration)
    }

    fun showNotification(
        message: String,
        duration: Long = 5000L,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        showToast(message, ToastType.NOTIFICATION, duration, actionLabel, onAction)
    }
}

/**
 * Modern Toast Component - Slides from top
 */
@Composable
fun Toast(
    toast: ToastMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }

    // Auto-dismiss
    LaunchedEffect(toast) {
        delay(toast.duration)
        visible = false
        onDismiss()
    }

    // Get colors and icon based on type
    val backgroundColor: Color
    val textColor: Color
    val iconColor: Color
    val icon: ImageVector
    
    when (toast.type) {
        ToastType.SUCCESS -> {
            backgroundColor = Color(0xFF10B981) // Green
            textColor = Color.White
            iconColor = Color.White
            icon = Icons.Default.CheckCircle
        }
        ToastType.ERROR -> {
            backgroundColor = Color(0xFFEF4444) // Red
            textColor = Color.White
            iconColor = Color.White
            icon = Icons.Default.Error
        }
        ToastType.WARNING -> {
            backgroundColor = Color(0xFFF59E0B) // Orange
            textColor = Color.White
            iconColor = Color.White
            icon = Icons.Default.Warning
        }
        ToastType.INFO -> {
            backgroundColor = Primary // Blue
            textColor = Color.White
            iconColor = Color.White
            icon = Icons.Default.Info
        }
        ToastType.NOTIFICATION -> {
            backgroundColor = Color(0xFF6366F1) // Indigo
            textColor = Color.White
            iconColor = Color.White
            icon = Icons.Default.Notifications
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(400)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon + Message
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = toast.message,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                }

                // Action Button (if provided)
                toast.actionLabel?.let { label ->
                    Spacer(Modifier.width(12.dp))
                    TextButton(
                        onClick = {
                            toast.onAction?.invoke()
                            visible = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = textColor
                        ),
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Toast Host - Place this at the top of your screen
 * Observes ToastManager state and displays toasts
 */
@Composable
fun ToastHost(
    modifier: Modifier = Modifier
) {
    val toastState by ToastManager.toastState

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        toastState?.let { toast ->
            Toast(
                toast = toast,
                onDismiss = { ToastManager.dismissToast() }
            )
        }
    }
}

