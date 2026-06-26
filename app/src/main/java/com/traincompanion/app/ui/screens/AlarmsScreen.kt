package com.traincompanion.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.traincompanion.app.data.ScheduledAlarm
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
fun AlarmsScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var alarms by remember { mutableStateOf(listOf<ScheduledAlarm>()) }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { alarms = container.alarmsRepository.list() }

    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Arrival alarms", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Set an alarm before your train reaches your destination - works fully offline.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))

        if (!container.alarmScheduler.canScheduleExactAlarms()) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }) {
                Text("Allow exact alarms in Settings")
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(onClick = { showCreate = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add alarm")
        }
        Spacer(Modifier.height(12.dp))

        if (alarms.isEmpty()) {
            Text("No alarms scheduled yet.")
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(alarms.sortedBy { it.triggerAtMillis }) { alarm ->
                    val formatted = SimpleDateFormat("EEE, d MMM - h:mm a", Locale.getDefault())
                        .format(alarm.triggerAtMillis)
                    ListItem(
                        headlineContent = { Text(alarm.label) },
                        supportingContent = { Text(formatted) },
                        trailingContent = {
                            IconButton(onClick = {
                                scope.launch {
                                    container.alarmScheduler.cancel(alarm)
                                    container.alarmsRepository.delete(alarm.id)
                                    alarms = container.alarmsRepository.list()
                                }
                            }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                        }
                    )
                    Divider()
                }
            }
        }
    }

    if (showCreate) {
        CreateAlarmDialog(
            onDismiss = { showCreate = false },
            onConfirm = { label, triggerMillis, buffer, trainNumber ->
                val alarm = ScheduledAlarm(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    triggerAtMillis = triggerMillis,
                    bufferMinutes = buffer,
                    sourceTrainNumber = trainNumber.ifBlank { null }
                )
                scope.launch {
                    container.alarmsRepository.upsert(alarm)
                    container.alarmScheduler.schedule(alarm)
                    alarms = container.alarmsRepository.list()
                }
                showCreate = false
            }
        )
    }
}

@Composable
private fun CreateAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, triggerMillis: Long, bufferMinutes: Int, trainNumber: String) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("Destination arrival") }
    var trainNumber by remember { mutableStateOf("") }
    var bufferMinutes by remember { mutableStateOf(20) }
    val calendar = remember { Calendar.getInstance() }
    var pickedMillis by remember { mutableStateOf<Long?>(null) }
    var pickedLabel by remember { mutableStateOf("Pick arrival date & time") }

    fun pickDateTime() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute, 0)
                        pickedMillis = calendar.timeInMillis
                        pickedLabel = SimpleDateFormat("EEE, d MMM - h:mm a", Locale.getDefault())
                            .format(calendar.time)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New arrival alarm") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = trainNumber,
                    onValueChange = { trainNumber = it },
                    label = { Text("Train number (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { pickDateTime() }, modifier = Modifier.fillMaxWidth()) {
                    Text(pickedLabel)
                }
                Spacer(Modifier.height(8.dp))
                Text("Alert $bufferMinutes min before scheduled arrival")
                Slider(
                    value = bufferMinutes.toFloat(),
                    onValueChange = { bufferMinutes = it.toInt() },
                    valueRange = 5f..60f,
                    steps = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pickedMillis != null && label.isNotBlank(),
                onClick = {
                    val trigger = (pickedMillis ?: return@TextButton) - bufferMinutes * 60_000L
                    onConfirm(label, trigger, bufferMinutes, trainNumber)
                }
            ) { Text("Set alarm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
