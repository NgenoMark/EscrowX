package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
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

object SettingsKeys {
    const val DARK_MODE = "dark_mode"
    const val PUSH_NOTIFICATIONS = "push_notifications"
    const val EMAIL_NOTIFICATIONS = "email_notifications"
    const val SMS_ALERTS = "sms_alerts"
    const val APP_LANGUAGE = "app_language"
}

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
    val prefs = context.getSharedPreferences("escrowx_settings", android.content.Context.MODE_PRIVATE)

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }

    var darkModeEnabled by remember {
        mutableStateOf(prefs.getBoolean(SettingsKeys.DARK_MODE, false))
    }
    var pushNotificationsEnabled by remember {
        mutableStateOf(prefs.getBoolean(SettingsKeys.PUSH_NOTIFICATIONS, true))
    }
    var emailNotificationsEnabled by remember {
        mutableStateOf(prefs.getBoolean(SettingsKeys.EMAIL_NOTIFICATIONS, true))
    }
    var smsAlertsEnabled by remember {
        mutableStateOf(prefs.getBoolean(SettingsKeys.SMS_ALERTS, false))
    }
    var selectedLanguage by remember {
        mutableStateOf(prefs.getString(SettingsKeys.APP_LANGUAGE, "English") ?: "English")
    }

    // Apply Dark Mode
    LaunchedEffect(darkModeEnabled) {
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        prefs.edit().putBoolean(SettingsKeys.DARK_MODE, darkModeEnabled).apply()
    }

    // Save notification settings
    LaunchedEffect(pushNotificationsEnabled) {
        prefs.edit().putBoolean(SettingsKeys.PUSH_NOTIFICATIONS, pushNotificationsEnabled).apply()
    }
    LaunchedEffect(emailNotificationsEnabled) {
        prefs.edit().putBoolean(SettingsKeys.EMAIL_NOTIFICATIONS, emailNotificationsEnabled).apply()
    }
    LaunchedEffect(smsAlertsEnabled) {
        prefs.edit().putBoolean(SettingsKeys.SMS_ALERTS, smsAlertsEnabled).apply()
    }

    // Fetch user profile
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
                } catch (_: Exception) { }
            }
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showHelpCenterDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    fun handleLogout() {
        session.clearSession()
        prefs.edit().clear().apply()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(context, LoginActivity::class.java))
        (context as? SettingsActivity)?.finishAffinity()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00236F)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? SettingsActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FF))
            )
        },
        bottomBar = { SettingsBottomNavigation() }
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
                onDarkModeToggle = { darkModeEnabled = !darkModeEnabled },
                selectedLanguage = selectedLanguage,
                onLanguageClick = { showLanguageDialog = true }
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
            SupportSection(onHelpCenterClick = { showHelpCenterDialog = true })
            Spacer(modifier = Modifier.height(24.dp))
            LegalSection(
                onPrivacyPolicyClick = { showPrivacyPolicyDialog = true },
                onTermsClick = { showTermsDialog = true }
            )
            Spacer(modifier = Modifier.height(24.dp))
            LogoutButton(onLogout = { handleLogout() })
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    listOf("English", "Swahili", "French").forEach { language ->
                        TextButton(
                            onClick = {
                                selectedLanguage = language
                                prefs.edit().putString(SettingsKeys.APP_LANGUAGE, language).apply()
                                Toast.makeText(context, "Language changed to $language", Toast.LENGTH_SHORT).show()
                                showLanguageDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(language, modifier = Modifier.padding(8.dp)) }
                        if (language != "French") HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") } }
        )
    }

    // Help Center Dialog
    if (showHelpCenterDialog) {
        AlertDialog(
            onDismissRequest = { showHelpCenterDialog = false },
            title = { Text("📖 Help Center") },
            text = {
                Column {
                    Text("• How to create an escrow?", modifier = Modifier.padding(8.dp))
                    Text("• How to release funds?", modifier = Modifier.padding(8.dp))
                    Text("• How to raise a dispute?", modifier = Modifier.padding(8.dp))
                    Text("• Verification process", modifier = Modifier.padding(8.dp))
                    Text("• Supported payment methods", modifier = Modifier.padding(8.dp))
                    Text("• Escrow fees explained", modifier = Modifier.padding(8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { showHelpCenterDialog = false }) { Text("Close") } }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "EscrowX Privacy Policy\n\n" +
                                "1. Information We Collect\n" +
                                "   • Personal information (name, email, phone)\n" +
                                "   • Transaction details\n" +
                                "   • Payment information\n\n" +
                                "2. How We Use Your Information\n" +
                                "   • Process escrow transactions\n" +
                                "   • Verify your identity\n" +
                                "   • Send notifications\n" +
                                "   • Improve our services\n\n" +
                                "3. Data Security\n" +
                                "   • Encrypted data transmission\n" +
                                "   • Secure servers\n" +
                                "   • Regular security audits\n\n" +
                                "4. Data Sharing\n" +
                                "   • We do not sell your data\n" +
                                "   • Shared only with transaction parties\n\n" +
                                "5. Contact Us\n" +
                                "   • Email: privacy@escrowx.com\n" +
                                "   • Phone: +254 700 000 000",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacyPolicyDialog = false }) { Text("Close") } }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "EscrowX Terms of Service\n\n" +
                                "1. Escrow Service\n" +
                                "   • Funds are held securely until both parties agree\n" +
                                "   • Disputes are resolved within 48-72 hours\n" +
                                "   • Escrow fee is 1.5% of transaction amount\n\n" +
                                "2. Buyer Responsibilities\n" +
                                "   • Verify items before releasing funds\n" +
                                "   • Raise disputes within inspection period\n\n" +
                                "3. Seller Responsibilities\n" +
                                "   • Deliver items as described\n" +
                                "   • Provide tracking information\n\n" +
                                "4. Dispute Resolution\n" +
                                "   • Both parties must provide evidence\n" +
                                "   • EscrowX mediates fairly\n" +
                                "   • Decision is final\n\n" +
                                "5. Fees & Charges\n" +
                                "   • Escrow fee: 1.5% of transaction value\n" +
                                "   • Refund fee: KES 100 for cancelled transactions\n\n" +
                                "6. Account Termination\n" +
                                "   • Violation of terms may lead to suspension\n" +
                                "   • Fraudulent activity reported to authorities\n\n" +
                                "7. Liability\n" +
                                "   • EscrowX is not responsible for item quality\n" +
                                "   • We facilitate secure payment only",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showTermsDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun ProfileHeaderSection(userProfile: UserDetailsResponse?) {
    val context = LocalContext.current
    val displayName = if (userProfile?.displayName?.isNotBlank() == true) userProfile.displayName
    else userProfile?.email?.substringBefore("@") ?: "User"
    val role = userProfile?.role ?: "BUYER"
    val accountNumber = userProfile?.id?.takeLast(6) ?: "4829"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { context.startActivity(Intent(context, ProfileActivity::class.java)) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF1E3A8A)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                Text("$role Account #$accountNumber", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
            }
        }
    }
}

