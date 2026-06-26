package com.traincompanion.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.traincompanion.app.data.AppContainer
import com.traincompanion.app.data.DocumentType
import com.traincompanion.app.data.StoredDocument
import com.traincompanion.app.ticket.TicketTextParser
import kotlinx.coroutines.launch

@Composable
fun DocumentsScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var documents by remember { mutableStateOf(listOf<StoredDocument>()) }
    var ticketText by remember { mutableStateOf("") }
    var parsedSummary by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        documents = container.documentsRepository.list()
    }

    val docType = if (tab == 0) DocumentType.TICKET else DocumentType.AADHAAR

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val name = uri.lastPathSegment ?: "document"
            container.documentsRepository.add(docType, name, mimeType, bytes)
            documents = container.documentsRepository.list()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Offline documents", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Stored encrypted on this device only - never uploaded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Ticket") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Aadhaar") })
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { pickerLauncher.launch(arrayOf("application/pdf", "image/*")) }) {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (tab == 0) "Upload ticket" else "Upload Aadhaar")
        }
        Spacer(Modifier.height(16.dp))

        val filtered = documents.filter { it.type == docType }
        LazyColumn(Modifier.weight(1f)) {
            items(filtered) { doc ->
                ListItem(
                    headlineContent = { Text(doc.displayName) },
                    supportingContent = { Text(doc.mimeType) },
                    trailingContent = {
                        IconButton(onClick = {
                            scope.launch {
                                container.documentsRepository.delete(doc)
                                documents = container.documentsRepository.list()
                            }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                )
                Divider()
            }
        }

        if (tab == 0) {
            Spacer(Modifier.height(16.dp))
            Text("Extract ticket details (Stage 1)", style = MaterialTheme.typography.titleSmall)
            Text(
                "Paste the ticket's text (copied from the PDF) to auto-detect PNR, train number, " +
                    "stations and times. Then set an arrival alarm from the Alarms tab.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = ticketText,
                onValueChange = { ticketText = it },
                label = { Text("Paste ticket text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val parsed = TicketTextParser.parse(ticketText)
                parsedSummary = buildString {
                    appendLine("PNR: ${parsed.pnr ?: "not found"}")
                    appendLine("Train: ${parsed.trainNumber ?: "not found"}")
                    appendLine("From: ${parsed.boardingStation ?: "not found"} -> To: ${parsed.destinationStation ?: "not found"}")
                    appendLine("Departure: ${parsed.departureTimeRaw ?: "not found"}")
                    append("Arrival: ${parsed.arrivalTimeRaw ?: "not found"}")
                }
            }) {
                Text("Extract details")
            }
            parsedSummary?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
