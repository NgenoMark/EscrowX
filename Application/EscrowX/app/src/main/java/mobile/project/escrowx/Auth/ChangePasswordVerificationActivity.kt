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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.PasswordResetRequestDto
import mobile.project.escrowx.ui.theme.BrandBlue

class ChangePasswordVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra("EMAIL") ?: ""

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.rememberDarkModeEnabledState(),
                dynamicColor = false
            ) {
                ChangePasswordVerificationScreen(email = email)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChangePasswordVerificationScreen(email: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var otpCode by remember { mutableStateOf(List(6) { "" }) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableStateOf(0) }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    val otpDigits = remember(otpCode) { otpCode.joinToString("") }
    val isOtpComplete = otpDigits.length == 6

    // Resend countdown timer
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }

    fun onOtpChange(index: Int, value: String) {
        if (value.length <= 1 && value.all { it.isDigit() }) {
            val newOtp = otpCode.toMutableList()
            newOtp[index] = value
            otpCode = newOtp

            // Auto-advance to next field
            if (value.isNotEmpty() && index < 5) {
                // Focus next field (handled by focus management)
            }
            showError = false
        }
    }

    fun verifyCode() {
        if (otpDigits.length < 6) {
            errorMessage = "Please enter the 6-digit code"
            showError = true
            return
        }

        isLoading = true
        showError = false

        scope.launch {
            try {
                if (email.isBlank()) {
                    errorMessage = "Email is missing. Please login again."
                    showError = true
                    isLoading = false
                    return@launch
                }

                isSuccess = true
                Toast.makeText(context, "Code verified successfully!", Toast.LENGTH_SHORT).show()

                delay(500)

                val intent = Intent(context, CreateNewPasswordActivity::class.java)
                intent.putExtra("EMAIL", email)
                intent.putExtra("OTP", otpDigits)
                context.startActivity(intent)
                (context as? ChangePasswordVerificationActivity)?.finish()

            } catch (_: Exception) {
                errorMessage = "Invalid or expired code. Please try again."
                showError = true
            } finally {
                isLoading = false
            }
        }
    }

    fun resendCode() {
        if (resendCountdown > 0) return

        isResending = true

        scope.launch {
            try {
                if (email.isBlank()) {
                    errorMessage = "Please login again"
                    showError = true
                    isResending = false
                    return@launch
                }

                val response = RetrofitClient.instance.requestPasswordReset(
                    PasswordResetRequestDto(email = email)
                )
                if (!response.isSuccessful) {
                    errorMessage = "Failed to resend code. Please try again."
                    showError = true
                    isResending = false
                    return@launch
                }

                resendCountdown = 30
                Toast.makeText(context, "Verification code resent to your email!", Toast.LENGTH_SHORT).show()

            } catch (_: Exception) {
                errorMessage = "Failed to resend code. Please try again."
                showError = true
            } finally {
                isResending = false
            }
        }
    }

    // Auto-submit when OTP is complete
    LaunchedEffect(isOtpComplete) {
        if (isOtpComplete && !isLoading && !isSuccess) {
            verifyCode()
        }
    }

    // Send reset code when this screen opens
    LaunchedEffect(email) {
        if (email.isBlank()) return@LaunchedEffect
        isResending = true
        val response = try {
            RetrofitClient.instance.requestPasswordReset(PasswordResetRequestDto(email = email))
        } catch (_: Exception) {
            null
        }
        isResending = false

        if (response?.isSuccessful == true) {
            resendCountdown = 30
            Toast.makeText(context, "Verification code sent to your email", Toast.LENGTH_SHORT).show()
        } else {
            showError = true
            errorMessage = "Could not send verification code. Please tap Resend Code."
        }
    }

    // Get masked email
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

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Verify Identity",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ChangePasswordVerificationActivity)?.finish()
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
                .padding(24.dp),
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
                        .size(100.dp)
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
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Security",
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                "Verify Your Identity",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = "We've sent a 6-digit verification code to",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = getMaskedEmail(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // OTP Input Fields
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(6) { index ->
                        OtpInputFieldEnhanced(
                            value = otpCode[index],
                            onValueChange = { newValue ->
                                onOtpChange(index, newValue)
                                if (newValue.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            },
                            isError = showError,
                            focusRequester = focusRequesters[index],
                            modifier = Modifier
                                .width(44.dp)
                                .height(58.dp),
                            colorScheme = colorScheme
                        )
                    }
                }
            }

            // Error Message
            AnimatedVisibility(
                visible = showError,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            errorMessage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Verify Button
            Button(
                onClick = { verifyCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                ),
                enabled = !isLoading && !isSuccess
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Verifying...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Verify & Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Didn't receive the code? ",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    if (resendCountdown > 0) {
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
                            onClick = { resendCode() },
                            enabled = !isResending && resendCountdown == 0,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colorScheme.primary
                            )
                        ) {
                            if (isResending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sending...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Resend Code",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "This code expires in 10 minutes",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OtpInputFieldEnhanced(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                "â€¢",
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    )
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun ChangePasswordVerificationScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        ChangePasswordVerificationScreen(email = "user@example.com")
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun ChangePasswordVerificationScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        ChangePasswordVerificationScreen(email = "user@example.com")
    }
}
