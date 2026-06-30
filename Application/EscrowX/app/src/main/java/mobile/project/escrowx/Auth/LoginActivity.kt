package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

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
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    // Login fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun performLogin() {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        val loginPayload = LoginRequest(email = trimmedEmail, password = trimmedPassword)

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.loginUser(loginPayload)

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
                        val errorMsg = response.errorBody()?.string()?.take(180)
                            ?.replace("\"", "")
                            ?.replace("{", "")
                            ?.replace("}", "")
                            ?: "Invalid email or password"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
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
    }

    Scaffold(
        containerColor = colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ===== TITLE =====
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Welcome Back",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Sign in to continue managing your escrows",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ===== FORM =====
                LoginForm(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    isLoading = isLoading,
                    onLogin = { performLogin() },
                    onForgotPassword = {
                        context.startActivity(Intent(context, ResetPasswordActivity::class.java))
                    },
                    colorScheme = colorScheme
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ===== SIGN UP REDIRECT =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Don't have an account?",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(context, SignUpActivity::class.java))
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Text(
                            "Sign Up",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ===== FOOTER =====
                Text(
                    "Secured by EscrowX",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ===================== LOGIN FORM =====================

@Composable
fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isLoading: Boolean,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email Address", color = colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                cursorColor = colorScheme.primary
            )
        )

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password", color = colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                cursorColor = colorScheme.primary
            ),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        // Forgot Password
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onForgotPassword,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.primary
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Forgot Password?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Login Button
        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Signing In...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            } else {
                Icon(
                    Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun LoginScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        LoginScreen()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun LoginScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        LoginScreen()
    }
}