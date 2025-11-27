package tn.esprit.fithnity.data

import retrofit2.http.*

interface NotificationApiService {
    /**
     * Get all notifications for current user
     * GET /api/notifications?page=1&limit=20&unreadOnly=false
     */
    @GET("/api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("unreadOnly") unreadOnly: Boolean = false
    ): NotificationsListResponse

    /**
     * Get unread notification count
     * GET /api/notifications/unread-count
     */
    @GET("/api/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("Authorization") bearer: String
    ): ApiResponse<UnreadCountResponse>

    /**
     * Mark notification as read
     * PUT /api/notifications/:notificationId/read
     */
    @PUT("/api/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Header("Authorization") bearer: String,
        @Path("notificationId") notificationId: String
    ): ApiResponse<Unit>

    /**
     * Mark all notifications as read
     * PUT /api/notifications/read-all
     */
    @PUT("/api/notifications/read-all")
    suspend fun markAllAsRead(
        @Header("Authorization") bearer: String
    ): ApiResponse<Unit>

    /**
     * Delete a notification
     * DELETE /api/notifications/:notificationId
     */
    @DELETE("/api/notifications/{notificationId}")
    suspend fun deleteNotification(
        @Header("Authorization") bearer: String,
        @Path("notificationId") notificationId: String
    ): ApiResponse<Unit>
}

