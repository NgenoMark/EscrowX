package mobile.project.escrowx

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobile.project.escrowx.auth.SessionManager

// --- Theme Definitions ---
private val EscrowXColors = lightColorScheme(
    primary = Color(0xFF00236F),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF151C27),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF151C27),
    surfaceVariant = Color(0xFFDCE2F3),
    onSurfaceVariant = Color(0xFF444651),
    outlineVariant = Color(0xFFC5C5D3)
)

private val EscrowXTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp)
)

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = EscrowXColors, typography = EscrowXTypography) {
                SettingsScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val userEmail = remember { session.getEmail() ?: "Buyer Account" }

    var biometricEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(true) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var darkThemeEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {

            Text("Account Profile", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Logged in as:", style = MaterialTheme.typography.bodySmall)
                    Text(userEmail, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Security Configuration", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            SettingsActionRow("Change Password", "Update credentials", Icons.Default.Lock) {
                Toast.makeText(context, "Route to Change Password", Toast.LENGTH_SHORT).show()
            }
            SettingsToggleRow("Biometric Auth", "Log in using fingerprint", Icons.Default.Fingerprint, biometricEnabled) { biometricEnabled = it }
            SettingsToggleRow("2FA", "Require secure SMS OTP", Icons.Default.Shield, twoFactorEnabled) { twoFactorEnabled = it }

            Spacer(Modifier.height(16.dp))

            Text("App Preferences", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            SettingsToggleRow("Push Notifications", "Escrow updates", Icons.Default.Notifications, pushNotificationsEnabled) { pushNotificationsEnabled = it }
            SettingsToggleRow("Dark Mode", "Toggle theme", Icons.Default.Palette, darkThemeEnabled) { darkThemeEnabled = it }
        }
    }
}

@Composable
fun SettingsActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}