package com.kaminova.novalogistikmobile

import com.kaminova.novalogistikmobile.data.api.AuthUser
import com.kaminova.novalogistikmobile.data.api.DropBoardCheckpoint
import com.kaminova.novalogistikmobile.data.api.DropShipment
import com.kaminova.novalogistikmobile.data.api.DropsBoard
import com.kaminova.novalogistikmobile.data.api.TransportDetail
import com.kaminova.novalogistikmobile.data.api.UserSummary
import com.kaminova.novalogistikmobile.data.api.VehicleDetail
import com.kaminova.novalogistikmobile.ui.screens.drops.DropsUiState
import com.kaminova.novalogistikmobile.ui.screens.drops.canUnload
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiDropTest {
    private val driver = AuthUser(1, "sopir", "Sopir", employeeId = 12)
    private val unrelated = AuthUser(2, "operator", "Operator", employeeId = 99)
    private val loaded = DropShipment(1, "RESI-1", dropStatus = "LOADED", effectiveDropCheckpointId = 5)
    private val transport = TransportDetail(
        id = 1,
        transportCode = "TRP-1",
        status = "DEPARTED",
        trackingMode = "ACTIVITY",
        transportMode = "MULTI_DROP",
        vehicle = VehicleDetail(1, "BK 1234 XY"),
        driver = UserSummary(12, "Sopir")
    )

    @Test
    fun permitsAssignedCrewAndAuthorizedOperatorOnlyAfterDeparture() {
        val board = DropsBoard("ACTIVITY", "MULTI_DROP", "DEPARTED")
        val state = DropsUiState(board = board, transport = transport)

        assertTrue(state.canUnload(loaded, driver))
        assertTrue(state.canUnload(loaded, unrelated.copy(permissions = listOf("transport.arrive"))))
        assertFalse(state.canUnload(loaded, unrelated))
        assertFalse(state.copy(board = board.copy(transportStatus = "PLANNED")).canUnload(loaded, driver))
        assertFalse(state.copy(board = board.copy(transportMode = "DIRECT")).canUnload(loaded, driver))
        assertFalse(state.canUnload(loaded.copy(dropStatus = "DROPPED"), driver))
    }

    @Test
    fun checkpointDropRequiresCheckInAtTheAssignedStop() {
        val stop = DropBoardCheckpoint(5, "Tujuan", sequence = 2, checkedIn = true)
        val board = DropsBoard("CHECKPOINT", "MULTI_DROP", "DEPARTED", checkpoints = listOf(stop))
        val state = DropsUiState(board = board, transport = transport)
        val atDropPoint = loaded.copy(dropStatus = "AT_DROP_POINT")

        assertTrue(state.canUnload(atDropPoint, driver))
        assertFalse(state.canUnload(loaded, driver))
        assertFalse(state.canUnload(atDropPoint.copy(effectiveDropCheckpointId = 6), driver))
        assertFalse(state.copy(board = board.copy(checkpoints = listOf(stop.copy(checkedIn = false))))
            .canUnload(atDropPoint, driver))
    }
}
