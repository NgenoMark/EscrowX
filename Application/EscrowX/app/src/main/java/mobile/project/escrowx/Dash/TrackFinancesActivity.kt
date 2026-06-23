package mobile.project.escrowx.dash

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import java.text.NumberFormat
import java.util.Locale

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

    val creditEntries = remember(entries) { entries.filter { it.type == FinanceType.CREDIT } }
    val debitEntries = remember(entries) { entries.filter { it.type == FinanceType.DEBIT } }

    val totalCredit = remember(creditEntries) { creditEntries.sumOf { it.amount } }
    val totalDebit = remember(debitEntries) { debitEntries.sumOf { it.amount } }

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
                        text = "Track Finances",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
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
    ) { paddingValues: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (role.equals("SELLER", ignoreCase = true)) {
                                "Seller Finance Overview"
                            } else {
                                "Buyer Finance Overview"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Track every credit and debit linked to your account. Tap any card to see full transaction details.",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinanceTotalCard(
                        modifier = Modifier.weight(1f),
                        title = "Credit",
                        amount = totalCredit,
                        iconTint = Color(0xFF166534),
                        background = Color(0xFFE9F9EF),
                        icon = Icons.Default.ArrowDownward
                    )
                    FinanceTotalCard(
                        modifier = Modifier.weight(1f),
                        title = "Debit",
                        amount = totalDebit,
                        iconTint = Color(0xFFB42318),
                        background = Color(0xFFFDECEC),
                        icon = Icons.Default.ArrowUpward
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                }
            }

            loadError?.let { error ->
                item {
                    MessageCard(
                        title = "Unable to load finances",
                        message = error,
                        tint = Color(0xFFB42318)
                    )
                }
            }

            if (!isLoading && loadError == null && entries.isEmpty()) {
                item {
                    MessageCard(
                        title = "No finance records yet",
                        message = "When you start paying or receiving escrow transactions, they will appear here.",
                        tint = Color(0xFF1D4ED8)
                    )
                }
            }

            if (creditEntries.isNotEmpty()) {
                item { SectionTitle("Money Received (Credit)") }
                items(creditEntries, key = { it.entryId }) { entry ->
                    FinanceEntryCard(entry = entry, onClick = { selectedEntry = entry })
                }
            }

            if (debitEntries.isNotEmpty()) {
                item { SectionTitle("Money Paid (Debit)") }
                items(debitEntries, key = { it.entryId }) { entry ->
                    FinanceEntryCard(entry = entry, onClick = { selectedEntry = entry })
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    selectedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = {
                Text(
                    text = if (entry.type == FinanceType.CREDIT) "Credit Details" else "Debit Details",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Message", value = entry.message)
                    DetailRow(label = "Amount", value = formatCurrency(entry.amount))
                    DetailRow(label = "Status", value = entry.statusLabel)
                    DetailRow(label = "Transaction ID", value = entry.transactionId)
                    DetailRow(label = "Reference", value = entry.reference ?: "-")
                    DetailRow(label = "Title", value = entry.title)
                    DetailRow(label = "Description", value = entry.description.ifBlank { "-" })
                    DetailRow(label = "Counterparty", value = entry.counterpartyId)
                    DetailRow(label = "Created", value = entry.createdAtDisplay)
                    DetailRow(label = "Updated", value = entry.updatedAtDisplay)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun FinanceTotalCard(
    modifier: Modifier,
    title: String,
    amount: Double,
    iconTint: Color,
    background: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, color = iconTint, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = formatCurrency(amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun MessageCard(title: String, message: String, tint: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = tint)
            Text(message, color = Color(0xFF334155), fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun FinanceEntryCard(entry: FinanceEntry, onClick: () -> Unit) {
    val accent = if (entry.type == FinanceType.CREDIT) Color(0xFF166534) else Color(0xFFB42318)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.message,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatCurrency(entry.amount),
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Status: ${entry.statusLabel}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Created: ${entry.createdAtDisplay}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap for full details",
                fontSize = 12.sp,
                color = accent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

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
    return raw
        .replace("T", " ")
        .replace("Z", "")
        .take(19)
}
