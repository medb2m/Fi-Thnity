package tn.esprit.fithnity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
// Firebase removed - using Twilio OTP instead
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.AuthScreen
import tn.esprit.fithnity.ui.theme.FithnityTheme
import tn.esprit.fithnity.ui.components.ToastHost
import tn.esprit.fithnity.ui.navigation.FiThnityBottomNavigation
import tn.esprit.fithnity.ui.navigation.FiThnityNavGraph
import tn.esprit.fithnity.ui.navigation.FiThnityTopBar
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.utils.LocaleManager
import tn.esprit.fithnity.ui.LanguageViewModel
import android.util.Log
import androidx.compose.material.icons.filled.Bolt
import tn.esprit.fithnity.ui.navigation.QuickActionsSheet
import tn.esprit.fithnity.ui.navigation.PlaceSuggestionsDropdown
import tn.esprit.fithnity.ui.navigation.SearchState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    private lateinit var userPreferences: UserPreferences
    private val languageViewModel: LanguageViewModel by viewModels()
    
    // DEV FLAG: Set to true to bypass authentication screens during development
    companion object {
        internal const val BYPASS_AUTH = false
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d(TAG, "=== MainActivity onCreate ===")
        super.onCreate(savedInstanceState)
        
        // Initialize user preferences
        userPreferences = (application as FiThnityApplication).userPreferences
        android.util.Log.d(TAG, "UserPreferences initialized")
        android.util.Log.d(TAG, "Stored language: ${userPreferences.getCurrentLanguage()}")
        android.util.Log.d(TAG, "Current locale: ${resources.configuration.locales[0].language}")
        
        enableEdgeToEdge()
        setContent {
            // This key will force a full recomposition when the language changes
            val currentLanguage by languageViewModel.currentLanguage.collectAsState()
            
            LaunchedEffect(currentLanguage) {
                // This effect will run when currentLanguage changes.
                // We check against the activity's actual current locale.
                // If they are different, it means a change was requested and we need to recreate.
                val activityLocale = resources.configuration.locales[0].language
                if (currentLanguage != "auto" && currentLanguage != activityLocale) {
                    Log.d(TAG, "Language mismatch detected. ViewModel: $currentLanguage, Activity: $activityLocale. Recreating.")
                    recreate()
                } else if (currentLanguage == "auto" && !activityLocale.isNullOrEmpty() && languageViewModel.getLocale().language != activityLocale) {
                    // Handle case where user switches back to auto and it's different from current
                    Log.d(TAG, "Switching back to system default. Recreating.")
                    recreate()
                }
            }

            FithnityTheme {
                FiThnityApp(
                    userPreferences = userPreferences,
                    languageViewModel = languageViewModel
                )
            }
        }
        android.util.Log.d(TAG, "Content set complete")
    }
    
    override fun attachBaseContext(newBase: Context) {
        android.util.Log.d(TAG, "=== attachBaseContext called ===")
        // Apply language setting before activity is created
        val prefs = UserPreferences.getInstance(newBase)
        val languageCode = prefs.getCurrentLanguage()
        android.util.Log.d(TAG, "Language from prefs: $languageCode")
        
        val context = LocaleManager.setLocale(newBase, languageCode)
        android.util.Log.d(TAG, "Locale set, calling super.attachBaseContext")
        
        super.attachBaseContext(context)
        android.util.Log.d(TAG, "attachBaseContext complete")
    }
}

/**
 * Main App Container
 * Handles authentication state and navigation
 * Checks for persistent login on startup
 */
