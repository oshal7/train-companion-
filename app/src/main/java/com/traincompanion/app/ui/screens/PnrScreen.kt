package com.traincompanion.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.traincompanion.app.data.AppContainer
import com.traincompanion.app.data.PnrRecord
import kotlinx.coroutines.launch

@Composable
fun PnrScreen(container: AppContainer) {
    var pnr by remember { mutableStateOf("") }
    var record by remember { mutableStateOf<PnrRecord?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun check() {
        error = null
        if (pnr.length != 10 || !pnr.all { it.isDigit() }) {
            error = "Enter a valid 10-digit PNR"
            return
        }
        loading = true
        scope.launch {
            container.currentRailProvider().pnrStatus(pnr)
                .onSuccess {
                    record = it
                    container.pnrRepository.save(it)
                }
                .onFailure { error = it.message ?: "Could not fetch PNR status" }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("PNR status", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pnr,
            onValueChange = { if (it.length <= 10) pnr = it.filter(Char::isDigit) },
            label = { Text("10-digit PNR") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                IconButton(onClick = { check() }) { Icon(Icons.Filled.Search, contentDescription = "Check") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
            record != null -> PnrCard(record!!)
            else -> Text(
                "Enter the PNR from your ticket to check booking status, coach, berth and class. " +
                    "Sample data is shown until a real provider key is set in Settings."
            )
        }
    }
}

@Composable
private fun PnrCard(record: PnrRecord) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("${record.trainNumber} - ${record.trainName}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Status: ${record.status}", fontWeight = FontWeight.Bold)
            Text("Coach ${record.coach}, Berth ${record.berth}, Class ${record.travelClass}")
        }
    }
}
