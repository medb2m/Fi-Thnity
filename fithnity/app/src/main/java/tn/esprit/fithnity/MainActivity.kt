package tn.esprit.fithnity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import tn.esprit.fithnity.ui.AuthScreen
import tn.esprit.fithnity.ui.theme.FithnityTheme
import tn.esprit.fithnity.ui.components.AlertBanner
import tn.esprit.fithnity.ui.components.AlertType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FithnityTheme {
                var userName by remember { mutableStateOf<String?>(null) }
                var needsEmailVerification by remember { mutableStateOf(false) }

                if (userName == null) {
                    AuthScreen(onAuthSuccess = { name, needsVerification ->
                        userName = name
                        needsEmailVerification = needsVerification
                    })
                } else {
                    WelcomeScreen(
                        userName = userName!!,
                        needsEmailVerification = needsEmailVerification,
                        onLogout = {
                            // Sign out from Firebase
                            FirebaseAuth.getInstance().signOut()
                            // Clear local user state
                            userName = null
                            needsEmailVerification = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    userName: String,
    needsEmailVerification: Boolean = false,
    onLogout: () -> Unit
) {
    var showAlert by remember { mutableStateOf(needsEmailVerification) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fi Thnity") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Welcome, $userName!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Button(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }

            // Alert banner at the top
            if (showAlert) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
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