package com.icadd.records

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * All backend access lives here. Everything is Firestore (fast, real-time)
 * instead of the old Apps Script + Drive JSON approach.
 */
object FirebaseRepo {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun keyFor(email: String) = email.trim().lowercase().replace(Regex("[^a-z0-9]"), "_")

    val currentEmail: String? get() = auth.currentUser?.email

    // ---------- Auth ----------

    // Self-registration rule: the very first user ever becomes admin (bootstrap).
    // After that, an admin must first add a placeholder record (Employees screen)
    // with the person's email; only then can that email complete registration
    // (which just attaches a password to the pre-approved record).
    suspend fun register(email: String, password: String, name: String): Result<Unit> {
      return try {
        val cleanEmail = email.trim().lowercase()
        val docId = keyFor(cleanEmail)
        val usersSnap = db.collection("users").limit(1).get().await()
        val isFirstUser = usersSnap.isEmpty
        val existing = db.collection("users").document(docId).get().await()

        if (!isFirstUser && !existing.exists()) {
            return Result.failure(Exception("Not authorized. Ask your admin to add you as an employee first."))
        }

        auth.createUserWithEmailAndPassword(cleanEmail, password).await()

        if (isFirstUser) {
            val user = AppUser(
                id = docId, email = cleanEmail, name = name,
                role = "admin", accessStatus = "active",
                permissions = Permissions(
                    dashboard_view = true, inward_view = true, inward_create = true, inward_edit = true,
                    outward_view = true, outward_create = true, outward_edit = true,
                    attendance_view = true, attendance_manage = true,
                    storage_view = true, reports_view = true
                ),
                createdAt = System.currentTimeMillis()
            )
            db.collection("users").document(docId).set(user).await()
        } else if (existing.getString("name").isNullOrBlank()) {
            db.collection("users").document(docId).update("name", name).await()
        }
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

    suspend fun login(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() = auth.signOut()

    suspend fun fetchCurrentUser(): AppUser? {
        val email = currentEmail ?: return null
        val doc = db.collection("users").document(keyFor(email)).get().await()
        return doc.toObject(AppUser::class.java)
    }

    // ---------- Users / Employees (admin) ----------

    fun employeesFlow(): Flow<List<AppUser>> = collectionFlow("users") { it.toObject(AppUser::class.java) }

    suspend fun createEmployeePlaceholder(email: String, name: String, role: String, perms: Permissions) {
        val user = AppUser(
            id = keyFor(email), email = email.trim().lowercase(), name = name,
            role = role, accessStatus = "active", permissions = perms,
            createdAt = System.currentTimeMillis()
        )
        db.collection("users").document(user.id).set(user).await()
    }

    suspend fun setAccessStatus(userId: String, status: String) {
        db.collection("users").document(userId).update("accessStatus", status).await()
    }

    // ---------- Inward ----------

    fun inwardFlow(): Flow<List<InwardRecord>> =
        collectionFlow("inward", orderByDesc = "createdAt") { it.toObject(InwardRecord::class.java) }

    suspend fun createInward(rec: InwardRecord) {
        val id = "in_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        db.collection("inward").document(id).set(rec.copy(id = id, createdAt = System.currentTimeMillis())).await()
    }

    suspend fun updateInward(rec: InwardRecord) {
        db.collection("inward").document(rec.id).set(rec).await()
    }

    suspend fun deleteInward(id: String) {
        db.collection("inward").document(id).delete().await()
    }

    // ---------- Outward ----------

    fun outwardFlow(): Flow<List<OutwardRecord>> =
        collectionFlow("outward", orderByDesc = "createdAt") { it.toObject(OutwardRecord::class.java) }

    suspend fun createOutward(rec: OutwardRecord) {
        val id = "out_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        db.collection("outward").document(id).set(rec.copy(id = id, createdAt = System.currentTimeMillis())).await()
        if (rec.linkedInwardId.isNotBlank()) {
            db.collection("inward").document(rec.linkedInwardId).update("status", "Outward Issued").await()
        }
    }

    suspend fun deleteOutward(id: String) {
        db.collection("outward").document(id).delete().await()
    }

    // ---------- Attendance ----------

    fun attendanceFlow(): Flow<List<AttendanceRecord>> =
        collectionFlow("attendance", orderByDesc = "createdAt") { it.toObject(AttendanceRecord::class.java) }

    suspend fun recordAttendance(rec: AttendanceRecord) {
        val id = "att_${System.currentTimeMillis()}"
        db.collection("attendance").document(id).set(rec.copy(id = id, createdAt = System.currentTimeMillis())).await()
    }

    // ---------- File upload (attachments) ----------

    suspend fun uploadAttachment(bytes: ByteArray, fileName: String): String {
        val ref = storage.reference.child("attachments/${System.currentTimeMillis()}_$fileName")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    // ---------- Central storage (folders + files) ----------
    // Firebase Storage has no real folders, only path prefixes. A folder "exists"
    // once anything is uploaded under its path; we create empty folders with a
    // hidden .keep placeholder file.

    data class StorageEntry(val name: String, val path: String, val isFolder: Boolean, val downloadUrl: String = "")

    suspend fun listStorage(path: String): List<StorageEntry> {
        val ref = if (path.isBlank()) storage.reference.child("central") else storage.reference.child("central/$path")
        val result = ref.listAll().await()
        val folders = result.prefixes.map { StorageEntry(it.name, it.path.removePrefix("central/"), true) }
        val files = result.items.filter { it.name != ".keep" }.map {
            StorageEntry(it.name, it.path.removePrefix("central/"), false)
        }
        return folders.sortedBy { it.name } + files.sortedBy { it.name }
    }

    suspend fun createStorageFolder(parentPath: String, folderName: String) {
        val path = if (parentPath.isBlank()) folderName else "$parentPath/$folderName"
        val ref = storage.reference.child("central/$path/.keep")
        ref.putBytes(ByteArray(0)).await()
    }

    suspend fun uploadStorageFile(parentPath: String, fileName: String, bytes: ByteArray): String {
        val path = if (parentPath.isBlank()) fileName else "$parentPath/$fileName"
        val ref = storage.reference.child("central/$path")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun getDownloadUrl(path: String): String =
        storage.reference.child("central/$path").downloadUrl.await().toString()

    /** Flat list of every folder path in Central Storage, for "target folder" dropdowns. */
    suspend fun listAllFolders(path: String = ""): List<String> {
        val results = mutableListOf<String>()
        val ref = if (path.isBlank()) storage.reference.child("central") else storage.reference.child("central/$path")
        val result = ref.listAll().await()
        result.prefixes.forEach { prefix ->
            val folderPath = prefix.path.removePrefix("central/")
            results.add(folderPath)
            results.addAll(listAllFolders(folderPath))
        }
        return results
    }

    suspend fun searchStorageAll(query: String, path: String = ""): List<StorageEntry> {
        val results = mutableListOf<StorageEntry>()
        val ref = if (path.isBlank()) storage.reference.child("central") else storage.reference.child("central/$path")
        val result = ref.listAll().await()
        result.items.filter { it.name != ".keep" && it.name.contains(query, ignoreCase = true) }.forEach {
            results.add(StorageEntry(it.name, it.path.removePrefix("central/"), false))
        }
        result.prefixes.forEach { prefix ->
            results.addAll(searchStorageAll(query, prefix.path.removePrefix("central/")))
        }
        return results
    }

    // ---------- generic real-time collection listener ----------

    private inline fun <reified T> collectionFlow(
        path: String,
        orderByDesc: String? = null,
        crossinline map: (com.google.firebase.firestore.DocumentSnapshot) -> T?
    ): Flow<List<T>> = callbackFlow {
        var query: Query = db.collection(path)
        if (orderByDesc != null) query = query.orderBy(orderByDesc, Query.Direction.DESCENDING)
        val reg = query.addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.mapNotNull { map(it) })
        }
        awaitClose { reg.remove() }
    }
}
