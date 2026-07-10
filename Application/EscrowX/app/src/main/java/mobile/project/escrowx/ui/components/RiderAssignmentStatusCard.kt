package mobile.project.escrowx.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal data class RiderProgressState(
    val currentRiderIndex: Int,
    val isRiderFlowComplete: Boolean,
    val riderStatusSummary: String
)

internal fun deriveRiderProgressState(statusUpper: String, assignmentStatusRaw: String?): RiderProgressState {
    val riderFlow = listOf(
        "Awaiting Assignment",
        "Assigned",
        "In Transit",
        "Delivered to Buyer",
        "Delivery Confirmed"
    )

    val assignmentStatus = assignmentStatusRaw?.trim()?.uppercase(Locale.ROOT)

    val currentRiderIndexFromAssignment = when (assignmentStatus) {
        "ASSIGNED", "ACCEPTED" -> 1
        "PICKED_UP", "IN_TRANSIT" -> 2
        "ARRIVED_AT_BUYER", "DELIVERED_TO_BUYER" -> 3
        else -> -1
    }

    val currentRiderIndexFromEscrow = when (statusUpper) {
        "SELLER_ACCEPTED" -> 1
        "IN_DELIVERY" -> 2
        "SELLER_DELIVERED" -> 3
        "BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING", "RELEASE_PROCESSING", "RELEASE_FAILED", "COMPLETED" -> 4
        else -> 0
    }

    val currentRiderIndex = when {
        statusUpper in setOf("BUYER_CONFIRMED_DELIVERED", "RELEASE_PENDING", "RELEASE_PROCESSING", "RELEASE_FAILED", "COMPLETED") -> 4
        currentRiderIndexFromAssignment >= 0 -> currentRiderIndexFromAssignment
        else -> currentRiderIndexFromEscrow
    }

    val isRiderFlowComplete = currentRiderIndex >= 4

    val riderStatusSummary = when (statusUpper) {
        "DISPUTED", "REFUND_PENDING", "REFUND_PROCESSING", "REFUNDED" -> "Delivery in Dispute/Refund"
        "CANCELLED", "DECLINED", "EXPIRED" -> "Transaction Closed"
        else -> riderFlow[currentRiderIndex.coerceIn(0, riderFlow.lastIndex)]
    }

    return RiderProgressState(
        currentRiderIndex = currentRiderIndex,
        isRiderFlowComplete = isRiderFlowComplete,
        riderStatusSummary = riderStatusSummary
    )
}

@Composable
fun RiderAssignmentStatusCard(
    statusUpper: String,
    riderName: String,
    riderPhone: String,
    riderAssignmentStatus: String?,
    colorScheme: ColorScheme,
    accentColor: Color = colorScheme.primary
) {
    var isExpanded by remember { mutableStateOf(false) }

    val riderFlow = listOf(
        "Awaiting Assignment",
        "Assigned",
        "In Transit",
        "Delivered to Buyer",
        "Delivery Confirmed"
    )

    val progressState = deriveRiderProgressState(statusUpper, riderAssignmentStatus)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Column {
                        Text(
                            "Rider Assignment",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            progressState.riderStatusSummary,
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = riderName,
                        fontSize = 12.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = riderPhone,
                        fontSize = 12.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    riderFlow.forEachIndexed { index, step ->
                        val isMet = if (progressState.isRiderFlowComplete) {
                            index <= progressState.currentRiderIndex
                        } else {
                            index < progressState.currentRiderIndex
                        }
                        val isCurrent = !progressState.isRiderFlowComplete && index == progressState.currentRiderIndex

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        when {
                                            isMet -> Color(0xFF10B981)
                                            else -> colorScheme.primary.copy(alpha = 0.12f)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isMet) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                } else {
                                    Icon(
                                        if (isCurrent) Icons.Default.Sync else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colorScheme.primary
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step,
                                    fontSize = if (isCurrent) 13.sp else 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        isMet -> Color(0xFF10B981)
                                        isCurrent -> colorScheme.onSurface
                                        else -> colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = when {
                                        isMet -> "Done"
                                        isCurrent -> "In progress"
                                        else -> "Awaiting"
                                    },
                                    fontSize = 10.sp,
                                    color = if (isMet) Color(0xFF10B981) else colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
