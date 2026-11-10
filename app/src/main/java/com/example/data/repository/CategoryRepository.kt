package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Category
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CategoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing categories.")
    }

    fun observeCategories(userId: String = requireUserId()): Flow<List<Category>> = flow {
        val path = "users/$userId/categories"
        emitAll(
            db.collection("users").document(userId).collection("categories")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Category::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addCategory(category: Category, userId: String = requireUserId()) {
        val docRef = db.collection("users").document(userId).collection("categories").document()
        val payload = mapOf(
            "userId" to userId,
            "name" to category.name,
            "iconName" to category.iconName,
            "colorHex" to category.colorHex,
            "isDefault" to category.isDefault,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCategory(category: Category, userId: String = requireUserId()) {
        require(category.id.isNotBlank()) { "Category ID must not be blank for update" }
        requireNotNull(category.createdAt) { "createdAt must not be null for update" }
        val docRef = db.collection("users").document(userId).collection("categories").document(category.id)
        val payload = mapOf(
            "userId" to userId,
            "name" to category.name,
            "iconName" to category.iconName,
            "colorHex" to category.colorHex,
            "isDefault" to category.isDefault,
            "createdAt" to category.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCategory(categoryId: String, userId: String = requireUserId()) {
        val docRef = db.collection("users").document(userId).collection("categories").document(categoryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun seedDefaultsIfEmpty(userId: String = requireUserId()) {
        val collection = db.collection("users").document(userId).collection("categories")
        val existing = collection.limit(1).get().await()
        if (existing.isEmpty) {
            val defaults = listOf(
                Triple("Makanan & Minuman", "restaurant", "#10B981"),
                Triple("Transportasi", "commute", "#3B82F6"),
                Triple("Hiburan", "movie", "#8B5CF6"),
                Triple("Tagihan", "receipt", "#EF4444"),
                Triple("Belanja", "shopping_bag", "#F59E0B"),
                Triple("Kesehatan", "medical_services", "#EC4899"),
                Triple("Pendidikan", "school", "#6366F1"),
                Triple("Investasi", "savings", "#14B8A6"),
                Triple("Elektronik", "devices", "#F97316"),
                Triple("Lain-lain", "more_horiz", "#64748B")
            )
            for ((name, icon, color) in defaults) {
                val doc = collection.document()
                val payload = mapOf(
                    "userId" to userId,
                    "name" to name,
                    "iconName" to icon,
                    "colorHex" to color,
                    "isDefault" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                doc.set(payload).await()
            }
        }
    }
}
