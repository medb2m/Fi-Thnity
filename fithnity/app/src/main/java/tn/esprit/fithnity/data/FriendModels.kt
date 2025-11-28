package tn.esprit.fithnity.data

/**
 * Friend request response
 */
data class FriendRequestResponse(
    val success: Boolean,
    val message: String? = null,
    val data: FriendRequest? = null
)

/**
 * Friend request data
 */
data class FriendRequest(
    val _id: String? = null,
    val requester: String? = null,
    val recipient: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val acceptedAt: String? = null
)

/**
 * Friend response (with user info)
 */
data class FriendResponse(
    val _id: String? = null,
    val user: UserInfo,
    val acceptedAt: String? = null,
    val createdAt: String? = null
)

/**
 * Friends list response
 */
data class FriendsListResponse(
    val success: Boolean,
    val data: List<FriendResponse>? = null
)

/**
 * Friend status response
 */
data class FriendStatusResponse(
    val success: Boolean,
    val data: FriendStatus? = null
)

/**
 * Friend status data
 */
data class FriendStatus(
    val status: String, // "NONE", "PENDING", "ACCEPTED", "REJECTED", "SELF"
    val isRequester: Boolean? = null,
    val requestId: String? = null
)

/**
 * Send friend request request
 */
data class SendFriendRequestRequest(
    val recipientId: String
)

