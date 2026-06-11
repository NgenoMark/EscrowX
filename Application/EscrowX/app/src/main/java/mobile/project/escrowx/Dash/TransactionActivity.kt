package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.seller.SellerTransactionDetailActivity

class TransactionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = intent.getStringExtra("ROLE") ?: "BUYER"
        setContent {
            MaterialTheme {
                TransactionsScreen(role = role)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(role: String) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()

    var allTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var filteredTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedBottomTab by remember { mutableIntStateOf(1) }

    // Dummy data for BUYER
    fun getDummyBuyerTransactions(): List<EscrowResponse> {
        return listOf(
            EscrowResponse(
                id = "1", reference = "ESC-BUY-001", buyerId = "buyer1", sellerId = "seller1",
                title = "iPhone 15 Pro Max", amount = "165000", currency = "KES",
                status = "IN_DELIVERY", createdAt = "2024-06-05T10:30:00", updatedAt = "2024-06-07T14:00:00"
            ),
            EscrowResponse(
                id = "2", reference = "ESC-BUY-002", buyerId = "buyer1", sellerId = "seller2",
                title = "MacBook Air M2", amount = "125000", currency = "KES",
                status = "COMPLETED", createdAt = "2024-05-20T09:00:00", updatedAt = "2024-05-28T16:30:00"
            ),
            EscrowResponse(
                id = "3", reference = "ESC-BUY-003", buyerId = "buyer1", sellerId = "seller3",
                title = "Sony WH-1000XM5", amount = "42500", currency = "KES",
                status = "FUNDS_HELD", createdAt = "2024-06-08T08:15:00", updatedAt = "2024-06-08T08:15:00"
            ),
            EscrowResponse(
                id = "4", reference = "ESC-BUY-004", buyerId = "buyer1", sellerId = "seller4",
                title = "Samsung 4K Monitor", amount = "75000", currency = "KES",
                status = "DELIVERED", createdAt = "2024-06-01T11:00:00", updatedAt = "2024-06-09T09:30:00"
            )
        )
    }

    // Dummy data for SELLER
    fun getDummySellerTransactions(): List<EscrowResponse> {
        return listOf(
            EscrowResponse(
                id = "101", reference = "ESC-SELL-001", buyerId = "buyerA", sellerId = "seller1",
                title = "Sold: iPhone 15 Pro Max", amount = "165000", currency = "KES",
                status = "IN_DELIVERY", createdAt = "2024-06-05T10:30:00", updatedAt = "2024-06-07T14:00:00"
            ),
            EscrowResponse(
                id = "102", reference = "ESC-SELL-002", buyerId = "buyerB", sellerId = "seller1",
                title = "Sold: MacBook Air M2", amount = "125000", currency = "KES",
                status = "COMPLETED", createdAt = "2024-05-20T09:00:00", updatedAt = "2024-05-28T16:30:00"
            ),
            EscrowResponse(
                id = "103", reference = "ESC-SELL-003", buyerId = "buyerC", sellerId = "seller1",
                title = "Sold: Sony Headphones", amount = "42500", currency = "KES",
                status = "FUNDS_HELD", createdAt = "2024-06-08T08:15:00", updatedAt = "2024-06-08T08:15:00"
            ),
            EscrowResponse(
                id = "104", reference = "ESC-SELL-004", buyerId = "buyerD", sellerId = "seller1",
                title = "Sold: Samsung Monitor", amount = "75000", currency = "KES",
                status = "DELIVERED", createdAt = "2024-06-01T11:00:00", updatedAt = "2024-06-09T09:30:00"
            )
        )
    }

    LaunchedEffect(role) {
        scope.launch {
            val token = session.getAccessToken()
            val userId = session.getUserId()
            if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                allTransactions = if (role == "BUYER") getDummyBuyerTransactions() else getDummySellerTransactions()
                isLoading = false
                return@launch
            }

            try {
                val api = RetrofitClient.authenticated(token)
                val response = api.listTransactions(
                    role = role.uppercase(),
                    status = null,
                    userId = userId,
                    dateFrom = null,
                    dateTo = null,
                    page = 0,
                    size = 50
                )
                if (response.isSuccessful && response.body() != null) {
                    val transactions = response.body()?.content ?: emptyList()
                    if (transactions.isNotEmpty()) {
                        allTransactions = transactions
                    } else {
                        allTransactions = if (role == "BUYER") getDummyBuyerTransactions() else getDummySellerTransactions()
                    }
                } else {
                    allTransactions = if (role == "BUYER") getDummyBuyerTransactions() else getDummySellerTransactions()
                }
            } catch (e: Exception) {
                allTransactions = if (role == "BUYER") getDummyBuyerTransactions() else getDummySellerTransactions()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(allTransactions, selectedFilter) {
        filteredTransactions = when (selectedFilter) {
            TransactionFilter.ALL -> allTransactions
            TransactionFilter.COMPLETE -> allTransactions.filter { it.status.equals("COMPLETED", ignoreCase = true) }
            TransactionFilter.INCOMPLETE -> allTransactions.filter { !it.status.equals("COMPLETED", ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Transactions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (role == "BUYER") context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                        else context.startActivity(Intent(context, SellerDashboardActivity::class.java))
                        (context as? TransactionsActivity)?.finish()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(80.dp), containerColor = Color.White) {
                val items = listOf("Home" to Icons.Default.Home, "Transactions" to Icons.Default.AccountBalanceWallet, "Profile" to Icons.Default.Person)
                items.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedBottomTab == index,
                        onClick = {
                            selectedBottomTab = index
                            when (index) {
                                0 -> {
                                    if (role == "BUYER") context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                                    else context.startActivity(Intent(context, SellerDashboardActivity::class.java))
                                }
                                1 -> { /* already here */ }
                                2 -> context.startActivity(Intent(context, ProfileActivity::class.java))
                            }
                        },
                        icon = { Icon(icon, contentDescription = label, tint = if (selectedBottomTab == index) Color(0xFF00236F) else Color.Gray) },
                        label = { Text(label, color = if (selectedBottomTab == index) Color(0xFF00236F) else Color.Gray, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF9F9FF))
        ) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChipButton("All", selectedFilter == TransactionFilter.ALL) { selectedFilter = TransactionFilter.ALL }
                FilterChipButton("Complete", selectedFilter == TransactionFilter.COMPLETE) { selectedFilter = TransactionFilter.COMPLETE }
                FilterChipButton("Incomplete", selectedFilter == TransactionFilter.INCOMPLETE) { selectedFilter = TransactionFilter.INCOMPLETE }
            }

            Text(
                "${filteredTransactions.size} transaction${if (filteredTransactions.size != 1) "s" else ""}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 12.sp,
                color = Color.Gray
            )

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00236F))
                    }
                }
                filteredTransactions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Text("No transactions found", color = Color.Gray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTransactions) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                role = role,
                                onViewDetails = {
                                    if (role == "BUYER") {
                                        val intent = Intent(context, BuyerTransactionDetailActivity::class.java).apply {
                                            putExtra("TRANSACTION_ID", transaction.id)
                                            putExtra("PRODUCT_NAME", transaction.title)
                                            putExtra("SELLER_NAME", "Seller")
                                            putExtra("AMOUNT", transaction.amount)
                                            putExtra("ORDER_ID", transaction.reference)
                                            putExtra("DATE", transaction.createdAt.take(10))
                                            putExtra("SHIPPING_ADDRESS", "Nairobi, Kenya")
                                            putExtra("STATUS", transaction.status)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        val currentStep = when (transaction.status.uppercase()) {
                                            "FUNDS_HELD" -> 1
                                            "IN_DELIVERY" -> 2
                                            "DELIVERED" -> 3
                                            "COMPLETED" -> 4
                                            else -> 1
                                        }
                                        val intent = Intent(context, SellerTransactionDetailActivity::class.java).apply {
                                            putExtra("TRANSACTION_ID", transaction.id)
                                            putExtra("PRODUCT_NAME", transaction.title)
                                            putExtra("BUYER_NAME", "Buyer")
                                            putExtra("BUYER_INITIALS", "BY")
                                            putExtra("AMOUNT", transaction.amount)
                                            putExtra("ORDER_ID", transaction.reference)
                                            putExtra("DATE", transaction.createdAt.take(10))
                                            putExtra("SHIPPING_ADDRESS", "Nairobi, Kenya")
                                            putExtra("CURRENT_STEP", currentStep)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                onRaiseDispute = {
                                    if (role == "BUYER") {
                                        val intent = Intent(context, RaiseDisputeActivity::class.java).apply {
                                            putExtra("TRANSACTION_ID", transaction.id)
                                            putExtra("TRANSACTION_TITLE", transaction.title)
                                            putExtra("TRANSACTION_AMOUNT", transaction.amount)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "Sellers can raise disputes via support", Toast.LENGTH_SHORT).show()
                                    }
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
fun FilterChipButton(text: String, active: Boolean, onClick: () -> Unit) {
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
fun TransactionCard(
    transaction: EscrowResponse,
    role: String,
    onViewDetails: () -> Unit,
    onRaiseDispute: () -> Unit
) {
    val statusColor = when {
        transaction.status.equals("COMPLETED", ignoreCase = true) -> Color(0xFF10B981)
        transaction.status.equals("IN_DELIVERY", ignoreCase = true) -> Color(0xFFF59E0B)
        transaction.status.equals("FUNDS_HELD", ignoreCase = true) -> Color(0xFF3B82F6)
        transaction.status.equals("CREATED", ignoreCase = true) -> Color(0xFF6B7280)
        transaction.status.equals("CANCELLED", ignoreCase = true) -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }

    val formattedDate = try {
        val parts = transaction.createdAt.split("T")
        val dateParts = parts[0].split("-")
        "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
    } catch (_: Exception) {
        transaction.createdAt
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(transaction.status, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = statusColor)
                }
                Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(transaction.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Text("KES ${transaction.amount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onViewDetails() }.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(20.dp), tint = Color(0xFF00236F))
                    Spacer(Modifier.width(4.dp))
                    Text("Details", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00236F))
                }
                // ✅ Buyer can dispute even if transaction is COMPLETED (only cancelled is excluded)
                if (role == "BUYER" && !transaction.status.equals("CANCELLED", ignoreCase = true)) {
                    Row(
                        modifier = Modifier.clickable { onRaiseDispute() }.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ReportProblem, null, modifier = Modifier.size(20.dp), tint = Color(0xFFEF4444))
                        Spacer(Modifier.width(4.dp))
                        Text("Dispute", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

enum class TransactionFilter { ALL, COMPLETE, INCOMPLETE }