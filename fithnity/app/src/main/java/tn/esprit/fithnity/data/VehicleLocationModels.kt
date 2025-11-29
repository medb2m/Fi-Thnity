package tn.esprit.fithnity.data

/**
 * Vehicle types for location sharing
 */
enum class VehicleType(val displayName: String, val iconName: String) {
    CAR("Car", "car"),
    BUS("Bus", "bus"),
    MINIBUS("Minibus", "minibus"),
    METRO("Metro", "metro"),
    TAXI("Taxi", "taxi"),
    MOTORCYCLE("Motorcycle", "motorcycle")
}

/**
 * Vehicle location update from client
 */
data class VehicleLocationUpdate(
    val vehicleId: String,
    val type: String, // VehicleType enum value
    val lat: Double,
    val lng: Double,
    val speed: Float = 0f, // km/h
    val bearing: Float = 0f, // degrees (0-360)
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Vehicle position broadcast to all clients
 */
data class VehiclePosition(
    val vehicleId: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val speed: Float,
    val bearing: Float,
    val timestamp: Long,
    val driverName: String? = null,
    val driverPhoto: String? = null
)

/**
 * WebSocket message types
 */
sealed class WebSocketMessage {
    data class UpdateLocation(val data: VehicleLocationUpdate) : WebSocketMessage()
    data class VehiclePosition(val data: VehiclePosition) : WebSocketMessage()
    data class Error(val message: String) : WebSocketMessage()
}

