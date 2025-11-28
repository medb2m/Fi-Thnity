package tn.esprit.fithnity.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import tn.esprit.fithnity.R
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*

/**
 * Custom Bottom Navigation Bar with Floating FAB
 */
@Composable
fun FiThnityBottomNavigation(
    navController: NavHostController,
    onQuickActionsClick: () -> Unit,
    unreadConversationCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = Screen.bottomNavItems

    // Bottom Navigation Structure (simplified - no transparent wrapper)
    // Surface contains navigation items, floating button extends above it
    // Box height increased to accommodate FAB extending 45.dp above (84 + 45 = 129.dp)
    // Added navigationBarsPadding to respect system navigation buttons
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(129.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // White spacing area at the bottom (24.dp) - drawn first so it's behind
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .align(Alignment.BottomCenter)
                .background(Color.White)
                .zIndex(0f)
        )
        
        // Main bottom bar - navigation items aligned to bottom
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-24).dp)
                .zIndex(1f),
            color = Color.Black,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First two items - independent, aligned to bottom
                items.take(2).forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    BottomNavItem(
                        icon = screen.icon!!,
                        label = screen.title,
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Spacer for the floating button
                Spacer(modifier = Modifier.weight(1f))
                
                // Last two items - independent, aligned to bottom
                items.takeLast(2).forEachIndexed { index, screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    BottomNavItem(
                        icon = screen.icon!!,
                        label = screen.title,
                        selected = isSelected,
                        badgeCount = null, // Don't render badge here, will render at root level
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Floating center button (3D effect) - half extends above navigation bar
        // Button is 90.dp, so 45.dp should be above the Surface top
        FloatingActionButton(
            onClick = onQuickActionsClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-45).dp)
                .size(90.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = Primary.copy(alpha = 0.5f)
                )
                .zIndex(2f),
            shape = CircleShape,
            containerColor = Primary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp,
                hoveredElevation = 10.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = stringResource(R.string.quick_actions),
                modifier = Modifier.size(42.dp),
                tint = Color.White
            )
        }
        
        // Badge for chat icon - rendered at root level to be above all other layers
        if (unreadConversationCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-28).dp, y = (-75).dp) // Position relative to bottom-end to align with chat icon
                    .size(22.dp) // Increased size
                    .zIndex(100f) // Above FAB
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadConversationCount > 9) "9+" else unreadConversationCount.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .size(45.dp),
            tint = if (selected) Primary else TextSecondary
        )
        
        // Badge removed from here - now rendered at root level for proper z-index
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(
    navController: NavHostController,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = TextPrimary,
        dragHandle = { 
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = TextSecondary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 5.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Items
            QuickActionItem(
                icon = Icons.Default.DirectionsCar,
                title = stringResource(R.string.i_need_ride),
                subtitle = "Find a ride to your destination",
                onClick = {
                    onDismiss()
                    navController.navigate("${Screen.Rides.route}?autoOpen=REQUEST")
                }
            )
            
            QuickActionItem(
                icon = Icons.Default.LocalTaxi,
                title = stringResource(R.string.i_offer_ride),
                subtitle = "Share your ride with others",
                onClick = {
                    onDismiss()
                    navController.navigate("${Screen.Rides.route}?autoOpen=OFFER")
                }
            )
            
            QuickActionItem(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.post_an_accident),
                subtitle = "Alert others about road incidents",
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = PrimaryLight.copy(alpha = 0.2f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}