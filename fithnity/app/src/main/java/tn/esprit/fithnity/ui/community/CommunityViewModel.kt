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

class CommunityViewModel : ViewModel() {
    private val api = NetworkModule.communityApi

    private val _uiState = MutableStateFlow<CommunityUiState>(CommunityUiState.Loading)
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _createPostState = MutableStateFlow<CreatePostUiState>(CreatePostUiState.Idle)
    val createPostState: StateFlow<CreatePostUiState> = _createPostState.asStateFlow()

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

            // Create RequestBody for text fields
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val postTypeBody = postType?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.createPost(
                bearer = "Bearer $token",
                content = contentBody,
                postType = postTypeBody,
                image = imagePart
            )
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
            Log.e(TAG, "createPost: Exception occurred", e)
            _createPostState.value = CreatePostUiState.Error(e.message ?: "Unknown error occurred")
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

            if (response.success) {
                Log.d(TAG, "votePost: Vote successful")
                // Refresh posts to get updated scores
                loadPosts()
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

            if (response.success) {
                Log.d(TAG, "addComment: Comment added successfully")
                // Refresh posts to get updated comments (will need authToken passed from screen)
                // loadPosts(authToken)
            } else {
                Log.e(TAG, "addComment: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "addComment: Exception occurred", e)
        }
    }

    /**
     * Reset create post state
     */
    fun resetCreatePostState() {
        _createPostState.value = CreatePostUiState.Idle
    }
}

