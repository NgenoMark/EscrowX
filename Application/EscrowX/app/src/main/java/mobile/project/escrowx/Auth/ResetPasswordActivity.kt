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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import mobile.project.escrowx.ui.theme.BrandBlue

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    // Step state
    var currentStep by remember { mutableStateOf(ResetStep.REQUEST_OTP) }

    // Email
    var email by remember { mutableStateOf("") }
    var isRequestingOtp by remember { mutableStateOf(false) }
    var otpRequested by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableStateOf(0) }

    // OTP
    var otpCode by remember { mutableStateOf(List(6) { "" }) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf(false) }
    var otpErrorMessage by remember { mutableStateOf("") }

    // Password
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val otpDigits = remember(otpCode) { otpCode.joinToString("") }
    val isOtpComplete = otpDigits.length == 6

    // Password validation
    val hasLength = newPassword.length >= 8
    val hasUpper = Regex("[A-Z]").containsMatchIn(newPassword)
    val hasNumber = Regex("[0-9]").containsMatchIn(newPassword)
    val isPasswordValid = hasLength && hasUpper && hasNumber
    val doPasswordsMatch = newPassword == confirmPassword && newPassword.isNotEmpty()

    // Resend countdown timer
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }

    fun getMaskedEmail(): String {
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

    fun requestOtp() {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show()
            return
        }

        isRequestingOtp = true

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.requestPasswordReset(PasswordResetRequestDto(email = trimmedEmail))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val otpPreview = response.body()!!.otpPreview
                        val message = if (!otpPreview.isNullOrBlank()) {
                            "📧 OTP sent to your email. Dev OTP: $otpPreview"
                        } else {
                            response.body()!!.message ?: "OTP sent successfully"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        otpRequested = true
                        currentStep = ResetStep.VERIFY_OTP
                        resendCountdown = 30
                    } else {
                        Toast.makeText(context, "Failed to send OTP. Please try again.", Toast.LENGTH_LONG).show()
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
    }

    fun verifyOtp() {
        if (otpDigits.length < 6) {
            otpError = true
            otpErrorMessage = "Please enter the 6-digit code"
            return
        }

        isVerifyingOtp = true
        otpError = false

        scope.launch(Dispatchers.IO) {
            try {
                // Simulate verification
                delay(1000)
                withContext(Dispatchers.Main) {
                    isVerifyingOtp = false
                    currentStep = ResetStep.SET_NEW_PASSWORD
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isVerifyingOtp = false
                    otpError = true
                    otpErrorMessage = "Invalid or expired code. Please try again."
                }
            }
        }
    }

    // Auto-verify OTP when complete
    LaunchedEffect(isOtpComplete) {
        if (isOtpComplete && currentStep == ResetStep.VERIFY_OTP && !isVerifyingOtp) {
            verifyOtp()
        }
    }

    fun resetPassword() {
        val trimmedEmail = email.trim()
        val trimmedOtp = otpDigits.trim()
        val trimmedNew = newPassword.trim()
        val trimmedConfirm = confirmPassword.trim()

        if (trimmedEmail.isEmpty() || trimmedOtp.isEmpty() || trimmedNew.isEmpty() || trimmedConfirm.isEmpty()) {
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
                        isSuccess = true
                        Toast.makeText(context, "✅ Password reset successful!", Toast.LENGTH_LONG).show()
                        delay(1000)
                        context.startActivity(Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
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
    }

    fun resendOtp() {
        if (resendCountdown > 0) return

        isRequestingOtp = true

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.requestPasswordReset(PasswordResetRequestDto(email = email.trim()))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        resendCountdown = 30
                        Toast.makeText(context, "OTP resent successfully!", Toast.LENGTH_SHORT).show()
                        // Reset OTP fields
                        otpCode = List(6) { "" }
                        otpError = false
                    } else {
                        Toast.makeText(context, "Failed to resend OTP", Toast.LENGTH_LONG).show()
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
    }

    fun onOtpChange(index: Int, value: String) {
        if (value.length <= 1 && value.all { it.isDigit() }) {
            val newOtp = otpCode.toMutableList()
            newOtp[index] = value
            otpCode = newOtp
            otpError = false
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
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ===== LOGO =====
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BrandBlue,
                                    BrandBlue.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LockReset,
                        contentDescription = "Reset Password",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== TITLE & SUBTITLE =====
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Reset Password",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        when (currentStep) {
                            ResetStep.REQUEST_OTP -> "Enter your email to receive a verification code"
                            ResetStep.VERIFY_OTP -> "Enter the 6-digit code sent to your email"
                            ResetStep.SET_NEW_PASSWORD -> "Create a new password for your account"
                        },
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ===== STEP INDICATOR =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepIndicator(
                        modifier = Modifier.weight(1f),
                        step = 1,
                        label = "Email",
                        isActive = currentStep == ResetStep.REQUEST_OTP,
                        isCompleted = currentStep.ordinal > ResetStep.REQUEST_OTP.ordinal,
                        colorScheme = colorScheme
                    )
                    StepIndicator(
                        modifier = Modifier.weight(1f),
                        step = 2,
                        label = "Verify",
                        isActive = currentStep == ResetStep.VERIFY_OTP,
                        isCompleted = currentStep.ordinal > ResetStep.VERIFY_OTP.ordinal,
                        colorScheme = colorScheme
                    )
                    StepIndicator(
                        modifier = Modifier.weight(1f),
                        step = 3,
                        label = "Reset",
                        isActive = currentStep == ResetStep.SET_NEW_PASSWORD,
                        isCompleted = false,
                        colorScheme = colorScheme
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ===== FORM =====
                when (currentStep) {
                    ResetStep.REQUEST_OTP -> {
                        RequestOtpForm(
                            email = email,
                            onEmailChange = { email = it },
                            isRequestingOtp = isRequestingOtp,
                            onRequestOtp = { requestOtp() },
                            colorScheme = colorScheme
                        )
                    }
                    ResetStep.VERIFY_OTP -> {
                        VerifyOtpForm(
                            otpCode = otpCode,
                            onOtpChange = { index, value -> onOtpChange(index, value) },
                            isVerifyingOtp = isVerifyingOtp,
                            otpError = otpError,
                            otpErrorMessage = otpErrorMessage,
                            resendCountdown = resendCountdown,
                            isRequestingOtp = isRequestingOtp,
                            onResendOtp = { resendOtp() },
                            email = email,
                            colorScheme = colorScheme
                        )
                    }
                    ResetStep.SET_NEW_PASSWORD -> {
                        SetNewPasswordForm(
                            newPassword = newPassword,
                            onNewPasswordChange = { newPassword = it },
                            confirmPassword = confirmPassword,
                            onConfirmPasswordChange = { confirmPassword = it },
                            isNewPasswordVisible = isNewPasswordVisible,
                            onNewPasswordVisibilityToggle = { isNewPasswordVisible = !isNewPasswordVisible },
                            isConfirmPasswordVisible = isConfirmPasswordVisible,
                            onConfirmPasswordVisibilityToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                            isPasswordValid = isPasswordValid,
                            doPasswordsMatch = doPasswordsMatch,
                            isResetting = isResetting,
                            isSuccess = isSuccess,
                            onReset = { resetPassword() },
                            colorScheme = colorScheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== BACK TO LOGIN =====
                TextButton(
                    onClick = {
                        context.startActivity(Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back to Login", fontSize = 13.sp)
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

// ===================== STEP INDICATOR =====================

@Composable
fun StepIndicator(
    modifier: Modifier = Modifier,
    step: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    colorScheme: ColorScheme
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Color(0xFF10B981)
                        isActive -> colorScheme.primary
                        else -> colorScheme.outlineVariant.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            } else {
                Text(
                    step.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            label,
            fontSize = 10.sp,
            color = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ===================== REQUEST OTP FORM =====================

@Composable
fun RequestOtpForm(
    email: String,
    onEmailChange: (String) -> Unit,
    isRequestingOtp: Boolean,
    onRequestOtp: () -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        Button(
            onClick = onRequestOtp,
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
            enabled = !isRequestingOtp && email.isNotBlank()
        ) {
            if (isRequestingOtp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sending...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            } else {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Send Verification Code", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ===================== VERIFY OTP FORM =====================

@Composable
fun VerifyOtpForm(
    otpCode: List<String>,
    onOtpChange: (Int, String) -> Unit,
    isVerifyingOtp: Boolean,
    otpError: Boolean,
    otpErrorMessage: String,
    resendCountdown: Int,
    isRequestingOtp: Boolean,
    onResendOtp: () -> Unit,
    email: String,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // OTP Input Fields
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(6) { index ->
                OtpInputFieldEnhanced(
                    value = otpCode[index],
                    onValueChange = { newValue ->
                        onOtpChange(index, newValue)
                    },
                    isError = otpError,
                    modifier = Modifier.weight(1f),
                    colorScheme = colorScheme
                )
            }
        }

        // Error Message
        if (otpError) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    otpErrorMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Resend Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(
                1.dp,
                colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Didn't receive the code?",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
                if (resendCountdown > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            "${resendCountdown}s",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                } else {
                    TextButton(
                        onClick = onResendOtp,
                        enabled = !isRequestingOtp,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
                        )
                    ) {
                        if (isRequestingOtp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resend Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Email info
        Text(
            "Code sent to ${email.take(4)}****${email.takeLast(5)}",
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ===================== SET NEW PASSWORD FORM =====================

@Composable
fun SetNewPasswordForm(
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isNewPasswordVisible: Boolean,
    onNewPasswordVisibilityToggle: () -> Unit,
    isConfirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    isPasswordValid: Boolean,
    doPasswordsMatch: Boolean,
    isResetting: Boolean,
    isSuccess: Boolean,
    onReset: () -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // New Password
        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New Password", color = colorScheme.onSurfaceVariant) },
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
                focusedBorderColor = if (newPassword.isNotEmpty() && !isPasswordValid)
                    MaterialTheme.colorScheme.error
                else
                    colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                cursorColor = colorScheme.primary
            ),
            visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (newPassword.isNotEmpty() && isPasswordValid) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Valid password",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = onNewPasswordVisibilityToggle) {
                        Icon(
                            if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isNewPasswordVisible) "Hide password" else "Show password",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            supportingText = {
                if (newPassword.isNotEmpty() && !isPasswordValid) {
                    Text(
                        "8+ chars with uppercase & number",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }
        )

        // Password Requirements
        if (newPassword.isNotEmpty() && !isPasswordValid) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ResetPasswordRequirementItem(
                        text = "At least 8 characters",
                        isMet = newPassword.length >= 8,
                        colorScheme = colorScheme
                    )
                    ResetPasswordRequirementItem(
                        text = "One uppercase letter",
                        isMet = Regex("[A-Z]").containsMatchIn(newPassword),
                        colorScheme = colorScheme
                    )
                    ResetPasswordRequirementItem(
                        text = "One number",
                        isMet = Regex("[0-9]").containsMatchIn(newPassword),
                        colorScheme = colorScheme
                    )
                }
            }
        }

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
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                    IconButton(onClick = onConfirmPasswordVisibilityToggle) {
                        Icon(
                            if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password",
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

        Spacer(modifier = Modifier.height(8.dp))

        // Reset Button
        Button(
            onClick = onReset,
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
            enabled = isPasswordValid && doPasswordsMatch && !isResetting && !isSuccess
        ) {
            if (isResetting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Resetting...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            } else if (isSuccess) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Success!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            } else {
                Icon(
                    Icons.Default.LockReset,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Reset Password", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ===================== OTP INPUT FIELD =====================

@Composable
fun OtpInputFieldEnhanced(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurface
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        singleLine = true,
        maxLines = 1,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else colorScheme.primary,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else colorScheme.outlineVariant,
            focusedContainerColor = if (isError)
                MaterialTheme.colorScheme.error.copy(alpha = 0.04f)
            else
                colorScheme.primary.copy(alpha = 0.04f),
            unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            cursorColor = colorScheme.primary,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.04f),
            errorTextColor = MaterialTheme.colorScheme.error
        ),
        isError = isError,
        placeholder = {
            Text(
                "•",
                fontSize = 20.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty() && !isError) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF10B981)
                )
            }
        }
    )
}

// ===================== PASSWORD REQUIREMENT ITEM =====================

@Composable
fun ResetPasswordRequirementItem(
    text: String,
    isMet: Boolean,
    colorScheme: ColorScheme
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
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
                modifier = Modifier.size(10.dp),
                tint = if (isMet) Color(0xFF10B981) else colorScheme.onSurfaceVariant
            )
        }
        Text(
            text,
            fontSize = 12.sp,
            color = if (isMet) colorScheme.onSurface else colorScheme.onSurfaceVariant,
            fontWeight = if (isMet) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ===================== ENUMS =====================

enum class ResetStep {
    REQUEST_OTP,
    VERIFY_OTP,
    SET_NEW_PASSWORD
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun ResetPasswordScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        ResetPasswordScreen()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun ResetPasswordScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        ResetPasswordScreen()
    }
}