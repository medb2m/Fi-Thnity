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

    // POST /api/users/firebase: sync Firebase-authenticated user (Requires Firebase Bearer ID token in the header)
    @POST("/api/users/firebase")
    suspend fun syncFirebaseUser(
        @Header("Authorization") bearer: String,
        @Body request: FirebaseRegisterRequest
    ): ApiResponse<UserInfo>
}






