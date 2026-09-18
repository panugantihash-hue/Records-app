package com.icadd.records

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    var currentUser by mutableStateOf<AppUser?>(null)
    var authError by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    private val _inward = MutableStateFlow<List<InwardRecord>>(emptyList())
    val inward: StateFlow<List<InwardRecord>> = _inward

    private val _outward = MutableStateFlow<List<OutwardRecord>>(emptyList())
    val outward: StateFlow<List<OutwardRecord>> = _outward

    private val _attendance = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendance: StateFlow<List<AttendanceRecord>> = _attendance

    private val _employees = MutableStateFlow<List<AppUser>>(emptyList())
    val employees: StateFlow<List<AppUser>> = _employees

    fun startListening() {
        viewModelScope.launch { FirebaseRepo.inwardFlow().collect { _inward.value = it } }
        viewModelScope.launch { FirebaseRepo.outwardFlow().collect { _outward.value = it } }
        viewModelScope.launch { FirebaseRepo.attendanceFlow().collect { _attendance.value = it } }
        viewModelScope.launch { FirebaseRepo.employeesFlow().collect { _employees.value = it } }
    }

    fun tryAutoLogin(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val u = FirebaseRepo.fetchCurrentUser()
            currentUser = u
            if (u != null) startListening()
            onDone(u != null)
        }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        loading = true; authError = null
        viewModelScope.launch {
            val res = FirebaseRepo.login(email, password)
            if (res.isSuccess) {
                currentUser = FirebaseRepo.fetchCurrentUser()
                startListening()
                loading = false
                onResult(true)
            } else {
                loading = false
                authError = res.exceptionOrNull()?.localizedMessage ?: "Login failed"
                onResult(false)
            }
        }
    }

    fun register(email: String, password: String, name: String, onResult: (Boolean) -> Unit) {
        loading = true; authError = null
        viewModelScope.launch {
            val res = FirebaseRepo.register(email, password, name)
            if (res.isSuccess) {
                currentUser = FirebaseRepo.fetchCurrentUser()
                startListening()
                loading = false
                onResult(true)
            } else {
                loading = false
                authError = res.exceptionOrNull()?.localizedMessage ?: "Registration failed"
                onResult(false)
            }
        }
    }

    fun logout() {
        FirebaseRepo.logout()
        currentUser = null
    }

    fun addInward(rec: InwardRecord) = viewModelScope.launch { FirebaseRepo.createInward(rec) }
    fun addOutward(rec: OutwardRecord) = viewModelScope.launch { FirebaseRepo.createOutward(rec) }
    fun addAttendance(rec: AttendanceRecord) = viewModelScope.launch { FirebaseRepo.recordAttendance(rec) }
    fun deleteInward(id: String) = viewModelScope.launch { FirebaseRepo.deleteInward(id) }
    fun deleteOutward(id: String) = viewModelScope.launch { FirebaseRepo.deleteOutward(id) }
    fun addEmployee(email: String, name: String, role: String, perms: Permissions) =
        viewModelScope.launch { FirebaseRepo.createEmployeePlaceholder(email, name, role, perms) }
    fun setAccessStatus(userId: String, status: String) =
        viewModelScope.launch { FirebaseRepo.setAccessStatus(userId, status) }

    val dashboardStats: DashboardStats
        get() {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
            return DashboardStats(
                inwardTotal = _inward.value.size,
                pendingCount = _inward.value.count { it.status != "Outward Issued" && it.status != "Disposed" },
                outwardTotal = _outward.value.size,
                employeesTotal = _employees.value.size,
                activeEmployees = _employees.value.count { it.accessStatus == "active" },
                presentToday = _attendance.value.count { it.date == today && it.status == "Present" },
                absentToday = _attendance.value.count { it.date == today && it.status == "Absent" }
            )
        }
}
