package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Budget
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

class BudgetRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing budgets.")
    }

    fun observeBudgets(userId: String = requireUserId()): Flow<List<Budget>> = flow {
        val path = "users/$userId/budgets"
        emitAll(
            db.collection("users").document(userId).collection("budgets")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Budget::class.java) }
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

    suspend fun saveBudget(budget: Budget, userId: String = requireUserId()) {
        val collection = db.collection("users").document(userId).collection("budgets")
        val docRef = if (budget.id.isNotBlank()) {
            collection.document(budget.id)
        } else {
            // Check if budget for this category and month exists
            val existing = collection
                .whereEqualTo("categoryId", budget.categoryId)
                .whereEqualTo("month", budget.month)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                collection.document(existing.documents.first().id)
            } else {
                collection.document()
            }
        }

        val isUpdate = budget.id.isNotBlank() || docRef.get().await().exists()
        val payload = if (isUpdate) {
            val existingDoc = docRef.get().await()
            val existingCreatedAt = existingDoc.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            mapOf(
                "userId" to userId,
                "categoryId" to budget.categoryId,
                "categoryName" to budget.categoryName,
                "monthlyLimit" to budget.monthlyLimit,
                "month" to budget.month,
                "createdAt" to existingCreatedAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "userId" to userId,
                "categoryId" to budget.categoryId,
                "categoryName" to budget.categoryName,
                "monthlyLimit" to budget.monthlyLimit,
                "month" to budget.month,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }

        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (isUpdate) OperationType.UPDATE else OperationType.CREATE
            handleFirestoreError(e, op, docRef.path)
            throw e
        }
    }

    suspend fun deleteBudget(budgetId: String, userId: String = requireUserId()) {
        val docRef = db.collection("users").document(userId).collection("budgets").document(budgetId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
