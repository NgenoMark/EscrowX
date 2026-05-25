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
import mobile.project.escrowx.ConfirmRequest
import mobile.project.escrowx.ConfirmResponse
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient // FIXED: Explicitly import RetrofitClient

class VerificationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        val etOtpCode = findViewById<EditText>(R.id.etOtpCode)
        val btnVerifySubmit = findViewById<Button>(R.id.btnVerifySubmit)

        // Pull contextual phone identifier sent cleanly from registration views
        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""

        btnVerifySubmit.setOnClickListener {
            val otp = etOtpCode.text.toString().trim()

            if (otp.isEmpty()) {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
            } else {
                val confirmPayload = ConfirmRequest(
                    phone = phoneNumber,
                    otp = otp
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // FIXED: Resolved reference now that RetrofitClient is imported
                        val response = RetrofitClient.instance.confirmAccount(confirmPayload)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                val confirmData = response.body()!!

                                // Checks against the true Boolean flag returned by the backend layout schema
                                if (confirmData.confirmed) {
                                    Toast.makeText(this@VerificationActivity, "Account Activated Successfully!", Toast.LENGTH_LONG).show()

                                    // Safely forward user to log in
                                    val intent = Intent(this@VerificationActivity, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@VerificationActivity, "Activation rejected by server.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@VerificationActivity, "Invalid or expired OTP token.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@VerificationActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}