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
    val time: String,
    val price: String? = null,
    val seatsAvailable: Int? = null, // For offers
    val matchingInfo: MatchingInfo? = null // Matching information
)

