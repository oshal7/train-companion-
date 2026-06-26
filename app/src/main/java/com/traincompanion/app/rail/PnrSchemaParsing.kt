package com.traincompanion.app.rail

import com.traincompanion.app.data.PnrRecord
import org.json.JSONObject

/**
 * Shared parsing for the "data/status" wrapped PNR JSON schema used by both RailwayAPI.com
 * and the free community pnrapi.dfth.in proxy, whose response mirrors RailwayAPI's shape:
 * {"data": {"train_number", "train_name", "class", "passenger": [{"status"}]}, "status": "OK"}
 */
internal fun parseDataWrappedPnr(json: JSONObject, pnr: String): PnrRecord {
    val data = json.optJSONObject("data") ?: json
    val passenger = data.optJSONArray("passenger")?.optJSONObject(0)
    val statusRaw = passenger?.optString("status", "")?.takeIf { it.isNotBlank() }
        ?: data.optString("status", "UNKNOWN")
    val (coach, berth) = parseCoachBerth(statusRaw)
    return PnrRecord(
        pnr = pnr,
        status = statusRaw,
        coach = coach,
        berth = berth,
        travelClass = data.optString("class", ""),
        trainNumber = data.optString("train_number", ""),
        trainName = data.optString("train_name", ""),
        lastCheckedMillis = System.currentTimeMillis()
    )
}

/**
 * Coach/berth are only split out of a "CNF/S6/71/GN"-style status string when confirmed -
 * waitlist statuses like "W/L 54,RLGN" have a slash inside the abbreviation itself and would
 * be corrupted by a naive split. The raw string is always kept in [PnrRecord.status] either way.
 */
internal fun parseCoachBerth(statusRaw: String): Pair<String, String> {
    if (!statusRaw.startsWith("CNF", ignoreCase = true)) return "" to ""
    val parts = statusRaw.split("/")
    return (parts.getOrNull(1) ?: "") to (parts.getOrNull(2) ?: "")
}
