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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    val evidence = dispute?.evidenceUrls.orEmpty()

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
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
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
                    IconButton(
                        onClick = { loadDispute() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = colorScheme.onSurface
                            )
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
                .background(colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            when {
                isLoading && dispute == null -> {
                    LoadingState(colorScheme = colorScheme)
                }
                errorMessage != null && dispute == null -> {
                    ErrorState(
                        message = errorMessage ?: "Unable to load dispute details",
                        onRetry = { loadDispute() },
                        colorScheme = colorScheme
                    )
                }
                dispute != null -> {
                    val details = dispute!!
                    val isResolved = details.status.uppercase(Locale.getDefault()) == "RESOLVED"

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // ===== STATUS HEADER =====
                        item {
                            StatusHeaderCard(
                                status = details.status,
                                isResolved = isResolved,
                                colorScheme = colorScheme
                            )
                        }

                        // ===== DISPUTE INFO =====
                        item {
                            DisputeInfoCard(
                                details = details,
                                colorScheme = colorScheme
                            )
                        }

                        // ===== DESCRIPTION & RESOLUTION =====
                        item {
                            DescriptionCard(
                                description = details.description ?: "-",
                                resolution = details.resolution ?: "Not resolved yet",
                                isResolved = isResolved,
                                colorScheme = colorScheme
                            )
                        }

                        // ===== EVIDENCE SECTION =====
                        if (evidence.isNotEmpty()) {
                            item {
                                EvidenceSection(
                                    evidence = evidence,
                                    onImageClick = { url, index ->
                                        previewImageUrl = url
                                        selectedImageIndex = index
                                    },
                                    colorScheme = colorScheme
                                )
                            }
                        }

                        // ===== TIMELINE =====
                        item {
                            TimelineCard(
                                createdAt = details.createdAt,
                                updatedAt = details.updatedAt,
                                resolvedAt = details.resolvedAt,
                                isResolved = isResolved,
                                colorScheme = colorScheme
                            )
                        }

                        // ===== ACTION BUTTONS =====
                        item {
                            DisputeActions(
                                isResolved = isResolved,
                                onContactSupport = {
                                    Toast.makeText(context, "Contact support coming soon", Toast.LENGTH_SHORT).show()
                                },
                                onClose = {
                                    Toast.makeText(context, "Close dispute coming soon", Toast.LENGTH_SHORT).show()
                                },
                                colorScheme = colorScheme
                            )
                        }
                    }
                }
            }
        }

        // ===== IMAGE PREVIEW DIALOG =====
        if (!previewImageUrl.isNullOrBlank()) {
            ImagePreviewDialog(
                imageUrl = previewImageUrl!!,
                evidence = evidence,
                currentIndex = selectedImageIndex,
                onDismiss = { previewImageUrl = null },
                onNavigate = { index ->
                    selectedImageIndex = index
                    previewImageUrl = evidence.getOrNull(index)
                },
                colorScheme = colorScheme
            )
        }
    }
}

// ===================== LOADING & ERROR STATES =====================

@Composable
fun LoadingState(colorScheme: ColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Loading dispute details...",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = "Error",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
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

// ===================== STATUS HEADER =====================

@Composable
fun StatusHeaderCard(
    status: String,
    isResolved: Boolean,
    colorScheme: ColorScheme
) {
    val statusConfig = getDisputeStatusConfig(status)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                statusConfig.color,
                                statusConfig.color.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusConfig.icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }

            Text(
                text = if (isResolved) "Resolved" else "Active Dispute",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = statusConfig.color.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, statusConfig.color.copy(alpha = 0.2f))
            ) {
                Text(
                    text = prettyEnum(status),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusConfig.color
                )
            }
        }
    }
}

// ===================== DISPUTE INFO CARD =====================

@Composable
fun DisputeInfoCard(
    details: DisputeDetailsResponse,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Dispute Information",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            DisputeInfoRow(
                icon = Icons.Default.Tag,
                label = "Dispute ID",
                value = details.id,
                colorScheme = colorScheme
            )

            DisputeInfoRow(
                icon = Icons.Default.ReceiptLong,
                label = "Transaction ID",
                value = details.transactionId,
                colorScheme = colorScheme
            )

            details.transactionReference?.let { ref ->
                DisputeInfoRow(
                    icon = Icons.Default.Tag,
                    label = "Transaction Ref",
                    value = ref,
                    colorScheme = colorScheme
                )
            }

            DisputeInfoRow(
                icon = Icons.Default.Category,
                label = "Category",
                value = prettyEnum(details.category),
                colorScheme = colorScheme
            )

            DisputeInfoRow(
                icon = Icons.Default.Person,
                label = "Raised By",
                value = details.raisedByName ?: details.raisedById,
                colorScheme = colorScheme
            )
        }
    }
}

