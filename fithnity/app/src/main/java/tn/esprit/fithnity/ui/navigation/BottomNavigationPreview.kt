package tn.esprit.fithnity.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Main Content Area")
            }
        }
        
        if (showQuickActions) {
            QuickActionsSheet(onDismiss = { showQuickActions = false })
        }
    }
}
