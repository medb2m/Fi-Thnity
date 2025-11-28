package tn.esprit.fithnity.data

import okhttp3.MultipartBody
import retrofit2.http.*

interface ChatApiService {
    /**
     * Get all conversations for current user
     * GET /api/chat/conversations?page=1&limit=20
     */
    @GET("/api/chat/conversations")
    suspend fun getConversations(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ConversationsListResponse

    /**
     * Get or create conversation with another user
     * POST /api/chat/conversations
     */
    @POST("/api/chat/conversations")
    suspend fun getOrCreateConversation(
        @Header("Authorization") bearer: String,
        @Body request: CreateConversationRequest
    ): ApiResponse<ConversationResponse>

    /**
     * Get messages in a conversation
     * GET /api/chat/conversations/:conversationId/messages?page=1&limit=50
     */
    @GET("/api/chat/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Header("Authorization") bearer: String,
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): MessagesListResponse

    /**
     * Send a message
     * POST /api/chat/conversations/:conversationId/messages
     */
    @POST("/api/chat/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") bearer: String,
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): ApiResponse<MessageResponse>

    /**
     * Mark messages as read
     * PUT /api/chat/conversations/:conversationId/read
     */
    @PUT("/api/chat/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Header("Authorization") bearer: String,
        @Path("conversationId") conversationId: String
    ): ApiResponse<Unit>

    /**
     * Delete conversation
     * DELETE /api/chat/conversations/:conversationId
     */
    @DELETE("/api/chat/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Header("Authorization") bearer: String,
        @Path("conversationId") conversationId: String
    ): ApiResponse<Unit>

    /**
     * Get users list (for starting new chats)
     * GET /api/chat/users?search=&page=1&limit=20
     */
    @GET("/api/chat/users")
    suspend fun getUsers(
        @Header("Authorization") bearer: String,
        @Query("search") search: String = "",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): UsersListResponse

    /**
     * Get unread conversation count
     * GET /api/chat/conversations/unread-count
     */
    @GET("/api/chat/conversations/unread-count")
    suspend fun getUnreadConversationCount(
        @Header("Authorization") bearer: String
    ): ApiResponse<UnreadConversationCountResponse>

    /**
     * Upload chat image
     * POST /api/chat/upload-image
     */
    @Multipart
    @POST("/api/chat/upload-image")
    suspend fun uploadChatImage(
        @Header("Authorization") bearer: String,
        @Part image: MultipartBody.Part
    ): ApiResponse<ChatImageUploadResponse>

    /**
     * Upload chat audio
     * POST /api/chat/upload-audio
     */
    @Multipart
    @POST("/api/chat/upload-audio")
    suspend fun uploadChatAudio(
        @Header("Authorization") bearer: String,
        @Part audio: MultipartBody.Part
    ): ApiResponse<ChatAudioUploadResponse>
}

