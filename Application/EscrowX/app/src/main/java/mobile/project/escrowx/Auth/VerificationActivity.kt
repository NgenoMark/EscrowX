package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent as AndroidKeyEvent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.notifications.FcmTokenRegistrar
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import mobile.project.escrowx.ui.theme.BrandBlue

class VerificationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("EMAIL") ?: ""

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                VerificationScreen(email = email)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun VerificationScreen(email: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var otpCode by remember { mutableStateOf(List(6) { "" }) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var resendCountdown by remember { mutableStateOf(0) }
    val otpFocusRequesters = remember { List(6) { FocusRequester() } }

    val otpDigits = remember(otpCode) { otpCode.joinToString("") }
    val isOtpComplete = otpDigits.length == 6

    fun verifyCode() {
        if (otpDigits.length < 6) {
            errorMessage = "Please enter the 6-digit code"
            showError = true
            return
        }

        isLoading = true
        showError = false

        val confirmPayload = ConfirmRequest(email = email, otp = otpDigits)

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.confirmAccount(confirmPayload)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null && response.body()!!.confirmed) {
                        val accountStatus = response.body()!!.status.uppercase()
                        isSuccess = true

                        FcmTokenRegistrar.registerByEmail(context, email)

                        if (accountStatus == "PENDING_ADMIN_APPROVAL") {
                            Toast.makeText(
                                context,
                                "Seller account verified. Waiting for admin approval before login.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "🎉 Account Activated Successfully!", Toast.LENGTH_LONG).show()
                        }

                        delay(800)

                        context.startActivity(Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
                        errorMessage = "Invalid or expired verification code"
                        showError = true
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Network error: ${e.message}"
                    showError = true
                    Toast.makeText(context, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Auto-verify when OTP is complete
    LaunchedEffect(isOtpComplete) {
        if (isOtpComplete && !isLoading && !isSuccess) {
            verifyCode()
        }
    }

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
            showError = false
        }
    }

    fun resendCode() {
        if (resendCountdown > 0) return

        isResending = true

        scope.launch(Dispatchers.IO) {
            try {
                // Simulate API call
                delay(1000)

                withContext(Dispatchers.Main) {
                    resendCountdown = 30
                    Toast.makeText(context, "Verification code resent to your email!", Toast.LENGTH_SHORT).show()
                    // Reset OTP fields
                    otpCode = List(6) { "" }
                    showError = false
                    isResending = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isResending = false
                    Toast.makeText(context, "Failed to resend code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Get masked email
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
                // ===== ANIMATED ICON =====
                AnimatedContent(
                    targetState = isSuccess,
                    transitionSpec = {
                        fadeIn() + scaleIn() with fadeOut() + scaleOut()
                    }
                ) { success ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
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
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Verification",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== TITLE =====
                AnimatedContent(
                    targetState = isSuccess,
                    transitionSpec = {
                        fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                    }
                ) { success ->
                    if (success) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Account Verified! 🎉",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Your account has been successfully activated",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Verify Your Account",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Enter the 6-digit code sent to",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                getMaskedEmail(email),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isSuccess) {
                    // ===== OTP INPUT FIELDS =====
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(6) { index ->
                                OtpInputFieldEnhanced(
                                    value = otpCode[index],
                                    onValueChange = { newValue ->
                                        val digit = newValue.lastOrNull()?.takeIf { it.isDigit() }?.toString().orEmpty()
                                        onOtpChange(index, digit)
                                        if (digit.isNotEmpty() && index < otpFocusRequesters.lastIndex) {
                                            otpFocusRequesters[index + 1].requestFocus()
                                        }
                                    },
                                    onBackspaceEmpty = {
                                        if (index > 0) {
                                            onOtpChange(index - 1, "")
                                            otpFocusRequesters[index - 1].requestFocus()
                                        }
                                    },
                                    isError = showError,
                                    modifier = Modifier
                                        .width(52.dp)
                                        .focusRequester(otpFocusRequesters[index]),
                                    imeAction = if (index == otpFocusRequesters.lastIndex) ImeAction.Done else ImeAction.Next,
                                    colorScheme = colorScheme
                                )
                            }
                        }
                    }

                    // ===== ERROR MESSAGE =====
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

                    // ===== VERIFY BUTTON =====
                    Button(
                        onClick = { verifyCode() },
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
                        enabled = !isLoading && !isSuccess
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        } else {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verify Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ===== RESEND SECTION =====
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
                                    onClick = { resendCode() },
                                    enabled = !isResending,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // ===== SECURITY NOTE =====
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
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back to Login", fontSize = 13.sp)
                    }
                } else {
                    // ===== SUCCESS STATE =====
                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "✓ Account Active",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                "You can now start using EscrowX",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Go to Login",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
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

// ===================== OTP INPUT FIELD =====================

@Composable
fun OtpInputFieldEnhanced(
    value: String,
    onValueChange: (String) -> Unit,
    onBackspaceEmpty: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    colorScheme: ColorScheme
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onKeyEvent { keyEvent ->
            if (
                keyEvent.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN &&
                keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DEL &&
                value.isEmpty()
            ) {
                onBackspaceEmpty()
                true
            } else {
                false
            }
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
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
        }
    )
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun VerificationScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        VerificationScreen(email = "user@example.com")
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun VerificationScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        VerificationScreen(email = "user@example.com")
    }
}