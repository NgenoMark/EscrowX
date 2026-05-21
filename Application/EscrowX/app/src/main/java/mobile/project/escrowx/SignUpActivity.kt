package mobile.project.escrowx
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class SignUpActivity : ComponentActivity() {
    // Jetpack Compose codebase sits here safely

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val btnSignUpSubmit = findViewById<Button>(R.id.btnSignUpSubmit)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        // Form Fields
        val etName = findViewById<EditText>(R.id.etSignUpName)
        val etPhone = findViewById<EditText>(R.id.etSignUpPhone)
        val etEmail = findViewById<EditText>(R.id.etSignUpEmail)
        val etPassword = findViewById<EditText>(R.id.etSignUpPassword)
        val etBusiness = findViewById<EditText>(R.id.etSignUpBusiness)

        // Populate Dropdown Options
        val roles = arrayOf("BUYER", "SELLER")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // Navigate back to Login
        tvBackToLogin.setOnClickListener {
            finish()
        }

        // Submit Action
        btnSignUpSubmit.setOnClickListener {
            val displayName = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val businessName = etBusiness.text.toString().trim()
            val selectedRole = spinnerRole.selectedItem.toString()

            if (displayName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            } else {
                val message = if (businessName.isNotEmpty()) {
                    "Registering $displayName ($businessName) as $selectedRole..."
                } else {
                    "Registering $displayName as $selectedRole..."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}