package mobile.project.escrowx.dash
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import java.util.Locale

data class IncomingRequest(
    val id: String,
    val sellerName: String,
    val businessName: String,
    val itemTitle: String,
    val amount: String,
    val status: String = "PENDING"
)

class IncomingRequestsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                IncomingRequestsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingRequestsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var incomingRequests by remember { mutableStateOf<List<IncomingRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val buyerId = session.getUserId()
            if (token.isNullOrBlank() || buyerId.isNullOrBlank()) {
                error = "Please login again"
                isLoading = false
                return@launch
            }

            try {
                // Fetching using /api/v1/transactions/buyer/{buyerId}
                val response = RetrofitClient.authenticated(token).getTransactionsByBuyer(buyerId)
                if (response.isSuccessful && response.body() != null) {
                    val allTransactions = response.body()!!
                    // Filter for CREATED status which represents incoming requests for the buyer
                    incomingRequests = allTransactions.filter {
                        it.status.equals("CREATED", ignoreCase = true) 
                    }.map { txn ->
                        val amountVal = txn.amount
                        IncomingRequest(
                            id = txn.id,
                            sellerName = "Seller ${txn.sellerId.take(6)}",
                            businessName = "Business",
                            itemTitle = txn.title,
                            amount = String.format(Locale.getDefault(), "%,.0f", amountVal),
                            status = txn.status
                        )
                    }
                } else {
                    error = "Failed to load requests: ${response.code()}"
                }
                isLoading = false
            } catch (e: Exception) {
                error = "Network error: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Incoming Requests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00236F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? IncomingRequestsActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00236F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9FF)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .padding(16.dp)
        ) {
            Text(
                "Pending Your Action",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF151C27)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "${incomingRequests.size} request${if (incomingRequests.size != 1) "s" else ""} awaiting your response",
                fontSize = 13.sp,
                color = Color(0xFF444651)
            )

            Spacer(modifier = Modifier.height(20.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00236F))
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error!!, color = Color.Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    error = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F))
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                incomingRequests.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE7EEFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = "No requests",
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF757682)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No incoming requests",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF151C27)
                        )
                        Text(
                            "When a seller initiates an escrow for you, it will appear here.",
                            fontSize = 13.sp,
                            color = Color(0xFF444651),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(incomingRequests) { request ->
                            IncomingRequestFullCard(
                                request = request,
                                onAccept = {
                                    val intent = Intent(context, TransactionDetailsActivity::class.java).apply {
                                        putExtra("TRANSACTION_ID", request.id)
                                        putExtra("SELLER_NAME", request.sellerName)
                                        putExtra("BUSINESS_NAME", request.businessName)
                                        putExtra("ITEM_TITLE", request.itemTitle)
                                        putExtra("AMOUNT", request.amount)
                                        putExtra("INSPECTION_DAYS", 3)
                                    }
                                    context.startActivity(intent)
                                },
                                onDecline = {
                                    Toast.makeText(context, "Request declined: ${request.itemTitle}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingRequestFullCard(
    request: IncomingRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE7EEFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Store",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            request.businessName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF151C27)
                        )
                        Text(
                            request.itemTitle,
                            fontSize = 13.sp,
                            color = Color(0xFF444651)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        "PENDING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "KES ${request.amount}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00236F)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFBA1A1A)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFBA1A1A))
                ) {
                    Text("Decline", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00236F),
                        contentColor = Color.White
                    )
                ) {
                    Text("Accept", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}