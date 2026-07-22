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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
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
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val MAX_PROFILE_IMAGE_BYTES = 5L * 1024L * 1024L

private val KENYA_COUNTIES = listOf(
    "Baringo", "Bomet", "Bungoma", "Busia", "Elgeyo-Marakwet", "Embu", "Garissa",
    "Homa Bay", "Isiolo", "Kajiado", "Kakamega", "Kericho", "Kiambu", "Kilifi",
    "Kirinyaga", "Kisii", "Kisumu", "Kitui", "Kwale", "Laikipia", "Lamu", "Machakos",
    "Makueni", "Mandera", "Marsabit", "Meru", "Migori", "Mombasa", "Murang'a",
    "Nairobi", "Nakuru", "Nandi", "Narok", "Nyamira", "Nyandarua", "Nyeri",
    "Samburu", "Siaya", "Taita-Taveta", "Tana River", "Tharaka-Nithi", "Trans Nzoia",
    "Turkana", "Uasin Gishu", "Vihiga", "Wajir", "West Pokot"
)

private fun parseLocation(raw: String): Pair<String, String> {
    val value = raw.trim()
    if (value.isBlank()) return "" to ""

    val pipeIndex = value.indexOf("|")
    if (pipeIndex > -1) {
        val county = value.substring(0, pipeIndex).trim()
        val description = value.substring(pipeIndex + 1).trim()
        if (county in KENYA_COUNTIES) return county to description
    }

    val matchedCounty = KENYA_COUNTIES.firstOrNull { county ->
        value.equals(county, ignoreCase = true) || value.startsWith("$county ", ignoreCase = true)
    }

    if (matchedCounty != null) {
        val description = value
            .removePrefix(matchedCounty)
            .trimStart(' ', '-', ',', ':')
            .trim()
        return matchedCounty to description
    }

    return "" to value
}

