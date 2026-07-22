package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.seller.SellerNotificationsActivity
import mobile.project.escrowx.ui.components.RiderNavBar
import mobile.project.escrowx.ui.components.RiderNavItem
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import mobile.project.escrowx.ui.theme.BrandBlue
import java.text.SimpleDateFormat
import java.util.*

private data class RiderAssignmentItem(
    val transaction: EscrowResponse,
    val buyerName: String? = null
)

class RiderDashboardActvity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                RiderDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderDashboardScreen() {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var assignments by remember { mutableStateOf<List<RiderAssignmentItem>>(emptyList()) }
    var newAssignmentsCount by remember { mutableIntStateOf(0) }
    var unreadNotificationsCount by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastBackPressAt by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (selectedTab != RiderNavItem.Home.index) {
            selectedTab = RiderNavItem.Home.index
            return@BackHandler
        }

        val now = System.currentTimeMillis()
        if (now - lastBackPressAt <= 2000L) {
            (context as? ComponentActivity)?.finishAffinity()
        } else {
            lastBackPressAt = now
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadRiderData(showLoading: Boolean = true) {
        if (showLoading) isLoading = true
        isRefreshing = true

        val token = session.getAccessToken()
        val email = session.getEmail()
        val userId = session.getUserId()
        if (token.isNullOrBlank() || email.isNullOrBlank() || userId.isNullOrBlank()) {
            isLoading = false
            isRefreshing = false
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.authenticated(token)

                val userResponse = api.getUserByEmail(email)
                if (userResponse.isSuccessful) {
                    profile = userResponse.body()
                }

                val assignmentsResponse = api.getTransactionsByRider(userId)
                val riderTransactions = if (assignmentsResponse.isSuccessful) {
                    assignmentsResponse.body().orEmpty()
                } else {
                    emptyList()
                }

                val buyerNameMap = riderTransactions
                    .map { it.buyerId }
                    .distinct()
                    .associateWith { buyerId ->
                        try {
                            val buyerResponse = api.getUserById(buyerId)
                            if (buyerResponse.isSuccessful) {
                                val buyer = buyerResponse.body()
                                buyer?.displayName?.takeIf { it.isNotBlank() }
                                    ?: buyer?.email?.substringBefore("@")
                            } else {
                                null
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }

                val unreadResponse = api.getUnreadNotificationsCount(userId)
                unreadNotificationsCount = if (unreadResponse.isSuccessful) {
                    (unreadResponse.body()?.get("unreadCount") ?: 0L).toInt()
                } else {
                    0
                }

                assignments = riderTransactions
                    .sortedByDescending { it.updatedAt }
                    .map { txn ->
                        RiderAssignmentItem(
                            transaction = txn,
                            buyerName = buyerNameMap[txn.buyerId]
                        )
                    }

                newAssignmentsCount = assignments.count {
                    it.transaction.status.equals("ASSIGNED", ignoreCase = true)
                }
            } catch (_: Exception) {
                assignments = emptyList()
                newAssignmentsCount = 0
                unreadNotificationsCount = 0
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    isRefreshing = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRiderData()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            val isHomeTab = selectedTab == RiderNavItem.Home.index
            TopAppBar(
                title = {
                    if (isHomeTab) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
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
                                Icon(
                                    Icons.Default.DirectionsBike,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Rider Workspace",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = colorScheme.onSurface,
                                letterSpacing = 0.3.sp
                            )
                        }
                    } else {
                        Text(
                            text = if (selectedTab == RiderNavItem.Assignments.index) "Assignments" else "Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isHomeTab) {
                        BadgedBox(
                            modifier = Modifier.padding(end = 8.dp, top = 4.dp),
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        modifier = Modifier.offset(x = (-2).dp, y = 2.dp),
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = {
                                context.startActivity(Intent(context, SellerNotificationsActivity::class.java))
                            }) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = "Notifications",
                                    tint = colorScheme.onSurface
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
            RiderNavBar(
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it.index }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                RiderLoadingState(colorScheme = colorScheme)
            }
            selectedTab == RiderNavItem.Home.index -> {
                RiderHomeTab(
                    padding = padding,
                    riderName = profile?.displayName?.ifBlank { null }
                        ?: profile?.email?.substringBefore("@")
                        ?: "Rider",
                    newAssignmentsCount = newAssignmentsCount,
                    assignments = assignments,
                    onOpenAssignmentDetails = { transaction ->
                        context.startActivity(
                            Intent(context, RiderAssignmentDetailsActivity::class.java).apply {
                                putExtra(RiderAssignmentDetailsActivity.EXTRA_TRANSACTION_ID, transaction.id)
                            }
                        )
                    },
                    colorScheme = colorScheme
                )
            }
            selectedTab == RiderNavItem.Assignments.index -> {
                RiderAssignmentsTab(
                    padding = padding,
                    assignments = assignments,
                    colorScheme = colorScheme,
                    onOpenAssignmentDetails = { transaction ->
                        context.startActivity(
                            Intent(context, RiderAssignmentDetailsActivity::class.java).apply {
                                putExtra(RiderAssignmentDetailsActivity.EXTRA_TRANSACTION_ID, transaction.id)
                            }
                        )
                    },
                    onOpenAssignmentsPage = {
                        context.startActivity(Intent(context, RiderAssignmentsActivity::class.java))
                    }
                )
            }
            else -> {
                RiderProfileTab(
                    padding = padding,
                    profile = profile,
                    onOpenProfile = {
                        context.startActivity(Intent(context, RiderProfileDetailsActivity::class.java))
                    },
                    onOpenSettings = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    colorScheme = colorScheme
                )
            }
        }
    }
}

// ===================== LOADING STATE =====================

@Composable
fun RiderLoadingState(colorScheme: ColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                "Loading your dashboard...",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===================== HOME TAB =====================

@Composable
private fun RiderHomeTab(
    padding: PaddingValues,
    riderName: String,
    newAssignmentsCount: Int,
    assignments: List<RiderAssignmentItem>,
    onOpenAssignmentDetails: (EscrowResponse) -> Unit,
    colorScheme: ColorScheme
) {
    val activeCount = assignments.count {
        val status = it.transaction.status.uppercase()
        status == "ASSIGNED" || status == "ACCEPTED" || status == "PICKED_UP" ||
                status == "IN_TRANSIT" || status == "ARRIVED_AT_BUYER" ||
                status == "IN_DELIVERY" || status == "SELLER_ACCEPTED"
    }
    val completedCount = assignments.count {
        val status = it.transaction.status.uppercase()
        status == "DELIVERED_TO_BUYER" || status == "COMPLETED" || status == "DELIVERED"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        item {
            WelcomeCard(
                riderName = riderName,
                newAssignmentsCount = newAssignmentsCount,
                colorScheme = colorScheme
            )
        }

        // Stats Row - Improved with better spacing
        item {
            StatsRow(
                newCount = newAssignmentsCount,
                activeCount = activeCount,
                completedCount = completedCount,
                colorScheme = colorScheme
            )
        }

        // Recent Assignments Header
        item {
            SectionHeader(
                title = "Recent Assignments",
                count = assignments.size,
                colorScheme = colorScheme
            )
        }

        val previewItems = assignments.take(5)
        if (previewItems.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "No assignments yet. New delivery assignments will appear here.",
                    colorScheme = colorScheme
                )
            }
        } else {
            items(previewItems) { item ->
                AssignmentCardEnhanced(
                    item = item,
                    onOpenDetails = onOpenAssignmentDetails,
                    colorScheme = colorScheme
                )
            }
        }
    }
}

// ===================== ASSIGNMENTS TAB =====================

@Composable
private fun RiderAssignmentsTab(
    padding: PaddingValues,
    assignments: List<RiderAssignmentItem>,
    colorScheme: ColorScheme,
    onOpenAssignmentDetails: (EscrowResponse) -> Unit,
    onOpenAssignmentsPage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Assignments",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        "${assignments.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (assignments.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "No assigned deliveries found.",
                    colorScheme = colorScheme
                )
            }
        } else {
            items(assignments) { item ->
                AssignmentCardEnhanced(
                    item = item,
                    onOpenDetails = onOpenAssignmentDetails,
                    colorScheme = colorScheme
                )
            }
        }
    }
}

// ===================== IMPROVED COMPONENTS =====================

@Composable
fun WelcomeCard(
    riderName: String,
    newAssignmentsCount: Int,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Welcome back,",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    riderName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    "Ready for your next delivery?",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            if (newAssignmentsCount > 0) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF2563EB),
                                    Color(0xFF3B82F6)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            newAssignmentsCount.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "NEW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(
    newCount: Int,
    activeCount: Int,
    completedCount: Int,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // New
            ImprovedStatItem(
                label = "New",
                value = newCount.toString(),
                icon = Icons.Default.Assignment,
                color = Color(0xFF2563EB),
                colorScheme = colorScheme
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(colorScheme.outlineVariant.copy(alpha = 0.2f))
            )

            // Active
            ImprovedStatItem(
                label = "Active",
                value = activeCount.toString(),
                icon = Icons.Default.LocalShipping,
                color = Color(0xFF10B981),
                colorScheme = colorScheme
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(colorScheme.outlineVariant.copy(alpha = 0.2f))
            )

            // Completed
            ImprovedStatItem(
                label = "Completed",
                value = completedCount.toString(),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF7C3AED),
                colorScheme = colorScheme
            )
        }
    }
}

