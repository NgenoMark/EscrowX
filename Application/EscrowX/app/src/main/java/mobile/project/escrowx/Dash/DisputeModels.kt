package mobile.project.escrowx.dash

enum class DisputeFilter {
    ALL,
    COMPLETE,
    INCOMPLETE
}

enum class DisputeStatus {
    UNDER_INVESTIGATION,
    AWAITING_EVIDENCE,
    RESOLVED
}

data class DisputeItem(
    val txnId: String,
    val title: String,
    val amount: String,
    val status: DisputeStatus,
    val isRefund: Boolean = false
)