private fun buildLocationValue(county: String, description: String): String {
    val countyClean = county.trim()
    val descriptionClean = description.trim()
    return when {
        countyClean.isBlank() -> descriptionClean
        descriptionClean.isBlank() -> countyClean
        else -> "$countyClean | $descriptionClean"
    }
}

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = ThemePreferenceManager.rememberDarkModeEnabledState()
            EscrowXTheme(
                darkTheme = darkTheme,
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
    var locationCounty by remember { mutableStateOf("") }
    var locationDescription by remember { mutableStateOf("") }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
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
                        val existingLocation = if (isSeller) shopLocation else address
                        val parsedLocation = parseLocation(existingLocation)
                        locationCounty = parsedLocation.first
                        locationDescription = parsedLocation.second
                        profileImageUrl = RetrofitClient.resolveApiUrl(userProfile?.avatarUrl)
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
        if (locationDescription.isNotBlank() && locationCounty.isBlank()) {
            Toast.makeText(context, "Please select a county", Toast.LENGTH_SHORT).show()
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

                val composedLocation = buildLocationValue(locationCounty, locationDescription)

                val request = UpdateProfileRequest(
                    displayName = fullName.takeIf { it.isNotBlank() },
                    phone = phoneNumber.takeIf { it.isNotBlank() },
                    businessName = businessName.takeIf { it.isNotBlank() },
                    address = if (isSeller) null else composedLocation.takeIf { it.isNotBlank() },
                    shopLocation = if (isSeller) composedLocation.takeIf { it.isNotBlank() } else null,
                    avatarUrl = RetrofitClient.toBackendRelativePath(profileImageUrl)
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
                        val parsedLocation = parseLocation(shopLocation)
                        locationCounty = parsedLocation.first
                        locationDescription = parsedLocation.second
                    } else {
                        address = updatedProfile.address ?: ""
                        val parsedLocation = parseLocation(address)
                        locationCounty = parsedLocation.first
                        locationDescription = parsedLocation.second
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

    fun uploadSelectedProfileImage(uri: Uri) {
        scope.launch {
            try {
                val token = session.getAccessToken()
                val userId = session.getUserId()
                if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val file = uriToTempFile(context, uri, "profile_upload")
                if (file == null || !file.exists()) {
                    Toast.makeText(context, "Unable to process selected image", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (file.length() > MAX_PROFILE_IMAGE_BYTES) {
                    Toast.makeText(context, "Image is too large. Maximum allowed size is 5MB.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val body = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, body)
                val response = RetrofitClient.authenticated(token).uploadProfileImage(part, userId)
                if (response.isSuccessful && response.body() != null) {
                    val uploadedUrl = response.body()!!.url
                    val backendAvatarPath = RetrofitClient.toBackendRelativePath(uploadedUrl)
                    val updateResponse = RetrofitClient.authenticated(token).updateProfile(
                        userId,
                        UpdateProfileRequest(avatarUrl = backendAvatarPath)
                    )

                    if (updateResponse.isSuccessful && updateResponse.body() != null) {
                        profileImageUrl = RetrofitClient.resolveApiUrl(backendAvatarPath)
                        profileImageUri = uri
                        userProfile = updateResponse.body()
                        Toast.makeText(context, "Profile image uploaded", Toast.LENGTH_SHORT).show()
                    } else {
                        val updateError = updateResponse.errorBody()?.string()?.take(220)
                        Toast.makeText(
                            context,
                            updateError ?: "Image uploaded but failed to save profile picture",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val rawError = response.errorBody()?.string()?.take(260)
                    val message = if ((response.code() == 413) || (rawError?.contains("Maximum upload size exceeded", ignoreCase = true) == true)) {
                        "Image is too large. Maximum allowed size is 5MB."
                    } else {
                        rawError ?: "Failed to upload profile image"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Image upload error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val croppedUri = UCrop.getOutput(data)
            if (croppedUri != null) {
                uploadSelectedProfileImage(croppedUri)
            } else {
                Toast.makeText(context, "Could not read cropped image", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && data != null) {
            val error = UCrop.getError(data)
            Toast.makeText(
                context,
                "Image edit failed: ${error?.message ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { capturedUri ->
                launchCropEditor(context, capturedUri) { cropIntent ->
                    cropLauncher.launch(cropIntent)
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            launchCropEditor(context, it) { cropIntent ->
                cropLauncher.launch(cropIntent)
            }
        }
    }

    fun onEditImageClick() { showImagePickerDialog = true }
    fun onCameraClick() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile = createImageFile()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            pendingCameraUri = uri
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
                                profileImageUrl = profileImageUrl,
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
                                countyValue = locationCounty,
                                onCountyChange = { locationCounty = it },
                                locationDescriptionValue = locationDescription,
                                onLocationDescriptionChange = { locationDescription = it },
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
    profileImageUrl: String?,
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
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!profileImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profileImageUrl,
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
    countyValue: String,
    onCountyChange: (String) -> Unit,
    locationDescriptionValue: String,
    onLocationDescriptionChange: (String) -> Unit,
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

            KenyaCountyDropdownField(
                selectedCounty = countyValue,
                onCountySelected = onCountyChange,
                label = if (isSeller) "Shop County" else "Delivery County",
                leadingIcon = Icons.Default.LocationOn,
                colorScheme = colorScheme
            )

            ProfileTextField(
                value = locationDescriptionValue,
                onValueChange = onLocationDescriptionChange,
                label = if (isSeller) "Shop Location Description" else "Delivery Address Description",
                placeholder = "Estate, street, building, nearest landmark",
                minLines = 2,
                maxLines = 4,
                colorScheme = colorScheme
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KenyaCountyDropdownField(
    selectedCounty: String,
    onCountySelected: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    colorScheme: ColorScheme
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val accentColor = BrandBlue
    val filteredCounties = remember(searchQuery, selectedCounty, expanded) {
        val query = if (expanded) searchQuery.trim() else selectedCounty.trim()
        if (query.isBlank()) {
            KENYA_COUNTIES
        } else {
            KENYA_COUNTIES.filter { it.contains(query, ignoreCase = true) }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            if (expanded) {
                searchQuery = selectedCounty
            } else {
                searchQuery = ""
            }
        }
    ) {
        OutlinedTextField(
            value = if (expanded) searchQuery else selectedCounty,
            onValueChange = {
                searchQuery = it
                if (!expanded) {
                    expanded = true
                }
            },
            readOnly = false,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            label = {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            placeholder = {
                Text(
                    text = "Select County",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedLabelColor = accentColor,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                cursorColor = accentColor,
                focusedLeadingIconColor = accentColor,
                unfocusedLeadingIconColor = colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = accentColor,
                unfocusedTrailingIconColor = colorScheme.onSurfaceVariant
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                color = colorScheme.onSurface
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                searchQuery = ""
            },
            containerColor = colorScheme.surface
        ) {
            if (filteredCounties.isEmpty()) {
                DropdownMenuItem(
                    enabled = false,
                    text = {
                        Text(
                            text = "No county found",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {}
                )
            } else {
                filteredCounties.forEach { county ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = county,
                                fontSize = 14.sp,
                                color = colorScheme.onSurface
                            )
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = colorScheme.onSurface,
                            disabledTextColor = colorScheme.onSurfaceVariant,
                            leadingIconColor = colorScheme.onSurfaceVariant,
                            trailingIconColor = colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            onCountySelected(county)
                            expanded = false
                            searchQuery = ""
                        }
                    )
                }
            }
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
    centerLabel: Boolean = false,
    centerPlaceholder: Boolean = false,
    centerInputText: Boolean = false,
    colorScheme: ColorScheme
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = if (centerLabel) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = if (centerPlaceholder) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
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
            lineHeight = 20.sp,
            textAlign = if (centerInputText) TextAlign.Center else TextAlign.Start
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
    val buttonColor = when {
        isSuccess -> Color(0xFF10B981)
        isSaving -> colorScheme.primary
        else -> colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Progress indicator row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(
                        label = "Profile",
                        isActive = !isSaving && !isSuccess,
                        isComplete = isSuccess,
                        colorScheme = colorScheme
                    )
                    StatusLine(isComplete = isSuccess, colorScheme = colorScheme)
                    StatusDot(
                        label = "Save",
                        isActive = isSaving,
                        isComplete = isSuccess,
                        colorScheme = colorScheme
                    )
                }
            }

            // Main Button
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White,
                    disabledContainerColor = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp
                ),
                enabled = !isSaving
            ) {
                when {
                    isSaving -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = buttonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        // Animated dots
                        AnimatedDots()
                    }
                    isSuccess -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
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
                        // Subtle shine effect
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0f),
                                            Color.White.copy(alpha = 0.05f),
                                            Color.White.copy(alpha = 0f)
                                        ),
                                        startX = 0f,
                                        endX = 1f
                                    )
                                )
                        )
                    }
                }
            }

            // Security & info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecurityBadge(
                    icon = Icons.Default.Lock,
                    text = "Encrypted",
                    colorScheme = colorScheme
                )
                SecurityBadge(
                    icon = Icons.Default.Shield,
                    text = "Protected",
                    colorScheme = colorScheme
                )
                SecurityBadge(
                    icon = Icons.Default.Verified,
                    text = "Secure",
                    colorScheme = colorScheme
                )
            }
        }
    }
}

@Composable
fun StatusDot(
    label: String,
    isActive: Boolean,
    isComplete: Boolean,
    colorScheme: ColorScheme
) {
    val dotColor = when {
        isComplete -> Color(0xFF10B981)
        isActive -> colorScheme.primary
        else -> colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
                .then(
                    if (isActive) {
                        Modifier
                            .size(14.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        dotColor.copy(alpha = 0.2f),
                                        dotColor.copy(alpha = 0f)
                                    ),
                                    radius = 10f
                                )
                            )
                    } else Modifier
                )
        )
        Text(
            label,
            fontSize = 8.sp,
            color = if (isActive || isComplete) colorScheme.onSurface else colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun StatusLine(
    isComplete: Boolean,
    colorScheme: ColorScheme
) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(2.dp)
            .background(
                if (isComplete) Color(0xFF10B981)
                else colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            )
    )
}

@Composable
fun AnimatedDots() {
    val infiniteTransition = rememberInfiniteTransition()
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Dot(dot1)
        Dot(dot2)
        Dot(dot3)
    }
}

@Composable
fun Dot(scale: Float) {
    Box(
        modifier = Modifier
            .size((4 + scale * 2).dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.5f + scale * 0.5f))
    )
}

@Composable
fun SecurityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    colorScheme: ColorScheme
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text,
            fontSize = 10.sp,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
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

fun uriToTempFile(context: Context, uri: Uri, filePrefix: String): File? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val file = File.createTempFile(filePrefix, ".jpg", context.cacheDir)
        input.use { inputStream ->
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
        }
        file
    } catch (_: Exception) {
        null
    }
}

fun isImageWithinLimit(context: Context, uri: Uri, maxBytes: Long): Boolean {
    return try {
        val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        if (size == null || size < 0L) {
            // If size can't be resolved from descriptor, allow upload and enforce again from temp file.
            true
        } else {
            size <= maxBytes
        }
    } catch (_: Exception) {
        true
    }
}

fun launchCropEditor(context: Context, sourceUri: Uri, onIntentReady: (Intent) -> Unit) {
    val destinationFile = File(
        context.cacheDir,
        "profile_cropped_${System.currentTimeMillis()}.jpg"
    )
    val destinationUri = Uri.fromFile(destinationFile)

    val options = UCrop.Options().apply {
        setCompressionQuality(92)
        setFreeStyleCropEnabled(true)
        setToolbarTitle("Edit Profile Photo")
        setHideBottomControls(false)
        setShowCropFrame(true)
        setShowCropGrid(true)
    }

    val cropIntent = UCrop.of(sourceUri, destinationUri)
        .withAspectRatio(1f, 1f)
        .withMaxResultSize(1080, 1080)
        .withOptions(options)
        .getIntent(context)

    onIntentReady(cropIntent)
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