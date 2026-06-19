package mobile.project.escrowx.dash

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.R
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UpdateProfileRequest
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.ChangePasswordVerificationActivity
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.seller.SellerDashboardActivity
import mobile.project.escrowx.ui.components.*
import mobile.project.escrowx.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                ProfileScreenContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent() {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var shopLocation by remember { mutableStateOf("") }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveButtonText by remember { mutableStateOf("Save Changes") }
    var isSaveSuccess by remember { mutableStateOf(false) }

    var showImagePickerDialog by remember { mutableStateOf(false) }

    val userRole = session.getUserRole() ?: "BUYER"
    val isSeller = userRole.equals("SELLER", ignoreCase = true)

    // Load profile from backend
    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()
            if (!token.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        userProfile = response.body()
                        displayName = userProfile?.displayName ?: ""
                        fullName = userProfile?.displayName ?: userProfile?.email?.substringBefore("@") ?: ""
                        email = userProfile?.email ?: ""
                        phoneNumber = userProfile?.phone ?: ""
                        businessName = userProfile?.businessName ?: ""
                        address = userProfile?.address ?: ""
                        shopLocation = userProfile?.shopLocation ?: ""
                    } else {
                        errorMessage = "Failed to load profile: ${response.code()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Network error: ${e.message}"
                } finally {
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }
    }

    fun saveChanges() {
        if (fullName.isBlank()) {
            Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
            return
        }
        isSaving = true
        saveButtonText = "Updating..."

        scope.launch {
            try {
                val token = session.getAccessToken()
                val userId = session.getUserId()
                if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    isSaving = false
                    saveButtonText = "Save Changes"
                    return@launch
                }

                val request = UpdateProfileRequest(
                    displayName = fullName.takeIf { it.isNotBlank() },
                    phone = phoneNumber.takeIf { it.isNotBlank() },
                    businessName = businessName.takeIf { it.isNotBlank() },
                    address = if (isSeller) null else address.takeIf { it.isNotBlank() },
                    shopLocation = if (isSeller) shopLocation.takeIf { it.isNotBlank() } else null
                )

                val response = RetrofitClient.authenticated(token).updateProfile(userId, request)
                if (response.isSuccessful && response.body() != null) {
                    val updatedProfile = response.body()!!
                    userProfile = updatedProfile
                    displayName = updatedProfile.displayName ?: ""
                    fullName = updatedProfile.displayName ?: updatedProfile.email?.substringBefore("@") ?: ""
                    phoneNumber = updatedProfile.phone ?: ""
                    businessName = updatedProfile.businessName ?: ""
                    if (isSeller) {
                        shopLocation = updatedProfile.shopLocation ?: ""
                    } else {
                        address = updatedProfile.address ?: ""
                    }

                    saveButtonText = "Saved Successfully"
                    isSaveSuccess = true
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_LONG).show()
                    delay(2000)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Update failed"
                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
                saveButtonText = "Save Changes"
                delay(500)
                isSaveSuccess = false
            }
        }
    }

    // Camera & gallery launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.cacheDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) Toast.makeText(context, "Photo taken (save not implemented)", Toast.LENGTH_SHORT).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            Toast.makeText(context, "Image selected", Toast.LENGTH_SHORT).show()
        }
    }

    fun onEditImageClick() { showImagePickerDialog = true }
    fun onCameraClick() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile = createImageFile()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        showImagePickerDialog = false
    }
    fun onGalleryClick() {
        galleryLauncher.launch("image/*")
        showImagePickerDialog = false
    }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = {
                Text(
                    "Choose Option",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Select image from gallery or take a photo",
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { onGalleryClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Gallery", fontSize = 15.sp)
                    }
                    TextButton(
                        onClick = { onCameraClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Camera", fontSize = 15.sp)
                    }
                    TextButton(
                        onClick = { showImagePickerDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            if (activity.isTaskRoot) {
                                val fallbackTarget = if (isSeller) {
                                    SellerDashboardActivity::class.java
                                } else {
                                    BuyerDashboardActivity::class.java
                                }
                                navigateTab(context, fallbackTarget)
                            } else {
                                activity.finish()
                            }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        },
        bottomBar = { ProfileBottomNavigationBar(userRole = userRole) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { /* retry */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            ProfileHero(
                                displayName = displayName,
                                fullName = fullName,
                                role = userProfile?.role ?: "BUYER",
                                profileImageUri = profileImageUri,
                                onEditClick = { onEditImageClick() }
                            )
                        }

                        item {
                            Spacer(Modifier.height(20.dp))
                        }

                        item {
                            AccountInfoCard(
                                email = email,
                                phoneNumber = phoneNumber,
                                role = userProfile?.role ?: "BUYER",
                                status = userProfile?.status ?: "ACTIVE",
                                createdAt = userProfile?.createdAt ?: ""
                            )
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                        }

                        item {
                            SectionHeader(title = "Personal Information")
                        }

                        item {
                            Spacer(Modifier.height(12.dp))
                        }

                        item {
                            ProfileFormFields(
                                fullName = fullName,
                                onFullNameChange = { fullName = it },
                                phoneNumber = phoneNumber,
                                onPhoneNumberChange = { phoneNumber = it },
                                businessName = businessName,
                                onBusinessNameChange = { businessName = it },
                                locationValue = if (isSeller) shopLocation else address,
                                onLocationChange = if (isSeller) { newValue -> shopLocation = newValue } else { newValue -> address = newValue },
                                userRole = userProfile?.role ?: "BUYER",
                                isSeller = isSeller
                            )
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                        }

                        item {
                            ProfileSaveButton(
                                isSaving = isSaving,
                                buttonText = saveButtonText,
                                isSuccess = isSaveSuccess
                            ) { saveChanges() }
                        }

                        item {
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = title.uppercase(Locale.getDefault()),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
fun ProfileHero(
    displayName: String,
    fullName: String,
    role: String,
    profileImageUri: Uri?,
    onEditClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val nameToShow = if (displayName.isNotBlank()) displayName else fullName.ifBlank { "User" }
    val isSeller = role.equals("SELLER", ignoreCase = true)
    val badgeText = if (isSeller) "Verified Seller" else "Verified Buyer"
    val badgeColor = if (isSeller) Color(0xFFE7EEFE) else Color(0xFFD1FAE5)
    val textColor = if (isSeller) BrandBlue else Color(0xFF065F46)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (colorScheme.background == Color(0xFF10141C)) {
                        listOf(
                            colorScheme.surface.copy(alpha = 0.8f),
                            colorScheme.background
                        )
                    } else {
                        listOf(
                            colorScheme.surface,
                            colorScheme.background
                        )
                    }
                )
            )
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image with gradient border
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gradient border
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colorScheme.primary,
                                colorScheme.secondary,
                                colorScheme.primary
                            )
                        )
                    )
            )

            // Inner image
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface)
            ) {
                if (profileImageUri != null) {
                    val painter = rememberImagePainter(profileImageUri)
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(56.dp),
                            tint = colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Edit button
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.BottomEnd)
                    .clickable { onEditClick() },
                shape = CircleShape,
                color = colorScheme.primary,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Photo",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Name
        Text(
            text = nameToShow,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(8.dp))

        // Role badge
        Surface(
            shape = RoundedCornerShape(50),
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Verified",
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = badgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
fun AccountInfoCard(
    email: String,
    phoneNumber: String,
    role: String,
    status: String,
    createdAt: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val isActive = status.equals("ACTIVE", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Information",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                // Status chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isActive)
                        Color(0xFF10B981).copy(alpha = 0.12f)
                    else
                        Color(0xFFF59E0B).copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
            }

            Divider(
                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            // Info grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Email
                InfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = email.ifBlank { "Not set" },
                    colorScheme = colorScheme
                )

                // Phone
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = phoneNumber.ifBlank { "Not set" },
                    colorScheme = colorScheme
                )

                // Role & Member Since
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoRowCompact(
                        icon = Icons.Default.PersonOutline,
                        label = "Role",
                        value = role,
                        colorScheme = colorScheme
                    )

                    InfoRowCompact(
                        icon = Icons.Default.CalendarToday,
                        label = "Member Since",
                        value = formatDateString(createdAt),
                        colorScheme = colorScheme
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InfoRowCompact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    colorScheme: ColorScheme
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileFormFields(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    locationValue: String,
    onLocationChange: (String) -> Unit,
    userRole: String,
    isSeller: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isSellerRole = userRole.equals("SELLER", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = "Full Name",
                leadingIcon = Icons.Default.Person,
                colorScheme = colorScheme
            )

            ProfileTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = "Phone Number",
                leadingIcon = Icons.Default.Phone,
                placeholder = "+254 700 000 000",
                colorScheme = colorScheme
            )

            if (isSellerRole) {
                ProfileTextField(
                    value = businessName,
                    onValueChange = onBusinessNameChange,
                    label = "Business Name",
                    leadingIcon = Icons.Default.Storefront,
                    colorScheme = colorScheme
                )
            }

            ProfileTextField(
                value = locationValue,
                onValueChange = onLocationChange,
                label = if (isSeller) "Shop Location" else "Delivery Address",
                leadingIcon = if (isSeller) Icons.Default.LocationOn else Icons.Default.Home,
                minLines = 2,
                maxLines = 4,
                colorScheme = colorScheme
            )
        }
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    placeholder: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    colorScheme: ColorScheme
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant
            )
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.5f),
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.onSurfaceVariant,
            cursorColor = colorScheme.primary,
            focusedLeadingIconColor = colorScheme.primary,
            unfocusedLeadingIconColor = colorScheme.onSurfaceVariant
        ),
        minLines = minLines,
        maxLines = maxLines,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            color = colorScheme.onSurface,
            lineHeight = 20.sp
        )
    )
}

