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
import mobile.project.escrowx.auth.SessionManager

// Theme colors matching BuyerDashboard
val EscrowXBackground = Color(0xFFF9F9FF)  // Light purple/white background
val EscrowXPrimary = Color(0xFF00236F)     // Dark blue
val EscrowXSurface = Color(0xFFFFFFFF)      // White

enum class TransactionFilter {
    ALL,
    COMPLETE,
    INCOMPLETE
}

class TransactionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TransactionsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen() {
    val context = LocalContext.current

    var allTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var filteredTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedBottomTab by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        // Only 3 mock transactions
        val mockTransactions = listOf(
            EscrowResponse(
                id = "1",
                reference = "ESC-ABC123",
                buyerId = "buyer1",
                sellerId = "seller1",
                title = "iPhone 15 Pro",
                amount = "125,000",
                currency = "KES",
                status = "COMPLETED",
                createdAt = "2024-01-15T10:30:00",
                updatedAt = "2024-01-20"
            ),
            EscrowResponse(
                id = "2",
                reference = "ESC-DEF456",
                buyerId = "buyer1",
                sellerId = "seller2",
                title = "Samsung 65\" TV",
                amount = "85,000",
                currency = "KES",
                status = "IN_DELIVERY",
                createdAt = "2024-01-10T14:15:00",
                updatedAt = "2024-01-18"
            ),
            EscrowResponse(
                id = "3",
                reference = "ESC-GHI789",
                buyerId = "buyer1",
                sellerId = "seller3",
                title = "MacBook Pro M3",
                amount = "250,000",
                currency = "KES",
                status = "FUNDS_HELD",
                createdAt = "2024-01-05T09:45:00",
                updatedAt = "2024-01-08"
            )
        )

