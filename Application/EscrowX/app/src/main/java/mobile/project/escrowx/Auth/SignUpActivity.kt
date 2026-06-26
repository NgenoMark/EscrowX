@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

class SignUpActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                SignUpScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var businessName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("BUYER") }
    var roleExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val roles = listOf("BUYER", "SELLER")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full Name") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
            Text(if (isPasswordVisible) "Hide password" else "Show password")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Business Name (Optional)") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        ExposedDropdownMenuBox(
            expanded = roleExpanded,
            onExpandedChange = { roleExpanded = !roleExpanded }
        ) {
            OutlinedTextField(
                value = role,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Register As") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) }
            )

            ExposedDropdownMenu(
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false }
            ) {
                roles.forEach { currentRole ->
                    DropdownMenuItem(
                        text = { Text(currentRole) },
                        onClick = {
                            role = currentRole
                            roleExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val trimmedName = displayName.trim()
                val trimmedPhone = phone.trim()
                val trimmedEmail = email.trim()
                val trimmedPassword = password.trim()
                val trimmedBusiness = businessName.trim().ifEmpty { null }

                if (trimmedName.isEmpty() || trimmedPhone.isEmpty() || trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
                    Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                val registrationPayload = RegisterRequest(
                    phone = trimmedPhone,
                    email = trimmedEmail,
                    password = trimmedPassword,
                    displayName = trimmedName,
                    businessName = trimmedBusiness
                )

                scope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.instance.registerUser(registrationPayload)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                Toast.makeText(context, "Registration Successful!", Toast.LENGTH_LONG).show()
                                context.startActivity(Intent(context, VerificationActivity::class.java).apply {
                                    putExtra("EMAIL", trimmedEmail)
                                })
                            } else {
                                Toast.makeText(context, "Registration failed: Account might exist.", Toast.LENGTH_LONG).show()
                            }
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            Toast.makeText(context, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            if (context is ComponentActivity) context.finish()
        }) {
            Text("Already have an account? Login")
        }
    }
}
