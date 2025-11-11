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
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R

/**
 * User Settings Screen
 * Allows users to manage app preferences and account settings
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier,
    languageViewModel: tn.esprit.fithnity.ui.LanguageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val activity = LocalContext.current as? Activity
    val currentLanguage by languageViewModel.currentLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Settings state
    var notificationsEnabled by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    
    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.choose_language)) },
            text = {
                Column {
                    listOf(
                        Triple("auto", stringResource(R.string.system_default), currentLanguage == "auto"),
                        Triple("en", stringResource(R.string.english), currentLanguage == "en"),
                        Triple("fr", stringResource(R.string.french), currentLanguage == "fr")
                    ).forEach { (code, name, isSelected) ->
                        TextButton(
                            onClick = {
                                languageViewModel.changeLanguage(code)
                                showLanguageDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name)
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Primary
                                    )
                                }
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
                text = stringResource(R.string.settings),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        // Settings Content
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.manage_preferences),
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Account Settings Section
        SettingsSectionHeader(stringResource(R.string.account))
        
        Spacer(Modifier.height(12.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Person,
            title = stringResource(R.string.edit_profile),
            subtitle = "Update your personal information",
            onClick = { navController.navigate("edit_profile") }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.privacy_security),
            subtitle = "Manage your privacy settings",
            onClick = { /* TODO: Navigate to privacy settings */ }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.notification_preferences),
            subtitle = "Choose what notifications you receive",
            onClick = { /* TODO: Navigate to notification settings */ }
        )
        
        Spacer(Modifier.height(24.dp))
        
        // App Preferences Section
        SettingsSectionHeader(stringResource(R.string.preferences))
        
        Spacer(Modifier.height(12.dp))
        
        SettingsSwitchItem(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.push_notifications),
            subtitle = stringResource(R.string.push_notifications_desc),
            checked = notificationsEnabled,
            onCheckedChange = { notificationsEnabled = it }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsSwitchItem(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.location_services),
            subtitle = stringResource(R.string.location_services_desc),
            checked = locationEnabled,
            onCheckedChange = { locationEnabled = it }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsSwitchItem(
            icon = Icons.Default.DarkMode,
            title = stringResource(R.string.dark_mode),
            subtitle = stringResource(R.string.dark_mode_desc),
            checked = darkModeEnabled,
            onCheckedChange = { darkModeEnabled = it }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsSwitchItem(
            icon = Icons.Default.VolumeUp,
            title = stringResource(R.string.sound_effects),
            subtitle = stringResource(R.string.sound_effects_desc),
            checked = soundEnabled,
            onCheckedChange = { soundEnabled = it }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.language),
            subtitle = when(currentLanguage) {
                "auto" -> stringResource(R.string.system_default)
                "en" -> stringResource(R.string.english)
                "fr" -> stringResource(R.string.french)
                else -> stringResource(R.string.english)
            },
            onClick = { showLanguageDialog = true }
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Support Section
        SettingsSectionHeader(stringResource(R.string.support))
        
        Spacer(Modifier.height(12.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Help,
            title = stringResource(R.string.help_center),
            subtitle = stringResource(R.string.help_center_desc),
            onClick = { /* TODO: Open help center */ }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Info,
            title = stringResource(R.string.about),
            subtitle = stringResource(R.string.about_desc),
            onClick = { /* TODO: Open about screen */ }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Star,
            title = stringResource(R.string.rate_us),
            subtitle = stringResource(R.string.rate_us_desc),
            onClick = { /* TODO: Open rating dialog */ }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsNavigationItem(
            icon = Icons.Default.Policy,
            title = stringResource(R.string.terms_conditions),
            subtitle = stringResource(R.string.terms_conditions_desc),
            onClick = { /* TODO: Open terms */ }
        )
        
        Spacer(Modifier.height(32.dp))
        
        // App Version
        Text(
            text = stringResource(R.string.app_version),
            fontSize = 13.sp,
            color = TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Settings Section Header
 */
@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

/**
 * Settings Navigation Item - Takes user to another screen
 */
@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
            
            // Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            
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

/**
 * Settings Switch Item - Toggle setting on/off
 */
@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
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
            
            // Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Switch
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = TextHint
                )
            )
        }
    }
}

