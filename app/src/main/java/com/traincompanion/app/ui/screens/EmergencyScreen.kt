package com.traincompanion.app.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.traincompanion.app.data.EmergencyContact
import com.traincompanion.app.location.LocationHelper
import kotlinx.coroutines.launch

private data class Helpline(val name: String, val number: String)

private val HELPLINES = listOf(
    Helpline("Railway Enquiry & Security Helpline", "139"),
    Helpline("RPF Security Helpline", "182"),
    Helpline("Women Helpline", "1091")
)

@Composable
fun EmergencyScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf(listOf<EmergencyContact>()) }
    var showAdd by remember { mutableStateOf(false) }
    var shareStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { contacts = container.contactsRepository.list() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { shareStatus = shareLocation(context) }
        } else {
            shareStatus = "Location permission denied"
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Emergency", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Helplines are bundled in-app and work without internet.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
        }
        items(HELPLINES) { helpline ->
            ListItem(
                headlineContent = { Text(helpline.name) },
                supportingContent = { Text(helpline.number) },
                trailingContent = {
                    IconButton(onClick = { context.startActivity(LocationHelper.buildDialIntent(helpline.number)) }) {
                        Icon(Icons.Filled.Call, contentDescription = "Call ${helpline.name}")
                    }
                }
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (LocationHelper.hasLocationPermission(context)) {
                    scope.launch { shareStatus = shareLocation(context) }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share my live location")
            }
            shareStatus?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Personal emergency contacts", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "Add contact") }
            }
        }
        items(contacts) { contact ->
            ListItem(
                headlineContent = { Text(contact.name) },
                supportingContent = { Text(contact.phone) },
                trailingContent = {
                    Row {
                        IconButton(onClick = { context.startActivity(LocationHelper.buildDialIntent(contact.phone)) }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call ${contact.name}")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                container.contactsRepository.delete(contact.id)
                                contacts = container.contactsRepository.list()
                            }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                }
            )
        }
    }

    if (showAdd) {
        AddContactDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, phone ->
                scope.launch {
                    container.contactsRepository.add(name, phone)
                    contacts = container.contactsRepository.list()
                }
                showAdd = false
            }
        )
    }
}

private suspend fun shareLocation(context: Context): String {
    val link = LocationHelper.getCurrentMapsLink(context) ?: return "Could not get current location"
    context.startActivity(LocationHelper.buildShareIntent(link, "Here is my current location:"))
    return "Opened share sheet with your current location"
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add emergency contact") },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number") })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && phone.isNotBlank(),
                onClick = { onConfirm(name.trim(), phone.trim()) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
