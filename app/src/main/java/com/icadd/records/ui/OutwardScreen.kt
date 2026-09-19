package com.icadd.records.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.icadd.records.AppViewModel
import com.icadd.records.FirebaseRepo
import com.icadd.records.InwardRecord
import com.icadd.records.OutwardRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutwardScreen(vm: AppViewModel) {
    val items by vm.outward.collectAsState()
    val inwardItems by vm.inward.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val canCreate = vm.currentUser?.role == "admin" || vm.currentUser?.permissions?.outward_create == true
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportHeaders = listOf("Outward No.", "Date", "Subject", "To", "Linked Inward", "Remarks")
    fun toRows() = items.map { rec ->
        val linked = inwardItems.find { it.id == rec.linkedInwardId }
        listOf(rec.refNo, rec.date, rec.subject, rec.to, linked?.refNo ?: "", rec.remarks)
    }

    val exportXlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )) { uri: Uri? ->
        if (uri != null) scope.launch {
            ExportUtils.writeBytesToUri(context, uri, ExportUtils.buildXlsx(exportHeaders, toRows()))
        }
    }
    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        if (uri != null) scope.launch {
            ExportUtils.writeBytesToUri(context, uri, ExportUtils.buildPdf("Outward Register", exportHeaders, toRows()))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = { exportXlsx.launch("outward_register.xlsx") }) { Text("↓ Excel") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { exportPdf.launch("outward_register.pdf") }) { Text("↓ PDF") }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(items) { rec ->
                    val linkedInward = inwardItems.find { it.id == rec.linkedInwardId }
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(rec.subject, style = MaterialTheme.typography.titleMedium)
                            Text("Ref: ${rec.refNo}  •  To: ${rec.to}", style = MaterialTheme.typography.bodySmall)
                            Text("Date: ${rec.date}", style = MaterialTheme.typography.bodySmall)
                            if (rec.remarks.isNotBlank()) Text(rec.remarks, style = MaterialTheme.typography.bodySmall)
                            if (linkedInward != null) {
                                Text(
                                    "In reply to Inward: ${linkedInward.refNo} — ${linkedInward.subject}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (rec.attachmentUrl.isNotBlank()) {
                                TextButton(onClick = { uriHandler.openUri(rec.attachmentUrl) }) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(rec.attachmentName.ifBlank { "Attachment" })
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
        if (canCreate) {
            FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Outward")
            }
        }
    }

    if (showAdd) {
        AddOutwardDialog(vm = vm, inwardItems = inwardItems, onDismiss = { showAdd = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOutwardDialog(vm: AppViewModel, inwardItems: List<InwardRecord>, onDismiss: () -> Unit) {
    var to by remember { mutableStateOf("") }
    var refNo by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var linkedInward by remember { mutableStateOf<InwardRecord?>(null) }
    var linkMenuExpanded by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf("") }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd").format(Date()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        folders = try { FirebaseRepo.listAllFolders() } catch (_: Exception) { emptyList() }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachmentUri = uri
            attachmentName = queryFileName(context, uri)
        }
    }

    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
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
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(expanded = linkMenuExpanded, onExpandedChange = { linkMenuExpanded = it }) {
                    OutlinedTextField(
                        value = linkedInward?.let { "${it.refNo} — ${it.subject}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link to Inward (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linkMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = linkMenuExpanded, onDismissRequest = { linkMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { linkedInward = null; linkMenuExpanded = false })
                        inwardItems.forEach { inw ->
                            DropdownMenuItem(
                                text = { Text("${inw.refNo} — ${inw.subject}") },
                                onClick = { linkedInward = inw; linkMenuExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = folderMenuExpanded, onExpandedChange = { folderMenuExpanded = it }) {
                    OutlinedTextField(
                        value = selectedFolder.ifBlank { "Storage (root)" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target storage folder") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = folderMenuExpanded, onDismissRequest = { folderMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Storage (root)") }, onClick = { selectedFolder = ""; folderMenuExpanded = false })
                        folders.forEach { f ->
                            DropdownMenuItem(text = { Text(f) }, onClick = { selectedFolder = f; folderMenuExpanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { pickFile.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(attachmentName.ifBlank { "Attach file (optional)" })
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !saving,
                        onClick = {
                            if (subject.isNotBlank() && refNo.isNotBlank()) {
                                scope.launch {
                                    saving = true
                                    var url = ""
                                    var name = ""
                                    val uri = attachmentUri
                                    if (uri != null) {
                                        val bytes = readBytes(context, uri)
                                        if (bytes != null) {
                                            name = attachmentName
                                            url = try { FirebaseRepo.uploadStorageFile(selectedFolder, name, bytes) } catch (_: Exception) { "" }
                                        }
                                    }
                                    vm.addOutward(
                                        OutwardRecord(
                                            date = today, to = to, refNo = refNo, subject = subject, remarks = remarks,
                                            attachmentUrl = url, attachmentName = name,
                                            linkedInwardId = linkedInward?.id ?: "",
                                            enteredBy = vm.currentUser?.email ?: ""
                                        )
                                    )
                                    saving = false
                                    onDismiss()
                                }
                            }
                        }
                    ) { Text(if (saving) "Saving..." else "Save") }
                }
            }
        }
    }
}
