package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class TransactionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TransactionDashboard()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDashboard() {
    var filter by remember { mutableStateOf("All") }
    // Sample items - In a real app, these would come from a ViewModel
    val items = listOf(
        TransactionItem("1", "iPhone 15 Pro Max", "KES 165,000", "Oct 24, 2023", TransactionStatus.INCOMPLETE),
        TransactionItem("2", "MacBook Air M2", "KES 142,000", "Oct 24, 2023", TransactionStatus.COMPLETE)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Transactions") }) },
        bottomBar = { MyBottomNavigation() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Filters
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Complete", "Incomplete").forEach { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Transaction List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val filtered = if (filter == "All") items else items.filter { it.status.name == filter.uppercase() }
                items(filtered) { item ->
                    TransactionCard(item)
                }
            }
        }
    }
}

@Composable
fun TransactionCard(item: TransactionItem) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon Placeholder
            Surface(Modifier.size(48.dp), shape = MaterialTheme.shapes.medium) { /* Add Image/Icon here */ }

            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(item.amount, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (item.status == TransactionStatus.INCOMPLETE) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) { Text("INCOMPLETE") }
                }
            }

            // Action Buttons
            Column {
                // Dispute Button - Opens RaiseDisputeActivity
                IconButton(onClick = {
                    val intent = Intent(context, RaiseDisputeActivity::class.java)
                    intent.putExtra("TRANSACTION_ID", item.id)
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Gavel, contentDescription = "Dispute", tint = Color.Red)
                }

                // Details Button - Placeholder for Detail activity
                IconButton(onClick = { /* Add navigation to DetailActivity if needed */ }) {
                    Icon(Icons.Default.Visibility, contentDescription = "Details")
                }
            }
        }
    }
}

@Composable
fun MyBottomNavigation() {
    val context = LocalContext.current
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { /* Add Intent to HomeActivity */ },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = true,
            onClick = { /* Already here */ },
            icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
            label = { Text("Transactions") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Add Intent to ProfileActivity */ },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profile") }
        )
    }
}