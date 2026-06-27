package com.traincompanion.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traincompanion.app.data.AppContainer
import com.traincompanion.app.data.TrainStatus
import com.traincompanion.app.location.LocationHelper
import com.traincompanion.app.rail.gps.GpsPositionEstimator
import kotlinx.coroutines.launch

@Composable
fun LiveStatusScreen(container: AppContainer) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<TrainStatus?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var gpsEstimate by remember { mutableStateOf<GpsPositionEstimator.GpsEstimate?>(null) }
    var gpsLoading by remember { mutableStateOf(false) }
    var gpsMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search() {
        if (query.isBlank()) return
        loading = true
        error = null
        gpsEstimate = null
        gpsMessage = null
        scope.launch {
            container.currentRailProvider().liveStatus(query.trim())
                .onSuccess { status = it }
                .onFailure { error = it.message ?: "Could not fetch live status" }
            loading = false
        }
    }

    fun trackWithGps() {
        val route = status?.route ?: return
        gpsLoading = true
        gpsMessage = null
        scope.launch {
            val latLng = LocationHelper.getCurrentLatLng(context)
            if (latLng == null) {
                gpsMessage = "Could not get a GPS fix - make sure location is enabled"
            } else {
                val (lat, lon) = latLng
                gpsEstimate = GpsPositionEstimator.estimate(route, lat, lon)
                if (gpsEstimate == null) {
                    gpsMessage = "None of this route's stations are in the bundled GPS " +
                        "database (major junctions only) - can't match your position"
                }
            }
            gpsLoading = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) trackWithGps() else gpsMessage = "Location permission denied"
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Live running status", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Train number or name") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { search() }) { Icon(Icons.Filled.Search, contentDescription = "Search") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
                status != null -> TrainStatusCard(
                    status = status!!,
                    gpsEstimate = gpsEstimate,
                    gpsLoading = gpsLoading,
                    gpsMessage = gpsMessage,
                    onTrackWithGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            trackWithGps()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                )
                else -> Text(
                    "Search any train by number or name to see its live running status. " +
                        "Sample data is shown until a real rail-data provider is set up in Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TrainStatusCard(
    status: TrainStatus,
    gpsEstimate: GpsPositionEstimator.GpsEstimate?,
    gpsLoading: Boolean,
    gpsMessage: String?,
    onTrackWithGps: () -> Unit
) {
    val minutesAgo = ((System.currentTimeMillis() - status.lastUpdatedMillis) / 60000).coerceAtLeast(0)
    Column(Modifier.fillMaxSize()) {
        Text("${status.trainNumber} - ${status.trainName}", style = MaterialTheme.typography.titleLarge)
        Text("Last updated $minutesAgo min ago", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Current station", style = MaterialTheme.typography.labelMedium)
                Text(status.currentStationName, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Delay", style = MaterialTheme.typography.labelMedium)
                Text(
                    if (status.delayMinutes <= 0) "On time" else "${status.delayMinutes} min late",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Next station", style = MaterialTheme.typography.labelMedium)
                Text("${status.nextStationName} - ETA ${status.nextStationEta}")
            }
            Column {
                Text("Distance left", style = MaterialTheme.typography.labelMedium)
                Text("${status.distanceRemainingKm} km")
            }
        }
        Spacer(Modifier.height(12.dp))
        GpsTrackingSection(gpsEstimate, gpsLoading, gpsMessage, onTrackWithGps)
        Spacer(Modifier.height(16.dp))
        Text("Route", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(status.route) { station ->
                ListItem(
                    headlineContent = { Text(station.stationName) },
                    supportingContent = {
                        Text("Sched ${station.scheduledTime} - Actual ${station.actualTime} - Platform ${station.platform}")
                    },
                    leadingContent = {
                        Icon(
                            if (station.hasCrossed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun GpsTrackingSection(
    gpsEstimate: GpsPositionEstimator.GpsEstimate?,
    gpsLoading: Boolean,
    gpsMessage: String?,
    onTrackWithGps: () -> Unit
) {
    Column {
        Button(onClick = onTrackWithGps, enabled = !gpsLoading) {
            Icon(Icons.Filled.GpsFixed, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (gpsLoading) "Getting GPS fix..." else "Track with GPS (offline, no internet)")
        }
        gpsEstimate?.let { estimate ->
            Spacer(Modifier.height(8.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Nearest matched station: ${estimate.nearestStationName}", fontWeight = FontWeight.Bold)
                    if (estimate.nextStationName != null && estimate.distanceToNextKm != null) {
                        Text("~${"%.0f".format(estimate.distanceToNextKm)} km to ${estimate.nextStationName}")
                    }
                    Text(
                        "${estimate.matchedStationCount}/${estimate.totalStationCount} route stations " +
                            "matched against the on-device GPS database",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        gpsMessage?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
