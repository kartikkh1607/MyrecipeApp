package com.example.myrecipeapp.domain.model

/**
 * Domain model for a single shopping list entry.
 *
 * Lives in the domain layer so both the data layer (Room mappers)
 * and the UI layer (ViewModel, screens) can import it without circular dependencies.
 */
data class ShoppingListItem(
    val key: String,            // "{recipeId}_{ingredientId}" — unique dedup key
    val ingredientName: String,
    val amount: String,
    val unit: String,
    val recipeName: String,
    val isChecked: Boolean = false
)
