package tn.esprit.fithnity.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import tn.esprit.fithnity.ui.theme.FithnityTheme

@Preview(showBackground = true, name = "Bottom Navigation Preview")
@Composable
fun BottomNavigationPreview() {
    FithnityTheme {
        var showQuickActions by remember { mutableStateOf(false) }
        val navController = rememberNavController()
        
        Scaffold(
            bottomBar = {
                FiThnityBottomNavigation(
                    navController = navController,
                    onQuickActionsClick = { showQuickActions = true }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4A90E2),
                                Color(0xFF357ABD)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Main Content Area\n(Map would be here)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        if (showQuickActions) {
            QuickActionsSheet(onDismiss = { showQuickActions = false })
        }
    }
}

