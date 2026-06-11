@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.dash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.LoginActivity
import mobile.project.escrowx.auth.SessionManager

class BuyerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BuyerDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerDashboardScreen(viewModel: BuyerDashViewmodel = viewModel()) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val userName by viewModel.userName.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }

    // Fetch user profile to get display name
    LaunchedEffect(Unit) {
        session.getEmail()?.let { email ->
            viewModel.loadUserData(email)
            val token = session.getAccessToken()
            if (!token.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(email)
                    if (response.isSuccessful) {
                        userProfile = response.body()
                    }
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Use displayName from profile, fallback to email username
    val displayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: userProfile?.email?.substringBefore("@")
        ?: userName

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF00236F))
                        .padding(24.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = session.getEmail() ?: "user@escrowx.com",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedTextColor = Color(0xFF00236F),
                        selectedIconColor = Color(0xFF00236F),
                        unselectedTextColor = Color.Black,
                        unselectedIconColor = Color.DarkGray
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "App Info") },
                    label = { Text("App Info", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(
                            context,
                            "EscrowX v1.0.0\nSecure mobile escrow for safer transactions",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedTextColor = Color(0xFF00236F),
                        selectedIconColor = Color(0xFF00236F),
                        unselectedTextColor = Color.Black,
                        unselectedIconColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray)

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFBA1A1A)
                        )
                    },
                    label = {
                        Text(
                            "Logout",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBA1A1A)
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        session.clearSession()
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        (context as? BuyerDashboardActivity)?.finishAffinity()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedTextColor = Color(0xFFBA1A1A),
                        selectedIconColor = Color(0xFFBA1A1A),
                        unselectedTextColor = Color(0xFFBA1A1A),
                        unselectedIconColor = Color(0xFFBA1A1A)
                    )
                )

                Text(
                    text = "EscrowX v1.0",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "EscrowX",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00236F)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF00236F))
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = Color(0xFF00236F))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF00236F)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(75.dp),
                    containerColor = Color.White
                ) {
                    val items = listOf(
                        "Home" to Icons.Default.Home,
                        "Transactions" to Icons.Default.AccountBalanceWallet,
                        "Profile" to Icons.Default.Person
                    )
                    items.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                when (index) {
                                    1 -> context.startActivity(Intent(context, TransactionsActivity::class.java))
                                    2 -> context.startActivity(Intent(context, ProfileActivity::class.java))
                                }
                            },
                            icon = {
                                Icon(icon, contentDescription = label, tint = if (selectedTab == index) Color(0xFF00236F) else Color.Gray)
                            },
                            label = {
                                Text(label, color = if (selectedTab == index) Color(0xFF00236F) else Color.Gray, fontSize = 11.sp)
                            }
                        )
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeTabContent(padding, context, displayName)
                1 -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Opening Transactions...")
                    }
                }
                2 -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Opening Profile...")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(paddingValues: PaddingValues, context: Context, displayName: String) {
    val disputesCount = 3

    val incomingRequests = listOf(
        IncomingRequestItem(
            id = "1",
            sellerName = "Tech Haven KE",
            itemTitle = "Payment for Wireless Mouse",
            amount = "8,200"
        ),
        IncomingRequestItem(
            id = "2",
            sellerName = "Phone Mart Kenya",
            itemTitle = "iPhone 15 Pro",
            amount = "125,000"
        )
    )

    // Mock active escrows with full details for tracking
    val activeEscrows = listOf(
        mapOf(
            "id" to "txn_123",
            "productName" to "iPhone 15 Pro",
            "sellerName" to "Jumia Electronics",
            "amount" to "125,000",
            "orderId" to "ESC-ABC123",
            "date" to "24 Oct, 2023",
            "shippingAddress" to "Westlands Hub, Ground Floor Wing A, Nairobi, Kenya",
            "status" to "IN_DELIVERY"
        ),
        mapOf(
            "id" to "txn_456",
            "productName" to "Samsung Galaxy S24",
            "sellerName" to "Samsung Store",
            "amount" to "85,000",
            "orderId" to "ESC-DEF456",
            "date" to "20 Oct, 2023",
            "shippingAddress" to "Imaara Mall, Nairobi, Kenya",
            "status" to "FUNDS_HELD"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFF9F9FF))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome header – shows actual registered name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Hello, $displayName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = "Welcome back to your secure dashboard.", fontSize = 13.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // New Escrow button – launches unified CreateEscrowActivity with role BUYER
        Button(
            onClick = {
                val intent = Intent(context, CreateEscrowActivity::class.java).apply {
                    putExtra("ROLE", "BUYER")
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Escrow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Disputes card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE7E7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable {
                    context.startActivity(Intent(context, DisputeCenterActivity::class.java))
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Disputes", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                    Text(text = "$disputesCount active", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
                Icon(Icons.Default.Gavel, contentDescription = "Disputes", tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Incoming Requests Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Incoming Requests", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF151C27))
            TextButton(onClick = {
                context.startActivity(Intent(context, IncomingRequestsActivity::class.java))
            }) {
                Text("View All", color = Color(0xFF00236F), fontSize = 12.sp)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View All", modifier = Modifier.size(16.dp), tint = Color(0xFF00236F))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            items(incomingRequests) { request ->
                IncomingRequestCard(
                    request = request,
                    onAccept = {
                        val intent = Intent(context, TransactionDetailsActivity::class.java).apply {
                            putExtra("TRANSACTION_ID", request.id)
                            putExtra("SELLER_NAME", request.sellerName)
                            putExtra("BUSINESS_NAME", request.sellerName)
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

        Spacer(modifier = Modifier.height(24.dp))

        // Active Escrows Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Escrows", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
            TextButton(onClick = {
                Toast.makeText(context, "Swipe to see more escrows", Toast.LENGTH_SHORT).show()
            }) {
                Text("View All", color = Color(0xFF00236F))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View All", modifier = Modifier.size(16.dp), tint = Color(0xFF00236F))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            items(activeEscrows) { escrow ->
                val status = escrow["status"] ?: "FUNDS_HELD"
                val topStatus = if (status == "IN_DELIVERY") "PENDING DELIVERY" else "PENDING PAYMENT"
                val topStatusColor = if (status == "IN_DELIVERY") Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                val topStatusTextColor = if (status == "IN_DELIVERY") Color(0xFFD97706) else Color(0xFFDC2626)
                val bottomStatus = if (status == "IN_DELIVERY") "IN INSPECTION" else "AWAITING"
                val bottomStatusColor = if (status == "IN_DELIVERY") Color(0xFFE7EEFE) else Color(0xFFFEF3C7)
                val bottomStatusTextColor = if (status == "IN_DELIVERY") Color(0xFF00236F) else Color(0xFFD97706)
                val timeLeft = if (status == "IN_DELIVERY") "2 days left" else "5 days left"

                ActiveEscrowCard(
                    topStatus = topStatus,
                    topStatusColor = topStatusColor,
                    topStatusTextColor = topStatusTextColor,
                    amountText = "KES ${escrow["amount"]}",
                    bottomStatus = bottomStatus,
                    bottomStatusColor = bottomStatusColor,
                    bottomStatusTextColor = bottomStatusTextColor,
                    partyName = escrow["sellerName"] ?: "Unknown",
                    partyIcon = Icons.Default.Store,
                    timeLeftText = timeLeft,
                    onTrackClick = {
                        val intent = Intent(context, BuyerTransactionDetailActivity::class.java).apply {
                            putExtra("TRANSACTION_ID", escrow["id"])
                            putExtra("PRODUCT_NAME", escrow["productName"])
                            putExtra("SELLER_NAME", escrow["sellerName"])
                            putExtra("AMOUNT", escrow["amount"])
                            putExtra("ORDER_ID", escrow["orderId"])
                            putExtra("DATE", escrow["date"])
                            putExtra("SHIPPING_ADDRESS", escrow["shippingAddress"])
                            putExtra("STATUS", status)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Transactions
        Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TransactionListItem(
                title = "Funds Released",
                subtitle = "M-Pesa Store #452",
                amount = "KES 2,400",
                date = "Oct 12",
                status = "COMPLETED",
                icon = Icons.Default.Store,
                iconColor = Color(0xFF00236F)
            )
            TransactionListItem(
                title = "Dispute Opened",
                subtitle = "Apple AirPods Gen 3",
                amount = "KES 18,000",
                date = "Oct 10",
                status = "UNDER REVIEW",
                icon = Icons.Default.Gavel,
                iconColor = Color(0xFFDC2626)
            )
            TransactionListItem(
                title = "Escrow Created",
                subtitle = "Jumia Electronics",
                amount = "KES 12,500",
                date = "Oct 09",
                status = "ACTIVE",
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = Color(0xFF00236F)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun IncomingRequestCard(
    request: IncomingRequestItem,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PENDING YOUR ACTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B),
                    letterSpacing = 0.5.sp
                )
                Text(
                    "KES ${request.amount}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00236F)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        request.sellerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                    Text(
                        request.itemTitle,
                        fontSize = 12.sp,
                        color = Color(0xFF444651)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFBA1A1A)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFBA1A1A))
                ) {
                    Text("Decline", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00236F),
                        contentColor = Color.White
                    )
                ) {
                    Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ActiveEscrowCard(
    topStatus: String,
    topStatusColor: Color,
    topStatusTextColor: Color,
    amountText: String,
    bottomStatus: String,
    bottomStatusColor: Color,
    bottomStatusTextColor: Color,
    partyName: String,
    partyIcon: ImageVector,
    timeLeftText: String,
    onTrackClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(4.dp), color = topStatusColor) {
                    Text(
                        text = topStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = topStatusTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = amountText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00236F)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = bottomStatusColor,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = bottomStatus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = bottomStatusTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = partyIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF00236F)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = partyName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timeLeftText, fontSize = 11.sp, color = Color.Gray)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onTrackClick() }
                ) {
                    Text(
                        text = "Track",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00236F)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Track",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF00236F)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    title: String,
    subtitle: String,
    amount: String,
    date: String,
    status: String,
    icon: ImageVector,
    iconColor: Color
) {
    val statusColor = when {
        status.equals("COMPLETED", ignoreCase = true) -> Color(0xFF10B981)
        status.equals("IN DELIVERY", ignoreCase = true) -> Color(0xFFF59E0B)
        status.equals("FUNDS HELD", ignoreCase = true) -> Color(0xFF3B82F6)
        status.equals("UNDER REVIEW", ignoreCase = true) -> Color(0xFFDC2626)
        else -> Color(0xFF00236F)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00236F)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = date,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

data class IncomingRequestItem(
    val id: String,
    val sellerName: String,
    val itemTitle: String,
    val amount: String,
    val status: String = "PENDING_YOUR_ACTION"
)

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun BuyerDashboardPreview() {
    MaterialTheme {
        BuyerDashboardScreen()
    }
}