@Composable
fun SecuritySettings(onThemeChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val colorScheme = MaterialTheme.colorScheme
    var darkModeEnabled by remember { mutableStateOf(ThemePreferenceManager.isDarkModeEnabled(context)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            SecurityMenuItem(
                icon = Icons.Default.Lock,
                title = "Change Password",
                subtitle = "Update your password regularly",
                onClick = {
                    val userEmail = session.getEmail()
                    if (!userEmail.isNullOrBlank()) {
                        val intent = Intent(context, ChangePasswordVerificationActivity::class.java)
                        intent.putExtra("EMAIL", userEmail)
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
                    }
                },
                colorScheme = colorScheme
            )

            Divider(
                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SecurityMenuItem(
                icon = Icons.Default.Security,
                title = "Two-Factor Authentication",
                subtitle = "Enhanced account security",
                onClick = { Toast.makeText(context, "2FA coming soon", Toast.LENGTH_SHORT).show() },
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF10B981).copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Enabled",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                },
                colorScheme = colorScheme
            )

            Divider(
                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Dark Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = colorScheme.onSurfaceVariant
                    )

                    Column {
                        Text(
                            text = "Dark Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (darkModeEnabled) "Enabled" else "Disabled",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = { enabled ->
                        if (darkModeEnabled == enabled) return@Switch
                        darkModeEnabled = enabled
                        ThemePreferenceManager.setDarkModeEnabled(context.applicationContext, enabled)
                        onThemeChanged()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
fun SecurityMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    colorScheme: ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = colorScheme.onSurfaceVariant
            )

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            trailingContent?.invoke()
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ProfileSaveButton(
    isSaving: Boolean,
    buttonText: String,
    isSuccess: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isSuccess -> Color(0xFF10B981)
                isSaving -> colorScheme.primary
                else -> colorScheme.primary
            },
            disabledContainerColor = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        ),
        enabled = !isSaving,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        when {
            isSaving -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
            isSuccess -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
            else -> {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ProfileBottomNavigationBar(userRole: String) {
    val context = LocalContext.current
    val isSeller = userRole.equals("SELLER", ignoreCase = true)

    if (isSeller) {
        SellerNavBar(
            selectedIndex = SellerNavItem.Profile.index,
            onItemSelected = { item ->
                when (item) {
                    SellerNavItem.Home -> navigateTab(context, SellerDashboardActivity::class.java)
                    SellerNavItem.Transactions -> {
                        navigateTab(
                            context,
                            TransactionsActivity::class.java,
                            Bundle().apply { putString("ROLE", "SELLER") }
                        )
                    }
                    SellerNavItem.Profile -> Unit
                }
            }
        )
    } else {
        BuyerNavBar(
            selectedIndex = BuyerNavItem.Profile.index,
            onItemSelected = { item ->
                when (item) {
                    BuyerNavItem.Home -> navigateTab(context, BuyerDashboardActivity::class.java)
                    BuyerNavItem.Transactions -> {
                        navigateTab(
                            context,
                            TransactionsActivity::class.java,
                            Bundle().apply { putString("ROLE", "BUYER") }
                        )
                    }
                    BuyerNavItem.Profile -> Unit
                }
            }
        )
    }
}

@Composable
fun rememberImagePainter(uri: Uri): Painter {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) { null }
    }
    return remember(bitmap) {
        if (bitmap != null) BitmapPainter(bitmap.asImageBitmap()) else ColorPainter(Color.Gray)
    }
}

fun formatDateString(dateString: String): String {
    return try {
        val parts = dateString.split("T")
        val dateParts = parts[0].split("-")
        val month = when (dateParts[1].toInt()) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
            7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> dateParts[1]
        }
        "$month ${dateParts[2]}, ${dateParts[0]}"
    } catch (_: Exception) {
        dateString
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun ProfileActivityPreview() {
    EscrowXTheme(darkTheme = false) {
        ProfileScreenContent()
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun ProfileActivityPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        ProfileScreenContent()
    }
}