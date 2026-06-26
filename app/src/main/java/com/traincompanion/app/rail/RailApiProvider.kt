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
import java.util.concurrent.TimeUnit

/**
 * Thin REST client for third-party rail-data aggregators (RailwayAPI.com / IndianRailAPI.com).
 * IRCTC/Indian Railways has no free official API for live status or PNR (PRD section 4) - every
 * commercial app sources this from a paid aggregator. Field names below follow each provider's
 * typical response shape; verify against current docs after signup and adjust [parseLiveStatus] /
 * [parsePnr] if their schema differs.
 */
class RailApiProvider(
    private val providerType: RailProviderType,
    private val apiKey: String
) : RailDataProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String = when (providerType) {
        RailProviderType.RAILWAY_API -> "https://api.railwayapi.com/v2"
        RailProviderType.INDIAN_RAIL_API -> "https://indianrailapi.com/api/v2"
        RailProviderType.MOCK -> ""
    }

    override suspend fun liveStatus(trainNumberOrName: String): Result<TrainStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$baseUrl/live/train/$trainNumberOrName/apikey/$apiKey/"
                parseLiveStatus(JSONObject(execute(url)), trainNumberOrName)
            }.recoverCatching {
                throw IllegalStateException(
                    "Live status request failed for provider $providerType. Check your API " +
                        "key and the provider's current endpoint/response schema.", it
                )
            }
        }

    override suspend fun pnrStatus(pnr: String): Result<PnrRecord> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(pnr.length == 10 && pnr.all { it.isDigit() }) { "PNR must be exactly 10 digits" }
                val url = "$baseUrl/pnr/$pnr/apikey/$apiKey/"
                parsePnr(JSONObject(execute(url)), pnr)
            }.recoverCatching {
                throw IllegalStateException(
                    "PNR request failed for provider $providerType. Check your API key and " +
                        "the provider's current endpoint/response schema.", it
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

    private fun parseLiveStatus(json: JSONObject, query: String): TrainStatus {
        val train = json.optJSONObject("train") ?: json
        val stationsArray = json.optJSONArray("stations")
        val route = mutableListOf<StationStatus>()
        if (stationsArray != null) {
            for (i in 0 until stationsArray.length()) {
                val s = stationsArray.getJSONObject(i)
                route += StationStatus(
                    stationCode = s.optString("station_code", s.optString("code", "")),
                    stationName = s.optString("station_name", s.optString("name", "")),
                    scheduledTime = s.optString("sta", s.optString("scheduled_time", "")),
                    actualTime = s.optString("eta", s.optString("actual_time", "")),
                    platform = s.optString("platform_number", "TBD"),
                    distanceFromOriginKm = s.optInt("distance", 0),
                    hasCrossed = s.optBoolean("has_arrived", false)
                )
            }
        }
        return TrainStatus(
            trainNumber = train.optString("number", query),
            trainName = train.optString("name", query),
            lastUpdatedMillis = System.currentTimeMillis(),
            currentStationName = json.optString("current_station_name", ""),
            delayMinutes = json.optInt("delay", 0),
            nextStationName = json.optString("next_station_name", ""),
            nextStationEta = json.optString("next_station_eta", ""),
            distanceRemainingKm = json.optInt("distance_remaining", 0),
            route = route
        )
    }

    private fun parsePnr(json: JSONObject, pnr: String): PnrRecord {
        val passenger = json.optJSONArray("passengers")?.optJSONObject(0)
        return PnrRecord(
            pnr = pnr,
            status = passenger?.optString("current_status", json.optString("status", "UNKNOWN"))
                ?: json.optString("status", "UNKNOWN"),
            coach = passenger?.optString("coach", "") ?: json.optString("coach", ""),
            berth = passenger?.optString("berth_no", "") ?: json.optString("berth", ""),
            travelClass = json.optString("class", ""),
            trainNumber = json.optString("train_number", ""),
            trainName = json.optString("train_name", ""),
            lastCheckedMillis = System.currentTimeMillis()
        )
    }
}
