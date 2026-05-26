package mobile.project.escrowx.auth

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("escrowx_session", Context.MODE_PRIVATE)

    fun saveSession(accessToken: String, refreshToken: String, email: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EMAIL = "email"
    }
}
