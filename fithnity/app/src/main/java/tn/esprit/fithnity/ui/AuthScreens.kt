package tn.esprit.fithnity.ui

import android.app.Activity
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import tn.esprit.fithnity.R
import tn.esprit.fithnity.ui.components.GlassButton
import tn.esprit.fithnity.ui.components.GlassCard
import tn.esprit.fithnity.ui.components.GlassTextField
import tn.esprit.fithnity.ui.components.GradientButton
import tn.esprit.fithnity.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: (name: String, needsEmailVerification: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    var isLogin by remember { mutableStateOf(true) }
    var isPhoneMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                        Text(phoneAuthError!!, color = MaterialTheme.colorScheme.error)
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
                text = "Save Time, Save Tunisia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

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
                                Text("Email")
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
                                Text("Phone")
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
                        onValueChange = { name = it },
                        label = "Name",
                        placeholder = "Enter your name",
                        leadingIcon = Icons.Default.Person
                    )

                    Spacer(Modifier.height(16.dp))

                    GlassTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone Number",
                        placeholder = "+216 XX XXX XXX",
                        supportingText = "Include country code (e.g., +216)",
                        leadingIcon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
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
                        text = "Continue with Phone",
                        onClick = {
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
                            onValueChange = { name = it },
                            label = "Name",
                            placeholder = "Enter your name",
                            leadingIcon = Icons.Default.Person
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    GlassTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "your@email.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(Modifier.height(16.dp))

                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(Modifier.height(24.dp))

                    GlassButton(
                        text = if (isLogin) "Login" else "Register",
                        onClick = {
                            if (isLogin) viewModel.loginWithEmail(email, password)
                            else viewModel.registerWithEmail(name, email, password)
                        },
                        enabled = state !is AuthUiState.Loading,
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
                            if (isLogin) "Don't have an account? Register" else "Already have an account? Login",
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
