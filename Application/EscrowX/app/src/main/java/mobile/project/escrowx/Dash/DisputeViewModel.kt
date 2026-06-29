package mobile.project.escrowx.dash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobile.project.escrowx.auth.SessionManager

data class DisputeUiState(
    val isLoading: Boolean = true,
    val disputesList: List<DisputeItem> = emptyList(),
    val errorMessage: String? = null
)

class DisputeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DisputeUiState())
    val uiState: StateFlow<DisputeUiState> = _uiState.asStateFlow()

    fun fetchUserDisputes(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val token = SessionManager(context).getAccessToken()
            val actorUserId = SessionManager(context).getUserId()
            if (token.isNullOrBlank()) {
                _uiState.value = DisputeUiState(isLoading = false, errorMessage = "Please login again")
                return@launch
            }
            if (actorUserId.isNullOrBlank()) {
                _uiState.value = DisputeUiState(isLoading = false, errorMessage = "Session expired")
                return@launch
            }
            try {
                // Dedicated "my disputes" list endpoint is not available yet in API service.
                // Keep UI data-real by showing an empty state until backend list endpoint is introduced.
                _uiState.value = DisputeUiState(isLoading = false, disputesList = emptyList())
            } catch (e: Exception) {
                _uiState.value = DisputeUiState(isLoading = false, errorMessage = "Network error: ${e.message}")
            }
        }
    }
}