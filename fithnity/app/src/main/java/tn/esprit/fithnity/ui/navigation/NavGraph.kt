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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.home.HomeScreen
import tn.esprit.fithnity.ui.rides.RidesScreen
import tn.esprit.fithnity.ui.rides.MyRidesScreen
import tn.esprit.fithnity.ui.user.ProfileScreen
import tn.esprit.fithnity.ui.user.SettingsScreen
import tn.esprit.fithnity.ui.user.EditProfileScreen
import tn.esprit.fithnity.ui.community.CommunityScreen
import tn.esprit.fithnity.ui.community.MyPostsScreen
import tn.esprit.fithnity.ui.chat.ChatListScreen
import tn.esprit.fithnity.ui.chat.ChatScreen
import tn.esprit.fithnity.ui.notifications.NotificationScreen
import tn.esprit.fithnity.ui.theme.*
import tn.esprit.fithnity.ui.LanguageViewModel

/**
 * Main Navigation Graph for the app
 * Handles all navigation between screens with smooth animations
 */
@Composable
fun FiThnityNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    onLogout: () -> Unit,
    isFirstHomeVisit: Boolean = true,
    onFirstHomeVisitComplete: () -> Unit = {},
    userPreferences: UserPreferences,
    languageViewModel: LanguageViewModel
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
            HomeScreen(
                navController = navController,
                showWelcomeBanner = isFirstHomeVisit,
                onWelcomeBannerDismissed = onFirstHomeVisitComplete
            )
        }

        // Rides Screen (base route without parameters)
        composable(Screen.Rides.route) {
            RidesScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }
        
        // Rides Screen with optional autoOpen parameter
        composable(
            route = "${Screen.Rides.route}?autoOpen={autoOpen}",
            arguments = listOf(
                navArgument("autoOpen") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val autoOpen = backStackEntry.arguments?.getString("autoOpen") ?: ""
            RidesScreen(
                navController = navController,
                userPreferences = userPreferences,
                autoOpenRideType = autoOpen.takeIf { it.isNotEmpty() }
            )
        }

        // Community Screen
        composable(Screen.Community.route) {
            CommunityScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }

        // Chat Screen (conversation list)
        composable(Screen.Chat.route) {
            ChatListScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }

        // Chat Detail Screen (individual conversation)
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                },
                navArgument("otherUserId") {
                    type = NavType.StringType
                },
                navArgument("otherUserName") {
                    type = NavType.StringType
                    defaultValue = "User"
                },
                navArgument("otherUserPhoto") {
                    type = NavType.StringType
                    defaultValue = "none"
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "User"
            val otherUserPhotoRaw = backStackEntry.arguments?.getString("otherUserPhoto") ?: "none"
            val otherUserPhoto = if (otherUserPhotoRaw == "none" || otherUserPhotoRaw.isEmpty()) {
                null
            } else {
                val decoded = java.net.URLDecoder.decode(otherUserPhotoRaw, "UTF-8")
                if (decoded == "none" || decoded.isEmpty()) null else decoded
            }
            ChatScreen(
                navController = navController,
                conversationId = conversationId,
                otherUserId = otherUserId,
                otherUserName = java.net.URLDecoder.decode(otherUserName, "UTF-8"),
                otherUserPhoto = otherUserPhoto,
                userPreferences = userPreferences
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                onLogout = onLogout,
                userPreferences = userPreferences,
                languageViewModel = languageViewModel
            )
        }

        // Edit Profile Screen
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                userPreferences = userPreferences,
                languageViewModel = languageViewModel
            )
        }

        // My Offers Screen
        composable(Screen.MyOffers.route) {
            MyRidesScreen(
                navController = navController,
                userPreferences = userPreferences,
                rideType = "OFFER"
            )
        }

        // My Requests Screen
        composable(Screen.MyRequests.route) {
            MyRidesScreen(
                navController = navController,
                userPreferences = userPreferences,
                rideType = "REQUEST"
            )
        }

        composable(Screen.Notifications.route) {
            NotificationScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }

        composable(Screen.MyPosts.route) {
            MyPostsScreen(
                navController = navController,
                userPreferences = userPreferences
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

        // New Post Screen - Now handled as dialog in CommunityScreen
        // Keeping route for backward compatibility but not used

        // Post Detail Screen (placeholder for now)
        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("postId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            // TODO: Implement PostDetailScreen
            PlaceholderScreen(
                title = "Post Details",
                message = "Post detail feature coming soon!\nPost ID: $postId",
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
