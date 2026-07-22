package mobile.project.escrowx.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

object ThemePreferenceManager {
    private const val PREFS_NAME = "escrowx_theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        prefs(context)
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

    @Composable
    fun rememberDarkModeEnabledState(): Boolean {
        val context = LocalContext.current
        val appContext = context.applicationContext
        val lifecycleOwner = LocalLifecycleOwner.current
        val sharedPreferences = remember(appContext) { prefs(appContext) }

        var darkModeEnabled by remember {
            mutableStateOf(sharedPreferences.getBoolean(KEY_DARK_MODE, false))
        }

        DisposableEffect(sharedPreferences) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
                if (key == KEY_DARK_MODE) {
                    darkModeEnabled = preferences.getBoolean(KEY_DARK_MODE, false)
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        DisposableEffect(lifecycleOwner, appContext) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    darkModeEnabled = isDarkModeEnabled(appContext)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(darkModeEnabled) {
            applyThemeMode(darkModeEnabled)
        }

        return darkModeEnabled
    }
}
