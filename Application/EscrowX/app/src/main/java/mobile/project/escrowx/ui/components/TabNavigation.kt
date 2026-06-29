package mobile.project.escrowx.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

fun navigateTab(
    context: Context,
    target: Class<out Activity>,
    extras: Bundle? = null
) {
    val currentActivity = context as? Activity
    if (currentActivity?.javaClass == target && extras == null) {
        return
    }

    val intent = Intent(context, target).apply {
        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        extras?.let { putExtras(it) }
    }

    context.startActivity(intent)
    currentActivity?.overridePendingTransition(0, 0)
    currentActivity?.finish()
    currentActivity?.overridePendingTransition(0, 0)
}
