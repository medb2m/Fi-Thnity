package tn.esprit.fithnity.data

import com.google.gson.annotations.SerializedName

/**
 * Community post response from backend
 */
data class CommunityPostResponse(
    val _id: String,
    val user: UserInfo,
    val firebaseUid: String,
    val content: String,
    val postType: String? = null,
    val imageUrl: String? = null,
    val upvotes: List<String>? = null,
    val downvotes: List<String>? = null,
    val score: Int = 0,
    val comments: List<CommentResponse>? = null,
    val commentsCount: Int = 0,
    val userVote: String? = null, // "up", "down", or null
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val timeAgo: String? = null
)

/**
 * List of community posts response
 */
data class CommunityPostsListResponse(
    val data: List<CommunityPostResponse>,
    val pagination: PaginationInfo? = null
)

/**
 * Pagination information
 */
data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Int,
    val pages: Int
)

/**
 * Comment response
 */
data class CommentResponse(
    val _id: String? = null,
    val user: UserInfo,
    val content: String,
    val createdAt: String? = null
)

/**
 * Vote request
 */
data class VoteRequest(
    val vote: String? // "up", "down", or null to remove vote
)

/**
 * Vote response
 */
data class VoteResponse(
    val score: Int,
    val upvotes: Int,
    val downvotes: Int,
    val userVote: String? // "up", "down", or null
)

/**
 * Comment request
 */
data class CommentRequest(
    val content: String
)

