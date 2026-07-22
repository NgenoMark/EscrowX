package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

data class RiderAssignmentRecord(
    val transaction: EscrowResponse,
    val buyerName: String? = null
)

class RiderAssignmentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.rememberDarkModeEnabledState(),
                dynamicColor = false
            ) {
                RiderAssignmentsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderAssignmentsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var assignments by remember { mutableStateOf<List<RiderAssignmentRecord>>(emptyList()) }

    fun loadAssignments() {
        val token = session.getAccessToken()
        val actorId = session.getUserId()
        if (token.isNullOrBlank() || actorId.isNullOrBlank()) {
            isLoading = false
            return
        }

        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.authenticated(token)
                val response = api.getTransactionsByRider(actorId)
                val riderTransactions = if (response.isSuccessful) {
                    response.body().orEmpty()
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

                assignments = riderTransactions
                    .sortedByDescending { it.updatedAt }
                    .map { txn -> RiderAssignmentRecord(transaction = txn, buyerName = buyerNameMap[txn.buyerId]) }
            } catch (_: Exception) {
                assignments = emptyList()
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAssignments()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rider Assignments",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        fontSize = 18.sp
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
                actions = {
                    IconButton(onClick = { loadAssignments() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = colorScheme.primary)
                }
            }
            assignments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "No assigned deliveries found.",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(colorScheme.background),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignments) { item ->
                        AssignmentRowCompact(
                            item = item,
                            onOpenDetails = { transaction ->
                                context.startActivity(
                                    Intent(context, RiderAssignmentDetailsActivity::class.java).apply {
                                        putExtra(RiderAssignmentDetailsActivity.EXTRA_TRANSACTION_ID, transaction.id)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ===================== COMPACT ASSIGNMENT ROW =====================

@Composable
private fun AssignmentRowCompact(
    item: RiderAssignmentRecord,
    onOpenDetails: (EscrowResponse) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val status = item.transaction.status
    val compactDate = formatCompactDate(item.transaction.updatedAt)
    val isNew = status.equals("ASSIGNED", ignoreCase = true)
    val buyerName = item.buyerName ?: "Unknown Buyer"
    val priceText = "KES ${formatAmountCompact(item.transaction.amount)}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails(item.transaction) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNew) 2.dp else 0.5.dp
        ),
        border = BorderStroke(
            width = if (isNew) 1.5.dp else 0.dp,
            color = if (isNew) colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Assignment Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Center: Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Row 1: Status + Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            status.take(8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = compactDate,
                            fontSize = 9.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Row 2: Product + Buyer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Product: ${item.transaction.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Buyer: $buyerName",
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Price + Address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = priceText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = item.transaction.deliveryAddress,
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right: Chevron
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ===================== ORIGINAL VERSION (Keeping for reference) =====================

@Composable
private fun AssignmentRow(
    item: RiderAssignmentRecord,
    onOpenDetails: (EscrowResponse) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val status = item.transaction.status
    val compactDate = formatCompactDate(item.transaction.updatedAt)
    val isNew = status.equals("ASSIGNED", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails(item.transaction) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNew) 2.dp else 0.5.dp
        ),
        border = BorderStroke(
            width = if (isNew) 1.5.dp else 0.dp,
            color = if (isNew) colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.transaction.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        status.take(8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Row 2: Address with icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = item.transaction.deliveryAddress,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Date + View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = compactDate,
                        fontSize = 9.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "View â†’",
                    fontSize = 10.sp,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // NEW Badge
            if (isNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFDC2626)
                    ) {
                        Text(
                            "NEW",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ===================== HELPER FUNCTION =====================

private fun formatCompactDate(value: String): String {
    return try {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        val parsed = patterns.firstNotNullOfOrNull { pattern ->
            try {
                java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    .parse(value)
            } catch (_: Exception) {
                null
            }
        }

        if (parsed != null) {
            java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(parsed)
        } else {
            value
        }
    } catch (_: Exception) {
        value
    }
}

private fun formatAmountCompact(amount: Double): String {
    return java.text.NumberFormat.getIntegerInstance(java.util.Locale.getDefault()).format(amount)
}
