package mobile.project.escrowx.seller

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.TransactionsActivity
import mobile.project.escrowx.ui.components.SellerNavBar
import mobile.project.escrowx.ui.components.SellerNavItem
import mobile.project.escrowx.ui.components.navigateTab

data class IncomingRequest(
    val id: String,
    val buyerName: String,
    val buyerImage: String? = null,
    val timeAgo: String,
    val productName: String,
    val amount: String,
    val status: String = "Pending"
)

class IncomingEscrowsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                IncomingEscrowsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingEscrowsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var requests by remember { mutableStateOf<List<IncomingRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            if (token.isNullOrBlank()) {
                error = "Please login again"
                isLoading = false
                return@launch
            }

            try {
                delay(1000)
                requests = listOf(
                    IncomingRequest(
                        id = "1",
                        buyerName = "Alice Wanjiku",
                        timeAgo = "2 mins ago",
                        productName = "MacBook Pro M2",
                        amount = "85,000"
                    ),
                    IncomingRequest(
                        id = "2",
                        buyerName = "John Musyoka",
                        timeAgo = "15 mins ago",
                        productName = "iPhone 14 Pro Max",
                        amount = "120,000"
                    )
                )
                isLoading = false
            } catch (e: Exception) {
                error = "Network error: ${e.message}"
                isLoading = false
            }
        }
    }

    fun handleAccept(request: IncomingRequest) {
        requests = requests.filter { it.id != request.id }
        val intent = Intent(context, RequestAcceptedActivity::class.java).apply {
            putExtra("TRANSACTION_ID", request.id)
            putExtra("PRODUCT_NAME", request.productName)
            putExtra("BUYER_NAME", request.buyerName)
            putExtra("AMOUNT", request.amount)
        }
        context.startActivity(intent)
    }

    fun handleReject(request: IncomingRequest) {
        val intent = Intent(context, RejectRequestActivity::class.java).apply {
            putExtra("TRANSACTION_ID", request.id)
            putExtra("PRODUCT_NAME", request.productName)
            putExtra("BUYER_NAME", request.buyerName)
            putExtra("AMOUNT", request.amount)
        }
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Incoming Requests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151C27)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? IncomingEscrowsActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00236F)
                        )
                    }
                },
                // REMOVED: actions = { ... } - No notification bell
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9FF)
                )
            )
        },
        bottomBar = {
            IncomingEscrowsBottomNavigation()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .padding(16.dp)
        ) {
            // Removed the duplicate title since it's now in the top bar

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
                requests.isEmpty() -> {
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
                            "No pending requests",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF151C27)
                        )
                        Text(
                            "You're all caught up. New buyer escrow requests will appear here.",
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
                        items(requests) { request ->
                            IncomingRequestCard(
                                request = request,
                                onAccept = { handleAccept(request) },
                                onReject = { handleReject(request) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingRequestCard(
    request: IncomingRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE7EEFE))
                            .border(1.dp, Color(0xFFC5C5D3), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Buyer",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            request.buyerName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF151C27)
                        )
                        Text(
                            request.timeAgo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF444651)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF5C3800).copy(alpha = 0.1f)
                ) {
                    Text(
                        request.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C3800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0F3FF),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "PRODUCT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444651),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            request.productName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF151C27)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "AMOUNT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444651),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "KES ${request.amount}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00236F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFBA1A1A)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFBA1A1A))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF006C49)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF006C49))
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Accept",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun IncomingEscrowsBottomNavigation() {
    val context = LocalContext.current
    SellerNavBar(
        selectedIndex = SellerNavItem.Transactions.index,
        onItemSelected = { item ->
            when (item) {
                SellerNavItem.Home -> navigateTab(context, SellerDashboardActivity::class.java)
                SellerNavItem.Transactions -> {
                    navigateTab(
                        context,
                        TransactionsActivity::class.java,
                        Bundle().apply { putString("ROLE", "SELLER") }
                    )
                }
                SellerNavItem.Profile -> navigateTab(context, ProfileActivity::class.java)
            }
        }
    )
}