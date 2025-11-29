package tn.esprit.fithnity.ui.support

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.*

sealed class SupportUiState {
    object Idle : SupportUiState()
    object Loading : SupportUiState()
    data class Success(val response: String, val shouldCreateTicket: Boolean = false) : SupportUiState()
    data class Error(val message: String) : SupportUiState()
}

sealed class TicketUiState {
    object Idle : TicketUiState()
    object Loading : TicketUiState()
    data class Success(val ticket: SupportTicket) : TicketUiState()
    data class Error(val message: String) : TicketUiState()
}

class SupportViewModel : ViewModel() {
    private val api = NetworkModule.supportApi
    private val TAG = "SupportViewModel"

    private val _chatState = MutableStateFlow<SupportUiState>(SupportUiState.Idle)
    val chatState: StateFlow<SupportUiState> = _chatState.asStateFlow()

    private val _ticketState = MutableStateFlow<TicketUiState>(TicketUiState.Idle)
    val ticketState: StateFlow<TicketUiState> = _ticketState.asStateFlow()

    // Store conversation history
    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    fun getConversationHistory(): List<ChatMessage> = _conversationHistory.value

    fun addUserMessage(content: String) {
        _conversationHistory.value = _conversationHistory.value + ChatMessage("user", content)
    }

    fun addBotMessage(content: String) {
        _conversationHistory.value = _conversationHistory.value + ChatMessage("assistant", content)
    }

    fun clearConversation() {
        _conversationHistory.value = emptyList()
    }

    /**
     * Send a message to the chatbot
     */
    fun sendMessage(authToken: String, message: String, ticketId: String? = null) = viewModelScope.launch {
        try {
            _chatState.value = SupportUiState.Loading
            addUserMessage(message)

            val request = ChatRequest(
                messages = _conversationHistory.value,
                ticketId = ticketId
            )

            val response = api.chatWithBot("Bearer $authToken", request)

            if (response.success && response.data != null) {
                val chatResponse = response.data
                addBotMessage(chatResponse.message)
                _chatState.value = SupportUiState.Success(
                    chatResponse.message,
                    chatResponse.shouldCreateTicket ?: false
                )
            } else {
                val errorMsg = response.message ?: response.error ?: "Unknown error"
                _chatState.value = SupportUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            _chatState.value = SupportUiState.Error(e.message ?: "Failed to send message")
        }
    }

    /**
     * Create a support ticket
     */
    fun createTicket(
        authToken: String,
        subject: String,
        description: String,
        category: String = "other"
    ) = viewModelScope.launch {
        try {
            _ticketState.value = TicketUiState.Loading

            val request = CreateTicketRequest(
                subject = subject,
                description = description,
                category = category,
                chatbotConversation = _conversationHistory.value
            )

            val response = api.createTicket("Bearer $authToken", request)

            if (response.success && response.data != null) {
                _ticketState.value = TicketUiState.Success(response.data)
            } else {
                val errorMsg = response.message ?: response.error ?: "Unknown error"
                _ticketState.value = TicketUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ticket", e)
            _ticketState.value = TicketUiState.Error(e.message ?: "Failed to create ticket")
        }
    }
}

