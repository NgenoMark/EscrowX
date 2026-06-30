package mobile.project.escrowx.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import coil.compose.AsyncImage
import coil.request.ImageRequest

class SignUpActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                SignUpScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val kenyaCodePrefix = "+254"

    BackHandler {
        context.startActivity(Intent(context, LoginActivity::class.java))
        (context as? SignUpActivity)?.finish()
    }

    var displayName by remember { mutableStateOf("") }
    var phoneSuffix by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var businessName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("BUYER") }
    var isLoading by remember { mutableStateOf(false) }

    // Password validation
    val hasLength = password.length >= 8
    val hasUpper = Regex("[A-Z]").containsMatchIn(password)
    val hasNumber = Regex("[0-9]").containsMatchIn(password)
    val isPasswordValid = hasLength && hasUpper && hasNumber

    // Form validation
    val hasValidKenyanSubscriberNumber = phoneSuffix.filter { it.isDigit() }.length == 9
    val isFormValid = displayName.isNotBlank() &&
            hasValidKenyanSubscriberNumber &&
            email.isNotBlank() &&
            isPasswordValid

    val fullPhoneNumber = "$kenyaCodePrefix${phoneSuffix.filter { it.isDigit() }}"

    fun performSignUp() {
        val trimmedName = displayName.trim()
        val trimmedPhone = fullPhoneNumber
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedBusiness = businessName.trim().ifEmpty { null }

        if (trimmedName.isEmpty() || trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (phoneSuffix.filter { it.isDigit() }.length != 9) {
            Toast.makeText(context, "Enter a valid phone number after +254", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isPasswordValid) {
            Toast.makeText(context, "Password must be at least 8 characters with uppercase and number", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        val registrationPayload = RegisterRequest(
            phone = trimmedPhone,
            email = trimmedEmail,
            password = trimmedPassword,
            displayName = trimmedName,
            businessName = trimmedBusiness,
            role = selectedRole
        )

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.registerUser(registrationPayload)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(context, "🎉 Registration Successful!", Toast.LENGTH_LONG).show()
                        context.startActivity(Intent(context, VerificationActivity::class.java).apply {
                            putExtra("EMAIL", trimmedEmail)
                        })
                        (context as? SignUpActivity)?.finish()
                    } else {
                        val errorMsg = response.errorBody()?.string()?.take(100) ?: "Registration failed. Account might exist."
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .background(colorScheme.background)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.74f)
                    .height(84.dp),
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("asset:///EscrowX.svg")
                        .crossfade(true)
                        .build(),
                    contentDescription = "EscrowX logo",
                    error = painterResource(id = R.drawable.escrowx_logo1),
                    fallback = painterResource(id = R.drawable.escrowx_logo1),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ===== TITLE =====
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Create Account",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Start your secure escrow journey today",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ===== FORM =====
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Full Name
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
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
                        ),
                        isError = displayName.isNotBlank() && displayName.length < 3,
                        supportingText = {
                            if (displayName.isNotBlank() && displayName.length < 3) {
                                Text(
                                    "Name must be at least 3 characters",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    )

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
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

                    // Phone - with static +254 prefix
                    OutlinedTextField(
                        value = phoneSuffix,
                        onValueChange = {
                            phoneSuffix = it.filter { char -> char.isDigit() }.take(9)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone Number", color = colorScheme.onSurfaceVariant) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                        },
                        placeholder = { Text("700000000", color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            cursorColor = colorScheme.primary
                        ),
                        prefix = {
                            Text(
                                "+254 ",
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                        },
                        supportingText = {
                            Text(
                                if (hasValidKenyanSubscriberNumber) "✓ Valid Kenya number" else "Enter 9 digits after +254",
                                color = if (hasValidKenyanSubscriberNumber) Color(0xFF10B981) else colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
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
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
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
                                    "8+ chars with uppercase & number",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            } else if (password.isNotEmpty() && isPasswordValid) {
                                Text(
                                    "✓ Strong password",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    )

                    // Password Requirements (shown when typing)
                    if (password.isNotEmpty() && !isPasswordValid) {
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
                                PasswordRequirementItem(
                                    text = "At least 8 characters",
                                    isMet = hasLength,
                                    colorScheme = colorScheme
                                )
                                PasswordRequirementItem(
                                    text = "One uppercase letter",
                                    isMet = hasUpper,
                                    colorScheme = colorScheme
                                )
                                PasswordRequirementItem(
                                    text = "One number",
                                    isMet = hasNumber,
                                    colorScheme = colorScheme
                                )
                            }
                        }
                    }

                    // Business Name (Optional)
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Business Name (Optional)", color = colorScheme.onSurfaceVariant) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Store,
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

                    // Role Selection
                    RoleSelectionChips(
                        selectedRole = selectedRole,
                        onRoleChange = { selectedRole = it },
                        colorScheme = colorScheme
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sign Up Button
                    Button(
                        onClick = { performSignUp() },
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
                        enabled = !isLoading && isFormValid
                    ) {
                        if (isLoading) {
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

                Spacer(modifier = Modifier.height(20.dp))

                // ===== LOGIN LINK =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Already have an account?",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            (context as? SignUpActivity)?.finish()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Text(
                            "Sign In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ===== FOOTER =====
            Text(
                "Secured by EscrowX",
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

// ===================== ROLE SELECTION CHIPS =====================

@Composable
fun RoleSelectionChips(
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "I want to register as a",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoleChipEnhanced(
                modifier = Modifier.weight(1f),
                text = "Buyer",
                icon = Icons.Default.ShoppingCart,
                selected = selectedRole == "BUYER",
                onClick = { onRoleChange("BUYER") },
                colorScheme = colorScheme
            )
            RoleChipEnhanced(
                modifier = Modifier.weight(1f),
                text = "Seller",
                icon = Icons.Default.Storefront,
                selected = selectedRole == "SELLER",
                onClick = { onRoleChange("SELLER") },
                colorScheme = colorScheme
            )
        }
    }
}

@Composable
fun RoleChipEnhanced(
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface
            )
        }
    }
}

// ===================== PASSWORD REQUIREMENT ITEM =====================

@Composable
fun PasswordRequirementItem(
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

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SignUpScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        SignUpScreen()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun SignUpScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        SignUpScreen()
    }
}