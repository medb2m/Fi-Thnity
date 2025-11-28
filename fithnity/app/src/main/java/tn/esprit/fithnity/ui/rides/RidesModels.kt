package tn.esprit.fithnity.ui.rides

/**
 * Filter options for ride list
 */
enum class RideFilter {
    ALL, REQUESTS, OFFERS
}

/**
 * Vehicle type options
 */
enum class VehicleType {
    ALL, PERSONAL_CAR, TAXI
}

/**
 * Ride type (Offer or Request)
 */
enum class RideType {
    REQUEST, OFFER
}

/**
 * Matching information for rides
 */
data class MatchingInfo(
    val matchedRiders: Int,
    val savings: String? = null // Savings in TND
)

/**
 * Ride item for UI display
 */
data class RideItem(
    val id: String,
    val isOffer: Boolean,
    val vehicleType: VehicleType,
    val origin: String,
    val destination: String,
    val userName: String,
    val userId: String? = null, // User ID who created the ride
    val userPhoto: String? = null, // User photo URL
    val time: String,
    val price: String? = null,
    val seatsAvailable: Int? = null, // For offers
    val matchingInfo: MatchingInfo? = null, // Matching information
    val matchedWithUserId: String? = null, // User ID who was added to the ride (backward compatibility)
    val matchedWithUserName: String? = null, // Name of user who was added to the ride (backward compatibility)
    val matchedWithUserPhoto: String? = null, // Photo of user who was added to the ride (backward compatibility)
    val passengers: List<PassengerInfo>? = null // List of all passengers added to the ride
)

/**
 * Passenger information for UI display
 */
data class PassengerInfo(
    val userId: String,
    val userName: String,
    val userPhoto: String? = null
)

