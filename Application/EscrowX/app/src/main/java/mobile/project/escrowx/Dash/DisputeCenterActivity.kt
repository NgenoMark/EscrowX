@file:Suppress("SpellCheckingInspection")
package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
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

        // Fetch disputes when activity starts
        viewModel.fetchUserDisputes(this)

        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentFilter by remember { mutableStateOf(DisputeFilter.ALL) }
                val context = LocalContext.current

                // Filter disputes based on selection
                val filteredDisputes = when (currentFilter) {
                    DisputeFilter.ALL -> uiState.disputesList
                    DisputeFilter.COMPLETE -> uiState.disputesList.filter {
                        it.status == DisputeStatus.RESOLVED
                    }
                    DisputeFilter.INCOMPLETE -> uiState.disputesList.filter {
                        it.status != DisputeStatus.RESOLVED
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Dispute Center",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF00236F)
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF00236F)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { /* Help action */ }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "Help",
                                        tint = Color(0xFF00236F)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = Color(0xFF00236F)
                            )
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Color(0xFFF9F9FF))
                    ) {
                        // Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChipButton(
                                text = "All",
                                active = currentFilter == DisputeFilter.ALL,
                                onClick = { currentFilter = DisputeFilter.ALL }
                            )
                            FilterChipButton(
                                text = "Resolved",
                                active = currentFilter == DisputeFilter.COMPLETE,
                                onClick = { currentFilter = DisputeFilter.COMPLETE }
                            )
                            FilterChipButton(
                                text = "Active",
                                active = currentFilter == DisputeFilter.INCOMPLETE,
                                onClick = { currentFilter = DisputeFilter.INCOMPLETE }
                            )
                        }

                        // Disputes List
                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF00236F))
                                }
                            }
                            uiState.errorMessage != null -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            uiState.errorMessage!!,
                                            color = Color.Red,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.fetchUserDisputes(context) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F))
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                            filteredDisputes.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Gavel,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No disputes found", color = Color.Gray)
                                        Text(
                                            "Your active disputes will appear here",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
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
                                                // Navigate to dispute details
                                                val intent = Intent(context, RaiseDisputeActivity::class.java)
                                                intent.putExtra("TRANSACTION_ID", dispute.txnId)
                                                intent.putExtra("TRANSACTION_TITLE", dispute.title)
                                                intent.putExtra("TRANSACTION_AMOUNT", dispute.amount)
                                                context.startActivity(intent)
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
private fun FilterChipButton(text: String, active: Boolean, onClick: () -> Unit) {
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
        DisputeStatus.UNDER_INVESTIGATION -> Color(0xFFF59E0B)
        DisputeStatus.AWAITING_EVIDENCE -> Color(0xFF3B82F6)
        DisputeStatus.RESOLVED -> Color(0xFF10B981)
    }

    val statusText = when (dispute.status) {
        DisputeStatus.UNDER_INVESTIGATION -> "Under Investigation"
        DisputeStatus.AWAITING_EVIDENCE -> "Awaiting Evidence"
        DisputeStatus.RESOLVED -> "Resolved"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = dispute.title,
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Transaction: ${dispute.txnId}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dispute.amount,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00236F)
                )

                if (dispute.isRefund) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Refund Issued",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}