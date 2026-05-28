package mobile.project.escrowx.dash

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

class RaiseDisputeActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the transaction ID passed from TransactionsActivity
        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: "Unknown"

        setContent {
            val context = LocalContext.current

            var reason by remember { mutableStateOf("") }
            var amount by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
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
                            title = { Text("Raise Dispute") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Mediation Alert
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EEFE))) {
                            Row(Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFF00236F))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Mediation Process", fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
                                    Text("Transaction ID: $transactionId", fontSize = 12.sp, color = Color.Gray)
                                    Text("Reviewed within 48-72 hours.", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value = reason, onValueChange = { reason = it },
                            label = { Text("Dispute Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = amount, onValueChange = { amount = it },
                            label = { Text("Amount (KES)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = description, onValueChange = { description = it },
                            label = { Text("Detailed Description") },
                            modifier = Modifier.fillMaxWidth(), minLines = 4
                        )

                        Spacer(Modifier.height(24.dp))

                        Text("Evidence Attachments (${attachedFiles.size})", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) {
                                Text("Gallery")
                            }
                            Button(onClick = {
                                val file = File(context.cacheDir, "dispute_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                tempUri = uri
                                cameraLauncher.launch(uri)
                            }, modifier = Modifier.weight(1f)) {
                                Text("Camera")
                            }
                        }

                        Button(
                            onClick = {
                                // Logic to submit using 'transactionId', 'reason', 'amount', 'description', and 'attachedFiles'
                                Toast.makeText(context, "Submitting dispute for: $transactionId", Toast.LENGTH_LONG).show()
                                finish() // Close screen after submission
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Submit Dispute")
                        }
                    }
                }
            }
        }
    }
}