package tn.esprit.fithnity.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Headers

interface AuthApiService {
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    // OTP Phone Authentication
    @POST("/api/auth/otp/send")
    suspend fun sendOTP(@Body request: SendOTPRequest): ApiResponse<SendOTPResponse>

    @POST("/api/auth/otp/verify")
    suspend fun verifyOTP(@Body request: VerifyOTPRequest): ApiResponse<VerifyOTPResponse>

    @POST("/api/auth/otp/resend")
    suspend fun resendOTP(@Body request: SendOTPRequest): ApiResponse<SendOTPResponse>
}