@Composable
fun GeneralPreferencesSection(
    darkModeEnabled: Boolean,
    onDarkModeToggle: () -> Unit,
    selectedLanguage: String,
    onLanguageClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("GENERAL PREFERENCES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757682), letterSpacing = 0.5.sp)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clickable { onLanguageClick() }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = "Language", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("App Language", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedLanguage, fontSize = 12.sp, color = Color(0xFF00236F), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = "Select", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = "Dark Mode", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Dark Mode", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(checked = darkModeEnabled, onCheckedChange = { onDarkModeToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFC5C5D3)))
                }
            }
        }
    }
}

@Composable
fun NotificationsSection(
    pushEnabled: Boolean, emailEnabled: Boolean, smsEnabled: Boolean,
    onPushToggle: () -> Unit, onEmailToggle: () -> Unit, onSmsToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("NOTIFICATIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757682), letterSpacing = 0.5.sp)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = "Push", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Push Notifications", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(checked = pushEnabled, onCheckedChange = { onPushToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFC5C5D3)))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Email Notifications", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(checked = emailEnabled, onCheckedChange = { onEmailToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFC5C5D3)))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sms, contentDescription = "SMS", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("SMS Alerts", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Switch(checked = smsEnabled, onCheckedChange = { onSmsToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00236F),
                            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFC5C5D3)))
                }
            }
        }
    }
}

@Composable
fun AccountSecuritySection() {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ACCOUNT & SECURITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757682), letterSpacing = 0.5.sp)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clickable { context.startActivity(Intent(context, ProfileActivity::class.java)) }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "Profile", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Profile Settings", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFFC5C5D3), modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "KYC", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("KYC Status", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF6CF8BB).copy(alpha = 0.2f)) {
                        Text("Verified", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00714D))
                    }
                }
            }
        }
    }
}

@Composable
fun SupportSection(onHelpCenterClick: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SUPPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757682), letterSpacing = 0.5.sp)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clickable { onHelpCenterClick() }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Help Center", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = Color(0xFFC5C5D3), modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@escrowx.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Support Request - EscrowX")
                    }
                    try { context.startActivity(Intent.createChooser(emailIntent, "Send email")) }
                    catch (e: Exception) { Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show() }
                }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ContactSupport, contentDescription = "Contact", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Contact Us", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    Toast.makeText(context, "FAQs:\n\n• How does escrow work?\n• What are the fees?\n• How to raise a dispute?\n• How long does payment take?\n• Is EscrowX secure?", Toast.LENGTH_LONG).show()
                }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Quiz, contentDescription = "FAQ", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("FAQs", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun LegalSection(onPrivacyPolicyClick: () -> Unit, onTermsClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("LEGAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757682), letterSpacing = 0.5.sp)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clickable { onPrivacyPolicyClick() }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Policy, contentDescription = "Privacy", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Privacy Policy", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().clickable { onTermsClick() }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = "Terms", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Terms of Service", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Version", tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("App Version", fontSize = 14.sp, color = Color(0xFF151C27))
                    }
                    Text("v1.4.2", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00236F))
                }
            }
        }
    }
}

@Composable
fun LogoutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFBA1A1A)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingsBottomNavigation() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(2) }
    NavigationBar(modifier = Modifier.height(64.dp), containerColor = Color(0xFFF9F9FF), tonalElevation = 0.dp) {
        NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0; context.startActivity(Intent(context, BuyerDashboardActivity::class.java)) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) }, label = { Text("Home", fontSize = 11.sp) })
        NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; context.startActivity(Intent(context, TransactionsActivity::class.java)) },
            icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Transactions", modifier = Modifier.size(24.dp)) }, label = { Text("Transactions", fontSize = 11.sp) })
        NavigationBarItem(selected = true, onClick = { selectedTab = 2; context.startActivity(Intent(context, ProfileActivity::class.java)) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) }, label = { Text("Profile", fontSize = 11.sp) })
    }
}