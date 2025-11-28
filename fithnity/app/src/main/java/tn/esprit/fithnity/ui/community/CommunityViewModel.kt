package tn.esprit.fithnity.ui.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Firebase removed - using JWT tokens instead
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import tn.esprit.fithnity.data.*
import java.io.File

private const val TAG = "CommunityViewModel"

sealed class CommunityUiState {
    object Loading : CommunityUiState()
    data class Success(val posts: List<CommunityPostResponse>) : CommunityUiState()
    data class Error(val message: String) : CommunityUiState()
}

sealed class CreatePostUiState {
    object Idle : CreatePostUiState()
    object Loading : CreatePostUiState()
    data class Success(val post: CommunityPostResponse) : CreatePostUiState()
    data class Error(val message: String) : CreatePostUiState()
}

sealed class PostDetailState {
    object Loading : PostDetailState()
    data class Success(val post: CommunityPostResponse) : PostDetailState()
    data class Error(val message: String) : PostDetailState()
}

class CommunityViewModel : ViewModel() {
    private val api = NetworkModule.communityApi

    private val _uiState = MutableStateFlow<CommunityUiState>(CommunityUiState.Loading)
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _createPostState = MutableStateFlow<CreatePostUiState>(CreatePostUiState.Idle)
    val createPostState: StateFlow<CreatePostUiState> = _createPostState.asStateFlow()
    
    private val _postDetailState = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val postDetailState: StateFlow<PostDetailState> = _postDetailState.asStateFlow()

    /**
     * Load a single post by ID
     */
    fun loadPostById(
        authToken: String?,
        postId: String
    ) = viewModelScope.launch {
        Log.d(TAG, "loadPostById: Loading post $postId")
        _postDetailState.value = PostDetailState.Loading
        
        try {
            val bearer = if (authToken != null) "Bearer $authToken" else null
            val response = api.getPostById(bearer = bearer, postId = postId)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "loadPostById: Post loaded successfully")
                _postDetailState.value = PostDetailState.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error ?: "Failed to load post"
                Log.e(TAG, "loadPostById: Failed - $errorMsg")
                _postDetailState.value = PostDetailState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadPostById: Exception occurred", e)
            _postDetailState.value = PostDetailState.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Load all community posts
     */
    fun loadPosts(
        authToken: String? = null,
        postType: String? = null,
        sort: String = "score", // "score" or "new"
        page: Int = 1,
        limit: Int = 20
    ) = viewModelScope.launch {
        Log.d(TAG, "loadPosts: Loading posts")
        _uiState.value = CommunityUiState.Loading

        try {
            val bearer = if (authToken != null) "Bearer $authToken" else null
            
            val response = api.getPosts(
                bearer = bearer,
                postType = postType,
                sort = sort,
                page = page,
                limit = limit
            )
            Log.d(TAG, "loadPosts: Response received - success: ${response.success}")

            if (response.success && response.data != null) {
                val posts = response.data
                Log.d(TAG, "loadPosts: Posts loaded successfully - ${posts.size} posts")
                _uiState.value = CommunityUiState.Success(posts)
            } else {
                val errorMsg = response.message ?: response.error ?: "Failed to load posts"
                Log.e(TAG, "loadPosts: Failed - $errorMsg")
                _uiState.value = CommunityUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadPosts: Exception occurred", e)
            _uiState.value = CommunityUiState.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Create a new post
     */
    fun createPost(
        authToken: String?,
        content: String,
        postType: String? = null,
        imageFile: File? = null
    ) = viewModelScope.launch {
        Log.d(TAG, "createPost: Starting post creation")
        _createPostState.value = CreatePostUiState.Loading

        try {
            val token = authToken
            if (token == null) {
                _createPostState.value = CreatePostUiState.Error("Not authenticated. Please sign in.")
                return@launch
            }

            val imagePart = imageFile?.let { file ->
                Log.d(TAG, "createPost: Preparing image file: ${file.name}, size: ${file.length()} bytes")
                val mimeType = when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }
                Log.d(TAG, "createPost: Image MIME type: $mimeType")
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", file.name, requestFile)
                Log.d(TAG, "createPost: MultipartBody.Part created for image")
                part
            }

            // Create RequestBody for text fields
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val postTypeBody = postType?.toRequestBody("text/plain".toMediaTypeOrNull())

            Log.d(TAG, "createPost: About to call API - hasImage: ${imagePart != null}, contentLength: ${content.length}")
            Log.d(TAG, "createPost: API endpoint: /api/community/posts")
            
            val response = api.createPost(
                bearer = "Bearer $token",
                content = contentBody,
                postType = postTypeBody,
                image = imagePart
            )
            
            Log.d(TAG, "createPost: API call completed successfully")
            Log.d(TAG, "createPost: Response received - success: ${response.success}")

            if (response.success && response.data != null) {
                Log.d(TAG, "createPost: Post created successfully - ${response.data._id}")
                _createPostState.value = CreatePostUiState.Success(response.data)
                // Refresh posts list
                loadPosts()
            } else {
                val errorMsg = response.message ?: response.error ?: "Failed to create post"
                Log.e(TAG, "createPost: Failed - $errorMsg")
                _createPostState.value = CreatePostUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createPost: Exception occurred - Type: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "createPost: Exception message: ${e.message}")
            Log.e(TAG, "createPost: Exception cause: ${e.cause?.message}")
            val errorMessage = when (e) {
                is java.net.SocketException -> "Connection lost. Please check your internet connection."
                is java.net.SocketTimeoutException -> "Request timed out. The image might be too large."
                is java.io.IOException -> "Network error: ${e.message}"
                else -> e.message ?: "Unknown error occurred"
            }
            _createPostState.value = CreatePostUiState.Error(errorMessage)
        }
    }

    /**
     * Vote on a post
     */
    fun votePost(authToken: String?, postId: String, vote: String?) = viewModelScope.launch {
        Log.d(TAG, "votePost: Voting on post $postId with vote: $vote")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "votePost: Not authenticated")
                return@launch
            }

            val response = api.votePost(
                bearer = "Bearer $token",
                postId = postId,
                request = VoteRequest(vote = vote)
            )

            if (response.success && response.data != null) {
                Log.d(TAG, "votePost: Vote successful - new score: ${response.data.score}")
                
                // Update the post in the current state with the new vote data
                val currentState = _uiState.value
                if (currentState is CommunityUiState.Success) {
                    val updatedPosts = currentState.posts.map { post ->
                        if (post._id == postId) {
                            post.copy(
                                score = response.data.score ?: post.score,
                                userVote = response.data.userVote
                            )
                        } else {
                            post
                        }
                    }
                    _uiState.value = CommunityUiState.Success(updatedPosts)
                    Log.d(TAG, "votePost: UI updated with new vote")
                }
            } else {
                Log.e(TAG, "votePost: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "votePost: Exception occurred", e)
        }
    }

    /**
     * Add a comment to a post
     */
    fun addComment(authToken: String?, postId: String, content: String) = viewModelScope.launch {
        Log.d(TAG, "addComment: Adding comment to post $postId")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "addComment: Not authenticated")
                return@launch
            }

            val response = api.addComment(
                bearer = "Bearer $token",
                postId = postId,
                request = CommentRequest(content = content)
            )

            if (response.success && response.data != null) {
                Log.d(TAG, "addComment: Comment added successfully")
                
                // Update the post in the current state with the new comment
                val currentState = _uiState.value
                if (currentState is CommunityUiState.Success) {
                    val updatedPosts = currentState.posts.map { post ->
                        if (post._id == postId) {
                            // Add the new comment to the post
                            val updatedComments = (post.comments ?: emptyList()) + response.data
                            post.copy(
                                comments = updatedComments,
                                commentsCount = updatedComments.size
                            )
                        } else {
                            post
                        }
                    }
                    _uiState.value = CommunityUiState.Success(updatedPosts)
                    Log.d(TAG, "addComment: UI updated with new comment")
                }
            } else {
                Log.e(TAG, "addComment: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "addComment: Exception occurred", e)
        }
    }

    /**
     * Update a post
     */
    fun updatePost(
        authToken: String?,
        postId: String,
        content: String,
        imageFile: File? = null,
        removeImage: Boolean = false
    ) = viewModelScope.launch {
        Log.d(TAG, "updatePost: Updating post $postId")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "updatePost: Not authenticated")
                return@launch
            }

