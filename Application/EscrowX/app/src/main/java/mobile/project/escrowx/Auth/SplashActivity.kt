package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.R
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.seller.SellerDashboardActivity

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
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

                when {
                    user.role.equals("BUYER", ignoreCase = true) -> openBuyerDashboard()
                    user.role.equals("SELLER", ignoreCase = true) -> openSellerDashboard()
                    else -> openLogin()
                }
            } catch (_: Exception) {
                session.clearSession()
                openLogin()
            }
        }
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun openBuyerDashboard() {
        val intent = Intent(this, BuyerDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun openSellerDashboard() {
        val intent = Intent(this, SellerDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
