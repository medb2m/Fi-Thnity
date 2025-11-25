package tn.esprit.fithnity.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import tn.esprit.fithnity.R
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.components.GlassButton
import tn.esprit.fithnity.ui.components.GlassCard
import tn.esprit.fithnity.ui.components.GlassTextField
import tn.esprit.fithnity.ui.components.GradientButton
import tn.esprit.fithnity.ui.theme.*
import tn.esprit.fithnity.utils.LocaleManager
import android.util.Log

/**
 * Extension function to find Activity from Context
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun AuthScreen(
    userPreferences: UserPreferences,
    onAuthSuccess: (name: String, needsEmailVerification: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
    languageViewModel: LanguageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    // Observe language changes
    val currentLanguage by languageViewModel.currentLanguage.collectAsState()
    
    var isLogin by remember { mutableStateOf(true) }
    var isPhoneMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    
    // Validation functions
    fun validateName(value: String): String? {
        return when {
            value.isBlank() -> "Name is required"
            value.length < 2 -> "Name must be at least 2 characters"
            value.length > 50 -> "Name must be less than 50 characters"
            !value.matches(Regex("^[a-zA-ZÀ-ÿ\\s'-]+$")) -> "Name can only contain letters, spaces, hyphens and apostrophes"
            else -> null
        }
    }
    
    fun validateEmail(value: String): String? {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        return when {
            value.isBlank() -> "Email is required"
            !emailPattern.matches(value) -> "Please enter a valid email address"
            value.length > 100 -> "Email must be less than 100 characters"
            else -> null
        }
    }
    
    fun validatePassword(value: String, isLogin: Boolean): String? {
        return when {
            value.isBlank() -> "Password is required"
            !isLogin && value.length < 6 -> "Password must be at least 6 characters"
            !isLogin && !value.matches(Regex(".*[A-Z].*")) -> "Password must contain at least one uppercase letter"
            !isLogin && !value.matches(Regex(".*[a-z].*")) -> "Password must contain at least one lowercase letter"
            !isLogin && !value.matches(Regex(".*\\d.*")) -> "Password must contain at least one number"
            else -> null
        }
    }
    
    fun validatePhone(value: String): String? {
        val phonePattern = Regex("^\\+?[0-9]{8,15}$")
        val cleanedPhone = value.replace(Regex("[\\s-]"), "")
        return when {
            value.isBlank() -> "Phone number is required"
            !phonePattern.matches(cleanedPhone) -> "Please enter a valid phone number (e.g., +216XXXXXXXX)"
            cleanedPhone.length < 8 -> "Phone number too short"
            cleanedPhone.length > 15 -> "Phone number too long"
            else -> null
        }
    }

    // Firebase phone auth dialog state
    var showCodeDialog by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var verificationCode by remember { mutableStateOf("") }
    var isFirebaseLoading by remember { mutableStateOf(false) }
    var phoneAuthError by remember { mutableStateOf<String?>(null) }
    var firebaseUserForSync by remember { mutableStateOf<FirebaseUser?>(null) }

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            val successState = state as AuthUiState.Success
            val user = successState.user
            val needsVerification = successState.needsEmailVerification
            
            // Save auth token from viewModel (will be set by viewModel after successful login)
            val token = viewModel.getAuthToken()
            if (token != null) {
                userPreferences.saveAuthData(
                    token = token,
                    userId = user._id,
                    userName = user.name,
                    userEmail = user.email,
                    needsVerification = needsVerification
                )
            }
            
            onAuthSuccess(
                user.name ?: user.phoneNumber ?: user.email ?: "User",
                needsVerification
            )
            viewModel.resetState()
        }
    }

    // Verification code dialog
    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (!verificationId.isNullOrEmpty() && verificationCode.length >= 6) {
                            isFirebaseLoading = true
                            val credential = PhoneAuthProvider.getCredential(verificationId!!, verificationCode)
                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                .addOnSuccessListener { result ->
                                    firebaseUserForSync = result.user
                                    showCodeDialog = false
                                    isFirebaseLoading = false
                                }.addOnFailureListener { ex ->
                                    phoneAuthError = ex.message
                                    isFirebaseLoading = false
                                }
                        }
                    },
                    enabled = verificationCode.length >= 6 && !isFirebaseLoading
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Enter Verification Code") },
            text = {
                Column {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("6-digit Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (phoneAuthError != null) {
                        Text(phoneAuthError!!, color = Error)
                    }
                    if (isFirebaseLoading) CircularProgressIndicator()
                }
            }
        )
    }

    // Sync user to backend if new Firebase login
    LaunchedEffect(firebaseUserForSync) {
        firebaseUserForSync?.let { user ->
            viewModel.syncFirebaseUser(user, name)
            firebaseUserForSync = null
        }
    }

    // Main UI with gradient background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryGradientStart,
                        PrimaryGradientEnd,
                        Background
                    )
                )
            )
    ) {
        // Language Switcher in top-right corner with system bar spacing
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, end = 16.dp, start = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // English Button
            FilledTonalButton(
                onClick = {
                    Log.d("AuthScreen", "EN button clicked!")
                    languageViewModel.changeLanguage("en")
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (currentLanguage == "en" || currentLanguage == "auto") 
                        Color.White.copy(alpha = 0.3f) 
                    else 
                        Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "EN",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (currentLanguage == "en" || currentLanguage == "auto") 
                        FontWeight.Bold 
                    else 
                        FontWeight.Normal
                )
            }
            
            // French Button
            FilledTonalButton(
                onClick = {
                    Log.d("AuthScreen", "FR button clicked!")
                    languageViewModel.changeLanguage("fr")
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (currentLanguage == "fr") 
                        Color.White.copy(alpha = 0.3f) 
                    else 
                        Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "FR",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (currentLanguage == "fr") 
                        FontWeight.Bold 
                    else 
                        FontWeight.Normal
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(32.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.fithnity_logo),
                contentDescription = "Fi Thnity Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(24.dp))

            // Slogan
            Text(
                text = stringResource(id = if (isLogin) R.string.login else R.string.register),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Your smart carpooling companion",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Glass Card for auth form
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Auth Mode Selector
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isPhoneMode,
                        onClick = { isPhoneMode = false },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.email))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        selected = isPhoneMode,
                        onClick = { isPhoneMode = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.phone))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Error message
                if (state is AuthUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Error.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                (state as AuthUiState.Error).message,
                                color = Error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Phone authentication form
                if (isPhoneMode) {
                    GlassTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            nameError = validateName(it)
                        },
                        label = stringResource(R.string.name),
                        placeholder = stringResource(R.string.name),
                        leadingIcon = Icons.Default.Person,
                        isError = nameError != null,
                        errorMessage = nameError
                    )

                    Spacer(Modifier.height(16.dp))

                    GlassTextField(
                        value = phone,
                        onValueChange = { 
                            phone = it
                            phoneError = validatePhone(it)
                        },
                        label = stringResource(R.string.phone_number),
                        placeholder = "+216 XX XXX XXX",
                        supportingText = if (phoneError == null) "Include country code (e.g., +216)" else null,
                        leadingIcon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        errorMessage = phoneError
                    )

                    if (phoneAuthError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = phoneAuthError!!,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    GlassButton(
                        text = stringResource(R.string.phone),
                        onClick = {
                            // Validate before proceeding
                            nameError = if (name.isBlank()) "Name is required" else validateName(name)
                            phoneError = validatePhone(phone)
                            
                            if (nameError != null || phoneError != null) {
                                phoneAuthError = nameError ?: phoneError
                                return@GlassButton
                            }
                            
                            phoneAuthError = null
                            val formattedPhone = if (!phone.startsWith("+")) {
                                if (phone.startsWith("216")) "+$phone" else "+216$phone"
                            } else phone

                            if (formattedPhone.length < 12) {
                                phoneAuthError = "Phone number too short. Use format: +216XXXXXXXX"
                                return@GlassButton
                            }

                            isFirebaseLoading = true
                            val activity = context as? Activity ?: return@GlassButton
                            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                                .setPhoneNumber(formattedPhone)
                                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                                .setActivity(activity)
                                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                        FirebaseAuth.getInstance().signInWithCredential(credential)
                                            .addOnSuccessListener { result ->
                                                firebaseUserForSync = result.user
                                                isFirebaseLoading = false
                                            }.addOnFailureListener { ex ->
                                                phoneAuthError = ex.message
                                                isFirebaseLoading = false
                                            }
                                    }

                                    override fun onVerificationFailed(e: FirebaseException) {
                                        phoneAuthError = e.message
                                        isFirebaseLoading = false
                                    }

                                    override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                        verificationId = vId
                                        showCodeDialog = true
                                        isFirebaseLoading = false
                                    }
                                })
                                .build()
                            PhoneAuthProvider.verifyPhoneNumber(options)
                        },
                        enabled = !isFirebaseLoading && state !is AuthUiState.Loading,
                        icon = Icons.Default.Phone,
                        isPrimary = true
                    )
                } else {
                    // Email authentication form
                    if (!isLogin) {
                        GlassTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                nameError = validateName(it)
                            },
                            label = stringResource(R.string.name),
                            placeholder = stringResource(R.string.name),
                            leadingIcon = Icons.Default.Person,
                            isError = nameError != null,
                            errorMessage = nameError
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    GlassTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = validateEmail(it)
                        },
                        label = stringResource(R.string.email),
                        placeholder = "your@email.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailError != null,
                        errorMessage = emailError
                    )

                    Spacer(Modifier.height(16.dp))

                    GlassTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = validatePassword(it, isLogin)
                        },
                        label = stringResource(R.string.password),
                        placeholder = stringResource(R.string.password),
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordError != null,
                        errorMessage = passwordError
                    )

                    Spacer(Modifier.height(24.dp))

                    GlassButton(
                        text = stringResource(if (isLogin) R.string.login else R.string.register),
                        onClick = {
                            // Validate all fields before submission
                            if (!isLogin) {
                                nameError = validateName(name)
                            }
                            emailError = validateEmail(email)
                            passwordError = validatePassword(password, isLogin)
                            
                            // Only proceed if all validations pass
                            val isValid = (isLogin || nameError == null) && emailError == null && passwordError == null
                            
                            if (isValid) {
                                if (isLogin) viewModel.loginWithEmail(email.trim(), password)
                                else viewModel.registerWithEmail(name.trim(), email.trim(), password)
                            }
                        },
                        enabled = state !is AuthUiState.Loading && 
                                 (isLogin || name.isNotBlank()) && 
                                 email.isNotBlank() && 
                                 password.isNotBlank(),
                        icon = if (isLogin) Icons.Default.Login else Icons.Default.PersonAdd,
                        isPrimary = true
                    )
                }

                // Loading indicator
                if (state is AuthUiState.Loading || isFirebaseLoading) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Switch between login/register
                if (!isPhoneMode) {
                    TextButton(
                        onClick = { isLogin = !isLogin },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(if (isLogin) R.string.dont_have_account else R.string.already_have_account),
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
