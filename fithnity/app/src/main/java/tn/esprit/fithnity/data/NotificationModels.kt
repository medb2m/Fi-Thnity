package tn.esprit.fithnity.data

/**
 * Notification response
 */
data class NotificationResponse(
    val _id: String,
    val user: String,
    val type: String, // MESSAGE, RIDE_REQUEST, RIDE_ACCEPTED, COMMENT, LIKE, SYSTEM
    val title: String,
    val message: String,
    val data: Map<String, Any>? = null,
    val read: Boolean = false,
    val readAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val timeAgo: String? = null
)

/**
 * Notifications list response
 */
data class NotificationsListResponse(
    val data: List<NotificationResponse>,
    val pagination: PaginationInfo? = null,
    val unreadCount: Int = 0
)

/**
 * Unread count response
 */
data class UnreadCountResponse(
    val unreadCount: Int
)

