package mobile.project.escrowx.seller
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.TransactionsActivity
import mobile.project.escrowx.ui.components.SellerNavBar
import mobile.project.escrowx.ui.components.SellerNavItem
import mobile.project.escrowx.ui.components.navigateTab

data class ActiveEscrowItem(
    val id: String,
    val buyerName: String,
    val buyerInitials: String,
    val productName: String,
    val amount: String,
    val status: String,
    val statusColor: Color,
    val statusBgColor: Color,
    val statusIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val shippingAddress: String,
    val date: String,
    val orderId: String,
    val currentStep: Int  // 1=Paid, 2=Shipping, 3=Delivery, 4=Release
)

class SellerActiveEscrowsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                SellerActiveEscrowsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerActiveEscrowsScreen() {
    val context = LocalContext.current
    val session = SessionManager(context)

    var selectedFilter by remember { mutableStateOf("All") }
    var escrows by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var buyerNamesById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val filters = listOf("All", "Paid", "Shipped", "Pending Delivery")

    LaunchedEffect(Unit) {
        isLoading = true
        loadError = null

        val token = session.getAccessToken()
        val sellerId = session.getUserId()
        if (token.isNullOrBlank() || sellerId.isNullOrBlank()) {
            loadError = "Session expired. Please login again."
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val api = RetrofitClient.authenticated(token)
            val response = api.getTransactionsBySeller(sellerId, null)
            if (!response.isSuccessful) {
                loadError = "Failed to load escrows: ${response.code()}"
                isLoading = false
                return@LaunchedEffect
            }

            escrows = response.body().orEmpty()
                .filter { !isTerminalSellerState(it.status) }
                .sortedByDescending { it.createdAt }

            val buyerIds = escrows.map { it.buyerId }.distinct()
            val fetchedBuyerNames = mutableMapOf<String, String>()
            buyerIds.forEach { buyerId ->
                val fallback = "Buyer ${buyerId.take(6)}"
                val buyerName = try {
                    val buyerResponse = api.getUserById(buyerId)
                    if (buyerResponse.isSuccessful) {
                        buyerResponse.body()?.displayName?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
                fetchedBuyerNames[buyerId] = buyerName ?: fallback
            }
            buyerNamesById = fetchedBuyerNames
        } catch (e: Exception) {
            loadError = "Network error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    val activeEscrows = remember(escrows, buyerNamesById) {
        escrows.map { escrow ->
            val buyerName = buyerNamesById[escrow.buyerId] ?: "Buyer ${escrow.buyerId.take(6)}"
            ActiveEscrowItem(
                id = escrow.id,
                buyerName = buyerName,
                buyerInitials = buyerName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }
                    .ifBlank { "BY" },
                productName = escrow.title,
                amount = formatAmount(escrow.amount),
                status = sellerStatusLabel(escrow.status),
                statusColor = sellerStatusColor(escrow.status),
                statusBgColor = sellerStatusBgColor(escrow.status),
                statusIcon = sellerStatusIcon(escrow.status),
                shippingAddress = escrow.deliveryAddress,
                date = escrow.createdAt.take(10),
                orderId = escrow.reference ?: escrow.id,
                currentStep = sellerCurrentStep(escrow.status)
            )
        }
    }

    val filteredEscrows = when (selectedFilter) {
        "Paid" -> activeEscrows.filter { it.currentStep == 1 }
        "Shipped" -> activeEscrows.filter { it.currentStep == 2 }
        "Pending Delivery" -> activeEscrows.filter { it.currentStep == 3 }
        else -> activeEscrows
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Active Escrows",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151C27)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? SellerActiveEscrowsActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            SellerActiveEscrowsBottomNavigation()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .padding(16.dp)
        ) {
            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00236F),
                            selectedLabelColor = Color.White,
                            disabledContainerColor = Color(0xFFF9F9FF),
                            disabledLabelColor = Color(0xFF444651)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Escrows List
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00236F))
                    }
                }
                loadError != null -> {
                    Text(
                        text = loadError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                filteredEscrows.isEmpty() -> {
                    Text(
                        text = "No active escrows found",
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(filteredEscrows) { escrow ->
                            ActiveEscrowCard(
                                escrow = escrow,
                                onTrackOrder = {
                                    val intent = Intent(context, SellerTransactionDetailActivity::class.java).apply {
                                        putExtra("TRANSACTION_ID", escrow.id)
                                        putExtra("PRODUCT_NAME", escrow.productName)
                                        putExtra("BUYER_NAME", escrow.buyerName)
                                        putExtra("BUYER_INITIALS", escrow.buyerInitials)
                                        putExtra("AMOUNT", escrow.amount)
                                        putExtra("ORDER_ID", escrow.orderId)
                                        putExtra("DATE", escrow.date)
                                        putExtra("SHIPPING_ADDRESS", escrow.shippingAddress)
                                        putExtra("CURRENT_STEP", escrow.currentStep)
                                    }
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

private fun formatAmount(amount: Double): String = java.text.NumberFormat.getIntegerInstance().format(amount)

private fun isTerminalSellerState(status: String?): Boolean {
    val normalized = status?.uppercase() ?: return false
    return normalized in setOf("COMPLETED", "CANCELLED", "DECLINED", "REFUNDED", "RELEASED")
}

private fun sellerCurrentStep(status: String?): Int {
    return when (status?.uppercase()) {
        "FUNDS_HELD", "PENDING_PAYMENT" -> 1
        "SELLER_ACCEPTED", "IN_DELIVERY" -> 2
        "SELLER_DELIVERED", "BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING" -> 3
        else -> 1
    }
}

private fun sellerStatusLabel(status: String?): String {
    return when (status?.uppercase()) {
        "FUNDS_HELD" -> "Paid - Ship Now"
        "PENDING_PAYMENT" -> "Awaiting Payment"
        "SELLER_ACCEPTED" -> "Accepted"
        "IN_DELIVERY" -> "In Transit"
        "SELLER_DELIVERED" -> "Pending Buyer Confirmation"
        "BUYER_CONFIRMED_DELIVERED" -> "Buyer Confirmed"
        "RELEASE_PENDING" -> "Payout Processing"
        else -> status ?: "Unknown"
    }
}

private fun sellerStatusColor(status: String?): Color {
    return when (status?.uppercase()) {
        "FUNDS_HELD", "SELLER_ACCEPTED" -> Color(0xFF006C49)
        "IN_DELIVERY", "SELLER_DELIVERED" -> Color(0xFF3E2400)
        "PENDING_PAYMENT", "RELEASE_PENDING" -> Color(0xFF92400E)
        else -> Color(0xFF374151)
    }
}

private fun sellerStatusBgColor(status: String?): Color {
    return when (status?.uppercase()) {
        "FUNDS_HELD", "SELLER_ACCEPTED" -> Color(0xFF6CF8BB).copy(alpha = 0.1f)
        "IN_DELIVERY", "SELLER_DELIVERED" -> Color(0xFFFFDDB8)
        "PENDING_PAYMENT", "RELEASE_PENDING" -> Color(0xFFFDE68A)
        else -> Color(0xFFE5E7EB)
    }
}

private fun sellerStatusIcon(status: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status?.uppercase()) {
        "FUNDS_HELD", "SELLER_ACCEPTED" -> Icons.Default.CheckCircle
        "IN_DELIVERY", "SELLER_DELIVERED" -> Icons.Default.Schedule
        "PENDING_PAYMENT", "RELEASE_PENDING" -> Icons.Default.HourglassTop
        else -> Icons.Default.Info
    }
}

@Composable
fun ActiveEscrowCard(
    escrow: ActiveEscrowItem,
    onTrackOrder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C5D3))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "BUYER: ${escrow.buyerName.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757682)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        escrow.productName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                }
                Text(
                    "KES ${escrow.amount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00236F)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = escrow.statusBgColor,
                border = BorderStroke(1.dp, escrow.statusColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        escrow.statusIcon,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = escrow.statusColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        escrow.status,
                        fontSize = 11.sp,
                        color = escrow.statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = Color(0xFF444651))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(escrow.shippingAddress.take(30) + "...", fontSize = 12.sp, color = Color(0xFF444651))
                }
                TextButton(onClick = onTrackOrder) {
                    Text("Track Order", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00236F))
                }
            }
        }
    }
}

@Composable
fun SellerActiveEscrowsBottomNavigation() {
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