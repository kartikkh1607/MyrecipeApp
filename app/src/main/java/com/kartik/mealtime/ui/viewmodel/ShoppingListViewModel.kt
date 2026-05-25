package com.kartik.mealtime.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.ShoppingDao
import com.kartik.mealtime.data.local.ShoppingItemEntity
import com.kartik.mealtime.data.local.toShoppingListItem
import com.kartik.mealtime.data.repository.SyncRepository
import com.kartik.mealtime.data.repository.UserRepository
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.ShoppingListItem
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _shoppingList = mutableStateOf<List<ShoppingListItem>>(emptyList())
    val shoppingList: State<List<ShoppingListItem>> = _shoppingList

    /** The last recipe name passed to [addToShoppingList]. Used by ShoppingListScreen to auto focus that section. */
    private val _lastAddedRecipeName = mutableStateOf<String?>(null)
    val lastAddedRecipeName: State<String?> = _lastAddedRecipeName

    init {
        viewModelScope.launch {
            shoppingDao.getAllFlow().collect { entities ->
                _shoppingList.value = entities.map { it.toShoppingListItem() }
            }
        }
    }

    /**
     * Appends [recipe]'s ingredients to the shopping list, preserving items already
     * added from other recipes. Duplicates (same recipe + ingredient, keyed below) are
     * de-duplicated by the DAO's IGNORE-on-conflict insert, so re-adding is a no-op.
     */
    fun addToShoppingList(recipe: Recipe) {
        _lastAddedRecipeName.value = recipe.name
        analytics.logShoppingListUpdated("add_recipe", recipe.name)
        viewModelScope.launch {
            val uid = userRepository.currentUser?.uid
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
                uid?.let { runCatching { syncRepository.uploadShoppingItem(it, entity) } }
            }
        }
    }

    fun toggleShoppingItem(key: String) {
        viewModelScope.launch {
            val current = _shoppingList.value.find { it.key == key } ?: return@launch
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
                runCatching { syncRepository.deleteShoppingItem(uid, key) }
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
                runCatching { syncRepository.clearShoppingList(uid) }
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
