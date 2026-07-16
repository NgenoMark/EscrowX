package mobile.project.escrowx.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mobile.project.escrowx.R
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.auth.SplashActivity
import mobile.project.escrowx.dash.BuyerTransactionDetailActivity
import mobile.project.escrowx.dash.RiderAssignmentDetailsActivity
import mobile.project.escrowx.seller.SellerTransactionDetailActivity
import java.util.Locale

class EscrowFirebaseMessagingService : FirebaseMessagingService() {

    private fun isPushEnabled(): Boolean {
        val prefs = getSharedPreferences("escrowx_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("push_notifications", true)
    }

    override fun onNewToken(token: String) {
        if (!isPushEnabled()) return
        val userId = SessionManager(this).getUserId()
        FcmTokenRegistrar.register(this, userId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (!isPushEnabled()) return

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "EscrowX"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new update"

        ensureChannel()
        showNotification(title, body, message.data)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "General Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "EscrowX updates"

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return
            }
        }

        val openIntent = buildDeepLinkIntent(data)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun buildDeepLinkIntent(data: Map<String, String>): Intent {
        val transactionId = data["transactionId"]?.takeIf { it.isNotBlank() }
        if (transactionId.isNullOrBlank()) {
            return Intent(this, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val role = SessionManager(this).getUserRole()?.uppercase(Locale.getDefault()) ?: "BUYER"
        val status = data["status"]?.ifBlank { null } ?: "FUNDS_HELD"

        return when (role) {
            "SELLER" -> Intent(this, SellerTransactionDetailActivity::class.java).apply {
                putExtra("TRANSACTION_ID", transactionId)
                putExtra("STATUS", status)
                putExtra("CURRENT_STEP", sellerStepForStatus(status))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            "RIDER" -> Intent(this, RiderAssignmentDetailsActivity::class.java).apply {
                putExtra(RiderAssignmentDetailsActivity.EXTRA_TRANSACTION_ID, transactionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            else -> Intent(this, BuyerTransactionDetailActivity::class.java).apply {
                putExtra("TRANSACTION_ID", transactionId)
                putExtra("STATUS", status)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }

    private fun sellerStepForStatus(statusRaw: String): Int {
        return when (statusRaw.trim().uppercase(Locale.getDefault())) {
            "CREATED", "PENDING_PAYMENT", "FUNDS_HELD" -> 1
            "SELLER_ACCEPTED", "IN_DELIVERY", "SELLER_DELIVERED" -> 2
            "BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING", "RELEASED", "COMPLETED" -> 3
            "DECLINED", "CANCELLED", "DISPUTED", "RELEASE_FAILED" -> 3
            else -> 1
        }
    }

    companion object {
        private const val CHANNEL_ID = "escrowx_general_notifications"
    }
}
