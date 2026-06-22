package mobile.project.escrowx.seller

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.DisputeDetailsResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.DisputeDetailsActivity
import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.TransactionsActivity
import mobile.project.escrowx.ui.components.SellerNavBar
import mobile.project.escrowx.ui.components.SellerNavItem
import mobile.project.escrowx.ui.components.navigateTab
import mobile.project.escrowx.ui.theme.BrandBlue
import java.text.NumberFormat
import java.util.*

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
        val currentStatus = intent.getStringExtra("STATUS") ?: statusFromSellerStep(currentStep)

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                SellerTransactionDetailScreen(
                    transactionId = transactionId,
                    productName = productName,
                    buyerName = buyerName,
                    buyerInitials = buyerInitials,
                    amount = amount,
                    orderId = orderId,
                    date = date,
                    shippingAddress = shippingAddress,
                    initialStep = currentStep,
                    initialStatus = currentStatus
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerTransactionDetailScreen(
    transactionId: String,
    productName: String,
    buyerName: String,
    buyerInitials: String,
    amount: String,
    orderId: String,
    date: String,
    shippingAddress: String,
    initialStep: Int,
    initialStatus: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var currentStatus by remember { mutableStateOf(initialStatus) }
    var displayProductName by remember { mutableStateOf(productName) }
    var displayAmount by remember { mutableStateOf(amount) }
    var displayOrderId by remember { mutableStateOf(orderId) }
    var displayDate by remember { mutableStateOf(date) }
    var displayShippingAddress by remember { mutableStateOf(shippingAddress) }
    val normalizedCurrentStatus = remember(currentStatus) { normalizeSellerEscrowStatus(currentStatus) }
    var isUpdating by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isPollingPayment by remember { mutableStateOf(false) }
    var showBuyerInfo by remember { mutableStateOf(false) }
    var disputeDetails by remember { mutableStateOf<DisputeDetailsResponse?>(null) }

    val parsedAmount = displayAmount.replace(",", "").toDoubleOrNull() ?: 0.0
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
        .format(parsedAmount)
        .replace("KES", "KES")

    // ===================== STATUS HELPERS =====================
    fun getStatusDisplay(): String {
        return when (normalizedCurrentStatus) {
            "PENDING_PAYMENT" -> "Awaiting Payment"
            "FUNDS_HELD" -> "Funds Secured"
            "SELLER_ACCEPTED" -> "Order Accepted"
            "IN_DELIVERY" -> "In Transit"
            "SELLER_DELIVERED" -> "Delivered"
            "BUYER_CONFIRMED_DELIVERED" -> "Awaiting Release"
            "RELEASE_PENDING" -> "Release Pending"
            "RELEASE_PROCESSING" -> "Processing"
            "COMPLETED" -> "Completed ✓"
            "CANCELLED" -> "Cancelled"
            "DECLINED" -> "Declined"
            "DISPUTED" -> "Disputed"
            "REFUNDED" -> "Refunded"
            else -> prettySellerEscrowState(normalizedCurrentStatus)
        }
    }

    fun getStatusConfig(): SellerStatusConfig {
        return when (normalizedCurrentStatus) {
            "PENDING_PAYMENT" -> SellerStatusConfig(
                "Awaiting Payment",
                Color(0xFFF59E0B),
                Color(0xFFF59E0B),
                Color(0xFFF59E0B).copy(alpha = 0.12f),
                Icons.Default.Pending
            )
            "FUNDS_HELD" -> SellerStatusConfig(
                "Funds Secured",
                Color(0xFF3B82F6),
                Color(0xFF3B82F6),
                Color(0xFF3B82F6).copy(alpha = 0.12f),
                Icons.Default.AccountBalanceWallet
            )
            "SELLER_ACCEPTED" -> SellerStatusConfig(
                "Order Accepted",
                BrandBlue,
                BrandBlue,
                BrandBlue.copy(alpha = 0.12f),
                Icons.Default.CheckCircle
            )
            "IN_DELIVERY" -> SellerStatusConfig(
                "In Transit",
                Color(0xFF8B5CF6),
                Color(0xFF8B5CF6),
                Color(0xFF8B5CF6).copy(alpha = 0.12f),
                Icons.Default.LocalShipping
            )
            "SELLER_DELIVERED" -> SellerStatusConfig(
                "Delivered",
                Color(0xFF10B981),
                Color(0xFF10B981),
                Color(0xFF10B981).copy(alpha = 0.12f),
                Icons.Default.Inventory
            )
            "BUYER_CONFIRMED_DELIVERED" -> SellerStatusConfig(
                "Awaiting Release",
                Color(0xFF06B6D4),
                Color(0xFF06B6D4),
                Color(0xFF06B6D4).copy(alpha = 0.12f),
                Icons.Default.Payments
            )
            "COMPLETED" -> SellerStatusConfig(
                "Completed ✓",
                Color(0xFF10B981),
                Color(0xFF10B981),
                Color(0xFF10B981).copy(alpha = 0.12f),
                Icons.Default.CheckCircle
            )
            "CANCELLED" -> SellerStatusConfig(
                "Cancelled",
                Color(0xFF5E5E66),
                Color(0xFF5E5E66),
                Color(0xFF5E5E66).copy(alpha = 0.12f),
                Icons.Default.Cancel
            )
            "DISPUTED" -> SellerStatusConfig(
                "Disputed",
                Color(0xFFDC2626),
                Color(0xFFDC2626),
                Color(0xFFDC2626).copy(alpha = 0.12f),
                Icons.Default.Warning
            )
            else -> SellerStatusConfig(
                prettySellerEscrowState(normalizedCurrentStatus),
                Color(0xFF444651),
                Color(0xFF444651),
                Color(0xFF444651).copy(alpha = 0.12f),
                Icons.Default.Info
            )
        }
    }

    fun getStatusStep(): Int {
        val flow = listOf("PENDING_PAYMENT", "FUNDS_HELD", "SELLER_ACCEPTED", "IN_DELIVERY",
            "SELLER_DELIVERED", "BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING",
            "RELEASE_PROCESSING", "COMPLETED")
        return flow.indexOf(normalizedCurrentStatus).coerceAtLeast(0)
    }

    fun getNextAction(): SellerAction? {
        return when (normalizedCurrentStatus) {
            "PENDING_PAYMENT" -> SellerAction("Check Payment", "CHECK_PAYMENT", Icons.Default.Refresh, Color(0xFFF59E0B))
            "FUNDS_HELD" -> SellerAction("Accept Order", "ACCEPT", Icons.Default.CheckCircle, BrandBlue)
            "SELLER_ACCEPTED" -> SellerAction("Mark In Transit", "IN_DELIVERY", Icons.Default.LocalShipping, Color(0xFF8B5CF6))
            "IN_DELIVERY" -> SellerAction("Confirm Delivery", "SELLER_DELIVERED", Icons.Default.Inventory, Color(0xFF10B981))
            else -> null
        }
    }

    // ===================== API FUNCTIONS =====================
    suspend fun pollPendingPayment(api: mobile.project.escrowx.AuthApiService) {
        isPollingPayment = true
        try {
            repeat(12) {
                delay(5000)
                val txnResp = api.getTransactionById(transactionId)
                val latestStatus = txnResp.body()?.status?.trim()?.uppercase(Locale.getDefault())
                if (!latestStatus.isNullOrBlank()) {
                    currentStatus = latestStatus
                }
                if (latestStatus == "FUNDS_HELD") {
                    Toast.makeText(context, "✅ Buyer payment confirmed! Funds are secured.", Toast.LENGTH_LONG).show()
                    return
                }
            }
            Toast.makeText(context, "⏳ Still waiting for buyer payment. Please check again shortly.", Toast.LENGTH_LONG).show()
        } finally {
            isPollingPayment = false
        }
    }

    fun refreshSellerTransactionData(showToast: Boolean = true) {
        scope.launch {
            try {
                isRefreshing = true
                val token = session.getAccessToken()
                val sellerId = session.getUserId()
                if (token.isNullOrBlank()) {
                    if (showToast) {
                        Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val api = RetrofitClient.authenticated(token)
                val txnResp = api.getTransactionById(transactionId)
                if (!txnResp.isSuccessful || txnResp.body() == null) {
                    if (showToast) {
                        Toast.makeText(context, "Failed to refresh transaction", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val txn = txnResp.body()!!
                currentStatus = txn.status
                displayProductName = txn.title
                displayAmount = txn.amount.toString()
                displayShippingAddress = txn.deliveryAddress
                displayOrderId = txn.reference ?: displayOrderId
                displayDate = txn.createdAt.take(10)

                if (!sellerId.isNullOrBlank()) {
                    val disputeResp = api.getDisputeByTransactionId(sellerId, transactionId)
                    disputeDetails = if (disputeResp.isSuccessful) {
                        disputeResp.body()
                    } else if (disputeResp.code() == 404) {
                        null
                    } else {
                        disputeDetails
                    }
                }

                if (showToast) {
                    Toast.makeText(context, "🔄 Transaction updated", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (showToast) {
                    Toast.makeText(context, "Refresh failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    fun updateStatus(actionType: String) {
        scope.launch {
            try {
                isUpdating = true
                val token = session.getAccessToken()
                val sellerId = session.getUserId()
                if (token.isNullOrBlank() || sellerId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val api = RetrofitClient.authenticated(token)

                if (actionType == "CHECK_PAYMENT") {
                    pollPendingPayment(api)
                    return@launch
                }

                val response = when (actionType) {
                    "ACCEPT" -> api.acceptTransaction(transactionId, sellerId)
                    "IN_DELIVERY" -> api.markInDelivery(transactionId, sellerId)
                    "SELLER_DELIVERED" -> api.sellerConfirmDelivery(transactionId, sellerId)
                    else -> null
                }

                if (response == null) {
                    Toast.makeText(context, "Unsupported action.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (!response.isSuccessful || response.body() == null) {
                    val err = response.errorBody()?.string()?.take(180)
                    Toast.makeText(context, err ?: "Failed to update transaction status", Toast.LENGTH_LONG).show()
                    return@launch
                }

                currentStatus = response.body()!!.status
                val statusName = prettySellerEscrowState(normalizeSellerEscrowStatus(currentStatus))
                Toast.makeText(context, "✅ Status updated to: $statusName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isUpdating = false
            }
        }
    }

    LaunchedEffect(transactionId) {
        refreshSellerTransactionData(showToast = false)
    }

    fun viewDispute() {
        val intent = Intent(context, DisputeDetailsActivity::class.java).apply {
            putExtra("DISPUTE_ID", disputeDetails?.id ?: "")
            putExtra("TRANSACTION_ID", transactionId)
        }
        context.startActivity(intent)
    }

    val nextAction = getNextAction()
    val isCompleted = normalizedCurrentStatus in listOf("COMPLETED", "CANCELLED", "DECLINED", "REFUNDED", "EXPIRED")
    val hasDisputeHistory = disputeDetails != null || normalizedCurrentStatus in listOf(
        "DISPUTED",
        "REFUND_PENDING",
        "REFUND_PROCESSING",
        "REFUNDED"
    )
    val statusConfig = getStatusConfig()
    val currentStepIndex = getStatusStep()

    // ===================== UI =====================
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Order Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? SellerTransactionDetailActivity)?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshSellerTransactionData(showToast = true) },
                        enabled = !isRefreshing && !isUpdating
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        },
        bottomBar = {
            SellerTransactionDetailBottomNavigation()
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== STATUS CARD =====
            item {
                SellerStatusCard(
                    statusDisplay = getStatusDisplay(),
                    statusConfig = statusConfig,
                    orderId = displayOrderId,
                    currentStep = currentStepIndex,
                    totalSteps = 9,
                    colorScheme = colorScheme
                )
            }

            // ===== ORDER SUMMARY CARD =====
            item {
                SellerOrderSummaryCard(
                    productName = displayProductName,
                    buyerName = buyerName,
                    buyerInitials = buyerInitials,
                    amount = formattedAmount,
                    shippingAddress = displayShippingAddress,
                    date = displayDate,
                    onBuyerInfoToggle = { showBuyerInfo = !showBuyerInfo },
                    showBuyerInfo = showBuyerInfo,
                    colorScheme = colorScheme
                )
            }

            // ===== PAYMENT STATUS CARD =====
            item {
                SellerPaymentStatusCard(
                    amount = formattedAmount,
                    normalizedStatus = normalizedCurrentStatus,
                    colorScheme = colorScheme
                )
            }

            // ===== TRANSACTION TIMELINE =====
            item {
                SellerTransactionTimeline(
                    currentStatus = normalizedCurrentStatus,
                    colorScheme = colorScheme
                )
            }

            // ===== ACTIONS CARD =====
            item {
                SellerActionsCard(
                    nextAction = nextAction,
                    isCompleted = isCompleted,
                    isUpdating = isUpdating,
                    isPollingPayment = isPollingPayment,
                    onAction = { updateStatus(it) },
                    onMessageBuyer = {
                        Toast.makeText(context, "💬 Message buyer coming soon", Toast.LENGTH_SHORT).show()
                    },
                    showViewDispute = hasDisputeHistory,
                    onViewDispute = { viewDispute() },
                    colorScheme = colorScheme
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ===================== COMPOSABLE COMPONENTS =====================

@Composable
fun SellerStatusCard(
    statusDisplay: String,
    statusConfig: SellerStatusConfig,
    orderId: String,
    currentStep: Int,
    totalSteps: Int,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Order #${orderId.takeLast(6)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusConfig.backgroundColor,
                    border = BorderStroke(1.dp, statusConfig.backgroundColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusConfig.dotColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            statusDisplay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusConfig.textColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    statusConfig.textColor,
                                    statusConfig.textColor.copy(alpha = 0.5f),
                                    statusConfig.textColor
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        statusConfig.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                Column {
                    Text(
                        "Current Status",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        statusDisplay,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    // Progress indicator
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(totalSteps) { index ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            index < currentStep -> Color(0xFF10B981)
                                            index == currentStep -> statusConfig.textColor
                                            else -> colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerOrderSummaryCard(
    productName: String,
    buyerName: String,
    buyerInitials: String,
    amount: String,
    shippingAddress: String,
    date: String,
    onBuyerInfoToggle: () -> Unit,
    showBuyerInfo: Boolean,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Order Summary",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        "Seller View",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Product
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFE65100),
                                    Color(0xFFF57C00)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = "Product",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Amount: $amount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            // Buyer Info (Collapsible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBuyerInfoToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        BrandBlue,
                                        BrandBlue.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            buyerInitials,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(
                            "Buyer",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            buyerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    if (showBuyerInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = showBuyerInfo,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Delivery Address",
                        value = shippingAddress,
                        colorScheme = colorScheme
                    )
                    DetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Order Date",
                        value = date,
                        colorScheme = colorScheme
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                label,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SellerPaymentStatusCard(
    amount: String,
    normalizedStatus: String,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Payment Status",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(1.dp, Color(0xFFFFE0B2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Order Amount",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            amount,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFE65100).copy(alpha = 0.3f)
                    )
                }
            }

            // Payment Stages - Seller Focused
            val stages = listOf(
                "Buyer Funds" to Icons.Default.ArrowForward,
                "Escrow Secured" to Icons.Default.Shield,
                "Release" to Icons.Default.CheckCircle
            )

            val stageIndex = when (normalizedStatus) {
                "FUNDS_HELD", "SELLER_ACCEPTED", "IN_DELIVERY",
                "SELLER_DELIVERED", "BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING" -> 1
                "RELEASE_PROCESSING", "RELEASE_FAILED", "COMPLETED" -> 2
                else -> 0
            }

            stages.forEachIndexed { index, (label, icon) ->
                val isMet = index < stageIndex
                val isCurrent = index == stageIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isMet -> Color(0xFF10B981)
                                        isCurrent -> Color(0xFFE65100)
                                        else -> colorScheme.outlineVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isMet || isCurrent) Color.White else colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                isMet -> Color(0xFF10B981)
                                isCurrent -> colorScheme.onSurface
                                else -> colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Text(
                        when {
                            isMet -> "✓ Complete"
                            isCurrent -> "In Progress"
                            else -> "Pending"
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isMet -> Color(0xFF10B981)
                            isCurrent -> Color(0xFFE65100)
                            else -> colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SellerTransactionTimeline(
    currentStatus: String,
    colorScheme: ColorScheme
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = Color(0xFFE65100)
                    )
                    Text(
                        "Transaction Timeline",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusUpper = currentStatus.trim().uppercase(Locale.getDefault())

                    val declineFlow = listOf(
                        "CREATED" to "Created",
                        "DECLINED" to "Declined"
                    )

                    val successOrReleaseFailedFlow = listOf(
                        "CREATED" to "Created",
                        "PENDING_PAYMENT" to "Pending Payment",
                        "FUNDS_HELD" to "Funds Held",
                        "SELLER_ACCEPTED" to "Seller Accepted",
                        "IN_DELIVERY" to "In Delivery",
                        "SELLER_DELIVERED" to "Seller Delivered",
                        "BUYER_CONFIRMED_DELIVERED" to "Buyer Confirmed Delivered",
                        "RELEASE_PENDING" to "Release Pending",
                        "RELEASE_PROCESSING" to "Release Processing",
                        "RELEASE_FAILED" to "Release Failed",
                        "COMPLETED" to "Completed"
                    )

                    val disputeRefundFlow = listOf(
                        "FUNDS_HELD" to "Funds Held",
                        "SELLER_ACCEPTED" to "Seller Accepted",
                        "IN_DELIVERY" to "In Delivery",
                        "SELLER_DELIVERED" to "Seller Delivered",
                        "BUYER_CONFIRMED_DELIVERED" to "Buyer Confirmed Delivered",
                        "DISPUTED" to "Disputed",
                        "REFUND_PENDING" to "Refund Pending",
                        "REFUND_PROCESSING" to "Refund Processing",
                        "REFUNDED" to "Refunded"
                    )

                    val cancelFromCreatedFlow = listOf(
                        "CREATED" to "Created",
                        "CANCELLED" to "Cancelled"
                    )

                    val cancelFromPendingPaymentFlow = listOf(
                        "CREATED" to "Created",
                        "PENDING_PAYMENT" to "Pending Payment",
                        "CANCELLED" to "Cancelled"
                    )

                    val expiredFlow = listOf(
                        "CREATED" to "Created",
                        "EXPIRED" to "Expired"
                    )

                    val activeFlow = when (statusUpper) {
                        "DECLINED" -> declineFlow
                        "DISPUTED", "REFUND_PENDING", "REFUND_PROCESSING", "REFUNDED" -> disputeRefundFlow
                        "EXPIRED" -> expiredFlow
                        "CANCELLED" -> cancelFromPendingPaymentFlow
                        else -> successOrReleaseFailedFlow
                    }

                    val terminalStates = setOf("COMPLETED", "DECLINED", "CANCELLED", "REFUNDED", "EXPIRED")
                    val isTerminalState = statusUpper in terminalStates

                    val currentFlowIndex = activeFlow.indexOfFirst { it.first == statusUpper }
                        .coerceAtLeast(0)

                    activeFlow.forEachIndexed { index, (state, label) ->
                        val isMet = index < currentFlowIndex
                        val isCurrent = index == currentFlowIndex

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Status indicator
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isMet -> Color(0xFF10B981)
                                            isCurrent -> Color(0xFFE65100)
                                            else -> colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isMet -> Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    isCurrent -> Icon(
                                        Icons.Default.RadioButtonChecked,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    else -> Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    )
                                }
                            }

                            // Connecting line
                            if (index < activeFlow.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(if (isMet || isCurrent) 20.dp else 20.dp)
                                        .background(
                                            if (isMet || isCurrent)
                                                Color(0xFF10B981)
                                            else
                                                colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                )
                            } else {
                                Spacer(modifier = Modifier.width(2.dp))
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    label,
                                    fontSize = if (isCurrent) 14.sp else 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        isMet -> Color(0xFF10B981)
                                        isCurrent -> colorScheme.onSurface
                                        else -> colorScheme.onSurfaceVariant
                                    }
                                )
                                if (isCurrent) {
                                    Text(
                                        if (isTerminalState) "Final State" else "Current Step",
                                        fontSize = 10.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (isMet) {
                                Text(
                                    "✓",
                                    fontSize = 14.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerActionsCard(
    nextAction: SellerAction?,
    isCompleted: Boolean,
    isUpdating: Boolean,
    isPollingPayment: Boolean,
    onAction: (String) -> Unit,
    onMessageBuyer: () -> Unit,
    showViewDispute: Boolean,
    onViewDispute: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            // Primary Action
            if (nextAction != null && !isCompleted) {
                Button(
                    onClick = { onAction(nextAction.actionType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdating && !isPollingPayment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = nextAction.color,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (isUpdating || isPollingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (nextAction.actionType == "CHECK_PAYMENT") "Checking payment..." else "Processing...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            nextAction.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            nextAction.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Message Buyer Button
            OutlinedButton(
                onClick = onMessageBuyer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorScheme.primary
                ),
                border = BorderStroke(1.5.dp, colorScheme.primary)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Message Buyer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (showViewDispute) {
                OutlinedButton(
                    onClick = onViewDispute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFDC2626).copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "View Dispute",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ===================== DATA CLASSES =====================

data class SellerStatusConfig(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val backgroundColor: Color,
    val icon: ImageVector
)

data class SellerAction(
    val label: String,
    val actionType: String,
    val icon: ImageVector,
    val color: Color
)

// ===================== HELPER FUNCTIONS =====================

@Composable
fun SellerTransactionDetailBottomNavigation() {
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

private fun statusFromSellerStep(step: Int): String {
    return when (step) {
        1 -> "FUNDS_HELD"
        2 -> "IN_DELIVERY"
        3 -> "SELLER_DELIVERED"
        else -> "COMPLETED"
    }
}

private val SELLER_ESCROW_STATE_TRANSITIONS: Map<String, List<String>> = mapOf(
    "CREATED" to listOf("DECLINED", "PENDING_PAYMENT", "CANCELLED", "EXPIRED"),
    "PENDING_PAYMENT" to listOf("FUNDS_HELD", "CANCELLED"),
    "FUNDS_HELD" to listOf("SELLER_ACCEPTED", "DISPUTED"),
    "SELLER_ACCEPTED" to listOf("IN_DELIVERY", "DISPUTED"),
    "IN_DELIVERY" to listOf("SELLER_DELIVERED", "DISPUTED"),
    "SELLER_DELIVERED" to listOf("BUYER_CONFIRMED_DELIVERED", "DISPUTED"),
    "BUYER_CONFIRMED_DELIVERED" to listOf("RELEASE_PENDING", "DISPUTED"),
    "RELEASE_PENDING" to listOf("RELEASE_PROCESSING"),
    "RELEASE_PROCESSING" to listOf("RELEASE_FAILED", "COMPLETED"),
    "DISPUTED" to listOf("REFUND_PENDING"),
    "REFUND_PENDING" to listOf("REFUND_PROCESSING"),
    "REFUND_PROCESSING" to listOf("REFUNDED")
)

private fun normalizeSellerEscrowStatus(raw: String): String {
    return raw.trim().uppercase(Locale.getDefault())
}

private fun prettySellerEscrowState(state: String): String {
    return state
        .lowercase(Locale.getDefault())
        .split("_")
        .joinToString(" ") { token -> token.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SellerTransactionDetailScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        SellerTransactionDetailScreen(
            transactionId = "TX-123456",
            productName = "iPhone 15 Pro Max - 256GB",
            buyerName = "Kwami Chen",
            buyerInitials = "KC",
            amount = "145,000",
            orderId = "ESC-8294-PRO",
            date = "24 Oct, 2023",
            shippingAddress = "Westlands Hub, Ground Floor Wing A, Nairobi, Kenya",
            initialStep = 1,
            initialStatus = "FUNDS_HELD"
        )
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SellerTransactionDetailScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        SellerTransactionDetailScreen(
            transactionId = "TX-123456",
            productName = "iPhone 15 Pro Max - 256GB",
            buyerName = "Kwami Chen",
            buyerInitials = "KC",
            amount = "145,000",
            orderId = "ESC-8294-PRO",
            date = "24 Oct, 2023",
            shippingAddress = "Westlands Hub, Ground Floor Wing A, Nairobi, Kenya",
            initialStep = 1,
            initialStatus = "FUNDS_HELD"
        )
    }
}