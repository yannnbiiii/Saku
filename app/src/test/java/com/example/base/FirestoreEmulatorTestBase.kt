package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

  protected lateinit var firestore: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var databaseId: String

  @Before
  open fun setUpFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)

    // Menggunakan "(default)" agar sinkron dengan instance Firestore di kode utama aplikasi
    databaseId = if (resId != 0) context.getString(resId) else "(default)"

    // Dapatkan instance App dari FirebaseInitProvider, atau inisialisasi baru jika belum ada
    val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
      context,
      FirebaseOptions.Builder()
        .setApplicationId("com.aistudio.sakufinance.kztmqr")
        .setProjectId(PROJECT_ID)
        .setApiKey("fake-api-key-for-emulator")
        .build()
    )

    firestore = FirebaseFirestore.getInstance(app, databaseId)
    auth = FirebaseAuth.getInstance(app)

    // Memastikan koneksi selalu mengarah ke emulator (menghindari koneksi ke production)
    try {
      firestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
      firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
        .build()
    } catch (e: IllegalStateException) {
      // Abaikan Exception ini. Wajar terjadi pada test ke-2 dst karena
      // useEmulator tidak bisa dipanggil dua kali pada JVM Robolectric yang sama.
    }

    try {
      auth.useEmulator(EMULATOR_HOST, AUTH_PORT)
    } catch (e: Exception) {
      // Abaikan jika auth emulator sudah tersetting sebelumnya
    }
  }

  @After
  open fun tearDownFirebase() {
    auth.signOut()
    firestore.clearPersistence() // Membersihkan sisa data cache agar tidak bocor ke test berikutnya
  }

  protected suspend fun signInTestUser(email: String): String = withContext(Dispatchers.IO) {
    withTimeout(AUTH_TIMEOUT_MS) {
      val result = try {
        auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      } catch (unused: FirebaseAuthInvalidUserException) {
        auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      }
      checkNotNull(result.user?.uid) { "User auth failed" }
    }
  }

  protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
    var current: Throwable? = this
    while (current != null) {
      if (current is FirebaseFirestoreException) return current
      current = current.cause
    }
    return null
  }

  private companion object {
    const val EMULATOR_HOST = "127.0.0.1"
    const val FIRESTORE_PORT = 8085
    const val AUTH_PORT = 9099
    const val PROJECT_ID = "demo-no-project"
    const val DEFAULT_PASSWORD = "password123"
    const val AUTH_TIMEOUT_MS = 5000L
  }
}
