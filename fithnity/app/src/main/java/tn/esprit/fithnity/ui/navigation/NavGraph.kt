package tn.esprit.fithnity.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import tn.esprit.fithnity.ui.home.HomeScreen
import tn.esprit.fithnity.ui.rides.RidesScreen
import tn.esprit.fithnity.ui.user.ProfileScreen
import tn.esprit.fithnity.ui.community.CommunityScreen
import tn.esprit.fithnity.ui.theme.*

/**
 * Main Navigation Graph for the app
 * Handles all navigation between screens with smooth animations
 */
@Composable
fun FiThnityNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        // Rides Screen
        composable(Screen.Rides.route) {
            RidesScreen(navController = navController)
        }

        // Community Screen
        composable(Screen.Community.route) {
            CommunityScreen(navController = navController)
        }

        // Chat Screen (placeholder for now)
        composable(Screen.Chat.route) {
            // TODO: Implement ChatScreen
            PlaceholderScreen(
                title = "Chat",
                message = "Chat feature coming soon!",
                navController = navController
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                onLogout = onLogout
            )
        }

        // Edit Profile Screen (placeholder for now)
        composable(Screen.EditProfile.route) {
            // TODO: Implement EditProfileScreen
            PlaceholderScreen(
                title = "Edit Profile",
                message = "Edit profile feature coming soon!",
                navController = navController
            )
        }

        // Settings Screen (placeholder for now)
        composable(Screen.Settings.route) {
            // TODO: Implement SettingsScreen
            PlaceholderScreen(
                title = "Settings",
                message = "Settings feature coming soon!",
                navController = navController
            )
        }

        // Offer Ride Screen (placeholder for now)
        composable(Screen.OfferRide.route) {
            // TODO: Implement OfferRideScreen
            PlaceholderScreen(
                title = "Offer a Ride",
                message = "Offer ride feature coming soon!",
                navController = navController
            )
        }

        // Demand Ride Screen (placeholder for now)
        composable(Screen.DemandRide.route) {
            // TODO: Implement DemandRideScreen
            PlaceholderScreen(
                title = "Need a Ride",
                message = "Request ride feature coming soon!",
                navController = navController
            )
        }
    }
}

/**
 * Placeholder screen for features under development
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Construction,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { navController.navigateUp() }
        ) {
            Text("Go Back")
        }
    }
}
