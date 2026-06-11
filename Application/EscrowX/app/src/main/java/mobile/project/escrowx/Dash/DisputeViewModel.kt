package mobile.project.escrowx.dash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient
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
            if (token.isNullOrBlank()) {
                _uiState.value = DisputeUiState(isLoading = false, errorMessage = "Please login again")
                return@launch
            }
            try {
                // TODO: Replace with actual endpoint when available (e.g., GET /api/v1/disputes/me)
                // For now, use mock data that matches the new DisputeItem structure
                val mockDisputes = listOf(
                    DisputeItem(
                        id = "d1",
                        transactionId = "ESC-99281",
                        raisedById = "buyer1",
                        category = "ITEM_NOT_AS_DESCRIBED",
                        status = DisputeStatus.UNDER_INVESTIGATION,
                        description = "The monitor arrived with a dead pixel.",
                        createdAt = "2024-06-01T10:00:00Z"
                    ),
                    DisputeItem(
                        id = "d2",
                        transactionId = "ESC-87211",
                        raisedById = "seller1",  // seller raised dispute
                        category = "DAMAGED_ITEM",
                        status = DisputeStatus.OPEN,
                        description = "Sofa has a tear on the side.",
                        createdAt = "2024-06-05T09:15:00Z"
                    ),
                    DisputeItem(
                        id = "d3",
                        transactionId = "ESC-77610",
                        raisedById = "buyer2",
                        category = "WRONG_ITEM",
                        status = DisputeStatus.RESOLVED,
                        description = "Received a different model.",
                        createdAt = "2024-05-28T12:00:00Z"
                    )
                )
                _uiState.value = DisputeUiState(isLoading = false, disputesList = mockDisputes)
            } catch (e: Exception) {
                _uiState.value = DisputeUiState(isLoading = false, errorMessage = "Network error: ${e.message}")
            }
        }
    }
}