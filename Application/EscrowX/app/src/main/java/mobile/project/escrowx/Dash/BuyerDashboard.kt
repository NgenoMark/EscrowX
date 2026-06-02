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

    val displayName = userProfile?.displayName ?: userName

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(16.dp)
            ) {
                // Drawer Header
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

                // Settings
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

                // App Info
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

                // Spacer to push content to top and logout to bottom
                Spacer(modifier = Modifier.weight(1f))

                // Divider before Logout
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.LightGray
                )

                // LOGOUT BUTTON AT BOTTOM
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
                        // Clear session and navigate to login
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

                // Footer with version
                Text(
                    text = "EscrowX v1.0",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                    // Profile tab - handled by navigation above
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFF9F9FF))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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

        Button(
            onClick = { context.startActivity(Intent(context, CreateEscrowActivity::class.java)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Escrow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Escrows", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
            TextButton(onClick = { }) {
                Text("View All", color = Color(0xFF00236F))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View All", modifier = Modifier.size(16.dp), tint = Color(0xFF00236F))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(2) { index ->
                if (index == 0) {
                    ActiveEscrowCard(
                        topStatus = "PENDING DELIVERY",
                        topStatusColor = Color(0xFFFEF3C7),
                        topStatusTextColor = Color(0xFFD97706),
                        amountText = "KES 12,500",
                        bottomStatus = "IN INSPECTION",
                        bottomStatusColor = Color(0xFFE7EEFE),
                        bottomStatusTextColor = Color(0xFF00236F),
                        partyName = "Jumia Electronics",
                        partyIcon = Icons.Default.Store,
                        timeLeftText = "2 days left"
                    )
                } else {
                    ActiveEscrowCard(
                        topStatus = "PENDING PAYMENT",
                        topStatusColor = Color(0xFFFEE2E2),
                        topStatusTextColor = Color(0xFFDC2626),
                        amountText = "KES 35,000",
                        bottomStatus = "AWAITING",
                        bottomStatusColor = Color(0xFFFEF3C7),
                        bottomStatusTextColor = Color(0xFFD97706),
                        partyName = "Samsung Store",
                        partyIcon = Icons.Default.ShoppingCart,
                        timeLeftText = "5 days left"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))

        Spacer(modifier = Modifier.height(12.dp))

        TransactionListItem(
            title = "iPhone 15 Pro",
            subtitle = "Seller: Phone Mart Kenya",
            amount = "KES 125,000",
            date = "Oct 24, 2023",
            status = "COMPLETED",
            icon = Icons.Default.Store,
            iconColor = Color(0xFF00236F)
        )

        TransactionListItem(
            title = "Samsung 65\" TV",
            subtitle = "Seller: Electronics Hub",
            amount = "KES 85,000",
            date = "Oct 22, 2023",
            status = "IN DELIVERY",
            icon = Icons.Default.Tv,
            iconColor = Color(0xFF00236F)
        )
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
    timeLeftText: String
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
                    Text(text = topStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = topStatusTextColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Text(text = amountText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(shape = RoundedCornerShape(4.dp), color = bottomStatusColor, modifier = Modifier.align(Alignment.Start)) {
                Text(text = bottomStatus, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = bottomStatusTextColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = partyIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00236F))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = partyName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
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
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timeLeftText, fontSize = 11.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Text(text = "Track", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00236F))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Track", modifier = Modifier.size(14.dp), tint = Color(0xFF00236F))
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
        else -> Color(0xFF00236F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
                Spacer(modifier = Modifier.height(2.dp))
                Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.1f)) {
                    Text(text = status, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = statusColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Text(text = date, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun BuyerDashboardPreview() {
    MaterialTheme {
        BuyerDashboardScreen()
    }
}