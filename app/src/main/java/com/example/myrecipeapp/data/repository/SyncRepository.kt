package com.example.myrecipeapp.data.repository

import com.example.myrecipeapp.data.local.FavoriteDao
import com.example.myrecipeapp.data.local.FavoriteEntity
import com.example.myrecipeapp.data.local.ShoppingDao
import com.example.myrecipeapp.data.local.ShoppingItemEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val favoriteDao: FavoriteDao,
    private val shoppingDao: ShoppingDao
) {
    private fun favoritesRef(uid: String) =
        firestore.collection("users").document(uid).collection("favorites")

    private fun shoppingRef(uid: String) =
        firestore.collection("users").document(uid).collection("shoppingItems")

    // ── Favorites ─────────────────────────────────────────────────────────────

    suspend fun uploadFavorite(uid: String, entity: FavoriteEntity) {
        favoritesRef(uid).document(entity.id).set(entity.toMap()).await()
    }

    suspend fun deleteFavorite(uid: String, recipeId: String) {
        favoritesRef(uid).document(recipeId).delete().await()
    }

    /** Pull all favorites from Firestore and upsert into local Room DB. */
    suspend fun pullFavorites(uid: String) {
        val docs = favoritesRef(uid).get().await()
        docs.documents.forEach { doc ->
            val entity = doc.toFavoriteEntity() ?: return@forEach
            favoriteDao.insert(entity)
        }
    }

    // ── Shopping List ─────────────────────────────────────────────────────────

    suspend fun uploadShoppingItem(uid: String, entity: ShoppingItemEntity) {
        shoppingRef(uid).document(entity.key).set(entity.toMap()).await()
    }

    suspend fun deleteShoppingItem(uid: String, key: String) {
        shoppingRef(uid).document(key).delete().await()
    }

    suspend fun clearShoppingList(uid: String) {
        val docs = shoppingRef(uid).get().await()
        val batch = firestore.batch()
        docs.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    /** Pull all shopping items from Firestore and upsert into local Room DB. */
    suspend fun pullShoppingItems(uid: String) {
        val docs = shoppingRef(uid).get().await()
        docs.documents.forEach { doc ->
            val entity = doc.toShoppingItemEntity() ?: return@forEach
            shoppingDao.insert(entity)
        }
    }
}

// ── Firestore ↔ Entity mappers ────────────────────────────────────────────────

private fun FavoriteEntity.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "imageUrl" to imageUrl,
    "category" to category,
    "rating" to rating,
    "prepTime" to prepTime,
    "cookTime" to cookTime,
    "difficulty" to difficulty
)

private fun com.google.firebase.firestore.DocumentSnapshot.toFavoriteEntity(): FavoriteEntity? {
    return try {
        FavoriteEntity(
            id = getString("id") ?: return null,
            name = getString("name") ?: "",
            imageUrl = getString("imageUrl") ?: "",
            category = getString("category") ?: "",
            rating = (get("rating") as? Number)?.toFloat() ?: 0f,
            prepTime = (get("prepTime") as? Number)?.toInt() ?: 0,
            cookTime = (get("cookTime") as? Number)?.toInt() ?: 0,
            difficulty = getString("difficulty") ?: "MEDIUM"
        )
    } catch (e: Exception) {
        null
    }
}

private fun ShoppingItemEntity.toMap() = mapOf(
    "key" to key,
    "ingredientName" to ingredientName,
    "amount" to amount,
    "unit" to unit,
    "recipeName" to recipeName,
    "checked" to checked
)

private fun com.google.firebase.firestore.DocumentSnapshot.toShoppingItemEntity(): ShoppingItemEntity? {
    return try {
        ShoppingItemEntity(
            key = getString("key") ?: return null,
            ingredientName = getString("ingredientName") ?: "",
            amount = getString("amount") ?: "",
            unit = getString("unit") ?: "",
            recipeName = getString("recipeName") ?: "",
            checked = getBoolean("checked") ?: false
        )
    } catch (e: Exception) {
        null
    }
}
