package mobile.project.escrowx.auth
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.auth.SessionManager

class ChangePasswordVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra("EMAIL") ?: ""

        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                ChangePasswordVerificationScreen(email = email)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordVerificationScreen(email: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var otpCode by remember { mutableStateOf(List(6) { "" }) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    val otpDigits = remember(otpCode) { otpCode.joinToString("") }

    fun onOtpChange(index: Int, value: String) {
        val newOtp = otpCode.toMutableList()
        newOtp[index] = value.takeLast(1)
        otpCode = newOtp
    }

    fun verifyCode() {
        if (otpDigits.length < 6) {
            toastMessage = "Please enter the 6-digit code"
            showToast = true
            return
        }

        isLoading = true

        scope.launch {
            try {
                val token = session.getAccessToken()
                val userEmail = session.getEmail()

                if (token.isNullOrBlank() || userEmail.isNullOrBlank()) {
                    toastMessage = "Session expired. Please login again."
                    showToast = true
                    isLoading = false
                    return@launch
                }

                delay(1500)

                toastMessage = "Code verified successfully!"
                showToast = true

                delay(500)

                val intent = Intent(context, CreateNewPasswordActivity::class.java)
                intent.putExtra("EMAIL", email)
                intent.putExtra("OTP", otpDigits)
                context.startActivity(intent)
                (context as? ChangePasswordVerificationActivity)?.finish()

            } catch (_: Exception) {
                toastMessage = "Invalid or expired code. Please try again."
                showToast = true
            } finally {
                isLoading = false
            }
        }
    }

    fun resendCode() {
        isResending = true

        scope.launch {
            try {
                val userEmail = session.getEmail()
                if (userEmail.isNullOrBlank()) {
                    toastMessage = "Please login again"
                    showToast = true
                    isResending = false
                    return@launch
                }

                delay(1000)

                toastMessage = "Verification code resent to your email!"
                showToast = true

            } catch (_: Exception) {
                toastMessage = "Failed to resend code. Please try again."
                showToast = true
            } finally {
                isResending = false
            }
        }
    }

    LaunchedEffect(showToast) {
        if (showToast) {
            delay(3000)
            showToast = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Verify Identity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ChangePasswordVerificationActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00236F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE7EEFE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = "Verified",
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF00236F)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                "Confirm Your Code",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF151C27),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = "We've sent a 6-digit verification code to ${email.take(4)}****${email.takeLast(5)}. Please enter it below to continue.",
                fontSize = 14.sp,
                color = Color(0xFF444651),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // OTP Input Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(6) { index ->
                    OtpInputField(
                        value = otpCode[index],
                        onValueChange = { newValue ->
                            if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                onOtpChange(index, newValue)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Verify Button
            Button(
                onClick = { verifyCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00236F),
                    contentColor = Color.White
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify & Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resend Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Didn't receive the code? ",
                    fontSize = 14.sp,
                    color = Color(0xFF444651)
                )
                TextButton(
                    onClick = { resendCode() },
                    enabled = !isResending
                ) {
                    if (isResending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF00236F)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sending...", color = Color(0xFF00236F))
                    } else {
                        Text("Resend Code", color = Color(0xFF00236F), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Toast message
            if (showToast) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF2A313D),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        toastMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 12.sp,
                        color = Color(0xFFEBF1FF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00236F),
            unfocusedBorderColor = Color(0xFFC5C5D3),
            focusedContainerColor = Color(0xFFF0F3FF),
            unfocusedContainerColor = Color(0xFFF0F3FF)
        )
    )
}