@Composable
fun RowScope.ImprovedStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    colorScheme: ColorScheme
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .weight(1f)
    ) {
        // Icon with subtle background glow
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            color.copy(alpha = 0.05f)
                        ),
                        radius = 20f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }

        // Value with subtle animation
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            letterSpacing = 0.5.sp
        )

        // Label with proper spacing
        Text(
            label,
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
        if (count > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Text(
                    "$count items",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AssignmentCardEnhanced(
    item: RiderAssignmentItem,
    onOpenDetails: (EscrowResponse) -> Unit,
    colorScheme: ColorScheme
) {
    val status = item.transaction.status
    val statusConfig = getRiderStatusConfig(status)
    val formattedDate = formatRiderDate(item.transaction.updatedAt)
    val title = item.transaction.title
    val address = item.transaction.deliveryAddress
    val buyerName = item.buyerName ?: "Unknown Buyer"
    val isNew = item.transaction.status.equals("ASSIGNED", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails(item.transaction) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNew) 2.dp else 1.dp,
            pressedElevation = 4.dp
        ),
        border = BorderStroke(
            1.dp,
            if (isNew) {
                colorScheme.primary.copy(alpha = 0.2f)
            } else {
                colorScheme.outlineVariant.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Status & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            statusConfig.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusConfig.textColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
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

            // Product + Buyer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Product: $title",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Buyer: $buyerName",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Price + Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KES ${formatAmount(item.transaction.amount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (address.isBlank()) "-" else address,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                message,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===================== HELPER FUNCTIONS =====================

data class RiderStatusConfig(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val backgroundColor: Color
)

fun getRiderStatusConfig(status: String): RiderStatusConfig {
    val normalized = status.uppercase()
    return when {
        normalized.contains("COMPLETED") -> RiderStatusConfig(
            label = "Completed",
            dotColor = Color(0xFF10B981),
            textColor = Color(0xFF10B981),
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.12f)
        )
        normalized.contains("DELIVERED") -> RiderStatusConfig(
            label = "Delivered",
            dotColor = Color(0xFF10B981),
            textColor = Color(0xFF10B981),
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.12f)
        )
        normalized.contains("IN_DELIVERY") || normalized.contains("IN TRANSIT") -> RiderStatusConfig(
            label = "In Transit",
            dotColor = Color(0xFFF59E0B),
            textColor = Color(0xFFF59E0B),
            backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
        )
        normalized.contains("ASSIGNED") || normalized.contains("PENDING") -> RiderStatusConfig(
            label = "Pending",
            dotColor = Color(0xFF3B82F6),
            textColor = Color(0xFF3B82F6),
            backgroundColor = Color(0xFF3B82F6).copy(alpha = 0.12f)
        )
        else -> RiderStatusConfig(
            label = status,
            dotColor = Color(0xFF6B7280),
            textColor = Color(0xFF6B7280),
            backgroundColor = Color(0xFF6B7280).copy(alpha = 0.12f)
        )
    }
}

fun formatRiderDate(dateString: String): String {
    return try {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        val parsedDate = patterns.firstNotNullOfOrNull { pattern ->
            try {
                SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(dateString)
            } catch (_: Exception) {
                null
            }
        }

        if (parsedDate != null) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(parsedDate)
        } else {
            dateString
        }
    } catch (_: Exception) {
        dateString
    }
}

fun formatAmount(amount: Double): String {
    return java.text.NumberFormat.getIntegerInstance(Locale.getDefault()).format(amount)
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderDashboardScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        RiderDashboardScreen()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderDashboardScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        RiderDashboardScreen()
    }
}