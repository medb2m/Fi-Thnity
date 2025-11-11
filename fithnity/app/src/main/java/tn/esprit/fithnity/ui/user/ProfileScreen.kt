package tn.esprit.fithnity.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R

/**
 * User Profile Screen
 */
@Composable
fun ProfileScreen(
    navController: NavHostController,
    onLogout: () -> Unit,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier,
    languageViewModel: tn.esprit.fithnity.ui.LanguageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val activity = LocalContext.current as? Activity
    val currentLanguage by languageViewModel.currentLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.choose_language)) },
            text = {
                Column {
                    // System Default
                    TextButton(
                        onClick = {
                            languageViewModel.changeLanguage("auto")
                            showLanguageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.system_default))
                            if (currentLanguage == "auto") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        }
                    }
                    
                    // English
                    TextButton(
                        onClick = {
                            languageViewModel.changeLanguage("en")
                            showLanguageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.english))
                            if (currentLanguage == "en") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        }
                    }
                    
                    // French
                    TextButton(
                        onClick = {
                            languageViewModel.changeLanguage("fr")
                            showLanguageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.french))
                            if (currentLanguage == "fr") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Button Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateUp() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = stringResource(R.string.profile),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        // Profile Content
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Name
                Text(
                    text = "John Doe", // TODO: Get from user data
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(4.dp))

                // Phone
                Text(
                    text = "+216 12 345 678", // TODO: Get from user data
                    fontSize = 15.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(24.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rating
                    StatItem(
                        value = "4.8",
                        label = stringResource(R.string.rating),
                        color = Primary
                    )

                    // Divider
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp),
                        color = Divider
                    )

                    // Total Rides
                    StatItem(
                        value = "0",
                        label = stringResource(R.string.total_rides),
                        color = Accent
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Menu Options
        MenuOption(
            icon = Icons.Default.Edit,
            title = stringResource(R.string.edit_profile),
            onClick = { navController.navigate(Screen.EditProfile.route) }
        )

        Spacer(Modifier.height(12.dp))

        MenuOption(
            icon = Icons.Default.History,
            title = stringResource(R.string.my_rides),
            onClick = { /* TODO: Navigate to My Rides */ }
        )

        Spacer(Modifier.height(12.dp))

        MenuOption(
            icon = Icons.Default.Language,
            title = stringResource(R.string.language),
            onClick = { showLanguageDialog = true }
        )

        Spacer(Modifier.height(12.dp))

        MenuOption(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.settings),
            onClick = { navController.navigate(Screen.Settings.route) }
        )

        Spacer(Modifier.height(12.dp))

        MenuOption(
            icon = Icons.Default.Help,
            title = stringResource(R.string.help_support),
            onClick = { /* TODO: Open help */ }
        )

        Spacer(Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Error.copy(alpha = 0.1f),
                contentColor = Error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.logout),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Stat Item Component
 */
@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

/**
 * Menu Option Component
 */
@Composable
private fun MenuOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Primary
                )
            }

            Spacer(Modifier.width(16.dp))

            // Title
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextHint
            )
        }
    }
}
