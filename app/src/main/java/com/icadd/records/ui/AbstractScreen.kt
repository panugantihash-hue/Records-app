package com.icadd.records.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icadd.records.AppViewModel
import com.icadd.records.InwardRecord
import com.icadd.records.OutwardRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbstractScreen(vm: AppViewModel) {
    val inward by vm.inward.collectAsState()
    val outward by vm.outward.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Inward", "Outward", "Combined")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }
        when (tab) {
            0 -> InwardAbstract(inward)
            1 -> OutwardAbstract(outward, inward)
            2 -> CombinedAbstract(inward, outward)
        }
    }
}

@Composable
private fun InwardAbstract(inward: List<InwardRecord>) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Inward abstract", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(inward) { rec ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${rec.date}  •  No. ${rec.refNo}", style = MaterialTheme.typography.labelMedium)
                        Text(rec.subject, style = MaterialTheme.typography.bodyMedium)
                        Text("From: ${rec.from}  •  Status: ${rec.status}", style = MaterialTheme.typography.bodySmall)
                        if (rec.attachmentUrl.isBlank()) Text("File: —", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (inward.isEmpty()) item { Text("No entries.") }
        }
    }
}

@Composable
private fun OutwardAbstract(outward: List<OutwardRecord>, inward: List<InwardRecord>) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Outward abstract", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(outward) { rec ->
                val linked = inward.find { it.id == rec.linkedInwardId }
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${rec.date}  •  No. ${rec.refNo}", style = MaterialTheme.typography.labelMedium)
                        Text(rec.subject, style = MaterialTheme.typography.bodyMedium)
                        Text("To: ${rec.to}", style = MaterialTheme.typography.bodySmall)
                        Text("Linked Inward: ${linked?.let { "${it.refNo} — ${it.subject}" } ?: "—"}", style = MaterialTheme.typography.bodySmall)
                        if (rec.attachmentUrl.isBlank()) Text("File: —", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (outward.isEmpty()) item { Text("No entries.") }
        }
    }
}

@Composable
private fun CombinedAbstract(inward: List<InwardRecord>, outward: List<OutwardRecord>) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Combined abstract", style = MaterialTheme.typography.titleLarge)
        Text(
            "Each Inward entry shown with its linked Outward. Entries without one are flagged PENDING.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        val standaloneOutward = outward.filter { it.linkedInwardId.isBlank() }

        LazyColumn {
            items(inward) { inw ->
                val out = outward.find { it.linkedInwardId == inw.id }
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${inw.date}  •  Inward ${inw.refNo}", style = MaterialTheme.typography.labelMedium)
                        Text(inw.subject, style = MaterialTheme.typography.bodyMedium)
                        Text("From: ${inw.from}", style = MaterialTheme.typography.bodySmall)
                        if (out != null) {
                            Text(
                                "Outward: ${out.refNo}  •  ${out.date}  •  To: ${out.to}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            AssistChip(onClick = {}, label = { Text("PENDING") })
                        }
                    }
                }
            }
            if (standaloneOutward.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)); Text("Outward with no linked Inward", style = MaterialTheme.typography.titleSmall) }
                items(standaloneOutward) { out ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${out.date}  •  Outward ${out.refNo}", style = MaterialTheme.typography.labelMedium)
                            Text(out.subject, style = MaterialTheme.typography.bodyMedium)
                            Text("To: ${out.to}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (inward.isEmpty() && standaloneOutward.isEmpty()) item { Text("No entries.") }
        }
    }
}