            val imagePart = imageFile?.let { file ->
                val mimeType = when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, requestFile)
            }

            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val removeImageBody = if (removeImage) {
                "true".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }

            val response = api.updatePost(
                bearer = "Bearer $token",
                postId = postId,
                content = contentBody,
                removeImage = removeImageBody,
                image = imagePart
            )

            if (response.success && response.data != null) {
                Log.d(TAG, "updatePost: Post updated successfully")
                
                // Update the post in the current state
                val currentState = _uiState.value
                if (currentState is CommunityUiState.Success) {
                    val updatedPosts = currentState.posts.map { post ->
                        if (post._id == postId) {
                            response.data
                        } else {
                            post
                        }
                    }
                    _uiState.value = CommunityUiState.Success(updatedPosts)
                }
            } else {
                Log.e(TAG, "updatePost: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "updatePost: Exception occurred", e)
        }
    }

    /**
     * Delete a post
     */
    fun deletePost(
        authToken: String?,
        postId: String
    ) = viewModelScope.launch {
        Log.d(TAG, "deletePost: Deleting post $postId")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "deletePost: Not authenticated")
                return@launch
            }

            val response = api.deletePost(
                bearer = "Bearer $token",
                postId = postId
            )

            if (response.success) {
                Log.d(TAG, "deletePost: Post deleted successfully")
                
                // Remove the post from the current state
                val currentState = _uiState.value
                if (currentState is CommunityUiState.Success) {
                    val updatedPosts = currentState.posts.filter { post ->
                        post._id != postId
                    }
                    _uiState.value = CommunityUiState.Success(updatedPosts)
                }
            } else {
                Log.e(TAG, "deletePost: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deletePost: Exception occurred", e)
        }
    }

    /**
     * Reset create post state
     */
    fun resetCreatePostState() {
        _createPostState.value = CreatePostUiState.Idle
    }
}

