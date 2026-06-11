@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.dash

import android.content.Context
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
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import java.util.Locale

enum class PaymentMethod {
    MPESA,
    CARD,
    BANK_TRANSFER
}

class PaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemName = intent.getStringExtra("ITEM_NAME") ?: "Item"
        val transactionAmount = intent.getDoubleExtra("TRANSACTION_AMOUNT", 0.0)
        val escrowFee = intent.getDoubleExtra("ESCROW_FEE", 0.0)
        val sellerName = intent.getStringExtra("SELLER_NAME") ?: "Seller"
        val deliveryDate = intent.getStringExtra("DELIVERY_DATE") ?: ""
        val deliveryMethod = intent.getStringExtra("DELIVERY_METHOD") ?: ""
        val deliveryAddress = intent.getStringExtra("DELIVERY_ADDRESS") ?: ""

        setContent {
            MaterialTheme {
                PaymentScreen(
                    itemName = itemName,
                    transactionAmount = transactionAmount,
                    escrowFee = escrowFee,
                    sellerName = sellerName,
                    deliveryDate = deliveryDate,
                    deliveryMethod = deliveryMethod,
                    deliveryAddress = deliveryAddress
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    itemName: String,
    transactionAmount: Double,
    escrowFee: Double,
    sellerName: String,
    deliveryDate: String,
    deliveryMethod: String,
    deliveryAddress: String
) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var registeredPhone by remember { mutableStateOf("") }

    // Load user profile to get registered phone number
    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()
            if (!token.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        userProfile = response.body()
                        registeredPhone = userProfile?.phone ?: ""
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // Load saved M-Pesa numbers from SharedPreferences
    fun getSavedPaymentNumbers(): List<String> {
        val prefs = context.getSharedPreferences("escrowx_payment", Context.MODE_PRIVATE)
        return prefs.getStringSet("payment_numbers", emptySet())?.toList() ?: emptyList()
    }

    // Combine registered phone with saved numbers (avoid duplicates)
    fun getMpesaNumbers(): List<String> {
        val saved = getSavedPaymentNumbers().toMutableList()
        if (registeredPhone.isNotBlank() && !saved.contains(registeredPhone)) {
            saved.add(0, registeredPhone) // put registered phone at the top
        }
        return saved
    }

    var paymentNumbers by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMpesaNumber by remember { mutableStateOf("") }
    var showNumberDropdown by remember { mutableStateOf(false) }

    // Update numbers when registeredPhone changes or saved numbers change
    LaunchedEffect(registeredPhone) {
        paymentNumbers = getMpesaNumbers()
        if (paymentNumbers.isNotEmpty()) {
            selectedMpesaNumber = paymentNumbers.first()
        }
    }

    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.MPESA) }
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val totalAmount = transactionAmount + escrowFee
    val formattedTotal = String.format(Locale.US, "%,.0f", totalAmount)
    var buttonText by remember { mutableStateOf("Pay KES $formattedTotal") }

    fun handlePayment() {
        if (selectedPaymentMethod == PaymentMethod.MPESA && selectedMpesaNumber.isBlank()) {
            Toast.makeText(context, "Please select an M-Pesa number", Toast.LENGTH_SHORT).show()
            return
        }
        isProcessing = true
        buttonText = "Processing..."

        scope.launch {
            try {
                val token = session.getAccessToken()
                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = "Pay KES $formattedTotal"
                    return@launch
                }

                // TODO: Call actual payment API (STK push, etc.)
                delay(1500)

                isSuccess = true
                buttonText = "Payment Successful!"
                Toast.makeText(
                    context,
                    "Payment of KES $formattedTotal successful via ${if (selectedPaymentMethod == PaymentMethod.MPESA) "M-Pesa ($selectedMpesaNumber)" else selectedPaymentMethod.name}",
                    Toast.LENGTH_LONG
                ).show()

                delay(2000)
                isProcessing = false
                isSuccess = false
                buttonText = "Pay KES $formattedTotal"

                context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                (context as? PaymentActivity)?.finish()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                isProcessing = false
                buttonText = "Pay KES $formattedTotal"
                isSuccess = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Method", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? PaymentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FF), titleContentColor = Color(0xFF151C27))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TransactionSummaryCard(
                    itemName = itemName,
                    transactionAmount = transactionAmount,
                    escrowFee = escrowFee,
                    sellerName = sellerName,
                    deliveryDate = deliveryDate,
                    deliveryMethod = deliveryMethod,
                    deliveryAddress = deliveryAddress,
                    formattedTotal = formattedTotal
                )
                PaymentMethodSelection(
                    selectedMethod = selectedPaymentMethod,
                    onMethodSelected = { selectedPaymentMethod = it },
                    mpesaNumbers = paymentNumbers,
                    selectedMpesaNumber = selectedMpesaNumber,
                    onMpesaNumberSelected = { selectedMpesaNumber = it },
                    showNumberDropdown = showNumberDropdown,
                    onDropdownToggle = { showNumberDropdown = !showNumberDropdown }
                )
                TrustBadgeSection()
                Spacer(modifier = Modifier.height(16.dp))
            }
            PaymentBottomButton(
                buttonText = buttonText,
                isProcessing = isProcessing,
                isSuccess = isSuccess,
                onPayClick = { handlePayment() }
            )
        }
    }
}

