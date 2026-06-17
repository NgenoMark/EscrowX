package mobile.project.escrowx.seller
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.R
import mobile.project.escrowx.auth.SessionManager

class RejectRequestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "MacBook Pro M2"
        val buyerName = intent.getStringExtra("BUYER_NAME") ?: "Alice Wanjiku"
        val amount = intent.getStringExtra("AMOUNT") ?: "85,000"

        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                RejectRequestScreen(
                    transactionId = transactionId,
                    productName = productName,
                    buyerName = buyerName,
                    amount = amount
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RejectRequestScreen(
    transactionId: String,
    productName: String,
    buyerName: String,
    amount: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)
    var selectedReason by remember { mutableStateOf("") }
    var otherReasonText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val reasons = listOf(
        "Incorrect price",
        "Item no longer available",
        "Shipping issues",
        "Other"
    )

    fun handleConfirmRejection() {
        if (selectedReason.isBlank()) {
            Toast.makeText(context, "Please select a reason for rejection", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedReason == "Other" && otherReasonText.isBlank()) {
            Toast.makeText(context, "Please describe your reason", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val finalReason = if (selectedReason == "Other") otherReasonText else selectedReason

        scope.launch {
            try {
                val token = session.getAccessToken()
                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@launch
                }

                delay(1500)

                Toast.makeText(context, "Request rejected: $finalReason", Toast.LENGTH_LONG).show()

                val intent = Intent(context, RequestDeclinedActivity::class.java).apply {
                    putExtra("BUYER_NAME", buyerName)
                }
                context.startActivity(intent)
                (context as? RejectRequestActivity)?.finish()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rejection Flow",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00236F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? RejectRequestActivity)?.finish() }) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3FF)),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDCE2F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = "Product",
                            tint = Color(0xFF00236F),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Text(
                            productName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF151C27)
                        )
                        Text(
                            "Buyer: $buyerName",
                            fontSize = 13.sp,
                            color = Color(0xFF444651)
                        )
                        Text(
                            "KES $amount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00236F),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Why are you declining?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF151C27)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Please select a reason for declining this request. This helps the buyer understand the situation.",
                fontSize = 14.sp,
                color = Color(0xFF444651)
            )

            Spacer(modifier = Modifier.height(20.dp))

            reasons.forEach { reason ->
                RejectionReasonOption(
                    reason = reason,
                    isSelected = selectedReason == reason,
                    onSelect = { selectedReason = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedReason == "Other") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = otherReasonText,
                    onValueChange = { otherReasonText = it },
                    label = { Text("Describe your reason") },
                    placeholder = { Text("Type your reason here...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00236F),
                        unfocusedBorderColor = Color(0xFFC5C5D3)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { handleConfirmRejection() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBA1A1A),
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
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Rejection", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { (context as? RejectRequestActivity)?.finish() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", fontSize = 14.sp, color = Color(0xFF444651))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RejectionReasonOption(
    reason: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(reason) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE7EEFE) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00236F) else Color(0xFFC5C5D3)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                reason,
                fontSize = 14.sp,
                color = if (isSelected) Color(0xFF00236F) else Color(0xFF151C27)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Color(0xFF00236F) else Color(0xFFC5C5D3),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00236F))
                    )
                }
            }
        }
    }
}