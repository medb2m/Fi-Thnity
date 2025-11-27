package tn.esprit.fithnity.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.data.UserSearchResponse
import tn.esprit.fithnity.ui.theme.*

/**
 * User Selection Dialog - For starting new chats
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSelectionDialog(
    userPreferences: UserPreferences,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onUserSelected: (UserSearchResponse) -> Unit
) {
    val usersState by viewModel.usersState.collectAsState()
    val authToken = userPreferences.getAuthToken()
    var searchQuery by remember { mutableStateOf("") }

    // Debounced search
    LaunchedEffect(searchQuery) {
        delay(300) // Debounce for 300ms
        viewModel.searchUsers(authToken, searchQuery)
    }

    // Load users initially
    LaunchedEffect(Unit) {
        viewModel.searchUsers(authToken, "")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Primary)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "New Message",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search users...", color = TextHint) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Primary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = TextHint.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Users List
                when (val state = usersState) {
                    is UsersUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }

                    is UsersUiState.Success -> {
                        if (state.users.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = TextHint
                                    )
                                    Text(
                                        text = if (searchQuery.isEmpty()) {
                                            "No users found"
                                        } else {
                                            "No results for \"$searchQuery\""
                                        },
                                        fontSize = 16.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.users) { user ->
                                    UserItem(
                                        user = user,
                                        onClick = { onUserSelected(user) }
                                    )
                                }
                            }
                        }
                    }

                    is UsersUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Error loading users",
                                    fontSize = 16.sp,
                                    color = Error
                                )
                                Text(
                                    text = state.message,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = { viewModel.searchUsers(authToken, searchQuery) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

/**
 * Single user item in the selection list
 */
@Composable
fun UserItem(
    user: UserSearchResponse,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (user.photoUrl != null && user.photoUrl.isNotEmpty()) {
                    val fullPhotoUrl = if (user.photoUrl.startsWith("http")) {
                        user.photoUrl
                    } else {
                        "http://72.61.145.239:9090${user.photoUrl}"
                    }
                    AsyncImage(
                        model = fullPhotoUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // User Name
            Text(
                text = user.name ?: "User",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }

    Divider(
        modifier = Modifier.padding(start = 76.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}

