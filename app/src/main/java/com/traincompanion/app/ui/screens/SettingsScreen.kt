package com.traincompanion.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.traincompanion.app.data.AppContainer
import com.traincompanion.app.data.RailProviderType

@Composable
fun SettingsScreen(container: AppContainer) {
    var settings by remember { mutableStateOf(container.securePrefs.getSettings()) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rail-data provider", style = MaterialTheme.typography.headlineSmall)
        Text(
            "IRCTC/Indian Railways has no free official API. IndianRailAPI.com offers a free " +
                "100-requests/day tier (signup required) for live status + PNR; the free " +
                "community PNR lookup needs no signup but only covers PNR; RailwayAPI.com is " +
                "a paid option. Or keep using sample data.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        RailProviderType.values().forEach { provider ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.providerType == provider,
                    onClick = { settings = settings.copy(providerType = provider) }
                )
                Text(providerLabel(provider))
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = settings.apiKey,
            onValueChange = { settings = settings.copy(apiKey = it) },
            label = { Text("API key") },
            enabled = settings.providerType == RailProviderType.RAILWAY_API ||
                settings.providerType == RailProviderType.INDIAN_RAIL_API,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            container.securePrefs.saveSettings(settings)
            saved = true
        }) {
            Text("Save")
        }
        if (saved) {
            Spacer(Modifier.height(8.dp))
            Text("Saved. New requests will use this provider.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun providerLabel(provider: RailProviderType): String = when (provider) {
    RailProviderType.MOCK -> "Sample data (no key needed)"
    RailProviderType.RAILWAY_API -> "RailwayAPI.com (paid, key required)"
    RailProviderType.INDIAN_RAIL_API -> "IndianRailAPI.com (free tier, key required)"
    RailProviderType.COMMUNITY_PNR -> "Free community PNR lookup (no key, PNR only)"
}
