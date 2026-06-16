@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
    private lateinit var btnToggleNewPassword: ImageButton
    private lateinit var btnToggleConfirmPassword: ImageButton
    private lateinit var btnRequestOtp: Button
    private lateinit var btnConfirmReset: Button
    private lateinit var tvBackToLogin: TextView
    private lateinit var tvInstruction: TextView

    private lateinit var otpContainer: View
    private lateinit var passwordContainer: View
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Find views by the new IDs
        etEmail = findViewById(R.id.etResetEmail)
        etOtp = findViewById(R.id.etResetOtp)
        etNewPassword = findViewById(R.id.etResetNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmNewPassword)
        btnToggleNewPassword = findViewById(R.id.btnToggleNewPassword)
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword)
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

        fun applyPasswordVisibility(editText: EditText, toggleButton: ImageButton, isVisible: Boolean) {
            editText.transformationMethod = if (isVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            toggleButton.setImageResource(if (isVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
            editText.setSelection(editText.text?.length ?: 0)
        }

        btnToggleNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            applyPasswordVisibility(etNewPassword, btnToggleNewPassword, isNewPasswordVisible)
        }

        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            applyPasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible)
        }

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
                    val response = RetrofitClient.instance.requestPasswordReset(
                        PasswordResetRequestDto(email = email)
                    )
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val otpPreview = response.body()!!.otpPreview
                            val message = if (!otpPreview.isNullOrBlank()) {
                                "OTP sent to your email. Dev OTP: $otpPreview"
                            } else {
                                response.body()!!.message
                            }
                            Toast.makeText(this@ResetPasswordActivity, message, Toast.LENGTH_LONG).show()
                            // Show OTP section
                            otpContainer.visibility = View.VISIBLE
                            etEmail.isEnabled = false
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

        // Show password fields immediately once OTP is complete.
        etOtp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val otp = s?.toString()?.trim().orEmpty()
                if (otp.length == 6) {
                    passwordContainer.visibility = View.VISIBLE
                    btnConfirmReset.visibility = View.VISIBLE
                    tvInstruction.text = "Enter your new password."
                } else {
                    passwordContainer.visibility = View.GONE
                    btnConfirmReset.visibility = View.GONE
                    tvInstruction.text = "Enter the OTP sent to your email."
                }
            }
        })

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
                            email = email,
                            otp = otp,
                            newPassword = newPassword
                        )
                    )

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
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