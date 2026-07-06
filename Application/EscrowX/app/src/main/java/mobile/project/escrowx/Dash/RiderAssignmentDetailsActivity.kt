package mobile.project.escrowx.dash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RiderAssignmentDetailsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                RiderAssignmentDetailsScreen(
                    transactionId = transactionId,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderAssignmentDetailsScreen(
    transactionId: String?,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var isLoading by remember { mutableStateOf(true) }
    var transaction by remember { mutableStateOf<EscrowResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadDetails() {
        val token = session.getAccessToken()
        if (transactionId.isNullOrBlank()) {
            error = "Missing transaction id"
            isLoading = false
            return
        }
        if (token.isNullOrBlank()) {
            error = "Session expired. Please log in again."
            isLoading = false
            return
        }

        isLoading = true
        error = null

        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.authenticated(token)
                val response = api.getTransactionById(transactionId)
                if (response.isSuccessful) {
                    transaction = response.body()
                } else {
                    error = "Failed to load assignment details"
                }
            } catch (_: Exception) {
                error = "Could not load assignment details"
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(transactionId) {
        loadDetails()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assignment Details",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            transaction != null -> {
                val txn = transaction!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(100),
                                        color = colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .padding(7.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = txn.title,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = txn.status,
                                            fontSize = 12.sp,
                                            color = colorScheme.primary
                                        )
                                    }
                                }

                                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)

                                DetailRow("Reference", txn.reference ?: "-")
                                DetailRow("Transaction ID", txn.id)
                                DetailRow("Buyer ID", txn.buyerId)
                                DetailRow("Seller ID", txn.sellerId)
                                DetailRow("Rider ID", txn.riderId ?: "-")
                                DetailRow("Amount", "${txn.amount} ${txn.currency ?: "KES"}")
                                DetailRow("Delivery Address", txn.deliveryAddress)
                                DetailRow("Delivery Due", formatAssignmentDate(txn.deliveryDueAt))
                                DetailRow("Auto Release", txn.autoReleaseAt?.let { formatAssignmentDate(it) } ?: "-")
                                DetailRow("Created", formatAssignmentDate(txn.createdAt))
                                DetailRow("Updated", formatAssignmentDate(txn.updatedAt))
                            }
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available", color = colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colorScheme: ColorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.62f)
        )
    }
}

private fun formatAssignmentDate(value: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(value)
        if (date != null) {
            val output = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            output.format(date)
        } else {
            value
        }
    } catch (_: Exception) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date? = parser.parse(value)
            if (date != null) {
                val output = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                output.format(date)
            } else {
                value
            }
        } catch (_: Exception) {
            value
        }
    }
}
