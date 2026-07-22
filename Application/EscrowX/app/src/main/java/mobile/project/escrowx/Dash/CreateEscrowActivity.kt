package mobile.project.escrowx.dash

import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
                dynamicColor = false
            ) {
                UnifiedCreateEscrowScreen(role = role)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCreateEscrowScreen(role: String) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)

    val searchLabel = "Buyer Contact"
    val addressLabel = "Delivery Address"
    val pageTitle = "Initialize Escrow"

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
    var buttonText by remember { mutableStateOf("Initialize Escrow") }

    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val escrowFee = parsedAmount * 0.015
    val totalAmount = parsedAmount + escrowFee

    // Load logged‑in user profile
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

    // Debounced search
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
        if (description.isBlank()) {
            Toast.makeText(context, "Please enter item description", Toast.LENGTH_SHORT).show()
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
        buttonText = "Initializing Escrow..."

        scope.launch {
            try {
                val token = session.getAccessToken()
                val loggedUserId = userProfile?.id ?: ""
                if (token.isNullOrBlank() || loggedUserId.isBlank()) {
                    Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    buttonText = "Initialize Escrow"
                    return@launch
                }

                val calendar = Calendar.getInstance().apply { timeInMillis = selectedDeliveryDate!! }
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val deliveryDueAt = String.format("%04d-%02d-%02dT12:00:00Z", year, month, day)

                // Business rule: creator is always the seller.
                val buyerId: String = selectedUser!!.id
                val sellerId: String = loggedUserId

                val request = CreateEscrowRequest(
                    buyerId = buyerId,
                    sellerId = sellerId,
                    title = itemName,
                    productDescription = description.trim(),
                    amount = totalAmount,
                    deliveryAddress = selectedUserAddress.trim(),
                    deliveryDueAt = deliveryDueAt
                )

                val response = RetrofitClient.authenticated(token).createEscrow(request)
                if (response.isSuccessful && response.body() != null) {
                    isSuccess = true
                    buttonText = "Success!"
                    Toast.makeText(context, "Escrow initialized successfully!", Toast.LENGTH_LONG).show()
                    context.startActivity(Intent(context, SellerDashboardActivity::class.java))
                    (context as? CreateEscrowActivity)?.finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("Escrow creation failed: ${response.code()} - $errorBody")
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
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        pageTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? CreateEscrowActivity)?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Loading...",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // ===== PROGRESS INDICATOR =====
                CreateEscrowProgress(
                    currentStep = when {
                        selectedUser != null && selectedDeliveryDate != null && itemName.isNotBlank() && description.isNotBlank() -> 4
                        selectedUser != null && selectedDeliveryDate != null && itemName.isNotBlank() -> 3
                        selectedUser != null && selectedDeliveryDate != null -> 2
                        selectedUser != null -> 1
                        else -> 0
                    },
                    colorScheme = colorScheme
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ===== ITEM DETAILS SECTION =====
                SectionCard(
                    title = "Item Details",
                    icon = Icons.Default.ShoppingBag,
                    colorScheme = colorScheme
                ) {
                    CreateEscrowTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = "Item Name",
                        placeholder = "e.g., iPhone 15 Pro Max",
                        leadingIcon = Icons.Default.ShoppingBag,
                        colorScheme = colorScheme
                    )

                    CreateEscrowTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Transaction Amount",
                        placeholder = "0.00",
                        leadingIcon = Icons.Default.Money,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingText = "KES",
                        colorScheme = colorScheme
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== PARTY SEARCH SECTION =====
                SectionCard(
                    title = searchLabel,
                    icon = Icons.Default.PersonSearch,
                    colorScheme = colorScheme
                ) {
                    PartySearchSection(
                        searchQuery = searchQuery,
                        onSearchQueryChange = {
                            searchQuery = it
                            selectedUser = null
                            selectedUserAddress = ""
                            searchError = null
                        },
                        isSearching = isSearching,
                        searchResults = searchResults,
                        searchError = searchError,
                        selectedUser = selectedUser,
                        onSelectUser = { user ->
                            selectUser(user)
                        },
                        colorScheme = colorScheme
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== DELIVERY DETAILS SECTION =====
                SectionCard(
                    title = "Delivery Details",
                    icon = Icons.Default.LocalShipping,
                    colorScheme = colorScheme
                ) {
                    // Delivery Date
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = displayDate,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            placeholder = {
                                Text(
                                    "Select delivery date",
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outlineVariant,
                                disabledBorderColor = colorScheme.outlineVariant,
                                disabledTextColor = colorScheme.onSurface,
                                disabledLeadingIconColor = colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                cursorColor = colorScheme.primary
                            ),
                            isError = selectedDeliveryDate == null,
                            supportingText = {
                                if (selectedDeliveryDate == null) {
                                    Text(
                                        "Delivery date is required",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delivery Address
                    OutlinedTextField(
                        value = selectedUserAddress.ifEmpty { "Search and select a user to see their address" },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            disabledTextColor = colorScheme.onSurface,
                            cursorColor = colorScheme.primary
                        ),
                        isError = selectedUserAddress.isBlank() || selectedUserAddress == "No delivery address on file",
                        supportingText = {
                            if (selectedUserAddress.isBlank() || selectedUserAddress == "No delivery address on file") {
                                Text(
                                    "Selected user has no delivery address",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== TERMS & DESCRIPTION SECTION =====
                SectionCard(
                    title = "Terms & Description",
                    icon = Icons.Default.Description,
                    colorScheme = colorScheme
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Describe the item condition, inclusions, and agreed terms...",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        minLines = 4,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            cursorColor = colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== TRANSACTION SUMMARY =====
                TransactionSummary(
                    parsedAmount = parsedAmount,
                    escrowFee = escrowFee,
                    totalAmount = totalAmount,
                    formatCurrency = ::formatCurrency,
                    colorScheme = colorScheme
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ===== SUBMIT BUTTON =====
                Button(
                    onClick = { handleSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) Color(0xFF10B981) else colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    ),
                    enabled = !isProcessing && selectedUser != null && selectedDeliveryDate != null &&
                            selectedUserAddress.isNotBlank() && selectedUserAddress != "No delivery address on file"
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            buttonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Initialize Escrow",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Security Note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Funds held securely in escrow until completion",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ===================== COMPOSABLE COMPONENTS =====================

@Composable
fun CreateEscrowProgress(
    currentStep: Int,
    colorScheme: ColorScheme
) {
    val steps = listOf("Party", "Delivery", "Item", "Terms")
    val totalSteps = steps.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep
            val isUpcoming = index > currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> Color(0xFF10B981)
                                isCurrent -> colorScheme.primary
                                else -> colorScheme.outlineVariant.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    } else {
                        Text(
                            (index + 1).toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) Color.White else colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    label,
                    fontSize = 9.sp,
                    color = when {
                        isCompleted -> Color(0xFF10B981)
                        isCurrent -> colorScheme.primary
                        else -> colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .height(2.dp)
                        .background(
                            if (isCompleted) Color(0xFF10B981)
                            else colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    colorScheme: ColorScheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.primary
                )
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
fun CreateEscrowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingText: String? = null,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = trailingText?.let {
                {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                cursorColor = colorScheme.primary
            ),
            keyboardOptions = keyboardOptions
        )
    }
}

@Composable
fun PartySearchSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearching: Boolean,
    searchResults: List<UserDetailsResponse>,
    searchError: String?,
    selectedUser: UserDetailsResponse?,
    onSelectUser: (UserDetailsResponse) -> Unit,
    colorScheme: ColorScheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search by email or phone",
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                if (selectedUser != null) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF10B981)
                    )
                } else if (searchQuery.isNotBlank() && !isSearching && searchError == null && searchResults.isEmpty()) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = "No user found",
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = colorScheme.primary
            ),
            isError = searchError != null
        )

        // Error Message
        if (searchError != null && selectedUser == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    searchError!!,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Search Results
        if (searchResults.isNotEmpty() && selectedUser == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column {
                    searchResults.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectUser(user) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (user.displayName?.take(2) ?: user.email.take(2)).uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    user.displayName ?: "User",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    user.email,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Select",
                                modifier = Modifier.size(16.dp),
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                        if (user != searchResults.last()) {
                            HorizontalDivider(
                                color = colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }

        // Selected User
        if (selectedUser != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.primary.copy(alpha = 0.06f)
                ),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (selectedUser.displayName?.take(2) ?: selectedUser.email.take(2)).uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(
                                selectedUser.displayName ?: selectedUser.email,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                "Email: ${selectedUser.email}",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                            selectedUser.phone?.let {
                                Text(
                                    "Phone: $it",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Verified",
                            modifier = Modifier
                                .size(20.dp)
                                .padding(4.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionSummary(
    parsedAmount: Double,
    escrowFee: Double,
    totalAmount: Double,
    formatCurrency: (Double) -> String,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                    Text(
                        "Transaction Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorScheme.onPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "1.5% Fee",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Item Price",
                    fontSize = 13.sp,
                    color = colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Text(
                    formatCurrency(parsedAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Escrow Fee",
                    fontSize = 13.sp,
                    color = colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Text(
                    formatCurrency(escrowFee),
                    fontSize = 14.sp,
                    color = colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            HorizontalDivider(
                color = colorScheme.onPrimary.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF6CF8BB)
                    )
                    Text(
                        "Total Amount",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onPrimary
                    )
                }
                Text(
                    formatCurrency(totalAmount),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6CF8BB)
                )
            }
        }
    }
}

// ===== PREVIEW =====

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun CreateEscrowPreview() {
    EscrowXTheme(darkTheme = false) {
        UnifiedCreateEscrowScreen(role = "BUYER")
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun CreateEscrowPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        UnifiedCreateEscrowScreen(role = "BUYER")
    }
}