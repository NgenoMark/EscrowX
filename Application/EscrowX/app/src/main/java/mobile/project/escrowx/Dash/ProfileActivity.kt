package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserProfileResponse
import mobile.project.escrowx.auth.LoginActivity
import mobile.project.escrowx.auth.SessionManager

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val session = SessionManager(this)
        val token = session.getAccessToken()
        val email = session.getEmail()

        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvPhone = findViewById<TextView>(R.id.tvProfilePhone)
        val tvRole = findViewById<TextView>(R.id.tvProfileRole)
        val tvStatus = findViewById<TextView>(R.id.tvProfileStatus)
        val tvCreated = findViewById<TextView>(R.id.tvProfileCreatedAt)
        val btnBack = findViewById<Button>(R.id.btnProfileBack)
        val btnLogout = findViewById<Button>(R.id.btnProfileLogout)

        btnBack.setOnClickListener { finish() }
        btnLogout.setOnClickListener {
            session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        if (token.isNullOrBlank() || email.isNullOrBlank()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.authenticated(token).getUserByEmail(email)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        bindProfile(response.body()!!, tvEmail, tvPhone, tvRole, tvStatus, tvCreated)
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to fetch profile", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindProfile(
        profile: UserProfileResponse,
        tvEmail: TextView,
        tvPhone: TextView,
        tvRole: TextView,
        tvStatus: TextView,
        tvCreated: TextView
    ) {
        tvEmail.text = "Email: ${profile.email}"
        tvPhone.text = "Phone: ${profile.phone}"
        tvRole.text = "Role: ${profile.role}"
        tvStatus.text = "Status: ${profile.status}"
        tvCreated.text = "Created At: ${profile.createdAt}"
    }
}
