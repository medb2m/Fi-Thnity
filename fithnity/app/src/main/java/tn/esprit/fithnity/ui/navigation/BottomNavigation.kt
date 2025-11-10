package tn.esprit.fithnity.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import tn.esprit.fithnity.ui.theme.*

/**
 * Modern Bottom Navigation Bar with glassmorphism effect
 */
@Composable
fun FiThnityBottomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                spotColor = Primary.copy(alpha = 0.1f)
            ),
        color = GlassBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(if (selected) 48.dp else 40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) Primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon!!,
                                contentDescription = screen.title,
                                modifier = Modifier.size(if (selected) 26.dp else 24.dp),
                                tint = if (selected) Primary else TextHint
                            )
                        }
                    },
                    label = {
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(animationSpec = tween(200)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically()
                        ) {
                            Text(
                                text = screen.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary
                            )
                        }
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        unselectedIconColor = TextHint,
                        selectedTextColor = Primary,
                        unselectedTextColor = TextHint,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
