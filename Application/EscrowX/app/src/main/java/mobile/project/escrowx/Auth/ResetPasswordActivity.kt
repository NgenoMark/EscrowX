@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    private lateinit var etEmail: EditText
    private lateinit var etOtp: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRequestOtp: Button
    private lateinit var btnConfirmReset: Button
    private lateinit var tvBackToLogin: TextView
    private lateinit var tvInstruction: TextView

    private lateinit var otpContainer: View
    private lateinit var passwordContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Find views by the new IDs
        etEmail = findViewById(R.id.etResetEmail)
        etOtp = findViewById(R.id.etResetOtp)
        etNewPassword = findViewById(R.id.etResetNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmNewPassword)
        btnRequestOtp = findViewById(R.id.btnRequestOtp)
        btnConfirmReset = findViewById(R.id.btnConfirmReset)
        tvBackToLogin = findViewById(R.id.tvResetBackToLogin)
        tvInstruction = findViewById(R.id.tvInstruction)

        otpContainer = findViewById(R.id.otpContainer)
        passwordContainer = findViewById(R.id.passwordContainer)

        // Initially hide OTP and password sections
        otpContainer.visibility = View.GONE
        passwordContainer.visibility = View.GONE
        btnConfirmReset.visibility = View.GONE

        tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRequestOtp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Backend expects 'phone' parameter – we send email as phone
                    val response = RetrofitClient.instance.requestPasswordReset(
                        PasswordResetRequestDto(phone = email)
                    )
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val otpPreview = response.body()!!.otpPreview
                            Toast.makeText(
                                this@ResetPasswordActivity,
                                "OTP sent to your email. Dev OTP: $otpPreview",
                                Toast.LENGTH_LONG
                            ).show()
                            // Show OTP section
                            otpContainer.visibility = View.VISIBLE
                            btnRequestOtp.isEnabled = false
                            tvInstruction.text = "Enter the OTP sent to your email."
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

        // When OTP field loses focus and has 6 digits, show password fields
        etOtp.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && etOtp.text.toString().trim().length == 6) {
                passwordContainer.visibility = View.VISIBLE
                btnConfirmReset.visibility = View.VISIBLE
                tvInstruction.text = "Enter your new password."
            }
        }

        btnConfirmReset.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val otp = etOtp.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.confirmPasswordReset(
                        PasswordResetConfirmRequest(
                            phone = email,
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