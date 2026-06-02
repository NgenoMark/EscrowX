package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager
import java.text.NumberFormat
import java.util.Locale

class CreateEscrowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CreateEscrowScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEscrowScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    // Form fields
    var itemName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var sellerSearch by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var inspectionDays by remember { mutableIntStateOf(3) }
    var deliveryMethod by remember { mutableStateOf("Courier") }

    // Summary calculations
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val escrowFee = parsedAmount * 0.015
    val totalAmount = parsedAmount + escrowFee

    // Button states
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("Proceed to Payment") }

    // Dropdown states
    var inspectionExpanded by remember { mutableStateOf(false) }
    var deliveryExpanded by remember { mutableStateOf(false) }

    // Format currency
    fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "KE"))
            .format(value)
            .replace("KES", "KES")
    }

    // Handle form submission and navigate to payment
    fun handleSubmit() {
        if (itemName.isBlank()) {
            Toast.makeText(context, "Please enter item name", Toast.LENGTH_SHORT).show()
            return
        }
        if (parsedAmount <= 0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (sellerSearch.isBlank()) {
            Toast.makeText(context, "Please enter seller email or phone", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        buttonText = "Creating Escrow..."

        scope.launch {
            try {
                val token = session.getAccessToken()
                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = "Proceed to Payment"
                    return@launch
                }

                // TODO: Implement actual API call to create escrow
                // val request = CreateEscrowRequest(
                //     itemName = itemName,
                //     amount = parsedAmount,
                //     sellerEmail = sellerSearch,
                //     description = description,
                //     inspectionDays = inspectionDays,
                //     deliveryMethod = deliveryMethod
                // )
                // val response = RetrofitClient.authenticated(token).createEscrow(request)

                delay(1500) // Simulate API call

                // Success - Navigate to PaymentActivity
                isSuccess = true
                buttonText = "Success!"

                Toast.makeText(context, "Escrow created! Proceed to payment.", Toast.LENGTH_LONG).show()

                // Navigate to Payment Activity with transaction details
                val intent = Intent(context, PaymentActivity::class.java).apply {
                    putExtra("ITEM_NAME", itemName)
                    putExtra("AMOUNT", totalAmount)
                    putExtra("TRANSACTION_AMOUNT", parsedAmount)
                    putExtra("ESCROW_FEE", escrowFee)
                    putExtra("SELLER_EMAIL", sellerSearch)
                }
                context.startActivity(intent)

                // Close current activity
                (context as? CreateEscrowActivity)?.finish()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                isProcessing = false
                buttonText = "Proceed to Payment"
                isSuccess = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create New Escrow",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? CreateEscrowActivity)?.finish()
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
        },
        bottomBar = {
            CreateEscrowBottomNavigation()
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
            // Step Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "New Escrow Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF151C27)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .background(Color(0xFF00236F), RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .background(Color(0xFFC5C5D3), RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Item Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Item Name Field
                    Column {
                        Text(
                            "Item Name",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF444651),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., iPhone 15 Pro") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00236F),
                                unfocusedBorderColor = Color(0xFFC5C5D3)
                            )
                        )
                    }

                    // Transaction Amount Field
                    Column {
                        Text(
                            "Transaction Amount (KES)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF444651),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00236F),
                                unfocusedBorderColor = Color(0xFFC5C5D3)
                            ),
                            trailingIcon = {
                                Text("KES", fontSize = 12.sp, color = Color(0xFF444651))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seller Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Seller Contact",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF444651)
                    )
                    OutlinedTextField(
                        value = sellerSearch,
                        onValueChange = { sellerSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search Seller") },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00236F),
                            unfocusedBorderColor = Color(0xFFC5C5D3)
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF444651))
                        }
                    )
                    Text(
                        "We'll invite the seller to join this transaction.",
                        fontSize = 12.sp,
                        color = Color(0xFF444651)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Terms of Sale / Description",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF444651)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Describe the item condition, inclusions, and agreed terms...") },
                        minLines = 4,
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00236F),
                            unfocusedBorderColor = Color(0xFFC5C5D3)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Grid - Inspection & Delivery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Inspection Days Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Inspection",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF444651),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "$inspectionDays Days",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { inspectionExpanded = true }
                                    .padding(vertical = 8.dp),
                                fontSize = 14.sp,
                                color = Color(0xFF151C27)
                            )
                            DropdownMenu(
                                expanded = inspectionExpanded,
                                onDismissRequest = { inspectionExpanded = false }
                            ) {
                                listOf(1, 3, 5, 7).forEach { days ->
                                    DropdownMenuItem(
                                        text = { Text("$days Day${if (days > 1) "s" else ""}") },
                                        onClick = {
                                            inspectionDays = days
                                            inspectionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Delivery Method Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Delivery",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF444651),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = deliveryMethod,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { deliveryExpanded = true }
                                    .padding(vertical = 8.dp),
                                fontSize = 14.sp,
                                color = Color(0xFF151C27)
                            )
                            DropdownMenu(
                                expanded = deliveryExpanded,
                                onDismissRequest = { deliveryExpanded = false }
                            ) {
                                listOf("Courier", "In-Person", "Digital").forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method) },
                                        onClick = {
                                            deliveryMethod = method
                                            deliveryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Transaction Summary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A8FF)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Item Price", fontSize = 14.sp, color = Color(0xFF90A8FF).copy(alpha = 0.8f))
                        Text(
                            formatCurrency(parsedAmount),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Escrow Fee (1.5%)", fontSize = 14.sp, color = Color(0xFF90A8FF).copy(alpha = 0.8f))
                        Text(
                            formatCurrency(escrowFee),
                            fontSize = 14.sp,
                            color = Color(0xFF90A8FF)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(color = Color(0xFF90A8FF).copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total to Pay", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            formatCurrency(totalAmount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6CF8BB)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = { handleSubmit() },
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
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Funds are held securely in trust until you verify the item.",
                fontSize = 12.sp,
                color = Color(0xFF444651),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CreateEscrowBottomNavigation() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = Color(0xFFF9F9FF),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = {
                selectedTab = 0
                context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
            label = { Text("Home", fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = {
                selectedTab = 1
                context.startActivity(Intent(context, TransactionsActivity::class.java))
            },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Transactions", modifier = Modifier.size(24.dp)) },
            label = { Text("Transactions", fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = {
                selectedTab = 2
                context.startActivity(Intent(context, ProfileActivity::class.java))
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) },
            label = { Text("Profile", fontSize = 11.sp) }
        )
    }
}