        allTransactions = mockTransactions
        isLoading = false
    }

    LaunchedEffect(allTransactions, selectedFilter) {
        filteredTransactions = when (selectedFilter) {
            TransactionFilter.ALL -> allTransactions
            TransactionFilter.COMPLETE -> allTransactions.filter {
                it.status.equals("COMPLETED", ignoreCase = true)
            }
            TransactionFilter.INCOMPLETE -> allTransactions.filter {
                !it.status.equals("COMPLETED", ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Transactions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EscrowXPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                        (context as? TransactionsActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EscrowXPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = EscrowXPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(80.dp),
                containerColor = Color.White
            ) {
                val items = listOf(
                    "Home" to Icons.Default.Home,
                    "Transactions" to Icons.Default.AccountBalanceWallet,
                    "Profile" to Icons.Default.Person
                )
                items.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedBottomTab == index,
                        onClick = {
                            selectedBottomTab = index
                            when (index) {
                                0 -> {
                                    context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                                    (context as? TransactionsActivity)?.finish()
                                }
                                1 -> { }
                                2 -> {
                                    context.startActivity(Intent(context, ProfileActivity::class.java))
                                }
                            }
                        },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (selectedBottomTab == index) EscrowXPrimary else Color(0xFF9E9E9E)
                            )
                        },
                        label = {
                            Text(
                                label,
                                color = if (selectedBottomTab == index) EscrowXPrimary else Color(0xFF9E9E9E)
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(EscrowXBackground)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.clickable { selectedFilter = TransactionFilter.ALL },
                    shape = RoundedCornerShape(32.dp),
                    color = if (selectedFilter == TransactionFilter.ALL) EscrowXPrimary else Color.White,
                    border = if (selectedFilter != TransactionFilter.ALL)
                        BorderStroke(1.dp, EscrowXPrimary)
                    else null
                ) {
                    Text(
                        text = "All",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = if (selectedFilter == TransactionFilter.ALL) Color.White else EscrowXPrimary,
                        fontWeight = if (selectedFilter == TransactionFilter.ALL) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    modifier = Modifier.clickable { selectedFilter = TransactionFilter.COMPLETE },
                    shape = RoundedCornerShape(32.dp),
                    color = if (selectedFilter == TransactionFilter.COMPLETE) EscrowXPrimary else Color.White,
                    border = if (selectedFilter != TransactionFilter.COMPLETE)
                        BorderStroke(1.dp, EscrowXPrimary)
                    else null
                ) {
                    Text(
                        text = "Complete",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = if (selectedFilter == TransactionFilter.COMPLETE) Color.White else EscrowXPrimary,
                        fontWeight = if (selectedFilter == TransactionFilter.COMPLETE) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    modifier = Modifier.clickable { selectedFilter = TransactionFilter.INCOMPLETE },
                    shape = RoundedCornerShape(32.dp),
                    color = if (selectedFilter == TransactionFilter.INCOMPLETE) EscrowXPrimary else Color.White,
                    border = if (selectedFilter != TransactionFilter.INCOMPLETE)
                        BorderStroke(1.dp, EscrowXPrimary)
                    else null
                ) {
                    Text(
                        text = "Incomplete",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = if (selectedFilter == TransactionFilter.INCOMPLETE) Color.White else EscrowXPrimary,
                        fontWeight = if (selectedFilter == TransactionFilter.INCOMPLETE) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }

            // Transaction Count
            Text(
                text = "${filteredTransactions.size} transaction${if (filteredTransactions.size != 1) "s" else ""}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )

            // Transactions List
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EscrowXPrimary)
                    }
                }
                filteredTransactions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
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
                                onViewDetails = {
                                    Toast.makeText(context, "View Details: ${transaction.title}", Toast.LENGTH_SHORT).show()
                                },
                                onRaiseDispute = {
                                    // Navigate to RaiseDisputeActivity
                                    val intent = Intent(context, RaiseDisputeActivity::class.java)
                                    intent.putExtra("TRANSACTION_ID", transaction.id)
                                    intent.putExtra("TRANSACTION_TITLE", transaction.title)
                                    intent.putExtra("TRANSACTION_AMOUNT", transaction.amount)
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

@Composable
fun TransactionCard(
    transaction: EscrowResponse,
    onViewDetails: () -> Unit,
    onRaiseDispute: () -> Unit
) {
    // Format date
    val formattedDate = try {
        val parts = transaction.createdAt.split("T")
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")
        val month = when (dateParts[1].toInt()) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Jan"
        }
        val day = dateParts[2].toInt()
        val year = dateParts[0]
        "$month $day, $year • ${timeParts[0]}:${timeParts[1]}"
    } catch (e: Exception) {
        transaction.createdAt
    }

    // Determine status properties
    val statusColor = when {
        transaction.status.equals("COMPLETED", ignoreCase = true) -> Color(0xFF10B981)
        transaction.status.equals("IN_DELIVERY", ignoreCase = true) -> Color(0xFFF59E0B)
        transaction.status.equals("FUNDS_HELD", ignoreCase = true) -> Color(0xFF3B82F6)
        transaction.status.equals("CREATED", ignoreCase = true) -> Color(0xFF6B7280)
        transaction.status.equals("CANCELLED", ignoreCase = true) -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }

    val statusDisplay = when {
        transaction.status.equals("COMPLETED", ignoreCase = true) -> "COMPLETED"
        transaction.status.equals("IN_DELIVERY", ignoreCase = true) -> "IN DELIVERY"
        transaction.status.equals("FUNDS_HELD", ignoreCase = true) -> "FUNDS HELD"
        transaction.status.equals("CREATED", ignoreCase = true) -> "CREATED"
        transaction.status.equals("CANCELLED", ignoreCase = true) -> "CANCELLED"
        else -> transaction.status
    }

    val statusMessage = when {
        transaction.status.equals("COMPLETED", ignoreCase = true) -> "Transaction completed successfully"
        transaction.status.equals("IN_DELIVERY", ignoreCase = true) -> "Item is on the way"
        transaction.status.equals("FUNDS_HELD", ignoreCase = true) -> "Waiting for seller confirmation"
        transaction.status.equals("CREATED", ignoreCase = true) -> "Transaction created, awaiting payment"
        transaction.status.equals("CANCELLED", ignoreCase = true) -> "Transaction was cancelled"
        else -> "Processing..."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Status and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusDisplay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }

                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Transaction Title and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Text(
                    text = "KES ${transaction.amount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EscrowXPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Status Message
            Text(
                text = statusMessage,
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))

            // Row 4: Action Icons (View Details and Raise Dispute)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Details Icon (Blue)
                Row(
                    modifier = Modifier
                        .clickable { onViewDetails() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "View Details",
                        modifier = Modifier.size(20.dp),
                        tint = EscrowXPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Details",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EscrowXPrimary
                    )
                }

                // Raise Dispute Icon (Red)
                Row(
                    modifier = Modifier
                        .clickable { onRaiseDispute() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ReportProblem,
                        contentDescription = "Raise Dispute",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dispute",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}