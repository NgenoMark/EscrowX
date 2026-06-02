package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.LoginActivity
import mobile.project.escrowx.auth.SessionManager

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }

    var darkModeEnabled by remember { mutableStateOf(false) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var emailNotificationsEnabled by remember { mutableStateOf(true) }
    var smsAlertsEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()

            if (!token.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        userProfile = response.body()
                    }
                } catch (_: Exception) {
                    // Ignore error
                }
            }
        }
    }

    fun handleLogout() {
        session.clearSession()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(context, LoginActivity::class.java))
        (context as? SettingsActivity)?.finishAffinity()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00236F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? SettingsActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00236F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9FF),
                    titleContentColor = Color(0xFF00236F)
                )
            )
        },
        bottomBar = {
            SettingsBottomNavigation()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ProfileHeaderSection(userProfile = userProfile)

            Spacer(modifier = Modifier.height(24.dp))

            GeneralPreferencesSection(
                darkModeEnabled = darkModeEnabled,
                onDarkModeToggle = { darkModeEnabled = !darkModeEnabled }
            )

            Spacer(modifier = Modifier.height(24.dp))

            NotificationsSection(
                pushEnabled = pushNotificationsEnabled,
                emailEnabled = emailNotificationsEnabled,
                smsEnabled = smsAlertsEnabled,
                onPushToggle = { pushNotificationsEnabled = !pushNotificationsEnabled },
                onEmailToggle = { emailNotificationsEnabled = !emailNotificationsEnabled },
                onSmsToggle = { smsAlertsEnabled = !smsAlertsEnabled }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AccountSecuritySection()

            Spacer(modifier = Modifier.height(24.dp))

            SupportSection()

            Spacer(modifier = Modifier.height(24.dp))

            LegalSection()

            Spacer(modifier = Modifier.height(24.dp))

            LogoutButton(onLogout = { handleLogout() })

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileHeaderSection(userProfile: UserDetailsResponse?) {
    val context = LocalContext.current
    val displayName = if (userProfile?.displayName?.isNotBlank() == true) {
        userProfile.displayName
    } else {
        userProfile?.email?.substringBefore("@") ?: "User"
    }
    val role = userProfile?.role ?: "BUYER"
    val accountNumber = userProfile?.id?.takeLast(6) ?: "4829"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(context, ProfileActivity::class.java))
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A8A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF151C27)
                )
                Text(
                    text = "$role Account #$accountNumber",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF444651)
                )
            }
        }
    }
}

@Composable
fun GeneralPreferencesSection(
    darkModeEnabled: Boolean,
    onDarkModeToggle: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "GENERAL PREFERENCES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757682),
            letterSpacing = 0.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Language selection coming soon", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = "Language",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("App Language", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("English", fontSize = 12.sp, color = Color(0xFF444651))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Select",
                            tint = Color(0xFFC5C5D3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Dark Mode", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { onDarkModeToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFC5C5D3)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsSection(
    pushEnabled: Boolean,
    emailEnabled: Boolean,
    smsEnabled: Boolean,
    onPushToggle: () -> Unit,
    onEmailToggle: () -> Unit,
    onSmsToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "NOTIFICATIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757682),
            letterSpacing = 0.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Push",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Push Notifications", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { onPushToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFC5C5D3)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Email Notifications", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(
                        checked = emailEnabled,
                        onCheckedChange = { onEmailToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFC5C5D3)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = "SMS",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("SMS Alerts", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(
                        checked = smsEnabled,
                        onCheckedChange = { onSmsToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFC5C5D3)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSecuritySection() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "ACCOUNT & SECURITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757682),
            letterSpacing = 0.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ManageAccounts,
                            contentDescription = "Profile",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Profile Settings", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color(0xFFC5C5D3),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = "KYC",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("KYC Status", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF6CF8BB).copy(alpha = 0.2f)
                    ) {
                        Text(
                            "Verified",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00714D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupportSection() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "SUPPORT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757682),
            letterSpacing = 0.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Help Center coming soon", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.Help,
                            contentDescription = "Help",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Help Center", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open",
                        tint = Color(0xFFC5C5D3),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Contact Us: support@escrowx.com", Toast.LENGTH_LONG).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ContactSupport,
                            contentDescription = "Contact",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Contact Us", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color(0xFFC5C5D3),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "FAQs coming soon", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Quiz,
                            contentDescription = "FAQ",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("FAQs", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color(0xFFC5C5D3),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LegalSection() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "LEGAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757682),
            letterSpacing = 0.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Privacy Policy coming soon", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Policy,
                            contentDescription = "Privacy",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Privacy Policy", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color(0xFFC5C5D3),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Terms of Service coming soon", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Gavel,
                            contentDescription = "Terms",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Terms of Service", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color(0xFFC5C5D3),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Version",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("App Version", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Text("v1.4.2", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                }
            }
        }
    }
}

@Composable
fun LogoutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFBA1A1A)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Logout",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingsBottomNavigation() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(2) }

    NavigationBar(
        modifier = Modifier.height(64.dp),
        containerColor = Color(0xFFF9F9FF),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = {
                selectedTab = 0
                context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
            label = { Text("Home", fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = {
                selectedTab = 1
                context.startActivity(Intent(context, TransactionsActivity::class.java))
            },
            icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Transactions", modifier = Modifier.size(24.dp)) },
            label = { Text("Transactions", fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = true,
            onClick = {
                selectedTab = 2
                context.startActivity(Intent(context, ProfileActivity::class.java))
            },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Profile", fontSize = 11.sp) }
        )
    }
}