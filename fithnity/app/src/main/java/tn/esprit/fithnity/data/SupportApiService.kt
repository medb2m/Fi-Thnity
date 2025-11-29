package tn.esprit.fithnity.data

import retrofit2.http.*

interface SupportApiService {
    /**
     * Chat with support bot
     * POST /api/support/chat
     */
    @POST("/api/support/chat")
    suspend fun chatWithBot(
        @Header("Authorization") bearer: String,
        @Body request: ChatRequest
    ): ApiResponse<ChatResponse>

    /**
     * Create a support ticket
     * POST /api/support/tickets
     */
    @POST("/api/support/tickets")
    suspend fun createTicket(
        @Header("Authorization") bearer: String,
        @Body request: CreateTicketRequest
    ): ApiResponse<SupportTicket>

    /**
     * Get user's tickets
     * GET /api/support/tickets
     */
    @GET("/api/support/tickets")
    suspend fun getUserTickets(
        @Header("Authorization") bearer: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<TicketsListResponse>

    /**
     * Get ticket by ID
     * GET /api/support/tickets/:ticketId
     */
    @GET("/api/support/tickets/{ticketId}")
    suspend fun getTicketById(
        @Header("Authorization") bearer: String,
        @Path("ticketId") ticketId: String
    ): ApiResponse<SupportTicket>

    /**
     * Add message to ticket
     * POST /api/support/tickets/:ticketId/messages
     */
    @POST("/api/support/tickets/{ticketId}/messages")
    suspend fun addMessageToTicket(
        @Header("Authorization") bearer: String,
        @Path("ticketId") ticketId: String,
        @Body request: AddMessageRequest
    ): ApiResponse<SupportTicket>
}

// Request/Response models
data class ChatRequest(
    val messages: List<ChatMessage>,
    val ticketId: String? = null
)

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

data class ChatResponse(
    val message: String,
    val shouldCreateTicket: Boolean? = false,
    val usage: UsageInfo? = null
)

data class UsageInfo(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

data class CreateTicketRequest(
    val subject: String,
    val description: String,
    val category: String? = "other",
    val chatbotConversation: List<ChatMessage>? = null
)

data class AddMessageRequest(
    val content: String
)

data class SupportTicket(
    val _id: String,
    val user: String? = null,
    val subject: String,
    val description: String,
    val status: String, // "open", "in_progress", "resolved", "closed"
    val priority: String, // "low", "medium", "high", "urgent"
    val category: String, // "technical", "account", "payment", "ride_issue", "other"
    val messages: List<SupportMessage>? = null,
    val chatbotConversation: List<ChatMessage>? = null,
    val assignedTo: String? = null,
    val resolvedAt: String? = null,
    val resolvedBy: String? = null,
    val createdAt: String,
    val updatedAt: String
)

data class SupportMessage(
    val sender: String, // "user", "admin", "bot"
    val senderId: String? = null,
    val content: String,
    val isBot: Boolean = false,
    val createdAt: String
)

data class TicketsListResponse(
    val tickets: List<SupportTicket>,
    val pagination: PaginationInfo? = null
)

data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Int
)

