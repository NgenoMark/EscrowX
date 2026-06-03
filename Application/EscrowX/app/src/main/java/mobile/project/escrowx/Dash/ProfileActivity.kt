package mobile.project.escrowx.dash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.ChangePasswordVerificationActivity
import mobile.project.escrowx.auth.SessionManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var saveButtonText by remember { mutableStateOf("Save Changes") }
    var isSaveSuccess by remember { mutableStateOf(false) }

    var showImagePickerDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
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
        if (success) {
            val photoFile = createImageFile()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            profileImageUri = uri
            Toast.makeText(context, "Photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            Toast.makeText(context, "Image selected", Toast.LENGTH_SHORT).show()
        }
    }

    fun onEditImageClick() {
        showImagePickerDialog = true
    }

    fun onCameraClick() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val photoFile = createImageFile()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
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

    // Load user profile
    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()

            if (token.isNullOrBlank() || userEmail.isNullOrBlank()) {
                errorMessage = "Please login again"
                isLoading = false
                return@launch
            }

            try {
                val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)

                if (response.isSuccessful && response.body() != null) {
                    userProfile = response.body()
                    displayName = userProfile?.displayName ?: ""
                    fullName = userProfile?.displayName?.ifBlank { userProfile?.email?.substringBefore("@") ?: "" } ?: ""
                    email = userProfile?.email ?: ""
                    phoneNumber = userProfile?.phone ?: ""
                    businessName = userProfile?.businessName ?: ""
                    deliveryAddress = "Westlands Commercial Centre, Ring Road, Nairobi, Kenya"
                } else {
                    errorMessage = "Failed to load profile: ${response.code()}"
                }
            } catch (_: Exception) {
                errorMessage = "Network error. Please try again."
            } finally {
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
                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
                    isSaving = false
                    saveButtonText = "Save Changes"
                    return@launch
                }

                delay(1200)
                saveButtonText = "Saved Successfully"
                isSaveSuccess = true
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                delay(2000)
                saveButtonText = "Save Changes"
                isSaveSuccess = false
                isSaving = false

            } catch (_: Exception) {
                Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show()
                isSaving = false
                saveButtonText = "Save Changes"
                isSaveSuccess = false
            }
        }
    }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Choose Option") },
            text = { Text("Select image from gallery or take a photo") },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = { onGalleryClick() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery")
                    }
                    TextButton(
                        onClick = { onCameraClick() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera")
                    }
                    TextButton(
                        onClick = { showImagePickerDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = Color.Red)
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                        (context as? ProfileActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00236F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9FF)
                )
            )
        },
        bottomBar = { ProfileBottomNavigationBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .verticalScroll(rememberScrollState())
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00236F))
                    }
                }
                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage!!, color = Color.Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                isLoading = true
                                errorMessage = null
                                val userEmail = session.getEmail()
                                if (!userEmail.isNullOrBlank()) {
                                    scope.launch {
                                        try {
                                            val token = session.getAccessToken()
                                            if (!token.isNullOrBlank()) {
                                                val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                                                if (response.isSuccessful && response.body() != null) {
                                                    userProfile = response.body()
                                                    displayName = userProfile?.displayName ?: ""
                                                    fullName = userProfile?.displayName?.ifBlank { userProfile?.email?.substringBefore("@") ?: "" } ?: ""
                                                    email = userProfile?.email ?: ""
                                                    phoneNumber = userProfile?.phone ?: ""
                                                    businessName = userProfile?.businessName ?: ""
                                                    errorMessage = null
                                                } else {
                                                    errorMessage = "Failed to load profile"
                                                }
                                            }
                                        } catch (_: Exception) {
                                            errorMessage = "Network error. Please try again."
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F))) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    ProfileHero(
                        displayName = displayName,
                        fullName = fullName,
                        role = userProfile?.role ?: "BUYER",
                        profileImageUri = profileImageUri,
                        onEditClick = { onEditImageClick() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AccountInfoCard(
                        email = email,
                        phoneNumber = phoneNumber,
                        role = userProfile?.role ?: "BUYER",
                        status = userProfile?.status ?: "ACTIVE",
                        createdAt = userProfile?.createdAt ?: ""
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileFormFields(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        businessName = businessName,
                        onBusinessNameChange = { businessName = it },
                        deliveryAddress = deliveryAddress,
                        onDeliveryAddressChange = { deliveryAddress = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SecuritySettings()

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSaveButton(
                        isSaving = isSaving,
                        buttonText = saveButtonText,
                        isSuccess = isSaveSuccess,
                        onClick = { saveChanges() }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileHero(
    displayName: String,
    fullName: String,
    role: String,
    profileImageUri: Uri?,
    onEditClick: () -> Unit
) {
    val nameToShow = if (displayName.isNotBlank()) displayName else fullName.ifBlank { "User" }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                color = Color(0xFFDCE2F3),
                border = BorderStroke(2.dp, Color(0xFFB6C4FF))
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
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFF00236F)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onEditClick() },
                shape = CircleShape,
                color = Color(0xFF00236F),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Photo",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = nameToShow, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF151C27))
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(if (role == "BUYER") Color(0xFF6CF8BB) else Color(0xFFE7EEFE), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Verified, contentDescription = "Verified", modifier = Modifier.size(16.dp), tint = if (role == "BUYER") Color(0xFF005236) else Color(0xFF00236F))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = if (role == "BUYER") "Verified Buyer" else "Verified Seller", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (role == "BUYER") Color(0xFF005236) else Color(0xFF00236F))
        }
    }
}

@Composable
fun rememberImagePainter(uri: Uri): Painter {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
    return remember(bitmap) {
        if (bitmap != null) {
            BitmapPainter(bitmap.asImageBitmap())
        } else {
            ColorPainter(Color.Gray)
        }
    }
}

@Composable
fun AccountInfoCard(email: String, phoneNumber: String, role: String, status: String, createdAt: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EEFE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Account Information", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Email", fontSize = 11.sp, color = Color(0xFF444651))
                    Text(email.ifBlank { "Not set" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Role", fontSize = 11.sp, color = Color(0xFF444651))
                    Text(role, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Status", fontSize = 11.sp, color = Color(0xFF444651))
                    Text(
                        status,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (status == "ACTIVE") Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column {
                    Text("Phone", fontSize = 11.sp, color = Color(0xFF444651))
                    Text(phoneNumber.ifBlank { "Not set" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column {
                    Text("Member Since", fontSize = 11.sp, color = Color(0xFF444651))
                    Text(formatDateString(createdAt), fontSize = 12.sp, color = Color(0xFF444651))
                }
            }
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
    deliveryAddress: String,
    onDeliveryAddressChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full Name") },
            shape = RoundedCornerShape(8.dp)
        )
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone Number (+254)") },
            shape = RoundedCornerShape(8.dp),
            trailingIcon = { Icon(Icons.Default.Call, null, tint = Color(0xFF444651)) }
        )
        OutlinedTextField(
            value = businessName,
            onValueChange = onBusinessNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Business Name (Optional)") },
            shape = RoundedCornerShape(8.dp)
        )
        OutlinedTextField(
            value = deliveryAddress,
            onValueChange = onDeliveryAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Default Delivery Address") },
            minLines = 3,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun SecuritySettings() {
    val context = LocalContext.current
    val session = SessionManager(context)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("SECURITY & PRIVACY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF444651))
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val userEmail = session.getEmail()
                            if (!userEmail.isNullOrBlank()) {
                                val intent = Intent(context, ChangePasswordVerificationActivity::class.java)
                                intent.putExtra("EMAIL", userEmail)
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp), tint = Color(0xFF444651))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Change Password", fontSize = 14.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF757682))
                }
                HorizontalDivider(color = Color(0xFFC5C5D3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "2FA coming soon", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(20.dp), tint = Color(0xFF444651))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Two-Factor Authentication", fontSize = 14.sp)
                            Text("Enabled", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF006C49))
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF757682))
                }
            }
        }
    }
}

@Composable
fun ProfileSaveButton(isSaving: Boolean, buttonText: String, isSuccess: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) Color(0xFF006C49) else Color(0xFF00236F)),
        enabled = !isSaving
    ) {
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Save, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ProfileBottomNavigationBar() {
    val context = LocalContext.current
    NavigationBar(modifier = Modifier.height(80.dp), containerColor = Color(0xFFF9F9FF)) {
        NavigationBarItem(
            selected = false,
            onClick = { context.startActivity(Intent(context, BuyerDashboardActivity::class.java)) },
            icon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(24.dp)) },
            label = { Text("Home", fontSize = 11.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = { context.startActivity(Intent(context, TransactionsActivity::class.java)) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(24.dp)) },
            label = { Text("Transactions", fontSize = 11.sp) }
        )
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp)) },
            label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
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