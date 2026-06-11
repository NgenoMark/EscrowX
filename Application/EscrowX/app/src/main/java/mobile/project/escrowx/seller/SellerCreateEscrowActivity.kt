package mobile.project.escrowx.seller

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobile.project.escrowx.CreateEscrowRequest
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.UserDetailsResponse
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.TransactionsActivity
import java.text.NumberFormat
import java.util.Locale

class SellerCreateEscrowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SellerCreateEscrowScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerCreateEscrowScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var itemName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var buyerSearch by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var buyerDeliveryAddress by remember { mutableStateOf("") }

    var searchResults by remember { mutableStateOf<List<UserDetailsResponse>>(emptyList()) }
    var selectedBuyer by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("Initialize Escrow") }

    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val escrowFee = parsedAmount * 0.015
    val totalAmount = parsedAmount + escrowFee

    // Load seller profile
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

    // Debounced search for buyer
    var searchJob: Job? by remember { mutableStateOf(null) }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            selectedBuyer = null
            buyerDeliveryAddress = ""
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
                } else {
                    searchResults = emptyList()
                    searchError = "No buyer found matching: $query"
                }
            } catch (e: Exception) {
                searchError = "Network error: ${e.message}"
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(buyerSearch) {
        searchJob?.cancel()
        if (buyerSearch.isNotBlank() && selectedBuyer?.email != buyerSearch && selectedBuyer?.phone != buyerSearch) {
            searchJob = scope.launch {
                delay(500)
                performSearch(buyerSearch)
            }
        } else if (buyerSearch.isBlank()) {
            searchResults = emptyList()
            selectedBuyer = null
            buyerDeliveryAddress = ""
            searchError = null
        }
    }

    fun selectBuyer(buyer: UserDetailsResponse) {
        selectedBuyer = buyer
        buyerSearch = buyer.email
        searchResults = emptyList()
        searchError = null
        buyerDeliveryAddress = buyer.deliveryAddress ?: "No delivery address on file"
        Toast.makeText(context, "Buyer selected: ${buyer.displayName ?: buyer.email}", Toast.LENGTH_SHORT).show()
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
        if (selectedBuyer == null) {
            Toast.makeText(context, "Please select a valid buyer", Toast.LENGTH_SHORT).show()
            return
        }
        if (buyerDeliveryAddress.isBlank() || buyerDeliveryAddress == "No delivery address on file") {
            Toast.makeText(context, "Buyer has no delivery address. Please ask them to update their profile.", Toast.LENGTH_LONG).show()
            return
        }

        isProcessing = true
        buttonText = "Initializing Escrow..."

        scope.launch {
            try {
                val token = session.getAccessToken()
                val sellerId = userProfile?.id ?: ""
                if (token.isNullOrBlank() || sellerId.isBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = "Initialize Escrow"
                    return@launch
                }

                // TODO: Add deliveryAddress to CreateEscrowRequest when backend supports it
                val request = CreateEscrowRequest(
                    buyerId = selectedBuyer!!.id,
                    sellerId = sellerId,
                    title = itemName,
                    amount = parsedAmount.toString(),
                    currency = "KES",
                    deliveryDueAt = null
                )

                val response = RetrofitClient.authenticated(token).createEscrow(request)
                if (response.isSuccessful) {
                    isSuccess = true
                    buttonText = "Success!"
                    Toast.makeText(context, "Escrow initialized successfully!", Toast.LENGTH_LONG).show()

                    val intent = Intent(context, SellerDashboardActivity::class.java)
                    context.startActivity(intent)
                    (context as? SellerCreateEscrowActivity)?.finish()
                } else {
                    Toast.makeText(context, "Failed to initialize escrow: ${response.code()}", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = "Initialize Escrow"
                    isSuccess = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                isProcessing = false
                buttonText = "Initialize Escrow"
                isSuccess = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Escrow", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151C27)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? SellerCreateEscrowActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00236F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FF), titleContentColor = Color(0xFF151C27))
            )
        },
        bottomBar = { SellerCreateEscrowBottomNavigation() }
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

                // Buyer Search Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Buyer Contact", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                        OutlinedTextField(
                            value = buyerSearch,
                            onValueChange = { buyerSearch = it; selectedBuyer = null; buyerDeliveryAddress = ""; searchError = null },
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
                                if (selectedBuyer != null) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF10B981))
                                } else null
                            }
                        )
                        if (searchError != null && selectedBuyer == null) {
                            Text(searchError!!, fontSize = 11.sp, color = Color.Red, modifier = Modifier.padding(start = 4.dp))
                        }
                        if (searchResults.isNotEmpty() && selectedBuyer == null) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                                Column {
                                    searchResults.forEach { buyer ->
                                        Row(modifier = Modifier.fillMaxWidth().clickable { selectBuyer(buyer) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, null, tint = Color(0xFF00236F), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(buyer.displayName ?: "User", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                Text(buyer.email, fontSize = 11.sp, color = Color(0xFF444651))
                                            }
                                        }
                                        if (buyer != searchResults.last()) HorizontalDivider(color = Color(0xFFEEEEEE))
                                    }
                                }
                            }
                        }
                        if (selectedBuyer != null) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EEFE))) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(text = selectedBuyer?.displayName ?: selectedBuyer?.email ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00236F))
                                        Text(text = "Email: ${selectedBuyer?.email ?: ""}", fontSize = 11.sp, color = Color(0xFF444651))
                                        Text(text = "Phone: ${selectedBuyer?.phone ?: ""}", fontSize = 11.sp, color = Color(0xFF444651))
                                    }
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        Text(if (selectedBuyer != null) "✓ Buyer verified! Proceed to initialize escrow." else "Enter buyer email or phone number to search and verify.", fontSize = 12.sp, color = if (selectedBuyer != null) Color(0xFF10B981) else Color(0xFF444651))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buyer Delivery Address Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Buyer's Delivery Address", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                        OutlinedTextField(
                            value = buyerDeliveryAddress,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Select a buyer to see their delivery address") },
                            shape = RoundedCornerShape(8.dp),
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00236F),
                                unfocusedBorderColor = Color(0xFFC5C5D3),
                                disabledTextColor = Color(0xFF444651)
                            ),
                            enabled = false
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)), elevation = CardDefaults.cardElevation(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Transaction Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A8FF))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Item Price", fontSize = 14.sp, color = Color(0xFF90A8FF).copy(alpha = 0.8f))
                            Text(formatCurrency(parsedAmount), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Escrow Fee (1.5%)", fontSize = 14.sp, color = Color(0xFF90A8FF).copy(alpha = 0.8f))
                            Text(formatCurrency(escrowFee), fontSize = 14.sp, color = Color(0xFF90A8FF))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF90A8FF).copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
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
                    enabled = !isProcessing && selectedBuyer != null && buyerDeliveryAddress.isNotBlank() && buyerDeliveryAddress != "No delivery address on file"
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    } else {
                        Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Shield, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Funds will be held securely in escrow until the buyer confirms receipt.", fontSize = 12.sp, color = Color(0xFF444651), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SellerCreateEscrowBottomNavigation() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    NavigationBar(modifier = Modifier.height(80.dp), containerColor = Color(0xFFF9F9FF), tonalElevation = 0.dp) {
        NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0; context.startActivity(Intent(context, SellerDashboardActivity::class.java)) }, icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) }, label = { Text("Home", fontSize = 11.sp) })
        NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; context.startActivity(Intent(context, TransactionsActivity::class.java)) }, icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Transactions", modifier = Modifier.size(24.dp)) }, label = { Text("Transactions", fontSize = 11.sp) })
        NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2; context.startActivity(Intent(context, ProfileActivity::class.java)) }, icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) }, label = { Text("Profile", fontSize = 11.sp) })
    }
}