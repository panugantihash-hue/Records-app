package com.icadd.records.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.icadd.records.FirebaseRepo
import kotlinx.coroutines.launch

@Composable
fun StorageScreen() {
    var currentPath by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<FirebaseRepo.StorageEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showNewFolder by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FirebaseRepo.StorageEntry>?>(null) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            entries = try { FirebaseRepo.listStorage(currentPath) } catch (_: Exception) { emptyList() }
            loading = false
        }
    }
    LaunchedEffect(currentPath) { refresh() }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val name = queryFileName(context, uri)
                val bytes = readBytes(context, uri)
                if (bytes != null) {
                    loading = true
                    try { FirebaseRepo.uploadStorageFile(currentPath, name, bytes) } catch (_: Exception) { }
                    refresh()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Central Storage", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search files (all folders)") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    TextButton(onClick = {
                        scope.launch {
                            loading = true
                            searchResults = try { FirebaseRepo.searchStorageAll(searchQuery) } catch (_: Exception) { emptyList() }
                            loading = false
                        }
                    }) { Text("Go") }
                }
            }
        )
        Spacer(Modifier.height(8.dp))

        Row {
            OutlinedButton(onClick = { showNewFolder = true }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New folder")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { pickFile.launch("*/*") }) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Upload")
            }
        }
        Spacer(Modifier.height(8.dp))

        // Breadcrumb
        if (searchResults == null) {
            Row {
                TextButton(onClick = { currentPath = "" }) { Text("Storage (root)") }
                currentPath.split("/").filter { it.isNotBlank() }.forEachIndexed { idx, part ->
                    Text("/", modifier = Modifier.padding(top = 12.dp))
                    val pathUpTo = currentPath.split("/").filter { it.isNotBlank() }.take(idx + 1).joinToString("/")
                    TextButton(onClick = { currentPath = pathUpTo }) { Text(part) }
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        val listToShow = searchResults ?: entries
        if (searchResults != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Search results for \"$searchQuery\"", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { searchResults = null; searchQuery = "" }) { Text("Clear") }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(listToShow) { entry ->
                ListItem(
                    headlineContent = { Text(entry.name) },
                    supportingContent = if (searchResults != null) { { Text(entry.path) } } else null,
                    leadingContent = {
                        Icon(if (entry.isFolder) Icons.Default.Folder else Icons.Default.InsertDriveFile, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        if (entry.isFolder && searchResults == null) {
                            currentPath = entry.path
                        } else if (!entry.isFolder) {
                            scope.launch {
                                val url = try { FirebaseRepo.getDownloadUrl(entry.path) } catch (_: Exception) { "" }
                                if (url.isNotBlank()) uriHandler.openUri(url)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showNewFolder) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("Folder name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (folderName.isNotBlank()) {
                        scope.launch {
                            loading = true
                            try { FirebaseRepo.createStorageFolder(currentPath, folderName) } catch (_: Exception) { }
                            showNewFolder = false
                            refresh()
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text("Cancel") } }
        )
    }
}
