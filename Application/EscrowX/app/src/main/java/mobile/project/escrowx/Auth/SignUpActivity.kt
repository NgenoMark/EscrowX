@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient // FIXED: Explicitly import RetrofitClient

class SignUpActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val btnSignUpSubmit = findViewById<Button>(R.id.btnSignUpSubmit)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        val etName = findViewById<EditText>(R.id.etSignUpName)
        val etPhone = findViewById<EditText>(R.id.etSignUpPhone)
        val etEmail = findViewById<EditText>(R.id.etSignUpEmail)
        val etPassword = findViewById<EditText>(R.id.etSignUpPassword)
        val etBusiness = findViewById<EditText>(R.id.etSignUpBusiness)

        val roles = arrayOf("BUYER", "SELLER")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        tvBackToLogin.setOnClickListener {
            finish()
        }

        btnSignUpSubmit.setOnClickListener {
            val displayName = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val rawBusinessName = etBusiness.text.toString().trim()

            // FIXED: Replaced the verbose if-else with Kotlin's cleaner, idiomatic .ifEmpty { null }
            val businessName = rawBusinessName.ifEmpty { null }

            if (displayName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sending registration request...", Toast.LENGTH_SHORT).show()

                val registrationPayload = RegisterRequest(
                    phone = phone,
                    email = email,
                    password = password,
                    displayName = displayName,
                    businessName = businessName
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // FIXED: Resolved the reference now that RetrofitClient is properly imported
                        val response = RetrofitClient.instance.registerUser(registrationPayload)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                Toast.makeText(this@SignUpActivity, "Registration Successful!", Toast.LENGTH_LONG).show()

                                // Explicitly targets VerificationActivity cleanly
                                val intent = Intent(this@SignUpActivity, VerificationActivity::class.java).apply {
                                    putExtra("EMAIL", email)
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this@SignUpActivity, "Registration failed: Account might exist.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SignUpActivity, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}
