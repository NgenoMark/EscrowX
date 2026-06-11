package mobile.project.escrowx.seller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import mobile.project.escrowx.dash.ProfileActivity
import mobile.project.escrowx.dash.TransactionsActivity

class LinkGeneratedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentLink = intent.getStringExtra("PAYMENT_LINK") ?: "escrowx.com/pay/default"

        setContent {
            MaterialTheme {
                LinkGeneratedScreen(paymentLink = paymentLink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkGeneratedScreen(paymentLink: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    fun copyLink() {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Payment Link", paymentLink)
        clipboard.setPrimaryClip(clip)
        isCopied = true
        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()

        scope.launch {
            delay(2000)
            isCopied = false
        }
    }

    fun shareViaWhatsApp() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Secure payment link: $paymentLink\n\nPowered by EscrowX")
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareViaEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "Secure Payment Link")
            putExtra(Intent.EXTRA_TEXT, "Please complete your payment using this secure link: $paymentLink\n\nRegards,\nEscrowX")
        }
        context.startActivity(intent)
    }

    fun goToMyLinks() {
        Toast.makeText(context, "My Links coming soon", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Link Generated",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00236F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? LinkGeneratedActivity)?.finish()
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
                    titleContentColor = Color(0xFF00236F)
                )
            )
        },
        bottomBar = {
            LinkGeneratedBottomNavigation()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9FF))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Illustration
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6CF8BB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF00714D)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00236F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Payment Link Ready!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF151C27)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Share this link with your buyer to receive secure escrow payments.",
                fontSize = 14.sp,
                color = Color(0xFF444651),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Link Display Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6CF8BB))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFFFF),
                        border = BorderStroke(1.dp, Color(0xFFC5C5D3))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                paymentLink,
                                fontSize = 14.sp,
                                color = Color(0xFF00236F),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { copyLink() },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF00236F)
                                )
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isCopied) "Copied!" else "Copy Link", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "SHARE VIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF757682),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // WhatsApp Button
                ShareOptionButton(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconBgColor = Color(0xFF25D366).copy(alpha = 0.1f),
                    iconTint = Color(0xFF25D366),
                    label = "WhatsApp",
                    onClick = { shareViaWhatsApp() }
                )

                // Email Button
                ShareOptionButton(
                    icon = Icons.Default.Email,
                    iconBgColor = Color(0xFF00236F).copy(alpha = 0.1f),
                    iconTint = Color(0xFF00236F),
                    label = "Email",
                    onClick = { shareViaEmail() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secondary Action - Go to My Links
            OutlinedButton(
                onClick = { goToMyLinks() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF00236F)
                ),
                border = BorderStroke(1.dp, Color(0xFF00236F))
            ) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go to My Links", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ShareOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF151C27)
        )
    }
}

@Composable
fun LinkGeneratedBottomNavigation() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) }

    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = Color(0xFFF9F9FF),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = {
                selectedTab = 0
                context.startActivity(Intent(context, SellerDashboardActivity::class.java))
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
            icon = {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = "Transactions",
                    modifier = Modifier.size(24.dp)
                )
            },
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