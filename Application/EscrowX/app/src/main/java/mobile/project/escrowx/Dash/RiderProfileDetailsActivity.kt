package mobile.project.escrowx.dash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import mobile.project.escrowx.ui.theme.BrandBlue
import java.text.SimpleDateFormat
import java.util.*

class RiderProfileDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                RiderProfileDetailsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderProfileDetailsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val session = remember { SessionManager(context) }

    var profile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = session.getAccessToken()
        val email = session.getEmail()
        if (!token.isNullOrBlank() && !email.isNullOrBlank()) {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.authenticated(token).getUserByEmail(email)
                }
                if (response.isSuccessful) {
                    profile = response.body()
                } else {
                    errorMessage = "Failed to load profile"
                }
            } catch (_: Exception) {
                errorMessage = "Network error"
            }
        } else {
            errorMessage = "Not logged in"
        }
        isLoading = false
    }

    val riderName = profile?.displayName?.ifBlank { null }
        ?: profile?.email?.substringBefore("@")
        ?: "Rider"
    val riderInitials = riderName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(Locale.getDefault()) }
        .ifBlank { "RD" }
    val isActive = profile?.status.equals("ACTIVE", ignoreCase = true) ?: true
    val formattedDate = profile?.createdAt?.let { formatRiderProfileDate(it) } ?: "N/A"

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rider Profile",
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
                    IconButton(onClick = {
                        // Refresh profile
                    }) {
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                        "Loading profile...",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        errorMessage!!,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== PROFILE HEADER CARD =====
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar with gradient
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        BrandBlue,
                                        BrandBlue.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            riderInitials,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Name & Role
                    Text(
                        riderName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isActive)
                            Color(0xFF10B981).copy(alpha = 0.12f)
                        else
                            Color(0xFFDC2626).copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color(0xFF10B981) else Color(0xFFDC2626))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isActive) "Active" else "Inactive",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) Color(0xFF10B981) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // ===== PROFILE INFORMATION =====
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Profile Information",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileDetailRow(
                        icon = Icons.Default.Badge,
                        label = "Role",
                        value = profile?.role ?: "RIDER",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Shield,
                        label = "Status",
                        value = profile?.status ?: "ACTIVE",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Person,
                        label = "Display Name",
                        value = profile?.displayName ?: "-",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = profile?.email ?: "-",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = profile?.phone ?: "Not set",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Business,
                        label = "Business Name",
                        value = profile?.businessName ?: "Not set",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Address",
                        value = profile?.address ?: "Not set",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Schedule,
                        label = "Member Since",
                        value = formattedDate,
                        colorScheme = colorScheme
                    )
                }
            }

            // ===== ACCOUNT STATS =====
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        "Account Statistics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = "Total Deliveries",
                            value = "0",
                            icon = Icons.Default.LocalShipping,
                            color = Color(0xFF3B82F6),
                            colorScheme = colorScheme
                        )
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = "Completed",
                            value = "0",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF10B981),
                            colorScheme = colorScheme
                        )
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = "Active",
                            value = "0",
                            icon = Icons.Default.Pending,
                            color = Color(0xFFF59E0B),
                            colorScheme = colorScheme
                        )
                    }
                }
            }

            // ===== QUICK ACTIONS =====
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Quick Actions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    QuickActionItem(
                        icon = Icons.Default.Edit,
                        label = "Edit Profile",
                        onClick = { /* Navigate to edit profile */ },
                        colorScheme = colorScheme
                    )
                    QuickActionItem(
                        icon = Icons.Default.Notifications,
                        label = "Notifications",
                        onClick = { /* Navigate to notifications */ },
                        colorScheme = colorScheme
                    )
                    QuickActionItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { /* Navigate to settings */ },
                        colorScheme = colorScheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ===================== COMPONENTS =====================

@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colorScheme.primary
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
                fontSize = 14.sp,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Optional: Show a verified icon for certain fields
        if (label == "Email" && value != "-" && value != "Not set") {
            Icon(
                Icons.Default.Verified,
                contentDescription = "Verified",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun StatItem(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    colorScheme: ColorScheme
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                label,
                fontSize = 10.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant
            )
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===================== HELPER FUNCTIONS =====================

fun formatRiderProfileDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(dateString)
        if (date != null) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        } else {
            dateString
        }
    } catch (_: Exception) {
        dateString
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderProfileDetailsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        RiderProfileDetailsScreen(onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderProfileDetailsScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        RiderProfileDetailsScreen(onBack = {})
    }
}