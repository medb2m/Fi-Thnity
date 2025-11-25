package tn.esprit.fithnity.data

import com.google.gson.annotations.SerializedName

/**
 * Location data class matching backend structure
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

/**
 * Create ride request matching backend API
 */
data class CreateRideRequest(
    @SerializedName("rideType")
    val rideType: String, // "REQUEST" or "OFFER"
    
    @SerializedName("transportType")
    val transportType: String, // "TAXI", "TAXI_COLLECTIF", "PRIVATE_CAR", "METRO", "BUS"
    
    @SerializedName("origin")
    val origin: Location,
    
    @SerializedName("destination")
    val destination: Location,
    
    @SerializedName("availableSeats")
    val availableSeats: Int? = null,
    
    @SerializedName("notes")
    val notes: String? = null,
    
    @SerializedName("departureDate")
    val departureDate: String? = null, // ISO 8601 format
    
    @SerializedName("price")
    val price: Double? = null // Required for TAXI/TAXI_COLLECTIF, null for others
)

/**
 * Ride response from backend
 */
data class RideResponse(
    val _id: String,
    val user: UserInfo?,
    val firebaseUid: String,
    val rideType: String,
    val transportType: String,
    val origin: Location,
    val destination: Location,
    val availableSeats: Int,
    val departureDate: String,
    val price: Double?,
    val status: String,
    val expiresAt: String,
    val matchedWith: UserInfo?,
    val distance: Double?,
    val notes: String?,
    val createdAt: String?,
    val updatedAt: String?
)

