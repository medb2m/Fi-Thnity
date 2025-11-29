package tn.esprit.fithnity.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import tn.esprit.fithnity.data.ChatMessage
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.theme.*

@Composable
fun HelpSupportScreen(
    navController: NavHostController,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: SupportViewModel = viewModel()
) {
    val authToken = userPreferences.getAuthToken()
    val chatState by viewModel.chatState.collectAsState()
    val ticketState by viewModel.ticketState.collectAsState()
    val conversationHistory by viewModel.conversationHistory.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    var showCreateTicketDialog by remember { mutableStateOf(false) }
    var ticketSubject by remember { mutableStateOf("") }
    var ticketDescription by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom when new message arrives
    LaunchedEffect(conversationHistory.size) {
        if (conversationHistory.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(conversationHistory.size - 1)
            }
        }
    }

    // Show create ticket dialog when bot suggests it
    LaunchedEffect(chatState) {
        when (val state = chatState) {
            is SupportUiState.Success -> {
                if (state.shouldCreateTicket) {
                    showCreateTicketDialog = true
                    ticketDescription = conversationHistory.joinToString("\n") { "${it.role}: ${it.content}" }
                }
            }
            else -> {}
        }
    }

    // Show success message when ticket is created
    LaunchedEffect(ticketState) {
        when (val state = ticketState) {
            is TicketUiState.Success -> {
                showCreateTicketDialog = false
                // Could show a snackbar here
            }
            else -> {}
        }
    }

    // Initialize with welcome message
    LaunchedEffect(Unit) {
        if (conversationHistory.isEmpty()) {
            viewModel.addBotMessage("Hello! I'm here to help you with any questions or issues you might have. How can I assist you today?")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Help & Support",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversationHistory) { message ->
                MessageBubble(
                    message = message,
                    isUser = message.role == "user"
                )
            }

            // Show loading indicator
            if (chatState is SupportUiState.Loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        MessageBubble(
                            message = ChatMessage("assistant", "Typing..."),
                            isUser = false,
                            isLoading = true
                        )
                    }
                }
            }
        }

        // Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextHint
                ),
                singleLine = true
            )
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank() && authToken != null) {
                        viewModel.sendMessage(authToken, messageText.trim())
                        messageText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Create Ticket Dialog
    if (showCreateTicketDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTicketDialog = false },
            title = { Text("Create Support Ticket") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = ticketSubject,
                        onValueChange = { ticketSubject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ticketDescription,
                        onValueChange = { ticketDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ticketSubject.isNotBlank() && ticketDescription.isNotBlank() && authToken != null) {
                            viewModel.createTicket(authToken, ticketSubject, ticketDescription)
                        }
                    },
                    enabled = ticketSubject.isNotBlank() && ticketDescription.isNotBlank()
                ) {
                    Text("Create Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTicketDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isUser: Boolean,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Primary else Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = if (isUser) Color.White else Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = message.content,
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

