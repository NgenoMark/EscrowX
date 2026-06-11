package mobile.project.escrowx.auth

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("escrowx_session", Context.MODE_PRIVATE)

    fun saveSession(accessToken: String, refreshToken: String, email: String, userId: String, role: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
    }
}