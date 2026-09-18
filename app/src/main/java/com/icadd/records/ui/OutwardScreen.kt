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
import com.icadd.records.OutwardRecord
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun OutwardScreen(vm: AppViewModel) {
    val items by vm.outward.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val canCreate = vm.currentUser?.role == "admin" || vm.currentUser?.permissions?.outward_create == true

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
            items(items) { rec ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(rec.subject, style = MaterialTheme.typography.titleMedium)
                        Text("Ref: ${rec.refNo}  •  To: ${rec.to}", style = MaterialTheme.typography.bodySmall)
                        Text("Date: ${rec.date}", style = MaterialTheme.typography.bodySmall)
                        if (rec.remarks.isNotBlank()) Text(rec.remarks, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (canCreate) {
            FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Outward")
            }
        }
    }

    if (showAdd) {
        AddOutwardDialog(onDismiss = { showAdd = false }, onSave = { rec ->
            vm.addOutward(rec.copy(enteredBy = vm.currentUser?.email ?: ""))
            showAdd = false
        })
    }
}

@Composable
private fun AddOutwardDialog(onDismiss: () -> Unit, onSave: (OutwardRecord) -> Unit) {
    var to by remember { mutableStateOf("") }
    var refNo by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd").format(Date()) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(Modifier.padding(20.dp)) {
                Text("New Outward Entry", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text("To") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = refNo, onValueChange = { refNo = it }, label = { Text("Outward / Ref No.") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (subject.isNotBlank() && refNo.isNotBlank()) {
                            onSave(OutwardRecord(date = today, to = to, refNo = refNo, subject = subject, remarks = remarks))
                        }
                    }) { Text("Save") }
                }
            }
        }
    }
}
