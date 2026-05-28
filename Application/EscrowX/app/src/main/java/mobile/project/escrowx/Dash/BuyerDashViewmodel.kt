// File: app/src/main/java/mobile/project/escrowx/dash/BuyerDashViewmodel.kt
package mobile.project.escrowx.dash

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mobile.project.escrowx.RetrofitClient

class BuyerDashViewmodel : ViewModel() {
    // FIX: Added EscrowItem inside the generic angle brackets
    val activeEscrows = mutableStateListOf<EscrowItem>()
    val recentTransactions = mutableStateListOf<TransactionItem>()

    fun loadDashboardData(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.authenticated(token).getDashboardData()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    activeEscrows.clear()
                    activeEscrows.addAll(body.escrows)
                    recentTransactions.clear()
                    recentTransactions.addAll(body.transactions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}