package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
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
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.ui.components.BuyerNavBar
import mobile.project.escrowx.ui.components.BuyerNavItem
import mobile.project.escrowx.ui.components.navigateTab

class BuyerTransactionDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "Product"
        val sellerName = intent.getStringExtra("SELLER_NAME") ?: "Seller"
        val amount = intent.getStringExtra("AMOUNT") ?: "0"
        val orderId = intent.getStringExtra("ORDER_ID") ?: "ESC-XXXX"
        val date = intent.getStringExtra("DATE") ?: ""
        val shippingAddress = intent.getStringExtra("SHIPPING_ADDRESS") ?: ""
        val currentStatus = intent.getStringExtra("STATUS") ?: "FUNDS_HELD"

        setContent {
            MaterialTheme {
                BuyerTransactionDetailScreen(
                    transactionId = transactionId,
                    productName = productName,
                    sellerName = sellerName,
                    amount = amount,
                    orderId = orderId,
                    date = date,
                    shippingAddress = shippingAddress,
                    initialStatus = currentStatus
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerTransactionDetailScreen(
    transactionId: String,
    productName: String,
    sellerName: String,
    amount: String,
    orderId: String,
    date: String,
    shippingAddress: String,
    initialStatus: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)
    var currentStatus by remember { mutableStateOf(initialStatus) }
    var isUpdating by remember { mutableStateOf(false) }
    var canConfirmReceipt by remember { mutableStateOf(initialStatus.equals("DELIVERED", ignoreCase = true)) }
    var canCancel by remember {
        mutableStateOf(initialStatus in listOf("CREATED", "FUNDS_HELD", "SELLER_ACCEPTED", "IN_DELIVERY"))
    }

    fun getStatusStep(): Int {
        return when {
            currentStatus.equals("FUNDS_HELD", ignoreCase = true) -> 1
            currentStatus.equals("SELLER_ACCEPTED", ignoreCase = true) -> 1
            currentStatus.equals("IN_DELIVERY", ignoreCase = true) -> 2
            currentStatus.equals("DELIVERED", ignoreCase = true) -> 3
            currentStatus.equals("COMPLETED", ignoreCase = true) -> 4
            else -> 1
        }
    }

    fun getStatusDisplay(): String {
        return when {
            currentStatus.equals("FUNDS_HELD", ignoreCase = true) -> "Funds Held"
            currentStatus.equals("SELLER_ACCEPTED", ignoreCase = true) -> "Seller Accepted"
            currentStatus.equals("IN_DELIVERY", ignoreCase = true) -> "In Delivery"
            currentStatus.equals("DELIVERED", ignoreCase = true) -> "Delivered - Confirm Receipt"
            currentStatus.equals("COMPLETED", ignoreCase = true) -> "Completed"
            else -> currentStatus
        }
    }

    fun getStatusColor(): Color {
        return when {
            currentStatus.equals("FUNDS_HELD", ignoreCase = true) -> Color(0xFF00236F)
            currentStatus.equals("IN_DELIVERY", ignoreCase = true) -> Color(0xFFF59E0B)
            currentStatus.equals("DELIVERED", ignoreCase = true) -> Color(0xFF10B981)
            currentStatus.equals("COMPLETED", ignoreCase = true) -> Color(0xFF006C49)
            else -> Color(0xFF444651)
        }
    }

    fun confirmReceipt() {
        isUpdating = true
        scope.launch {
            try {
                val token = session.getAccessToken()
                val buyerId = session.getUserId()
                if (token.isNullOrBlank() || buyerId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    isUpdating = false
                    return@launch
                }
                // TODO: Uncomment when backend endpoint is ready
                // val response = RetrofitClient.authenticated(token).confirmReceipt(transactionId, buyerId)
                Toast.makeText(context, "Receipt confirmed! Funds released to seller.", Toast.LENGTH_LONG).show()
                currentStatus = "COMPLETED"
                canConfirmReceipt = false
                canCancel = false
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    fun cancelTransaction() {
        isUpdating = true
        scope.launch {
            try {
                val token = session.getAccessToken()
                val buyerId = session.getUserId()
                if (token.isNullOrBlank() || buyerId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
                    isUpdating = false
                    return@launch
                }
                Toast.makeText(context, "Transaction cancelled. Refund initiated.", Toast.LENGTH_LONG).show()
                currentStatus = "CANCELLED"
                canCancel = false
                canConfirmReceipt = false
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    fun raiseDispute() {
        val intent = Intent(context, RaiseDisputeActivity::class.java).apply {
            putExtra("TRANSACTION_ID", transactionId)
            putExtra("TRANSACTION_TITLE", productName)
            putExtra("TRANSACTION_AMOUNT", amount)
        }
        context.startActivity(intent)
    }

    val currentStep = getStatusStep()
    val isCompleted = currentStatus.equals("COMPLETED", ignoreCase = true) || currentStatus.equals("CANCELLED", ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transaction ${orderId.takeLast(4)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00236F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? BuyerTransactionDetailActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FF))
            )
        },
        bottomBar = {
            BuyerTransactionDetailBottomNavigation()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Transaction Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                "BUYING ITEM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFF444651)
                            )
                            Text(
                                productName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF151C27)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = getStatusColor().copy(alpha = 0.1f)
                        ) {
                            Text(
                                getStatusDisplay(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = getStatusColor()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE7EEFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = "Seller",
                                tint = Color(0xFF00236F),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                "Seller: $sellerName",
                                fontSize = 12.sp,
                                color = Color(0xFF444651)
                            )
                            Text(
                                "KES $amount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00236F)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Stepper
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "TRANSACTION PROGRESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF444651)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BuyerProgressStep(
                            label = "Paid",
                            isActive = currentStep >= 1,
                            isCompleted = currentStep > 1,
                            icon = Icons.Default.Check
                        )
                        BuyerProgressStep(
                            label = "Shipping",
                            isActive = currentStep >= 2,
                            isCompleted = currentStep > 2,
                            icon = Icons.Default.LocalShipping
                        )
                        BuyerProgressStep(
                            label = "Delivery",
                            isActive = currentStep >= 3,
                            isCompleted = currentStep > 3,
                            icon = Icons.Default.Inventory
                        )
                        BuyerProgressStep(
                            label = "Release",
                            isActive = currentStep >= 4,
                            isCompleted = currentStep > 4,
                            icon = Icons.Default.LockOpen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ORDER DETAILS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF444651)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Order ID", fontSize = 11.sp, color = Color(0xFF444651))
                            Text(
                                orderId,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF151C27)
                            )
                        }
                        Column {
                            Text("Date", fontSize = 11.sp, color = Color(0xFF444651))
                            Text(
                                date,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF151C27)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Shipping Address", fontSize = 11.sp, color = Color(0xFF444651))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = Color(0xFF00236F))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            shippingAddress,
                            fontSize = 13.sp,
                            color = Color(0xFF151C27)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact Seller Button
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Message seller coming soon", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00236F)),
                border = BorderStroke(1.dp, Color(0xFF00236F))
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message Seller", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Sticky Bottom Action Buttons (Buyer actions)
        if (!isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
                    .wrapContentSize(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (canConfirmReceipt) {
                        Button(
                            onClick = { confirmReceipt() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF006C49),
                                contentColor = Color.White
                            ),
                            enabled = !isUpdating
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirming...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirm Receipt & Release Funds", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (canCancel) {
                        OutlinedButton(
                            onClick = { cancelTransaction() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A)),
                            border = BorderStroke(1.dp, Color(0xFFBA1A1A)),
                            enabled = !isUpdating
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Transaction", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    OutlinedButton(
                        onClick = { raiseDispute() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Raise Dispute", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.BuyerProgressStep(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Color(0xFF006C49)
                        isActive -> Color(0xFF00236F)
                        else -> Color(0xFFE7EEFE)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            } else {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = if (isActive) Color.White else Color(0xFF444651)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color(0xFF00236F) else Color(0xFF444651)
        )
    }
}

@Composable
fun BuyerTransactionDetailBottomNavigation() {
    val context = LocalContext.current
    BuyerNavBar(
        selectedIndex = BuyerNavItem.Home.index,
        onItemSelected = { item ->
            when (item) {
                BuyerNavItem.Home -> navigateTab(context, BuyerDashboardActivity::class.java)
                BuyerNavItem.Transactions -> {
                    navigateTab(
                        context,
                        TransactionsActivity::class.java,
                        Bundle().apply { putString("ROLE", "BUYER") }
                    )
                }
                BuyerNavItem.Profile -> navigateTab(context, ProfileActivity::class.java)
            }
        }
    )
}