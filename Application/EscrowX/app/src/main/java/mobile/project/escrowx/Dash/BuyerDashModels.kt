package mobile.project.escrowx.dash

data class DashboardResponse(
    val escrows: List<EscrowItem>,
    val transactions: List<TransactionItem>
)

data class EscrowItem(
    val id: Int,
    val status: String,
    val amount: String,
    val partyName: String,
    val timeLeft: String
)

data class TransactionItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val amount: String,
    val date: String
)