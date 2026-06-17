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
    val filters = listOf("All", "Paid", "Shipped", "Pending Delivery")

    // Mock data for active escrows
    val activeEscrows = listOf(
        ActiveEscrowItem(
            id = "1",
            buyerName = "Kwami Chen",
            buyerInitials = "KC",
            productName = "iPhone 15 Pro Max",
            amount = "145,000",
            status = "Paid - Ship Now",
            statusColor = Color(0xFF006C49),
            statusBgColor = Color(0xFF6CF8BB).copy(alpha = 0.1f),
            statusIcon = Icons.Default.CheckCircle,
            shippingAddress = "Westlands Hub, Ground Floor Wing A, Nairobi, Kenya",
            date = "24 Oct, 2023",
            orderId = "ESC-8294-PRO",
            currentStep = 1
        ),
        ActiveEscrowItem(
            id = "2",
            buyerName = "Sarah Ochieng",
            buyerInitials = "SO",
            productName = "Sony WH-1000XM5",
            amount = "42,500",
            status = "In Transit",
            statusColor = Color(0xFF3E2400),
            statusBgColor = Color(0xFFFFDDB8),
            statusIcon = Icons.Default.Schedule,
            shippingAddress = "Industrial Area, Mombasa Road, Nairobi, Kenya",
            date = "20 Oct, 2023",
            orderId = "ESC-8295-PRO",
            currentStep = 2
        )
    )

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