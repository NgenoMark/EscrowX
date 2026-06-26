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
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

class ResetPasswordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                ResetPasswordScreen()
            }
        }
    }
}

@Composable
private fun ResetPasswordScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isRequestingOtp by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var otpRequested by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Reset Password", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            enabled = !otpRequested
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val trimmedEmail = email.trim()
                if (trimmedEmail.isEmpty()) {
                    Toast.makeText(context, "Enter your email address", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isRequestingOtp = true
                scope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.instance.requestPasswordReset(PasswordResetRequestDto(email = trimmedEmail))
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                val otpPreview = response.body()!!.otpPreview
                                val message = if (!otpPreview.isNullOrBlank()) {
                                    "OTP sent to your email. Dev OTP: $otpPreview"
                                } else {
                                    response.body()!!.message
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                otpRequested = true
                            } else {
                                Toast.makeText(context, "Failed to request OTP", Toast.LENGTH_LONG).show()
                            }
                            isRequestingOtp = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isRequestingOtp = false
                            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRequestingOtp && !otpRequested
        ) {
            if (isRequestingOtp) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Request OTP")
            }
        }

        if (otpRequested) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OTP") },
                singleLine = true
            )

            if (otp.trim().length >= 6) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )

                TextButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                    Text(if (isNewPasswordVisible) "Hide new password" else "Show new password")
                }

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )

                TextButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Text(if (isConfirmPasswordVisible) "Hide confirm password" else "Show confirm password")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        val trimmedOtp = otp.trim()
                        val trimmedNew = newPassword.trim()
                        val trimmedConfirm = confirmPassword.trim()

                        if (trimmedEmail.isEmpty() || trimmedOtp.isEmpty() || trimmedNew.isEmpty() || trimmedConfirm.isEmpty()) {
                            Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (trimmedNew != trimmedConfirm) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isResetting = true

                        scope.launch(Dispatchers.IO) {
                            try {
                                val response = RetrofitClient.instance.confirmPasswordReset(
                                    PasswordResetConfirmRequest(
                                        email = trimmedEmail,
                                        otp = trimmedOtp,
                                        newPassword = trimmedNew
                                    )
                                )

                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Password reset successful. Please login.", Toast.LENGTH_LONG).show()
                                        context.startActivity(Intent(context, LoginActivity::class.java))
                                    } else {
                                        Toast.makeText(context, "Invalid OTP or password does not meet policy", Toast.LENGTH_LONG).show()
                                    }
                                    isResetting = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isResetting = false
                                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isResetting
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm Reset")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }) {
            Text("Back to Login")
        }
    }
}