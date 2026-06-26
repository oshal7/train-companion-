package com.traincompanion.app.rail

import com.traincompanion.app.data.PnrRecord
import com.traincompanion.app.data.RailProviderType
import com.traincompanion.app.data.StationStatus
import com.traincompanion.app.data.TrainStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * REST client for the two key-based rail-data aggregators: IndianRailAPI.com (genuinely free
 * 100 requests/day "Starter" tier, signup required) and RailwayAPI.com (paid). IRCTC/Indian
 * Railways itself has no free official API for live status or PNR (PRD section 4). Endpoint
 * paths and field names below follow each provider's documented response shape as of this
 * build; verify against current docs if a provider changes its schema.
 */
class RailApiProvider(
    private val providerType: RailProviderType,
    private val apiKey: String
) : RailDataProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun liveStatus(trainNumberOrName: String): Result<TrainStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trainNumber = trainNumberOrName.trim()
                when (providerType) {
                    RailProviderType.INDIAN_RAIL_API -> {
                        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                        val url = "https://indianrailapi.com/api/v2/livetrainstatus/apikey/" +
                            "$apiKey/trainnumber/$trainNumber/date/$date/"
                        parseIndianRailLiveStatus(JSONObject(execute(url)), trainNumber)
                    }
                    RailProviderType.RAILWAY_API -> {
                        val date = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
                        val url = "https://api.railwayapi.com/v2/live/train/$trainNumber/date/" +
                            "$date/apikey/$apiKey/"
                        parseRailwayApiLiveStatus(JSONObject(execute(url)), trainNumber)
                    }
                    else -> error("Live status is not supported by provider $providerType")
                }
            }.recoverCatching {
                throw IllegalStateException(
                    "Live status request failed for provider $providerType. Check your API " +
                        "key, your network connection, and that the train number is correct.", it
                )
            }
        }

    override suspend fun pnrStatus(pnr: String): Result<PnrRecord> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(pnr.length == 10 && pnr.all { it.isDigit() }) { "PNR must be exactly 10 digits" }
                when (providerType) {
                    RailProviderType.INDIAN_RAIL_API -> {
                        val url = "https://indianrailapi.com/api/v2/PNRCheck/apikey/$apiKey/" +
                            "PNRNumber/$pnr/Route/1/"
                        parseIndianRailPnr(JSONObject(execute(url)), pnr)
                    }
                    RailProviderType.RAILWAY_API -> {
                        val url = "https://api.railwayapi.com/v2/pnr/$pnr/apikey/$apiKey/"
                        parseDataWrappedPnr(JSONObject(execute(url)), pnr)
                    }
                    else -> error("PNR status is not supported by provider $providerType")
                }
            }.recoverCatching {
                throw IllegalStateException(
                    "PNR request failed for provider $providerType. Check your API key, your " +
                        "network connection, and that the PNR number is correct.", it
                )
            }
        }

    private fun execute(url: String): String {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            check(response.isSuccessful) { "HTTP ${response.code}: $body" }
            return body
        }
    }

    private fun parseIndianRailLiveStatus(json: JSONObject, query: String): TrainStatus {
        val routeArray = json.optJSONArray("TrainRoute")
        val rawRoute = mutableListOf<StationStatus>()
        if (routeArray != null) {
            for (i in 0 until routeArray.length()) {
                val s = routeArray.getJSONObject(i)
                rawRoute += StationStatus(
                    stationCode = s.optString("StationCode", ""),
                    stationName = s.optString("StationName", ""),
                    scheduledTime = s.optString("ScheduleArrival", s.optString("ScheduleDeparture", "")),
                    actualTime = s.optString("ActualArrival", s.optString("ActualDeparture", "")),
                    platform = s.optString("Platform", "TBD"),
                    distanceFromOriginKm = s.optInt("Distance", s.optInt("KM", 0)),
                    hasCrossed = false
                )
            }
        }
        val currentStationName = json.optJSONObject("CurrentStation")?.optString("StationName")
            ?.takeIf { it.isNotBlank() }
            ?: json.optString("CurrentStationName", "")
        val currentIndex = rawRoute.indexOfFirst { it.stationName.equals(currentStationName, ignoreCase = true) }
        val route = rawRoute.mapIndexed { index, station ->
            if (currentIndex >= 0) station.copy(hasCrossed = index < currentIndex) else station
        }
        val next = if (currentIndex >= 0) route.getOrNull(currentIndex + 1) else null
        val delayRaw = routeArray?.optJSONObject(currentIndex.coerceAtLeast(0))
            ?.let { it.optString("DelayInArrival", it.optString("DelayInDeparture", "")) }
            ?: ""
        val lastStationDistance = route.lastOrNull()?.distanceFromOriginKm ?: 0
        val currentDistance = if (currentIndex >= 0) route[currentIndex].distanceFromOriginKm else lastStationDistance
        return TrainStatus(
            trainNumber = json.optString("TrainNumber", query),
            trainName = json.optString("TrainName", query),
            lastUpdatedMillis = System.currentTimeMillis(),
            currentStationName = currentStationName,
            delayMinutes = Regex("\\d+").find(delayRaw)?.value?.toIntOrNull() ?: 0,
            nextStationName = next?.stationName ?: "",
            nextStationEta = next?.scheduledTime ?: "",
            distanceRemainingKm = (lastStationDistance - currentDistance).coerceAtLeast(0),
            route = route
        )
    }

    private fun parseIndianRailPnr(json: JSONObject, pnr: String): PnrRecord {
        val passenger = json.optJSONArray("Passangers")?.optJSONObject(0)
            ?: json.optJSONArray("Passengers")?.optJSONObject(0)
        val statusRaw = passenger?.optString("CurrentStatus", "")?.takeIf { it.isNotBlank() }
            ?: json.optString("Status", "UNKNOWN")
        val (coach, berth) = parseCoachBerth(statusRaw)
        return PnrRecord(
            pnr = pnr,
            status = statusRaw,
            coach = coach,
            berth = berth,
            travelClass = json.optString("JourneyClass", ""),
            trainNumber = json.optString("TrainNumber", ""),
            trainName = json.optString("TrainName", ""),
            lastCheckedMillis = System.currentTimeMillis()
        )
    }

    private fun parseRailwayApiLiveStatus(json: JSONObject, query: String): TrainStatus {
        val train = json.optJSONObject("train")
        val routeArray = json.optJSONArray("route")
        val rawRoute = mutableListOf<StationStatus>()
        if (routeArray != null) {
            for (i in 0 until routeArray.length()) {
                val s = routeArray.getJSONObject(i)
                val station = s.optJSONObject("station")
                rawRoute += StationStatus(
                    stationCode = station?.optString("code", "") ?: "",
                    stationName = station?.optString("name", "") ?: "",
                    scheduledTime = s.optString("scharr", s.optString("schdep", "")),
                    actualTime = s.optString("actarr", s.optString("actdep", "")),
                    platform = s.optString("platform", "TBD"),
                    distanceFromOriginKm = s.optInt("distance", 0),
                    hasCrossed = false
                )
            }
        }
        val currentStationName = json.optJSONObject("current_station")?.optString("name", "") ?: ""
        val currentIndex = rawRoute.indexOfFirst { it.stationName.equals(currentStationName, ignoreCase = true) }
        val route = rawRoute.mapIndexed { index, station ->
            if (currentIndex >= 0) station.copy(hasCrossed = index < currentIndex) else station
        }
        val next = if (currentIndex >= 0) route.getOrNull(currentIndex + 1) else null
        val positionRaw = json.optString("position", "")
        val lastStationDistance = route.lastOrNull()?.distanceFromOriginKm ?: 0
        val currentDistance = if (currentIndex >= 0) route[currentIndex].distanceFromOriginKm else lastStationDistance
        return TrainStatus(
            trainNumber = train?.optString("number", query) ?: query,
            trainName = train?.optString("name", query) ?: query,
            lastUpdatedMillis = System.currentTimeMillis(),
            currentStationName = currentStationName,
            delayMinutes = Regex("\\d+").find(positionRaw)?.value?.toIntOrNull() ?: 0,
            nextStationName = next?.stationName ?: "",
            nextStationEta = next?.scheduledTime ?: "",
            distanceRemainingKm = (lastStationDistance - currentDistance).coerceAtLeast(0),
            route = route
        )
    }
}
