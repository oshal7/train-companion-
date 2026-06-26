package com.traincompanion.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Home", Icons.Filled.Home),
    LiveStatus("live_status", "Status", Icons.Filled.LocationOn),
    Pnr("pnr", "PNR", Icons.Filled.ConfirmationNumber),
    Documents("documents", "Docs", Icons.Filled.Description),
    Alarms("alarms", "Alarms", Icons.Filled.Alarm),
    Emergency("emergency", "SOS", Icons.Filled.Warning),
    Settings("settings", "Settings", Icons.Filled.Settings)
}

val BOTTOM_NAV_ITEMS = listOf(
    Destination.Dashboard,
    Destination.LiveStatus,
    Destination.Pnr,
    Destination.Documents,
    Destination.Alarms,
    Destination.Emergency
)
