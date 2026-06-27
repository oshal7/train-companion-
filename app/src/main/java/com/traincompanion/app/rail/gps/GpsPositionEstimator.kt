package com.traincompanion.app.rail.gps

import com.traincompanion.app.data.StationStatus
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Matches the device's own GPS fix against [StationCoordinates.KNOWN] to pinpoint progress
 * along a train's route - no live-status API call involved, works fully offline once the
 * route (station order) is already known from any provider including [MockRailDataProvider].
 */
object GpsPositionEstimator {
    private const val EARTH_RADIUS_KM = 6371.0

    data class GpsEstimate(
        val nearestStationName: String,
        val nextStationName: String?,
        val distanceToNextKm: Double?,
        val matchedStationCount: Int,
        val totalStationCount: Int
    )

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    private fun lookup(stationName: String): StationCoordinate? {
        val normalized = stationName.trim().lowercase()
            .removeSuffix(" junction").removeSuffix(" jn.").removeSuffix(" jn")
            .removeSuffix(" terminus").removeSuffix(" central").trim()
        return StationCoordinates.KNOWN.firstOrNull { it.name.lowercase().contains(normalized) }
            ?: StationCoordinates.KNOWN.firstOrNull { normalized.contains(it.name.lowercase()) }
    }

    /** Returns null only when none of the route's stations are in the bundled coordinate set. */
    fun estimate(route: List<StationStatus>, deviceLat: Double, deviceLon: Double): GpsEstimate? {
        val resolved = route.mapIndexedNotNull { index, station ->
            lookup(station.stationName)?.let { coord -> Triple(index, station, coord) }
        }
        if (resolved.isEmpty()) return null

        val nearest = resolved.minByOrNull { (_, _, coord) ->
            distanceKm(deviceLat, deviceLon, coord.lat, coord.lon)
        } ?: return null

        val nearestPositionInResolved = resolved.indexOf(nearest)
        val next = resolved.getOrNull(nearestPositionInResolved + 1)
        val distanceToNext = next?.let { (_, _, coord) ->
            distanceKm(deviceLat, deviceLon, coord.lat, coord.lon)
        }

        return GpsEstimate(
            nearestStationName = nearest.second.stationName,
            nextStationName = next?.second?.stationName,
            distanceToNextKm = distanceToNext,
            matchedStationCount = resolved.size,
            totalStationCount = route.size
        )
    }
}
