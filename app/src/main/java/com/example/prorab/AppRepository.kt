package com.example.prorab.data

import android.app.Application
import com.example.prorab.presentation.auth.GoogleAuthClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await

class AppRepository(application: Application) {

    private val projectDao = AppDatabase.getDatabase(application).projectDao()
    private val recordDao = AppDatabase.getDatabase(application).recordDao()

    private val firestore = FirebaseFirestore.getInstance()
    private val authClient = GoogleAuthClient(application)
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val userFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.uid) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    // --- PROJECTS ---
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllProjects(): Flow<List<Project>> {
        return userFlow.flatMapLatest { userId ->
            if (userId != null) getCloudProjects(userId) else projectDao.getAllProjects()
        }
    }

    private fun getCloudProjects(userId: String): Flow<List<Project>> = callbackFlow {
        val collection = firestore.collection("users").document(userId).collection("projects")
            .orderBy("dateCreated", Query.Direction.DESCENDING)
        val listener = collection.addSnapshotListener { snapshot, _ ->
            val projects = snapshot?.documents?.map { doc ->
                Project(
                    id = doc.getString("id")?.hashCode() ?: 0,
                    name = doc.getString("name") ?: "Без названия",
                    dateCreated = doc.getLong("dateCreated") ?: 0L
                )
            } ?: emptyList()
            trySend(projects)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addProject(name: String) {
        val user = authClient.getSignedInUser()
        if (user != null) {
            val newRef = firestore.collection("users").document(user.userId).collection("projects").document()
            val data = hashMapOf("id" to newRef.id, "name" to name, "dateCreated" to System.currentTimeMillis())
            newRef.set(data).await()
        } else {
            projectDao.insertProject(Project(name = name))
        }
    }

    suspend fun deleteProject(project: Project) {
        val user = authClient.getSignedInUser()
        if (user != null) {
            val projQuery = firestore.collection("users").document(user.userId).collection("projects")
                .whereEqualTo("dateCreated", project.dateCreated).get().await()
            for (doc in projQuery.documents) doc.reference.delete()
        } else {
            projectDao.deleteProject(project)
        }
    }

    // --- RECORDS ---
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getRecords(projectId: Int): Flow<List<Record>> {
        return userFlow.flatMapLatest { userId ->
            if (userId != null) getCloudRecords(userId, projectId) else recordDao.getRecordsByProject(projectId)
        }
    }

    private fun getCloudRecords(userId: String, projectId: Int): Flow<List<Record>> = callbackFlow {
        val query = firestore.collection("users").document(userId).collection("records")
            .whereEqualTo("projectId", projectId)
        val listener = query.addSnapshotListener { snapshot, _ ->
            val records = snapshot?.documents?.map { doc ->
                Record(
                    id = doc.getString("id")?.hashCode() ?: 0,
                    projectId = (doc.getLong("projectId") ?: 0).toInt(),
                    type = (doc.getLong("type") ?: 0).toInt(),
                    title = doc.getString("title") ?: "",
                    quantity = doc.getDouble("quantity") ?: 0.0,
                    unit = doc.getString("unit") ?: "",
                    unitPrice = doc.getDouble("unitPrice") ?: 0.0,
                    amount = doc.getDouble("amount") ?: 0.0,
                    date = doc.getLong("date") ?: 0L,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            } ?: emptyList()

            // ИСПРАВЛЕНО: Теперь и старые, и новые данные из облака железно идут слева направо!
            trySend(records.sortedBy { it.date })
        }
        awaitClose { listener.remove() }
    }


    suspend fun addRecord(record: Record) {
        val user = authClient.getSignedInUser()
        if (user != null) {
            val newRef = firestore.collection("users").document(user.userId).collection("records").document()
            val data = hashMapOf(
                "id" to newRef.id,
                "projectId" to record.projectId,
                "type" to record.type,
                "title" to record.title,
                // Сохраняем новые поля + UNIT
                "quantity" to record.quantity,
                "unit" to record.unit, // <--- СОХРАНЯЕМ В ОБЛАКО
                "unitPrice" to record.unitPrice,
                "amount" to record.amount,
                "date" to record.date,
                "createdAt" to record.createdAt,
                "updatedAt" to record.updatedAt
            )
            newRef.set(data).await()
        } else {
            recordDao.insertRecord(record)
        }
    }

    suspend fun updateRecord(record: Record) {
        val user = authClient.getSignedInUser()
        if (user != null) {
            val snapshot = firestore.collection("users").document(user.userId).collection("records")
                .whereEqualTo("createdAt", record.createdAt).get().await()
            for (doc in snapshot.documents) {
                val updates = mapOf(
                    "type" to record.type,
                    "title" to record.title,
                    "quantity" to record.quantity,
                    "unit" to record.unit, // <--- ОБНОВЛЯЕМ В ОБЛАКЕ
                    "unitPrice" to record.unitPrice,
                    "amount" to record.amount,
                    "date" to record.date,
                    "updatedAt" to record.updatedAt
                )
                doc.reference.update(updates).await()
            }
        } else {
            recordDao.updateRecord(record)
        }
    }

    suspend fun deleteRecord(record: Record) {
        val user = authClient.getSignedInUser()
        if (user != null) {
            val snapshot = firestore.collection("users").document(user.userId).collection("records")
                .whereEqualTo("createdAt", record.createdAt).get().await()
            for (doc in snapshot.documents) doc.reference.delete().await()
        } else {
            recordDao.deleteRecord(record)
        }
    }
}