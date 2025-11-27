package tn.esprit.fithnity.ui.rides

import java.text.SimpleDateFormat
import java.util.*

/**
 * Format date and time from ISO string or display string
 */
fun formatDateTime(timeString: String): Pair<String, String> {
    return try {
        // Try to parse ISO format (e.g., "2024-01-15T14:30:00")
        if (timeString.contains("T") || timeString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(timeString) ?: Date()
            
            val calendar = Calendar.getInstance()
            calendar.time = date
            
            val today = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            
            val dateText = when {
                calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
                calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
                else -> {
                    val dayFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                    dayFormat.format(date)
                }
            }
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeText = timeFormat.format(date)
            
            Pair(dateText, timeText)
        } else {
            // If it's already formatted (e.g., "Today, 8:30 AM")
            val parts = timeString.split(", ")
            if (parts.size >= 2) {
                val datePart = parts[0]
                val timePart = parts[1]
                // Convert 12-hour to 24-hour if needed
                val time24 = convertTo24Hour(timePart)
                Pair(datePart, time24)
            } else {
                Pair("", timeString)
            }
        }
    } catch (e: Exception) {
        // Fallback to original string
        Pair("", timeString)
    }
}

/**
 * Convert 12-hour format to 24-hour format
 */
fun convertTo24Hour(timeStr: String): String {
    // If already in 24-hour format (e.g., "14:30"), return as is
    if (timeStr.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
        return timeStr
    }
    
    // Parse 12-hour format (e.g., "8:30 AM" or "2:45 PM")
    val pattern = Regex("^(\\d{1,2}):([0-5][0-9])\\s*(AM|PM|am|pm)$")
    val match = pattern.find(timeStr) ?: return "00:00"
    
    var hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2]
    val amPm = match.groupValues[3].uppercase()
    
    if (amPm == "PM" && hour != 12) {
        hour += 12
    } else if (amPm == "AM" && hour == 12) {
        hour = 0
    }
    
    return String.format("%02d:%s", hour, minute)
}

/**
 * Parse time string to hour and minute
 */
fun parseTime(timeStr: String): Pair<Int, Int> {
    val parts = timeStr.split(":")
    val hour = parts[0].toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return Pair(hour, minute)
}

/**
 * Sample rides data for development
 */
fun getSampleRides(): List<RideItem> {
    return listOf(
        RideItem(
            id = "1",
            isOffer = true,
            vehicleType = VehicleType.PERSONAL_CAR,
            origin = "Lac 1, Tunis",
            destination = "Ariana",
            userName = "Ahmed B.",
            time = "Today, 8:30 AM",
            price = null, // No price for personal cars
            seatsAvailable = 3,
            matchingInfo = MatchingInfo(matchedRiders = 2, savings = "3")
        ),
        RideItem(
            id = "2",
            isOffer = false,
            vehicleType = VehicleType.TAXI,
            origin = "Centre-ville, Tunis",
            destination = "La Marsa",
            userName = "Sara M.",
            time = "Today, 9:15 AM",
            price = "8", // Price for taxi
            matchingInfo = MatchingInfo(matchedRiders = 1, savings = "4")
        ),
        RideItem(
            id = "3",
            isOffer = true,
            vehicleType = VehicleType.TAXI,
            origin = "Ben Arous",
            destination = "Tunis Centre",
            userName = "Mohamed K.",
            time = "Today, 10:00 AM",
            price = "6", // Price for taxi
            seatsAvailable = 2,
            matchingInfo = MatchingInfo(matchedRiders = 1, savings = "2")
        ),
        RideItem(
            id = "4",
            isOffer = true,
            vehicleType = VehicleType.PERSONAL_CAR,
            origin = "Sidi Bou Said",
            destination = "Carthage",
            userName = "Fatma L.",
            time = "Today, 11:30 AM",
            price = null, // No price for personal cars
            seatsAvailable = 4
        ),
        RideItem(
            id = "5",
            isOffer = false,
            vehicleType = VehicleType.PERSONAL_CAR,
            origin = "Manouba",
            destination = "Tunis",
            userName = "Youssef A.",
            time = "Today, 2:00 PM",
            price = null // No price for personal cars
        )
    )
}

