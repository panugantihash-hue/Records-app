package com.icadd.records

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.icadd.records.ui.AbstractScreen
import com.icadd.records.ui.AttendanceScreen
import com.icadd.records.ui.DashboardScreen
import com.icadd.records.ui.EmployeesScreen
import com.icadd.records.ui.InwardScreen
import com.icadd.records.ui.LoginScreen
import com.icadd.records.ui.OutwardScreen
import com.icadd.records.ui.StorageScreen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    RootApp(vm)
                }
            }
        }
    }
}

@Composable
fun RootApp(vm: AppViewModel) {
    var checkedAuth by remember { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.tryAutoLogin { ok -> loggedIn = ok; checkedAuth = true }
    }

    if (!checkedAuth) {
        Box(Modifier.padding(24.dp)) { CircularProgressIndicator() }
        return
    }

    if (!loggedIn) {
        LoginScreen(vm) { loggedIn = true }
    } else {
        AppShell(vm) { loggedIn = false }
    }
}

private data class NavTab(val route: String, val label: String)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppShell(vm: AppViewModel, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val isAdmin = vm.currentUser?.role == "admin"
    val tabs = buildList {
        add(NavTab("dashboard", "Home"))
        add(NavTab("inward", "Inward"))
        add(NavTab("outward", "Outward"))
        add(NavTab("abstract", "Abstract"))
        add(NavTab("attendance", "Attendance"))
        add(NavTab("storage", "Storage"))
        if (isAdmin) add(NavTab("employees", "Users"))
    }
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("ICADD Records")
                            Text(
                                "${vm.currentUser?.name ?: ""} · ${if (isAdmin) "Administrator" else "User"}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.logout(); onLogout() }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                        }
                    }
                )
                ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)) {
                    tabs.forEach { t ->
                        Tab(
                            selected = currentRoute == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            text = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(vm) }
            composable("inward") { InwardScreen(vm) }
            composable("outward") { OutwardScreen(vm) }
            composable("abstract") { AbstractScreen(vm) }
            composable("attendance") { AttendanceScreen(vm) }
            composable("storage") { StorageScreen() }
            if (isAdmin) composable("employees") { EmployeesScreen(vm) }
        }
    }
}
