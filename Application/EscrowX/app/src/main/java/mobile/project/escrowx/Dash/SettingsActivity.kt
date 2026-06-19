package mobile.project.escrowx.dash

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.LoginActivity
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.ui.components.BuyerNavBar
import mobile.project.escrowx.ui.components.BuyerNavItem
import mobile.project.escrowx.ui.components.SellerNavBar
import mobile.project.escrowx.ui.components.SellerNavItem
import mobile.project.escrowx.ui.components.navigateTab
import mobile.project.escrowx.ui.theme.BrandBlue

object SettingsKeys {
    const val PUSH_NOTIFICATIONS = "push_notifications"
    const val EMAIL_NOTIFICATIONS = "email_notifications"
    const val SMS_ALERTS = "sms_alerts"
    const val APP_LANGUAGE = "app_language"
}

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
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
    val colorScheme = MaterialTheme.colorScheme

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }

    var darkModeEnabled by remember {
        mutableStateOf(ThemePreferenceManager.isDarkModeEnabled(context))
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
    var showAboutDialog by remember { mutableStateOf(false) }

    val displayName: String = if (userProfile?.displayName?.isNotBlank() == true) {
        userProfile?.displayName ?: "User"
    } else {
        userProfile?.email?.substringBefore("@") ?: "User"
    }
    val role = userProfile?.role ?: "BUYER"
    val accountNumber = userProfile?.id?.takeLast(6) ?: "4829"
    val userInitials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "U" }

    fun handleLogout() {
        session.clearSession()
        prefs.edit().clear().apply()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(context, LoginActivity::class.java))
        (context as? SettingsActivity)?.finishAffinity()
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? SettingsActivity)?.finish() }) {
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
        },
        bottomBar = { SettingsBottomNavigation() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ===== PROFILE HEADER =====
            ProfileHeaderCard(
                displayName = displayName,
                role = role,
                accountNumber = accountNumber,
                userInitials = userInitials,
                onProfileClick = {
                    context.startActivity(Intent(context, ProfileActivity::class.java))
                },
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ===== GENERAL PREFERENCES =====
            SectionHeader(title = "General Preferences", colorScheme = colorScheme)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsMenuItem(
                    icon = Icons.Default.Language,
                    title = "App Language",
                    trailing = selectedLanguage,
                    showChevron = true,
                    onClick = { showLanguageDialog = true },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    checked = darkModeEnabled,
                    onCheckedChange = { enabled ->
                        if (darkModeEnabled == enabled) return@SettingsSwitchItem
                        darkModeEnabled = enabled
                        ThemePreferenceManager.setDarkModeEnabled(context.applicationContext, enabled)
                        (context as? SettingsActivity)?.recreate()
                    },
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== NOTIFICATIONS =====
            SectionHeader(title = "Notifications", colorScheme = colorScheme)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    checked = pushNotificationsEnabled,
                    onCheckedChange = { enabled -> pushNotificationsEnabled = enabled },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsSwitchItem(
                    icon = Icons.Default.Email,
                    title = "Email Notifications",
                    checked = emailNotificationsEnabled,
                    onCheckedChange = { enabled -> emailNotificationsEnabled = enabled },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsSwitchItem(
                    icon = Icons.Default.Sms,
                    title = "SMS Alerts",
                    checked = smsAlertsEnabled,
                    onCheckedChange = { enabled -> smsAlertsEnabled = enabled },
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== ACCOUNT & SECURITY =====
            SectionHeader(title = "Account & Security", colorScheme = colorScheme)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsMenuItem(
                    icon = Icons.Default.Person,
                    title = "Profile Settings",
                    onClick = {
                        context.startActivity(Intent(context, ProfileActivity::class.java))
                    },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsMenuItem(
                    icon = Icons.Default.VerifiedUser,
                    title = "KYC Status",
                    trailing = "Verified",
                    trailingColor = Color(0xFF10B981),
                    onClick = {
                        Toast.makeText(context, "KYC verification complete", Toast.LENGTH_SHORT).show()
                    },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    onClick = {
                        val userEmail = session.getEmail()
                        if (!userEmail.isNullOrBlank()) {
                            val intent = Intent(context, mobile.project.escrowx.auth.ChangePasswordVerificationActivity::class.java)
                            intent.putExtra("EMAIL", userEmail)
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsMenuItem(
                    icon = Icons.Default.Security,
                    title = "Two-Factor Authentication",
                    trailing = "Enabled",
                    trailingColor = Color(0xFF10B981),
                    onClick = {
                        Toast.makeText(context, "2FA coming soon", Toast.LENGTH_SHORT).show()
                    },
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== SUPPORT =====
            SectionHeader(title = "Support", colorScheme = colorScheme)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsMenuItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help Center",
                    onClick = { showHelpCenterDialog = true },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsMenuItem(
                    icon = Icons.AutoMirrored.Filled.ContactSupport,
                    title = "Contact Us",
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@escrowx.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Support Request - EscrowX")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send email"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsMenuItem(
                    icon = Icons.Default.Quiz,
                    title = "FAQs",
                    onClick = {
                        Toast.makeText(
                            context,
                            "FAQs:\n\n• How does escrow work?\n• What are the fees?\n• How to raise a dispute?\n• How long does payment take?\n• Is EscrowX secure?",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== LEGAL =====
            SectionHeader(title = "Legal", colorScheme = colorScheme)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsMenuItem(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    onClick = { showPrivacyPolicyDialog = true },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsMenuItem(
                    icon = Icons.Default.Gavel,
                    title = "Terms of Service",
                    onClick = { showTermsDialog = true },
                    colorScheme = colorScheme
                )
                SettingsDivider(colorScheme)
                SettingsMenuItem(
                    icon = Icons.Default.Info,
                    title = "About EscrowX",
                    trailing = "v1.4.2",
                    onClick = { showAboutDialog = true },
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== LOGOUT BUTTON =====
            LogoutButton(
                onLogout = { handleLogout() },
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ===== DIALOGS =====

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                    Text(
                        "Select Language",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("English", "Swahili", "French").forEach { language ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguage = language
                                    prefs.edit().putString(SettingsKeys.APP_LANGUAGE, language).apply()
                                    Toast.makeText(context, "Language changed to $language", Toast.LENGTH_SHORT).show()
                                    showLanguageDialog = false
                                }
                                .padding(12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedLanguage == language)
                                colorScheme.primary.copy(alpha = 0.08f)
                            else
                                Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    language,
                                    fontSize = 14.sp,
                                    color = if (selectedLanguage == language)
                                        colorScheme.primary
                                    else
                                        colorScheme.onSurface
                                )
                                if (selectedLanguage == language) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Help Center Dialog
    if (showHelpCenterDialog) {
        AlertDialog(
            onDismissRequest = { showHelpCenterDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Help,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                    Text(
                        "Help Center",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HelpItem(text = "How to create an escrow?")
                    HelpItem(text = "How to release funds?")
                    HelpItem(text = "How to raise a dispute?")
                    HelpItem(text = "Verification process")
                    HelpItem(text = "Supported payment methods")
                    HelpItem(text = "Escrow fees explained")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelpCenterDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Policy,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                    Text(
                        "Privacy Policy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "EscrowX Privacy Policy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    PolicySection(
                        title = "1. Information We Collect",
                        content = "• Personal information (name, email, phone)\n• Transaction details\n• Payment information"
                    )
                    PolicySection(
                        title = "2. How We Use Your Information",
                        content = "• Process escrow transactions\n• Verify your identity\n• Send notifications\n• Improve our services"
                    )
                    PolicySection(
                        title = "3. Data Security",
                        content = "• Encrypted data transmission\n• Secure servers\n• Regular security audits"
                    )
                    PolicySection(
                        title = "4. Data Sharing",
                        content = "• We do not sell your data\n• Shared only with transaction parties"
                    )
                    PolicySection(
                        title = "5. Contact Us",
                        content = "• Email: privacy@escrowx.com\n• Phone: +254 700 000 000"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                    Text(
                        "Terms of Service",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "EscrowX Terms of Service",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    PolicySection(
                        title = "1. Escrow Service",
                        content = "• Funds are held securely until both parties agree\n• Disputes are resolved within 48-72 hours\n• Escrow fee is 1.5% of transaction amount"
                    )
                    PolicySection(
                        title = "2. Buyer Responsibilities",
                        content = "• Verify items before releasing funds\n• Raise disputes within inspection period"
                    )
                    PolicySection(
                        title = "3. Seller Responsibilities",
                        content = "• Deliver items as described\n• Provide tracking information"
                    )
                    PolicySection(
                        title = "4. Dispute Resolution",
                        content = "• Both parties must provide evidence\n• EscrowX mediates fairly\n• Decision is final"
                    )
                    PolicySection(
                        title = "5. Fees & Charges",
                        content = "• Escrow fee: 1.5% of transaction value\n• Refund fee: KES 100 for cancelled transactions"
                    )
                    PolicySection(
                        title = "6. Account Termination",
                        content = "• Violation of terms may lead to suspension\n• Fraudulent activity reported to authorities"
                    )
                    PolicySection(
                        title = "7. Liability",
                        content = "• EscrowX is not responsible for item quality\n• We facilitate secure payment only"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                    Text(
                        "About EscrowX",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            "E",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        "EscrowX",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        "Version 1.4.2",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Secure mobile escrow for safer transactions.\nProtecting buyers and sellers since 2024.",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "🔒 Secure",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            "⚡ Fast",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            "💪 Reliable",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }
}

// ===================== COMPOSABLE COMPONENTS =====================

@Composable
fun ProfileHeaderCard(
    displayName: String,
    role: String,
    accountNumber: String,
    userInitials: String,
    onProfileClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() },
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colorScheme.primary,
                                colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    userInitials,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            role,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        "·",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Account #$accountNumber",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Edit Profile",
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    colorScheme: ColorScheme
) {
    Text(
        title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp
    )
}

@Composable
fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    trailing: String? = null,
    trailingColor: Color? = null,
    showChevron: Boolean = false,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant
            )
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            trailing?.let {
                Text(
                    it,
                    fontSize = 13.sp,
                    color = trailingColor ?: colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            if (showChevron) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant
            )
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun SettingsDivider(colorScheme: ColorScheme) {
    HorizontalDivider(
        color = colorScheme.outlineVariant.copy(alpha = 0.2f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun LogoutButton(
    onLogout: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        border = BorderStroke(
            1.dp,
            colorScheme.error.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Logout",
                modifier = Modifier.size(20.dp),
                tint = colorScheme.error
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Logout",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.error
            )
        }
    }
}

@Composable
fun HelpItem(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colorScheme.primary)
        )
        Text(
            text,
            fontSize = 14.sp,
            color = colorScheme.onSurface
        )
    }
}

@Composable
fun PolicySection(
    title: String,
    content: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
        Text(
            content,
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun SettingsBottomNavigation() {
    val context = LocalContext.current
    val role = SessionManager(context).getUserRole() ?: "BUYER"
    if (role.equals("SELLER", ignoreCase = true)) {
        SellerNavBar(
            selectedIndex = SellerNavItem.Profile.index,
            onItemSelected = { item ->
                when (item) {
                    SellerNavItem.Home -> navigateTab(context, SellerDashboardActivity::class.java)
                    SellerNavItem.Transactions -> {
                        navigateTab(
                            context,
                            TransactionsActivity::class.java,
                            Bundle().apply { putString("ROLE", "SELLER") }
                        )
                    }
                    SellerNavItem.Profile -> navigateTab(context, ProfileActivity::class.java)
                }
            }
        )
    } else {
        BuyerNavBar(
            selectedIndex = BuyerNavItem.Profile.index,
            onItemSelected = { item ->
                when (item) {
                    BuyerNavItem.Home -> navigateTab(context, BuyerDashboardActivity::class.java)
                    BuyerNavItem.Transactions -> {
                        navigateTab(
                            context,
                            TransactionsActivity::class.java,
                            Bundle().apply { putString("ROLE", "BUYER") }
                        )
                    }
                    BuyerNavItem.Profile -> navigateTab(context, ProfileActivity::class.java)
                }
            }
        )
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SettingsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        SettingsScreen()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SettingsScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        SettingsScreen()
    }
}