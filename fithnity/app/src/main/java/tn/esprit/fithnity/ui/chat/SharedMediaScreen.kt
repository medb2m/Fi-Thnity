package tn.esprit.fithnity.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import tn.esprit.fithnity.data.SharedMediaItem
import tn.esprit.fithnity.data.UserPreferences
import tn.esprit.fithnity.ui.theme.*

/**
 * Shared Media Screen - Shows gallery of photos shared in conversation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaScreen(
    navController: NavHostController,
    conversationId: String,
    userPreferences: UserPreferences,
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val authToken = remember { userPreferences.getAuthToken() }
    val sharedMediaState by viewModel.sharedMediaState.collectAsState()
    var selectedImage by remember { mutableStateOf<SharedMediaItem?>(null) }

    LaunchedEffect(conversationId) {
        viewModel.loadSharedMedia(authToken, conversationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Media, Links & Docs",
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Surface)
        ) {
            when (sharedMediaState) {
                is SharedMediaUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is SharedMediaUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Error loading media",
                                fontSize = 16.sp,
                                color = Error
                            )
                            Text(
                                text = (sharedMediaState as SharedMediaUiState.Error).message,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Button(
                                onClick = { viewModel.loadSharedMedia(authToken, conversationId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is SharedMediaUiState.Success -> {
                    val media = (sharedMediaState as SharedMediaUiState.Success).media
                    if (media.isEmpty()) {
                        EmptyMediaState()
                    } else {
                        MediaGrid(
                            media = media,
                            onImageClick = { selectedImage = it }
                        )
                    }
                }
                else -> {}
            }
        }
    }

    // Full screen image viewer
    selectedImage?.let { image ->
        FullScreenImageViewer(
            image = image,
            onDismiss = { selectedImage = null }
        )
    }
}

@Composable
private fun MediaGrid(
    media: List<SharedMediaItem>,
    onImageClick: (SharedMediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(media) { item ->
            MediaItem(
                item = item,
                onClick = { onImageClick(item) }
            )
        }
    }
}

@Composable
private fun MediaItem(
    item: SharedMediaItem,
    onClick: () -> Unit
) {
    val imageUrl = if (item.imageUrl.startsWith("http")) {
        item.imageUrl
    } else {
        "http://72.61.145.239:9090${item.imageUrl}"
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Shared media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun EmptyMediaState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextHint
            )
            Text(
                text = "No media shared",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = "Photos shared in this conversation will appear here",
                fontSize = 14.sp,
                color = TextHint
            )
        }
    }
}

@Composable
private fun FullScreenImageViewer(
    image: SharedMediaItem,
    onDismiss: () -> Unit
) {
    val imageUrl = if (image.imageUrl.startsWith("http")) {
        image.imageUrl
    } else {
        "http://72.61.145.239:9090${image.imageUrl}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full screen image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

