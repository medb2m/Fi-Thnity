package tn.esprit.fithnity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import tn.esprit.fithnity.ui.AuthScreen
import tn.esprit.fithnity.ui.theme.FithnityTheme
import tn.esprit.fithnity.ui.components.AlertBanner
import tn.esprit.fithnity.ui.components.AlertType
import tn.esprit.fithnity.ui.navigation.FiThnityBottomNavigation
import tn.esprit.fithnity.ui.navigation.FiThnityNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FithnityTheme {
                FiThnityApp()
            }
        }
    }
}

/**
 * Main App Container
 * Handles authentication state and navigation
 */
@Composable
fun FiThnityApp() {
    var isAuthenticated by remember { mutableStateOf(false) }
    var needsEmailVerification by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        // Show Authentication Screen
        AuthScreen(onAuthSuccess = { name, needsVerification ->
            isAuthenticated = true
            needsEmailVerification = needsVerification
        })
    } else {
        // Show Main App with Navigation
        MainAppScreen(
            needsEmailVerification = needsEmailVerification,
            onLogout = {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
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
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var showAlert by remember { mutableStateOf(needsEmailVerification) }

    Scaffold(
        bottomBar = {
            FiThnityBottomNavigation(navController = navController)
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
                onLogout = onLogout
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