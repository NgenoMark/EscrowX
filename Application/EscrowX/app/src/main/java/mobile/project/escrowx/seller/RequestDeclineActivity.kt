package mobile.project.escrowx.seller
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobile.project.escrowx.R

class RequestDeclinedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buyerName = intent.getStringExtra("BUYER_NAME") ?: "the buyer"

        setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {
                RequestDeclinedScreen(buyerName = buyerName)
            }
        }
    }
}

@Composable
fun RequestDeclinedScreen(buyerName: String) {
    val context = LocalContext.current

    fun handleBackToDashboard() {
        context.startActivity(Intent(context, SellerDashboardActivity::class.java))
        (context as? RequestDeclinedActivity)?.finish()
    }

    fun handleViewOtherRequests() {
        context.startActivity(Intent(context, IncomingEscrowsActivity::class.java))
        (context as? RequestDeclinedActivity)?.finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FF))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFDAD6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Declined",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFBA1A1A)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Request Declined",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF151C27)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You've declined the request from $buyerName. The buyer has been notified and no funds have been moved.",
            fontSize = 14.sp,
            color = Color(0xFF444651),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(0.5f))

        Button(
            onClick = { handleBackToDashboard() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00236F),
                contentColor = Color.White
            )
        ) {
            Text("Back to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { handleViewOtherRequests() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF00236F)
            ),
            border = BorderStroke(1.dp, Color(0xFF00236F))
        ) {
            Text("View Other Requests", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}