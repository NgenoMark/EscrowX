@file:Suppress("SpellCheckingInspection")

package mobile.project.escrowx.dash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CreateEscrowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CreateEscrowScreen(onBackClick = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEscrowScreen(onBackClick: () -> Unit = {}) {
    var searchQuery by remember { mutableStateOf("") }
    var transactionAmount by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var selectedTimeline by remember { mutableStateOf("1-3 days") }

    // Business Math Computations
    val amountVal = transactionAmount.toDoubleOrNull() ?: 0.0
    val escrowFee = if (amountVal > 0) amountVal * 0.015 else 0.0 // 1.5% Escrow Guard Fee
    val totalToPay = amountVal + escrowFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Escrow", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Help context */ }) {
                        Icon(imageVector = Icons.Filled.HelpOutline, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9FF)
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = Color(0xFFF9F9FF),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { /* Proceed execution payload */ },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00236F)),
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text("Proceed to Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(0.dp, Color.Transparent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF444651))
                    ) {
                        Text("Cancel Transaction", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        containerColor = Color(0xFFF9F9FF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("New Transaction", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF151C27))
            Text("Secure your purchase by setting up a conditional payment.", fontSize = 14.sp, color = Color(0xFF444651), modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Seller Selector
            Text("Search or select seller", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter phone number or username", color = Color(0xFF757682).copy(alpha = 0.6f)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF757682)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00236F),
                    unfocusedBorderColor = Color(0xFFC5C5D3)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Recent Sellers Row List
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SellerAvatarItem(name = "New", isAction = true) {}
                SellerAvatarItem(name = "Kamau", isAction = false) {}
                SellerAvatarItem(name = "Amani", isAction = false) {}
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Financial Amount Input
            Text("Amount (KES)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = transactionAmount,
                onValueChange = { transactionAmount = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00") },
                leadingIcon = { Text("KES", fontWeight = FontWeight.Bold, color = Color(0xFF151C27), modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00236F),
                    unfocusedBorderColor = Color(0xFFC5C5D3)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Item Context Descriptions
            Text("Item Description", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = itemDescription,
                onValueChange = { itemDescription = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text("What are you paying for? (e.g. MacBook Air M2, Space Gray)") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00236F),
                    unfocusedBorderColor = Color(0xFFC5C5D3)
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Timelines Choice Selection Grid Map
            Text("Delivery Timeline", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444651))
            Spacer(modifier = Modifier.height(8.dp))

            val options = listOf("1-3 days", "4-7 days", "7-14 days", "Custom")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimelineGridCard(text = options[0], selected = selectedTimeline == options[0], modifier = Modifier.weight(1f)) { selectedTimeline = options[0] }
                    TimelineGridCard(text = options[1], selected = selectedTimeline == options[1], modifier = Modifier.weight(1f)) { selectedTimeline = options[1] }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimelineGridCard(text = options[2], selected = selectedTimeline == options[2], modifier = Modifier.weight(1f)) { selectedTimeline = options[2] }
                    TimelineGridCard(text = options[3], selected = selectedTimeline == options[3], modifier = Modifier.weight(1f)) { selectedTimeline = options[3] }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 5: Real-time Mathematical Calculation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3FF)),
                border = BorderStroke(1.dp, Color(0xFFC5C5D3).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = Color(0xFF444651), fontSize = 14.sp)
                        Text(String.format("KES %.2f", amountVal), color = Color(0xFF151C27), fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Escrow Fee", color = Color(0xFF444651), fontSize = 14.sp)
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF00236F), modifier = Modifier.size(16.dp))
                        }
                        Text(String.format("KES %.2f", escrowFee), color = Color(0xFF444651), fontSize = 14.sp)
                    }

                    HorizontalDivider(color = Color(0xFFC5C5D3).copy(alpha = 0.4f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total to Pay", color = Color(0xFF00236F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("KES %.2f", totalToPay), color = Color(0xFF00236F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SellerAvatarItem(name: String, isAction: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isAction) Color(0xFF6CF8BB) else Color(0xFFE2E8F8)),
            contentAlignment = Alignment.Center
        ) {
            if (isAction) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF00714D))
            } else {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF444651).copy(alpha = 0.4f))
            }
        }
        Text(name, fontSize = 12.sp, color = Color(0xFF151C27), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TimelineGridCard(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF6CF8BB) else Color(0xFFF9F9FF)
        ),
        border = BorderStroke(1.dp, if (selected) Color(0xFF00714D) else Color(0xFFC5C5D3))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF151C27))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateEscrowPreview() {
    CreateEscrowScreen()
}