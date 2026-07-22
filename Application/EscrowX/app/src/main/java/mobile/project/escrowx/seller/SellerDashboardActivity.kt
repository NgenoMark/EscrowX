package mobile.project.escrowx.seller

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
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
import kotlinx.coroutines.launch
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.CreateEscrowActivity
import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.SettingsActivity
import mobile.project.escrowx.dash.TrackFinancesActivity
import mobile.project.escrowx.dash.TransactionsActivity
import mobile.project.escrowx.ui.components.SellerNavBar
import mobile.project.escrowx.ui.components.SellerNavItem
import mobile.project.escrowx.ui.components.navigateTab
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private data class SellerDashboardStats(
    val activeOrders: Int,
    val pendingPayout: Double,
    val totalEarnings: Double
)

class SellerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = ThemePreferenceManager.rememberDarkModeEnabledState()
            EscrowXTheme(
                darkTheme = darkTheme,
                dynamicColor = false
            ) {
                SellerDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen() {
    val context = LocalContext.current
    val session = SessionManager(context)
    val colorScheme = MaterialTheme.colorScheme
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var sellerOrders by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var buyerNamesById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var unreadCount by remember { mutableIntStateOf(0) }

    val sellerName = userProfile?.businessName?.takeIf { it.isNotBlank() }
        ?: userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: "Seller"
    val sellerInitials = sellerName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(Locale.getDefault()) }
        .ifBlank { "SL" }
    val sellerEmail = session.getEmail() ?: "seller@escrowx.com"

    val recentOrders = remember(sellerOrders) {
        sellerOrders.sortedByDescending { it.createdAt }.take(3)
    }

    val stats = remember(sellerOrders) {
        SellerDashboardStats(
            activeOrders = sellerOrders.count { !isTerminalSellerState(it.status) },
            pendingPayout = sellerOrders
                .filter { isPendingPayoutState(it.status) }
                .sumOf { it.amount },
            totalEarnings = sellerOrders
                .filter { isCompletedState(it.status) }
                .sumOf { it.amount }
        )
    }

    val currentDate = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(Unit) {
        val token = session.getAccessToken()
        val userEmail = session.getEmail()
        val sellerId = session.getUserId()

        if (token.isNullOrBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        val api = RetrofitClient.authenticated(token)

        if (!userEmail.isNullOrBlank()) {
            try {
                val response = api.getUserByEmail(userEmail)
                if (response.isSuccessful && response.body() != null) {
                    userProfile = response.body()
                }
            } catch (_: Exception) { }
        }

        if (!sellerId.isNullOrBlank()) {
            try {
                val response = api.getTransactionsBySeller(sellerId, null)
                if (response.isSuccessful) {
                    sellerOrders = response.body().orEmpty().sortedByDescending { it.createdAt }

                    val buyerIds = sellerOrders.map { it.buyerId }.distinct()
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
                }
            } catch (_: Exception) { }

            try {
                val unreadResponse = api.getUnreadNotificationsCount(sellerId)
                if (unreadResponse.isSuccessful) {
                    unreadCount = (unreadResponse.body()?.get("unreadCount") ?: 0L).toInt()
                }
            } catch (_: Exception) { }
        }

        isLoading = false
    }

    // ===== IMPROVED SIDEBAR =====
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ImprovedSellerDrawerContent(
                sellerName = sellerName,
                sellerInitials = sellerInitials,
                sellerEmail = sellerEmail,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onNavigateToTransactions = {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, TransactionsActivity::class.java).apply {
                        putExtra("ROLE", "SELLER")
                    })
                },
                onNavigateToProfile = {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, ProfileActivity::class.java))
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                onNavigateToTrackFinances = {
                    scope.launch { drawerState.close() }
                    context.startActivity(Intent(context, TrackFinancesActivity::class.java).apply {
                        putExtra("ROLE", "SELLER")
                    })
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    session.clearSession()
                    context.startActivity(
                        Intent(context, mobile.project.escrowx.auth.LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
            )
        }
    ) {
        Scaffold(
            containerColor = colorScheme.background,
            topBar = {
                ImprovedTopAppBar(
                    unreadCount = unreadCount,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationClick = {
                        context.startActivity(Intent(context, SellerNotificationsActivity::class.java))
                    },
                    colorScheme = colorScheme
                )
            },
            bottomBar = {
                SellerNavBar(
                    selectedIndex = selectedBottomTab,
                    onItemSelected = { item ->
                        selectedBottomTab = item.index
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
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Loading your dashboard...",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colorScheme.background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ===== WELCOME CARD =====
                item {
                    WelcomeCard(
                        sellerName = sellerName,
                        currentDate = currentDate,
                        colorScheme = colorScheme
                    )
                }

                // ===== STATS CARDS =====
                item {
                    StatsRow(
                        stats = stats,
                        colorScheme = colorScheme
                    )
                }

                // ===== ACTION BUTTONS =====
                item {
                    ActionButtonsRow(
                        onCreateEscrow = {
                            val intent = Intent(context, CreateEscrowActivity::class.java).apply {
                                putExtra("ROLE", "SELLER")
                            }
                            context.startActivity(intent)
                        },
                        onViewAllOrders = {
                            context.startActivity(Intent(context, TransactionsActivity::class.java).apply {
                                putExtra("ROLE", "SELLER")
                            })
                        },
                        colorScheme = colorScheme
                    )
                }

                // ===== RECENT ORDERS SECTION =====
                item {
                    RecentOrdersContainerCard(
                        orders = recentOrders,
                        buyerNamesById = buyerNamesById,
                        onSeeAll = {
                            context.startActivity(Intent(context, TransactionsActivity::class.java).apply {
                                putExtra("ROLE", "SELLER")
                            })
                        },
                        onViewOrder = { order, buyerName ->
                            val currentStep = when (normalizeSellerState(order.status)) {
                                "FUNDS_HELD", "SELLER_ACCEPTED" -> 1
                                "IN_DELIVERY" -> 2
                                "SELLER_DELIVERED", "BUYER_CONFIRMED_DELIVERED",
                                "RELEASE_PENDING", "RELEASE_PROCESSING", "RELEASE_FAILED" -> 3
                                else -> 4
                            }

                            val intent = Intent(context, SellerTransactionDetailActivity::class.java).apply {
                                putExtra("TRANSACTION_ID", order.id)
                                putExtra("PRODUCT_NAME", order.title)
                                putExtra("BUYER_NAME", buyerName)
                                putExtra("BUYER_INITIALS", buyerName.take(2).uppercase(Locale.getDefault()))
                                putExtra("AMOUNT", order.amount.toString())
                                putExtra("ORDER_ID", order.reference ?: order.id)
                                putExtra("DATE", order.createdAt.take(10))
                                putExtra("SHIPPING_ADDRESS", order.deliveryAddress)
                                putExtra("STATUS", order.status)
                                putExtra("CURRENT_STEP", currentStep)
                            }
                            context.startActivity(intent)
                        },
                        colorScheme = colorScheme
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ===== IMPROVED SIDEBAR =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedSellerDrawerContent(
    sellerName: String,
    sellerInitials: String,
    sellerEmail: String,
    onCloseDrawer: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrackFinances: () -> Unit,
    onLogout: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedItem by remember { mutableStateOf(DrawerNavItem.HOME) }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp),
        drawerContainerColor = colorScheme.surface,
        drawerShape = RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp
        )
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00236F),
                            Color(0xFF1A4B8C)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sellerInitials,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = sellerName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sellerEmail,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        DrawerMenuItem(
            icon = Icons.Default.Dashboard,
            label = "Dashboard",
            isSelected = selectedItem == DrawerNavItem.HOME,
            onClick = {
                selectedItem = DrawerNavItem.HOME
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.ReceiptLong,
            label = "Transactions",
            isSelected = selectedItem == DrawerNavItem.TRANSACTIONS,
            onClick = {
                selectedItem = DrawerNavItem.TRANSACTIONS
                onCloseDrawer()
                onNavigateToTransactions()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Person,
            label = "Profile",
            isSelected = selectedItem == DrawerNavItem.PROFILE,
            onClick = {
                selectedItem = DrawerNavItem.PROFILE
                onCloseDrawer()
                onNavigateToProfile()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            isSelected = selectedItem == DrawerNavItem.SETTINGS,
            onClick = {
                selectedItem = DrawerNavItem.SETTINGS
                onCloseDrawer()
                onNavigateToSettings()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.AccountBalance,
            label = "Track Finances",
            isSelected = selectedItem == DrawerNavItem.FINANCES,
            onClick = {
                selectedItem = DrawerNavItem.FINANCES
                onCloseDrawer()
                onNavigateToTrackFinances()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Version info
        Text(
            text = "EscrowX v1.0.0",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        HorizontalDivider(
            color = colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Logout
        DrawerMenuItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            label = "Logout",
            isSelected = false,
            onClick = onLogout,
            isDestructive = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    badge: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = when {
                    isDestructive -> Color(0xFFDC2626)
                    isSelected -> colorScheme.primary
                    else -> colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp)
            )

            Text(
                label,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    isDestructive -> Color(0xFFDC2626)
                    isSelected -> colorScheme.primary
                    else -> colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            badge?.let {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDC2626)
                ) {
                    Text(
                        it,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

enum class DrawerNavItem { HOME, TRANSACTIONS, PROFILE, SETTINGS, FINANCES }

// ===== IMPROVED TOP APP BAR =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTopAppBar(
    unreadCount: Int,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    colorScheme: ColorScheme
) {
    TopAppBar(
        title = {
            Text(
                "EscrowX",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00236F),
                letterSpacing = 0.5.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color(0xFF00236F)
                )
            }
        },
        actions = {
            // Notification Bell
            BadgedBox(
                modifier = Modifier.padding(end = 8.dp, top = 4.dp),
                badge = {
                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.offset(x = (-2).dp, y = 2.dp),
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ) {
                            Text(
                                if (unreadCount > 9) "9+" else unreadCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            ) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = Color(0xFF00236F)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.surface,
            scrolledContainerColor = colorScheme.surface
        )
    )
}

// ===== WELCOME CARD =====

@Composable
fun WelcomeCard(
    sellerName: String,
    currentDate: String,
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Hello, $sellerName ",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = currentDate,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "Your store is active",
                    fontSize = 13.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ===== STATS ROW =====

@Composable
private fun StatsRow(
    stats: SellerDashboardStats,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Active\nOrders",
            value = stats.activeOrders.toString(),
            icon = Icons.Default.ShoppingCart,
            accentColor = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f),
            colorScheme = colorScheme
        )
        StatCard(
            title = "Pending\nPayouts",
            value = formatKes(stats.pendingPayout),
            icon = Icons.Default.AccountBalanceWallet,
            accentColor = Color(0xFF7C3AED),
            modifier = Modifier.weight(1f),
            colorScheme = colorScheme
        )
        StatCard(
            title = "Total\nEarnings",
            value = formatKes(stats.totalEarnings),
            icon = Icons.Default.TrendingUp,
            accentColor = Color(0xFF10B981),
            modifier = Modifier.weight(1f),
            colorScheme = colorScheme
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accentColor
                    )
                }
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ===== ACTION BUTTONS ROW =====

@Composable
fun ActionButtonsRow(
    onCreateEscrow: () -> Unit,
    onViewAllOrders: () -> Unit,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onCreateEscrow,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00236F),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Create Escrow",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        OutlinedButton(
            onClick = onViewAllOrders,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00236F)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF00236F)
            )
        ) {
            Icon(
                Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "View Orders",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// ===== SECTION HEADER =====

@Composable
fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
        }
        TextButton(
            onClick = onSeeAll,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorScheme.primary
            )
        ) {
            Text("See All", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun RecentOrdersContainerCard(
    orders: List<EscrowResponse>,
    buyerNamesById: Map<String, String>,
    onSeeAll: () -> Unit,
    onViewOrder: (EscrowResponse, String) -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(
                title = "Recent Orders",
                onSeeAll = onSeeAll,
                colorScheme = colorScheme
            )

            if (orders.isEmpty()) {
                EmptyOrdersCard(colorScheme = colorScheme)
            } else {
                orders.forEach { order ->
                    val buyerName = buyerNamesById[order.buyerId] ?: "Buyer ${order.buyerId.take(6)}"
                    ImprovedRecentOrderCard(
                        order = order,
                        buyerName = buyerName,
                        onView = { onViewOrder(order, buyerName) }
                    )
                }
            }
        }
    }
}

// ===== IMPROVED RECENT ORDER CARD =====

@Composable
fun ImprovedRecentOrderCard(
    order: EscrowResponse,
    buyerName: String,
    onView: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val status = normalizeSellerState(order.status)
    val (statusTextColor, statusBgColor) = sellerStatusBadgeColors(status)
    val formattedDate = formatDate(order.createdAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onView() }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Status & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBgColor,
                    border = BorderStroke(1.dp, statusBgColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = prettySellerStatus(status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextColor
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Text(
                        formattedDate,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Product & Buyer Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F3FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color(0xFF00236F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Buyer: $buyerName",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom Row: Amount & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Amount",
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatKes(order.amount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00236F)
                    )
                }

                Button(
                    onClick = onView,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00236F),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        "View Details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ===== EMPTY ORDERS CARD =====

@Composable
fun EmptyOrdersCard(colorScheme: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = colorScheme.primary
                )
            }
            Text(
                "No Orders Yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                "Your orders will appear here once you start selling",
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===== HELPER FUNCTIONS =====

private fun formatDate(dateString: String): String {
    return try {
        val parts = dateString.split("T")
        val dateParts = parts[0].split("-")
        val month = when (dateParts[1].toInt()) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
            7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> dateParts[1]
        }
        "$month ${dateParts[2]}, ${dateParts[0]}"
    } catch (_: Exception) {
        dateString
    }
}

private fun normalizeSellerState(state: String): String = state.trim().uppercase(Locale.getDefault())

private fun prettySellerStatus(state: String): String {
    return state
        .lowercase(Locale.getDefault())
        .split("_")
        .joinToString(" ") { token -> token.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }
}

private fun formatKes(amount: Double): String {
    return "KES ${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)}"
}

private fun isTerminalSellerState(state: String): Boolean {
    return normalizeSellerState(state) in setOf("COMPLETED", "DECLINED", "CANCELLED", "REFUNDED", "EXPIRED")
}

private fun isCompletedState(state: String): Boolean {
    return normalizeSellerState(state) == "COMPLETED"
}

private fun isPendingPayoutState(state: String): Boolean {
    return normalizeSellerState(state) in setOf(
        "SELLER_DELIVERED",
        "BUYER_CONFIRMED_DELIVERED",
        "RELEASE_PENDING",
        "RELEASE_PROCESSING",
        "RELEASE_FAILED"
    )
}

private fun isAttentionNeededState(state: String): Boolean {
    return normalizeSellerState(state) in setOf("PENDING_PAYMENT", "DISPUTED", "RELEASE_FAILED")
}

private fun sellerStatusBadgeColors(state: String): Pair<Color, Color> {
    return when (state) {
        "COMPLETED" -> Color(0xFF10B981) to Color(0xFFD1FAE5)
        "DECLINED", "CANCELLED", "EXPIRED" -> Color(0xFF5E5E66) to Color(0xFFECECEC)
        "DISPUTED", "RELEASE_FAILED", "REFUND_PENDING", "REFUND_PROCESSING" -> Color(0xFFDC2626) to Color(0xFFFFDAD6)
        "RELEASE_PENDING", "RELEASE_PROCESSING" -> Color(0xFF7C3AED) to Color(0xFFEDE9FE)
        "FUNDS_HELD", "PENDING_PAYMENT" -> Color(0xFFE65100) to Color(0xFFFFEACC)
        else -> Color(0xFF1D4ED8) to Color(0xFFE7EEFE)
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SellerDashboardPreview() {
    EscrowXTheme(darkTheme = false) {
        SellerDashboardScreen()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SellerDashboardPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        SellerDashboardScreen()
    }
}