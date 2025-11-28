package tn.esprit.fithnity.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface CommunityApiService {
    /**
     * Create a new community post
     * POST /api/community/posts
     */
    @Multipart
    @POST("/api/community/posts")
    suspend fun createPost(
        @Header("Authorization") bearer: String,
        @Part("content") content: RequestBody,
        @Part("postType") postType: RequestBody? = null,
        @Part image: MultipartBody.Part? = null
    ): ApiResponse<CommunityPostResponse>

    /**
     * Get all community posts
     * GET /api/community/posts?postType=GENERAL&sort=score&page=1&limit=20
     */
    @GET("/api/community/posts")
    suspend fun getPosts(
        @Header("Authorization") bearer: String? = null,
        @Query("postType") postType: String? = null,
        @Query("sort") sort: String? = null, // "score" or "new"
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): CommunityPostsApiResponse

    /**
     * Get post by ID
     * GET /api/community/posts/:postId
     */
    @GET("/api/community/posts/{postId}")
    suspend fun getPostById(
        @Header("Authorization") bearer: String? = null,
        @Path("postId") postId: String
    ): ApiResponse<CommunityPostResponse>

    /**
     * Vote on a post (upvote/downvote)
     * POST /api/community/posts/:postId/vote
     */
    @POST("/api/community/posts/{postId}/vote")
    suspend fun votePost(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Body request: VoteRequest
    ): ApiResponse<VoteResponse>

    /**
     * Add a comment to a post
     * POST /api/community/posts/:postId/comments
     */
    @POST("/api/community/posts/{postId}/comments")
    suspend fun addComment(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Body request: CommentRequest
    ): ApiResponse<CommentResponse>

    /**
     * Update a comment
     * PUT /api/community/posts/:postId/comments/:commentId
     */
    @PUT("/api/community/posts/{postId}/comments/{commentId}")
    suspend fun updateComment(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Path("commentId") commentId: String,
        @Body request: CommentRequest
    ): ApiResponse<CommentResponse>

    /**
     * Delete a comment
     * DELETE /api/community/posts/:postId/comments/:commentId
     */
    @DELETE("/api/community/posts/{postId}/comments/{commentId}")
    suspend fun deleteComment(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Path("commentId") commentId: String
    ): ApiResponse<Unit>

    /**
     * Update a post
     * PUT /api/community/posts/:postId
     */
    @Multipart
    @PUT("/api/community/posts/{postId}")
    suspend fun updatePost(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Part("content") content: RequestBody,
        @Part("removeImage") removeImage: RequestBody? = null,
        @Part image: MultipartBody.Part? = null
    ): ApiResponse<CommunityPostResponse>

    /**
     * Delete a post
     * DELETE /api/community/posts/:postId
     */
    @DELETE("/api/community/posts/{postId}")
    suspend fun deletePost(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String
    ): ApiResponse<Unit>
}

