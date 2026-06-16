package mobile.project.escrowx.dash
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.components.BuyerNavBar
import mobile.project.escrowx.ui.components.BuyerNavItem
import mobile.project.escrowx.ui.components.navigateTab
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        val sellerName = intent.getStringExtra("SELLER_NAME") ?: "Tech Haven KE"
        val businessName = intent.getStringExtra("BUSINESS_NAME") ?: sellerName
        val itemTitle = intent.getStringExtra("ITEM_TITLE") ?: "Wireless Mouse"
        val amount = intent.getStringExtra("AMOUNT") ?: "8,200"

        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                TransactionDetailsScreen(
                    transactionId = transactionId,
                    sellerName = sellerName,
                    businessName = businessName,
                    itemTitle = itemTitle,
                    amount = amount
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: String,
    sellerName: String,
    businessName: String,
    itemTitle: String,
    amount: String
) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var address by remember { mutableStateOf("") }          // ✅ changed to "address"

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val displayDate = selectedDate?.let { dateFormatter.format(Date(it)) } ?: ""

    // Delivery method dropdown
    var deliveryMethodExpanded by remember { mutableStateOf(false) }
    val deliveryMethods = listOf("Courier", "In-Person", "Digital")
    var selectedDeliveryMethod by remember { mutableStateOf(deliveryMethods[0]) }

    // Load buyer profile to get address
    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()
            if (!token.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        userProfile = response.body()
                        address = userProfile?.address ?: ""        // ✅ use "address"
                    }
                } catch (_: Exception) { }
            }
        }
    }

    val parsedAmount = amount.replace(",", "").toDoubleOrNull() ?: 0.0
    val escrowFee = parsedAmount * 0.015
    val totalAmount = parsedAmount + escrowFee

    fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "KE"))
            .format(value)
            .replace("KES", "KES")
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Accept Request",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? TransactionDetailsActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
            )
        },
        bottomBar = {
            TransactionDetailsBottomNavigation()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Item & Seller Summary Card (unchanged)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                itemTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = "Store",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF006C49)
                                )
                                Text(
                                    businessName,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF6CF8BB)
                            ) {
                                Text(
                                    "Verified Seller",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFF00714D)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFC5C5D3))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Agreement Terms",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF757682)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp), tint = Color(0xFF006C49))
                            Text("Funds held securely in EscrowX till inspection.", fontSize = 13.sp, color = Color(0xFF444651))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp), tint = Color(0xFF006C49))
                            Text("24-hour inspection period after delivery.", fontSize = 13.sp, color = Color(0xFF444651))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delivery Date Card (Interactive)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Delivery Date *", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = displayDate,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        readOnly = true,
                        placeholder = { Text("Select delivery date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00236F),
                            unfocusedBorderColor = Color(0xFFC5C5D3)
                        ),
                        isError = selectedDate == null,
                        supportingText = {
                            if (selectedDate == null) {
                                Text("Delivery date is required", fontSize = 11.sp, color = Color.Red)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delivery Method Card (Interactive Dropdown)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Delivery Method *", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = deliveryMethodExpanded,
                        onExpandedChange = { deliveryMethodExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDeliveryMethod,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryMethodExpanded) },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00236F),
                                unfocusedBorderColor = Color(0xFFC5C5D3)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = deliveryMethodExpanded,
                            onDismissRequest = { deliveryMethodExpanded = false }
                        ) {
                            deliveryMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        selectedDeliveryMethod = method
                                        deliveryMethodExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Address Card (Fetched from Profile) – label "Delivery Address" for buyer context
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Delivery Address *", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address.ifEmpty { "No address found. Please update your profile." },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00236F),
                            unfocusedBorderColor = Color(0xFFC5C5D3),
                            disabledTextColor = Color(0xFF444651)
                        ),
                        isError = address.isBlank(),
                        supportingText = {
                            if (address.isBlank()) {
                                Text("Please add your address in the profile", fontSize = 11.sp, color = Color.Red)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Summary Card (unchanged)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Transaction Summary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Item Price", fontSize = 14.sp, color = Color(0xFF444651))
                        Text(formatCurrency(parsedAmount), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Escrow Fee", fontSize = 14.sp, color = Color(0xFF444651))
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE7EEFE)
                            ) {
                                Text(
                                    "1.5%",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00236F)
                                )
                            }
                        }
                        Text(formatCurrency(escrowFee), fontSize = 14.sp, color = Color(0xFF444651))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFC5C5D3), modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total to Pay", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27))
                        Text(formatCurrency(totalAmount), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Security Note (unchanged)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6CF8BB).copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color(0xFF006C49).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = "Secure", tint = Color(0xFF006C49), modifier = Modifier.size(24.dp))
                    Text(
                        "Your payment is protected. The seller only receives the funds after you confirm the item's condition.",
                        fontSize = 13.sp,
                        color = Color(0xFF00714D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Proceed to Payment Button
            Button(
                onClick = {
                    if (selectedDate == null) {
                        Toast.makeText(context, "Please select a delivery date", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (address.isBlank()) {
                        Toast.makeText(context, "Your address not found. Please update your profile.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val intent = Intent(context, PaymentActivity::class.java).apply {
                        putExtra("ITEM_NAME", itemTitle)
                        putExtra("AMOUNT", totalAmount)
                        putExtra("TRANSACTION_AMOUNT", parsedAmount)
                        putExtra("ESCROW_FEE", escrowFee)
                        putExtra("SELLER_NAME", sellerName)
                        putExtra("TRANSACTION_ID", transactionId)
                        putExtra("DELIVERY_DATE", displayDate)
                        putExtra("DELIVERY_METHOD", selectedDeliveryMethod)
                        putExtra("DELIVERY_ADDRESS", address)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00236F),
                    contentColor = Color.White
                ),
                enabled = selectedDate != null && address.isNotBlank()
            ) {
                Text("Proceed to Payment", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TransactionDetailsBottomNavigation() {
    val context = LocalContext.current
    BuyerNavBar(
        selectedIndex = BuyerNavItem.Transactions.index,
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
                BuyerNavItem.Profile -> navigateTab(context, ProfileActivity::class.java)
            }
        }
    )
}