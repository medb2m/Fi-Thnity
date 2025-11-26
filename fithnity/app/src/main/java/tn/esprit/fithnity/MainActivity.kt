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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.AuthScreen
import tn.esprit.fithnity.ui.theme.FithnityTheme
import tn.esprit.fithnity.ui.components.AlertBanner
import tn.esprit.fithnity.ui.components.AlertType
import tn.esprit.fithnity.ui.navigation.FiThnityBottomNavigation
import tn.esprit.fithnity.ui.navigation.FiThnityNavGraph
import tn.esprit.fithnity.ui.navigation.FiThnityTopBar
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.utils.LocaleManager
import tn.esprit.fithnity.ui.LanguageViewModel
import android.util.Log
import androidx.compose.material.icons.filled.Bolt
import tn.esprit.fithnity.ui.navigation.QuickActionsSheet
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
    // Start with a safe initial state - only check SharedPreferences (fast, synchronous)
    // Avoid calling Firebase synchronously during composition to prevent ANR
    var isAuthenticated by remember { 
        mutableStateOf(
            MainActivity.BYPASS_AUTH || 
            userPreferences.getAuthToken() != null // Only check stored token, not Firebase
        )
    }
    var needsEmailVerification by remember { mutableStateOf(userPreferences.needsEmailVerification()) }
    var isCheckingAuth by remember { mutableStateOf(true) }
    
    // Verify Firebase session asynchronously on app startup
    // This runs in a coroutine scope, so it won't block the main thread
    LaunchedEffect(Unit) {
        try {
            // Use withContext to ensure this runs off the main thread if needed
            // Firebase currentUser is usually fast, but we wrap it in try-catch for safety
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            
            if (firebaseUser != null) {
                // Firebase user exists - authenticate
                isAuthenticated = true
            } else if (isAuthenticated && userPreferences.getAuthToken() == "firebase_session") {
                // Firebase session expired but we have placeholder token - clear auth
                userPreferences.clearAuthData()
                isAuthenticated = false
            }
        } catch (e: Exception) {
            Log.e("FiThnityApp", "Error checking Firebase auth state", e)
            // On error, fall back to stored token check
            isAuthenticated = userPreferences.getAuthToken() != null
        } finally {
            isCheckingAuth = false
        }
    }

    // Show loading indicator while checking auth state
    if (isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (!isAuthenticated) {
        // Show Authentication Screen
        AuthScreen(
            userPreferences = userPreferences,
            languageViewModel = languageViewModel,
            onAuthSuccess = { name, needsVerification ->
                isAuthenticated = true
                needsEmailVerification = needsVerification
            }
        )
    } else {
        // Show Main App with Navigation
        MainAppScreen(
            needsEmailVerification = needsEmailVerification,
            userPreferences = userPreferences,
            languageViewModel = languageViewModel,
            onLogout = {
                // Sign out from Firebase asynchronously
                try {
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Log.e("FiThnityApp", "Error signing out from Firebase", e)
                }
                // Clear authentication data
                userPreferences.clearAuthData()
                // Clear authentication state
                isAuthenticated = false
                needsEmailVerification = false
            }
        )
    }
}

/**
 * Main App Screen with Bottom Navigation
 */
@Composable
fun MainAppScreen(
    needsEmailVerification: Boolean,
    userPreferences: UserPreferences,
    languageViewModel: LanguageViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var showAlert by remember { mutableStateOf(needsEmailVerification) }
    
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
    
    // Routes that should NOT show the top bar (they have their own back buttons)
    val routesWithoutTopBar = listOf(
        Screen.Profile.route,
        Screen.Settings.route,
        Screen.EditProfile.route
    )
    
    val showTopBar = currentRoute !in routesWithoutTopBar
    var showQuickActionsSheet by remember { mutableStateOf(false) }
    
    // Testing flag: Set to false to hide bottom navigation
    val showBottomNavigation = remember { true }

    if (showQuickActionsSheet) {
        QuickActionsSheet(onDismiss = { showQuickActionsSheet = false })
    }

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

            // Email Verification Alert
            if (showAlert) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AlertBanner(
                        message = "Please verify your email to access all features",
                        type = AlertType.WARNING,
                        isVisible = showAlert,
                        onDismiss = { showAlert = false },
                        autoDismissMillis = 2000L
                    )
                }
            }
        }
    }
}