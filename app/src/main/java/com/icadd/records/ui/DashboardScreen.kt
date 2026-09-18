package com.icadd.records.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icadd.records.AppViewModel

@Composable
fun DashboardScreen(vm: AppViewModel) {
    val stats = vm.dashboardStats
    val cards = listOf(
        "Total Inward" to stats.inwardTotal.toString(),
        "Pending" to stats.pendingCount.toString(),
        "Total Outward" to stats.outwardTotal.toString(),
        "Employees" to "${stats.activeEmployees}/${stats.employeesTotal}",
        "Present Today" to stats.presentToday.toString(),
        "Absent Today" to stats.absentToday.toString()
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome, ${vm.currentUser?.name ?: ""}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cards) { (label, value) ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(value, style = MaterialTheme.typography.headlineMedium)
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
