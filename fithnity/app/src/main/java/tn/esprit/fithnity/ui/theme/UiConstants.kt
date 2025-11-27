package tn.esprit.fithnity.ui.theme

import androidx.compose.ui.unit.dp

/**
 * UI Constants for consistent spacing and sizing across the app
 */
object UiConstants {
    /**
     * Bottom Navigation Bar total height
     * Includes: 24.dp white space + 60.dp navigation bar + 45.dp quick action button extension
     * Total: 129.dp
     */
    val BottomNavigationHeight = 129.dp
    
    /**
     * Padding to add to content that should not be hidden under bottom navigation
     * Use this for screens with scrollable content (Profile, Community, Rides)
     * HomeScreen doesn't need this as the map can extend under the navigation
     */
    val ContentBottomPadding = BottomNavigationHeight
}

