package mobile.project.escrowx.seller

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import mobile.project.escrowx.R
import mobile.project.escrowx.dash.BuyerDashboardActivity
import mobile.project.escrowx.seller.SellerDashboardActivity

class RequestAcceptedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: "EX-9284-KNY"

        setContent {
            MaterialTheme {
                RequestAcceptedScreen(transactionId = transactionId)
            }
        }
    }
}

@Composable
fun RequestAcceptedScreen(transactionId: String) {
    val context = LocalContext.current

    fun handleViewActiveEscrows() {
        context.startActivity(Intent(context, SellerDashboardActivity::class.java))
        (context as? RequestAcceptedActivity)?.finish()
    }

    fun handleBackToDashboard() {
        context.startActivity(Intent(context, SellerDashboardActivity::class.java))
        (context as? RequestAcceptedActivity)?.finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FF))
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // Success Checkmark Icon
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Request Accepted!",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00236F)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "We've notified the buyer. The transaction will move to your Active Escrows once payment is confirmed.",
            fontSize = 14.sp,
            color = Color(0xFF444651),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Transaction Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC5C5D3))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE7EEFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Handshake,
                        contentDescription = "Handshake",
                        tint = Color(0xFF00236F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TRANSACTION ID",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF757682),
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF6CF8BB).copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Secure",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF00714D)
                            )
                        }
                    }
                    Text(
                        "#$transactionId",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151C27)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Schedule",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF444651)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Awaiting Buyer Payment",
                            fontSize = 13.sp,
                            color = Color(0xFF444651)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Primary Button - View Active Escrows
        Button(
            onClick = { handleViewActiveEscrows() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00236F),
                contentColor = Color.White
            )
        ) {
            Text("View Active Escrows", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Button - Back to Dashboard
        OutlinedButton(
            onClick = { handleBackToDashboard() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF00236F)
            ),
            border = BorderStroke(1.dp, Color(0xFF00236F).copy(alpha = 0.2f))
        ) {
            Icon(
                Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.weight(0.5f))
    }
}