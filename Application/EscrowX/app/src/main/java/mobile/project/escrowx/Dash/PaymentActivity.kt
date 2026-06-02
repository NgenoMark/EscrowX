package mobile.project.escrowx.dash

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.R
import mobile.project.escrowx.auth.SessionManager
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush

// Payment method enum
enum class PaymentMethod {
    MPESA,
    CARD,
    BANK_TRANSFER
}

class PaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get transaction details from intent
        val itemName = intent.getStringExtra("ITEM_NAME") ?: "iPhone 15 Pro Max"
        val amount = intent.getDoubleExtra("AMOUNT", 165000.0)

        setContent {
            MaterialTheme {
                PaymentScreen(itemName = itemName, amount = amount)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(itemName: String, amount: Double) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()

    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.MPESA) }
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("Pay KES ${String.format("%,.0f", amount)}") }

    // Format amount with commas
    val formattedAmount = String.format("%,.0f", amount)

    fun handlePayment() {
        isProcessing = true
        buttonText = "Processing..."

        scope.launch {
            try {
                val token = session.getAccessToken()
                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = "Pay KES $formattedAmount"
                    return@launch
                }

                // TODO: Implement actual payment API call
                delay(1500)

                isSuccess = true
                buttonText = "Payment Successful!"
                Toast.makeText(context, "Payment initiated successfully!", Toast.LENGTH_LONG).show()

                delay(2000)
                isProcessing = false
                isSuccess = false
                buttonText = "Pay KES $formattedAmount"

                // Navigate back to dashboard or transaction details
                context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                (context as? PaymentActivity)?.finish()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                isProcessing = false
                buttonText = "Pay KES $formattedAmount"
                isSuccess = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Payment Method",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? PaymentActivity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00236F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9FF),
                    titleContentColor = Color(0xFF151C27)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Transaction Summary Card
                TransactionSummaryCard(itemName = itemName, amount = amount, formattedAmount = formattedAmount)

                // Payment Method Selection
                PaymentMethodSelection(
                    selectedMethod = selectedPaymentMethod,
                    onMethodSelected = { selectedPaymentMethod = it }
                )

                // Trust Badge Section
                TrustBadgeSection()

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sticky Bottom Action Button
            PaymentBottomButton(
                amount = amount,
                formattedAmount = formattedAmount,
                buttonText = buttonText,
                isProcessing = isProcessing,
                isSuccess = isSuccess,
                onPayClick = { handlePayment() }
            )
        }
    }
}

@Composable
fun TransactionSummaryCard(itemName: String, amount: Double, formattedAmount: String) {
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
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "PURCHASING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF444651),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        itemName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE7EEFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = "Item",
                        tint = Color(0xFF00236F),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color(0xFFC5C5D3))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Amount", fontSize = 14.sp, color = Color(0xFF444651))
                Text(
                    "KES $formattedAmount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00236F)
                )
            }
        }
    }
}

@Composable
fun PaymentMethodSelection(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Select Payment Method",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF151C27)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF6CF8BB).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Secure",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF006C49)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Secure",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF006C49)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Payment Options
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // M-Pesa Option
            PaymentOptionCard(
                title = "M-Pesa",
                subtitle = "Instant payment via Safaricom",
                icon = { MpesaIcon() },
                isSelected = selectedMethod == PaymentMethod.MPESA,
                onClick = { onMethodSelected(PaymentMethod.MPESA) }
            )

            // Credit/Debit Card Option
            PaymentOptionCard(
                title = "Credit / Debit Card",
                subtitle = "Visa, Mastercard, Amex",
                icon = {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = "Card",
                        tint = Color(0xFF00236F),
                        modifier = Modifier.size(24.dp)
                    )
                },
                isSelected = selectedMethod == PaymentMethod.CARD,
                onClick = { onMethodSelected(PaymentMethod.CARD) }
            )

            // Bank Transfer Option
            PaymentOptionCard(
                title = "Bank Transfer",
                subtitle = "EFT or RTGS (1-2 business days)",
                icon = {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = "Bank",
                        tint = Color(0xFF3E2400),
                        modifier = Modifier.size(24.dp)
                    )
                },
                isSelected = selectedMethod == PaymentMethod.BANK_TRANSFER,
                onClick = { onMethodSelected(PaymentMethod.BANK_TRANSFER) }
            )
        }
    }
}

@Composable
fun PaymentOptionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF00236F) else Color(0xFFC5C5D3)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            title.contains("M-Pesa") -> Color(0xFF6CF8BB).copy(alpha = 0.2f)
                            title.contains("Card") -> Color(0xFF1E3A8A).copy(alpha = 0.1f)
                            else -> Color(0xFFFFDDD8)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF444651))
            }

            // Selection Indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF00236F) else Color(0xFFC5C5D3)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00236F))
                    )
                }
            }
        }
    }
}

@Composable
fun MpesaIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF49C51A)),
        contentAlignment = Alignment.Center
    ) {
        Text("M", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun TrustBadgeSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trust icons row
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verified badge icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF00236F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Shield icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Secure",
                        tint = Color(0xFF00236F),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                "Joined over 50,000+ Kenyans securing their transactions with EscrowX.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF444651),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun PaymentBottomButton(
    amount: Double,
    formattedAmount: String,
    buttonText: String,
    isProcessing: Boolean,
    isSuccess: Boolean,
    onPayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF9F9FF).copy(alpha = 0f), Color(0xFFF9F9FF))
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onPayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSuccess) Color(0xFF006C49) else Color(0xFF00236F)
                ),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                } else {
                    Icon(
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Funds will be held in escrow until you confirm delivery.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF444651).copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

