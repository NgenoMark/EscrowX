package mobile.project.escrowx.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferenceManager {
    private const val PREFS_NAME = "escrowx_theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun isDarkModeEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
        applyThemeMode(enabled)
    }

    fun applySavedTheme(context: Context) {
        applyThemeMode(isDarkModeEnabled(context))
    }

    private fun applyThemeMode(darkModeEnabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
