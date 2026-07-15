package mobile.project.escrowx.dash

import mobile.project.escrowx.ui.components.deriveRiderProgressState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiderFlowMappingTest {

    @Test
    fun deriveNextRiderAction_returnsNone_whenAssignmentStatusMissing() {
        assertEquals(
            RiderNextAction.NONE,
            deriveNextRiderAction(
                status = "IN_DELIVERY",
                hasAcceptedDelivery = true,
                riderAssignmentStatus = null
            )
        )
    }

    @Test
    fun deriveNextRiderAction_usesAssignmentStatus_progressesSequentially() {
        assertEquals(
            RiderNextAction.START_TRANSIT,
            deriveNextRiderAction(
                status = "IN_DELIVERY",
                hasAcceptedDelivery = true,
                riderAssignmentStatus = "PICKED_UP"
            )
        )

        assertEquals(
            RiderNextAction.ARRIVED,
            deriveNextRiderAction(
                status = "IN_DELIVERY",
                hasAcceptedDelivery = true,
                riderAssignmentStatus = "IN_TRANSIT"
            )
        )

        assertEquals(
            RiderNextAction.DELIVERED,
            deriveNextRiderAction(
                status = "IN_DELIVERY",
                hasAcceptedDelivery = true,
                riderAssignmentStatus = "ARRIVED_AT_BUYER"
            )
        )
    }

    @Test
    fun deriveRiderProgressState_prefersAssignmentStatus_overEscrowGuessing() {
        val state = deriveRiderProgressState(
            statusUpper = "IN_DELIVERY",
            assignmentStatusRaw = "IN_TRANSIT"
        )

        assertEquals(2, state.currentRiderIndex)
        assertEquals("In Transit", state.riderStatusSummary)
    }

    @Test
    fun deriveRiderProgressState_marksComplete_forPostDeliveryEscrowStatus() {
        val state = deriveRiderProgressState(
            statusUpper = "COMPLETED",
            assignmentStatusRaw = "DELIVERED_TO_BUYER"
        )

        assertEquals(4, state.currentRiderIndex)
        assertTrue(state.isRiderFlowComplete)
    }
}
