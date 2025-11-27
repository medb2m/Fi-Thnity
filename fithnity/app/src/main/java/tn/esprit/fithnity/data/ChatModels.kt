package tn.esprit.fithnity.data

/**
 * User info for chat
 */
data class ChatUserInfo(
    val _id: String,
    val name: String? = null,
    val photoUrl: String? = null
)

/**
 * Message in a conversation
 */
data class MessageResponse(
    val _id: String,
    val conversation: String,
    val sender: ChatUserInfo,
    val content: String,
    val messageType: String = "TEXT", // TEXT, IMAGE, LOCATION
    val imageUrl: String? = null,
    val location: LocationData? = null,
    val status: String = "SENT", // SENT, DELIVERED, READ
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val timeAgo: String? = null
)

/**
 * Location data for location messages
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

/**
 * Last message info in conversation list
 */
data class LastMessageInfo(
    val _id: String,
    val content: String,
    val sender: String,
    val createdAt: String? = null,
    val messageType: String = "TEXT"
)

/**
 * Conversation (chat) response
 */
data class ConversationResponse(
    val _id: String,
    val otherUser: ChatUserInfo,
    val lastMessage: LastMessageInfo? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * List of conversations response
 */
data class ConversationsListResponse(
    val data: List<ConversationResponse>,
    val pagination: PaginationInfo? = null
)

/**
 * Messages list response
 */
data class MessagesListResponse(
    val data: List<MessageResponse>,
    val pagination: PaginationInfo? = null
)

/**
 * Create conversation request
 */
data class CreateConversationRequest(
    val otherUserId: String
)

/**
 * Send message request
 */
data class SendMessageRequest(
    val content: String,
    val messageType: String = "TEXT",
    val imageUrl: String? = null,
    val location: LocationData? = null
)

/**
 * User search result for starting new chats
 */
data class UserSearchResponse(
    val _id: String,
    val name: String? = null,
    val photoUrl: String? = null
)

/**
 * Users list response
 */
data class UsersListResponse(
    val data: List<UserSearchResponse>,
    val pagination: PaginationInfo? = null
)

/**
 * Unread conversation count response
 */
data class UnreadConversationCountResponse(
    val unreadConversationCount: Int
)

