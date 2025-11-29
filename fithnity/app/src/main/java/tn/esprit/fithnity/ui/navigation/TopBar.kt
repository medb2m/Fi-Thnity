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
import android.util.Log
import tn.esprit.fithnity.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
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
    
    // Refresh unread count on mount (with delay to avoid blocking)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
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
        currentRoute?.startsWith(Screen.Chat.route) == true -> 
            stringResource(R.string.search_users)
        else -> stringResource(R.string.search_placeholder)
    }
    
    // Fetch place suggestions when on HomeScreen and query changes
    LaunchedEffect(searchQuery, isHomeScreen) {
        // Start showing suggestions after 2 characters for more responsive search
        if (isHomeScreen && searchQuery.isNotBlank() && searchQuery.length >= 2) {
            SearchState.updateSuggestions(emptyList(), true, true)
            
            // Debounce the geocoding call (reduced from 400ms to 300ms for faster response)
            delay(300)
            
            try {
                val addresses = withContext(Dispatchers.IO) {
                    try {
                        val queryTrimmed = searchQuery.trim()
                        val queryLower = queryTrimmed.lowercase()
                        
                        // Get current app language for Nominatim API
                        val currentLocale = Locale.getDefault()
                        val languageCode = when {
                            currentLocale.language == "fr" -> "fr"
                            currentLocale.language == "ar" -> "en" // Use English instead of Arabic
                            else -> "en" // Default to English
                        }
                        
                        // Use Nominatim (OpenStreetMap) API - free and works great for Tunisia
                        val encodedQuery = java.net.URLEncoder.encode(queryTrimmed, "UTF-8")
                        // Limit to Tunisia, return up to 20 results, format as JSON, specify language
                        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&countrycodes=tn&limit=20&format=json&addressdetails=1&accept-language=$languageCode"
                        
                        val client = OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                val request = chain.request().newBuilder()
                                    .addHeader("User-Agent", "FiThnityApp/1.0") // Required by Nominatim
                                    .addHeader("Accept-Language", languageCode) // Request results in app language
                                    .build()
                                chain.proceed(request)
                            }
                            .build()
                        
                        val request = Request.Builder()
                            .url(url)
                            .get()
                            .build()
                        
                        val response = client.newCall(request).execute()
                        val responseBody = response.body?.string()
                        
                        if (!response.isSuccessful || responseBody == null) {
                            Log.e("TopBar", "Nominatim API error: ${response.code}, falling back to Geocoder")
                            // Fallback to Android Geocoder
                            return@withContext try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val fallbackResults = geocoder.getFromLocationName(queryTrimmed, 10) ?: emptyList()
                                fallbackResults.filter { address ->
                                    address.countryName?.equals("Tunisia", ignoreCase = true) == true ||
                                    address.countryCode?.equals("TN", ignoreCase = true) == true ||
                                    address.countryName?.equals("Tunisie", ignoreCase = true) == true
                                }
                            } catch (e: Exception) {
                                Log.e("TopBar", "Geocoder fallback also failed", e)
                                emptyList<Address>()
                            }
                        }
                        
                        val jsonArray = JSONArray(responseBody)
                        
                        if (jsonArray.length() == 0) {
                            // No results from Nominatim, try Geocoder as fallback
                            Log.d("TopBar", "No Nominatim results, trying Geocoder fallback")
                            return@withContext try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val fallbackResults = geocoder.getFromLocationName(queryTrimmed, 10) ?: emptyList()
                                fallbackResults.filter { address ->
                                    address.countryName?.equals("Tunisia", ignoreCase = true) == true ||
                                    address.countryCode?.equals("TN", ignoreCase = true) == true ||
                                    address.countryName?.equals("Tunisie", ignoreCase = true) == true
                                }
                            } catch (e: Exception) {
                                Log.e("TopBar", "Geocoder fallback failed", e)
                                emptyList<Address>()
                            }
                        }
                        
                        val addressesList = mutableListOf<Address>()
                        
                        for (i in 0 until jsonArray.length()) {
                            try {
                                val item = jsonArray.getJSONObject(i)
                                val addressDetails = item.optJSONObject("address") ?: JSONObject()
                                
                                // Create Address object from Nominatim response
                                val address = Address(Locale.getDefault())
                                
                                // Set coordinates
                                address.latitude = item.getDouble("lat")
                                address.longitude = item.getDouble("lon")
                                
                                // Extract address components
                                val displayName = item.optString("display_name", "")
                                val name = item.optString("name", "")
                                val city = addressDetails.optString("city", "")
                                val town = addressDetails.optString("town", "")
                                val village = addressDetails.optString("village", "")
                                val municipality = addressDetails.optString("municipality", "")
                                val state = addressDetails.optString("state", "")
                                val district = addressDetails.optString("county", "")
                                val postcode = addressDetails.optString("postcode", "")
                                val road = addressDetails.optString("road", "")
                                val houseNumber = addressDetails.optString("house_number", "")
                                val country = addressDetails.optString("country", "")
                                
                                // Determine locality (city, town, village, or municipality)
                                val locality = city.ifBlank { town.ifBlank { village.ifBlank { municipality } } }
                                
                                // Build address line
                                val addressParts = mutableListOf<String>()
                                if (houseNumber.isNotBlank()) addressParts.add(houseNumber)
                                if (road.isNotBlank()) addressParts.add(road)
                                if (addressParts.isEmpty() && name.isNotBlank()) {
                                    addressParts.add(name)
                                } else if (addressParts.isEmpty() && displayName.isNotBlank()) {
                                    // Use display_name but take only the first part (before comma)
                                    val firstPart = displayName.split(",").firstOrNull()?.trim() ?: displayName
                                    addressParts.add(firstPart)
                                }
                                
                                if (addressParts.isNotEmpty()) {
                                    address.setAddressLine(0, addressParts.joinToString(" "))
                                } else if (name.isNotBlank()) {
                                    address.setAddressLine(0, name)
                                } else if (displayName.isNotBlank()) {
                                    // Use first part of display_name
                                    val firstPart = displayName.split(",").firstOrNull()?.trim() ?: displayName
                                    address.setAddressLine(0, firstPart)
                                } else {
                                    address.setAddressLine(0, locality.ifBlank { state })
                                }
                                
                                // Set locality and other fields
                                address.locality = locality.ifBlank { null }
                                address.subLocality = district.ifBlank { null }
                                address.postalCode = postcode.ifBlank { null }
                                address.adminArea = state.ifBlank { null }
                                address.countryName = country.ifBlank { "Tunisia" }
                                address.countryCode = "TN"
                                address.featureName = name.ifBlank { null }
                                address.thoroughfare = road.ifBlank { null }
                                
                                addressesList.add(address)
                            } catch (e: Exception) {
                                Log.e("TopBar", "Error parsing Nominatim result $i", e)
                            }
                        }
                        
                        Log.d("TopBar", "Found ${addressesList.size} addresses from Nominatim for query: $queryTrimmed")
                        
                        // Filter addresses to only keep those that match the query
                        val queryWords = queryLower.split(" ").filter { it.isNotBlank() }
                        val filteredAddresses = addressesList.filter { address ->
                            val addressLine = address.getAddressLine(0)?.lowercase() ?: ""
                            val locality = address.locality?.lowercase() ?: ""
                            val subLocality = address.subLocality?.lowercase() ?: ""
                            val featureName = address.featureName?.lowercase() ?: ""
                            val thoroughfare = address.thoroughfare?.lowercase() ?: ""
                            val adminArea = address.adminArea?.lowercase() ?: ""
                            
                            // Check if all query words appear in at least one field
                            val allFields = listOf(addressLine, locality, subLocality, featureName, thoroughfare, adminArea)
                            val combinedText = allFields.joinToString(" ").lowercase()
                            
                            // All query words must be found in the combined text
                            queryWords.all { word ->
                                combinedText.contains(word)
                            }
                        }
                        
                        Log.d("TopBar", "Filtered to ${filteredAddresses.size} addresses matching query")
                        
                        // Sort by relevance with more precise scoring
                        val sortedAddresses = filteredAddresses.sortedWith(compareBy<Address> { address ->
                            val addressLine = address.getAddressLine(0)?.lowercase() ?: ""
                            val locality = address.locality?.lowercase() ?: ""
                            val subLocality = address.subLocality?.lowercase() ?: ""
                            val featureName = address.featureName?.lowercase() ?: ""
                            val thoroughfare = address.thoroughfare?.lowercase() ?: ""
                            
                            // Calculate relevance score (lower is better)
                            when {
                                // Exact match at start of address line (highest priority)
                                addressLine.startsWith(queryLower) -> 0
                                // Exact match at start of feature name
                                featureName.startsWith(queryLower) -> 1
                                // Exact match at start of locality
                                locality.startsWith(queryLower) -> 2
                                // Query words all at start of address line
                                queryWords.all { addressLine.startsWith(it) } -> 3
                                // Query appears as complete phrase in address line
                                addressLine.contains(queryLower) -> 4
                                // Query appears in feature name
                                featureName.contains(queryLower) -> 5
                                // Query words all appear in address line (in any order)
                                queryWords.all { addressLine.contains(it) } -> 6
                                // Query appears in locality
                                locality.contains(queryLower) -> 7
                                // Query words all appear in locality
                                queryWords.all { locality.contains(it) } -> 8
                                // Query appears in thoroughfare
                                thoroughfare.contains(queryLower) -> 9
                                // Query words all appear in feature name
                                queryWords.all { featureName.contains(it) } -> 10
                                // At least some query words in address line
                                queryWords.any { addressLine.contains(it) } -> 11
                                // At least some query words in locality
                                queryWords.any { locality.contains(it) } -> 12
                                // At least some query words in feature name
                                queryWords.any { featureName.contains(it) } -> 13
                                else -> 14
                            }
                        }.thenBy { address ->
                            // Secondary sort: prefer shorter names (more specific)
                            (address.getAddressLine(0)?.length ?: Int.MAX_VALUE)
                        })
                        
                        sortedAddresses
                    } catch (e: Exception) {
                        Log.e("TopBar", "Error in MapTiler geocoding", e)
                        emptyList<Address>()
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
        color = Color.White,
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
                color = Color.White,
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
                        navController.navigate(Screen.Notifications.route) {
                            // Don't add to back stack in a way that persists across tab switches
                            launchSingleTop = true
                        }
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
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(22.dp) // Increased size
                            .zIndex(1f)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    .heightIn(max = 500.dp) // Increased to show up to 20 suggestions
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
        MenuItem("My offers", Icons.Default.DirectionsCar, Screen.MyOffers.route),
        MenuItem("My requests", Icons.Default.Assignment, Screen.MyRequests.route),
        MenuItem("My friends", Icons.Default.People, Screen.MyFriends.route),
        MenuItem("My posts", Icons.Default.Article, Screen.MyPosts.route)
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
                        // Simple navigation without saving state - these are secondary screens
                        navController.navigate(item.route) {
                            launchSingleTop = true
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
