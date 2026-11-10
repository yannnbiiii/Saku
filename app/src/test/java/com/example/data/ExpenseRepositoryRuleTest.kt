package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpenseRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testCreateAndReadExpense() = runBlocking {
    val uid = signInTestUser("alice_rule@test.com")
    val repo = ExpenseRepository(firestore)

    val expense = Expense(
      userId = uid,
      title = "Makan Siang",
      amount = 35000.0,
      categoryId = "cat_food",
      categoryName = "Makanan",
      date = "2026-11-08",
      notes = "Nasi Padang"
    )

    repo.addExpense(expense, uid)

    val expenses = repo.observeExpenses(uid).first()
    assertTrue("Should have at least 1 expense", expenses.isNotEmpty())
    val saved = expenses.first { it.title == "Makan Siang" }
    assertEquals(35000.0, saved.amount, 0.01)
    assertEquals("Makanan", saved.categoryName)
  }

  @Test
  fun testCategorySeedAndObservation() = runBlocking {
    val uid = signInTestUser("bob_rule@test.com")
    val catRepo = CategoryRepository(firestore)

    catRepo.seedDefaultsIfEmpty(uid)

    val categories = catRepo.observeCategories(uid).first()
    assertTrue("Should seed default categories", categories.size >= 5)
  }

  @Test
  fun testUnauthenticatedAccessRejected() = runBlocking {
    auth.signOut()
    val repo = ExpenseRepository(firestore)
    try {
      repo.addExpense(
        Expense(title = "Hacker", amount = 100.0, categoryId = "c", categoryName = "C", date = "2026-11-08"),
        "fake_uid"
      )
      fail("Expected exception when unauthenticated")
    } catch (e: Exception) {
      assertNotNull(e)
    }
  }
}
