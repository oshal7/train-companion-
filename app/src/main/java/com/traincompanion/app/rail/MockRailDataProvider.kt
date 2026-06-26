package com.traincompanion.app.rail

import com.traincompanion.app.data.PnrRecord
import com.traincompanion.app.data.StationStatus
import com.traincompanion.app.data.TrainStatus
import kotlinx.coroutines.delay

/**
 * Deterministic offline data source used until a real provider API key is configured in
 * Settings. Lets every screen be built/tested end-to-end without a paid subscription.
 */
class MockRailDataProvider : RailDataProvider {

    override suspend fun liveStatus(trainNumberOrName: String): Result<TrainStatus> {
        delay(400)
        val seed = trainNumberOrName.trim().ifBlank { "12345" }
        val hash = seed.sumOf { it.code }.let { if (it == 0) 1 else it }
        val delayMinutes = hash % 45

        val stations = SAMPLE_STATIONS.mapIndexed { index, name ->
            val crossed = index < SAMPLE_STATIONS.size / 2
            StationStatus(
                stationCode = name.take(3).uppercase(),
                stationName = name,
                scheduledTime = "%02d:%02d".format((6 + index * 2) % 24, (index * 13) % 60),
                actualTime = "%02d:%02d".format((6 + index * 2) % 24, ((index * 13) + delayMinutes) % 60),
                platform = if (crossed) "${(index % 5) + 1}" else "TBD",
                distanceFromOriginKm = index * 145,
                hasCrossed = crossed
            )
        }
        val currentIndex = (stations.size / 2 - 1).coerceAtLeast(0)
        val nextIndex = (currentIndex + 1).coerceAtMost(stations.size - 1)

        val status = TrainStatus(
            trainNumber = seed.filter { it.isDigit() }.ifBlank { "12345" },
            trainName = if (seed.any { it.isLetter() }) seed else "Sample Express",
            lastUpdatedMillis = System.currentTimeMillis(),
            currentStationName = stations[currentIndex].stationName,
            delayMinutes = delayMinutes,
            nextStationName = stations[nextIndex].stationName,
            nextStationEta = stations[nextIndex].actualTime,
            distanceRemainingKm = stations.last().distanceFromOriginKm - stations[currentIndex].distanceFromOriginKm,
            route = stations
        )
        return Result.success(status)
    }

    override suspend fun pnrStatus(pnr: String): Result<PnrRecord> {
        delay(400)
        if (pnr.length != 10 || !pnr.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PNR must be exactly 10 digits"))
        }
        val hash = pnr.sumOf { it.code }
        val statusCodes = listOf("CNF", "RAC", "WL")
        val baseStatus = statusCodes[hash % statusCodes.size]
        val status = if (baseStatus == "CNF") baseStatus else "$baseStatus/${(hash % 20) + 1}"

        val record = PnrRecord(
            pnr = pnr,
            status = status,
            coach = "S${(hash % 12) + 1}",
            berth = "${(hash % 72) + 1}",
            travelClass = listOf("SL", "3A", "2A", "1A")[hash % 4],
            trainNumber = "${10000 + (hash % 9000)}",
            trainName = "Sample Express",
            lastCheckedMillis = System.currentTimeMillis()
        )
        return Result.success(record)
    }

    companion object {
        private val SAMPLE_STATIONS = listOf(
            "Origin Junction", "Riverside", "Hilltop", "Central City", "Lakeside",
            "Greenfield", "Old Town", "Destination Terminus"
        )
    }
}
