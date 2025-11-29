package tn.esprit.fithnity.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.Dialog
import tn.esprit.fithnity.ui.theme.*

/**
 * Dialog: What are you looking for? (Metro or Bus)
 */
@Composable
fun PublicTransportSearchDialog(
    onDismiss: () -> Unit,
    onMetroSelected: () -> Unit,
    onBusSelected: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "What are you looking for?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Metro Option
                Surface(
                    onClick = {
                        onMetroSelected()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Metro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Track metro lines",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Bus Option
                Surface(
                    onClick = {
                        onBusSelected()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Bus",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Track bus routes",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Dialog: Select Metro Line (1-6)
 */
@Composable
fun MetroLineSelectionDialog(
    onDismiss: () -> Unit,
    onLineSelected: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Metro Line",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Which metro line are you looking for?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Metro Lines 1-6
                (1..6).forEach { lineNumber ->
                    MetroLineItem(
                        lineNumber = lineNumber,
                        onClick = {
                            onLineSelected(lineNumber)
                            onDismiss()
                        }
                    )
                    if (lineNumber < 6) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun MetroLineItem(
    lineNumber: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lineNumber.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Metro $lineNumber",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

/**
 * Dialog: Input Bus Number
 */
@Composable
fun BusNumberInputDialog(
    onDismiss: () -> Unit,
    onBusSelected: (String) -> Unit
) {
    var busNumber by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Enter Bus Number",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Which bus are you looking for?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = busNumber,
                    onValueChange = { busNumber = it },
                    label = { Text("Bus Number") },
                    placeholder = { Text("e.g., 12, 25, 101") },
                    leadingIcon = {
                        Icon(Icons.Default.DirectionsBus, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (busNumber.isNotBlank()) {
                                onBusSelected(busNumber.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = busNumber.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Search")
                    }
                }
            }
        }
    }
}

/**
 * Dialog: Are you on the metro or bus? (Notification response)
 */
@Composable
fun PublicTransportConfirmationDialog(
    onDismiss: () -> Unit,
    onMetroSelected: () -> Unit,
    onBusSelected: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Are you on your way today?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Share your location to help others track public transport",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Metro Option
                Surface(
                    onClick = {
                        onMetroSelected()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "I'm on the Metro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Bus Option
                Surface(
                    onClick = {
                        onBusSelected()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "I'm on the Bus",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Not Now")
                }
            }
        }
    }
}

/**
 * Dialog: Select Metro Line when user confirms they're on metro
 */
@Composable
fun MetroLineConfirmationDialog(
    onDismiss: () -> Unit,
    onLineConfirmed: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Which Metro Line?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Select the metro line you're currently on",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(24.dp))
                
                (1..6).forEach { lineNumber ->
                    MetroLineItem(
                        lineNumber = lineNumber,
                        onClick = {
                            onLineConfirmed(lineNumber)
                            onDismiss()
                        }
                    )
                    if (lineNumber < 6) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Dialog: Input Bus Number when user confirms they're on bus
 */
@Composable
fun BusNumberConfirmationDialog(
    onDismiss: () -> Unit,
    onBusConfirmed: (String) -> Unit
) {
    var busNumber by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Which Bus?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Enter the bus number you're currently on",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = busNumber,
                    onValueChange = { busNumber = it },
                    label = { Text("Bus Number") },
                    placeholder = { Text("e.g., 12, 25, 101") },
                    leadingIcon = {
                        Icon(Icons.Default.DirectionsBus, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (busNumber.isNotBlank()) {
                                onBusConfirmed(busNumber.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = busNumber.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Start Sharing")
                    }
                }
            }
        }
    }
}

