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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.CreateEscrowRequest
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.seller.SellerDashboardActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CreateEscrowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = intent.getStringExtra("ROLE") ?: "BUYER"
        setContent {
            MaterialTheme {
                UnifiedCreateEscrowScreen(role = role)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCreateEscrowScreen(role: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    val isBuyer = role.equals("BUYER", ignoreCase = true)
    // New label: for buyer, "Escrow User Contact", for seller, "Buyer Contact"
    val searchLabel = if (isBuyer) "Escrow User Contact" else "Buyer Contact"
    val addressLabel = "User's Delivery Address" // same for both

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedUserAddress by remember { mutableStateOf("") }

    var itemName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDeliveryDate by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val displayDate = selectedDeliveryDate?.let { dateFormatter.format(Date(it)) } ?: ""

    var searchResults by remember { mutableStateOf<List<UserDetailsResponse>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf(if (isBuyer) "Proceed to Payment" else "Initialize Escrow") }

    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val escrowFee = parsedAmount * 0.015
    val totalAmount = parsedAmount + escrowFee

    // Load logged‑in user profile (only needed for user ID and maybe role)
    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()
            if (!token.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        userProfile = response.body()
                    }
                } catch (_: Exception) { }
            }
            isLoading = false
        }
    }

    // Debounced search for the other party
    var searchJob: Job? by remember { mutableStateOf(null) }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            selectedUser = null
            selectedUserAddress = ""
            searchError = null
            return
        }
        isSearching = true
        searchError = null
        scope.launch {
            val token = session.getAccessToken()
            if (token.isNullOrBlank()) {
                searchError = "Session expired"
                isSearching = false
                return@launch
            }
            try {
                var foundUser: UserDetailsResponse? = null
                if (query.contains("@")) {
                    try {
                        val response = RetrofitClient.authenticated(token).getUserByEmail(query)
                        if (response.isSuccessful && response.body() != null) {
                            foundUser = response.body()
                        }
                    } catch (_: Exception) { }
                } else if (query.matches(Regex("^[0-9+]+$"))) {
                    val phoneQuery = when {
                        query.startsWith("0") -> "+254${query.substring(1)}"
                        query.startsWith("+254") -> query
                        else -> "+254$query"
                    }
                    try {
                        val response = RetrofitClient.authenticated(token).getUserByPhone(phoneQuery)
                        if (response.isSuccessful && response.body() != null) {
                            foundUser = response.body()
                        }
                    } catch (_: Exception) { }
                }
                if (foundUser != null) {
                    searchResults = listOf(foundUser)
                    // Always show the selected user's address
                    selectedUserAddress = foundUser.address ?: "No delivery address on file"
                } else {
                    searchResults = emptyList()
                    searchError = "No user found matching: $query"
                }
            } catch (e: Exception) {
                searchError = "Network error: ${e.message}"
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.isNotBlank() && selectedUser?.email != searchQuery && selectedUser?.phone != searchQuery) {
            searchJob = scope.launch {
                delay(500)
                performSearch(searchQuery)
            }
        } else if (searchQuery.isBlank()) {
            searchResults = emptyList()
            selectedUser = null
            selectedUserAddress = ""
            searchError = null
        }
    }

    fun selectUser(user: UserDetailsResponse) {
        selectedUser = user
        searchQuery = user.email
        searchResults = emptyList()
        searchError = null
        selectedUserAddress = user.address ?: "No delivery address on file"
        Toast.makeText(context, "User selected: ${user.displayName ?: user.email}", Toast.LENGTH_SHORT).show()
    }

    fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "KE"))
            .format(value)
            .replace("KES", "KES")
    }

    fun handleSubmit() {
        if (itemName.isBlank()) {
            Toast.makeText(context, "Please enter item name", Toast.LENGTH_SHORT).show()
            return
        }
        if (parsedAmount <= 0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedUser == null) {
            Toast.makeText(context, "Please select a valid user", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDeliveryDate == null) {
            Toast.makeText(context, "Please select a delivery date", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedUserAddress.isBlank() || selectedUserAddress == "No delivery address on file") {
            Toast.makeText(context, "Selected user has no delivery address. Please ask them to update their profile.", Toast.LENGTH_LONG).show()
            return
        }

        isProcessing = true
        buttonText = if (isBuyer) "Creating Escrow..." else "Initializing Escrow..."

        scope.launch {
            try {
                val token = session.getAccessToken()
                val loggedUserId = userProfile?.id ?: ""
                if (token.isNullOrBlank() || loggedUserId.isBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = if (isBuyer) "Proceed to Payment" else "Initialize Escrow"
                    return@launch
                }

                val calendar = Calendar.getInstance().apply { timeInMillis = selectedDeliveryDate!! }
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val deliveryDueAt = String.format("%04d-%02d-%02dT00:00:00+03:00", year, month, day)

                // Determine buyerId and sellerId based on role
                val buyerId: String
                val sellerId: String
                if (isBuyer) {
                    // Buyer creates escrow: buyer = logged user, seller = selected user
                    buyerId = loggedUserId
                    sellerId = selectedUser!!.id
                } else {
                    // Seller creates escrow: seller = logged user, buyer = selected user
                    buyerId = selectedUser!!.id
                    sellerId = loggedUserId
                }

                val request = CreateEscrowRequest(
                    buyerId = buyerId,
                    sellerId = sellerId,
                    title = itemName,
                    amount = String.format("%.2f", parsedAmount),
                    currency = "KES",
                    deliveryDueAt = deliveryDueAt
                )

                val response = RetrofitClient.authenticated(token).createEscrow(request)
                if (response.isSuccessful && response.body() != null) {
                    val escrow = response.body()!!
                    isSuccess = true
                    buttonText = "Success!"
                    Toast.makeText(context, "Escrow ${if (isBuyer) "created" else "initialized"} successfully!", Toast.LENGTH_LONG).show()

                    if (isBuyer) {
                        val intent = Intent(context, PaymentActivity::class.java).apply {
                            putExtra("ITEM_NAME", itemName)
                            putExtra("TRANSACTION_AMOUNT", parsedAmount)
                            putExtra("ESCROW_FEE", escrowFee)
                            putExtra("SELLER_NAME", selectedUser?.displayName ?: selectedUser?.email)
                            putExtra("TRANSACTION_ID", escrow.id)
                            putExtra("DELIVERY_DATE", displayDate)
                            putExtra("DELIVERY_METHOD", "Courier")
                            putExtra("DELIVERY_ADDRESS", selectedUserAddress)
                        }
                        context.startActivity(intent)
                    } else {
                        context.startActivity(Intent(context, SellerDashboardActivity::class.java))
                    }
                    (context as? CreateEscrowActivity)?.finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("Escrow creation failed: ${response.code()} - $errorBody")
                    Toast.makeText(context, "Failed to ${if (isBuyer) "create" else "initialize"} escrow: ${response.code()}", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = if (isBuyer) "Proceed to Payment" else "Initialize Escrow"
                    isSuccess = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                isProcessing = false
                buttonText = if (isBuyer) "Proceed to Payment" else "Initialize Escrow"
                isSuccess = false
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDeliveryDate = datePickerState.selectedDateMillis
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
        topBar = {
            TopAppBar(
                title = { Text("Create New Escrow", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? CreateEscrowActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FF), titleContentColor = Color(0xFF151C27))
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(80.dp), containerColor = Color(0xFFF9F9FF), tonalElevation = 0.dp) {
                val items = listOf("Home" to Icons.Default.Home, "Transactions" to Icons.Default.AccountBalanceWallet, "Profile" to Icons.Default.Person)
                items.forEach { (label, icon) ->
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            if (isBuyer) {
                                context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
                            } else {
                                context.startActivity(Intent(context, SellerDashboardActivity::class.java))
                            }
                        },
                        icon = { Icon(icon, contentDescription = label, tint = Color(0xFF00236F)) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00236F))
                }
            } else {
                Text("New Escrow Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF151C27))
                Spacer(modifier = Modifier.height(24.dp))

                // Item Info Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Item Name", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                            OutlinedTextField(value = itemName, onValueChange = { itemName = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g., iPhone 15 Pro") }, shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00236F), unfocusedBorderColor = Color(0xFFC5C5D3)))
                        }
                        Column {
                            Text("Transaction Amount (KES)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                            OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0.00") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00236F), unfocusedBorderColor = Color(0xFFC5C5D3)), trailingIcon = { Text("KES", fontSize = 12.sp, color = Color(0xFF444651)) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(searchLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; selectedUser = null; selectedUserAddress = ""; searchError = null },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by email or phone") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00236F), unfocusedBorderColor = Color(0xFFC5C5D3), errorBorderColor = Color.Red),
                            isError = searchError != null,
                            leadingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF00236F))
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF444651))
                                }
                            },
                            trailingIcon = {
                                if (selectedUser != null) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF10B981))
                                } else null
                            }
                        )
                        if (searchError != null && selectedUser == null) {
                            Text(searchError!!, fontSize = 11.sp, color = Color.Red, modifier = Modifier.padding(start = 4.dp))
                        }
                        if (searchResults.isNotEmpty() && selectedUser == null) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                                Column {
                                    searchResults.forEach { user ->
                                        Row(modifier = Modifier.fillMaxWidth().clickable { selectUser(user) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, null, tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(user.displayName ?: "User", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                Text(user.email, fontSize = 11.sp, color = Color(0xFF444651))
                                            }
                                        }
                                        if (user != searchResults.last()) HorizontalDivider(color = Color(0xFFEEEEEE))
                                    }
                                }
                            }
                        }
                        if (selectedUser != null) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EEFE))) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(text = selectedUser?.displayName ?: selectedUser?.email ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
                                        Text(text = "Email: ${selectedUser?.email ?: ""}", fontSize = 11.sp, color = Color(0xFF444651))
                                        Text(text = "Phone: ${selectedUser?.phone ?: ""}", fontSize = 11.sp, color = Color(0xFF444651))
                                    }
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        Text(if (selectedUser != null) "✓ User verified! Proceed." else "Enter email or phone number to search and verify.", fontSize = 12.sp, color = if (selectedUser != null) Color(0xFF10B981) else Color(0xFF444651))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delivery Date Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
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
                            isError = selectedDeliveryDate == null,
                            supportingText = {
                                if (selectedDeliveryDate == null) {
                                    Text("Delivery date is required", fontSize = 11.sp, color = Color.Red)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Address Card (shows selected user's address)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(addressLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = selectedUserAddress.ifEmpty { "Search and select a user to see their address" },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00236F),
                                unfocusedBorderColor = Color(0xFFC5C5D3),
                                disabledTextColor = Color(0xFF444651)
                            ),
                            isError = selectedUserAddress.isBlank() || selectedUserAddress == "No delivery address on file",
                            supportingText = {
                                if (selectedUserAddress.isBlank() || selectedUserAddress == "No delivery address on file") {
                                    Text("Selected user has no delivery address", fontSize = 11.sp, color = Color.Red)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Terms of Sale / Description", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                        OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Describe the item condition, inclusions, and agreed terms...") }, minLines = 4, maxLines = 4, shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00236F), unfocusedBorderColor = Color(0xFFC5C5D3)))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Transaction Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A8FF))
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Item Price", fontSize = 14.sp, color = Color(0xFF90A8FF).copy(alpha = 0.8f))
                            Text(formatCurrency(parsedAmount), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Escrow Fee (1.5%)", fontSize = 14.sp, color = Color(0xFF90A8FF).copy(alpha = 0.8f))
                            Text(formatCurrency(escrowFee), fontSize = 14.sp, color = Color(0xFF90A8FF))
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF90A8FF).copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total to Pay", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(formatCurrency(totalAmount), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6CF8BB))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { handleSubmit() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) Color(0xFF006C49) else Color(0xFF00236F)),
                    enabled = !isProcessing && selectedUser != null && selectedDeliveryDate != null &&
                            selectedUserAddress.isNotBlank() && selectedUserAddress != "No delivery address on file"
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    } else {
                        Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Shield, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Funds will be held securely in escrow until the transaction is completed.",
                    fontSize = 12.sp,
                    color = Color(0xFF444651),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}