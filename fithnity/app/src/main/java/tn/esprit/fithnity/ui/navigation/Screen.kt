package tn.esprit.fithnity.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    // Bottom Navigation Screens
    object Home : Screen(
            route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Rides : Screen(
        route = "rides",
        title = "Rides",
        icon = Icons.Default.DirectionsCar
    )

    object Community : Screen(
        route = "community",
        title = "Community",
        icon = Icons.Default.Groups
    )

    object Chat : Screen(
        route = "chat",
        title = "Chat",
        icon = Icons.Default.Chat
    )

    // Profile and Settings
    object Profile : Screen(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )

    object EditProfile : Screen(
        route = "edit_profile",
        title = "Edit Profile"
    )

    object Settings : Screen(
        route = "settings",
        title = "Settings"
    )

    // Rides Detail Screens
    object OfferRide : Screen(
        route = "offer_ride",
        title = "Offer a Ride"
    )

    object DemandRide : Screen(
        route = "demand_ride",
        title = "Need a Ride"
    )

    object RideDetail : Screen(
        route = "ride_detail/{rideId}",
        title = "Ride Details"
    ) {
        fun createRoute(rideId: String) = "ride_detail/$rideId"
    }

    // Community Screens
    object NewPost : Screen(
        route = "new_post",
        title = "New Post"
    )

    object PostDetail : Screen(
        route = "post_detail/{postId}",
        title = "Post"
    ) {
        fun createRoute(postId: String) = "post_detail/$postId"
    }

    // Chat Screens
    object ChatConversation : Screen(
        route = "chat_conversation/{userId}",
        title = "Chat"
    ) {
        fun createRoute(userId: String) = "chat_conversation/$userId"
    }

    companion object {
        /**
         * List of screens to show in bottom navigation
         */
        val bottomNavItems = listOf(
            Home,
            Rides,
            Community,
            Chat
        )
    }
}
