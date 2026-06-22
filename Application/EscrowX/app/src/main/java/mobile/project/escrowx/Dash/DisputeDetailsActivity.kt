package mobile.project.escrowx.dash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import mobile.project.escrowx.DisputeDetailsResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import java.util.Locale

class DisputeDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val disputeId = intent.getStringExtra("DISPUTE_ID") ?: ""
        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                DisputeDetailsScreen(disputeId = disputeId, transactionId = transactionId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisputeDetailsScreen(
    disputeId: String,
    transactionId: String
) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var dispute by remember { mutableStateOf<DisputeDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadDispute() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = session.getAccessToken()
                val userId = session.getUserId()

                if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                    errorMessage = "Session expired. Please login again."
                    return@launch
                }

                val api = RetrofitClient.authenticated(token)
                val response = if (disputeId.isNotBlank()) {
                    api.getDisputeById(userId, disputeId)
                } else {
                    api.getDisputeByTransactionId(userId, transactionId)
                }

                if (response.isSuccessful && response.body() != null) {
                    dispute = response.body()
                } else {
                    errorMessage = response.errorBody()?.string()?.take(220) ?: "Unable to load dispute details"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to load dispute details"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(disputeId, transactionId) {
        loadDispute()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dispute Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? DisputeDetailsActivity)?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadDispute() }, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            when {
                isLoading && dispute == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null && dispute == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(errorMessage ?: "Unable to load dispute details", color = colorScheme.error)
                        Button(onClick = { loadDispute() }) {
                            Text("Retry")
                        }
                    }
                }
                dispute != null -> {
                    val details = dispute!!
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = prettyEnum(details.status),
                                        color = when (details.status.uppercase(Locale.getDefault())) {
                                            "RESOLVED" -> Color(0xFF0B8A42)
                                            "REJECTED" -> Color(0xFFDC2626)
                                            else -> Color(0xFFB45309)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    DetailRow("Dispute ID", details.id)
                                    DetailRow("Transaction ID", details.transactionId)
                                    DetailRow("Transaction Ref", details.transactionReference ?: "-")
                                    DetailRow("Category", prettyEnum(details.category))
                                    DetailRow("Raised By", details.raisedByName ?: details.raisedById)
                                    DetailRow("Created", details.createdAt)
                                    DetailRow("Updated", details.updatedAt ?: "-")
                                    DetailRow("Resolved At", details.resolvedAt ?: "-")
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Description", fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                    Text(
                                        details.description ?: "-",
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Text("Resolution", fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                    Text(
                                        details.resolution ?: "Not resolved yet",
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        val evidence = details.evidenceUrls.orEmpty()
                        if (evidence.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Evidence Images", fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                        evidence.forEachIndexed { index, url ->
                                            EvidenceImageCard(
                                                index = index + 1,
                                                url = url,
                                                onOpen = {
                                                    try {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        )
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Unable to open evidence URL", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colorScheme = colorScheme
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.62f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EvidenceImageCard(
    index: Int,
    url: String,
    onOpen: () -> Unit,
    colorScheme: ColorScheme
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Text(
                    text = "Evidence $index",
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Dispute evidence image $index",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.height(14.dp)
                        )
                        Text(
                            text = "Open",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = url,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorScheme.onSurfaceVariant
            )

            if (!looksLikeImageUrl(url)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = colorScheme.error,
                        modifier = Modifier.height(14.dp)
                    )
                    Text(
                        text = "Preview may fail for non-image files. Tap to open.",
                        color = colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun looksLikeImageUrl(url: String): Boolean {
    val lower = url.lowercase(Locale.getDefault())
    return lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".png") ||
        lower.endsWith(".webp") ||
        lower.endsWith(".gif") ||
        lower.endsWith(".bmp") ||
        lower.contains("/uploads/disputes/")
}

private fun prettyEnum(raw: String): String {
    return raw
        .lowercase(Locale.getDefault())
        .split("_")
        .joinToString(" ") { token -> token.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) } }
}
