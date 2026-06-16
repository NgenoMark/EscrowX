package mobile.project.escrowx.seller

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
import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.TransactionsActivity
import mobile.project.escrowx.ui.components.SellerNavBar
import mobile.project.escrowx.ui.components.SellerNavItem
import mobile.project.escrowx.ui.components.navigateTab

class SellerTransactionDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "iPhone 15 Pro Max"
        val buyerName = intent.getStringExtra("BUYER_NAME") ?: "Kwami Chen"
        val buyerInitials = intent.getStringExtra("BUYER_INITIALS") ?: "KC"
        val amount = intent.getStringExtra("AMOUNT") ?: "145,000"
        val orderId = intent.getStringExtra("ORDER_ID") ?: "ESC-8294-PRO"
        val date = intent.getStringExtra("DATE") ?: "24 Oct, 2023"
        val shippingAddress = intent.getStringExtra("SHIPPING_ADDRESS") ?: "Westlands Hub, Ground Floor Wing A, Nairobi, Kenya"
        val currentStep = intent.getIntExtra("CURRENT_STEP", 1)

        setContent {
            MaterialTheme {
                TransactionDetailScreen(
                    transactionId = transactionId,
                    productName = productName,
                    buyerName = buyerName,
                    buyerInitials = buyerInitials,
                    amount = amount,
                    orderId = orderId,
                    date = date,
                    shippingAddress = shippingAddress,
                    initialStep = currentStep
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    productName: String,
    buyerName: String,
    buyerInitials: String,
    amount: String,
    orderId: String,
    date: String,
    shippingAddress: String,
    initialStep: Int
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(initialStep) }
    var isUpdating by remember { mutableStateOf(false) }

    fun getNextAction(): Pair<String, String>? {
        return when (currentStep) {
            1 -> Pair("Mark as Shipped", "shipped")
            2 -> Pair("Mark as Delivered", "delivered")
            3 -> Pair("Release Payment", "release")
            else -> null
        }
    }

    fun updateStatus() {
        isUpdating = true
        // Simulate API call
        androidx.compose.runtime.snapshots.SnapshotStateList<Int>()
        // In real implementation, call API here
        when (currentStep) {
            1 -> currentStep = 2
            2 -> currentStep = 3
            3 -> currentStep = 4
        }
        Toast.makeText(context, "Status updated successfully", Toast.LENGTH_SHORT).show()
        isUpdating = false
    }

    val nextAction = getNextAction()
    val isCompleted = currentStep == 4

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
                    IconButton(onClick = { (context as? SellerTransactionDetailActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FF))
            )
        },
        bottomBar = {
            TransactionDetailBottomNavigation()
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
                                "SELLING ITEM",
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
                            color = if (currentStep == 1) Color(0xFF6CF8BB).copy(alpha = 0.1f) else Color(0xFFE7EEFE)
                        ) {
                            Text(
                                when (currentStep) {
                                    1 -> "Paid - Ship Now"
                                    2 -> "In Transit"
                                    3 -> "Out for Delivery"
                                    else -> "Completed"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (currentStep) {
                                    1 -> Color(0xFF006C49)
                                    2 -> Color(0xFF3E2400)
                                    else -> Color(0xFF00236F)
                                }
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
                                Icons.Default.PhoneIphone,
                                contentDescription = "Product",
                                tint = Color(0xFF00236F),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                "Buyer: $buyerName",
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
                        TransactionProgressStep(
                            stepNumber = 1,
                            label = "Paid",
                            isActive = currentStep >= 1,
                            isCompleted = currentStep > 1,
                            icon = Icons.Default.Check
                        )
                        TransactionProgressStep(
                            stepNumber = 2,
                            label = "Shipping",
                            isActive = currentStep >= 2,
                            isCompleted = currentStep > 2,
                            icon = Icons.Default.LocalShipping
                        )
                        TransactionProgressStep(
                            stepNumber = 3,
                            label = "Delivery",
                            isActive = currentStep >= 3,
                            isCompleted = currentStep > 3,
                            icon = Icons.Default.Inventory
                        )
                        TransactionProgressStep(
                            stepNumber = 4,
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

            // Message Buyer Button
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Message buyer coming soon", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00236F)),
                border = BorderStroke(1.dp, Color(0xFF00236F))
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message Buyer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Sticky Bottom Action Button
        if (nextAction != null && !isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
                    .wrapContentSize(Alignment.BottomCenter)
            ) {
                Button(
                    onClick = { updateStatus() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00236F),
                        contentColor = Color.White
                    ),
                    enabled = !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Updating...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            when (nextAction.second) {
                                "shipped" -> Icons.Default.LocalShipping
                                "delivered" -> Icons.Default.Inventory
                                else -> Icons.Default.LockOpen
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(nextAction.first, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Completed message
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
                    .wrapContentSize(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF006C49))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transaction Completed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TransactionProgressStep(
    stepNumber: Int,
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
fun TransactionDetailBottomNavigation() {
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