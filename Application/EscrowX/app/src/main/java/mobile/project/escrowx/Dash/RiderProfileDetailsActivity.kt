package mobile.project.escrowx.dash

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.project.escrowx.RiderProfileResponse
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UpdateProfileRequest
import mobile.project.escrowx.UpdateRiderProfileRequest
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager
import mobile.project.escrowx.ui.theme.BrandBlue
import java.text.SimpleDateFormat
import java.util.*

class RiderProfileDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                RiderProfileDetailsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderProfileDetailsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var riderProfile by remember { mutableStateOf<RiderProfileResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editOperationArea by remember { mutableStateOf("") }
    var editLicenseNumber by remember { mutableStateOf("") }
    var editVehicleType by remember { mutableStateOf("") }
    var editVehiclePlate by remember { mutableStateOf("") }
    var editRiderStatus by remember { mutableStateOf("AVAILABLE") }
    var isRiderStatusExpanded by remember { mutableStateOf(false) }
    val riderStatusOptions = listOf("AVAILABLE", "BUSY", "OFFLINE")

    suspend fun loadProfileData() {
        val token = session.getAccessToken()
        val userId = session.getUserId()
        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            errorMessage = "Not logged in"
            isLoading = false
            return
        }

        try {
            val api = RetrofitClient.authenticated(token)
            val userResponse = withContext(Dispatchers.IO) {
                api.getUserById(userId)
            }

            if (userResponse.isSuccessful) {
                profile = userResponse.body()
                editDisplayName = profile?.displayName ?: ""
                editPhone = profile?.phone ?: ""
                editAddress = profile?.address ?: ""
            } else {
                errorMessage = "Failed to load profile"
            }

            val riderResponse = withContext(Dispatchers.IO) {
                api.getRiderProfileByUserId(userId)
            }

            if (riderResponse.isSuccessful) {
                riderProfile = riderResponse.body()
                editOperationArea = riderProfile?.operationArea ?: ""
                editLicenseNumber = riderProfile?.licenseNumber ?: ""
                editVehicleType = riderProfile?.vehicleType ?: ""
                editVehiclePlate = riderProfile?.vehiclePlate ?: ""
                editRiderStatus = riderProfile?.riderStatus ?: "AVAILABLE"
            }
        } catch (_: Exception) {
            errorMessage = "Network error"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(refreshKey) {
        loadProfileData()
    }

    val riderName = profile?.displayName?.ifBlank { null }
        ?: profile?.email?.substringBefore("@")
        ?: "Rider"
    val riderInitials = riderName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(Locale.getDefault()) }
        .ifBlank { "RD" }
    val isActive = profile?.status.equals("ACTIVE", ignoreCase = true) ?: true
    val formattedDate = profile?.createdAt?.let { formatRiderProfileDate(it) } ?: "N/A"

