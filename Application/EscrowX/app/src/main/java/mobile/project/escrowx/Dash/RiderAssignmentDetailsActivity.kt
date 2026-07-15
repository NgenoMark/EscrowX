package mobile.project.escrowx.dash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.AuthApiService
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import mobile.project.escrowx.ui.theme.BrandBlue
import java.text.SimpleDateFormat
import java.util.*

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
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var isLoading by remember { mutableStateOf(true) }
    var isActionLoading by remember { mutableStateOf(false) }
    var transaction by remember { mutableStateOf<EscrowResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf("") }
    var hasAcceptedDelivery by remember { mutableStateOf(false) }
    var nextRiderAction by remember { mutableStateOf(RiderNextAction.NONE) }
    var riderAssignmentStatus by remember { mutableStateOf<String?>(null) }
    var buyerDetails by remember { mutableStateOf(PartySectionData()) }
    var sellerDetails by remember { mutableStateOf(PartySectionData()) }

    suspend fun resolvePartySectionData(api: AuthApiService, userId: String?): PartySectionData {
        if (userId.isNullOrBlank()) return PartySectionData(name = "Not assigned")
        return try {
            val response = api.getUserById(userId)
            if (!response.isSuccessful) {
                PartySectionData(name = userId.take(12))
            } else {
                val body = response.body()
                val resolvedName = when {
                    !body?.displayName.isNullOrBlank() -> body?.displayName ?: userId.take(12)
                    !body?.email.isNullOrBlank() -> body?.email?.substringBefore("@") ?: userId.take(12)
                    else -> userId.take(12)
                }
                PartySectionData(
                    name = resolvedName,
                    phone = body?.phone ?: "-",
                    address = body?.address ?: "-"
                )
            }
        } catch (_: Exception) {
            PartySectionData(name = userId.take(12))
        }
    }

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
                    val txn = response.body()
                    transaction = txn

                    if (txn != null) {
                        riderAssignmentStatus = txn.riderAssignmentStatus
                        val acceptedOrBeyond = isAcceptedOrBeyondAssignment(txn.riderAssignmentStatus)
                        hasAcceptedDelivery = acceptedOrBeyond
                        nextRiderAction = deriveNextRiderAction(
                            status = txn.status,
                            hasAcceptedDelivery = acceptedOrBeyond,
                            riderAssignmentStatus = txn.riderAssignmentStatus
                        )
                    }

                    if (txn != null) {
                        buyerDetails = resolvePartySectionData(api, txn.buyerId)
                        sellerDetails = resolvePartySectionData(api, txn.sellerId)
                    }
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

    fun runRiderAction(
        successMessage: String,
        onSuccess: (() -> Unit)? = null,
        action: suspend (AuthApiService, String, String) -> retrofit2.Response<EscrowResponse>
    ) {
        val token = session.getAccessToken()
        val actorUserId = session.getUserId()
        if (transactionId.isNullOrBlank() || token.isNullOrBlank() || actorUserId.isNullOrBlank()) {
            actionMessage = "Session or transaction is missing"
            return
        }

        isActionLoading = true
        actionMessage = null

        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.authenticated(token)
                val response = action(api, transactionId, actorUserId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        transaction = response.body() ?: transaction
                        riderAssignmentStatus = transaction?.riderAssignmentStatus
                        onSuccess?.invoke()
                        transaction?.let { updatedTxn ->
                            val acceptedOrBeyond = isAcceptedOrBeyondAssignment(updatedTxn.riderAssignmentStatus)
                            hasAcceptedDelivery = acceptedOrBeyond
                            nextRiderAction = deriveNextRiderAction(
                                status = updatedTxn.status,
                                hasAcceptedDelivery = acceptedOrBeyond,
                                riderAssignmentStatus = updatedTxn.riderAssignmentStatus
                            )
                        }
                        successDialogMessage = successMessage
                        showSuccessDialog = true
                        actionMessage = successMessage
                    } else {
                        actionMessage = "Action failed. Please check current package stage and try again."
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    actionMessage = "Could not update package status right now"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isActionLoading = false
                }
            }
        }
    }

    LaunchedEffect(transactionId) {
        loadDetails()
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF10B981)
                    )
                }
            },
            title = {
                Text(
                    "Success! 🎉",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    successDialogMessage,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showSuccessDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assignment Details",
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
                actions = {
                    IconButton(
                        onClick = { loadDetails() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
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
    ) { padding ->
        when {
            isLoading -> {
                RiderAssignmentLoadingState(colorScheme = colorScheme)
            }
            error != null -> {
                RiderAssignmentErrorState(
                    message = error ?: "Unknown error",
                    onRetry = { loadDetails() },
                    colorScheme = colorScheme
                )
            }
            transaction != null -> {
                val txn = transaction!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(colorScheme.background),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ===== STATUS HEADER CARD =====
                    item {
                        StatusHeaderCard(
                            status = txn.status,
                            title = txn.title,
                            reference = txn.reference,
                            colorScheme = colorScheme
                        )
                    }

                    // ===== BUYER SECTION =====
                    item {
                        ParticipantSectionCard(
                            sectionTitle = "Buyer",
                            sectionIcon = Icons.Default.Person,
                            participantName = buyerDetails.name,
                            participantPhone = buyerDetails.phone,
                            participantAddress = buyerDetails.address,
                            colorScheme = colorScheme
                        )
                    }

                    // ===== SELLER SECTION =====
                    item {
                        ParticipantSectionCard(
                            sectionTitle = "Seller",
                            sectionIcon = Icons.Default.Store,
                            participantName = sellerDetails.name,
                            participantPhone = sellerDetails.phone,
                            participantAddress = sellerDetails.address,
                            colorScheme = colorScheme
                        )
                    }

                    // ===== DELIVERY INFO =====
                    item {
                        DeliveryInfoCard(
                            transaction = txn,
                            colorScheme = colorScheme
                        )
                    }

                    // ===== RIDER ACTIONS =====
                    item {
                        RiderActionCardEnhanced(
                            status = txn.status,
                            hasAcceptedDelivery = hasAcceptedDelivery,
                            nextAction = nextRiderAction,
                            isActionLoading = isActionLoading,
                            actionMessage = actionMessage,
                            riderAssignmentStatus = riderAssignmentStatus,
                            onAcceptDelivery = {
                                runRiderAction(
                                    successMessage = "✅ Delivery accepted",
                                    onSuccess = {
                                        hasAcceptedDelivery = true
                                        nextRiderAction = RiderNextAction.PICKUP
                                    }
                                ) { api, id, actorId ->
                                    api.riderAcceptDelivery(id, actorId)
                                }
                            },
                            onPickup = {
                                runRiderAction(
                                    successMessage = "📦 Package marked as picked up",
                                    onSuccess = { nextRiderAction = RiderNextAction.START_TRANSIT }
                                ) { api, id, actorId ->
                                    api.riderPickup(id, actorId)
                                }
                            },
                            onStartTransit = {
                                runRiderAction(
                                    successMessage = "🚚 Transit started",
                                    onSuccess = { nextRiderAction = RiderNextAction.ARRIVED }
                                ) { api, id, actorId ->
                                    api.riderStartTransit(id, actorId)
                                }
                            },
                            onArrived = {
                                runRiderAction(
                                    successMessage = "📍 Arrived at buyer location",
                                    onSuccess = { nextRiderAction = RiderNextAction.DELIVERED }
                                ) { api, id, actorId ->
                                    api.riderArrived(id, actorId)
                                }
                            },
                            onDelivered = {
                                runRiderAction(
                                    successMessage = "✅ Successfully delivered",
                                    onSuccess = { nextRiderAction = RiderNextAction.NONE }
                                ) { api, id, actorId ->
                                    api.riderMarkDelivered(id, actorId)
                                }
                            },
                            colorScheme = colorScheme
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
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

// ===================== LOADING & ERROR STATES =====================

@Composable
private fun RiderAssignmentLoadingState(colorScheme: ColorScheme) {
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
                "Loading assignment details...",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RiderAssignmentErrorState(
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
                    .size(64.dp)
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

// ===================== STATUS HEADER CARD =====================

@Composable
fun StatusHeaderCard(
    status: String,
    title: String,
    reference: String?,
    colorScheme: ColorScheme
) {
    val statusConfig = getAssignmentStatusConfig(status)
    val isNew = status.equals("SELLER_ACCEPTED", ignoreCase = true)

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
            // Reference & Status
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
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Ref: ${reference?.take(8) ?: "N/A"}",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
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
            }

            // Title
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // New badge
            if (isNew) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFDC2626),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        "NEW ASSIGNMENT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ===================== PARTICIPANT SECTION CARD =====================

@Composable
fun ParticipantSectionCard(
    sectionTitle: String,
    sectionIcon: ImageVector,
    participantName: String,
    participantPhone: String,
    participantAddress: String,
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
                sectionTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            DetailRowEnhanced(
                icon = sectionIcon,
                label = "Name",
                value = participantName,
                colorScheme = colorScheme
            )
            DetailRowEnhanced(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = participantPhone,
                colorScheme = colorScheme
            )
            DetailRowEnhanced(
                icon = Icons.Default.Place,
                label = "Address",
                value = participantAddress,
                colorScheme = colorScheme
            )
        }
    }
}

// ===================== DELIVERY INFO CARD =====================

@Composable
fun DeliveryInfoCard(
    transaction: EscrowResponse,
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
                Text(
                    "Delivery Information",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Icon(
                    Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            DetailRowEnhanced(
                icon = Icons.Default.Inventory2,
                label = "Product",
                value = transaction.title,
                colorScheme = colorScheme
            )
            DetailRowEnhanced(
                icon = Icons.Default.Money,
                label = "Cost",
                value = "${transaction.amount} ${transaction.currency ?: "KES"}",
                colorScheme = colorScheme,
                isHighlighted = true
            )
            DetailRowEnhanced(
                icon = Icons.Default.Place,
                label = "Location",
                value = transaction.deliveryAddress,
                colorScheme = colorScheme
            )
            DetailRowEnhanced(
                icon = Icons.Default.Schedule,
                label = "Delivery Date",
                value = formatAssignmentDate(transaction.deliveryDueAt),
                colorScheme = colorScheme
            )
        }
    }
}

// ===================== DETAIL ROW ENHANCED =====================

@Composable
fun DetailRowEnhanced(
    icon: ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme,
    isHighlighted: Boolean = false
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
            tint = if (isHighlighted) colorScheme.primary else colorScheme.onSurfaceVariant
        )
        Text(
            label,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            ":",
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = if (isHighlighted) 15.sp else 13.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) colorScheme.primary else colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ===================== RIDER ACTION CARD ENHANCED =====================

@Composable
fun RiderActionCardEnhanced(
    status: String,
    hasAcceptedDelivery: Boolean,
    nextAction: RiderNextAction,
    isActionLoading: Boolean,
    actionMessage: String?,
    riderAssignmentStatus: String?,
    onAcceptDelivery: () -> Unit,
    onPickup: () -> Unit,
    onStartTransit: () -> Unit,
    onArrived: () -> Unit,
    onDelivered: () -> Unit,
    colorScheme: ColorScheme
) {
    val normalizedStatus = status.trim().uppercase(Locale.ROOT)
    val isInActionableState = normalizedStatus == "SELLER_ACCEPTED" || normalizedStatus == "IN_DELIVERY"

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
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.primary
                    )
                    Text(
                        "Rider Actions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                if (isActionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.primary
                    )
                }
            }

            if (actionMessage != null && !isActionLoading) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (actionMessage.contains("✅"))
                        Color(0xFF10B981).copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                ) {
                    Text(
                        actionMessage,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 13.sp,
                        color = if (actionMessage.contains("✅"))
                            Color(0xFF10B981)
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            if (isInActionableState) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val effectiveNextAction = when {
                        nextAction != RiderNextAction.NONE -> nextAction
                        riderAssignmentStatus != null -> RiderNextAction.NONE
                        normalizedStatus == "SELLER_ACCEPTED" && !hasAcceptedDelivery -> RiderNextAction.ACCEPT
                        normalizedStatus == "SELLER_ACCEPTED" && hasAcceptedDelivery -> RiderNextAction.PICKUP
                        else -> RiderNextAction.NONE
                    }

                    when (effectiveNextAction) {
                        RiderNextAction.ACCEPT -> ActionButton(
                            text = "Accept Delivery",
                            icon = Icons.Default.CheckCircle,
                            color = colorScheme.primary,
                            onClick = onAcceptDelivery,
                            isActionLoading = isActionLoading,
                            isPrimary = true,
                            colorScheme = colorScheme
                        )

                        RiderNextAction.PICKUP -> ActionButton(
                            text = "Mark as Picked Up",
                            icon = Icons.Default.Inventory,
                            color = Color(0xFF3B82F6),
                            onClick = onPickup,
                            isActionLoading = isActionLoading,
                            isPrimary = true,
                            colorScheme = colorScheme
                        )

                        RiderNextAction.START_TRANSIT -> ActionButton(
                            text = "Start Transit",
                            icon = Icons.Default.LocalShipping,
                            color = Color(0xFFF59E0B),
                            onClick = onStartTransit,
                            isActionLoading = isActionLoading,
                            isPrimary = true,
                            colorScheme = colorScheme
                        )

                        RiderNextAction.ARRIVED -> ActionButton(
                            text = "Arrived at Buyer",
                            icon = Icons.Default.Place,
                            color = Color(0xFF8B5CF6),
                            onClick = onArrived,
                            isActionLoading = isActionLoading,
                            isPrimary = true,
                            colorScheme = colorScheme
                        )

                        RiderNextAction.DELIVERED -> ActionButton(
                            text = "Mark as Delivered",
                            icon = Icons.Default.DoneAll,
                            color = Color(0xFF10B981),
                            onClick = onDelivered,
                            isActionLoading = isActionLoading,
                            isPrimary = true,
                            colorScheme = colorScheme
                        )

                        RiderNextAction.NONE -> {
                            val staleStatusMessage = if (riderAssignmentStatus.isNullOrBlank()) {
                                "No rider assignment status found. Pull to refresh and retry."
                            } else {
                                "No next rider action available"
                            }
                            Text(
                                staleStatusMessage,
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Not actionable state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "No rider actions available at this stage",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Current status: ${statusConfigDisplay(normalizedStatus)}",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    isActionLoading: Boolean,
    isPrimary: Boolean,
    colorScheme: ColorScheme
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = !isActionLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = !isActionLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = color
            ),
            border = BorderStroke(1.5.dp, color.copy(alpha = 0.5f))
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ===================== HELPER FUNCTIONS =====================

data class AssignmentStatusConfig(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val backgroundColor: Color
)

private data class PartySectionData(
    val name: String = "-",
    val phone: String = "-",
    val address: String = "-"
)

enum class RiderNextAction {
    ACCEPT,
    PICKUP,
    START_TRANSIT,
    ARRIVED,
    DELIVERED,
    NONE
}

internal fun deriveNextRiderAction(
    status: String,
    hasAcceptedDelivery: Boolean,
    riderAssignmentStatus: String?
): RiderNextAction {
    if (riderAssignmentStatus.isNullOrBlank()) {
        return RiderNextAction.NONE
    }

    return when (riderAssignmentStatus?.trim()?.uppercase(Locale.ROOT)) {
        "ASSIGNED" -> if (hasAcceptedDelivery) RiderNextAction.PICKUP else RiderNextAction.ACCEPT
        "ACCEPTED" -> RiderNextAction.PICKUP
        "PICKED_UP" -> RiderNextAction.START_TRANSIT
        "IN_TRANSIT" -> RiderNextAction.ARRIVED
        "ARRIVED_AT_BUYER" -> RiderNextAction.DELIVERED
        "DELIVERED_TO_BUYER", "FAILED", "CANCELLED" -> RiderNextAction.NONE
        else -> RiderNextAction.NONE
    }
}

private fun isAcceptedOrBeyondAssignment(riderAssignmentStatus: String?): Boolean {
    return when (riderAssignmentStatus?.trim()?.uppercase(Locale.ROOT)) {
        "ACCEPTED", "PICKED_UP", "IN_TRANSIT", "ARRIVED_AT_BUYER", "DELIVERED_TO_BUYER" -> true
        else -> false
    }
}

fun getAssignmentStatusConfig(status: String): AssignmentStatusConfig {
    val normalized = status.uppercase()
    return when {
        normalized.contains("COMPLETED") || normalized.contains("DELIVERED") -> AssignmentStatusConfig(
            label = "Completed",
            dotColor = Color(0xFF10B981),
            textColor = Color(0xFF10B981),
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.12f)
        )
        normalized.contains("IN_DELIVERY") || normalized.contains("TRANSIT") -> AssignmentStatusConfig(
            label = "In Transit",
            dotColor = Color(0xFFF59E0B),
            textColor = Color(0xFFF59E0B),
            backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
        )
        normalized.contains("ACCEPTED") -> AssignmentStatusConfig(
            label = "Accepted",
            dotColor = Color(0xFF3B82F6),
            textColor = Color(0xFF3B82F6),
            backgroundColor = Color(0xFF3B82F6).copy(alpha = 0.12f)
        )
        normalized.contains("ASSIGNED") -> AssignmentStatusConfig(
            label = "Assigned",
            dotColor = Color(0xFF8B5CF6),
            textColor = Color(0xFF8B5CF6),
            backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)
        )
        else -> AssignmentStatusConfig(
            label = status,
            dotColor = Color(0xFF6B7280),
            textColor = Color(0xFF6B7280),
            backgroundColor = Color(0xFF6B7280).copy(alpha = 0.12f)
        )
    }
}

fun statusConfigDisplay(status: String): String {
    return status
        .lowercase(Locale.getDefault())
        .split("_")
        .joinToString(" ") { token ->
            token.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
        }
}

private fun formatAssignmentDate(value: String): String {
    return try {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        val parsed = patterns.firstNotNullOfOrNull { pattern ->
            try {
                SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)
            } catch (_: Exception) {
                null
            }
        }

        if (parsed != null) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(parsed)
        } else {
            value
        }
    } catch (_: Exception) {
        value
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderAssignmentDetailsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        RiderAssignmentDetailsScreen(transactionId = "TX-123", onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderAssignmentDetailsScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        RiderAssignmentDetailsScreen(transactionId = "TX-123", onBack = {})
    }
}