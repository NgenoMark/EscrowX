package mobile.project.escrowx.auth

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.PasswordResetConfirmRequest
import mobile.project.escrowx.ui.theme.BrandBlue

class CreateNewPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra("EMAIL") ?: ""
        val otp = intent.getStringExtra("OTP") ?: ""

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.rememberDarkModeEnabledState(),
                dynamicColor = false
            ) {
                CreateNewPasswordScreen(email = email, otp = otp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CreateNewPasswordScreen(email: String, otp: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var passwordStrength by remember { mutableStateOf(0f) }

    // Password validation
    val hasLength = newPassword.length >= 8
    val hasUpper = Regex("[A-Z]").containsMatchIn(newPassword)
    val hasLower = Regex("[a-z]").containsMatchIn(newPassword)
    val hasNumber = Regex("[0-9]").containsMatchIn(newPassword)
    val hasSpecial = Regex("[!@#\$%^&*(),.?\":{}|<>]").containsMatchIn(newPassword)

    val isPasswordValid = hasLength && hasUpper && hasLower && hasNumber && hasSpecial
    val doPasswordsMatch = newPassword == confirmPassword && newPassword.isNotEmpty()

    // Calculate password strength
    LaunchedEffect(newPassword) {
        var strength = 0f
        if (hasLength) strength += 0.2f
        if (hasUpper) strength += 0.2f
        if (hasLower) strength += 0.15f
        if (hasNumber) strength += 0.2f
        if (hasSpecial) strength += 0.25f
        passwordStrength = strength
    }

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
                if (email.isBlank() || otp.isBlank()) {
                    Toast.makeText(context, "Reset session expired. Please request a new code.", Toast.LENGTH_LONG).show()
                    isLoading = false
                    return@launch
                }

                val request = PasswordResetConfirmRequest(
                    email = email,
                    otp = otp,
                    newPassword = newPassword
                )
                val response = RetrofitClient.instance.confirmPasswordReset(request)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Failed to update password. Please verify your code and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    isLoading = false
                    return@launch
                }

                val passwordUpdated = response.body()?.passwordUpdated ?: true
                if (!passwordUpdated) {
                    Toast.makeText(context, "Password update rejected by server", Toast.LENGTH_LONG).show()
                    isLoading = false
                    return@launch
                }

                isSuccess = true
                Toast.makeText(context, "âœ… Password updated successfully!", Toast.LENGTH_LONG).show()

                delay(1500)

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

    // Get masked email for display
    fun getMaskedEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        return if (name.length <= 4) {
            "${name.take(1)}****@$domain"
        } else {
            "${name.take(2)}****${name.takeLast(2)}@$domain"
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create New Password",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? CreateNewPasswordActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Icon
            AnimatedContent(
                targetState = isSuccess,
                transitionSpec = {
                    fadeIn() + scaleIn() with fadeOut() + scaleOut()
                }
            ) { success ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (success) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF34D399)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        BrandBlue,
                                        BrandBlue.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (success) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Success",
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Security",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                "Set New Password",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                "Create a strong password that you'll remember",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email display - Fixed for long emails
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Updating password for:",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = getMaskedEmail(email),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // New Password Field
            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    if (isSuccess) isSuccess = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "New Password",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                },
                placeholder = {
                    Text(
                        "Enter strong password",
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                    cursorColor = colorScheme.primary,
                    focusedLabelColor = colorScheme.primary
                ),
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showNewPassword) "Hide password" else "Show password",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            // Password Strength Indicator
            if (newPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(passwordStrength)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        passwordStrength < 0.3f -> Color(0xFFDC2626)
                                        passwordStrength < 0.6f -> Color(0xFFF59E0B)
                                        passwordStrength < 0.8f -> Color(0xFF3B82F6)
                                        else -> Color(0xFF10B981)
                                    }
                                )
                        )
                    }
                    Text(
                        when {
                            passwordStrength < 0.3f -> "Weak"
                            passwordStrength < 0.6f -> "Fair"
                            passwordStrength < 0.8f -> "Good"
                            else -> "Strong"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            passwordStrength < 0.3f -> Color(0xFFDC2626)
                            passwordStrength < 0.6f -> Color(0xFFF59E0B)
                            passwordStrength < 0.8f -> Color(0xFF3B82F6)
                            else -> Color(0xFF10B981)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (isSuccess) isSuccess = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Confirm Password",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                },
                placeholder = {
                    Text(
                        "Re-type new password",
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmPassword.isNotEmpty() && !doPasswordsMatch)
                        MaterialTheme.colorScheme.error
                    else
                        colorScheme.primary,
                    unfocusedBorderColor = if (confirmPassword.isNotEmpty() && !doPasswordsMatch)
                        MaterialTheme.colorScheme.error
                    else
                        colorScheme.outlineVariant,
                    cursorColor = colorScheme.primary,
                    focusedLabelColor = if (confirmPassword.isNotEmpty() && !doPasswordsMatch)
                        MaterialTheme.colorScheme.error
                    else
                        colorScheme.primary
                ),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
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
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                isError = confirmPassword.isNotEmpty() && !doPasswordsMatch,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                        Text(
                            "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    } else if (confirmPassword.isNotEmpty() && doPasswordsMatch) {
                        Text(
                            "âœ“ Passwords match",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Requirements Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Password Requirements",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    PasswordRequirementItemEnhanced(
                        text = "At least 8 characters",
                        isMet = hasLength,
                        colorScheme = colorScheme
                    )
                    PasswordRequirementItemEnhanced(
                        text = "One uppercase letter",
                        isMet = hasUpper,
                        colorScheme = colorScheme
                    )
                    PasswordRequirementItemEnhanced(
                        text = "One lowercase letter",
                        isMet = hasLower,
                        colorScheme = colorScheme
                    )
                    PasswordRequirementItemEnhanced(
                        text = "One number",
                        isMet = hasNumber,
                        colorScheme = colorScheme
                    )
                    PasswordRequirementItemEnhanced(
                        text = "One special character (!@#$%^&*)",
                        isMet = hasSpecial,
                        colorScheme = colorScheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Update Button
            Button(
                onClick = { updatePassword() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSuccess) Color(0xFF10B981) else colorScheme.primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                ),
                enabled = isPasswordValid && doPasswordsMatch && !isLoading && !isSuccess
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Updating Password...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                } else if (isSuccess) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Success!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Update Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Security Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Use a unique password you haven't used before",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PasswordRequirementItemEnhanced(
    text: String,
    isMet: Boolean,
    colorScheme: ColorScheme
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (isMet) Color(0xFF10B981).copy(alpha = 0.12f)
                    else colorScheme.outlineVariant.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isMet) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isMet) Color(0xFF10B981) else colorScheme.onSurfaceVariant
            )
        }
        Text(
            text,
            fontSize = 12.sp,
            color = if (isMet) colorScheme.onSurface else colorScheme.onSurfaceVariant,
            fontWeight = if (isMet) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun CreateNewPasswordScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        CreateNewPasswordScreen(email = "user@example.com", otp = "123456")
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun CreateNewPasswordScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        CreateNewPasswordScreen(email = "user@example.com", otp = "123456")
    }
}
