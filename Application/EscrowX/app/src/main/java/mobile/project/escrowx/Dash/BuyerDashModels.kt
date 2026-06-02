package mobile.project.escrowx.dash

// Add this enum to handle the filter logic in your TransactionDashboard
enum class TransactionStatus {
    COMPLETE, INCOMPLETE
}

data class DashboardResponse(
    val escrows: List<EscrowItem>,
    val transactions: List<TransactionItem>
)

data class EscrowItem(
    val id: Int,
    val status: String, // Keep as String if coming from API, or map to Enum
    val amount: String,
    val partyName: String,
    val timeLeft: String
)

data class TransactionItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val amount: String,
    val date: String,
    val status: TransactionStatus // Added to match your TransactionDashboard requirement
)