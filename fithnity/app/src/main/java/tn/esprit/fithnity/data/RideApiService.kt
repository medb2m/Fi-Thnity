package tn.esprit.fithnity.data

import retrofit2.http.*

interface RideApiService {
    /**
     * Create a new ride (request or offer)
     * POST /api/rides
     */
    @POST("/api/rides")
    suspend fun createRide(
        @Header("Authorization") bearer: String,
        @Body request: CreateRideRequest
    ): ApiResponse<RideResponse>

    /**
     * Get all active rides with filtering
     * GET /api/rides?rideType=REQUEST&transportType=TAXI&page=1&limit=20
     */
    @GET("/api/rides")
    suspend fun getRides(
        @Query("rideType") rideType: String? = null,
        @Query("transportType") transportType: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<RidesListResponse>

    /**
     * Get ride by ID
     * GET /api/rides/:rideId
     */
    @GET("/api/rides/{rideId}")
    suspend fun getRideById(
        @Path("rideId") rideId: String
    ): ApiResponse<RideResponse>

    /**
     * Get user's rides
     * GET /api/rides/my-rides?status=ACTIVE
     */
    @GET("/api/rides/my-rides")
    suspend fun getMyRides(
        @Header("Authorization") bearer: String,
        @Query("status") status: String? = null
    ): ApiResponse<List<RideResponse>>

    /**
     * Update ride status
     * PUT /api/rides/:rideId/status
     */
    @PUT("/api/rides/{rideId}/status")
    suspend fun updateRideStatus(
        @Header("Authorization") bearer: String,
        @Path("rideId") rideId: String,
        @Body request: UpdateRideStatusRequest
    ): ApiResponse<RideResponse>

    /**
     * Delete/Cancel a ride
     * DELETE /api/rides/:rideId
     */
    @DELETE("/api/rides/{rideId}")
    suspend fun deleteRide(
        @Header("Authorization") bearer: String,
        @Path("rideId") rideId: String
    ): ApiResponse<Unit>

    /**
     * Find matching rides
     * POST /api/rides/match
     */
    @POST("/api/rides/match")
    suspend fun findMatchingRides(
        @Header("Authorization") bearer: String,
        @Body request: MatchRidesRequest
    ): ApiResponse<MatchRidesResponse>
}

data class RidesListResponse(
    val data: List<RideResponse>,
    val pagination: PaginationInfo?
)

data class UpdateRideStatusRequest(
    val status: String // "ACTIVE", "MATCHED", "COMPLETED", "CANCELLED", "EXPIRED"
)

data class MatchRidesRequest(
    val origin: Location,
    val destination: Location,
    val rideType: String,
    val maxDistance: Double? = 2.0
)

data class MatchRidesResponse(
    val data: List<RideResponse>,
    val count: Int
)

