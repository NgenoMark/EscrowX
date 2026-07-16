package mobile.project.escrowx.seller

import android.content.Intent
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
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.InAppNotificationResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.BuyerTransactionDetailActivity
import mobile.project.escrowx.dash.RiderAssignmentDetailsActivity
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import mobile.project.escrowx.ui.theme.BrandBlue
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class SellerNotificationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                SellerNotificationsScreen(onBack = { finish() })
            }
        }
    }
}

private enum class NotificationTab(val apiValue: String) {
    UNREAD("UNREAD"),
    READ("READ"),
    ARCHIVED("ARCHIVED")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellerNotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var isLoading by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf<List<InAppNotificationResponse>>(emptyList()) }
    var selectedNotification by remember { mutableStateOf<InAppNotificationResponse?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(NotificationTab.UNREAD) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var readCount by remember { mutableIntStateOf(0) }
    var archivedCount by remember { mutableIntStateOf(0) }

    fun loadNotifications(tab: NotificationTab) {
        val token = session.getAccessToken()
        val actorId = session.getUserId()
        if (token.isNullOrBlank() || actorId.isNullOrBlank()) {
            isLoading = false
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.authenticated(token)
                    .getNotifications(actorId, 0, 100, tab.apiValue)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        notifications = response.body()?.content.orEmpty()
                            .sortedByDescending { it.createdAt }
                    } else {
                        Toast.makeText(context, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Network error loading notifications", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun loadCounts() {
        val token = session.getAccessToken()
        val actorId = session.getUserId()
        if (token.isNullOrBlank() || actorId.isNullOrBlank()) return

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.authenticated(token).getNotificationCounts(actorId)
                if (response.isSuccessful) {
                    val counts = response.body().orEmpty()
                    withContext(Dispatchers.Main) {
                        unreadCount = (counts["unread"] ?: 0L).toInt()
                        readCount = (counts["read"] ?: 0L).toInt()
                        archivedCount = (counts["archived"] ?: 0L).toInt()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun markRead(item: InAppNotificationResponse) {
        val token = session.getAccessToken()
        val actorId = session.getUserId()
        if (token.isNullOrBlank() || actorId.isNullOrBlank()) return

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.authenticated(token).markNotificationRead(actorId, item.id)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        notifications = notifications.map {
                            if (it.id == item.id) it.copy(status = "READ", readAt = response.body()?.updatedAt ?: it.readAt)
                            else it
                        }
                        loadCounts()
                        if (selectedTab == NotificationTab.UNREAD) {
                            notifications = notifications.filterNot { it.status.equals("READ", true) }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun archiveNotification(item: InAppNotificationResponse) {
        val token = session.getAccessToken()
        val actorId = session.getUserId()
        if (token.isNullOrBlank() || actorId.isNullOrBlank()) return

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.authenticated(token).archiveNotification(actorId, item.id)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        notifications = notifications.filterNot { it.id == item.id }
                        Toast.makeText(context, "Notification archived", Toast.LENGTH_SHORT).show()
                        loadCounts()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun openNotification(item: InAppNotificationResponse) {
        if (item.status.equals("UNREAD", true)) {
            markRead(item)
        }
        if (openNotificationDeepLink(context, session, item)) {
            return
        }
        selectedNotification = item
        showDetailDialog = true
    }

    LaunchedEffect(Unit) {
        loadCounts()
        loadNotifications(selectedTab)
    }

    LaunchedEffect(selectedTab) {
        loadNotifications(selectedTab)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
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
                    if (unreadCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDC2626)
                        ) {
                            Text(
                                unreadCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            loadCounts()
                            loadNotifications(selectedTab)
                        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colorScheme.background)
        ) {
            NotificationTabBar(
                selectedTab = selectedTab,
                unreadCount = unreadCount,
                readCount = readCount,
                archivedCount = archivedCount,
                onSelected = { selectedTab = it },
                colorScheme = colorScheme
            )

            when {
                isLoading -> {
                    LoadingState(colorScheme = colorScheme)
                }
                notifications.isEmpty() -> {
                    EmptyState(
                        onRefresh = {
                            loadCounts()
                            loadNotifications(selectedTab)
                        },
                        colorScheme = colorScheme
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications, key = { it.id }) { item ->
                            ModernNotificationCard(
                                item = item,
                                onOpen = { openNotification(item) },
                                onMarkRead = { markRead(item) },
                                onArchive = { archiveNotification(item) },
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
    if (showDetailDialog && selectedNotification != null) {
        NotificationDetailDialog(
            notification = selectedNotification!!,
            onDismiss = { showDetailDialog = false },
            onArchive = {
                archiveNotification(selectedNotification!!)
                showDetailDialog = false
            },
            colorScheme = colorScheme
        )
    }
}

@Composable
private fun NotificationTabBar(
    selectedTab: NotificationTab,
    unreadCount: Int,
    readCount: Int,
    archivedCount: Int,
    onSelected: (NotificationTab) -> Unit,
    colorScheme: ColorScheme
) {
    val tabs = listOf(
        NotificationTab.UNREAD to unreadCount,
        NotificationTab.READ to readCount,
        NotificationTab.ARCHIVED to archivedCount
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (tab, count) ->
            val selected = tab == selectedTab
            FilterChip(
                selected = selected,
                onClick = { onSelected(tab) },
                label = {
                    Text(
                        text = "${tab.name} ($count)",
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary.copy(alpha = 0.12f),
                    selectedLabelColor = colorScheme.primary,
                    containerColor = colorScheme.surface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = if (selected) colorScheme.primary.copy(alpha = 0.5f) else colorScheme.outlineVariant
                )
            )
        }
    }
}

// ===================== LOADING STATE =====================

@Composable
fun LoadingState(colorScheme: ColorScheme) {
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
                "Loading notifications...",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===================== EMPTY STATE =====================

@Composable
fun EmptyState(
    onRefresh: () -> Unit,
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
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.NotificationsOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = colorScheme.primary
                )
            }
            Text(
                "All Clear!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                "You have no notifications at the moment.\nWe'll notify you when something important happens.",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRefresh,
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
                Text("Refresh")
            }
        }
    }
}

// ===================== MODERN NOTIFICATION CARD =====================

@Composable
fun ModernNotificationCard(
    item: InAppNotificationResponse,
    onOpen: () -> Unit,
    onMarkRead: () -> Unit,
    onArchive: () -> Unit,
    colorScheme: ColorScheme
) {
    val statusConfig = getStatusConfig(item)
    val formattedDate = formatNotificationDate(item.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusConfig.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusConfig.icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = statusConfig.iconColor
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = item.body,
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

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

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusConfig.badgeColor,
                        border = BorderStroke(1.dp, statusConfig.badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            statusConfig.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusConfig.badgeTextColor
                        )
                    }
                }
            }
        }
    }
}

// ===================== NOTIFICATION DETAIL DIALOG =====================

@Composable
fun NotificationDetailDialog(
    notification: InAppNotificationResponse,
    onDismiss: () -> Unit,
    onArchive: () -> Unit,
    colorScheme: ColorScheme
) {
    val isUnread = notification.status.equals("UNREAD", true)
    val statusConfig = getStatusConfig(notification)
    val formattedDate = formatNotificationDate(notification.createdAt)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 550.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(statusConfig.bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            statusConfig.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = statusConfig.iconColor
                        )
                    }
                    Column {
                        Text(
                            notification.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusConfig.badgeColor,
                                border = BorderStroke(1.dp, statusConfig.badgeColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    statusConfig.label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusConfig.badgeTextColor
                                )
                            }
                            if (isUnread) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626))
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Body
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Text(
                        notification.body,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colorScheme.onSurface
                    )
                }

                // Metadata
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NotificationDetailRow(
                        icon = Icons.Default.Schedule,
                        label = "Received",
                        value = formattedDate,
                        colorScheme = colorScheme
                    )
                    NotificationDetailRow(
                        icon = Icons.Default.Tag,
                        label = "Status",
                        value = if (isUnread) "Unread" else "Read",
                        colorScheme = colorScheme
                    )
                    NotificationDetailRow(
                        icon = Icons.Default.Info,
                        label = "Type",
                        value = notification.type ?: "General",
                        colorScheme = colorScheme
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onArchive,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Archive", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    Text("Close", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun NotificationDetailRow(
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
        Text(
            label,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            ":",
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ===================== STATUS CONFIG =====================

data class NotificationStatusConfig(
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val label: String,
    val badgeColor: Color,
    val badgeTextColor: Color
)

fun getStatusConfig(item: InAppNotificationResponse): NotificationStatusConfig {
    val isUnread = item.status.equals("UNREAD", true)
    val type = item.type?.uppercase(Locale.getDefault()) ?: "GENERAL"

    return when {
        isUnread -> NotificationStatusConfig(
            icon = Icons.Default.Notifications,
            iconColor = Color(0xFF1D4ED8),
            bgColor = Color(0xFF1D4ED8).copy(alpha = 0.12f),
            label = "UNREAD",
            badgeColor = Color(0xFF1D4ED8).copy(alpha = 0.12f),
            badgeTextColor = Color(0xFF1D4ED8)
        )
        type.contains("DISPUTE") -> NotificationStatusConfig(
            icon = Icons.Default.Warning,
            iconColor = Color(0xFFDC2626),
            bgColor = Color(0xFFDC2626).copy(alpha = 0.12f),
            label = "DISPUTE",
            badgeColor = Color(0xFFDC2626).copy(alpha = 0.12f),
            badgeTextColor = Color(0xFFDC2626)
        )
        type.contains("PAYMENT") -> NotificationStatusConfig(
            icon = Icons.Default.Payments,
            iconColor = Color(0xFF10B981),
            bgColor = Color(0xFF10B981).copy(alpha = 0.12f),
            label = "PAYMENT",
            badgeColor = Color(0xFF10B981).copy(alpha = 0.12f),
            badgeTextColor = Color(0xFF10B981)
        )
        type.contains("ORDER") -> NotificationStatusConfig(
            icon = Icons.Default.ShoppingBag,
            iconColor = Color(0xFF7C3AED),
            bgColor = Color(0xFF7C3AED).copy(alpha = 0.12f),
            label = "ORDER",
            badgeColor = Color(0xFF7C3AED).copy(alpha = 0.12f),
            badgeTextColor = Color(0xFF7C3AED)
        )
        else -> NotificationStatusConfig(
            icon = Icons.Default.Info,
            iconColor = Color(0xFF3B82F6),
            bgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
            label = "INFO",
            badgeColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
            badgeTextColor = Color(0xFF3B82F6)
        )
    }
}

// ===================== HELPER FUNCTIONS =====================

private fun formatNotificationDate(value: String): String {
    return try {
        val timestamp = OffsetDateTime.parse(value)
        val now = OffsetDateTime.now()
        val duration = Duration.between(timestamp, now)

        when {
            duration.isNegative || duration.seconds < 60 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            duration.toDays() == 1L -> "Yesterday"
            else -> timestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault()))
        }
    } catch (_: Exception) {
        value
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SellerNotificationsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        SellerNotificationsScreen(onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SellerNotificationsScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        SellerNotificationsScreen(onBack = {})
    }
}

private fun openNotificationDeepLink(
    context: android.content.Context,
    session: SessionManager,
    item: InAppNotificationResponse
): Boolean {
    val payload = item.payloadJson.orEmpty()
    val transactionId = payload["transactionId"]?.toString()?.takeIf { it.isNotBlank() }
        ?: item.referenceId?.takeIf { it.isNotBlank() }
        ?: return false
    val status = payload["status"]?.toString()?.ifBlank { null } ?: "FUNDS_HELD"

    val role = session.getUserRole()?.uppercase(Locale.getDefault()) ?: "BUYER"
    val intent = when (role) {
        "SELLER" -> Intent(context, SellerTransactionDetailActivity::class.java).apply {
            putExtra("TRANSACTION_ID", transactionId)
            putExtra("STATUS", status)
            putExtra("CURRENT_STEP", sellerStepForNotificationStatus(status))
        }
        "RIDER" -> Intent(context, RiderAssignmentDetailsActivity::class.java).apply {
            putExtra(RiderAssignmentDetailsActivity.EXTRA_TRANSACTION_ID, transactionId)
        }
        else -> Intent(context, BuyerTransactionDetailActivity::class.java).apply {
            putExtra("TRANSACTION_ID", transactionId)
            putExtra("STATUS", status)
        }
    }
    context.startActivity(intent)
    return true
}

private fun sellerStepForNotificationStatus(statusRaw: String): Int {
    return when (statusRaw.trim().uppercase(Locale.getDefault())) {
        "CREATED", "PENDING_PAYMENT", "FUNDS_HELD" -> 1
        "SELLER_ACCEPTED", "IN_DELIVERY", "SELLER_DELIVERED" -> 2
        "BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING", "RELEASED", "COMPLETED" -> 3
        "DECLINED", "CANCELLED", "DISPUTED", "RELEASE_FAILED" -> 3
        else -> 1
    }
}