package com.traincompanion.app.data

enum class DocumentType { TICKET, AADHAAR }

data class StoredDocument(
    val id: String,
    val type: DocumentType,
    val displayName: String,
    val fileName: String,
    val mimeType: String,
    val addedAtMillis: Long
)

data class EmergencyContact(
    val id: String,
    val name: String,
    val phone: String
)

data class ScheduledAlarm(
    val id: String,
    val label: String,
    val triggerAtMillis: Long,
    val bufferMinutes: Int = 20,
    val sourceTrainNumber: String? = null,
    val enabled: Boolean = true
)

data class PnrRecord(
    val pnr: String,
    val status: String,
    val coach: String,
    val berth: String,
    val travelClass: String,
    val trainNumber: String,
    val trainName: String,
    val lastCheckedMillis: Long
)

data class StationStatus(
    val stationCode: String,
    val stationName: String,
    val scheduledTime: String,
    val actualTime: String,
    val platform: String,
    val distanceFromOriginKm: Int,
    val hasCrossed: Boolean
)

data class TrainStatus(
    val trainNumber: String,
    val trainName: String,
    val lastUpdatedMillis: Long,
    val currentStationName: String,
    val delayMinutes: Int,
    val nextStationName: String,
    val nextStationEta: String,
    val distanceRemainingKm: Int,
    val route: List<StationStatus>
)

data class ParsedTicketInfo(
    val pnr: String? = null,
    val trainNumber: String? = null,
    val trainName: String? = null,
    val boardingStation: String? = null,
    val destinationStation: String? = null,
    val departureTimeRaw: String? = null,
    val arrivalTimeRaw: String? = null,
    val travelDateRaw: String? = null
)

enum class RailProviderType { MOCK, RAILWAY_API, INDIAN_RAIL_API, COMMUNITY_PNR }

data class AppSettings(
    val providerType: RailProviderType = RailProviderType.MOCK,
    val apiKey: String = ""
)
