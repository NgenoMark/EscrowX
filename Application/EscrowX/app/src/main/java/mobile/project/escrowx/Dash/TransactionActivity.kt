package mobile.project.escrowx.dash

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.seller.SellerTransactionDetailActivity
import mobile.project.escrowx.ui.components.*
import mobile.project.escrowx.ui.theme.BrandBlue
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.text.NumberFormat
import java.util.*

class TransactionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionRole = SessionManager(this).getUserRole()
        val role = intent.getStringExtra("ROLE") ?: sessionRole ?: "BUYER"
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                TransactionsScreen(role = role)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(role: String) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val currentUserId = session.getUserId().orEmpty()

    var allTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var filteredTransactions by remember { mutableStateOf<List<EscrowResponse>>(emptyList()) }
    var sellerNamesById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatusFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedPartyFilter by remember {
        mutableStateOf(
            when (role.uppercase()) {
                "SELLER" -> TransactionPartyFilter.SELLER
                "BUYER" -> TransactionPartyFilter.BUYER
                else -> TransactionPartyFilter.ALL
            }
        )
    }
    var selectedBottomTab by remember { mutableIntStateOf(1) }

    // Helper to create dummy transactions
    fun createDummyTransaction(
        id: String,
        reference: String,
        buyerId: String,
        sellerId: String,
        title: String,
        amount: String,
        status: String,
        createdAt: String
    ): EscrowResponse {
        return EscrowResponse(
            id = id,
            reference = reference,
            buyerId = buyerId,
            sellerId = sellerId,
            title = title,
            productDescription = "Sample product description",
            amount = amount.toDoubleOrNull() ?: 0.0,
            deliveryAddress = "Nairobi, Kenya",
            initialDepositAmount = null,
            currency = "KES",
            status = status,
            deliveryDueAt = "2024-12-31T23:59:59Z",
            autoReleaseAt = null,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    fun getDummyBuyerTransactions(): List<EscrowResponse> {
        return listOf(
            createDummyTransaction("1", "ESC-BUY-001", "buyer1", "seller1", "iPhone 15 Pro Max", "165000", "IN_DELIVERY", "2024-06-05T10:30:00"),
            createDummyTransaction("2", "ESC-BUY-002", "buyer1", "seller2", "MacBook Air M2", "125000", "COMPLETED", "2024-05-20T09:00:00"),
            createDummyTransaction("3", "ESC-BUY-003", "buyer1", "seller3", "Sony WH-1000XM5", "42500", "FUNDS_HELD", "2024-06-08T08:15:00"),
            createDummyTransaction("4", "ESC-BUY-004", "buyer1", "seller4", "Samsung 4K Monitor", "75000", "DELIVERED", "2024-06-01T11:00:00")
        )
    }

    fun getDummySellerTransactions(): List<EscrowResponse> {
        return listOf(
            createDummyTransaction("101", "ESC-SELL-001", "buyerA", "seller1", "Sold: iPhone 15 Pro Max", "165000", "IN_DELIVERY", "2024-06-05T10:30:00"),
            createDummyTransaction("102", "ESC-SELL-002", "buyerB", "seller1", "Sold: MacBook Air M2", "125000", "COMPLETED", "2024-05-20T09:00:00"),
            createDummyTransaction("103", "ESC-SELL-003", "buyerC", "seller1", "Sold: Sony Headphones", "42500", "FUNDS_HELD", "2024-06-08T08:15:00"),
            createDummyTransaction("104", "ESC-SELL-004", "buyerD", "seller1", "Sold: Samsung Monitor", "75000", "DELIVERED", "2024-06-01T11:00:00")
        )
    }

    fun parseTransactionsFromBody(body: Any?): List<EscrowResponse> {
        return when (body) {
            is List<*> -> body.filterIsInstance<EscrowResponse>()
            else -> {
                try {
                    val json = Gson().toJson(body)
                    val wrapper = Gson().fromJson(json, PageResponseWrapper::class.java)
                    wrapper.content ?: emptyList()
                } catch (e: Exception) {
                    println("Failed to parse wrapper: ${e.message}")
                    emptyList()
                }
            }
        }
    }

    fun toEpochMillis(dateTime: String?): Long {
        if (dateTime.isNullOrBlank()) return Long.MIN_VALUE

        return runCatching { Instant.parse(dateTime).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(dateTime).toInstant().toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(dateTime).toInstant(ZoneOffset.UTC).toEpochMilli() }
            .getOrElse { Long.MIN_VALUE }
    }

    LaunchedEffect(role) {
        scope.launch {
            val token = session.getAccessToken()
            val userId = session.getUserId()
            if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                allTransactions = (getDummyBuyerTransactions() + getDummySellerTransactions())
                    .distinctBy { it.id }
                isLoading = false
                return@launch
            }

            try {
                val api = RetrofitClient.authenticated(token)

                val buyerTransactions = try {
                    val buyerResponse = api.getTransactionsByBuyer(userId, null)
                    if (buyerResponse.isSuccessful) parseTransactionsFromBody(buyerResponse.body()) else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                val sellerTransactions = try {
                    val sellerResponse = api.getTransactionsBySeller(userId, null)
                    if (sellerResponse.isSuccessful) parseTransactionsFromBody(sellerResponse.body()) else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                val mergedTransactions = (buyerTransactions + sellerTransactions)
                    .distinctBy { it.id }
                    .sortedByDescending { toEpochMillis(it.createdAt) }
                allTransactions = if (mergedTransactions.isNotEmpty()) {
                    mergedTransactions
                } else {
                    (getDummyBuyerTransactions() + getDummySellerTransactions()).distinctBy { it.id }
                        .sortedByDescending { toEpochMillis(it.createdAt) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                allTransactions = (getDummyBuyerTransactions() + getDummySellerTransactions())
                    .distinctBy { it.id }
                    .sortedByDescending { toEpochMillis(it.createdAt) }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(allTransactions, selectedStatusFilter, selectedPartyFilter, currentUserId) {
        val partyScoped = when (selectedPartyFilter) {
            TransactionPartyFilter.ALL -> allTransactions
            TransactionPartyFilter.BUYER -> allTransactions.filter { it.buyerId == currentUserId }
            TransactionPartyFilter.SELLER -> allTransactions.filter { it.sellerId == currentUserId }
        }

        filteredTransactions = when (selectedStatusFilter) {
            TransactionFilter.ALL -> partyScoped
            TransactionFilter.COMPLETE -> partyScoped.filter { isTerminalEscrowState(it.status) }
            TransactionFilter.INCOMPLETE -> partyScoped.filter { !isTerminalEscrowState(it.status) }
        }.sortedByDescending { toEpochMillis(it.createdAt) }
    }

    LaunchedEffect(allTransactions) {
        val token = session.getAccessToken()
        if (token.isNullOrBlank() || allTransactions.isEmpty()) {
            sellerNamesById = emptyMap()
            return@LaunchedEffect
        }

        val api = RetrofitClient.authenticated(token)
        val uniqueSellerIds = allTransactions.map { it.sellerId }.distinct()
        val fetchedNames = mutableMapOf<String, String>()

        uniqueSellerIds.forEach { sellerId ->
            val fallback = "Seller ${sellerId.take(6)}"
            val displayName = try {
                val response = api.getUserById(sellerId)
                if (response.isSuccessful) {
                    response.body()?.displayName?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
            fetchedNames[sellerId] = displayName ?: fallback
        }

        sellerNamesById = fetchedNames
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transactions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (role == "BUYER") context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                        else context.startActivity(Intent(context, SellerDashboardActivity::class.java))
                        (context as? TransactionsActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (role.equals("SELLER", ignoreCase = true)) {
                SellerNavBar(
                    selectedIndex = selectedBottomTab,
                    onItemSelected = { item ->
                        selectedBottomTab = item.index
                        when (item) {
                            SellerNavItem.Home -> navigateTab(context, SellerDashboardActivity::class.java)
                            SellerNavItem.Transactions -> Unit
                            SellerNavItem.Profile -> navigateTab(context, ProfileActivity::class.java)
                        }
                    }
                )
            } else {
                BuyerNavBar(
                    selectedIndex = selectedBottomTab,
                    onItemSelected = { item ->
                        selectedBottomTab = item.index
                        when (item) {
                            BuyerNavItem.Home -> navigateTab(context, BuyerDashboardActivity::class.java)
                            BuyerNavItem.Transactions -> Unit
                            BuyerNavItem.Profile -> navigateTab(context, ProfileActivity::class.java)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
        ) {
            // Filters Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransactionFilters(
                    selectedStatusFilter = selectedStatusFilter,
                    onStatusFilterChange = { selectedStatusFilter = it },
                    selectedPartyFilter = selectedPartyFilter,
                    onPartyFilterChange = { selectedPartyFilter = it },
                    onCreateEscrow = {
                        context.startActivity(Intent(context, CreateEscrowActivity::class.java))
                    },
                    colorScheme = colorScheme
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Transaction Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filteredTransactions.size} transaction${if (filteredTransactions.size != 1) "s" else ""} found",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (filteredTransactions.isNotEmpty()) {
                    Text(
                        "Showing ${filteredTransactions.size} of ${allTransactions.size}",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                isLoading -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
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
                                "Loading transactions...",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                filteredTransactions.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = colorScheme.primary
                                )
                            }
                            Text(
                                "No Transactions Found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                "Create your first escrow transaction to get started",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Button(
                                onClick = {
                                    context.startActivity(Intent(context, CreateEscrowActivity::class.java))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Create Escrow",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTransactions) { transaction ->
                            val transactionRole = when {
                                transaction.sellerId == currentUserId -> "SELLER"
                                transaction.buyerId == currentUserId -> "BUYER"
                                else -> role
                            }
                            val sellerName = sellerNamesById[transaction.sellerId] ?: "Seller ${transaction.sellerId.take(6)}"
                            ModernTransactionCard(
                                transaction = transaction,
                                sellerName = sellerName,
                                role = transactionRole,
                                onViewDetails = {
                                    if (transactionRole == "BUYER") {
                                        val intent = Intent(context, BuyerTransactionDetailActivity::class.java).apply {
                                            putExtra("TRANSACTION_ID", transaction.id)
                                            putExtra("PRODUCT_NAME", transaction.title)
                                            putExtra("PRODUCT_DESCRIPTION", transaction.productDescription)
                                            putExtra("SELLER_NAME", sellerName)
                                            putExtra("AMOUNT", transaction.amount.toString())
                                            putExtra("ORDER_ID", transaction.reference)
                                            putExtra("DATE", transaction.createdAt.take(10))
                                            putExtra(
                                                "DELIVERY_DATE",
                                                transaction.deliveryDueAt.takeIf { it.isNotBlank() }?.take(10)
                                                    ?: transaction.createdAt.take(10)
                                            )
                                            putExtra("SHIPPING_ADDRESS", transaction.deliveryAddress)
                                            putExtra("STATUS", transaction.status)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        val currentStep = sellerStepForStatus(transaction.status)
                                        val intent = Intent(context, SellerTransactionDetailActivity::class.java).apply {
                                            putExtra("TRANSACTION_ID", transaction.id)
                                            putExtra("PRODUCT_NAME", transaction.title)
                                            putExtra("BUYER_NAME", "Buyer")
                                            putExtra("BUYER_INITIALS", "BY")
                                            putExtra("AMOUNT", transaction.amount.toString())
                                            putExtra("ORDER_ID", transaction.reference)
                                            putExtra("DATE", transaction.createdAt.take(10))
                                            putExtra("SHIPPING_ADDRESS", transaction.deliveryAddress)
                                            putExtra("STATUS", transaction.status)
                                            putExtra("CURRENT_STEP", currentStep)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionFilterChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colorScheme.primary else colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else colorScheme.outlineVariant
        ),
        modifier = Modifier.wrapContentSize(),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TransactionFilters(
    selectedStatusFilter: TransactionFilter,
    onStatusFilterChange: (TransactionFilter) -> Unit,
    selectedPartyFilter: TransactionPartyFilter,
    onPartyFilterChange: (TransactionPartyFilter) -> Unit,
    onCreateEscrow: () -> Unit,
    colorScheme: ColorScheme
) {
    var isExpanded by remember { mutableStateOf(false) }
    val activeFiltersCount =
        (if (selectedStatusFilter != TransactionFilter.ALL) 1 else 0) +
        (if (selectedPartyFilter != TransactionPartyFilter.ALL) 1 else 0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
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
            // Header Row: Filters + Create Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Filter Label with expand/collapse
                Row(
                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.primary
                    )
                    Text(
                        text = "Filters",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    // Active filter count badge
                    if (activeFiltersCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary
                        ) {
                            Text(
                                text = activeFiltersCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Right: Create Escrow Button + Expand/Collapse
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create Escrow Button - Compact version
                    Button(
                        onClick = onCreateEscrow,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
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
                            Icons.Default.Add,
                            contentDescription = "Create Escrow",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Create",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Expand/Collapse icon
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse filters" else "Expand filters",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { isExpanded = !isExpanded }
                    )
                }
            }

            // ===== EXPANDABLE FILTER CONTENT =====
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )

                    // ===== STATUS FILTER GROUP =====
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            if (selectedStatusFilter != TransactionFilter.ALL) {
                                Text(
                                    text = selectedStatusFilter.name.lowercase(Locale.getDefault()),
                                    fontSize = 10.sp,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChipCompact(
                                text = "All",
                                icon = Icons.Default.List,
                                selected = selectedStatusFilter == TransactionFilter.ALL,
                                onClick = { onStatusFilterChange(TransactionFilter.ALL) },
                                colorScheme = colorScheme
                            )
                            FilterChipCompact(
                                text = "Complete",
                                icon = Icons.Default.CheckCircle,
                                selected = selectedStatusFilter == TransactionFilter.COMPLETE,
                                onClick = { onStatusFilterChange(TransactionFilter.COMPLETE) },
                                colorScheme = colorScheme
                            )
                            FilterChipCompact(
                                text = "Incomplete",
                                icon = Icons.Default.Pending,
                                selected = selectedStatusFilter == TransactionFilter.INCOMPLETE,
                                onClick = { onStatusFilterChange(TransactionFilter.INCOMPLETE) },
                                colorScheme = colorScheme
                            )
                        }
                    }

                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )

                    // ===== ROLE FILTER GROUP =====
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ROLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            if (selectedPartyFilter != TransactionPartyFilter.ALL) {
                                Text(
                                    text = selectedPartyFilter.name.lowercase(Locale.getDefault()),
                                    fontSize = 10.sp,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChipCompact(
                                text = "All",
                                icon = Icons.Default.People,
                                selected = selectedPartyFilter == TransactionPartyFilter.ALL,
                                onClick = { onPartyFilterChange(TransactionPartyFilter.ALL) },
                                colorScheme = colorScheme
                            )
                            FilterChipCompact(
                                text = "Buyer",
                                icon = Icons.Default.ShoppingCart,
                                selected = selectedPartyFilter == TransactionPartyFilter.BUYER,
                                onClick = { onPartyFilterChange(TransactionPartyFilter.BUYER) },
                                colorScheme = colorScheme
                            )
                            FilterChipCompact(
                                text = "Seller",
                                icon = Icons.Default.Storefront,
                                selected = selectedPartyFilter == TransactionPartyFilter.SELLER,
                                onClick = { onPartyFilterChange(TransactionPartyFilter.SELLER) },
                                colorScheme = colorScheme
                            )
                        }
                    }

                    // ===== CLEAR FILTERS BUTTON =====
                    if (activeFiltersCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    onStatusFilterChange(TransactionFilter.ALL)
                                    onPartyFilterChange(TransactionPartyFilter.ALL)
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear filters",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Clear All Filters",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
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
fun <T> FilterGroup(
    title: String,
    options: List<FilterOption<T>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChipCompact(
                    text = option.label,
                    icon = option.icon,
                    selected = option.value == selectedOption,
                    onClick = { onOptionSelected(option.value) },
                    colorScheme = colorScheme
                )
            }
        }
    }
}

@Composable
fun FilterChipCompact(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colorScheme.primary else colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.wrapContentSize(),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface
            )
        }
    }
}

data class FilterOption<T>(
    val label: String,
    val icon: ImageVector,
    val value: T
)

fun getActiveFilterSummary(
    statusFilter: TransactionFilter,
    partyFilter: TransactionPartyFilter
): String {
    val parts = mutableListOf<String>()
    if (statusFilter != TransactionFilter.ALL) {
        parts.add(statusFilter.name.lowercase(Locale.getDefault()))
    }
    if (partyFilter != TransactionPartyFilter.ALL) {
        parts.add(partyFilter.name.lowercase(Locale.getDefault()))
    }
    return parts.joinToString(" • ")
}

@Composable
fun ModernTransactionCard(
    transaction: EscrowResponse,
    sellerName: String,
    role: String,
    onViewDetails: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val statusConfig = getStatusConfig(transaction.status, colorScheme)
    val formattedDate = formatDate(transaction.createdAt)
    val formattedAmount = NumberFormat.getIntegerInstance(Locale.getDefault())
        .format(transaction.amount.toInt())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onViewDetails() },
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
            // Header: Status & Reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusConfig.backgroundColor,
                    modifier = Modifier
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(statusConfig.dotColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusConfig.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusConfig.textColor,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                // Reference
                Text(
                    text = "Ref: ${transaction.reference?.take(8) ?: transaction.id.take(8)}",
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // Product Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Product Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sellerName,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Divider
            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            // Transaction Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransactionDetailChip(
                    icon = Icons.Default.Money,
                    label = "Amount",
                    value = "KES $formattedAmount",
                    colorScheme = colorScheme
                )
                TransactionDetailChip(
                    icon = Icons.Default.CalendarToday,
                    label = "Date",
                    value = formattedDate,
                    colorScheme = colorScheme
                )
                TransactionDetailChip(
                    icon = if (role == "BUYER") Icons.Default.ShoppingCart else Icons.Default.Storefront,
                    label = "Role",
                    value = role,
                    colorScheme = colorScheme
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDetailChip(
    icon: ImageVector,
    label: String,
    value: String,
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

data class StatusConfig(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val backgroundColor: Color
)

fun getStatusConfig(status: String, colorScheme: ColorScheme): StatusConfig {
    return when (normalizeEscrowStatus(status)) {
        "COMPLETED" -> StatusConfig(
            label = "Completed",
            dotColor = Color(0xFF10B981),
            textColor = Color(0xFF10B981),
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.12f)
        )
        "IN_DELIVERY" -> StatusConfig(
            label = "In Delivery",
            dotColor = Color(0xFFF59E0B),
            textColor = Color(0xFFF59E0B),
            backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
        )
        "FUNDS_HELD" -> StatusConfig(
            label = "Funds Held",
            dotColor = Color(0xFF3B82F6),
            textColor = Color(0xFF3B82F6),
            backgroundColor = Color(0xFF3B82F6).copy(alpha = 0.12f)
        )
        "CREATED" -> StatusConfig(
            label = "Created",
            dotColor = Color(0xFF6B7280),
            textColor = Color(0xFF6B7280),
            backgroundColor = Color(0xFF6B7280).copy(alpha = 0.12f)
        )
        "CANCELLED" -> StatusConfig(
            label = "Cancelled",
            dotColor = Color(0xFF6B7280),
            textColor = Color(0xFF6B7280),
            backgroundColor = Color(0xFF6B7280).copy(alpha = 0.12f)
        )
        "DECLINED" -> StatusConfig(
            label = "Declined",
            dotColor = Color(0xFF6B7280),
            textColor = Color(0xFF6B7280),
            backgroundColor = Color(0xFF6B7280).copy(alpha = 0.12f)
        )
        "EXPIRED" -> StatusConfig(
            label = "Expired",
            dotColor = Color(0xFF6B7280),
            textColor = Color(0xFF6B7280),
            backgroundColor = Color(0xFF6B7280).copy(alpha = 0.12f)
        )
        "REFUNDED" -> StatusConfig(
            label = "Refunded",
            dotColor = Color(0xFF006C49),
            textColor = Color(0xFF006C49),
            backgroundColor = Color(0xFF006C49).copy(alpha = 0.12f)
        )
        "DELIVERED" -> StatusConfig(
            label = "Delivered",
            dotColor = Color(0xFF8B5CF6),
            textColor = Color(0xFF8B5CF6),
            backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)
        )
        "PENDING_PAYMENT" -> StatusConfig(
            label = "Pending Payment",
            dotColor = Color(0xFFF59E0B),
            textColor = Color(0xFFF59E0B),
            backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
        )
        "RELEASE_PENDING" -> StatusConfig(
            label = "Release Pending",
            dotColor = Color(0xFF7C3AED),
            textColor = Color(0xFF7C3AED),
            backgroundColor = Color(0xFF7C3AED).copy(alpha = 0.12f)
        )
        "RELEASE_PROCESSING" -> StatusConfig(
            label = "Release Processing",
            dotColor = Color(0xFF7C3AED),
            textColor = Color(0xFF7C3AED),
            backgroundColor = Color(0xFF7C3AED).copy(alpha = 0.12f)
        )
        "RELEASE_FAILED" -> StatusConfig(
            label = "Release Failed",
            dotColor = Color(0xFFBA1A1A),
            textColor = Color(0xFFBA1A1A),
            backgroundColor = Color(0xFFBA1A1A).copy(alpha = 0.12f)
        )
        "DISPUTED" -> StatusConfig(
            label = "Disputed",
            dotColor = Color(0xFFBA1A1A),
            textColor = Color(0xFFBA1A1A),
            backgroundColor = Color(0xFFBA1A1A).copy(alpha = 0.12f)
        )
        "REFUND_PENDING" -> StatusConfig(
            label = "Refund Pending",
            dotColor = Color(0xFFBA1A1A),
            textColor = Color(0xFFBA1A1A),
            backgroundColor = Color(0xFFBA1A1A).copy(alpha = 0.12f)
        )
        "REFUND_PROCESSING" -> StatusConfig(
            label = "Refund Processing",
            dotColor = Color(0xFFBA1A1A),
            textColor = Color(0xFFBA1A1A),
            backgroundColor = Color(0xFFBA1A1A).copy(alpha = 0.12f)
        )
        else -> StatusConfig(
            label = status,
            dotColor = colorScheme.onSurfaceVariant,
            textColor = colorScheme.onSurfaceVariant,
            backgroundColor = colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
        )
    }
}

fun formatDate(dateString: String): String {
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

enum class TransactionFilter { ALL, COMPLETE, INCOMPLETE }
enum class TransactionPartyFilter { ALL, BUYER, SELLER }

data class PageResponseWrapper(val content: List<EscrowResponse>?)

private val ESCROW_STATE_TRANSITIONS: Map<String, List<String>> = mapOf(
    "CREATED" to listOf("DECLINED", "PENDING_PAYMENT", "CANCELLED", "EXPIRED"),
    "PENDING_PAYMENT" to listOf("FUNDS_HELD", "CANCELLED"),
    "FUNDS_HELD" to listOf("IN_DELIVERY", "DISPUTED"),
    "IN_DELIVERY" to listOf("DELIVERED", "DISPUTED"),
    "DELIVERED" to listOf("RELEASE_PENDING", "DISPUTED"),
    "RELEASE_PENDING" to listOf("RELEASE_PROCESSING", "DISPUTED"),
    "RELEASE_PROCESSING" to listOf("RELEASE_FAILED", "COMPLETED"),
    "RELEASE_FAILED" to listOf("RELEASE_PROCESSING", "DISPUTED"),
    "DISPUTED" to listOf("REFUND_PENDING", "RELEASE_PENDING"),
    "REFUND_PENDING" to listOf("REFUND_PROCESSING"),
    "REFUND_PROCESSING" to listOf("REFUNDED")
)

private val TERMINAL_ESCROW_STATES: Set<String> = setOf(
    "COMPLETED",
    "DECLINED",
    "CANCELLED",
    "REFUNDED",
    "EXPIRED"
)

private fun normalizeEscrowStatus(status: String?): String = status?.trim().orEmpty().uppercase(Locale.getDefault())

private fun isTerminalEscrowState(status: String?): Boolean = normalizeEscrowStatus(status) in TERMINAL_ESCROW_STATES

private fun hasAcceptedTransitionPath(status: String?): Boolean =
    normalizeEscrowStatus(status) in ESCROW_STATE_TRANSITIONS.keys

private fun sellerStepForStatus(status: String?): Int {
    return when (normalizeEscrowStatus(status)) {
        "CREATED", "PENDING_PAYMENT", "FUNDS_HELD" -> 1
        "IN_DELIVERY" -> 2
        "DELIVERED", "RELEASE_PENDING", "RELEASE_PROCESSING", "RELEASE_FAILED" -> 3
        in TERMINAL_ESCROW_STATES -> 4
        else -> 1
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun TransactionsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        TransactionsScreen(role = "BUYER")
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun TransactionsScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        TransactionsScreen(role = "BUYER")
    }
}