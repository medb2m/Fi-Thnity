package tn.esprit.fithnity.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import tn.esprit.fithnity.data.FriendResponse
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.theme.*

/**
 * My Friends Screen - Shows friends and pending invitations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFriendsScreen(
    navController: NavHostController,
    userPreferences: UserPreferences,
    viewModel: FriendViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val authToken = remember { userPreferences.getAuthToken() }
    val friendsState by viewModel.friendsState.collectAsState()
    val invitationsState by viewModel.invitationsState.collectAsState()
    
    var selectedFilter by remember { mutableStateOf("All Friends") } // "All Friends" or "Invitations"
    
    LaunchedEffect(Unit) {
        // Small delay to avoid blocking initial render
        kotlinx.coroutines.delay(100)
        viewModel.loadFriends(authToken)
        // Load invitations after a small delay to stagger API calls
        kotlinx.coroutines.delay(200)
        viewModel.loadInvitations(authToken)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Friends",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = Primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Surface)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "All Friends",
                    onClick = { selectedFilter = "All Friends" },
                    label = { Text("All Friends") }
                )
                FilterChip(
                    selected = selectedFilter == "Invitations",
                    onClick = { selectedFilter = "Invitations" },
                    label = { Text("Invitations") }
                )
            }
            
            // Content based on selected filter
            when (selectedFilter) {
                "All Friends" -> {
                    when (val state = friendsState) {
                        is FriendsUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                        is FriendsUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Error loading friends",
                                        fontSize = 16.sp,
                                        color = Error
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                    Button(
                                        onClick = { viewModel.loadFriends(authToken) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        is FriendsUiState.Success -> {
                            if (state.friends.isEmpty()) {
                                EmptyFriendsState()
                            } else {
                                FriendsList(
                                    friends = state.friends,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                "Invitations" -> {
                    when (val state = invitationsState) {
                        is InvitationsUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                        is InvitationsUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Error loading invitations",
                                        fontSize = 16.sp,
                                        color = Error
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                    Button(
                                        onClick = { viewModel.loadInvitations(authToken) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        is InvitationsUiState.Success -> {
                            if (state.invitations.isEmpty()) {
                                EmptyInvitationsState()
                            } else {
                                InvitationsList(
                                    invitations = state.invitations,
                                    authToken = authToken,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsList(
    friends: List<FriendResponse>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(friends) { friend ->
            FriendItem(friend = friend)
        }
    }
}

@Composable
private fun InvitationsList(
    invitations: List<FriendResponse>,
    authToken: String?,
    viewModel: FriendViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(invitations) { invitation ->
            InvitationItem(
                invitation = invitation,
                authToken = authToken,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun FriendItem(friend: FriendResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val profileImageUrl = friend.user.photoUrl?.let { url ->
                if (url.startsWith("http")) url else "http://72.61.145.239:9090$url"
            }
            
            if (profileImageUrl != null) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.user.name ?: "User",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Friends",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun InvitationItem(
    invitation: FriendResponse,
    authToken: String?,
    viewModel: FriendViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val profileImageUrl = invitation.user.photoUrl?.let { url ->
                if (url.startsWith("http")) url else "http://72.61.145.239:9090$url"
            }
            
            if (profileImageUrl != null) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invitation.user.name ?: "User",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Wants to be your friend",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            Button(
                onClick = {
                    if (authToken != null && invitation._id != null) {
                        viewModel.acceptFriendRequest(authToken, invitation._id)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Accept")
            }
        }
    }
}

@Composable
private fun EmptyFriendsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextHint
            )
            Text(
                text = "No friends yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = "Add friends to see them here",
                fontSize = 14.sp,
                color = TextHint
            )
        }
    }
}

@Composable
private fun EmptyInvitationsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextHint
            )
            Text(
                text = "No invitations",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = "You don't have any pending friend requests",
                fontSize = 14.sp,
                color = TextHint
            )
        }
    }
}

