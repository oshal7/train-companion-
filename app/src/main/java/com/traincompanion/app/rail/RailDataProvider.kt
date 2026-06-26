package com.traincompanion.app.rail

import com.traincompanion.app.data.PnrRecord
import com.traincompanion.app.data.TrainStatus

/**
 * Abstraction over whichever third-party rail-data aggregator backs live status + PNR lookup.
 * IRCTC/Indian Railways has no free official API (PRD section 4) - every implementation here
 * is either the offline [MockRailDataProvider] or a paid aggregator client.
 */
interface RailDataProvider {
    suspend fun liveStatus(trainNumberOrName: String): Result<TrainStatus>
    suspend fun pnrStatus(pnr: String): Result<PnrRecord>
}
