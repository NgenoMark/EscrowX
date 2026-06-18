package mobile.project.escrowx.dash

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.InitiateStkPushRequest
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.components.BuyerNavBar
import mobile.project.escrowx.ui.components.BuyerNavItem
import mobile.project.escrowx.ui.components.navigateTab
import java.text.NumberFormat
import java.util.*

class BuyerTransactionDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "Product"
        val productDescription = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "No description provided"
        val sellerName = intent.getStringExtra("SELLER_NAME") ?: "Seller"
        val amount = intent.getStringExtra("AMOUNT") ?: "0"
        val orderId = intent.getStringExtra("ORDER_ID") ?: "ESC-XXXX"
        val date = intent.getStringExtra("DATE") ?: ""
        val deliveryDate = intent.getStringExtra("DELIVERY_DATE") ?: date
        val shippingAddress = intent.getStringExtra("SHIPPING_ADDRESS") ?: ""
        val currentStatus = intent.getStringExtra("STATUS") ?: "FUNDS_HELD"

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                BuyerTransactionDetailScreen(
                    transactionId = transactionId,
                    productName = productName,
                    productDescription = productDescription,
                    sellerName = sellerName,
                    amount = amount,
                    orderId = orderId,
                    date = date,
                    deliveryDate = deliveryDate,
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
    productDescription: String,
    sellerName: String,
    amount: String,
    orderId: String,
    date: String,
    deliveryDate: String,
    shippingAddress: String,
    initialStatus: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var currentStatus by remember { mutableStateOf(initialStatus) }
    var displayProductName by remember { mutableStateOf(productName) }
    var displayProductDescription by remember { mutableStateOf(productDescription) }
    var displayAmount by remember { mutableStateOf(amount) }
    var displayOrderId by remember { mutableStateOf(orderId) }
    var displayDeliveryDate by remember { mutableStateOf(deliveryDate) }
    var displayShippingAddress by remember { mutableStateOf(shippingAddress) }
    var isUpdating by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    var phoneLocalPart by remember { mutableStateOf("") }
    var isPollingPayment by remember { mutableStateOf(false) }
    var showStatusTimeline by remember { mutableStateOf(false) }

    val statusUpper = remember(currentStatus) { currentStatus.trim().uppercase() }
    val parsedAmount = displayAmount.replace(",", "").toDoubleOrNull() ?: 0.0
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
        .format(parsedAmount)
        .replace("KES", "KES")

    // ===================== STATUS HELPERS =====================
    fun getStatusDisplay(): String {
        return when {
            currentStatus.equals("FUNDS_HELD", ignoreCase = true) -> "Funds Held"
            currentStatus.equals("SELLER_ACCEPTED", ignoreCase = true) -> "Seller Accepted"
            currentStatus.equals("IN_DELIVERY", ignoreCase = true) -> "In Delivery"
            currentStatus.equals("DELIVERED", ignoreCase = true) -> "Delivered"
            currentStatus.equals("SELLER_DELIVERED", ignoreCase = true) -> "Seller Delivered"
            currentStatus.equals("BUYER_CONFIRMED_DELIVERED", ignoreCase = true) -> "Buyer Confirmed"
            currentStatus.equals("RELEASE_PENDING", ignoreCase = true) -> "Release Pending"
            currentStatus.equals("RELEASE_PROCESSING", ignoreCase = true) -> "Release Processing"
            currentStatus.equals("RELEASE_FAILED", ignoreCase = true) -> "Release Failed"
            currentStatus.equals("REFUND_PENDING", ignoreCase = true) -> "Refund Pending"
            currentStatus.equals("REFUND_PROCESSING", ignoreCase = true) -> "Refund Processing"
            currentStatus.equals("REFUNDED", ignoreCase = true) -> "Refunded"
            currentStatus.equals("DECLINED", ignoreCase = true) -> "Declined"
            currentStatus.equals("CANCELLED", ignoreCase = true) -> "Cancelled"
            currentStatus.equals("EXPIRED", ignoreCase = true) -> "Expired"
            currentStatus.equals("COMPLETED", ignoreCase = true) -> "Completed"
            else -> currentStatus
        }
    }

    fun getStatusConfig(): BuyerStatusConfig {
        return when {
            currentStatus.equals("FUNDS_HELD", ignoreCase = true) ->
                BuyerStatusConfig("Funds Held", Color(0xFF3B82F6), Color(0xFF3B82F6), Color(0xFF3B82F6).copy(alpha = 0.12f))
            currentStatus.equals("IN_DELIVERY", ignoreCase = true) ->
                BuyerStatusConfig("In Delivery", Color(0xFFF59E0B), Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.12f))
            currentStatus.equals("SELLER_DELIVERED", ignoreCase = true) ->
                BuyerStatusConfig("Seller Delivered", Color(0xFF8B5CF6), Color(0xFF8B5CF6), Color(0xFF8B5CF6).copy(alpha = 0.12f))
            currentStatus.equals("BUYER_CONFIRMED_DELIVERED", ignoreCase = true) ->
                BuyerStatusConfig("Buyer Confirmed", Color(0xFF10B981), Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.12f))
            currentStatus.equals("COMPLETED", ignoreCase = true) ->
                BuyerStatusConfig("Completed", Color(0xFF10B981), Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.12f))
            currentStatus.equals("CANCELLED", ignoreCase = true) ->
                BuyerStatusConfig("Cancelled", Color(0xFF5E5E66), Color(0xFF5E5E66), Color(0xFF5E5E66).copy(alpha = 0.12f))
            currentStatus.equals("DECLINED", ignoreCase = true) ->
                BuyerStatusConfig("Declined", Color(0xFF5E5E66), Color(0xFF5E5E66), Color(0xFF5E5E66).copy(alpha = 0.12f))
            currentStatus.equals("DISPUTED", ignoreCase = true) ->
                BuyerStatusConfig("Disputed", Color(0xFFDC2626), Color(0xFFDC2626), Color(0xFFDC2626).copy(alpha = 0.12f))
            currentStatus.equals("RELEASE_FAILED", ignoreCase = true) ->
                BuyerStatusConfig("Release Failed", Color(0xFFDC2626), Color(0xFFDC2626), Color(0xFFDC2626).copy(alpha = 0.12f))
            currentStatus.equals("REFUNDED", ignoreCase = true) ->
                BuyerStatusConfig("Refunded", Color(0xFF006C49), Color(0xFF006C49), Color(0xFF006C49).copy(alpha = 0.12f))
            else -> BuyerStatusConfig(currentStatus, Color(0xFF444651), Color(0xFF444651), Color(0xFF444651).copy(alpha = 0.12f))
        }
    }

    fun getStatusIcon(): ImageVector {
        return when {
            currentStatus.equals("FUNDS_HELD", ignoreCase = true) -> Icons.Default.AccountBalanceWallet
            currentStatus.equals("IN_DELIVERY", ignoreCase = true) -> Icons.Default.LocalShipping
            currentStatus.equals("COMPLETED", ignoreCase = true) -> Icons.Default.CheckCircle
            currentStatus.equals("CANCELLED", ignoreCase = true) -> Icons.Default.Cancel
            currentStatus.equals("DISPUTED", ignoreCase = true) -> Icons.Default.Warning
            currentStatus.equals("REFUNDED", ignoreCase = true) -> Icons.Default.ReceiptLong
            else -> Icons.Default.Info
        }
    }

    // ===================== API FUNCTIONS =====================
    fun updateTransactionState(targetState: String, message: String) {
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

                val api = RetrofitClient.authenticated(token)
                if (targetState == "RELEASE_PENDING") {
                    val confirmResponse = api.confirmReceipt(transactionId, buyerId)
                    if (!confirmResponse.isSuccessful || confirmResponse.body() == null) {
                        val errorMessage = confirmResponse.errorBody()?.string()?.take(200)
                        Toast.makeText(context, errorMessage ?: "Failed to authorize payout", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    currentStatus = confirmResponse.body()!!.status

                    val releaseResponse = api.releasePayout(transactionId, buyerId)
                    if (!releaseResponse.isSuccessful || releaseResponse.body() == null) {
                        val payoutError = releaseResponse.errorBody()?.string()?.take(220)
                        Toast.makeText(
                            context,
                            payoutError ?: "Payout authorization was recorded, but release initiation failed",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    val latestTxn = api.getTransactionById(transactionId)
                    latestTxn.body()?.status?.let { currentStatus = it }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    return@launch
                }

                val response = when (targetState) {
                    "BUYER_CONFIRMED_DELIVERED" -> api.buyerConfirmDelivery(transactionId, buyerId)
                    "DECLINED" -> api.declineTransaction(transactionId, buyerId)
                    "CANCELLED" -> api.cancelTransaction(transactionId, buyerId)
                    else -> null
                }

                if (response == null) {
                    Toast.makeText(context, "Unsupported action", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (!response.isSuccessful || response.body() == null) {
                    val errorMessage = response.errorBody()?.string()?.take(200)
                    Toast.makeText(context, errorMessage ?: "Failed to update transaction", Toast.LENGTH_LONG).show()
                    return@launch
                }

                currentStatus = response.body()!!.status
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    fun normalizeMpesaPhone(rawLocalPart: String): String? {
        val digits = rawLocalPart.filter { it.isDigit() }
        if (digits.length != 9 || !digits.startsWith("7")) return null
        return "+254$digits"
    }

    fun refreshTransactionData(showToast: Boolean = true) {
        scope.launch {
            try {
                isRefreshing = true
                val token = session.getAccessToken()
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
                displayProductDescription = txn.productDescription
                displayAmount = txn.amount.toString()
                displayShippingAddress = txn.deliveryAddress
                displayDeliveryDate = txn.deliveryDueAt.takeIf { it.isNotBlank() }?.take(10) ?: displayDeliveryDate
                displayOrderId = txn.reference ?: displayOrderId

                if (showToast) {
                    Toast.makeText(context, "Transaction updated", Toast.LENGTH_SHORT).show()
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

    suspend fun pollPaymentUntilSettled(
        api: mobile.project.escrowx.AuthApiService,
        paymentId: String,
        escrowId: String
    ) {
        isPollingPayment = true
        try {
            repeat(12) {
                delay(5000)
                val paymentResp = api.getPayment(paymentId)
                val paymentStatus = paymentResp.body()?.status?.trim()?.uppercase()

                val txnResp = api.getTransactionById(escrowId)
                val latestTxnStatus = txnResp.body()?.status?.trim()?.uppercase()
                if (!latestTxnStatus.isNullOrBlank()) {
                    currentStatus = latestTxnStatus
                }

                if (latestTxnStatus == "FUNDS_HELD") {
                    Toast.makeText(context, "Payment received. Funds are now held in escrow.", Toast.LENGTH_LONG).show()
                    return
                }

                if (paymentStatus in listOf("PAID", "SUCCESS", "COMPLETED")) {
                    Toast.makeText(context, "Payment confirmed.", Toast.LENGTH_LONG).show()
                    return
                }

                if (paymentStatus in listOf("FAILED", "CANCELLED", "REJECTED")) {
                    Toast.makeText(context, "Payment was not successful. Please try again.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val txnResp = api.getTransactionById(escrowId)
            val latestTxnStatus = txnResp.body()?.status?.trim()?.uppercase()
            if (!latestTxnStatus.isNullOrBlank()) {
                currentStatus = latestTxnStatus
            }
            if (latestTxnStatus == "FUNDS_HELD") {
                Toast.makeText(context, "Payment completed.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Awaiting M-Pesa callback. You can refresh this transaction shortly.", Toast.LENGTH_LONG).show()
            }
        } finally {
            isPollingPayment = false
        }
    }

    fun initiateStkPayment() {
        val normalizedPhone = normalizeMpesaPhone(phoneLocalPart)
        if (normalizedPhone == null) {
            Toast.makeText(context, "Enter a valid number after +254 (e.g. 7XXXXXXXX)", Toast.LENGTH_LONG).show()
            return
        }

        scope.launch {
            try {
                isUpdating = true
                val token = session.getAccessToken()
                val buyerId = session.getUserId()
                if (token.isNullOrBlank() || buyerId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val api = RetrofitClient.authenticated(token)
                val resp = api.initiateStkPush(
                    escrowId = transactionId,
                    actorUserId = buyerId,
                    request = InitiateStkPushRequest(phoneNumber = normalizedPhone)
                )

                if (!resp.isSuccessful || resp.body() == null) {
                    val err = resp.errorBody()?.string()?.take(180)
                    Toast.makeText(context, err ?: "Failed to initiate STK push", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val body = resp.body()!!
                val paymentId = body.paymentId
                phoneLocalPart = normalizedPhone.removePrefix("+254")
                showPayDialog = false
                Toast.makeText(context, body.message ?: "STK push initiated. Check your phone.", Toast.LENGTH_LONG).show()

                pollPaymentUntilSettled(api, paymentId, transactionId)
                refreshTransactionData(showToast = false)
            } catch (e: Exception) {
                Toast.makeText(context, "Payment error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isUpdating = false
            }
        }
    }

    fun raiseDispute() {
        val intent = Intent(context, RaiseDisputeActivity::class.java).apply {
            putExtra("TRANSACTION_ID", transactionId)
            putExtra("TRANSACTION_TITLE", displayProductName)
            putExtra("TRANSACTION_AMOUNT", displayAmount)
        }
        context.startActivity(intent)
    }

    LaunchedEffect(transactionId) {
        refreshTransactionData(showToast = false)
    }

    // ===================== BUTTON STATES =====================
    val cancelEnabled = statusUpper == "CREATED" || statusUpper == "PENDING_PAYMENT"
    val disputeEnabled = statusUpper in listOf(
        "FUNDS_HELD",
        "SELLER_ACCEPTED",
        "IN_DELIVERY",
        "SELLER_DELIVERED",
        "BUYER_CONFIRMED_DELIVERED"
    )

    val primaryAction: Pair<String, String>? = when (statusUpper) {
        "SELLER_DELIVERED" -> "Confirm Delivery" to "BUYER_CONFIRMED_DELIVERED"
        "BUYER_CONFIRMED_DELIVERED" -> "Authorize Payout" to "RELEASE_PENDING"
        "CREATED" -> "Decline Transaction" to "DECLINED"
        else -> null
    }

    // ===================== UI =====================
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transaction Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? BuyerTransactionDetailActivity)?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshTransactionData(showToast = true) },
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
            BuyerTransactionDetailBottomNavigation()
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
                StatusCard(
                    statusDisplay = getStatusDisplay(),
                    statusConfig = getStatusConfig(),
                    statusIcon = getStatusIcon(),
                    orderId = displayOrderId,
                    colorScheme = colorScheme
                )
            }

            // ===== ITEM DETAILS CARD =====
            item {
                ItemDetailsCard(
                    productName = displayProductName,
                    productDescription = displayProductDescription,
                    sellerName = sellerName,
                    amount = formattedAmount,
                    shippingAddress = displayShippingAddress,
                    deliveryDate = displayDeliveryDate,
                    colorScheme = colorScheme
                )
            }

            // ===== PAYMENT STATUS CARD =====
            item {
                PaymentStatusCard(
                    amount = formattedAmount,
                    statusUpper = statusUpper,
                    colorScheme = colorScheme
                )
            }

            // ===== TRANSACTION PROGRESS CARD =====
            item {
                TransactionProgressCard(
                    statusUpper = statusUpper,
                    colorScheme = colorScheme
                )
            }

            // ===== ACTIONS CARD =====
            item {
                ActionsCard(
                    primaryAction = primaryAction,
                    statusUpper = statusUpper,
                    cancelEnabled = cancelEnabled,
                    disputeEnabled = disputeEnabled,
                    isUpdating = isUpdating,
                    isPollingPayment = isPollingPayment,
                    onPrimaryAction = { state, message ->
                        updateTransactionState(state, message)
                    },
                    onCancel = {
                        updateTransactionState("CANCELLED", "Transaction cancelled")
                    },
                    onDispute = { raiseDispute() },
                    onPay = { showPayDialog = true },
                    colorScheme = colorScheme
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ===== PAYMENT DIALOG =====
    if (showPayDialog) {
        PaymentDialog(
            productName = displayProductName,
            amount = formattedAmount,
            phoneLocalPart = phoneLocalPart,
            onPhoneChange = { phoneLocalPart = it },
            isUpdating = isUpdating,
            isPollingPayment = isPollingPayment,
            onInitiatePayment = { initiateStkPayment() },
            onDismiss = { showPayDialog = false },
            colorScheme = colorScheme
        )
    }
}

// ===================== COMPOSABLE COMPONENTS =====================

@Composable
fun StatusCard(
    statusDisplay: String,
    statusConfig: BuyerStatusConfig,
    statusIcon: ImageVector,
    orderId: String,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusConfig.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = statusConfig.textColor
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ItemDetailsCard(
    productName: String,
    productDescription: String,
    sellerName: String,
    amount: String,
    shippingAddress: String,
    deliveryDate: String,
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
                    "Item Details",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFD1FAE5)
                ) {
                    Text(
                        "Verified Seller",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.primary,
                                    colorScheme.primary.copy(alpha = 0.7f)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Store",
                            modifier = Modifier.size(14.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                        Text(
                            sellerName,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            DetailRow(
                icon = Icons.Default.Description,
                label = "Description",
                value = productDescription,
                colorScheme = colorScheme
            )

            DetailRow(
                icon = Icons.Default.LocationOn,
                label = "Delivery Address",
                value = shippingAddress,
                colorScheme = colorScheme
            )

            DetailRow(
                icon = Icons.Default.CalendarToday,
                label = "Delivery Date",
                value = deliveryDate,
                colorScheme = colorScheme
            )
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
fun PaymentStatusCard(
    amount: String,
    statusUpper: String,
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
                color = colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.15f)),
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
                            "Amount Paid",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            amount,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            // Payment Stages
            val paymentStages = listOf(
                "Buyer Funded" to Icons.Default.ArrowForward,
                "Funds Held" to Icons.Default.AccountBalanceWallet,
                "Released" to Icons.Default.CheckCircle,
                "Refunded" to Icons.Default.ReceiptLong
            )

            val paymentStageIndex = when (statusUpper) {
                "FUNDS_HELD", "SELLER_ACCEPTED", "IN_DELIVERY",
                "SELLER_DELIVERED", "BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING" -> 1
                "RELEASE_PROCESSING", "RELEASE_FAILED", "COMPLETED" -> 2
                "DISPUTED", "REFUND_PENDING", "REFUND_PROCESSING", "REFUNDED" -> 3
                else -> 0
            }

            paymentStages.forEachIndexed { index, (label, icon) ->
                val isMet = index < paymentStageIndex
                val isCurrent = index == paymentStageIndex
                val isUpcoming = index > paymentStageIndex

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
                                        isCurrent -> colorScheme.primary
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
                            isMet -> "✓ Done"
                            isCurrent -> "In Progress"
                            else -> "Pending"
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isMet -> Color(0xFF10B981)
                            isCurrent -> colorScheme.primary
                            else -> colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionProgressCard(
    statusUpper: String,
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
                        tint = colorScheme.primary
                    )
                    Text(
                        "Transaction Progress",
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        "CANCELLED" -> cancelFromCreatedFlow
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
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isMet -> Color(0xFF10B981)
                                            isCurrent -> colorScheme.primary
                                            else -> colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isMet) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                } else if (isCurrent) {
                                    if (isTerminalState) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.outlineVariant)
                                    )
                                }
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
                                        color = colorScheme.primary,
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
fun ActionsCard(
    primaryAction: Pair<String, String>?,
    statusUpper: String,
    cancelEnabled: Boolean,
    disputeEnabled: Boolean,
    isUpdating: Boolean,
    isPollingPayment: Boolean,
    onPrimaryAction: (String, String) -> Unit,
    onCancel: () -> Unit,
    onDispute: () -> Unit,
    onPay: () -> Unit,
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
            primaryAction?.let { action ->
                Button(
                    onClick = {
                        val message = when (action.second) {
                            "BUYER_CONFIRMED_DELIVERED" -> "Buyer confirmed delivery"
                            "RELEASE_PENDING" -> "Payout authorized successfully"
                            "DECLINED" -> "Transaction declined"
                            else -> "Transaction updated"
                        }
                        onPrimaryAction(action.second, message)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Icon(
                        when (action.first) {
                            "Confirm Delivery" -> Icons.Default.Check
                            "Authorize Payout" -> Icons.Default.Payments
                            "Decline Transaction" -> Icons.Default.Close
                            else -> Icons.Default.ArrowForward
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        action.first,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Pay Now Button
            if (statusUpper == "PENDING_PAYMENT") {
                Button(
                    onClick = onPay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdating && !isPollingPayment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (isUpdating || isPollingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Processing...", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Pay with M-Pesa", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = cancelEnabled && !isUpdating,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (cancelEnabled) MaterialTheme.colorScheme.error else colorScheme.outlineVariant
                )
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Cancel Transaction", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // Dispute Button
            OutlinedButton(
                onClick = onDispute,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = disputeEnabled && !isUpdating,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFDC2626)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (disputeEnabled) Color(0xFFDC2626) else colorScheme.outlineVariant
                )
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Raise Dispute", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PaymentDialog(
    productName: String,
    amount: String,
    phoneLocalPart: String,
    onPhoneChange: (String) -> Unit,
    isUpdating: Boolean,
    isPollingPayment: Boolean,
    onInitiatePayment: () -> Unit,
    onDismiss: () -> Unit,
    colorScheme: ColorScheme
) {
    AlertDialog(
        onDismissRequest = {
            if (!isUpdating && !isPollingPayment) {
                onDismiss()
            }
        },
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    "M-Pesa Payment",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.primary.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Product",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            productName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Amount",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            amount,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Text(
                    "Enter your M-Pesa registered phone number",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = phoneLocalPart,
                    onValueChange = { input ->
                        onPhoneChange(input.filter { it.isDigit() }.take(9))
                    },
                    singleLine = true,
                    label = { Text("Phone Number") },
                    prefix = { Text("+254 ") },
                    placeholder = { Text("7XXXXXXXX") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isUpdating && !isPollingPayment,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = colorScheme.outlineVariant,
                        focusedLabelColor = Color(0xFF10B981)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onInitiatePayment,
                enabled = !isUpdating && !isPollingPayment,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                if (isUpdating || isPollingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Initiate STK Push", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating && !isPollingPayment,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel", fontSize = 14.sp)
            }
        }
    )
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

data class BuyerStatusConfig(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val backgroundColor: Color
)

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun BuyerTransactionDetailScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        BuyerTransactionDetailScreen(
            transactionId = "TX-123456",
            productName = "iPhone 15 Pro Max - 256GB",
            productDescription = "Brand new iPhone 15 Pro Max in excellent condition",
            sellerName = "Tech Haven KE",
            amount = "165,000",
            orderId = "ESC-2024-001",
            date = "2024-06-15",
            deliveryDate = "2024-06-20",
            shippingAddress = "123 Main Street, Nairobi, Kenya",
            initialStatus = "FUNDS_HELD"
        )
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun BuyerTransactionDetailScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        BuyerTransactionDetailScreen(
            transactionId = "TX-123456",
            productName = "iPhone 15 Pro Max - 256GB",
            productDescription = "Brand new iPhone 15 Pro Max in excellent condition",
            sellerName = "Tech Haven KE",
            amount = "165,000",
            orderId = "ESC-2024-001",
            date = "2024-06-15",
            deliveryDate = "2024-06-20",
            shippingAddress = "123 Main Street, Nairobi, Kenya",
            initialStatus = "FUNDS_HELD"
        )
    }
}