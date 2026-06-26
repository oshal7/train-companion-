package com.traincompanion.app.ticket

import com.traincompanion.app.data.ParsedTicketInfo

/**
 * Stage-1 ticket parser (PRD 3.5 / 4.4): regex extraction over plain text copied/extracted from
 * an IRCTC ticket PDF/printout. Deliberately avoids any OCR/cloud call so PNR/Aadhaar-adjacent
 * data never leaves the device for v1. Whatever a field misses, the user fills in manually on
 * the Documents screen.
 */
object TicketTextParser {

    private val pnrRegex = Regex("""PNR\s*(?:No\.?|Number)?\s*[:\-]?\s*(\d{10})""", RegexOption.IGNORE_CASE)
    private val trainNoRegex = Regex("""Train\s*(?:No\.?|Number)?\s*[:\-]?\s*(\d{5})""", RegexOption.IGNORE_CASE)
    private val timeRegex = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\s*(AM|PM|hrs)?\b""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex(
        """\b(\d{1,2}[\-/][A-Za-z]{3}[\-/]\d{2,4}|\d{1,2}[\-/]\d{1,2}[\-/]\d{2,4})\b"""
    )
    private val stationPairRegex = Regex(
        """From\s*[:\-]?\s*([A-Za-z .]{3,30})\s*(?:To|-|–)\s*([A-Za-z .]{3,30})""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String): ParsedTicketInfo {
        val pnr = pnrRegex.find(text)?.groupValues?.get(1)
        val trainNumber = trainNoRegex.find(text)?.groupValues?.get(1)
        val times = timeRegex.findAll(text).map { it.value }.toList()
        val dates = dateRegex.findAll(text).map { it.value }.toList()
        val stationPair = stationPairRegex.find(text)

        return ParsedTicketInfo(
            pnr = pnr,
            trainNumber = trainNumber,
            trainName = null,
            boardingStation = stationPair?.groupValues?.get(1)?.trim(),
            destinationStation = stationPair?.groupValues?.get(2)?.trim(),
            departureTimeRaw = times.getOrNull(0),
            arrivalTimeRaw = times.getOrNull(1) ?: times.getOrNull(0),
            travelDateRaw = dates.getOrNull(0)
        )
    }
}
