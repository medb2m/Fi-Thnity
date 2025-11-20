package tn.esprit.fithnity.data

// Common API response
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)

// Email registration
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)
data class RegisterResponse(
    val userId: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val emailSent: Boolean = false
)

// Email login
data class LoginRequest(
    val email: String,
    val password: String
)
data class LoginResponse(
    val user: UserInfo,
    val token: String,
    val emailVerified: Boolean? = null,
    val needsVerification: Boolean? = null
)

data class UserInfo(
    val _id: String?,
    val name: String?,
    val email: String?,
    val phoneNumber: String?,
    val firebaseUid: String?,
    val photoUrl: String?,
    val isVerified: Boolean? = false,
    val emailVerified: Boolean? = false
)

// Firebase phone registration/sync
// Sent after successful Firebase sign-in
// (token sent as Bearer token in header, not in the payload)
data class FirebaseRegisterRequest(
    val firebaseUid: String,
    val phoneNumber: String,
    val name: String,
    val email: String? = null,
    val photoUrl: String? = null
)



