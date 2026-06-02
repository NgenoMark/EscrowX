package mobile.project.escrowx.dash

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Theme colors matching the app
val DisputePrimary = Color(0xFF00236F)
val DisputeBackground = Color(0xFFF9F9FF)
val DisputeRed = Color(0xFFEF4444)

class RaiseDisputeActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the transaction details passed from TransactionsActivity
        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: "Unknown"
        val transactionTitle = intent.getStringExtra("TRANSACTION_TITLE") ?: "Unknown Transaction"
        val transactionAmount = intent.getStringExtra("TRANSACTION_AMOUNT") ?: "0"

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val session = SessionManager(context)

            var reason by remember { mutableStateOf("") }
            var amount by remember { mutableStateOf(transactionAmount) }
            var description by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            val attachedFiles = remember { mutableStateListOf<Uri>() }
            var tempUri by remember { mutableStateOf<Uri?>(null) }

            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? -> uri?.let { attachedFiles.add(it) } }

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success -> if (success) tempUri?.let { attachedFiles.add(it) } }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Raise Dispute",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DisputePrimary
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = DisputePrimary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = DisputePrimary
                            )
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(DisputeBackground)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Transaction Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = transactionTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "KES $transactionAmount",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DisputePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Transaction ID: $transactionId",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Mediation Alert Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EEFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = DisputePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Mediation Process",
                                        fontWeight = FontWeight.Bold,
                                        color = DisputePrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Reviewed within 48-72 hours.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Dispute Reason Dropdown
                        Text(
                            text = "Dispute Reason",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DisputePrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        var expanded by remember { mutableStateOf(false) }
                        val reasons = listOf(
                            "Item not received",
                            "Item not as described",
                            "Seller unresponsive",
                            "Wrong item delivered",
                            "Damaged item",
                            "Other"
                        )

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Reason") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DisputePrimary,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                reasons.forEach { selectedReason ->
                                    DropdownMenuItem(
                                        text = { Text(selectedReason) },
                                        onClick = {
                                            reason = selectedReason
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Amount (pre-filled, can be modified)
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount (KES)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DisputePrimary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Detailed Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Detailed Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DisputePrimary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Evidence Attachments
                        Text(
                            text = "Evidence Attachments",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DisputePrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "${attachedFiles.size} file(s) selected",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DisputePrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Gallery",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gallery")
                            }

                            OutlinedButton(
                                onClick = {
                                    val file = File(context.cacheDir, "dispute_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    tempUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DisputePrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera")
                            }
                        }

                        // Show attached files list
                        if (attachedFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
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
                                            Text(
                                                text = uri.lastPathSegment?.takeLast(20) ?: "Image",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { attachedFiles.remove(uri) }
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Red
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (reason.isBlank()) {
                                    Toast.makeText(context, "Please select a dispute reason", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (description.isBlank()) {
                                    Toast.makeText(context, "Please provide a detailed description", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true

                                scope.launch {
                                    try {
                                        val token = session.getAccessToken()
                                        if (token.isNullOrBlank()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                                            }
                                            isLoading = false
                                            return@launch
                                        }

                                        // TODO: Connect to actual dispute API endpoint
                                        // val disputeRequest = DisputeRequest(
                                        //     transactionId = transactionId,
                                        //     reason = reason,
                                        //     amount = amount.toDoubleOrNull() ?: 0.0,
                                        //     description = description,
                                        //     attachments = attachedFiles
                                        // )
                                        // val response = RetrofitClient.authenticated(token).raiseDispute(disputeRequest)

                                        // For now, show success and close
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Dispute submitted successfully for: $transactionId",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            finish()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DisputePrimary,
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
                                Text("Submit Dispute", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}