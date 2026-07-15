@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.dash

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.LoginActivity
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.seller.SellerNotificationsActivity
import mobile.project.escrowx.ui.components.BuyerNavBar
import mobile.project.escrowx.ui.components.BuyerNavItem
import mobile.project.escrowx.ui.components.navigateTab
import mobile.project.escrowx.ui.theme.BrandBlue
import java.text.NumberFormat
import java.util.*

class BuyerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
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
    val colorScheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current
    val userName by viewModel.userName.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var unreadNotificationsCount by remember { mutableIntStateOf(0) }

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
                } catch (_: Exception) { }
            }
        }
    }

    fun loadUnreadNotificationsCount() {
        val token = session.getAccessToken()
        val actorId = session.getUserId()
        if (token.isNullOrBlank() || actorId.isNullOrBlank()) {
            unreadNotificationsCount = 0
            return
        }

        scope.launch {
            try {
                val response = RetrofitClient.authenticated(token).getUnreadNotificationsCount(actorId)
                if (response.isSuccessful) {
                    val count = (response.body()?.get("count") ?: 0L).toInt()
                    unreadNotificationsCount = count
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) {
        loadUnreadNotificationsCount()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadUnreadNotificationsCount()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val displayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: userProfile?.email?.substringBefore("@")
        ?: userName

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
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
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    BrandBlue,
                                    BrandBlue.copy(alpha = 0.8f)
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
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = session.getEmail() ?: "user@escrowx.com",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }

                // Drawer Items (streamlined)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = "Dashboard",
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "Dashboard",
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                    },
                    selected = true,
                    onClick = {
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = "Transactions",
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "Transactions",
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, TransactionsActivity::class.java).apply {
                            putExtra("ROLE", "BUYER")
                        })
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "My Profile",
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, ProfileActivity::class.java))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "Settings",
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = "Track Finances",
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "Track Finances",
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, TrackFinancesActivity::class.java).apply {
                            putExtra("ROLE", "BUYER")
                        })
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = colorScheme.outlineVariant
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFDC2626)
                        )
                    },
                    label = {
                        Text(
                            "Logout",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Text(
                    text = "EscrowX v1.0",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
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
                            color = colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = {
                                context.startActivity(Intent(context, SellerNotificationsActivity::class.java))
                                loadUnreadNotificationsCount()
                            }) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = colorScheme.primary
                                )
                            }

                            if (unreadNotificationsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
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
                BuyerNavBar(
                    selectedIndex = selectedTab,
                    onItemSelected = { item ->
                        selectedTab = item.index
                        when (item) {
                            BuyerNavItem.Home -> {
                                navigateTab(context, BuyerDashboardActivity::class.java)
                            }
                            BuyerNavItem.Transactions -> {
                                navigateTab(context, TransactionsActivity::class.java)
                            }
                            BuyerNavItem.Profile -> {
                                navigateTab(context, ProfileActivity::class.java)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeTabContent(padding, context, displayName)
                1 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                2 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(paddingValues: PaddingValues, context: Context, displayName: String) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)
    val brandPrimary = colorScheme.primary

    var realIncomingTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var allBuyerTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var sellerNamesById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingReal by remember { mutableStateOf(true) }
    var realError by remember { mutableStateOf<String?>(null) }

    val pendingStates = remember {
        setOf("CREATED", "PENDING_PAYMENT")
    }
    val completedStates = remember {
        setOf("COMPLETED", "CANCELLED", "DECLINED", "REFUNDED", "EXPIRED")
    }

    val pendingCount = remember(allBuyerTransactions) {
        allBuyerTransactions.count { txn ->
            txn.status.trim().uppercase(Locale.ROOT) in pendingStates
        }
    }
    val completedCount = remember(allBuyerTransactions) {
        allBuyerTransactions.count { txn ->
            txn.status.trim().uppercase(Locale.ROOT) in completedStates
        }
    }
    val activeCount = remember(allBuyerTransactions) {
        allBuyerTransactions.count { txn ->
            val status = txn.status.trim().uppercase(Locale.ROOT)
            status !in pendingStates && status !in completedStates
        }
    }

    fun formatAmount(amount: Double): String {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(amount)
    }

    fun loadRealIncoming() {
        scope.launch {
            isLoadingReal = true
            realError = null
            try {
                val token = session.getAccessToken()
                val buyerId = session.getUserId()
                if (token.isNullOrBlank() || buyerId.isNullOrBlank()) {
                    realError = "Session expired. Please login again."
                    sellerNamesById = emptyMap()
                    isLoadingReal = false
                    return@launch
                }
                val api = RetrofitClient.authenticated(token)
                val response = api.getTransactionsByBuyer(buyerId, null)
                if (response.isSuccessful && response.body() != null) {
                    val buyerTransactions = response.body()!!
                    allBuyerTransactions = buyerTransactions
                    val createdTransactions = buyerTransactions.filter {
                        it.status.equals("CREATED", ignoreCase = true)
                    }
                    realIncomingTransactions = createdTransactions

                    val sellerIds = createdTransactions.map { it.sellerId }.distinct()
                    sellerNamesById = coroutineScope {
                        sellerIds.map { sellerId ->
                            async {
                                val sellerName = try {
                                    val sellerResponse = api.getUserById(sellerId)
                                    if (sellerResponse.isSuccessful) {
                                        sellerResponse.body()?.displayName?.takeIf { it.isNotBlank() }
                                    } else {
                                        null
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                                sellerId to (sellerName ?: "Seller ${sellerId.take(6)}")
                            }
                        }.map { it.await() }.toMap()
                    }
                } else if (response.code() == 404) {
                    // Backward compatibility with older backend behavior that returned 404 on empty results.
                    allBuyerTransactions = emptyList()
                    realIncomingTransactions = emptyList()
                    sellerNamesById = emptyMap()
                } else {
                    allBuyerTransactions = emptyList()
                    sellerNamesById = emptyMap()
                    realError = "Failed to load: ${response.code()}"
                }
            } catch (e: Exception) {
                allBuyerTransactions = emptyList()
                sellerNamesById = emptyMap()
                realError = "Network error. ${e.message ?: "Please check your connection."}"
            } finally {
                isLoadingReal = false
            }
        }
    }

    fun approveTransaction(transactionId: String, onSuccess: () -> Unit) {
        scope.launch {
            try {
                val token = session.getAccessToken()
                val actorUserId = session.getUserId()
                if (token.isNullOrBlank() || actorUserId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val response = RetrofitClient.authenticated(token)
                    .approveTransaction(transactionId, actorUserId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Transaction approved!", Toast.LENGTH_LONG).show()
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("Approval failed: ${response.code()} - $errorBody")
                    Toast.makeText(context, "Approval failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                println("Error approving transaction: ${e.message}")
                Toast.makeText(context, "Error approving transaction", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun declineTransaction( transactionId: String, onSuccess:  () -> Unit){
        scope.launch{
            try {
                val token = session.getAccessToken()
                val actorUserId = session.getUserId()
                if (token.isNullOrBlank() || actorUserId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val response = RetrofitClient.authenticated(token)
                    .declineTransaction(transactionId, actorUserId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Transaction Declined!", Toast.LENGTH_LONG).show()
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("Decline failed: ${response.code()} - $errorBody")
                    Toast.makeText(context, "Decline failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                println("Error declining transaction: ${e.message}")
                Toast.makeText(context, "Error declining transaction", Toast.LENGTH_SHORT).show()
            }

        }

    }

    LaunchedEffect(Unit) {
        loadRealIncoming()
    }

    val awaitingAcceptanceItems = remember(realIncomingTransactions, sellerNamesById) {
        realIncomingTransactions.map { txn ->
            IncomingRequestItem(
                id = txn.id,
                reference = txn.reference?.ifBlank { txn.id } ?: txn.id,
                sellerName = sellerNamesById[txn.sellerId] ?: "Seller ${txn.sellerId.take(6)}",
                itemTitle = txn.title,
                amount = formatAmount(txn.amount),
                status = txn.status
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Welcome Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(
                1.dp,
                colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Welcome back",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    letterSpacing = 0.3.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Manage your escrows securely from one place.",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Pending,
                title = "Pending",
                value = pendingCount.toString(),
                color = Color(0xFFF59E0B),
                colorScheme = colorScheme
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                title = "Active",
                value = activeCount.toString(),
                color = Color(0xFF10B981),
                colorScheme = colorScheme
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History,
                title = "Completed",
                value = completedCount.toString(),
                color = BrandBlue,
                colorScheme = colorScheme
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // How It Works Section
        ExpandableHowItWorksCard()

        Spacer(modifier = Modifier.height(24.dp))

        // Transactions Awaiting Acceptance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardSectionHeader(title = "Awaiting Acceptance")
            TextButton(
                onClick = {
                    val intent = Intent(context, TransactionsActivity::class.java).apply {
                        putExtra("ROLE", "BUYER")
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = brandPrimary
                )
            ) {
                Text(
                    "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoadingReal -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = brandPrimary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Loading transactions...",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            realError != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            realError!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { loadRealIncoming() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brandPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
            awaitingAcceptanceItems.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
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
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(brandPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = brandPrimary
                            )
                        }
                        Text(
                            "All Clear!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            "No transactions awaiting your acceptance",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(awaitingAcceptanceItems) { request ->
                        IncomingRequestCard(
                            request = request,
                            onAccept = {
                                approveTransaction(request.id) {
                                    loadRealIncoming()
                                }
                            },
                            onDecline = {
                                declineTransaction(request.id){
                                    loadRealIncoming()
                                }
                            }
//                            {
//                                Toast.makeText(
//                                    context,
//                                    "Decline action not enabled on this screen yet.",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logistics Section
        DashboardSectionHeader(title = "Logistics")
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(
                1.dp,
                colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brandPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = brandPrimary
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Shipping & Delivery",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        "Logistics details will be added here in the next update",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DashboardSectionHeader(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = title.uppercase(Locale.getDefault()),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp
    )
}

@Composable
fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    colorScheme: ColorScheme
) {
    Card(
        modifier = modifier
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            }
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HowItWorksStep(
    number: Int,
    title: String,
    description: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
            Text(
                description,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExpandableHowItWorksCard() {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "How Escrow Works",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (expanded) "Tap to collapse" else "Tap to view steps",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                    HowItWorksStep(
                        number = 1,
                        title = "Create or Receive",
                        description = "Create an escrow request or receive one from a seller"
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                    HowItWorksStep(
                        number = 2,
                        title = "Review & Accept",
                        description = "Review transaction details and accept the escrow"
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                    HowItWorksStep(
                        number = 3,
                        title = "Secure Completion",
                        description = "Funds stay secure until all terms are met"
                    )
                }
            }
        }
    }
}

// ===================== COMPOSABLES =====================

@Composable
fun IncomingRequestCard(
    request: IncomingRequestItem,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(340.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Badge & Reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Awaiting Action",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "Ref: ${request.reference.take(8)}",
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // Main Content - Seller & Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Seller Avatar with gradient
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.primary,
                                    colorScheme.primary.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.sellerName
                            .split(" ")
                            .map { it.firstOrNull()?.toString() ?: "" }
                            .joinToString("")
                            .take(2)
                            .uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        request.sellerName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        request.itemTitle,
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Divider
            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )

            // Transaction Summary - Compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryChip(
                    label = "Amount",
                    value = "KES ${request.amount}",
                    icon = Icons.Default.AttachMoney,
                    colorScheme = colorScheme
                )
                SummaryChip(
                    label = "Status",
                    value = "Pending",
                    icon = Icons.Default.Pending,
                    colorScheme = colorScheme
                )
                SummaryChip(
                    label = "Reference",
                    value = request.reference.take(6),
                    icon = Icons.Default.LocalOffer,
                    colorScheme = colorScheme
                )
            }

            // Expandable Details
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailRow(
                                label = "Transaction ID",
                                value = request.id,
                                colorScheme = colorScheme
                            )
                            DetailRow(
                                label = "Full Reference",
                                value = request.reference,
                                colorScheme = colorScheme
                            )
                            DetailRow(
                                label = "Seller",
                                value = request.sellerName,
                                colorScheme = colorScheme
                            )
                            DetailRow(
                                label = "Item",
                                value = request.itemTitle,
                                colorScheme = colorScheme
                            )
                            DetailRow(
                                label = "Amount",
                                value = "KES ${request.amount}",
                                colorScheme = colorScheme,
                                isHighlighted = true
                            )
                        }
                    }
                }
            }

            // Expand/Collapse Button
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Show less" else "Show more",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (expanded) "Show Less" else "Show Details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Decline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
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
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Approve",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
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
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .width(300.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
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
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = topStatusColor
                ) {
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = partyIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = partyName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeLeftText,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onTrackClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Track",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Track",
                        modifier = Modifier.size(18.dp)
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
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = when {
        status.equals("COMPLETED", ignoreCase = true) -> Color(0xFF10B981)
        status.equals("IN DELIVERY", ignoreCase = true) -> Color(0xFFF59E0B)
        status.equals("FUNDS HELD", ignoreCase = true) -> Color(0xFF3B82F6)
        status.equals("UNDER REVIEW", ignoreCase = true) -> Color(0xFFDC2626)
        else -> colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = date,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class IncomingRequestItem(
    val id: String,
    val reference: String,
    val sellerName: String,
    val itemTitle: String,
    val amount: String,
    val status: String = "PENDING_YOUR_ACTION"
)

@Composable
fun SummaryChip(
    label: String,
    value: String,
    icon: ImageVector,
    colorScheme: ColorScheme
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    colorScheme: ColorScheme,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = if (isHighlighted) colorScheme.primary else colorScheme.onSurface,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun BuyerDashboardPreview() {
    EscrowXTheme(darkTheme = false) {
        BuyerDashboardScreen()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun BuyerDashboardPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        BuyerDashboardScreen()
    }
}