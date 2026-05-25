@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.LoginRequest
import mobile.project.escrowx.LoginResponse
import mobile.project.escrowx.RetrofitClient // FIXED: Import the correct RetrofitClient object
import mobile.project.escrowx.R
import retrofit2.Response

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnTabSignUp = findViewById<Button>(R.id.btnTabSignUp)
        val btnLoginContinue = findViewById<Button>(R.id.btnLoginContinue)
        val etLoginIdentifier = findViewById<EditText>(R.id.etLoginIdentifier)
        val etLoginPassword = findViewById<EditText>(R.id.etLoginPassword)

        btnTabSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        btnLoginContinue.setOnClickListener {
            val phone = etLoginIdentifier.text.toString().trim()
            val password = etLoginPassword.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

                val loginPayload = LoginRequest(
                    phone = phone,
                    password = password
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // FIXED: Replaced the non-existent ApiService with RetrofitClient.instance
                        val response: Response<LoginResponse> =
                            RetrofitClient.instance.loginUser(loginPayload)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                val loginData = response.body()!!
                                val userRole = loginData.user.role

                                if (userRole.equals("BUYER", ignoreCase = true)) {
                                    val intent = Intent(this@LoginActivity, BuyerDashboardActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@LoginActivity, "Welcome $userRole! Dashboard coming soon.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@LoginActivity, "Invalid phone number or password", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Network connection error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}