package mobile.project.escrowx.dash

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
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TrackFinancesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionRole = SessionManager(this).getUserRole()
        val role = intent.getStringExtra("ROLE") ?: sessionRole ?: "BUYER"
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                TrackFinancesScreen(role = role, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackFinancesScreen(role: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<FinanceEntry>>(emptyList()) }
    var selectedEntry by remember { mutableStateOf<FinanceEntry?>(null) }
    var selectedTab by remember { mutableStateOf(FinanceTab.ALL) }

    val creditEntries = remember(entries) { entries.filter { it.type == FinanceType.CREDIT } }
    val debitEntries = remember(entries) { entries.filter { it.type == FinanceType.DEBIT } }

    val filteredEntries = when (selectedTab) {
        FinanceTab.ALL -> entries
        FinanceTab.CREDITS -> creditEntries
        FinanceTab.DEBITS -> debitEntries
    }

    val totalCredit = remember(creditEntries) { creditEntries.sumOf { it.amount } }
    val totalDebit = remember(debitEntries) { debitEntries.sumOf { it.amount } }
    val balance = totalCredit - totalDebit

    LaunchedEffect(role) {
        scope.launch {
            isLoading = true
            loadError = null

            val token = session.getAccessToken()
            val userId = session.getUserId()

            if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                loadError = "You are not logged in."
                isLoading = false
                return@launch
            }

            try {
                val api = RetrofitClient.authenticated(token)

                val asBuyer = try {
                    val response = api.getTransactionsByBuyer(userId, null)
                    if (response.isSuccessful) response.body().orEmpty() else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                val asSeller = try {
                    val response = api.getTransactionsBySeller(userId, null)
                    if (response.isSuccessful) response.body().orEmpty() else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                val merged = (asBuyer + asSeller).distinctBy { it.id }
                entries = merged
                    .flatMap { txn -> mapToFinanceEntries(txn = txn, currentUserId = userId) }
                    .sortedByDescending { it.createdAtRaw }
            } catch (e: Exception) {
                loadError = "Failed to load finances."
                Toast.makeText(context, e.message ?: "Failed to load finances.", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Finances",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
        ) {
            // ===== SUMMARY CARDS =====
            FinanceSummarySection(
                totalCredit = totalCredit,
                totalDebit = totalDebit,
                balance = balance,
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== TAB FILTERS =====
            FinanceTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                creditCount = creditEntries.size,
                debitCount = debitEntries.size,
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ===== TRANSACTION LIST =====
            when {
                isLoading -> {
                    FinanceLoadingState(colorScheme = colorScheme)
                }
                loadError != null -> {
                    FinanceErrorState(
                        message = loadError!!,
                        onRetry = {
                            // Re-fetch logic would go here
                        },
                        colorScheme = colorScheme
                    )
                }
                filteredEntries.isEmpty() -> {
                    EmptyState(
                        tab = selectedTab,
                        colorScheme = colorScheme
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredEntries, key = { it.entryId }) { entry ->
                            FinanceEntryCardEnhanced(
                                entry = entry,
                                onClick = { selectedEntry = entry },
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
    }

    // ===== DETAIL DIALOG =====
    selectedEntry?.let { entry ->
        FinanceDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            colorScheme = colorScheme
        )
    }
}

// ===================== SUMMARY SECTION =====================

@Composable
private fun FinanceSummarySection(
    totalCredit: Double,
    totalDebit: Double,
    balance: Double,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            // Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Balance",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        formatCurrency(balance),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) Color(0xFF10B981) else Color(0xFFDC2626)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (balance >= 0)
                                Color(0xFF10B981).copy(alpha = 0.12f)
                            else
                                Color(0xFFDC2626).copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (balance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (balance >= 0) Color(0xFF10B981) else Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Credit & Debit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Credit",
                    amount = totalCredit,
                    icon = Icons.Default.ArrowDownward,
                    color = Color(0xFF10B981),
                    colorScheme = colorScheme
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Debit",
                    amount = totalDebit,
                    icon = Icons.Default.ArrowUpward,
                    color = Color(0xFFDC2626),
                    colorScheme = colorScheme
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    colorScheme: ColorScheme
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column {
                Text(
                    title,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatCurrency(amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

// ===================== TAB ROW =====================

@Composable
private fun FinanceTabRow(
    selectedTab: FinanceTab,
    onTabSelected: (FinanceTab) -> Unit,
    creditCount: Int,
    debitCount: Int,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FinanceTabChip(
            modifier = Modifier.weight(1f),
            text = "All",
            icon = Icons.Default.List,
            count = creditCount + debitCount,
            selected = selectedTab == FinanceTab.ALL,
            onClick = { onTabSelected(FinanceTab.ALL) },
            colorScheme = colorScheme
        )
        FinanceTabChip(
            modifier = Modifier.weight(1f),
            text = "Credits",
            icon = Icons.Default.ArrowDownward,
            count = creditCount,
            selected = selectedTab == FinanceTab.CREDITS,
            onClick = { onTabSelected(FinanceTab.CREDITS) },
            colorScheme = colorScheme
        )
        FinanceTabChip(
            modifier = Modifier.weight(1f),
            text = "Debits",
            icon = Icons.Default.ArrowUpward,
            count = debitCount,
            selected = selectedTab == FinanceTab.DEBITS,
            onClick = { onTabSelected(FinanceTab.DEBITS) },
            colorScheme = colorScheme
        )
    }
}

@Composable
private fun FinanceTabChip(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(50),
        color = if (selected) colorScheme.primary else colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected)
                        colorScheme.onPrimary.copy(alpha = 0.2f)
                    else
                        colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                ) {
                    Text(
                        count.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ===================== STATE COMPOSABLES =====================

@Composable
private fun FinanceLoadingState(colorScheme: ColorScheme) {
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
                "Loading your finances...",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FinanceErrorState(
    message: String,
    onRetry: () -> Unit,
    colorScheme: ColorScheme
) {
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
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Text(
                message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyState(
    tab: FinanceTab,
    colorScheme: ColorScheme
) {
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
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (tab) {
                        FinanceTab.ALL -> Icons.Default.ReceiptLong
                        FinanceTab.CREDITS -> Icons.Default.ArrowDownward
                        FinanceTab.DEBITS -> Icons.Default.ArrowUpward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colorScheme.primary
                )
            }
            Text(
                when (tab) {
                    FinanceTab.ALL -> "No transactions found"
                    FinanceTab.CREDITS -> "No credits found"
                    FinanceTab.DEBITS -> "No debits found"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                when (tab) {
                    FinanceTab.ALL -> "Your financial activity will appear here"
                    FinanceTab.CREDITS -> "You haven't received any credits yet"
                    FinanceTab.DEBITS -> "You haven't made any payments yet"
                },
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===================== ENHANCED ENTRY CARD =====================

@Composable
private fun FinanceEntryCardEnhanced(
    entry: FinanceEntry,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    val accentColor = if (entry.type == FinanceType.CREDIT) Color(0xFF10B981) else Color(0xFFDC2626)
    val accentBg = if (entry.type == FinanceType.CREDIT)
        Color(0xFF10B981).copy(alpha = 0.08f)
    else
        Color(0xFFDC2626).copy(alpha = 0.08f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 3.dp
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (entry.type == FinanceType.CREDIT)
                        Icons.Default.ArrowDownward
                    else
                        Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    entry.message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            entry.statusLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        "•",
                        fontSize = 8.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        entry.createdAtDisplay,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                if (entry.title.isNotBlank()) {
                    Text(
                        entry.title,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    formatCurrency(entry.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    "View →",
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ===================== DETAIL DIALOG =====================

@Composable
private fun FinanceDetailDialog(
    entry: FinanceEntry,
    onDismiss: () -> Unit,
    colorScheme: ColorScheme
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                        .background(
                            if (entry.type == FinanceType.CREDIT)
                                Color(0xFF10B981).copy(alpha = 0.12f)
                            else
                                Color(0xFFDC2626).copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (entry.type == FinanceType.CREDIT)
                            Icons.Default.ArrowDownward
                        else
                            Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (entry.type == FinanceType.CREDIT)
                            Color(0xFF10B981)
                        else
                            Color(0xFFDC2626)
                    )
                }
                Column {
                    Text(
                        if (entry.type == FinanceType.CREDIT) "Credit Details" else "Debit Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        entry.statusLabel,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (entry.type == FinanceType.CREDIT)
                        Color(0xFF10B981).copy(alpha = 0.06f)
                    else
                        Color(0xFFDC2626).copy(alpha = 0.06f),
                    border = BorderStroke(1.dp,
                        if (entry.type == FinanceType.CREDIT)
                            Color(0xFF10B981).copy(alpha = 0.15f)
                        else
                            Color(0xFFDC2626).copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Amount",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatCurrency(entry.amount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.type == FinanceType.CREDIT)
                                Color(0xFF10B981)
                            else
                                Color(0xFFDC2626)
                        )
                    }
                }

                DetailRowEnhanced(
                    icon = Icons.Default.ReceiptLong,
                    label = "Transaction ID",
                    value = entry.transactionId,
                    colorScheme = colorScheme
                )

                entry.reference?.let { ref ->
                    DetailRowEnhanced(
                        icon = Icons.Default.Info,
                        label = "Reference",
                        value = ref,
                        colorScheme = colorScheme
                    )
                }

                if (entry.title.isNotBlank()) {
                    DetailRowEnhanced(
                        icon = Icons.Default.ShoppingBag,
                        label = "Title",
                        value = entry.title,
                        colorScheme = colorScheme
                    )
                }

                if (entry.description.isNotBlank()) {
                    DetailRowEnhanced(
                        icon = Icons.Default.Description,
                        label = "Description",
                        value = entry.description,
                        colorScheme = colorScheme
                    )
                }

                DetailRowEnhanced(
                    icon = Icons.Default.Person,
                    label = "Counterparty",
                    value = entry.counterpartyId,
                    colorScheme = colorScheme
                )

                DetailRowEnhanced(
                    icon = Icons.Default.CalendarToday,
                    label = "Created",
                    value = entry.createdAtDisplay,
                    colorScheme = colorScheme
                )

                DetailRowEnhanced(
                    icon = Icons.Default.Update,
                    label = "Updated",
                    value = entry.updatedAtDisplay,
                    colorScheme = colorScheme
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary
                )
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
private fun DetailRowEnhanced(
    icon: ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                letterSpacing = 0.3.sp
            )
            Text(
                value,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ===================== ENUM AND DATA CLASS =====================

private enum class FinanceTab { ALL, CREDITS, DEBITS }
private enum class FinanceType { CREDIT, DEBIT }

private data class FinanceEntry(
    val entryId: String,
    val type: FinanceType,
    val transactionId: String,
    val reference: String?,
    val title: String,
    val description: String,
    val amount: Double,
    val statusLabel: String,
    val message: String,
    val counterpartyId: String,
    val createdAtRaw: String,
    val createdAtDisplay: String,
    val updatedAtDisplay: String
)

// ===================== HELPER FUNCTIONS =====================

private fun mapToFinanceEntries(txn: EscrowResponse, currentUserId: String): List<FinanceEntry> {
    val entries = mutableListOf<FinanceEntry>()

    if (txn.sellerId == currentUserId) {
        entries += FinanceEntry(
            entryId = "${txn.id}:credit",
            type = FinanceType.CREDIT,
            transactionId = txn.id,
            reference = txn.reference,
            title = txn.title,
            description = txn.productDescription,
            amount = txn.amount,
            statusLabel = txn.status,
            message = "Received from buyer ${shortId(txn.buyerId)}",
            counterpartyId = txn.buyerId,
            createdAtRaw = txn.createdAt,
            createdAtDisplay = formatDate(txn.createdAt),
            updatedAtDisplay = formatDate(txn.updatedAt)
        )
    }

    if (txn.buyerId == currentUserId) {
        entries += FinanceEntry(
            entryId = "${txn.id}:debit",
            type = FinanceType.DEBIT,
            transactionId = txn.id,
            reference = txn.reference,
            title = txn.title,
            description = txn.productDescription,
            amount = txn.amount,
            statusLabel = txn.status,
            message = "Paid to seller ${shortId(txn.sellerId)}",
            counterpartyId = txn.sellerId,
            createdAtRaw = txn.createdAt,
            createdAtDisplay = formatDate(txn.createdAt),
            updatedAtDisplay = formatDate(txn.updatedAt)
        )
    }

    return entries
}

private fun shortId(id: String): String = if (id.length <= 8) id else id.take(8)

private fun formatCurrency(value: Double): String {
    val formatted = NumberFormat.getNumberInstance(Locale.US).format(value)
    return "KES $formatted"
}

private fun formatDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    return try {
        val parts = raw.split("T")
        val dateParts = parts[0].split("-")
        val month = when (dateParts[1].toInt()) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
            7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> dateParts[1]
        }
        "$month ${dateParts[2]}, ${dateParts[0]}"
    } catch (_: Exception) {
        raw.take(10)
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun TrackFinancesScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        TrackFinancesScreen(role = "BUYER", onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun TrackFinancesScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        TrackFinancesScreen(role = "BUYER", onBack = {})
    }
}