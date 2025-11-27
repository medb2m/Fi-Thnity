package tn.esprit.fithnity.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R

/**
 * Edit Profile Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authToken = userPreferences.getAuthToken()
    
    val profileState by profileViewModel.uiState.collectAsState()
    val resendVerificationState by profileViewModel.resendVerificationState.collectAsState()
    
    // Form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    
    var emailVerified by remember { mutableStateOf(false) }
    var phoneVerified by remember { mutableStateOf(false) }
    var hasEmail by remember { mutableStateOf(false) }
    var hasPhone by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var isResendingVerification by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isInitialLoad by remember { mutableStateOf(true) }
    var hasUpdated by remember { mutableStateOf(false) }
    
    // Load profile data
    LaunchedEffect(Unit) {
        if (authToken != null) {
            profileViewModel.loadProfile(authToken)
        }
    }
    
    // Update form fields when profile loads
    LaunchedEffect(profileState) {
        when (val state = profileState) {
            is ProfileUiState.Success -> {
                val user = state.user
                // Only update form fields on initial load or after update
                if (isInitialLoad || hasUpdated) {
                    name = user.name ?: ""
                    email = user.email ?: ""
                    phoneNumber = user.phoneNumber ?: ""
                    emailVerified = user.emailVerified == true
                    phoneVerified = user.isVerified == true
                    hasEmail = !user.email.isNullOrBlank()
                    hasPhone = !user.phoneNumber.isNullOrBlank()
                    
                    if (hasUpdated) {
                        // Update was successful
                        successMessage = "Profile updated successfully"
                        // Update user preferences
                        user.name?.let { 
                            userPreferences.saveAuthData(
                                token = authToken ?: "",
                                userId = user._id ?: "",
                                userName = it,
                                userEmail = user.email,
                                needsVerification = user.emailVerified != true
                            ) 
                        }
                        // Clear success message after 3 seconds
                        scope.launch {
                            kotlinx.coroutines.delay(3000)
                            successMessage = null
                        }
                        hasUpdated = false
                    }
                    
                    isInitialLoad = false
                }
                isLoading = false
            }
            is ProfileUiState.Loading -> {
                isLoading = true
            }
            is ProfileUiState.Error -> {
                errorMessage = state.message
                isLoading = false
                hasUpdated = false
            }
            else -> {}
        }
    }
    
    // Handle resend verification result
    LaunchedEffect(resendVerificationState) {
        when (val state = resendVerificationState) {
            is ProfileUiState.Success -> {
                isResendingVerification = false
                successMessage = "Verification email sent! Please check your inbox."
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    successMessage = null
                }
            }
            is ProfileUiState.Loading -> {
                isResendingVerification = true
            }
            is ProfileUiState.Error -> {
                isResendingVerification = false
                errorMessage = state.message
            }
            else -> {}
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.edit_profile),
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
                containerColor = Surface
            )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Success/Error Messages
            successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = message,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = message,
                            color = Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                trailingIcon = {
                    if (hasEmail) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (emailVerified) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "Not verified",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text(
                        text = if (hasEmail) email else "Add email address",
                        color = TextHint
                    )
                }
            )
            
            // Email Verification Status Card
            if (hasEmail) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (emailVerified) {
                            Primary.copy(alpha = 0.1f)
                        } else {
                            Error.copy(alpha = 0.1f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (emailVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (emailVerified) Primary else Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (emailVerified) "Email Verified" else "Email Not Verified",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (emailVerified) Primary else Error
                                )
                                if (!emailVerified) {
                                    Text(
                                        text = "Check your email for verification link",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        if (!emailVerified) {
                            TextButton(
                                onClick = {
                                    errorMessage = null
                                    successMessage = null
                                    isResendingVerification = true
                                    scope.launch {
                                        profileViewModel.resendVerificationEmail(authToken)
                                    }
                                },
                                enabled = !isResendingVerification && !isLoading
                            ) {
                                if (isResendingVerification) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Primary
                                    )
                                } else {
                                    Text(
                                        text = "Resend",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Phone Number Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(stringResource(R.string.phone_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                trailingIcon = {
                    if (hasPhone) {
                        if (phoneVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Not verified",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text(
                        text = if (hasPhone) phoneNumber else "Add phone number",
                        color = TextHint
                    )
                }
            )
            
            // Phone Verification Status Card
            if (hasPhone) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (phoneVerified) {
                            Primary.copy(alpha = 0.1f)
                        } else {
                            Error.copy(alpha = 0.1f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (phoneVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (phoneVerified) Primary else Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (phoneVerified) "Phone Verified" else "Phone Not Verified",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (phoneVerified) Primary else Error
                            )
                            if (!phoneVerified) {
                                Text(
                                    text = "Use OTP verification to verify your phone",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Save Button
            Button(
                onClick = {
                    errorMessage = null
                    successMessage = null
                    if (name.isBlank()) {
                        errorMessage = "Name is required"
                        return@Button
                    }
                    if (name.length < 2) {
                        errorMessage = "Name must be at least 2 characters"
                        return@Button
                    }
                    isLoading = true
                    hasUpdated = true
                    scope.launch {
                        profileViewModel.updateProfile(
                            authToken = authToken,
                            name = name.trim(),
                            email = if (email.isNotBlank()) email.trim() else null,
                            phoneNumber = if (phoneNumber.isNotBlank()) phoneNumber.trim() else null,
                            bio = if (bio.isNotBlank()) bio.trim() else null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.save),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

