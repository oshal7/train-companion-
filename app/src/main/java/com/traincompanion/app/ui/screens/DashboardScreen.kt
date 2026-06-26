package com.traincompanion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.traincompanion.app.data.AppContainer
import com.traincompanion.app.ui.Destination

private data class QuickLink(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

private val QUICK_LINKS = listOf(
    QuickLink("Live status", "Track your train in real time", Icons.Filled.LocationOn, Destination.LiveStatus.route),
    QuickLink("PNR status", "Confirmed / RAC / waitlisted", Icons.Filled.ConfirmationNumber, Destination.Pnr.route),
    QuickLink("Documents", "Ticket & Aadhaar, offline & encrypted", Icons.Filled.Description, Destination.Documents.route),
    QuickLink("Alarms", "Auto-alarm from your ticket", Icons.Filled.Alarm, Destination.Alarms.route),
    QuickLink("Emergency", "Helplines & one-tap location share", Icons.Filled.Warning, Destination.Emergency.route),
    QuickLink("Settings", "Rail-data provider & API key", Icons.Filled.Settings, Destination.Settings.route)
)

@Composable
fun DashboardScreen(container: AppContainer, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Train Companion", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "One app for live status, PNR, offline documents and smart alarms.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(QUICK_LINKS) { link ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    onClick = { onNavigate(link.route) }
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(link.icon, contentDescription = link.title, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(link.title, style = MaterialTheme.typography.titleMedium)
                            Text(link.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