@Composable
fun FiThnityApp(userPreferences: UserPreferences, languageViewModel: LanguageViewModel) {
    // Check authentication state from stored token
    var isAuthenticated by remember { 
        mutableStateOf(
            MainActivity.BYPASS_AUTH || 
            userPreferences.getAuthToken() != null
        )
    }
    if (!isAuthenticated) {
        // Show Authentication Screen
        AuthScreen(
            userPreferences = userPreferences,
            languageViewModel = languageViewModel,
            onAuthSuccess = { name ->
                isAuthenticated = true
            }
        )
    } else {
        // Show Main App with Navigation
        MainAppScreen(
            userPreferences = userPreferences,
            languageViewModel = languageViewModel,
            onLogout = {
                // Clear authentication data
                userPreferences.clearAuthData()
                // Clear authentication state
                isAuthenticated = false
            }
        )
    }
}

/**
 * Main App Screen with Bottom Navigation
 */
@Composable
fun MainAppScreen(
    userPreferences: UserPreferences,
    languageViewModel: LanguageViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    
    // Track if user has visited home screen for the first time
    var isFirstHomeVisit by remember { mutableStateOf(true) }
    
    // Get current route to conditionally show top bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Mark first home visit as complete when home screen is shown
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Home.route && isFirstHomeVisit) {
            // Keep it true until the banner dismisses itself
            // The banner will handle its own state
        }
    }
    
    // Check if current route is a chat detail screen (dynamic route)
    val isChatDetailScreen = currentRoute?.startsWith("chat_detail/") == true
    
    // Routes that should NOT show the top bar (they have their own back buttons)
    val routesWithoutTopBar = listOf(
        Screen.Profile.route,
        Screen.Settings.route,
        Screen.EditProfile.route
    )
    
    // Routes that should NOT show the bottom navigation (full-screen views)
    val routesWithoutBottomNav = listOf(
        Screen.EditProfile.route,
        Screen.Settings.route
    )
    
    val showTopBar = currentRoute !in routesWithoutTopBar && !isChatDetailScreen
    val showBottomNavigation = currentRoute !in routesWithoutBottomNav && !isChatDetailScreen
    var showQuickActionsSheet by remember { mutableStateOf(false) }

    if (showQuickActionsSheet) {
        QuickActionsSheet(
            navController = navController,
            onDismiss = { showQuickActionsSheet = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.White,
            topBar = {
                if (showTopBar) {
                    FiThnityTopBar(navController = navController)
                }
            },
            bottomBar = {
                if (showBottomNavigation) {
                    FiThnityBottomNavigation(
                        navController = navController,
                        onQuickActionsClick = { showQuickActionsSheet = true }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Navigation Graph
                FiThnityNavGraph(
                    navController = navController,
                    onLogout = onLogout,
                    isFirstHomeVisit = isFirstHomeVisit,
                    onFirstHomeVisitComplete = { isFirstHomeVisit = false },
                    userPreferences = userPreferences,
                    languageViewModel = languageViewModel
                )

                // Toast Host - Global toast notifications
                ToastHost(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (showTopBar) 0.dp else 8.dp)
                )
            }
        }
        
        // Place Suggestions Dropdown - Positioned directly below TopBar (only on HomeScreen)
        if (SearchState.isHomeScreen && SearchState.showSuggestions && showTopBar) {
            val density = LocalDensity.current
            val statusBarHeight = with(density) {
                WindowInsets.statusBars.getTop(density).toDp()
            }
            // TopBar height: status bar + 12dp padding + 44dp search bar + 12dp padding = ~68dp + status bar
            val topBarHeight = statusBarHeight + 68.dp
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = topBarHeight) // Position directly below TopBar
                    .zIndex(1000f)
            ) {
                PlaceSuggestionsDropdown(
                    isVisible = SearchState.showSuggestions,
                    isLoading = SearchState.isLoadingSuggestions,
                    suggestions = SearchState.placeSuggestions,
                    searchQuery = SearchState.searchQuery,
                    onSuggestionSelected = { address ->
                        val addressLine = address.getAddressLine(0) ?: SearchState.searchQuery
                        SearchState.updateQuery(addressLine)
                        SearchState.updateSuggestions(emptyList(), false, false)
                    }
                )
            }
        }
    }
}