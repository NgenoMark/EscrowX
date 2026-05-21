package com.example.escrowx // ⚠️ REPLACE THIS with your exact package name

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This links the layout file safely
        setContentView(R.layout.activity_login)

        // Finding elements from the XML layout safely
        val btnTabSignUp = findViewById<Button>(R.id.btnTabSignUp)
        val btnLoginContinue = findViewById<Button>(R.id.btnLoginContinue)
        val etLoginIdentifier = findViewById<EditText>(R.id.etLoginIdentifier)
        val etLoginPassword = findViewById<EditText>(R.id.etLoginPassword)

        // Navigate to Sign Up Activity
        btnTabSignUp.setOnClickListener {
            val intent = Intent(this as Context, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Process Login Action
        btnLoginContinue.setOnClickListener {
            val identifier = etLoginIdentifier.text.toString().trim()
            val password = etLoginPassword.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this as Context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this as Context, "Processing Login...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}