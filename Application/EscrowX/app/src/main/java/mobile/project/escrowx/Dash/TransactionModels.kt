package mobile.project.escrowx.dash

enum class TransactionStatus {
    COMPLETE,
    INCOMPLETE
}

data class TransactionItem(
    val id: String,
    val title: String,
    val amount: String,
    val date: String,
    val status: TransactionStatus,
    val iconRes: String = "shopping_bag"
)