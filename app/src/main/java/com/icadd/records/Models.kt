package com.icadd.records

data class Permissions(
    val dashboard_view: Boolean = true,
    val inward_view: Boolean = false,
    val inward_create: Boolean = false,
    val inward_edit: Boolean = false,
    val outward_view: Boolean = false,
    val outward_create: Boolean = false,
    val outward_edit: Boolean = false,
    val attendance_view: Boolean = false,
    val attendance_manage: Boolean = false,
    val storage_view: Boolean = false,
    val reports_view: Boolean = false
)

data class AppUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val employeeCode: String = "",
    val designation: String = "",
    val section: String = "",
    val office: String = "",
    val mobile: String = "",
    val role: String = "field_officer", // "admin" or "field_officer"
    val accessStatus: String = "active", // "active" or "inactive"
    val permissions: Permissions = Permissions(),
    val createdAt: Long = 0L
)

data class InwardRecord(
    val id: String = "",
    val date: String = "",
    val from: String = "",
    val refNo: String = "",
    val subject: String = "",
    val remarks: String = "",
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val status: String = "Received",
    val assignedTo: String = "",
    val priority: String = "Normal",
    val dueDate: String = "",
    val enteredBy: String = "",
    val createdAt: Long = 0L
)

data class OutwardRecord(
    val id: String = "",
    val date: String = "",
    val to: String = "",
    val refNo: String = "",
    val subject: String = "",
    val remarks: String = "",
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val linkedInwardId: String = "",
    val enteredBy: String = "",
    val createdAt: Long = 0L
)

data class AttendanceRecord(
    val id: String = "",
    val date: String = "",
    val personName: String = "",
    val status: String = "Present",
    val remarks: String = "",
    val recordedBy: String = "",
    val createdAt: Long = 0L
)

data class DashboardStats(
    val inwardTotal: Int = 0,
    val pendingCount: Int = 0,
    val outwardTotal: Int = 0,
    val employeesTotal: Int = 0,
    val activeEmployees: Int = 0,
    val presentToday: Int = 0,
    val absentToday: Int = 0
)
