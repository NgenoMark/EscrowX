package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
@OptIn(ExperimentalAnimationApi::class)
private fun LoginScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    var isLoginMode by remember { mutableStateOf(true) }

    // Login fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Register fields
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regIsPasswordVisible by remember { mutableStateOf(false) }
    var regIsConfirmVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("BUYER") }

    // Password validation
    val hasLength = regPassword.length >= 8
    val hasUpper = Regex("[A-Z]").containsMatchIn(regPassword)
    val hasNumber = Regex("[0-9]").containsMatchIn(regPassword)
    val isPasswordValid = hasLength && hasUpper && hasNumber
    val doPasswordsMatch = regPassword == regConfirmPassword && regPassword.isNotEmpty()

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

    fun performRegister() {
        val name = regFullName.trim()
        val registerEmail = regEmail.trim()
        val registerPhone = regPhone.trim()
        val registerPassword = regPassword.trim()
        val confirm = regConfirmPassword.trim()
        val registerBusinessName = null

        if (name.isEmpty() || registerEmail.isEmpty() || registerPhone.isEmpty() || registerPassword.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isPasswordValid) {
            Toast.makeText(context, "Password must be at least 8 characters with uppercase and number", Toast.LENGTH_SHORT).show()
            return
        }

        if (!doPasswordsMatch) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        isRegistering = true

        val registerPayload = RegisterRequest(
            displayName = name,
            email = registerEmail,
            phone = registerPhone,
            password = registerPassword,
            businessName = registerBusinessName,
            role = selectedRole
        )

        scope.launch(Dispatchers.IO) {
            try {
                val response: Response<RegisterResponse> = RetrofitClient.instance.registerUser(registerPayload)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(context, "Account created successfully! Please login.", Toast.LENGTH_LONG).show()
                        isRegistering = false
                        // Switch to login mode with email pre-filled
                        email = registerEmail
                        isLoginMode = true
                    } else {
                        val errorMsg = response.errorBody()?.string()?.take(100) ?: "Registration failed"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        isRegistering = false
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isRegistering = false
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
                AnimatedContent(
                    targetState = isLoginMode,
                    transitionSpec = {
                        fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                    }
                ) { isLogin ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isLogin) "Welcome Back" else "Create Account",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isLogin)
                                "Sign in to continue managing your escrows"
                            else
                                "Start your secure escrow journey today",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ===== FORM =====
                AnimatedContent(
                    targetState = isLoginMode,
                    transitionSpec = {
                        fadeIn() + slideInHorizontally() with fadeOut() + slideOutHorizontally()
                    }
                ) { isLogin ->
                    if (isLogin) {
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
                    } else {
                        RegisterForm(
                            fullName = regFullName,
                            onFullNameChange = { regFullName = it },
                            email = regEmail,
                            onEmailChange = { regEmail = it },
                            phone = regPhone,
                            onPhoneChange = { regPhone = it },
                            password = regPassword,
                            onPasswordChange = { regPassword = it },
                            confirmPassword = regConfirmPassword,
                            onConfirmPasswordChange = { regConfirmPassword = it },
                            isPasswordVisible = regIsPasswordVisible,
                            onPasswordVisibilityToggle = { regIsPasswordVisible = !regIsPasswordVisible },
                            isConfirmVisible = regIsConfirmVisible,
                            onConfirmVisibilityToggle = { regIsConfirmVisible = !regIsConfirmVisible },
                            selectedRole = selectedRole,
                            onRoleChange = { selectedRole = it },
                            isPasswordValid = isPasswordValid,
                            doPasswordsMatch = doPasswordsMatch,
                            isRegistering = isRegistering,
                            onRegister = { performRegister() },
                            colorScheme = colorScheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== TOGGLE MODE =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (isLoginMode) "Don't have an account?" else "Already have an account?",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            if (isLoginMode) {
                                context.startActivity(Intent(context, SignUpActivity::class.java))
                            } else {
                                isLoginMode = true
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Text(
                            if (isLoginMode) "Sign Up" else "Sign In",
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

// ===================== REGISTER FORM =====================

@Composable
fun RegisterForm(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isConfirmVisible: Boolean,
    onConfirmVisibilityToggle: () -> Unit,
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    isPasswordValid: Boolean,
    doPasswordsMatch: Boolean,
    isRegistering: Boolean,
    onRegister: () -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full Name", color = colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
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

        // Email
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

        // Phone
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone Number", color = colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            },
            placeholder = { Text("+254 700 000 000", color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                cursorColor = colorScheme.primary
            )
        )

        // Password
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
                focusedBorderColor = if (password.isNotEmpty() && !isPasswordValid)
                    MaterialTheme.colorScheme.error
                else
                    colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                cursorColor = colorScheme.primary
            ),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (password.isNotEmpty() && isPasswordValid) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Valid password",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            supportingText = {
                if (password.isNotEmpty() && !isPasswordValid) {
                    Text(
                        "Password must be 8+ chars with uppercase & number",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }
        )

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm Password", color = colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (confirmPassword.isNotEmpty() && !doPasswordsMatch)
                    MaterialTheme.colorScheme.error
                else
                    colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                cursorColor = colorScheme.primary
            ),
            visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (confirmPassword.isNotEmpty() && doPasswordsMatch) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Passwords match",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = onConfirmVisibilityToggle) {
                        Icon(
                            if (isConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isConfirmVisible) "Hide password" else "Show password",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            supportingText = {
                if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                    Text(
                        "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                } else if (confirmPassword.isNotEmpty() && doPasswordsMatch) {
                    Text(
                        "✓ Passwords match",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp
                    )
                }
            }
        )

        // Role Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoleChip(
                modifier = Modifier.weight(1f),
                text = "Buyer",
                icon = Icons.Default.ShoppingCart,
                selected = selectedRole == "BUYER",
                onClick = { onRoleChange("BUYER") },
                colorScheme = colorScheme
            )
            RoleChip(
                modifier = Modifier.weight(1f),
                text = "Seller",
                icon = Icons.Default.Storefront,
                selected = selectedRole == "SELLER",
                onClick = { onRoleChange("SELLER") },
                colorScheme = colorScheme
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Register Button
        Button(
            onClick = onRegister,
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
            enabled = !isRegistering &&
                    fullName.isNotBlank() &&
                    email.isNotBlank() &&
                    phone.isNotBlank() &&
                    isPasswordValid &&
                    doPasswordsMatch
        ) {
            if (isRegistering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Creating Account...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            } else {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ===================== ROLE CHIP =====================

@Composable
fun RoleChip(
    modifier: Modifier = Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(50),
        color = if (selected) colorScheme.primary else colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface
            )
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