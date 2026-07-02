package mobile.project.escrowx.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.RegisterDeviceTokenRequest

object FcmTokenRegistrar {

    private const val TAG = "FcmTokenRegistrar"

    fun register(context: Context, userId: String?) {
        if (userId.isNullOrBlank()) return

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Failed to fetch FCM token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result?.trim().orEmpty()
                if (token.isBlank()) {
                    return@addOnCompleteListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.instance.registerDeviceToken(
                            actorUserId = userId,
                            request = RegisterDeviceTokenRequest(token = token)
                        )
                    } catch (ex: Exception) {
                        Log.w(TAG, "Failed to register FCM token with backend", ex)
                    }
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "FCM unavailable (check google-services.json / Firebase config)", ex)
        }
    }

    fun registerByEmail(context: Context, email: String?) {
        if (email.isNullOrBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getUserByEmail(email.trim())
                if (!response.isSuccessful) {
                    return@launch
                }

                val userId = response.body()?.id
                register(context, userId)
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to resolve user by email for FCM registration", ex)
            }
        }
    }
}
