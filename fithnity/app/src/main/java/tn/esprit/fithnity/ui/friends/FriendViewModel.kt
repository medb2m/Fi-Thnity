package tn.esprit.fithnity.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.FriendResponse
import tn.esprit.fithnity.data.FriendStatus
import tn.esprit.fithnity.data.NetworkModule

private const val TAG = "FriendViewModel"

sealed class FriendsUiState {
    object Loading : FriendsUiState()
    data class Success(val friends: List<FriendResponse>) : FriendsUiState()
    data class Error(val message: String) : FriendsUiState()
}

sealed class InvitationsUiState {
    object Loading : InvitationsUiState()
    data class Success(val invitations: List<FriendResponse>) : InvitationsUiState()
    data class Error(val message: String) : InvitationsUiState()
}

class FriendViewModel : ViewModel() {
    private val api = NetworkModule.friendApi

    private val _friendsState = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val friendsState: StateFlow<FriendsUiState> = _friendsState.asStateFlow()

    private val _invitationsState = MutableStateFlow<InvitationsUiState>(InvitationsUiState.Loading)
    val invitationsState: StateFlow<InvitationsUiState> = _invitationsState.asStateFlow()

    private val _friendStatusState = MutableStateFlow<FriendStatus?>(null)
    val friendStatusState: StateFlow<FriendStatus?> = _friendStatusState.asStateFlow()

    /**
     * Load all friends
     */
    fun loadFriends(authToken: String?) = viewModelScope.launch {
        Log.d(TAG, "loadFriends: Loading friends")
        _friendsState.value = FriendsUiState.Loading

        try {
            val token = authToken
            if (token == null) {
                _friendsState.value = FriendsUiState.Error("Not authenticated")
                return@launch
            }

            val response = api.getFriends(bearer = "Bearer $token")

            if (response.success && response.data != null) {
                Log.d(TAG, "loadFriends: Friends loaded successfully - ${response.data.size} friends")
                _friendsState.value = FriendsUiState.Success(response.data)
            } else {
                _friendsState.value = FriendsUiState.Error("Failed to load friends")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadFriends: Exception occurred", e)
            _friendsState.value = FriendsUiState.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Load pending invitations
     */
    fun loadInvitations(authToken: String?) = viewModelScope.launch {
        Log.d(TAG, "loadInvitations: Loading invitations")
        _invitationsState.value = InvitationsUiState.Loading

        try {
            val token = authToken
            if (token == null) {
                _invitationsState.value = InvitationsUiState.Error("Not authenticated")
                return@launch
            }

            val response = api.getInvitations(bearer = "Bearer $token")

            if (response.success && response.data != null) {
                Log.d(TAG, "loadInvitations: Invitations loaded successfully - ${response.data.size} invitations")
                _invitationsState.value = InvitationsUiState.Success(response.data)
            } else {
                _invitationsState.value = InvitationsUiState.Error("Failed to load invitations")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadInvitations: Exception occurred", e)
            _invitationsState.value = InvitationsUiState.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Send a friend request
     */
    fun sendFriendRequest(authToken: String?, recipientId: String) = viewModelScope.launch {
        Log.d(TAG, "sendFriendRequest: Sending friend request to $recipientId")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "sendFriendRequest: Not authenticated")
                return@launch
            }

            val response = api.sendFriendRequest(
                bearer = "Bearer $token",
                request = tn.esprit.fithnity.data.SendFriendRequestRequest(recipientId = recipientId)
            )

            if (response.success) {
                Log.d(TAG, "sendFriendRequest: Friend request sent successfully")
            } else {
                Log.e(TAG, "sendFriendRequest: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendFriendRequest: Exception occurred", e)
        }
    }

    /**
     * Accept a friend request
     */
    fun acceptFriendRequest(authToken: String?, requestId: String) = viewModelScope.launch {
        Log.d(TAG, "acceptFriendRequest: Accepting friend request $requestId")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "acceptFriendRequest: Not authenticated")
                return@launch
            }

            val response = api.acceptFriendRequest(
                bearer = "Bearer $token",
                requestId = requestId
            )

            if (response.success) {
                Log.d(TAG, "acceptFriendRequest: Friend request accepted successfully")
                // Reload friends and invitations
                loadFriends(token)
                loadInvitations(token)
            } else {
                Log.e(TAG, "acceptFriendRequest: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "acceptFriendRequest: Exception occurred", e)
        }
    }

    /**
     * Get friend status with a user
     */
    fun getFriendStatus(authToken: String?, userId: String) = viewModelScope.launch {
        Log.d(TAG, "getFriendStatus: Getting friend status with $userId")

        try {
            val token = authToken
            if (token == null) {
                return@launch
            }

            val response = api.getFriendStatus(
                bearer = "Bearer $token",
                userId = userId
            )

            if (response.success && response.data != null) {
                _friendStatusState.value = response.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFriendStatus: Exception occurred", e)
        }
    }
}

