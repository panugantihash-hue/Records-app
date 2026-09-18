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
import com.icadd.records.Permissions

@Composable
fun EmployeesScreen(vm: AppViewModel) {
    val items by vm.employees.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
            items(items) { u ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(u.name.ifBlank { u.email }, style = MaterialTheme.typography.titleMedium)
                            Text("${u.email}  •  ${u.role}", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = u.accessStatus == "active", onCheckedChange = {
                            vm.setAccessStatus(u.id, if (it) "active" else "inactive")
                        })
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Add Employee")
        }
    }

    if (showAdd) {
        AddEmployeeDialog(onDismiss = { showAdd = false }, onSave = { email, name, role, perms ->
            vm.addEmployee(email, name, role, perms)
            showAdd = false
        })
    }
}

@Composable
private fun AddEmployeeDialog(onDismiss: () -> Unit, onSave: (String, String, String, Permissions) -> Unit) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var canInward by remember { mutableStateOf(true) }
    var canOutward by remember { mutableStateOf(true) }
    var canAttendance by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(Modifier.padding(20.dp)) {
                Text("Add Employee", style = MaterialTheme.typography.titleLarge)
                Text("They register in the app with this exact email to set their own password.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAdmin, onCheckedChange = { isAdmin = it })
                    Text("Admin (full access)")
                }
                if (!isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canInward, onCheckedChange = { canInward = it })
                        Text("Inward view/create")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canOutward, onCheckedChange = { canOutward = it })
                        Text("Outward view/create")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canAttendance, onCheckedChange = { canAttendance = it })
                        Text("Attendance manage")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (email.isNotBlank()) {
                            val perms = if (isAdmin) Permissions(
                                dashboard_view = true, inward_view = true, inward_create = true, inward_edit = true,
                                outward_view = true, outward_create = true, outward_edit = true,
                                attendance_view = true, attendance_manage = true, storage_view = true, reports_view = true
                            ) else Permissions(
                                dashboard_view = true,
                                inward_view = canInward, inward_create = canInward,
                                outward_view = canOutward, outward_create = canOutward,
                                attendance_view = canAttendance, attendance_manage = canAttendance
                            )
                            onSave(email, name, if (isAdmin) "admin" else "field_officer", perms)
                        }
                    }) { Text("Save") }
                }
            }
        }
    }
}