@Composable
fun DisputeInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
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
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ===================== DESCRIPTION CARD =====================

@Composable
fun DescriptionCard(
    description: String,
    resolution: String,
    isResolved: Boolean,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Description
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.primary
                    )
                    Text(
                        "Description",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Text(
                        description,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Resolution
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isResolved) Icons.Default.CheckCircle else Icons.Default.Pending,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isResolved) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Text(
                        "Resolution",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isResolved)
                        Color(0xFF10B981).copy(alpha = 0.06f)
                    else
                        colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isResolved)
                        BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                    else
                        null
                ) {
                    Text(
                        resolution,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = if (isResolved) Color(0xFF10B981) else colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ===================== EVIDENCE SECTION =====================

@Composable
fun EvidenceSection(
    evidence: List<String>,
    onImageClick: (String, Int) -> Unit,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.primary
                    )
                    Text(
                        "Evidence (${evidence.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        "${evidence.size} image${if (evidence.size > 1) "s" else ""}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Evidence Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                evidence.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEachIndexed { index, url ->
                            val globalIndex = evidence.indexOf(url)
                            val resolvedUrl = RetrofitClient.resolveApiUrl(url) ?: url
                            EvidenceThumbnail(
                                url = resolvedUrl,
                                index = globalIndex + 1,
                                total = evidence.size,
                                onClick = { onImageClick(resolvedUrl, globalIndex) },
                                modifier = Modifier.weight(1f),
                                colorScheme = colorScheme
                            )
                        }
                        // Fill empty space if odd number
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EvidenceThumbnail(
    url: String,
    index: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Evidence image $index",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay with index
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    "$index",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // View button overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "View",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ===================== TIMELINE CARD =====================

@Composable
fun TimelineCard(
    createdAt: String,
    updatedAt: String?,
    resolvedAt: String?,
    isResolved: Boolean,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Timeline",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            TimelineItem(
                icon = Icons.Default.CalendarToday,
                label = "Created",
                value = formatDisputeDate(createdAt),
                color = colorScheme.primary,
                colorScheme = colorScheme
            )

            if (updatedAt != null && updatedAt.isNotBlank()) {
                TimelineItem(
                    icon = Icons.Default.Update,
                    label = "Last Updated",
                    value = formatDisputeDate(updatedAt),
                    color = colorScheme.onSurfaceVariant,
                    colorScheme = colorScheme
                )
            }

            if (isResolved && resolvedAt != null && resolvedAt.isNotBlank()) {
                TimelineItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Resolved",
                    value = formatDisputeDate(resolvedAt),
                    color = Color(0xFF10B981),
                    colorScheme = colorScheme
                )
            }
        }
    }
}

@Composable
fun TimelineItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                label,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ===================== DISPUTE ACTIONS =====================

@Composable
fun DisputeActions(
    isResolved: Boolean,
    onContactSupport: () -> Unit,
    onClose: () -> Unit,
    colorScheme: ColorScheme
) {
    if (!isResolved) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Actions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )

                Button(
                    onClick = onContactSupport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
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
                        Icons.Default.SupportAgent,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Contact Support",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Close Dispute",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ===================== IMAGE PREVIEW DIALOG =====================

@Composable
fun ImagePreviewDialog(
    imageUrl: String,
    evidence: List<String>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onNavigate: (Int) -> Unit,
    colorScheme: ColorScheme
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Image Counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "${currentIndex + 1} / ${evidence.size}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { /* Don't dismiss on image click */ },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Evidence preview ${currentIndex + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                // Navigation Buttons
                if (evidence.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentIndex > 0) {
                            Button(
                                onClick = { onNavigate(currentIndex - 1) },
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(end = 8.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (currentIndex < evidence.size - 1) {
                            Button(
                                onClick = { onNavigate(currentIndex + 1) },
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(start = 8.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Close", fontSize = 14.sp)
                }
            }
        }
    }
}

// ===================== HELPER FUNCTIONS =====================

private fun getDisputeStatusConfig(status: String): DisputeStatusConfig {
    val normalized = status.uppercase(Locale.getDefault())
    return when (normalized) {
        "RESOLVED" -> DisputeStatusConfig(
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF10B981)
        )
        "REJECTED" -> DisputeStatusConfig(
            icon = Icons.Default.Cancel,
            color = Color(0xFFDC2626)
        )
        else -> DisputeStatusConfig(
            icon = Icons.Default.Warning,
            color = Color(0xFFF59E0B)
        )
    }
}

private data class DisputeStatusConfig(
    val icon: ImageVector,
    val color: Color
)

private fun prettyEnum(raw: String): String {
    return raw
        .lowercase(Locale.getDefault())
        .split("_")
        .joinToString(" ") { token -> token.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) } }
}

private fun formatDisputeDate(dateString: String): String {
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

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun DisputeDetailsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        // Preview with mock data would go here
    }
}