package mobile.project.escrowx.auth

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.dash.RiderDashboardActvity
import mobile.project.escrowx.notifications.FcmTokenRegistrar
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

class SplashActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureNotificationPermission()

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                SplashScreenContent()
            }
        }

        lifecycleScope.launch {
            // Ensure splash is visible briefly before routing.
            delay(1200)
            val session = SessionManager(this@SplashActivity)
            val token = session.getAccessToken()
            val email = session.getEmail()

            if (token.isNullOrBlank() || email.isNullOrBlank()) {
                openLogin()
                return@launch
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.authenticated(token).getUserByEmail(email)
                }

                if (!response.isSuccessful || response.body() == null) {
                    session.clearSession()
                    openLogin()
                    return@launch
                }

                val user = response.body()!!
                val refreshToken = session.getRefreshToken().orEmpty()
                session.saveSession(
                    accessToken = token,
                    refreshToken = refreshToken,
                    email = user.email,
                    userId = user.id,
                    role = user.role
                )

                FcmTokenRegistrar.register(this@SplashActivity, user.id)

                when {
                    user.role.equals("BUYER", ignoreCase = true) -> openBuyerDashboard()
                    user.role.equals("SELLER", ignoreCase = true) -> openSellerDashboard()
                    user.role.equals("RIDER", ignoreCase = true) -> openRiderDashboard()
                    else -> openLogin()
                }
            } catch (_: Exception) {
                session.clearSession()
                openLogin()
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun openBuyerDashboard() {
        val intent = Intent(this, BuyerDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun openSellerDashboard() {
        val intent = Intent(this, SellerDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun openRiderDashboard() {
        val intent = Intent(this, RiderDashboardActvity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}

@Composable
private fun SplashScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.splashscreen_logo),
            contentDescription = "EscrowX splash",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}
