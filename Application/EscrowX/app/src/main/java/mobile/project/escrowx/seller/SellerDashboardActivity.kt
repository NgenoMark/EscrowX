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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.CreateEscrowActivity   // ✅ unified escrow creation
import mobile.project.escrowx.dash.DisputeCenterActivity
import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.SettingsActivity
import mobile.project.escrowx.dash.TransactionsActivity
import mobile.project.escrowx.ui.components.SellerNavBar
import mobile.project.escrowx.ui.components.SellerNavItem
import mobile.project.escrowx.ui.components.navigateTab

data class SellerRecentTransaction(
    val id: String,
    val buyerName: String,
    val itemTitle: String,
    val amount: String,
    val status: String = "PENDING"
)

class SellerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
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
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }

    // Mock recent transactions – replace with real data later
    val recentTransactions = listOf(
        SellerRecentTransaction(
            id = "1",
            buyerName = "Jane Kamau",
            itemTitle = "MacBook Air M2 2023",
            amount = "120,000",
            status = "PENDING"
        ),
        SellerRecentTransaction(
            id = "2",
            buyerName = "David Mutua",
            itemTitle = "iPhone 15 Pro Max",
            amount = "165,000",
            status = "PENDING"
        )
    )

    // Fetch user profile from backend
    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()
            if (!token.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        userProfile = response.body()
                    }
                } catch (_: Exception) {
                    // Ignore errors for now
                }
            }
            isLoading = false
        }
    }

    // Prefer business name for seller, fallback to display name or "Seller"
    val sellerName = userProfile?.businessName?.takeIf { it.isNotBlank() }
        ?: userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: "Seller"
    val sellerInitials = sellerName.take(2).uppercase()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SellerDrawerContent(
                sellerName = sellerName,
                sellerInitials = sellerInitials,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onNavigateToTransactions = {
                    val intent = Intent(context, TransactionsActivity::class.java).apply {
                        putExtra("ROLE", "SELLER")
                    }
                    context.startActivity(intent)
                },
                onNavigateToProfile = {
                    context.startActivity(Intent(context, ProfileActivity::class.java))
                },
                onNavigateToDisputes = {
                    context.startActivity(Intent(context, DisputeCenterActivity::class.java))
                },
                onNavigateToSettings = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                onLogout = {
                    session.clearSession()
                    context.startActivity(
                        Intent(context, mobile.project.escrowx.auth.LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
            )
        },
        gesturesEnabled = drawerState.isOpen || drawerState.isAnimationRunning
    ) {
        Scaffold(
            containerColor = colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "EscrowX",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Notifications */ }) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCE2F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface
                    )
                )
            },
            bottomBar = {
                SellerNavBar(
                    selectedIndex = selectedBottomTab,
                    onItemSelected = { item ->
                        selectedBottomTab = item.index
                        when (item) {
                            SellerNavItem.Home -> {
                                navigateTab(context, SellerDashboardActivity::class.java)
                            }
                            SellerNavItem.Transactions -> {
                                navigateTab(
                                    context,
                                    TransactionsActivity::class.java,
                                    Bundle().apply { putString("ROLE", "SELLER") }
                                )
                            }
                            SellerNavItem.Profile -> {
                                navigateTab(context, ProfileActivity::class.java)
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colorScheme.background)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                } else {
                    // Welcome Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Hello, $sellerName",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF006C49)
                            )
                        }
                        Text(
                            text = "Your business is thriving today.",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // New Escrow Button – launches unified CreateEscrowActivity with ROLE SELLER
                    Button(
                        onClick = {
                            val intent = Intent(context, CreateEscrowActivity::class.java).apply {
                                putExtra("ROLE", "SELLER")
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Actions Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Default.Link,
                            iconBgColor = Color(0xFF6CF8BB),
                            iconTint = Color(0xFF00714D),
                            title = "Create Payment Link",
                            onClick = {
                                context.startActivity(Intent(context, CreatePaymentLinkActivity::class.java))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            icon = Icons.Default.MoveToInbox,
                            iconBgColor = Color(0xFFE7EEFE),
                            iconTint = Color(0xFF00236F),
                            title = "Incoming Escrows",
                            onClick = {
                                context.startActivity(Intent(context, IncomingEscrowsActivity::class.java))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Actions Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Default.Gavel,
                            iconBgColor = Color(0xFFFFDAD6),
                            iconTint = Color(0xFFBA1A1A),
                            title = "Disputes",
                            onClick = {
                                context.startActivity(Intent(context, DisputeCenterActivity::class.java))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            icon = Icons.Default.Sync,
                            iconBgColor = Color(0xFFF0F3FF),
                            iconTint = Color(0xFF3E2400),
                            title = "Active Escrows",
                            onClick = {
                                context.startActivity(Intent(context, SellerActiveEscrowsActivity::class.java))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Recent Transactions Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Transactions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF151C27)
                        )
                        TextButton(onClick = { /* View all */ }) {
                            Text("View all", color = Color(0xFF00236F), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentTransactions) { transaction ->
                            RecentTransactionCard(transaction = transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerDrawerContent(
    sellerName: String,
    sellerInitials: String,
    onCloseDrawer: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDisputes: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFE7EEFE))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sellerInitials,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF00236F)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sellerName,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF151C27)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = false,
            onClick = onCloseDrawer
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            label = { Text("Transactions") },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToTransactions()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToProfile()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Gavel, contentDescription = null) },
            label = { Text("Disputes") },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToDisputes()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToSettings()
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            label = { Text("Logout") },
            selected = false,
            onClick = {
                onCloseDrawer()
                onLogout()
            }
        )
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3FF)),
        border = BorderStroke(1.dp, Color(0xFFC5C5D3))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF151C27))
        }
    }
}

@Composable
fun RecentTransactionCard(transaction: SellerRecentTransaction) {
    val statusColor = when {
        transaction.status.equals("COMPLETED", ignoreCase = true) -> Color(0xFF10B981)
        transaction.status.equals("IN DELIVERY", ignoreCase = true) -> Color(0xFFF59E0B)
        else -> Color(0xFFF59E0B)
    }
    val statusBgColor = when {
        transaction.status.equals("COMPLETED", ignoreCase = true) -> Color(0xFFD1FAE5)
        transaction.status.equals("IN DELIVERY", ignoreCase = true) -> Color(0xFFFEF3C7)
        else -> Color(0xFFFFDDB8)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE7EEFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Buyer", tint = Color(0xFF444651), modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(transaction.buyerName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                    Text(transaction.itemTitle, fontSize = 13.sp, color = Color(0xFF444651))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("KES ${transaction.amount}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00236F))
                Surface(shape = RoundedCornerShape(4.dp), color = statusBgColor) {
                    Text(transaction.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
        }
    }
}
