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
    val user: UserInfo,
    val token: String,
    val emailVerified: Boolean,
    val needsVerification: Boolean? = null,
    val emailSent: Boolean = false,
    // Legacy fields for backwards compatibility
    val userId: String? = null,
    val name: String? = null,
    val email: String? = null
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
    val photoUrl: String?,
    val isVerified: Boolean? = false,
    val emailVerified: Boolean? = false,
    val rating: Double? = null,
    val totalRides: Int? = null
)

// OTP Phone Authentication
data class SendOTPRequest(
    val phoneNumber: String
)

data class SendOTPResponse(
    val phoneNumber: String,
    val expiresIn: Int, // seconds
    val messageSid: String? = null
)

data class VerifyOTPRequest(
    val phoneNumber: String,
    val code: String,
    val name: String? = null // Optional, only for new users
)

data class VerifyOTPResponse(
    val user: UserInfo,
    val token: String // JWT token
)






