package com.icadd.records

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.icadd.records.ui.AttendanceScreen
import com.icadd.records.ui.DashboardScreen
import com.icadd.records.ui.EmployeesScreen
import com.icadd.records.ui.InwardScreen
import com.icadd.records.ui.LoginScreen
import com.icadd.records.ui.OutwardScreen

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

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppShell(vm: AppViewModel, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val isAdmin = vm.currentUser?.role == "admin"
    val tabs = buildList {
        add(Tab("dashboard", "Home", Icons.Filled.Dashboard))
        add(Tab("inward", "Inward", Icons.Filled.Inbox))
        add(Tab("outward", "Outward", Icons.Filled.Outbox))
        add(Tab("attendance", "Attendance", Icons.Filled.Groups))
        if (isAdmin) add(Tab("employees", "Employees", Icons.Filled.People))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ICADD Records") },
                actions = {
                    IconButton(onClick = { vm.logout(); onLogout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            val backStackEntry by nav.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(vm) }
            composable("inward") { InwardScreen(vm) }
            composable("outward") { OutwardScreen(vm) }
            composable("attendance") { AttendanceScreen(vm) }
            if (isAdmin) composable("employees") { EmployeesScreen(vm) }
        }
    }
}
