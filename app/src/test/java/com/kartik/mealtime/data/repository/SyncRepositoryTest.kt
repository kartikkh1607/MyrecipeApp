package com.kartik.mealtime.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.local.FavoriteEntity
import com.kartik.mealtime.data.local.ShoppingDao
import com.kartik.mealtime.data.local.ShoppingItemEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Unit tests for [SyncRepository]'s Firestore wiring.
 *
 * The whole `collection(...).document(...).collection(...)` fluent chain is
 * Mockito-stubbed, and every async leaf returns an *already-completed*
 * [com.google.android.gms.tasks.Task] (via [Tasks.forResult]). `kotlinx-coroutines`'
 * `Task.await()` resolves a completed task synchronously on a direct executor, so
 * these run on the plain JVM with no Robolectric / Looper. Local Room writes land
 * in recording fakes we assert against.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryTest {

    private class RecordingFavoriteDao : FavoriteDao {
        val inserted = mutableListOf<FavoriteEntity>()
        var deleteAllCount = 0
        override fun getAllFlow(): Flow<List<FavoriteEntity>> = throw UnsupportedOperationException()
        override fun getAllSync(): List<FavoriteEntity> = emptyList()
        override suspend fun insert(entity: FavoriteEntity) { inserted += entity }
        override suspend fun delete(id: String) {}
        override suspend fun deleteAll() { deleteAllCount++ }
    }

    private class RecordingShoppingDao : ShoppingDao {
        var deleteAllCount = 0
        override fun getAllFlow(): Flow<List<ShoppingItemEntity>> = throw UnsupportedOperationException()
        override suspend fun insert(entity: ShoppingItemEntity) {}
        override suspend fun setChecked(key: String, checked: Boolean) {}
        override suspend fun deleteByKey(key: String) {}
        override suspend fun deleteChecked() {}
        override suspend fun deleteAll() { deleteAllCount++ }
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var usersCol: CollectionReference
    private lateinit var userDoc: DocumentReference
    private lateinit var favCol: CollectionReference
    private lateinit var shopCol: CollectionReference
    private lateinit var favoriteDao: RecordingFavoriteDao
    private lateinit var shoppingDao: RecordingShoppingDao

    private fun voidTask(): Task<Void> = Tasks.forResult(null)
    private fun <T> task(value: T): Task<T> = Tasks.forResult(value)

    @Before
    fun setUp() {
        firestore = mock(FirebaseFirestore::class.java)
        usersCol = mock(CollectionReference::class.java)
        userDoc = mock(DocumentReference::class.java)
        favCol = mock(CollectionReference::class.java)
        shopCol = mock(CollectionReference::class.java)
        favoriteDao = RecordingFavoriteDao()
        shoppingDao = RecordingShoppingDao()

        Mockito.`when`(firestore.collection("users")).thenReturn(usersCol)
        Mockito.`when`(usersCol.document("uid1")).thenReturn(userDoc)
        Mockito.`when`(userDoc.collection("favorites")).thenReturn(favCol)
        Mockito.`when`(userDoc.collection("shoppingItems")).thenReturn(shopCol)
    }

    private fun repo() = SyncRepository(firestore, favoriteDao, shoppingDao)

    private fun favEntity(id: String) = FavoriteEntity(
        id = id, name = "Recipe $id", imageUrl = "", category = "",
        rating = 0f, prepTime = 0, cookTime = 0, difficulty = "MEDIUM"
    )

    // ── uploadFavorite / deleteFavorite ────────────────────────────────────────────

    @Test
    fun `uploadFavorite writes the entity to its favorites document`() = runTest {
        val favDoc = mock(DocumentReference::class.java)
        Mockito.`when`(favCol.document("r1")).thenReturn(favDoc)
        Mockito.`when`(favDoc.set(any())).thenReturn(voidTask())

        repo().uploadFavorite("uid1", favEntity("r1"))

        verify(favDoc).set(any())
    }

    @Test
    fun `deleteFavorite deletes the matching favorites document`() = runTest {
        val favDoc = mock(DocumentReference::class.java)
        Mockito.`when`(favCol.document("r1")).thenReturn(favDoc)
        Mockito.`when`(favDoc.delete()).thenReturn(voidTask())

        repo().deleteFavorite("uid1", "r1")

        verify(favDoc).delete()
    }

    // ── pullFavorites ──────────────────────────────────────────────────────────────

    @Test
    fun `pullFavorites upserts every remote document into the local DAO`() = runTest {
        val docSnap = mock(DocumentSnapshot::class.java)
        Mockito.`when`(docSnap.getString("id")).thenReturn("r1")
        val snapshot = mock(QuerySnapshot::class.java)
        Mockito.`when`(snapshot.documents).thenReturn(listOf(docSnap))
        Mockito.`when`(favCol.get()).thenReturn(task(snapshot))

        repo().pullFavorites("uid1")

        assertEquals(listOf("r1"), favoriteDao.inserted.map { it.id })
    }

    // ── deleteAllUserData ──────────────────────────────────────────────────────────

    @Test
    fun `deleteAllUserData clears both remote collections, the user doc, and local mirrors`() = runTest {
        val emptySnap = mock(QuerySnapshot::class.java)
        Mockito.`when`(emptySnap.isEmpty).thenReturn(true)   // each batch loop breaks immediately

        val favQuery = mock(Query::class.java)
        Mockito.`when`(favCol.limit(500L)).thenReturn(favQuery)
        Mockito.`when`(favQuery.get()).thenReturn(task(emptySnap))

        val shopQuery = mock(Query::class.java)
        Mockito.`when`(shopCol.limit(500L)).thenReturn(shopQuery)
        Mockito.`when`(shopQuery.get()).thenReturn(task(emptySnap))

        Mockito.`when`(userDoc.delete()).thenReturn(voidTask())

        repo().deleteAllUserData("uid1")

        verify(favQuery).get()
        verify(shopQuery).get()
        verify(userDoc).delete()
        assertEquals(1, favoriteDao.deleteAllCount)
        assertEquals(1, shoppingDao.deleteAllCount)
    }
}
