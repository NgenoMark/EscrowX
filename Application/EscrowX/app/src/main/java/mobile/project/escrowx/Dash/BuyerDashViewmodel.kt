package mobile.project.escrowx.dash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
import mobile.project.escrowx.auth.SessionManager

// Remove any import of DashboardResponse - use the one in this package

class BuyerDashViewmodel : ViewModel() {
    private val _userName = MutableStateFlow("User")
    val userName = _userName.asStateFlow()

    // Use the DashboardResponse from this package (mobile.project.escrowx.dash)
    private val _dashboardData = MutableStateFlow<DashboardResponse?>(null)
    val dashboardData = _dashboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadUserData(email: String?) {
        email?.let {
            _userName.value = it.substringBefore("@")
        }
    }

    fun fetchDashboardData(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.authenticated(token).getDashboardData()
                if (response.isSuccessful) {
                    _dashboardData.value = response.body()
                } else {
                    _error.value = "Failed to load dashboard: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}