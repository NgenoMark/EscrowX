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
import mobile.project.escrowx.UserDetailsResponse
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

    // Seller search results
    var sellerSuggestions by remember { mutableStateOf<List<UserDetailsResponse>>(emptyList()) }
    var selectedSeller by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

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

    // Search for seller by email or phone using backend API
    fun searchSeller(query: String) {
        if (query.length < 3) {
            sellerSuggestions = emptyList()
            showSuggestions = false
            searchError = null
            return
        }

        isSearching = true
        searchError = null

        scope.launch {
            try {
                val token = session.getAccessToken()
                if (token.isNullOrBlank()) {
                    searchError = "Session expired. Please login again."
                    isSearching = false
                    return@launch
                }

                var foundSeller: UserDetailsResponse? = null

                // Try to search by email first (if query contains @ symbol)
                if (query.contains("@")) {
                    try {
                        val emailResponse = RetrofitClient.authenticated(token).getUserByEmail(query)
                        if (emailResponse.isSuccessful && emailResponse.body() != null) {
                            foundSeller = emailResponse.body()
                        }
                    } catch (e: Exception) {
                        // Email search failed, continue to phone search
                    }
                }

                // If not found by email, try searching by phone
                if (foundSeller == null) {
                    try {
                        // Format phone number if needed (ensure it starts with +254)
                        val phoneQuery = if (query.startsWith("0")) {
                            "+254${query.substring(1)}"
                        } else if (query.startsWith("+254")) {
                            query
                        } else {
                            "+254$query"
                        }

                        val phoneResponse = RetrofitClient.authenticated(token).getUserByPhone(phoneQuery)
                        if (phoneResponse.isSuccessful && phoneResponse.body() != null) {
                            foundSeller = phoneResponse.body()
                        }
                    } catch (e: Exception) {
                        // Phone search failed
                    }
                }

                if (foundSeller != null) {
                    sellerSuggestions = listOf(foundSeller)
                    showSuggestions = true
                    searchError = null
                } else {
                    sellerSuggestions = emptyList()
                    showSuggestions = false
                    searchError = "No seller found with email or phone: $query"
                }

            } catch (e: Exception) {
                searchError = "Network error: ${e.message}"
                sellerSuggestions = emptyList()
                showSuggestions = false
            } finally {
                isSearching = false
            }
        }
    }

    // Debounce search to avoid too many API calls
    LaunchedEffect(sellerSearch) {
        if (sellerSearch.isNotBlank() && selectedSeller?.email != sellerSearch && selectedSeller?.phone != sellerSearch) {
            delay(500) // Debounce delay
            searchSeller(sellerSearch)
        } else if (sellerSearch.isBlank()) {
            selectedSeller = null
            sellerSuggestions = emptyList()
            showSuggestions = false
            searchError = null
        }
    }

    // Select a seller from suggestions
    fun selectSeller(seller: UserDetailsResponse) {
        selectedSeller = seller
        sellerSearch = seller.email
        showSuggestions = false
        searchError = null
        Toast.makeText(context, "Seller selected: ${seller.displayName ?: seller.email}", Toast.LENGTH_SHORT).show()
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
        if (selectedSeller == null) {
            Toast.makeText(context, "Please select a valid seller from search results", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isBlank()) {
            Toast.makeText(context, "Please enter terms of sale", Toast.LENGTH_SHORT).show()
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
                //     buyerId = currentUserId,
                //     sellerId = selectedSeller?.id,
                //     title = itemName,
                //     amount = parsedAmount,
                //     description = description,
                //     inspectionDays = inspectionDays,
                //     deliveryMethod = deliveryMethod
                // )
                // val response = RetrofitClient.authenticated(token).createEscrow(request)

                delay(1500) // Simulate API call

                isSuccess = true
                buttonText = "Success!"

                Toast.makeText(context, "Escrow created! Proceed to payment.", Toast.LENGTH_LONG).show()

                val intent = Intent(context, PaymentActivity::class.java).apply {
                    putExtra("ITEM_NAME", itemName)
                    putExtra("AMOUNT", totalAmount)
                    putExtra("TRANSACTION_AMOUNT", parsedAmount)
                    putExtra("ESCROW_FEE", escrowFee)
                    putExtra("SELLER_NAME", selectedSeller?.displayName ?: selectedSeller?.email)
                    putExtra("SELLER_EMAIL", selectedSeller?.email)
                    putExtra("SELLER_ID", selectedSeller?.id)
                }
                context.startActivity(intent)
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
            // Title
            Text(
                "New Escrow Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF151C27)
            )

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

            // Seller Info Card with Search Functionality
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

                    // Search Input Field
                    OutlinedTextField(
                        value = sellerSearch,
                        onValueChange = {
                            sellerSearch = it
                            selectedSeller = null
                            searchError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by email (e.g., seller@example.com) or phone (e.g., 0712345678)") },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00236F),
                            unfocusedBorderColor = Color(0xFFC5C5D3),
                            errorBorderColor = Color.Red
                        ),
                        isError = searchError != null,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF444651))
                        },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00236F)
                                )
                            } else if (selectedSeller != null) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF10B981)
                                )
                            } else {
                                null
                            }
                        }
                    )

                    // Search Error
                    if (searchError != null && selectedSeller == null) {
                        Text(
                            text = searchError!!,
                            fontSize = 11.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Selected Seller Info
                    if (selectedSeller != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EEFE))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = selectedSeller?.displayName ?: selectedSeller?.email ?: "",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00236F)
                                        )
                                        Text(
                                            text = "Email: ${selectedSeller?.email ?: ""}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF444651)
                                        )
                                        Text(
                                            text = "Phone: ${selectedSeller?.phone ?: ""}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF444651)
                                        )
                                        Text(
                                            text = "Role: ${selectedSeller?.role ?: "BUYER"}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF444651)
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified Seller",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Suggestions Dropdown
                    if (showSuggestions && sellerSuggestions.isNotEmpty() && selectedSeller == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column {
                                sellerSuggestions.forEach { seller ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectSeller(seller) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF00236F),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = seller.displayName ?: "User",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF151C27)
                                            )
                                            Text(
                                                text = seller.email,
                                                fontSize = 11.sp,
                                                color = Color(0xFF444651)
                                            )
                                            Text(
                                                text = seller.phone,
                                                fontSize = 11.sp,
                                                color = Color(0xFF444651)
                                            )
                                        }
                                    }
                                    if (seller != sellerSuggestions.last()) {
                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        if (selectedSeller != null) "✓ Seller verified! Proceed to create escrow."
                        else "Enter seller email or phone number to search and verify.",
                        fontSize = 12.sp,
                        color = if (selectedSeller != null) Color(0xFF10B981) else Color(0xFF444651)
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
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Inspection", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "$inspectionDays Days",
                                modifier = Modifier.clickable { inspectionExpanded = true }.padding(vertical = 8.dp),
                                fontSize = 14.sp,
                                color = Color(0xFF151C27)
                            )
                            DropdownMenu(expanded = inspectionExpanded, onDismissRequest = { inspectionExpanded = false }) {
                                listOf(1, 3, 5, 7).forEach { days ->
                                    DropdownMenuItem(
                                        text = { Text("$days Day${if (days > 1) "s" else ""}") },
                                        onClick = { inspectionDays = days; inspectionExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Delivery", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = deliveryMethod,
                                modifier = Modifier.clickable { deliveryExpanded = true }.padding(vertical = 8.dp),
                                fontSize = 14.sp,
                                color = Color(0xFF151C27)
                            )
                            DropdownMenu(expanded = deliveryExpanded, onDismissRequest = { deliveryExpanded = false }) {
                                listOf("Courier", "In-Person", "Digital").forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method) },
                                        onClick = { deliveryMethod = method; deliveryExpanded = false }
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

            // Submit Button
            Button(
                onClick = { handleSubmit() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) Color(0xFF006C49) else Color(0xFF00236F)),
                enabled = !isProcessing && selectedSeller != null
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

            Text(
                "Funds are held securely in trust until you verify the item.",
                fontSize = 12.sp, color = Color(0xFF444651), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CreateEscrowBottomNavigation() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    NavigationBar(modifier = Modifier.height(80.dp), containerColor = Color(0xFFF9F9FF), tonalElevation = 0.dp) {
        NavigationBarItem(selected = selectedTab == 0, onClick = {
            selectedTab = 0
            context.startActivity(Intent(context, BuyerDashboardActivity::class.java))
        }, icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) }, label = { Text("Home", fontSize = 11.sp) })

        NavigationBarItem(selected = selectedTab == 1, onClick = {
            selectedTab = 1
            context.startActivity(Intent(context, TransactionsActivity::class.java))
        }, icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Transactions", modifier = Modifier.size(24.dp)) }, label = { Text("Transactions", fontSize = 11.sp) })

        NavigationBarItem(selected = selectedTab == 2, onClick = {
            selectedTab = 2
            context.startActivity(Intent(context, ProfileActivity::class.java))
        }, icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) }, label = { Text("Profile", fontSize = 11.sp) })
    }
}