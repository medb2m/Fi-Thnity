package tn.esprit.fithnity.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.navigation.Screen
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R
import java.io.File

/**
 * User Profile Screen
 */
@Composable
fun ProfileScreen(
    navController: NavHostController,
    onLogout: () -> Unit,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier,
    languageViewModel: tn.esprit.fithnity.ui.LanguageViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current
    val currentLanguage by languageViewModel.currentLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Profile state
    val profileState by profileViewModel.uiState.collectAsState()
    val uploadState by profileViewModel.uploadState.collectAsState()
    val authToken = userPreferences.getAuthToken()
    
    // Image picker
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    
    // Load profile when screen is visible or authToken changes
    LaunchedEffect(Unit) {
        if (authToken != null) {
            profileViewModel.loadProfile(authToken)
        }
    }
    
    // Reload profile when authToken changes
    LaunchedEffect(authToken) {
        if (authToken != null) {
            profileViewModel.loadProfile(authToken)
        }
    }
    
    // Handle profile load success - save photoUrl to preferences
    LaunchedEffect(profileState) {
        when (val state = profileState) {
            is ProfileUiState.Success -> {
                // Save photoUrl to preferences for persistence
                state.user.photoUrl?.let { photoUrl ->
                    userPreferences.savePhotoUrl(photoUrl)
                }
            }
            else -> {}
        }
    }
    
    // Handle upload result
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is ProfileUiState.Success -> {
                // Upload successful, reload profile
                if (authToken != null) {
                    profileViewModel.loadProfile(authToken)
                }
            }
            is ProfileUiState.Error -> {
                // Show error (could add a snackbar here)
            }
            else -> {}
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Convert URI to File and upload
            scope.launch {
                val file = getFileFromUri(context, it)
                if (file != null) {
                    imageFile = file
                    // Try Firebase token first, then fallback to stored auth token
                    val token = profileViewModel.getFirebaseIdToken() ?: authToken
                    if (token != null) {
                        profileViewModel.uploadProfilePicture(file, token)
                    }
                }
            }
        }
    }
    
    // Get user info from state
    val userInfo = when (val state = profileState) {
        is ProfileUiState.Success -> state.user
        else -> null
    }
    
    val userName = userInfo?.name ?: userPreferences.getUserName() ?: "User"
    val userPhone = userInfo?.phoneNumber ?: "+216 XX XXX XXX"
    // Use photoUrl from API response, fallback to saved photoUrl from preferences
    val photoUrl = userInfo?.photoUrl ?: userPreferences.getPhotoUrl()
    
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
                // Profile Photo with click to upload
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl != null && photoUrl.isNotEmpty()) {
                        // Ensure full URL for image loading
                        val fullImageUrl = if (photoUrl.startsWith("http")) {
                            photoUrl
                        } else {
                            // If relative URL, prepend base URL
                            "http://10.0.2.2:3000$photoUrl"
                        }
                        AsyncImage(
                            model = fullImageUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                    }
                    
                    // Upload indicator overlay
                    if (uploadState is ProfileUiState.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = Color.White
                            )
                        }
                    } else {
                        // Camera icon overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change profile picture",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Name
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(4.dp))

                // Phone
                Text(
                    text = userPhone,
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
                        value = userInfo?.rating?.toString() ?: "5.0",
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
                        value = userInfo?.totalRides?.toString() ?: "0",
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
 * Helper function to get File from Uri
 */
private fun getFileFromUri(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        android.util.Log.e("ProfileScreen", "Error getting file from URI", e)
        null
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
