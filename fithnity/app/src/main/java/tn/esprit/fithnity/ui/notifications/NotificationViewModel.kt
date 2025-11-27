package tn.esprit.fithnity.ui.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.*

sealed class NotificationsUiState {
    object Idle : NotificationsUiState()
    object Loading : NotificationsUiState()
    data class Success(val notifications: List<NotificationResponse>, val unreadCount: Int) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationViewModel : ViewModel() {
    companion object {
        private const val TAG = "NotificationViewModel"
    }

    private val api = NetworkModule.notificationApi

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Idle)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /**
     * Load notifications
     */
    fun loadNotifications(authToken: String?, page: Int = 1, unreadOnly: Boolean = false) = viewModelScope.launch {
        Log.d(TAG, "loadNotifications: Loading notifications page $page")
        _uiState.value = NotificationsUiState.Loading

        try {
            val token = authToken
            if (token == null) {
                _uiState.value = NotificationsUiState.Error("Not authenticated")
                return@launch
            }

            val response = api.getNotifications(
                bearer = "Bearer $token",
                page = page,
                limit = 20,
                unreadOnly = unreadOnly
            )

            Log.d(TAG, "loadNotifications: Loaded ${response.data.size} notifications, unread: ${response.unreadCount}")
            _unreadCount.value = response.unreadCount
            _uiState.value = NotificationsUiState.Success(response.data, response.unreadCount)
        } catch (e: Exception) {
            Log.e(TAG, "loadNotifications: Exception occurred", e)
            _uiState.value = NotificationsUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Refresh unread count
     */
    fun refreshUnreadCount(authToken: String?) = viewModelScope.launch {
        try {
            val token = authToken ?: return@launch

            val response = api.getUnreadCount(bearer = "Bearer $token")
            if (response.success && response.data != null) {
                _unreadCount.value = response.data.unreadCount
                Log.d(TAG, "refreshUnreadCount: Unread count = ${response.data.unreadCount}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshUnreadCount: Exception occurred", e)
        }
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(authToken: String?, notificationId: String) = viewModelScope.launch {
        try {
            val token = authToken ?: return@launch

            val response = api.markAsRead(
                bearer = "Bearer $token",
                notificationId = notificationId
            )

            if (response.success) {
                Log.d(TAG, "markAsRead: Notification marked as read")
                // Refresh unread count
                refreshUnreadCount(authToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "markAsRead: Exception occurred", e)
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead(authToken: String?) = viewModelScope.launch {
        try {
            val token = authToken ?: return@launch

            val response = api.markAllAsRead(bearer = "Bearer $token")

            if (response.success) {
                Log.d(TAG, "markAllAsRead: All notifications marked as read")
                _unreadCount.value = 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "markAllAsRead: Exception occurred", e)
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(authToken: String?, notificationId: String) = viewModelScope.launch {
        try {
            val token = authToken ?: return@launch

            val response = api.deleteNotification(
                bearer = "Bearer $token",
                notificationId = notificationId
            )

            if (response.success) {
                Log.d(TAG, "deleteNotification: Notification deleted")
                // Refresh notifications
                loadNotifications(authToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteNotification: Exception occurred", e)
        }
    }
}

