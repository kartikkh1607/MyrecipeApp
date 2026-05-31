package com.kartik.mealtime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.ShoppingDao
import com.kartik.mealtime.data.local.ShoppingItemEntity
import com.kartik.mealtime.data.local.toShoppingListItem
import com.kartik.mealtime.data.repository.SyncRepository
import com.kartik.mealtime.data.repository.UserRepository
import com.kartik.mealtime.data.repository.syncBestEffort
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.ShoppingListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the Room-backed shopping list (extracted from MainViewModel).
 *
 * Shared as a single activity-scoped instance across RecipeDetailScreen (which
 * calls [addToShoppingList]) and ShoppingListScreen (which renders + edits it),
 * so the in-memory [lastAddedRecipeName] auto-focus hint survives the navigation
 * between them. The list itself is the single source of truth in Room — both
 * screens observe the same DAO Flow.
 */
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingDao: ShoppingDao,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
    private val analytics: AnalyticsHelper
) : ViewModel() {

    val shoppingList: StateFlow<ImmutableList<ShoppingListItem>> = shoppingDao.getAllFlow()
        .map { entities -> entities.map { it.toShoppingListItem() }.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    /** The last recipe name passed to [addToShoppingList]. Used by ShoppingListScreen to auto focus that section. */
    private val _lastAddedRecipeName = MutableStateFlow<String?>(null)
    val lastAddedRecipeName: StateFlow<String?> = _lastAddedRecipeName.asStateFlow()

    /**
     * Replaces the shopping list with [recipe]'s ingredients. Tapping "Add to
     * shopping list" on a recipe is treated as starting a fresh list, so anything
     * previously there (other recipes' items and custom additions) is cleared
     * first — both locally and remotely when signed in.
     */
    fun addToShoppingList(recipe: Recipe) {
        _lastAddedRecipeName.value = recipe.name
        analytics.logShoppingListUpdated("add_recipe", recipe.name)
        viewModelScope.launch { replaceShoppingListInternal(listOf(recipe)) }
    }

    /**
     * Replaces the shopping list with the ingredients from every recipe in
     * [recipes]. Used when adding a whole meal plan in one tap — clears the
     * list once, then inserts every meal's ingredients.
     */
    fun replaceShoppingListWith(recipes: List<Recipe>) {
        if (recipes.isEmpty()) return
        _lastAddedRecipeName.value = null
        analytics.logShoppingListUpdated("add_recipes")
        viewModelScope.launch { replaceShoppingListInternal(recipes) }
    }

    private suspend fun replaceShoppingListInternal(recipes: List<Recipe>) {
        val uid = userRepository.currentUser?.uid
        shoppingDao.deleteAll()
        uid?.let { syncBestEffort("clearShoppingList") { syncRepository.clearShoppingList(it) } }
        recipes.forEach { recipe ->
            recipe.ingredients.forEach { ing ->
                val key = "${recipe.id}_${ing.id.ifEmpty { ing.name }}"
                val entity = ShoppingItemEntity(
                    key = key,
                    ingredientName = ing.name,
                    amount = ing.amount,
                    unit = ing.unit,
                    recipeName = recipe.name
                )
                shoppingDao.insert(entity)
                uid?.let { syncBestEffort("uploadShoppingItem") { syncRepository.uploadShoppingItem(it, entity) } }
            }
        }
    }

    fun toggleShoppingItem(key: String) {
        viewModelScope.launch {
            val current = shoppingList.value.find { it.key == key } ?: return@launch
            shoppingDao.setChecked(key, !current.isChecked)
        }
    }

    fun removeCheckedItems() {
        viewModelScope.launch { shoppingDao.deleteChecked() }
    }

    fun removeItem(key: String) {
        viewModelScope.launch {
            shoppingDao.deleteByKey(key)
            userRepository.currentUser?.uid?.let { uid ->
                syncBestEffort("deleteShoppingItem") { syncRepository.deleteShoppingItem(uid, key) }
            }
        }
    }

    /** Restores a previously deleted shopping item (used by undo-snackbar). */
    fun restoreShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            shoppingDao.insert(
                ShoppingItemEntity(
                    key = item.key,
                    ingredientName = item.ingredientName,
                    amount = item.amount,
                    unit = item.unit,
                    recipeName = item.recipeName,
                    checked = item.isChecked
                )
            )
        }
    }

    fun clearShoppingList() {
        viewModelScope.launch {
            shoppingDao.deleteAll()
            userRepository.currentUser?.uid?.let { uid ->
                syncBestEffort("clearShoppingList") { syncRepository.clearShoppingList(uid) }
            }
        }
    }

    /**
     * Adds a free-text item (not from any recipe) to the shopping list.
     * Groups under the "Custom" section so it's visually distinct.
     */
    fun addCustomShoppingItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val key =
                "custom_${System.currentTimeMillis()}_${trimmed.lowercase().replace(' ', '_')}"
            shoppingDao.insert(
                ShoppingItemEntity(
                    key = key,
                    ingredientName = trimmed,
                    amount = "",
                    unit = "",
                    recipeName = "Custom"
                )
            )
        }
    }
}
