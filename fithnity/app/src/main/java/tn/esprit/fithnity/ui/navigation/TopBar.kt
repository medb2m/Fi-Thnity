package tn.esprit.fithnity.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import android.location.Geocoder
import android.location.Address
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.BoxScope
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.notifications.NotificationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Modern Futuristic Top Bar - All elements in one line
 * Borderless search bar with modern design
 * Responsive with proper system bar spacing
 */
@Composable
fun FiThnityTopBar(
    navController: NavHostController,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val isHomeScreen = currentRoute?.startsWith(Screen.Home.route) == true
    
    // Notification ViewModel
    val notificationViewModel: NotificationViewModel = viewModel()
    val authToken = remember { userPreferences.getAuthToken() }
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    
    // Refresh unread count on mount
    LaunchedEffect(Unit) {
        notificationViewModel.refreshUnreadCount(authToken)
    }
    
    // Hamburger menu state
    var showMenuDropdown by remember { mutableStateOf(false) }
    
    // Get search query from global state
    var searchQuery by remember { mutableStateOf(SearchState.searchQuery) }
    
    // Update local state when global state changes
    LaunchedEffect(SearchState.searchQuery) {
        searchQuery = SearchState.searchQuery
    }
    
    // Update global state for home screen
    LaunchedEffect(isHomeScreen) {
        SearchState.updateHomeScreen(isHomeScreen)
    }
    
    // Determine search placeholder based on current screen
    val searchPlaceholder = when {
        isHomeScreen -> 
            stringResource(R.string.search_places)
        currentRoute?.startsWith(Screen.Rides.route) == true -> 
            stringResource(R.string.search_rides)
        currentRoute?.startsWith(Screen.Community.route) == true -> 
            stringResource(R.string.search_posts)
        else -> stringResource(R.string.search_placeholder)
    }
    
    // Fetch place suggestions when on HomeScreen and query changes
    LaunchedEffect(searchQuery, isHomeScreen) {
        // Start showing suggestions after 2 characters for more responsive search
        if (isHomeScreen && searchQuery.isNotBlank() && searchQuery.length >= 2) {
            SearchState.updateSuggestions(emptyList(), true, true)
            
            // Debounce the geocoding call
            delay(400)
            
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    try {
                        // Get more suggestions (30) to have better results after filtering
                        // Geocoder already does partial matching, so "tun" will find "Tunis", "Tunisia", etc.
                        val allAddresses = geocoder.getFromLocationName(searchQuery, 30) ?: emptyList()
                        
                        // Filter to only include addresses in Tunisia
                        // The Geocoder already does partial matching, so we just need to filter by country
                        val tunisiaAddresses = allAddresses.filter { address ->
                            address.countryName?.equals("Tunisia", ignoreCase = true) == true ||
                            address.countryCode?.equals("TN", ignoreCase = true) == true ||
                            address.countryName?.equals("Tunisie", ignoreCase = true) == true
                        }
                        
                        // Limit to 10 best results
                        tunisiaAddresses.take(10)
                    } catch (e: IOException) {
                        emptyList()
                    }
                }
                SearchState.updateSuggestions(addresses, false, true)
            } catch (e: Exception) {
                SearchState.updateSuggestions(emptyList(), false, true)
            }
        } else {
            SearchState.updateSuggestions(emptyList(), false, false)
        }
        
        // Note: SearchState.updateQuery is already called immediately in onValueChange
        // This debounce is only for the geocoding suggestions, not for the search query itself
    }
    
    // Hide suggestions when route changes
    LaunchedEffect(currentRoute) {
        if (!isHomeScreen) {
            SearchState.updateSuggestions(emptyList(), false, false)
        }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Add system bar spacing
            Spacer(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.statusBars
                )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Hamburger Menu Button (Left)
            Box {
                IconButton(
                    onClick = { showMenuDropdown = !showMenuDropdown },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.menu),
                        tint = Primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                // Dropdown Menu
                MenuDropdown(
                    navController = navController,
                    expanded = showMenuDropdown,
                    onDismiss = { showMenuDropdown = false }
                )
            }
            
            // Modern Borderless Search Bar (Center - takes remaining space)
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = Surface,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(Modifier.width(12.dp))
                        
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { newValue ->
                                searchQuery = newValue
                                // Update SearchState immediately for real-time search in other screens
                                SearchState.updateQuery(newValue)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Normal
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = searchPlaceholder,
                                        color = TextHint,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        
                        if (searchQuery.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { 
                                    searchQuery = ""
                                    SearchState.updateQuery("")
                                    SearchState.updateSuggestions(emptyList(), false, false)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Notification Icon Button (Right)
            Box(modifier = Modifier.size(40.dp)) {
                IconButton(
                    onClick = {
                        navController.navigate(Screen.Notifications.route)
                        // Refresh unread count when opening notifications
                        notificationViewModel.refreshUnreadCount(authToken)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                // Unread count badge
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }
    }
}

/**
 * Floating dropdown for place suggestions (to be used outside TopBar)
 */
@Composable
fun PlaceSuggestionsDropdown(
    isVisible: Boolean,
    isLoading: Boolean,
    suggestions: List<Address>,
    searchQuery: String,
    onSuggestionSelected: (Address) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible && searchQuery.isNotBlank()) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .zIndex(1000f),
            shape = RoundedCornerShape(12.dp),
            color = Surface,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                    }
                } else if (suggestions.isEmpty() && searchQuery.length >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No places found",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                } else if (suggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(suggestions) { address ->
                            PlaceSuggestionItem(
                                address = address,
                                onClick = { onSuggestionSelected(address) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Place suggestion item in dropdown
 */
@Composable
private fun PlaceSuggestionItem(
    address: Address,
    onClick: () -> Unit
) {
    val addressLine = address.getAddressLine(0) ?: ""
    val secondaryLine = if (address.maxAddressLineIndex > 0) {
        address.getAddressLine(1) ?: ""
    } else {
        "${address.locality ?: ""}, ${address.countryName ?: ""}".trimStart(',', ' ')
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = addressLine,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (secondaryLine.isNotBlank()) {
                    Text(
                        text = secondaryLine,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
    
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = TextSecondary.copy(alpha = 0.1f),
        thickness = 0.5.dp
    )
}

/**
 * Modern Top App Bar with Back Button (for detail screens)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiThnityDetailTopBar(
    title: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background,
            titleContentColor = TextPrimary,
            navigationIconContentColor = Primary
        ),
        modifier = modifier
    )
}

/**
 * Smooth dropdown menu for hamburger menu
 */
@Composable
fun BoxScope.MenuDropdown(
    navController: NavHostController,
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    val menuItems = listOf(
        MenuItem("Profile", Icons.Default.Person, Screen.Profile.route),
        MenuItem("My offers", Icons.Default.DirectionsCar, null), // TODO: Add route
        MenuItem("My requests", Icons.Default.Assignment, null), // TODO: Add route
        MenuItem("My friends", Icons.Default.People, null), // TODO: Add route
        MenuItem("My posts", Icons.Default.Article, null) // TODO: Add route
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(200.dp)
            .offset(x = 0.dp, y = 8.dp)
    ) {
        menuItems.forEachIndexed { index, item ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                },
                onClick = {
                    if (item.route != null) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Menu item data class
 */
private data class MenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String?
)
