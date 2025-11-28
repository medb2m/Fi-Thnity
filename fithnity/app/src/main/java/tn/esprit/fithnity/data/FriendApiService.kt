package tn.esprit.fithnity.data

import retrofit2.http.*

interface FriendApiService {
    /**
     * Send a friend request
     * POST /api/friends/request
     */
    @POST("/api/friends/request")
    suspend fun sendFriendRequest(
        @Header("Authorization") bearer: String,
        @Body request: SendFriendRequestRequest
    ): FriendRequestResponse

    /**
     * Accept a friend request
     * PUT /api/friends/accept/:requestId
     */
    @PUT("/api/friends/accept/{requestId}")
    suspend fun acceptFriendRequest(
        @Header("Authorization") bearer: String,
        @Path("requestId") requestId: String
    ): FriendRequestResponse

    /**
     * Reject a friend request
     * PUT /api/friends/reject/:requestId
     */
    @PUT("/api/friends/reject/{requestId}")
    suspend fun rejectFriendRequest(
        @Header("Authorization") bearer: String,
        @Path("requestId") requestId: String
    ): FriendRequestResponse

    /**
     * Get all friends
     * GET /api/friends
     */
    @GET("/api/friends")
    suspend fun getFriends(
        @Header("Authorization") bearer: String
    ): FriendsListResponse

    /**
     * Get pending invitations
     * GET /api/friends/invitations
     */
    @GET("/api/friends/invitations")
    suspend fun getInvitations(
        @Header("Authorization") bearer: String
    ): FriendsListResponse

    /**
     * Get friend status with a specific user
     * GET /api/friends/status/:userId
     */
    @GET("/api/friends/status/{userId}")
    suspend fun getFriendStatus(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String
    ): FriendStatusResponse
}

