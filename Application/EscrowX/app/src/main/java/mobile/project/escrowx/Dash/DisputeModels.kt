package mobile.project.escrowx.dash

enum class DisputeFilter {
    ALL,
    COMPLETE,   // resolved disputes
    INCOMPLETE  // open or under investigation
}

enum class DisputeStatus {
    OPEN,
    UNDER_INVESTIGATION,
    RESOLVED
}

data class DisputeItem(
    val id: String,
    val transactionId: String,
    val raisedById: String,
    val category: String,       // e.g., "NON_DELIVERY", "ITEM_NOT_AS_DESCRIBED"
    val status: DisputeStatus,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)