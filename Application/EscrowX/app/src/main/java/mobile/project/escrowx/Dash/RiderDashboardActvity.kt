package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.EscrowResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.InAppNotificationResponse
import mobile.project.escrowx.auth.LoginActivity
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.components.RiderNavBar
import mobile.project.escrowx.ui.components.RiderNavItem
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

private data class RiderAssignmentItem(
    val notification: InAppNotificationResponse,
    val transaction: EscrowResponse?
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var assignments by remember { mutableStateOf<List<RiderAssignmentItem>>(emptyList()) }
    var newAssignmentsCount by remember { mutableIntStateOf(0) }

    fun loadRiderData() {
        val token = session.getAccessToken()
        val email = session.getEmail()
        val userId = session.getUserId()
        if (token.isNullOrBlank() || email.isNullOrBlank() || userId.isNullOrBlank()) {
            isLoading = false
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.authenticated(token)

                val userResponse = api.getUserByEmail(email)
                if (userResponse.isSuccessful) {
                    profile = userResponse.body()
                }

                val notificationResponse = api.getNotifications(
                    actorUserId = userId,
                    page = 0,
                    size = 100,
                    status = "ACTIVE"
                )

                val notificationContent = notificationResponse.body()?.content.orEmpty()
                val assignmentNotifications = notificationContent.filter {
                    val type = it.type.uppercase()
                    val title = it.title.uppercase()
                    type.contains("ASSIGN") || title.contains("ASSIGN") ||
                        (it.referenceType?.equals("TRANSACTION", ignoreCase = true) == true)
                }

                newAssignmentsCount = assignmentNotifications.count { it.status.equals("UNREAD", ignoreCase = true) }

                val transactionIds = assignmentNotifications
                    .mapNotNull { it.referenceId }
                    .distinct()

                val transactionMap = transactionIds.map { txnId ->
                    async {
                        val txn = try {
                            val txnResponse = api.getTransactionById(txnId)
                            if (txnResponse.isSuccessful) txnResponse.body() else null
                        } catch (_: Exception) {
                            null
                        }
                        txnId to txn
                    }
                }.awaitAll().toMap()

                assignments = assignmentNotifications
                    .sortedByDescending { it.createdAt }
                    .map { note ->
                        RiderAssignmentItem(
                            notification = note,
                            transaction = note.referenceId?.let { transactionMap[it] }
                        )
                    }
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
        loadRiderData()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rider Workspace",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        loadRiderData()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colorScheme.primary)
                    }
                    IconButton(onClick = {
                        session.clearSession()
                        context.startActivity(Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = colorScheme.primary)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            }
            selectedTab == RiderNavItem.Home.index -> {
                RiderHomeTab(
                    padding = padding,
                    riderName = profile?.displayName?.ifBlank { null }
                        ?: profile?.email?.substringBefore("@")
                        ?: "Rider",
                    newAssignmentsCount = newAssignmentsCount,
                    assignments = assignments
                )
            }
            selectedTab == RiderNavItem.Assignments.index -> {
                RiderAssignmentsTab(
                    padding = padding,
                    assignments = assignments
                )
            }
            else -> {
                RiderProfileTab(
                    padding = padding,
                    profile = profile,
                    onOpenProfile = {
                        context.startActivity(Intent(context, RiderProfileDetailsActivity::class.java))
                    },
                    onOpenRiderProfile = {
                        context.startActivity(Intent(context, RiderProfileDetailsActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun RiderHomeTab(
    padding: PaddingValues,
    riderName: String,
    newAssignmentsCount: Int,
    assignments: List<RiderAssignmentItem>
) {
    val colorScheme = MaterialTheme.colorScheme
    val activeCount = assignments.count {
        val status = it.transaction?.status?.uppercase() ?: ""
        status == "SELLER_ACCEPTED" || status == "IN_DELIVERY"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Welcome, $riderName",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "New Assignments",
                    value = newAssignmentsCount.toString(),
                    icon = Icons.Default.Assignment,
                    tint = Color(0xFF2563EB)
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Deliveries",
                    value = activeCount.toString(),
                    icon = Icons.Default.LocalShipping,
                    tint = Color(0xFF10B981)
                )
            }
        }

        item {
            Text(
                text = "Current assignment status",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
        }

        val previewItems = assignments.take(3)
        if (previewItems.isEmpty()) {
            item {
                EmptyStateCard("No assignments yet. New delivery assignments will appear here.")
            }
        } else {
            items(previewItems) { item ->
                AssignmentCard(item)
            }
        }
    }
}

@Composable
private fun RiderAssignmentsTab(
    padding: PaddingValues,
    assignments: List<RiderAssignmentItem>
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Assigned Deliveries",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        }

        if (assignments.isEmpty()) {
            item {
                EmptyStateCard("No assigned deliveries found.")
            }
        } else {
            items(assignments) { item ->
                AssignmentCard(item)
            }
        }
    }
}

@Composable
private fun RiderProfileTab(
    padding: PaddingValues,
    profile: UserDetailsResponse?,
    onOpenProfile: () -> Unit,
    onOpenRiderProfile: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Rider Profile",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Name: ${profile?.displayName ?: "-"}", color = colorScheme.onSurface)
                Text("Email: ${profile?.email ?: "-"}", color = colorScheme.onSurfaceVariant)
                Text("Role: ${profile?.role ?: "RIDER"}", color = colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = onOpenProfile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Text(" Open Profile DB")
        }

        Button(
            onClick = onOpenRiderProfile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Badge, contentDescription = null)
            Text(" Open Rider Profile DB")
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint)
                Spacer(modifier = Modifier.size(6.dp))
                Text(title, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
        }
    }
}

@Composable
private fun AssignmentCard(item: RiderAssignmentItem) {
    val colorScheme = MaterialTheme.colorScheme
    val status = item.transaction?.status ?: "ASSIGNED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.transaction?.title ?: item.notification.title,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = item.transaction?.deliveryAddress ?: item.notification.body,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkHistory, contentDescription = null, tint = colorScheme.primary)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(status, fontSize = 12.sp, color = colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = item.transaction?.updatedAt ?: item.notification.createdAt,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(message, color = colorScheme.onSurfaceVariant)
        }
    }
}
