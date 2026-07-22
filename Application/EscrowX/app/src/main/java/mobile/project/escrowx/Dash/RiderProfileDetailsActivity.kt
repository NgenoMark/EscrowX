package mobile.project.escrowx.dash

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                darkTheme = ThemePreferenceManager.rememberDarkModeEnabledState(),
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

    // Edit fields
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
                    if (!isEditMode) {
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
            ProfileHeaderCard(
                riderName = riderName,
                riderInitials = riderInitials,
                isActive = isActive,
                colorScheme = colorScheme
            )

            // ===== EDIT MODE TOGGLE =====
            if (!isEditMode) {
                // View Mode - Show Edit Button
                EditProfileButton(
                    onClick = { isEditMode = true },
                    colorScheme = colorScheme
                )
            }

            // ===== PROFILE INFORMATION =====
            ProfileInfoSection(
                profile = profile,
                riderProfile = riderProfile,
                formattedDate = formattedDate,
                isEditMode = isEditMode,
                isSaving = isSaving,
                editDisplayName = editDisplayName,
                onEditDisplayNameChange = { editDisplayName = it },
                editPhone = editPhone,
                onEditPhoneChange = { editPhone = it },
                editAddress = editAddress,
                onEditAddressChange = { editAddress = it },
                editOperationArea = editOperationArea,
                onEditOperationAreaChange = { editOperationArea = it },
                editLicenseNumber = editLicenseNumber,
                onEditLicenseNumberChange = { editLicenseNumber = it },
                editVehicleType = editVehicleType,
                onEditVehicleTypeChange = { editVehicleType = it },
                editVehiclePlate = editVehiclePlate,
                onEditVehiclePlateChange = { editVehiclePlate = it },
                editRiderStatus = editRiderStatus,
                onEditRiderStatusChange = { editRiderStatus = it },
                isRiderStatusExpanded = isRiderStatusExpanded,
                onRiderStatusExpandedChange = { isRiderStatusExpanded = it },
                riderStatusOptions = riderStatusOptions,
                onSave = { saveProfileChanges() },
                onCancel = {
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
                colorScheme = colorScheme
            )

            // ===== ACCOUNT STATS =====
            AccountStatsCard(colorScheme = colorScheme)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ===================== PROFILE HEADER =====================

@Composable
fun ProfileHeaderCard(
    riderName: String,
    riderInitials: String,
    isActive: Boolean,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with gradient ring
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                BrandBlue,
                                Color(0xFF7C3AED),
                                BrandBlue
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Name & Role
            Text(
                riderName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            Text(
                "Rider",
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // Status Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isActive)
                    Color(0xFF10B981).copy(alpha = 0.12f)
                else
                    Color(0xFFDC2626).copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    if (isActive) Color(0xFF10B981).copy(alpha = 0.2f)
                    else Color(0xFFDC2626).copy(alpha = 0.2f)
                )
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
}

// ===================== EDIT PROFILE BUTTON =====================

@Composable
fun EditProfileButton(
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        )
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Edit Profile",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ===================== PROFILE INFO SECTION =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoSection(
    profile: UserDetailsResponse?,
    riderProfile: RiderProfileResponse?,
    formattedDate: String,
    isEditMode: Boolean,
    isSaving: Boolean,
    editDisplayName: String,
    onEditDisplayNameChange: (String) -> Unit,
    editPhone: String,
    onEditPhoneChange: (String) -> Unit,
    editAddress: String,
    onEditAddressChange: (String) -> Unit,
    editOperationArea: String,
    onEditOperationAreaChange: (String) -> Unit,
    editLicenseNumber: String,
    onEditLicenseNumberChange: (String) -> Unit,
    editVehicleType: String,
    onEditVehicleTypeChange: (String) -> Unit,
    editVehiclePlate: String,
    onEditVehiclePlateChange: (String) -> Unit,
    editRiderStatus: String,
    onEditRiderStatusChange: (String) -> Unit,
    isRiderStatusExpanded: Boolean,
    onRiderStatusExpandedChange: (Boolean) -> Unit,
    riderStatusOptions: List<String>,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profile Information",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                if (isEditMode) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            "Editing",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Personal Information
            Text(
                "Personal Information",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (isEditMode) {
                OutlinedTextField(
                    value = editDisplayName,
                    onValueChange = onEditDisplayNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display Name") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = onEditPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editAddress,
                    onValueChange = onEditAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Address") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    maxLines = 3,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                ProfileDetailRow(
                    icon = Icons.Default.Person,
                    label = "Display Name",
                    value = profile?.displayName ?: "-",
                    colorScheme = colorScheme
                )
                ProfileDetailRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = profile?.phone ?: "Not set",
                    colorScheme = colorScheme
                )
                ProfileDetailRow(
                    icon = Icons.Default.LocationOn,
                    label = "Address",
                    value = profile?.address ?: "Not set",
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rider Details
            Text(
                "Rider Details",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant
            )

            if (isEditMode) {
                OutlinedTextField(
                    value = editOperationArea,
                    onValueChange = onEditOperationAreaChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Operation Area") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editLicenseNumber,
                    onValueChange = onEditLicenseNumberChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("License Number") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editVehicleType,
                    onValueChange = onEditVehicleTypeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Vehicle Type") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editVehiclePlate,
                    onValueChange = onEditVehiclePlateChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Vehicle Plate") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = isRiderStatusExpanded,
                    onExpandedChange = { if (!isSaving) onRiderStatusExpandedChange(!isRiderStatusExpanded) }
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
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isRiderStatusExpanded,
                        onDismissRequest = { onRiderStatusExpandedChange(false) }
                    ) {
                        riderStatusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    onEditRiderStatusChange(status)
                                    onRiderStatusExpandedChange(false)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save/Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onSave,
                        enabled = !isSaving && editDisplayName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes")
                        }
                    }
                }
            } else {
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
                ProfileDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Member Since",
                    value = formattedDate,
                    colorScheme = colorScheme
                )
            }
        }
    }
}

// ===================== PROFILE DETAIL ROW =====================

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
            .padding(vertical = 4.dp),
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

// ===================== ACCOUNT STATS CARD =====================

@Composable
fun AccountStatsCard(colorScheme: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
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
                StatItemCompact(
                    modifier = Modifier.weight(1f),
                    label = "Deliveries",
                    value = "0",
                    icon = Icons.Default.LocalShipping,
                    color = Color(0xFF3B82F6),
                    colorScheme = colorScheme
                )
                StatItemCompact(
                    modifier = Modifier.weight(1f),
                    label = "Completed",
                    value = "0",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF10B981),
                    colorScheme = colorScheme
                )
                StatItemCompact(
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
}

@Composable
fun StatItemCompact(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    colorScheme: ColorScheme
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
            }
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

// ===================== QUICK ACTIONS CARD =====================

@Composable
fun QuickActionsCard(
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Quick Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            QuickActionItemEnhanced(
                icon = Icons.Default.Edit,
                label = "Edit Profile",
                description = "Update your personal and rider information",
                onClick = onEditProfile,
                colorScheme = colorScheme
            )

            QuickActionItemEnhanced(
                icon = Icons.Default.Settings,
                label = "Settings",
                description = "App preferences and account settings",
                onClick = onOpenSettings,
                colorScheme = colorScheme
            )
        }
    }
}

@Composable
fun QuickActionItemEnhanced(
    icon: ImageVector,
    label: String,
    description: String,
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
