package tn.esprit.fithnity.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface UserApiService {
    // Get user profile
    @GET("/api/users/profile")
    suspend fun getProfile(
        @Header("Authorization") bearer: String
    ): ApiResponse<UserInfo>

    // Update user profile
    @PUT("/api/users/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body request: UpdateProfileRequest
    ): ApiResponse<UserInfo>

    // Upload profile picture
    @Multipart
    @POST("/api/users/profile/upload-picture")
    suspend fun uploadProfilePicture(
        @Header("Authorization") bearer: String,
        @Part picture: MultipartBody.Part
    ): ApiResponse<UploadPictureResponse>
}

data class UpdateProfileRequest(
    val name: String? = null,
    val bio: String? = null,
    val photoUrl: String? = null
)

data class UploadPictureResponse(
    val photoUrl: String
)

