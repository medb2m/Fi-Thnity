package tn.esprit.fithnity.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.*

sealed class ConversationsUiState {
    object Idle : ConversationsUiState()
    object Loading : ConversationsUiState()
    data class Success(val conversations: List<ConversationResponse>) : ConversationsUiState()
    data class Error(val message: String) : ConversationsUiState()
}

sealed class MessagesUiState {
    object Idle : MessagesUiState()
    object Loading : MessagesUiState()
    data class Success(val messages: List<MessageResponse>) : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
}

sealed class SendMessageUiState {
    object Idle : SendMessageUiState()
    object Sending : SendMessageUiState()
    data class Success(val message: MessageResponse) : SendMessageUiState()
    data class Error(val message: String) : SendMessageUiState()
}

sealed class UsersUiState {
    object Idle : UsersUiState()
    object Loading : UsersUiState()
    data class Success(val users: List<UserSearchResponse>) : UsersUiState()
    data class Error(val message: String) : UsersUiState()
}

class ChatViewModel : ViewModel() {
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val api = NetworkModule.chatApi

    // State for conversations list
    private val _conversationsState = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Idle)
    val conversationsState: StateFlow<ConversationsUiState> = _conversationsState

    // State for messages in current conversation
    private val _messagesState = MutableStateFlow<MessagesUiState>(MessagesUiState.Idle)
    val messagesState: StateFlow<MessagesUiState> = _messagesState

    // State for sending message
    private val _sendMessageState = MutableStateFlow<SendMessageUiState>(SendMessageUiState.Idle)
    val sendMessageState: StateFlow<SendMessageUiState> = _sendMessageState

    // State for users search
    private val _usersState = MutableStateFlow<UsersUiState>(UsersUiState.Idle)
    val usersState: StateFlow<UsersUiState> = _usersState

    // Current conversation ID
    private var currentConversationId: String? = null

    /**
     * Load all conversations for the current user
     */
    fun loadConversations(authToken: String?, page: Int = 1) = viewModelScope.launch {
        Log.d(TAG, "loadConversations: Loading conversations page $page")
        _conversationsState.value = ConversationsUiState.Loading

        try {
            val token = authToken
            if (token == null) {
                _conversationsState.value = ConversationsUiState.Error("Not authenticated")
                return@launch
            }

            val response = api.getConversations(
                bearer = "Bearer $token",
                page = page,
                limit = 20
            )

            Log.d(TAG, "loadConversations: Loaded ${response.data.size} conversations")
            _conversationsState.value = ConversationsUiState.Success(response.data)
        } catch (e: Exception) {
            Log.e(TAG, "loadConversations: Exception occurred", e)
            _conversationsState.value = ConversationsUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get or create conversation with another user
     */
    fun getOrCreateConversation(authToken: String?, otherUserId: String, onSuccess: (ConversationResponse) -> Unit) = viewModelScope.launch {
        Log.d(TAG, "getOrCreateConversation: Getting conversation with user $otherUserId")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "getOrCreateConversation: Not authenticated")
                return@launch
            }

            val response = api.getOrCreateConversation(
                bearer = "Bearer $token",
                request = CreateConversationRequest(otherUserId = otherUserId)
            )

            if (response.success && response.data != null) {
                Log.d(TAG, "getOrCreateConversation: Conversation created/retrieved: ${response.data._id}")
                currentConversationId = response.data._id
                onSuccess(response.data)
            } else {
                Log.e(TAG, "getOrCreateConversation: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getOrCreateConversation: Exception occurred", e)
        }
    }

    /**
     * Load messages for a conversation
     */
    fun loadMessages(authToken: String?, conversationId: String, page: Int = 1) = viewModelScope.launch {
        Log.d(TAG, "loadMessages: Loading messages for conversation $conversationId")
        _messagesState.value = MessagesUiState.Loading
        currentConversationId = conversationId

        try {
            val token = authToken
            if (token == null) {
                _messagesState.value = MessagesUiState.Error("Not authenticated")
                return@launch
            }

            val response = api.getMessages(
                bearer = "Bearer $token",
                conversationId = conversationId,
                page = page,
                limit = 50
            )

            Log.d(TAG, "loadMessages: Loaded ${response.data.size} messages")
            _messagesState.value = MessagesUiState.Success(response.data)

            // Mark messages as read
            markAsRead(authToken, conversationId)
        } catch (e: Exception) {
            Log.e(TAG, "loadMessages: Exception occurred", e)
            _messagesState.value = MessagesUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(authToken: String?, conversationId: String, content: String) = viewModelScope.launch {
        Log.d(TAG, "sendMessage: Sending message to conversation $conversationId")
        _sendMessageState.value = SendMessageUiState.Sending

        try {
            val token = authToken
            if (token == null) {
                _sendMessageState.value = SendMessageUiState.Error("Not authenticated")
                return@launch
            }

            val response = api.sendMessage(
                bearer = "Bearer $token",
                conversationId = conversationId,
                request = SendMessageRequest(content = content)
            )

            if (response.success && response.data != null) {
                Log.d(TAG, "sendMessage: Message sent successfully")
                _sendMessageState.value = SendMessageUiState.Success(response.data)

                // Add message to current messages list
                val currentState = _messagesState.value
                if (currentState is MessagesUiState.Success) {
                    val updatedMessages = currentState.messages + response.data
                    _messagesState.value = MessagesUiState.Success(updatedMessages)
                }

                // Reset send state
                _sendMessageState.value = SendMessageUiState.Idle
            } else {
                Log.e(TAG, "sendMessage: Failed - ${response.message}")
                _sendMessageState.value = SendMessageUiState.Error(response.message ?: "Failed to send message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage: Exception occurred", e)
            _sendMessageState.value = SendMessageUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Mark messages as read
     */
    private fun markAsRead(authToken: String?, conversationId: String) = viewModelScope.launch {
        try {
            val token = authToken ?: return@launch

            api.markAsRead(
                bearer = "Bearer $token",
                conversationId = conversationId
            )
            Log.d(TAG, "markAsRead: Messages marked as read")
        } catch (e: Exception) {
            Log.e(TAG, "markAsRead: Exception occurred", e)
        }
    }

    /**
     * Search users for starting new chats
     */
    fun searchUsers(authToken: String?, query: String = "", page: Int = 1) = viewModelScope.launch {
        Log.d(TAG, "searchUsers: Searching users with query '$query'")
        _usersState.value = UsersUiState.Loading

        try {
            val token = authToken
            if (token == null) {
                _usersState.value = UsersUiState.Error("Not authenticated")
                return@launch
            }

            val response = api.getUsers(
                bearer = "Bearer $token",
                search = query,
                page = page,
                limit = 20
            )

            Log.d(TAG, "searchUsers: Found ${response.data.size} users")
            _usersState.value = UsersUiState.Success(response.data)
        } catch (e: Exception) {
            Log.e(TAG, "searchUsers: Exception occurred", e)
            _usersState.value = UsersUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Delete a conversation
     */
    fun deleteConversation(authToken: String?, conversationId: String) = viewModelScope.launch {
        Log.d(TAG, "deleteConversation: Deleting conversation $conversationId")

        try {
            val token = authToken
            if (token == null) {
                Log.e(TAG, "deleteConversation: Not authenticated")
                return@launch
            }

            val response = api.deleteConversation(
                bearer = "Bearer $token",
                conversationId = conversationId
            )

            if (response.success) {
                Log.d(TAG, "deleteConversation: Conversation deleted successfully")
                // Refresh conversations list
                loadConversations(authToken)
            } else {
                Log.e(TAG, "deleteConversation: Failed - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteConversation: Exception occurred", e)
        }
    }

    /**
     * Reset send message state
     */
    fun resetSendMessageState() {
        _sendMessageState.value = SendMessageUiState.Idle
    }
}

