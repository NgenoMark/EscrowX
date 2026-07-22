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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
        val currentStatus = intent.getStringExtra("STATUS") ?: "CREATED"

        setContent {
            EscrowXTheme(
                darkTheme = ThemePreferenceManager.rememberDarkModeEnabledState(),
                dynamicColor = false
            ) {
                TransactionDetailsScreen(
                    transactionId = transactionId,
                    sellerName = sellerName,
                    businessName = businessName,
                    itemTitle = itemTitle,
                    amount = amount,
                    currentStatus = currentStatus
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
    amount: String,
    currentStatus: String
) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var userProfile by remember { mutableStateOf<UserDetailsResponse?>(null) }
    var address by remember { mutableStateOf("") }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val displayDate = selectedDate?.let { dateFormatter.format(Date(it)) } ?: ""

    // Delivery method dropdown
    var deliveryMethodExpanded by remember { mutableStateOf(false) }
    val deliveryMethods = listOf("Courier", "In-Person", "Digital")
    var selectedDeliveryMethod by remember { mutableStateOf(deliveryMethods[0]) }

    // Escrow state flow dropdown
    val normalizedCurrentStatus = remember(currentStatus) { normalizeEscrowStatus(currentStatus) }
    val allowedNextStates = remember(normalizedCurrentStatus) { getAllowedNextStatuses(normalizedCurrentStatus) }
    var stateDropdownExpanded by remember { mutableStateOf(false) }
    var selectedNextState by remember(normalizedCurrentStatus) { mutableStateOf<String?>(null) }

    // Load buyer profile
    LaunchedEffect(Unit) {
        scope.launch {
            val token = session.getAccessToken()
            val userEmail = session.getEmail()
            if (!token.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.authenticated(token).getUserByEmail(userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        userProfile = response.body()
                        address = userProfile?.address ?: ""
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
                        "Transaction Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? TransactionDetailsActivity)?.finish() }) {
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ========== STATUS SECTION ==========
            StatusSection(
                currentStatus = normalizedCurrentStatus,
                selectedNextState = selectedNextState,
                allowedNextStates = allowedNextStates,
                onStateSelected = { selectedNextState = it },
                stateDropdownExpanded = stateDropdownExpanded,
                onDropdownExpandedChange = { stateDropdownExpanded = it },
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== ITEM & SELLER SUMMARY ==========
            ItemSellerCard(
                itemTitle = itemTitle,
                businessName = businessName,
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== DELIVERY DETAILS ==========
            DeliveryDetailsCard(
                displayDate = displayDate,
                selectedDate = selectedDate,
                onDateClick = { showDatePicker = true },
                selectedDeliveryMethod = selectedDeliveryMethod,
                deliveryMethods = deliveryMethods,
                onDeliveryMethodChange = { selectedDeliveryMethod = it },
                address = address,
                deliveryMethodExpanded = deliveryMethodExpanded,
                onDeliveryMethodExpandedChange = { deliveryMethodExpanded = it },
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== TRANSACTION SUMMARY ==========
            TransactionSummaryCard(
                parsedAmount = parsedAmount,
                escrowFee = escrowFee,
                totalAmount = totalAmount,
                formatCurrency = ::formatCurrency,
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== SECURITY NOTE ==========
            SecurityNoteCard(colorScheme = colorScheme)

            Spacer(modifier = Modifier.height(24.dp))

            // ========== PROCEED TO PAYMENT ==========
            ProceedToPaymentButton(
                selectedDate = selectedDate,
                address = address,
                onClick = {
                    if (selectedDate == null) {
                        Toast.makeText(context, "Please select a delivery date", Toast.LENGTH_SHORT).show()
                        return@ProceedToPaymentButton
                    }
                    if (address.isBlank()) {
                        Toast.makeText(context, "Your address not found. Please update your profile.", Toast.LENGTH_SHORT).show()
                        return@ProceedToPaymentButton
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
                colorScheme = colorScheme
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ===================== COMPOSABLE SECTIONS =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSection(
    currentStatus: String,
    selectedNextState: String?,
    allowedNextStates: List<String>,
    onStateSelected: (String) -> Unit,
    stateDropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    colorScheme: ColorScheme
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Escrow Status",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            }

            // Current Status
            val statusConfig = getTransactionDetailsStatusConfig(currentStatus, colorScheme)
            Surface(
                shape = RoundedCornerShape(50),
                color = statusConfig.backgroundColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusConfig.dotColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Current Status",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            statusConfig.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusConfig.textColor
                        )
                    }
                }
            }

            // Next State Dropdown
            if (allowedNextStates.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = stateDropdownExpanded,
                    onExpandedChange = onDropdownExpandedChange
                ) {
                    OutlinedTextField(
                        value = selectedNextState?.let { prettyEscrowState(it) }
                            ?: "Select next state",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = stateDropdownExpanded
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            cursorColor = colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = stateDropdownExpanded,
                        onDismissRequest = { onDropdownExpandedChange(false) }
                    ) {
                        allowedNextStates.forEach { next ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        prettyEscrowState(next),
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onStateSelected(next)
                                    onDropdownExpandedChange(false)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.TrendingFlat,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No further state transitions available",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemSellerCard(
    itemTitle: String,
    businessName: String,
    colorScheme: ColorScheme
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Item & Seller",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFD1FAE5)
                ) {
                    Text(
                        "Verified",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.primary,
                                    colorScheme.primary.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = "Product",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        itemTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Store",
                            modifier = Modifier.size(14.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                        Text(
                            businessName,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Agreement Terms
            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AgreementTermRow(
                    icon = Icons.Default.Shield,
                    text = "Funds held securely in EscrowX till inspection",
                    colorScheme = colorScheme
                )
                AgreementTermRow(
                    icon = Icons.Default.Timer,
                    text = "24-hour inspection period after delivery",
                    colorScheme = colorScheme
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailsCard(
    displayDate: String,
    selectedDate: Long?,
    onDateClick: () -> Unit,
    selectedDeliveryMethod: String,
    deliveryMethods: List<String>,
    onDeliveryMethodChange: (String) -> Unit,
    address: String,
    deliveryMethodExpanded: Boolean,
    onDeliveryMethodExpandedChange: (Boolean) -> Unit,
    colorScheme: ColorScheme
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
            Text(
                "Delivery Details",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            // Delivery Date
            OutlinedTextField(
                value = displayDate,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateClick() },
                readOnly = true,
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
                    cursorColor = colorScheme.primary
                ),
                isError = selectedDate == null,
                supportingText = {
                    if (selectedDate == null) {
                        Text(
                            "Delivery date is required",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            // Delivery Method
            ExposedDropdownMenuBox(
                expanded = deliveryMethodExpanded,
                onExpandedChange = onDeliveryMethodExpandedChange
            ) {
                OutlinedTextField(
                    value = selectedDeliveryMethod,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = deliveryMethodExpanded
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant,
                        cursorColor = colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = deliveryMethodExpanded,
                    onDismissRequest = { onDeliveryMethodExpandedChange(false) }
                ) {
                    deliveryMethods.forEach { method ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    method,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onDeliveryMethodChange(method)
                                onDeliveryMethodExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    when (method) {
                                        "Courier" -> Icons.Default.LocalShipping
                                        "In-Person" -> Icons.Default.Person
                                        else -> Icons.Default.Cloud
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            // Delivery Address
            OutlinedTextField(
                value = address.ifEmpty { "No address found. Please update your profile." },
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
                isError = address.isBlank(),
                supportingText = {
                    if (address.isBlank()) {
                        Text(
                            "Please add your address in the profile",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionSummaryCard(
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Payment Summary",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        "1.5% Fee",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            SummaryRow(
                label = "Item Price",
                value = formatCurrency(parsedAmount),
                icon = Icons.Default.ShoppingCart,
                colorScheme = colorScheme
            )

            SummaryRow(
                label = "Escrow Fee",
                value = formatCurrency(escrowFee),
                icon = Icons.Default.Shield,
                colorScheme = colorScheme,
                isFee = true
            )

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

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
                        Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.primary
                    )
                    Text(
                        "Total to Pay",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                Text(
                    formatCurrency(totalAmount),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SecurityNoteCard(colorScheme: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD1FAE5).copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFF10B981).copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Secure",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Protected Payment",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF065F46)
                )
                Text(
                    "Your payment is protected. The seller only receives funds after you confirm the item's condition.",
                    fontSize = 13.sp,
                    color = Color(0xFF065F46).copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ProceedToPaymentButton(
    selectedDate: Long?,
    address: String,
    onClick: () -> Unit,
    colorScheme: ColorScheme
) {
    val isEnabled = selectedDate != null && address.isNotBlank()

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        ),
        enabled = isEnabled
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Proceed to Payment",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            letterSpacing = 0.3.sp
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
    }
}

// ===================== HELPER COMPOSABLES =====================

@Composable
fun AgreementTermRow(
    icon: ImageVector,
    text: String,
    colorScheme: ColorScheme
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFF10B981)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    icon: ImageVector,
    colorScheme: ColorScheme,
    isFee: Boolean = false
) {
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
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isFee) colorScheme.onSurfaceVariant else colorScheme.onSurfaceVariant
            )
            Text(
                label,
                fontSize = 14.sp,
                color = if (isFee) colorScheme.onSurfaceVariant else colorScheme.onSurface
            )
        }
        Text(
            value,
            fontSize = if (isFee) 14.sp else 15.sp,
            fontWeight = if (isFee) FontWeight.Normal else FontWeight.SemiBold,
            color = if (isFee) colorScheme.onSurfaceVariant else colorScheme.onSurface
        )
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

// ===================== STATUS HELPERS =====================

private val ESCROW_STATE_TRANSITIONS: Map<String, List<String>> = mapOf(
    "CREATED" to listOf("DECLINED", "PENDING_PAYMENT", "CANCELLED", "EXPIRED"),
    "PENDING_PAYMENT" to listOf("FUNDS_HELD", "CANCELLED"),
    "FUNDS_HELD" to listOf("SELLER_ACCEPTED", "DISPUTED"),
    "SELLER_ACCEPTED" to listOf("IN_DELIVERY", "DISPUTED"),
    "IN_DELIVERY" to listOf("SELLER_DELIVERED", "DISPUTED"),
    "SELLER_DELIVERED" to listOf("BUYER_CONFIRMED_DELIVERED", "DISPUTED"),
    "BUYER_CONFIRMED_DELIVERED" to listOf("RELEASE_PENDING", "DISPUTED"),
    "RELEASE_PENDING" to listOf("RELEASE_PROCESSING"),
    "RELEASE_PROCESSING" to listOf("RELEASE_FAILED", "COMPLETED"),
    "DISPUTED" to listOf("REFUND_PENDING"),
    "REFUND_PENDING" to listOf("REFUND_PROCESSING"),
    "REFUND_PROCESSING" to listOf("REFUNDED")
)

private fun normalizeEscrowStatus(raw: String): String {
    return raw.trim().uppercase(Locale.getDefault())
}

private fun getAllowedNextStatuses(current: String): List<String> {
    return ESCROW_STATE_TRANSITIONS[current] ?: emptyList()
}

private fun prettyEscrowState(state: String): String {
    return state
        .lowercase(Locale.getDefault())
        .split("_")
        .joinToString(" ") { token -> token.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }
}

data class TransactionDetailsStatusConfig(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val backgroundColor: Color
)

fun getTransactionDetailsStatusConfig(status: String, colorScheme: ColorScheme): TransactionDetailsStatusConfig {
    return when (status.uppercase()) {
        "COMPLETED" -> TransactionDetailsStatusConfig(
            label = "Completed",
            dotColor = Color(0xFF10B981),
            textColor = Color(0xFF10B981),
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.12f)
        )
        "IN_DELIVERY" -> TransactionDetailsStatusConfig(
            label = "In Delivery",
            dotColor = Color(0xFFF59E0B),
            textColor = Color(0xFFF59E0B),
            backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
        )
        "FUNDS_HELD" -> TransactionDetailsStatusConfig(
            label = "Funds Held",
            dotColor = Color(0xFF3B82F6),
            textColor = Color(0xFF3B82F6),
            backgroundColor = Color(0xFF3B82F6).copy(alpha = 0.12f)
        )
        "CREATED" -> TransactionDetailsStatusConfig(
            label = "Created",
            dotColor = Color(0xFF6B7280),
            textColor = Color(0xFF6B7280),
            backgroundColor = Color(0xFF6B7280).copy(alpha = 0.12f)
        )
        "CANCELLED" -> TransactionDetailsStatusConfig(
            label = "Cancelled",
            dotColor = Color(0xFFEF4444),
            textColor = Color(0xFFEF4444),
            backgroundColor = Color(0xFFEF4444).copy(alpha = 0.12f)
        )
        "DELIVERED" -> TransactionDetailsStatusConfig(
            label = "Delivered",
            dotColor = Color(0xFF8B5CF6),
            textColor = Color(0xFF8B5CF6),
            backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)
        )
        else -> TransactionDetailsStatusConfig(
            label = status,
            dotColor = colorScheme.onSurfaceVariant,
            textColor = colorScheme.onSurfaceVariant,
            backgroundColor = colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
        )
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun TransactionDetailsScreenPreview() {
    EscrowXTheme(darkTheme = false) {
        TransactionDetailsScreen(
            transactionId = "TX-123456",
            sellerName = "Tech Haven KE",
            businessName = "Tech Haven KE",
            itemTitle = "iPhone 15 Pro Max - 256GB",
            amount = "165,000",
            currentStatus = "CREATED"
        )
    }
}

@Preview(showBackground = true, widthDp = 428, heightDp = 920)
@Composable
fun TransactionDetailsScreenPreviewDark() {
    EscrowXTheme(darkTheme = true) {
        TransactionDetailsScreen(
            transactionId = "TX-123456",
            sellerName = "Tech Haven KE",
            businessName = "Tech Haven KE",
            itemTitle = "iPhone 15 Pro Max - 256GB",
            amount = "165,000",
            currentStatus = "CREATED"
        )
    }
}
