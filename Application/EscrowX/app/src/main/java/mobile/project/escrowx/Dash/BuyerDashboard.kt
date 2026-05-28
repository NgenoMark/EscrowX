package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mobile.project.escrowx.SettingsActivity

class BuyerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BuyerDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerDashboardScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text("EscrowX Menu", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                    HorizontalDivider()

                    NavigationDrawerItem(label = { Text("Settings") }, selected = false, onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    })
                    NavigationDrawerItem(label = { Text("App Info") }, selected = false, onClick = { })

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider()
                    NavigationDrawerItem(label = { Text("Log Out", color = MaterialTheme.colorScheme.error) }, selected = false, onClick = { /* Logout logic */ })
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("EscrowX", style = MaterialTheme.typography.headlineMedium) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val items = listOf("Home" to Icons.Default.Home, "Transactions" to Icons.Default.AccountBalanceWallet, "Profile" to Icons.Default.Person)
                    items.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                if (index == 1) {
                                    context.startActivity(Intent(context, TransactionsActivity::class.java))
                                } else {
                                    selectedTab = index
                                }
                            },
                            label = { Text(label) },
                            icon = { Icon(icon, label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeTabContent(context)
                    2 -> ProfileTabContent()
                }
            }
        }
    }
}

@Composable
fun HomeTabContent(context: android.content.Context) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Hello, Buyer", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* Create Escrow */ }, modifier = Modifier.fillMaxWidth().height(80.dp)) {
            Text("New Escrow")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionButton("History", Icons.Default.History, Color.Blue) {}
            QuickActionButton("Disputes", Icons.Default.Gavel, Color.Red) {
                context.startActivity(Intent(context, DisputeCenterActivity::class.java))
            }
        }
    }
}

@Composable
fun QuickActionButton(text: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.size(100.dp).clickable { onClick() }) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconColor)
            Text(text)
        }
    }
}

@Composable
fun ProfileTabContent() {
    Column(Modifier.padding(24.dp)) {
        Text("My Profile", style = MaterialTheme.typography.headlineSmall)
        // Add profile details here
    }
}