package tn.esprit.fithnity.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import tn.esprit.fithnity.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import tn.esprit.fithnity.R
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Modern Futuristic Top Bar - All elements in one line
 * Borderless search bar with modern design
 * Responsive with proper system bar spacing
 */
@Composable
fun FiThnityTopBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Add system bar spacing
            Spacer(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.statusBars
                )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Hamburger Menu Button (Left)
            IconButton(
                onClick = { /* TODO: Open navigation drawer */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.menu),
                    tint = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            // Modern Borderless Search Bar (Center - takes remaining space)
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = Surface,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Normal
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    color = TextHint,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    
                    if (searchQuery.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                }
            }
            
            // Profile Icon Button (Right)
            IconButton(
                onClick = {
                    navController.navigate(Screen.Profile.route) {
                        // Pop up to the start destination of the graph to avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Primary.copy(alpha = 0.15f), PrimaryLight.copy(alpha = 0.15f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile),
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            }
        }
    }
}

/**
 * Modern Top App Bar with Back Button (for detail screens)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiThnityDetailTopBar(
    title: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background,
            titleContentColor = TextPrimary,
            navigationIconContentColor = Primary
        ),
        modifier = modifier
    )
}

