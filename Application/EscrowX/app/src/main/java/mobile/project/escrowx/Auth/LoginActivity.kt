@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.RetrofitClient
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
        val btnToggleLoginPassword = findViewById<ImageButton>(R.id.btnToggleLoginPassword)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        var isPasswordVisible = false

        btnToggleLoginPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etLoginPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnToggleLoginPassword.setImageResource(R.drawable.ic_visibility)
            } else {
                etLoginPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnToggleLoginPassword.setImageResource(R.drawable.ic_visibility_off)
            }
            etLoginPassword.setSelection(etLoginPassword.text.length)
        }

        btnTabSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        btnLoginContinue.setOnClickListener {
            val email = etLoginIdentifier.text.toString().trim()
            val password = etLoginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

                val loginPayload = LoginRequest(
                    email = email,
                    password = password
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response: Response<LoginResponse> = RetrofitClient.instance.loginUser(loginPayload)

                        withContext(Dispatchers.Main) {
                            val loginData = response.body()
                            if (response.isSuccessful && loginData != null) {
                                // Extract userId and role from the nested user object
                                val userId = loginData.user.id
                                val userRole = loginData.user.role

                                // Persist authentication session with new fields
                                SessionManager(this@LoginActivity).saveSession(
                                    accessToken = loginData.accessToken,
                                    refreshToken = loginData.refreshToken,
                                    email = loginData.user.email,
                                    userId = userId,
                                    role = userRole
                                )

                                if (userRole.equals("BUYER", ignoreCase = true)) {
                                    val intent = Intent(this@LoginActivity, BuyerDashboardActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                } else if (userRole.equals("SELLER", ignoreCase = true)) {
                                    val intent = Intent(this@LoginActivity, SellerDashboardActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@LoginActivity, "Welcome $userRole!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Network connection error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}