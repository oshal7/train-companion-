package com.traincompanion.app.rail

import com.traincompanion.app.data.PnrRecord
import com.traincompanion.app.data.TrainStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Free, zero-signup PNR lookup via the community-run pnrapi.dfth.in proxy
 * (https://github.com/sanketsaurav/pnrapi). No API key needed, but it's an informal,
 * unmaintained-risk service with no uptime guarantee, served over plain HTTP - a scoped
 * cleartext exception for this domain is required in network_security_config.xml. It only
 * covers PNR status; live train status needs a key-based provider instead.
 */
class CommunityPnrProvider : RailDataProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun liveStatus(trainNumberOrName: String): Result<TrainStatus> =
        Result.failure(
            UnsupportedOperationException(
                "Live train status needs a free IndianRailAPI.com key or a RailwayAPI.com key " +
                    "- pick one in Settings. The free community option only covers PNR status."
            )
        )

    override suspend fun pnrStatus(pnr: String): Result<PnrRecord> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(pnr.length == 10 && pnr.all { it.isDigit() }) { "PNR must be exactly 10 digits" }
                val request = Request.Builder().url("http://pnrapi.dfth.in/pnr/$pnr").get().build()
                val body = client.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    check(response.isSuccessful) { "HTTP ${response.code}: $text" }
                    text
                }
                parseDataWrappedPnr(JSONObject(body), pnr)
            }.recoverCatching {
                throw IllegalStateException(
                    "Free community PNR lookup failed. This is an informal, unmaintained-risk " +
                        "service - if it's down, try again later or switch to a key-based " +
                        "provider in Settings.", it
                )
            }
        }
}
