@file:Suppress("SpellCheckingInspection")
package mobile.project.escrowx.dash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import mobile.project.escrowx.R
import mobile.project.escrowx.RaiseDisputeRequest
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RaiseDisputeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        val transactionTitle = intent.getStringExtra("TRANSACTION_TITLE") ?: "Unknown Transaction"
        val transactionAmount = intent.getStringExtra("TRANSACTION_AMOUNT") ?: "0"

        setContent {
            MaterialTheme {
                RaiseDisputeScreen(
                    transactionId = transactionId,
                    transactionTitle = transactionTitle,
                    transactionAmount = transactionAmount
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseDisputeScreen(
    transactionId: String,
    transactionTitle: String,
    transactionAmount: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var selectedReason by remember { mutableStateOf("") }
    var otherReasonText by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(transactionAmount) }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val attachedFiles = remember { mutableStateListOf<Uri>() }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    val reasons = listOf(
        "Item not received",
        "Item not as described",
        "Seller unresponsive",
        "Wrong item delivered",
        "Damaged item",
        "Other"
    )

    val isOtherSelected = selectedReason == "Other"
    val isFormValid = selectedReason.isNotBlank() &&
            (!isOtherSelected || otherReasonText.isNotBlank()) &&
            description.isNotBlank()

    // Camera & gallery launchers (same as before)
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
        if (success) {
            // In a real app, you'd upload and get URL, then add to attachedFiles
            Toast.makeText(context, "Photo captured (upload not implemented)", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        attachedFiles.addAll(uris)
        Toast.makeText(context, "${uris.size} image(s) added", Toast.LENGTH_SHORT).show()
    }

    fun onAddAttachmentClick() { showImagePickerDialog = true }
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
    fun removeAttachment(uri: Uri) { attachedFiles.remove(uri) }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Add Attachment") },
            text = { Text("Choose image from gallery or take a photo") },
            confirmButton = {
                Column {
                    TextButton(onClick = { onGalleryClick() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Gallery")
                    }
                    TextButton(onClick = { onCameraClick() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Camera")
                    }
                    TextButton(onClick = { showImagePickerDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = Color.Red)
                    }
                }
            }
        )
    }

    fun submitDispute() {
        if (!isFormValid) {
            when {
                selectedReason.isBlank() -> Toast.makeText(context, "Please select a dispute reason", Toast.LENGTH_SHORT).show()
                isOtherSelected && otherReasonText.isBlank() -> Toast.makeText(context, "Please specify the reason", Toast.LENGTH_SHORT).show()
                description.isBlank() -> Toast.makeText(context, "Please provide a detailed description", Toast.LENGTH_SHORT).show()
            }
            return
        }

        isLoading = true
        val finalReason = if (isOtherSelected) otherReasonText else selectedReason
        // Map to backend category enum
        val category = when (finalReason) {
            "Item not received" -> "NON_DELIVERY"
            "Item not as described" -> "ITEM_NOT_AS_DESCRIBED"
            "Damaged item" -> "DAMAGED_ITEM"
            "Wrong item delivered" -> "WRONG_ITEM"
            "Seller unresponsive" -> "SELLER_UNRESPONSIVE"
            else -> "OTHER"
        }

        scope.launch {
            try {
                val token = session.getAccessToken()
                val actorUserId = session.getUserId()
                if (token.isNullOrBlank() || actorUserId.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isLoading = false
                    return@launch
                }

                val request = RaiseDisputeRequest(
                    transactionId = transactionId,
                    category = category,
                    description = description
                )
                val response = RetrofitClient.authenticated(token).raiseDispute(actorUserId, request)
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(context, "Dispute submitted successfully", Toast.LENGTH_LONG).show()
                    (context as? RaiseDisputeActivity)?.finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Submission failed"
                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raise Dispute", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? RaiseDisputeActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color(0xFF00236F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9F9FF))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Transaction Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(transactionTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("KES $transactionAmount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Transaction ID: $transactionId", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Mediation Alert Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EEFE))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF00236F), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Mediation Process", fontWeight = FontWeight.Bold, color = Color(0xFF00236F), fontSize = 14.sp)
                        Text("Reviewed within 48-72 hours.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Dispute Reason Dropdown
            Text("Dispute Reason *", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00236F), modifier = Modifier.padding(bottom = 8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedReason,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Reason") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    isError = selectedReason.isBlank(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00236F), unfocusedBorderColor = Color.Gray)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    reasons.forEach { reasonItem ->
                        DropdownMenuItem(
                            text = { Text(reasonItem) },
                            onClick = {
                                selectedReason = reasonItem
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (isOtherSelected) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = otherReasonText,
                    onValueChange = { otherReasonText = it },
                    label = { Text("Please specify your reason *") },
                    placeholder = { Text("Type your reason here...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = otherReasonText.isBlank(),
                    supportingText = { if (otherReasonText.isBlank()) Text("This field is required", color = Color.Red, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00236F), unfocusedBorderColor = Color.Gray)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Detailed Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Description *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                isError = description.isBlank(),
                supportingText = { if (description.isBlank()) Text("Description is required", color = Color.Red, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00236F), unfocusedBorderColor = Color.Gray)
            )

            Spacer(Modifier.height(24.dp))

            // Evidence Attachments (optional)
            Text("Evidence Attachments", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00236F), modifier = Modifier.padding(bottom = 8.dp))
            Text("${attachedFiles.size} file(s) selected", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

            OutlinedButton(
                onClick = { onAddAttachmentClick() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00236F))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Attachment")
            }

            if (attachedFiles.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        attachedFiles.forEach { uri ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.InsertDriveFile, null, modifier = Modifier.size(16.dp), tint = Color(0xFF00236F))
                                    Spacer(Modifier.width(8.dp))
                                    Text(uri.lastPathSegment?.takeLast(30) ?: "Image", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                }
                                IconButton(onClick = { removeAttachment(uri) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp), tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = { submitDispute() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F), contentColor = Color.White),
                enabled = !isLoading && isFormValid
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Dispute", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}