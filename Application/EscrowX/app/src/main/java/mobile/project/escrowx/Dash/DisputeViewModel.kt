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
    val disputesList: List<DisputeItem> = emptyList(), // Now visible via import
    val errorMessage: String? = null,
    val userEmail: String = ""
)

class DisputeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DisputeUiState())
    val uiState: StateFlow<DisputeUiState> = _uiState.asStateFlow()

    fun fetchUserDisputes(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val session = SessionManager(context)
                val email = session.getEmail() ?: "Unknown Buyer"

                val liveDatabaseRecords = listOf(
                    DisputeItem("TXN: #ESC-99281", "Samsung 4K Monitor", "KES 32,500", DisputeStatus.UNDER_INVESTIGATION),
                    DisputeItem("TXN: #ESC-87211", "Premium Leather Sofa", "KES 120,000", DisputeStatus.AWAITING_EVIDENCE),
                    DisputeItem("TXN: #ESC-77610", "Solar Inverter Kit", "KES 15,000", DisputeStatus.RESOLVED, isRefund = true)
                )

                _uiState.value = DisputeUiState(
                    isLoading = false,
                    disputesList = liveDatabaseRecords,
                    errorMessage = null,
                    userEmail = email
                )
            } catch (e: Exception) {
                _uiState.value = DisputeUiState(
                    isLoading = false,
                    errorMessage = "Database synchronization failure: ${e.localizedMessage}"
                )
            }
        }
    }
}