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

                // Mock data - replace with actual API call
                val liveDatabaseRecords = listOf(
                    DisputeItem(
                        txnId = "TXN: #ESC-99281",
                        title = "Samsung 4K Monitor",
                        amount = "KES 32,500",
                        status = DisputeStatus.UNDER_INVESTIGATION
                    ),
                    DisputeItem(
                        txnId = "TXN: #ESC-87211",
                        title = "Premium Leather Sofa",
                        amount = "KES 120,000",
                        status = DisputeStatus.AWAITING_EVIDENCE
                    ),
                    DisputeItem(
                        txnId = "TXN: #ESC-77610",
                        title = "Solar Inverter Kit",
                        amount = "KES 15,000",
                        status = DisputeStatus.RESOLVED,
                        isRefund = true
                    )
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
                    errorMessage = "Failed to load disputes: ${e.localizedMessage}"
                )
            }
        }
    }
}