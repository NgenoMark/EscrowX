package mobile.project.escrowx.dash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val session = remember { SessionManager(context) }

    var profile by remember { mutableStateOf<UserDetailsResponse?>(null) }

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
                }
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rider Profile Details",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow("Role", profile?.role ?: "RIDER", Icons.Default.Badge)
            DetailRow("Status", profile?.status ?: "ACTIVE", Icons.Default.Shield)
            DetailRow("Display Name", profile?.displayName ?: "-", Icons.Default.Person)
            DetailRow("Email", profile?.email ?: "-", Icons.Default.Email)
            DetailRow("Phone", profile?.phone ?: "-", Icons.Default.Call)
            DetailRow("Account ID", profile?.id ?: "-", Icons.Default.Badge)
            DetailRow("Created", profile?.createdAt ?: "-", Icons.Default.Badge)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = colorScheme.primary)
                Text(label, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
        }
    }
}
