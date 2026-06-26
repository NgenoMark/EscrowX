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
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import retrofit2.Response

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                LoginScreen()
            }
        }
    }
}

@Composable
private fun LoginScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome Back", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        TextButton(onClick = {
            context.startActivity(Intent(context, ResetPasswordActivity::class.java))
        }) {
            Text("Forgot password?")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val trimmedEmail = email.trim()
                val trimmedPassword = password.trim()

                if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                val loginPayload = LoginRequest(email = trimmedEmail, password = trimmedPassword)

                scope.launch(Dispatchers.IO) {
                    try {
                        val response: Response<LoginResponse> = RetrofitClient.instance.loginUser(loginPayload)

                        withContext(Dispatchers.Main) {
                            val loginData = response.body()
                            if (response.isSuccessful && loginData != null) {
                                val userId = loginData.user.id
                                val userRole = loginData.user.role

                                SessionManager(context).saveSession(
                                    accessToken = loginData.accessToken,
                                    refreshToken = loginData.refreshToken,
                                    email = loginData.user.email,
                                    userId = userId,
                                    role = userRole
                                )

                                when {
                                    userRole.equals("BUYER", ignoreCase = true) -> {
                                        context.startActivity(Intent(context, BuyerDashboardActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                    }

                                    userRole.equals("SELLER", ignoreCase = true) -> {
                                        context.startActivity(Intent(context, SellerDashboardActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                    }

                                    else -> Toast.makeText(context, "Welcome $userRole!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Invalid email or password", Toast.LENGTH_LONG).show()
                            }
                            isLoading = false
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            Toast.makeText(context, "Network connection error", Toast.LENGTH_LONG).show()
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
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            context.startActivity(Intent(context, SignUpActivity::class.java))
        }) {
            Text("Create account")
        }
    }
}