package com.icadd.records.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.icadd.records.AppViewModel
import com.icadd.records.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AttendanceScreen(vm: AppViewModel) {
    val items by vm.attendance.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val canManage = vm.currentUser?.role == "admin" || vm.currentUser?.permissions?.attendance_manage == true

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
            items(items) { rec ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(rec.personName, style = MaterialTheme.typography.titleMedium)
                            Text(rec.date, style = MaterialTheme.typography.bodySmall)
                        }
                        AssistChip(onClick = {}, label = { Text(rec.status) })
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (canManage) {
            FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Record Attendance")
            }
        }
    }

    if (showAdd) {
        AddAttendanceDialog(onDismiss = { showAdd = false }, onSave = { rec ->
            vm.addAttendance(rec.copy(recordedBy = vm.currentUser?.email ?: ""))
            showAdd = false
        })
    }
}

@Composable
private fun AddAttendanceDialog(onDismiss: () -> Unit, onSave: (AttendanceRecord) -> Unit) {
    var personName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Present") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd").format(Date()) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(Modifier.padding(20.dp)) {
                Text("Record Attendance", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = personName, onValueChange = { personName = it }, label = { Text("Person name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    FilterChip(selected = status == "Present", onClick = { status = "Present" }, label = { Text("Present") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = status == "Absent", onClick = { status = "Absent" }, label = { Text("Absent") })
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (personName.isNotBlank()) onSave(AttendanceRecord(date = today, personName = personName, status = status))
                    }) { Text("Save") }
                }
            }
        }
    }
}