@Composable
fun TransactionSummaryCard(
    itemName: String,
    transactionAmount: Double,
    escrowFee: Double,
    sellerName: String,
    deliveryDate: String,
    deliveryMethod: String,
    deliveryAddress: String,
    formattedTotal: String
) {
    val formattedTransaction = String.format(Locale.US, "%,.0f", transactionAmount)
    val formattedFee = String.format(Locale.US, "%,.0f", escrowFee)

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
                    Text("PURCHASING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF444651), letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(itemName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                    Spacer(Modifier.height(4.dp))
                    Text("Seller: $sellerName", fontSize = 12.sp, color = Color(0xFF444651))
                    if (deliveryDate.isNotBlank()) {
                        Text("Delivery: $deliveryMethod on $deliveryDate", fontSize = 12.sp, color = Color(0xFF444651))
                    }
                    if (deliveryAddress.isNotBlank()) {
                        Text("Address: $deliveryAddress", fontSize = 12.sp, color = Color(0xFF444651))
                    }
                }
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE7EEFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = "Item", tint = Color(0xFF00236F), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFC5C5D3))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Item Price", fontSize = 14.sp, color = Color(0xFF444651))
                Text("KES $formattedTransaction", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Escrow Fee (1.5%)", fontSize = 14.sp, color = Color(0xFF444651))
                Text("KES $formattedFee", fontSize = 14.sp, color = Color(0xFF444651))
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFC5C5D3))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total Amount", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                Text("KES $formattedTotal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
            }
        }
    }
}

@Composable
fun PaymentMethodSelection(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
    mpesaNumbers: List<String>,
    selectedMpesaNumber: String,
    onMpesaNumberSelected: (String) -> Unit,
    showNumberDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Select Payment Method", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(Color(0xFF6CF8BB).copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Verified, contentDescription = "Secure", modifier = Modifier.size(14.dp), tint = Color(0xFF006C49))
                Spacer(Modifier.width(4.dp))
                Text("Secure", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF006C49))
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentOptionCard(
                title = "M-Pesa",
                subtitle = "Instant payment via Safaricom",
                icon = { MpesaIcon() },
                isSelected = selectedMethod == PaymentMethod.MPESA,
                onClick = { onMethodSelected(PaymentMethod.MPESA) }
            )
            if (selectedMethod == PaymentMethod.MPESA && mpesaNumbers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3FF)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select M-Pesa Number", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00236F))
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedMpesaNumber,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable { onDropdownToggle() },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00236F),
                                    unfocusedBorderColor = Color(0xFFC5C5D3)
                                )
                            )
                            DropdownMenu(
                                expanded = showNumberDropdown,
                                onDismissRequest = { onDropdownToggle() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                mpesaNumbers.forEach { number ->
                                    DropdownMenuItem(
                                        text = { Text(number) },
                                        onClick = {
                                            onMpesaNumberSelected(number)
                                            onDropdownToggle()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            PaymentOptionCard(
                title = "Credit / Debit Card",
                subtitle = "Visa, Mastercard, Amex",
                icon = { Icon(Icons.Default.CreditCard, contentDescription = "Card", tint = Color(0xFF00236F), modifier = Modifier.size(24.dp)) },
                isSelected = selectedMethod == PaymentMethod.CARD,
                onClick = { onMethodSelected(PaymentMethod.CARD) }
            )
            PaymentOptionCard(
                title = "Bank Transfer",
                subtitle = "EFT or RTGS (1-2 business days)",
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Bank", tint = Color(0xFF3E2400), modifier = Modifier.size(24.dp)) },
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
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(
                    when {
                        title.contains("M-Pesa") -> Color(0xFF6CF8BB).copy(alpha = 0.2f)
                        title.contains("Card") -> Color(0xFF1E3A8A).copy(alpha = 0.1f)
                        else -> Color(0xFFFFDDD8)
                    }
                ),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF444651))
            }
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).border(BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF00236F) else Color(0xFFC5C5D3)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF00236F)))
                }
            }
        }
    }
}

@Composable
fun MpesaIcon() {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF49C51A)),
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
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF00236F), modifier = Modifier.size(24.dp))
                }
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Shield, contentDescription = "Secure", tint = Color(0xFF00236F), modifier = Modifier.size(24.dp))
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
    buttonText: String,
    isProcessing: Boolean,
    isSuccess: Boolean,
    onPayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9FF))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onPayClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) Color(0xFF006C49) else Color(0xFF00236F)),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                } else {
                    Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Lock, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
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