    fun saveProfileChanges() {
        if (editDisplayName.isBlank()) {
            Toast.makeText(context, "Display name is required", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                isSaving = true
                val token = session.getAccessToken()
                val userId = session.getUserId()
                if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val api = RetrofitClient.authenticated(token)
                val profileResponse = withContext(Dispatchers.IO) {
                    api.updateProfile(
                        userId,
                        UpdateProfileRequest(
                            displayName = editDisplayName.trim(),
                            phone = editPhone.trim().takeIf { it.isNotBlank() },
                            address = editAddress.trim().takeIf { it.isNotBlank() }
                        )
                    )
                }

                if (!profileResponse.isSuccessful || profileResponse.body() == null) {
                    val err = profileResponse.errorBody()?.string()?.take(180)
                    Toast.makeText(context, err ?: "Failed to update profile", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val riderResponse = withContext(Dispatchers.IO) {
                    api.updateRiderProfile(
                        userId,
                        UpdateRiderProfileRequest(
                            operationArea = editOperationArea.trim().takeIf { it.isNotBlank() },
                            licenseNumber = editLicenseNumber.trim().takeIf { it.isNotBlank() },
                            vehicleType = editVehicleType.trim().takeIf { it.isNotBlank() },
                            vehiclePlate = editVehiclePlate.trim().takeIf { it.isNotBlank() },
                            riderStatus = editRiderStatus.trim().takeIf { it.isNotBlank() }
                        )
                    )
                }

                if (!riderResponse.isSuccessful || riderResponse.body() == null) {
                    val err = riderResponse.errorBody()?.string()?.take(180)
                    Toast.makeText(context, err ?: "Failed to update rider profile", Toast.LENGTH_LONG).show()
                    return@launch
                }

                profile = profileResponse.body()
                riderProfile = riderResponse.body()
                editDisplayName = profile?.displayName ?: ""
                editPhone = profile?.phone ?: ""
                editAddress = profile?.address ?: ""
                editOperationArea = riderProfile?.operationArea ?: ""
                editLicenseNumber = riderProfile?.licenseNumber ?: ""
                editVehicleType = riderProfile?.vehicleType ?: ""
                editVehiclePlate = riderProfile?.vehiclePlate ?: ""
                editRiderStatus = riderProfile?.riderStatus ?: "AVAILABLE"
                isEditMode = false
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rider Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        errorMessage = null
                        riderProfile = null
                        profile = null
                        refreshKey += 1
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Loading profile...",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        errorMessage!!,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== PROFILE HEADER CARD =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                border = BorderStroke(
                    1.dp,
                    colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar with gradient
                    Box(
                        modifier = Modifier
                            .size(80.dp)
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
                        Text(
                            riderInitials,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Name & Role
                    Text(
                        riderName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isActive)
                            Color(0xFF10B981).copy(alpha = 0.12f)
                        else
                            Color(0xFFDC2626).copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color(0xFF10B981) else Color(0xFFDC2626))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isActive) "Active" else "Inactive",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) Color(0xFF10B981) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // ===== PROFILE INFORMATION =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                border = BorderStroke(
                    1.dp,
                    colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Profile Information",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileDetailRow(
                        icon = Icons.Default.Badge,
                        label = "Role",
                        value = profile?.role ?: "RIDER",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Shield,
                        label = "Status",
                        value = profile?.status ?: "ACTIVE",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Person,
                        label = "Display Name",
                        value = profile?.displayName ?: riderProfile?.displayName ?: "-",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = profile?.email ?: "-",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = profile?.phone ?: riderProfile?.phone ?: "Not set",
                        colorScheme = colorScheme
                    )
                    if (isEditMode) {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Display Name") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            enabled = !isSaving
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Phone") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            enabled = !isSaving
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Address") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                            },
                            maxLines = 3,
                            enabled = !isSaving
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editDisplayName = profile?.displayName ?: ""
                                    editPhone = profile?.phone ?: ""
                                    editAddress = profile?.address ?: ""
                                    editOperationArea = riderProfile?.operationArea ?: ""
                                    editLicenseNumber = riderProfile?.licenseNumber ?: ""
                                    editVehicleType = riderProfile?.vehicleType ?: ""
                                    editVehiclePlate = riderProfile?.vehiclePlate ?: ""
                                    editRiderStatus = riderProfile?.riderStatus ?: "AVAILABLE"
                                    isEditMode = false
                                },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = { saveProfileChanges() },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (isSaving) "Saving..." else "Save")
                            }
                        }
                    } else {
                        ProfileDetailRow(
                            icon = Icons.Default.LocationOn,
                            label = "Address",
                            value = profile?.address ?: "Not set",
                            colorScheme = colorScheme
                        )
                    }
                    ProfileDetailRow(
                        icon = Icons.Default.Schedule,
                        label = "Member Since",
                        value = formattedDate,
                        colorScheme = colorScheme
                    )
                }
            }

            // ===== RIDER PROFILE INFORMATION =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                border = BorderStroke(
                    1.dp,
                    colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Rider Profile Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileDetailRow(
                        icon = Icons.Default.Route,
                        label = "Operation Area",
                        value = riderProfile?.operationArea ?: "Not set",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.CreditCard,
                        label = "License Number",
                        value = riderProfile?.licenseNumber ?: "Not set",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.DirectionsBike,
                        label = "Vehicle Type",
                        value = riderProfile?.vehicleType ?: "Not set",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.ConfirmationNumber,
                        label = "Vehicle Plate",
                        value = riderProfile?.vehiclePlate ?: "Not set",
                        colorScheme = colorScheme
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.LocalShipping,
                        label = "Rider Status",
                        value = riderProfile?.riderStatus ?: "AVAILABLE",
                        colorScheme = colorScheme
                    )

                    if (isEditMode) {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editOperationArea,
                            onValueChange = { editOperationArea = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Operation Area") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                            enabled = !isSaving
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editLicenseNumber,
                            onValueChange = { editLicenseNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("License Number") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                            enabled = !isSaving
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editVehicleType,
                            onValueChange = { editVehicleType = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Vehicle Type") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                            enabled = !isSaving
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editVehiclePlate,
                            onValueChange = { editVehiclePlate = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Vehicle Plate") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                            enabled = !isSaving
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = isRiderStatusExpanded,
                            onExpandedChange = { if (!isSaving) isRiderStatusExpanded = !isRiderStatusExpanded }
                        ) {
                            OutlinedTextField(
                                value = editRiderStatus,
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = !isSaving
                                    ),
                                label = { Text("Rider Status") },
                                singleLine = true,
                                readOnly = true,
                                leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRiderStatusExpanded) },
                                enabled = !isSaving
                            )

                            ExposedDropdownMenu(
                                expanded = isRiderStatusExpanded,
                                onDismissRequest = { isRiderStatusExpanded = false }
                            ) {
                                riderStatusOptions.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status) },
                                        onClick = {
                                            editRiderStatus = status
                                            isRiderStatusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== ACCOUNT STATS =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                border = BorderStroke(
                    1.dp,
                    colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Account Statistics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = "Total Deliveries",
                            value = "0",
                            icon = Icons.Default.LocalShipping,
                            color = Color(0xFF3B82F6),
                            colorScheme = colorScheme
                        )
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = "Completed",
                            value = "0",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF10B981),
                            colorScheme = colorScheme
                        )
                        StatItem(
                            modifier = Modifier.weight(1f),
                            label = "Active",
                            value = "0",
                            icon = Icons.Default.Pending,
                            color = Color(0xFFF59E0B),
                            colorScheme = colorScheme
                        )
                    }
                }
            }

            // ===== QUICK ACTIONS =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                border = BorderStroke(
                    1.dp,
                    colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Quick Actions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    QuickActionItem(
                        icon = Icons.Default.Edit,
                        label = "Edit Profile",
                        onClick = { isEditMode = true },
                        colorScheme = colorScheme
                    )
                    QuickActionItem(
                        icon = Icons.Default.Notifications,
                        label = "Notifications",
                        onClick = { /* Navigate to notifications */ },
                        colorScheme = colorScheme
                    )
                    QuickActionItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { /* Navigate to settings */ },
                        colorScheme = colorScheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ===================== COMPONENTS =====================

@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                label,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontSize = 14.sp,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Optional: Show a verified icon for certain fields
        if (label == "Email" && value != "-" && value != "Not set") {
            Icon(
                Icons.Default.Verified,
                contentDescription = "Verified",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun StatItem(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    colorScheme: ColorScheme
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                label,
                fontSize = 10.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant
            )
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===================== HELPER FUNCTIONS =====================

fun formatRiderProfileDate(dateString: String): String {
    return try {
        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        inputFormats.forEach { pattern ->
            runCatching {
                val parser = SimpleDateFormat(pattern, Locale.getDefault())
                parser.isLenient = false
                parser.parse(dateString)
            }.getOrNull()?.let { parsed ->
                return output.format(parsed)
            }
        }

        dateString.substringBefore("T").takeIf { it.length >= 10 } ?: dateString
    } catch (_: Exception) {
        dateString
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderProfileDetailsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        RiderProfileDetailsScreen(onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun RiderProfileDetailsScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        RiderProfileDetailsScreen(onBack = {})
    }
}