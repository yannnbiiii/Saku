package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Expense
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

class ExpenseRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing expenses.")
    }

    fun observeExpenses(userId: String = requireUserId()): Flow<List<Expense>> = flow {
        val path = "users/$userId/expenses"
        emitAll(
            db.collection("users").document(userId).collection("expenses")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Expense::class.java) }
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

    suspend fun addExpense(expense: Expense, userId: String = requireUserId()) {
        val docRef = db.collection("users").document(userId).collection("expenses").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to expense.title,
            "amount" to expense.amount,
            "categoryId" to expense.categoryId,
            "categoryName" to expense.categoryName,
            "date" to expense.date,
            "notes" to expense.notes,
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

    suspend fun updateExpense(expense: Expense, userId: String = requireUserId()) {
        require(expense.id.isNotBlank()) { "Expense ID must not be blank for update" }
        requireNotNull(expense.createdAt) { "createdAt must not be null for update" }
        val docRef = db.collection("users").document(userId).collection("expenses").document(expense.id)
        val payload = mapOf(
            "userId" to userId,
            "title" to expense.title,
            "amount" to expense.amount,
            "categoryId" to expense.categoryId,
            "categoryName" to expense.categoryName,
            "date" to expense.date,
            "notes" to expense.notes,
            "createdAt" to expense.createdAt,
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

    suspend fun deleteExpense(expenseId: String, userId: String = requireUserId()) {
        val docRef = db.collection("users").document(userId).collection("expenses").document(expenseId)
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
