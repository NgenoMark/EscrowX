package mobile.project.escrowx.ui.components

import mobile.project.escrowx.DeliveryAssignmentHistoryItemResponse
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.material3.Surface
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
import java.time.OffsetDateTime
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
    assignmentHistory: List<DeliveryAssignmentHistoryItemResponse> = emptyList(),
    currentActiveAssignmentId: String? = null,
    riderNameMap: Map<String, String> = emptyMap(),
    riderPhoneMap: Map<String, String> = emptyMap(),
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
    val activeAssignment = remember(assignmentHistory, currentActiveAssignmentId) {
        resolveCurrentAssignment(assignmentHistory, currentActiveAssignmentId)
    }
    val previousAssignments = remember(assignmentHistory, activeAssignment) {
        assignmentHistory.filter { it.id != activeAssignment?.id }
    }
    val statusFailure = remember(activeAssignment?.status) {
        activeAssignment?.status?.trim()?.uppercase(Locale.ROOT) in setOf("FAILED", "CANCELLED")
    }

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

            RiderAssignmentPeopleSection(
                currentAssignment = activeAssignment,
                previousAssignments = previousAssignments,
                riderNameMap = riderNameMap,
                riderPhoneMap = riderPhoneMap,
                colorScheme = colorScheme
            )

            if (activeAssignment != null && !isExpanded) {
                AssignmentEventChips(
                    events = deriveAssignmentEvents(activeAssignment),
                    colorScheme = colorScheme,
                    accentColor = accentColor
                )
            }

            if (statusFailure) {
                RetryPathHint(
                    statusLabel = activeAssignment?.status.orEmpty(),
                    colorScheme = colorScheme
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Detailed Progress",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )
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

@Composable
private fun RiderAssignmentPeopleSection(
    currentAssignment: DeliveryAssignmentHistoryItemResponse?,
    previousAssignments: List<DeliveryAssignmentHistoryItemResponse>,
    riderNameMap: Map<String, String>,
    riderPhoneMap: Map<String, String>,
    colorScheme: ColorScheme
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Current Rider",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant
        )
        val currentRiderId = currentAssignment?.riderUserId
        Text(
            text = if (currentRiderId.isNullOrBlank()) "Not assigned" else riderNameMap[currentRiderId] ?: currentRiderId.take(8),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
        Text(
            text = if (currentRiderId.isNullOrBlank()) "-" else riderPhoneMap[currentRiderId] ?: "-",
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant
        )

        Text(
            text = "Previous Riders",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (previousAssignments.isEmpty()) {
            Text(
                text = "No previous riders",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        } else {
            previousAssignments.take(3).forEach { item ->
                val previousId = item.riderUserId
                val name = riderNameMap[previousId] ?: previousId.take(8)
                val status = prettifyAssignmentStatus(item.status)
                Text(
                    text = "- $name ($status)",
                    fontSize = 12.sp,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AssignmentEventChips(
    events: List<String>,
    colorScheme: ColorScheme,
    accentColor: Color
) {
    if (events.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Assignment Events",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(events) { event ->
                val style = eventChipStyle(event, accentColor)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = style.background,
                    border = BorderStroke(1.dp, style.border)
                ) {
                    Text(
                        text = event,
                        fontSize = 11.sp,
                        color = style.text,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun RetryPathHint(
    statusLabel: String,
    colorScheme: ColorScheme
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFDC2626).copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = "Assignment ${prettifyAssignmentStatus(statusLabel)}",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Awaiting retry/reassignment by admin.",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun resolveCurrentAssignment(
    assignments: List<DeliveryAssignmentHistoryItemResponse>,
    currentActiveAssignmentId: String?
): DeliveryAssignmentHistoryItemResponse? {
    if (assignments.isEmpty()) return null
    currentActiveAssignmentId?.let { activeId ->
        assignments.firstOrNull { it.id == activeId }?.let { return it }
    }
    return assignments.maxByOrNull { item ->
        runCatching { OffsetDateTime.parse(item.updatedAt).toInstant().toEpochMilli() }
            .getOrDefault(0L)
    }
}

private fun deriveAssignmentEvents(item: DeliveryAssignmentHistoryItemResponse): List<String> {
    val status = item.status.trim().uppercase(Locale.ROOT)
    val events = linkedSetOf<String>()
    events.add("Assigned")
    if (!item.previousRiderUserId.isNullOrBlank() || !item.reassignmentReason.isNullOrBlank()) {
        events.add("Reassigned")
    }
    when (status) {
        "ACCEPTED" -> events.add("Accepted")
        "PICKED_UP" -> {
            events.add("Accepted")
            events.add("Picked Up")
        }
        "IN_TRANSIT" -> {
            events.add("Accepted")
            events.add("Picked Up")
            events.add("In Transit")
        }
        "ARRIVED_AT_BUYER" -> {
            events.add("Accepted")
            events.add("Picked Up")
            events.add("In Transit")
            events.add("Arrived")
        }
        "DELIVERED_TO_BUYER" -> {
            events.add("Accepted")
            events.add("Picked Up")
            events.add("In Transit")
            events.add("Delivered")
        }
        "SELLER_CONFIRMED_DELIVERED" -> {
            events.add("Accepted")
            events.add("Delivered")
            events.add("Seller Confirmed")
        }
        "BUYER_CONFIRMED_DELIVERED" -> {
            events.add("Accepted")
            events.add("Delivered")
            events.add("Buyer Confirmed")
        }
        "FAILED" -> events.add("Failed")
        "CANCELLED" -> events.add("Cancelled")
    }
    return events.toList()
}

private fun prettifyAssignmentStatus(raw: String?): String {
    if (raw.isNullOrBlank()) return "Unknown"
    return raw.trim()
        .lowercase(Locale.ROOT)
        .split('_')
        .joinToString(" ") { token -> token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
}

private data class EventChipStyle(
    val background: Color,
    val border: Color,
    val text: Color
)

private fun eventChipStyle(event: String, accentColor: Color): EventChipStyle {
    return when (event.trim().uppercase(Locale.ROOT)) {
        "FAILED", "CANCELLED" -> EventChipStyle(
            background = Color(0xFFDC2626).copy(alpha = 0.12f),
            border = Color(0xFFDC2626).copy(alpha = 0.30f),
            text = Color(0xFFB91C1C)
        )
        "REASSIGNED" -> EventChipStyle(
            background = Color(0xFFF59E0B).copy(alpha = 0.14f),
            border = Color(0xFFF59E0B).copy(alpha = 0.32f),
            text = Color(0xFFB45309)
        )
        "ASSIGNED", "ACCEPTED", "PICKED UP", "IN TRANSIT", "ARRIVED", "DELIVERED", "SELLER CONFIRMED", "BUYER CONFIRMED" -> EventChipStyle(
            background = Color(0xFF10B981).copy(alpha = 0.12f),
            border = Color(0xFF10B981).copy(alpha = 0.28f),
            text = Color(0xFF047857)
        )
        else -> EventChipStyle(
            background = accentColor.copy(alpha = 0.12f),
            border = accentColor.copy(alpha = 0.25f),
            text = accentColor
        )
    }
}
