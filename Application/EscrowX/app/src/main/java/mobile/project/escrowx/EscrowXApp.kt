package mobile.project.escrowx

import android.app.Application
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

class EscrowXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreferenceManager.applySavedTheme(this)
    }
}
