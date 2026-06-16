@file:Suppress("SpellCheckingInspection")
package mobile.project.escrowx.dash

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class DisputeCenterActivity : ComponentActivity() {
    private val viewModel: DisputeViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.fetchUserDisputes(this)

        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentFilter by remember { mutableStateOf(DisputeFilter.ALL) }
                val context = LocalContext.current

                val filteredDisputes = when (currentFilter) {
                    DisputeFilter.ALL -> uiState.disputesList
                    DisputeFilter.COMPLETE -> uiState.disputesList.filter { it.status == DisputeStatus.RESOLVED }
                    DisputeFilter.INCOMPLETE -> uiState.disputesList.filter { it.status != DisputeStatus.RESOLVED }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Dispute Center", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00236F)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                                }
                            },
                            actions = {
                                IconButton(onClick = { }) {
                                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = Color(0xFF00236F))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color(0xFF00236F))
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Color(0xFFF9F9FF))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DisputeFilterChip("All", currentFilter == DisputeFilter.ALL) { currentFilter = DisputeFilter.ALL }
                            DisputeFilterChip("Resolved", currentFilter == DisputeFilter.COMPLETE) { currentFilter = DisputeFilter.COMPLETE }
                            DisputeFilterChip("Active", currentFilter == DisputeFilter.INCOMPLETE) { currentFilter = DisputeFilter.INCOMPLETE }
                        }

                        when {
                            uiState.isLoading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF00236F))
                                }
                            }
                            uiState.errorMessage != null -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(uiState.errorMessage!!, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Spacer(Modifier.height(16.dp))
                                        Button(onClick = { viewModel.fetchUserDisputes(context) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F))) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                            filteredDisputes.isEmpty() -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Gavel, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                        Spacer(Modifier.height(16.dp))
                                        Text("No disputes found", color = Color.Gray)
                                        Text("Your active disputes will appear here", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredDisputes) { dispute ->
                                        DisputeCard(
                                            dispute = dispute,
                                            onClick = {
                                                // TODO: Create a DisputeDetailActivity to view full details
                                                Toast.makeText(context, "View dispute details coming soon", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisputeFilterChip(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = if (active) Color(0xFF00236F) else Color.White,
        border = BorderStroke(1.dp, if (active) Color.Transparent else Color(0xFFC5C5D3)),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (active) Color.White else Color.Black,
            fontSize = 13.sp
        )
    }
}

@Composable
fun DisputeCard(dispute: DisputeItem, onClick: () -> Unit) {
    val statusColor = when (dispute.status) {
        DisputeStatus.OPEN -> Color(0xFFF59E0B)
        DisputeStatus.UNDER_INVESTIGATION -> Color(0xFF3B82F6)
        DisputeStatus.RESOLVED -> Color(0xFF10B981)
    }
    val statusText = when (dispute.status) {
        DisputeStatus.OPEN -> "Open"
        DisputeStatus.UNDER_INVESTIGATION -> "Under Investigation"
        DisputeStatus.RESOLVED -> "Resolved"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dispute.category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = "Transaction: ${dispute.transactionId}", fontSize = 12.sp, color = Color.Gray)
            if (!dispute.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = dispute.description, fontSize = 12.sp, color = Color.DarkGray, maxLines = 2)
            }
            Spacer(Modifier.height(8.dp))
            Text(text = dispute.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
        }
    }
}