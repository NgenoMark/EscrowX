@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.auth


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient

class ResetPasswordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val etPhone = findViewById<EditText>(R.id.etResetPhone)
        val etOtp = findViewById<EditText>(R.id.etResetOtp)
        val etNewPassword = findViewById<EditText>(R.id.etResetNewPassword)
        val btnRequestOtp = findViewById<Button>(R.id.btnRequestOtp)
        val btnConfirmReset = findViewById<Button>(R.id.btnConfirmReset)
        val tvBackToLogin = findViewById<TextView>(R.id.tvResetBackToLogin)

        tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRequestOtp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.isBlank()) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.requestPasswordReset(
                        PasswordResetRequestDto(phone = phone)
                    )
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val otpPreview = response.body()!!.otpPreview
                            Toast.makeText(
                                this@ResetPasswordActivity,
                                "OTP sent. Dev OTP: $otpPreview",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ResetPasswordActivity,
                                "Failed to request OTP",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Network error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        btnConfirmReset.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val otp = etOtp.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()

            if (phone.isBlank() || otp.isBlank() || newPassword.isBlank()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.confirmPasswordReset(
                        PasswordResetConfirmRequest(
                            phone = phone,
                            otp = otp,
                            newPassword = newPassword
                        )
                    )

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.passwordUpdated == true) {
                            Toast.makeText(
                                this@ResetPasswordActivity,
                                "Password reset successful. Please login.",
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(this@ResetPasswordActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this@ResetPasswordActivity,
                                "Invalid OTP or password does not meet policy",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Network error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
