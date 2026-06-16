package mobile.project.escrowx.auth
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager

class CreateNewPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra("EMAIL") ?: ""
        val otp = intent.getStringExtra("OTP") ?: ""

        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                CreateNewPasswordScreen(email = email, otp = otp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewPasswordScreen(email: String, otp: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Password validation
    val hasLength = newPassword.length >= 8
    val hasUpper = Regex("[A-Z]").containsMatchIn(newPassword)
    val hasNumber = Regex("[0-9]").containsMatchIn(newPassword)
    val isPasswordValid = hasLength && hasUpper && hasNumber
    val doPasswordsMatch = newPassword == confirmPassword

    fun updatePassword() {
        if (!isPasswordValid) {
            Toast.makeText(context, "Please meet all password requirements", Toast.LENGTH_SHORT).show()
            return
        }

        if (!doPasswordsMatch) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        scope.launch {
            try {
                val token = session.getAccessToken()
                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isLoading = false
                    return@launch
                }

                // TODO: Call API to update password
                // val request = PasswordResetConfirmRequest(
                //     phone = phone,
                //     otp = otp,
                //     newPassword = newPassword
                // )
                // val response = RetrofitClient.instance.confirmPasswordReset(request)

                delay(1500) // Simulate API call

                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_LONG).show()

                // Navigate to login
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                (context as? CreateNewPasswordActivity)?.finish()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create New Escrow",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00236F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? CreateNewPasswordActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00236F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9FF)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .padding(24.dp)
        ) {
            // Title Section
            Text(
                "New Password",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF151C27)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Please choose a strong password that you haven't used before.",
                fontSize = 14.sp,
                color = Color(0xFF444651)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // New Password Field
            Text(
                "New Password",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF444651),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter new password") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00236F),
                    unfocusedBorderColor = Color(0xFFC5C5D3)
                ),
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showNewPassword) "Hide password" else "Show password",
                            tint = Color(0xFF444651)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            Text(
                "Confirm New Password",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF444651),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Re-type new password") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00236F),
                    unfocusedBorderColor = Color(0xFFC5C5D3)
                ),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                            tint = Color(0xFF444651)
                        )
                    }
                },
                isError = confirmPassword.isNotEmpty() && !doPasswordsMatch,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                        Text("Passwords do not match", color = Color.Red, fontSize = 11.sp)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Password Requirements Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PasswordRequirementItem(
                        text = "At least 8 characters",
                        isMet = hasLength,
                        icon = Icons.Default.CheckCircle
                    )
                    PasswordRequirementItem(
                        text = "One uppercase letter",
                        isMet = hasUpper,
                        icon = Icons.Default.CheckCircle
                    )
                    PasswordRequirementItem(
                        text = "One number",
                        isMet = hasNumber,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Update Button
            Button(
                onClick = { updatePassword() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00236F),
                    contentColor = Color.White
                ),
                enabled = isPasswordValid && doPasswordsMatch && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Update Password", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PasswordRequirementItem(text: String, isMet: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isMet) Color(0xFF006C49) else Color(0xFF444651),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = if (isMet) Color(0xFF006C49) else Color(0xFF444651)
        )
    }
}