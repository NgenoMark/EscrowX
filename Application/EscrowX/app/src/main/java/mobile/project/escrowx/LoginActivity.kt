package mobile.project.escrowx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import mobile.project.escrowx.R   // <-- explicit R import

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Using findViewById with explicit type parameter (no casting needed)
        val btnTabSignUp = findViewById<Button>(R.id.btnTabSignUp)
        val btnLoginContinue = findViewById<Button>(R.id.btnLoginContinue)
        val etLoginIdentifier = findViewById<EditText>(R.id.etLoginIdentifier)
        val etLoginPassword = findViewById<EditText>(R.id.etLoginPassword)

        btnTabSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        btnLoginContinue.setOnClickListener {
            val identifier = etLoginIdentifier.text.toString().trim()
            val password = etLoginPassword.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: Actual login logic (network call, validation, etc.)
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}