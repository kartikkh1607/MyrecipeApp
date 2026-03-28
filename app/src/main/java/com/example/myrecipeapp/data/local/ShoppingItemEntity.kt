package com.example.myrecipeapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myrecipeapp.ui.viewmodel.MainViewModel

/**
 * Room entity for shopping list items.
 *
 * [key] is the natural deduplication key: "{recipeId}_{ingredientId}".
 * Using INSERT OR IGNORE on this primary key prevents the same ingredient
 * from being added twice when the user taps "Add to Shopping List" again.
 */
@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val key: String,          // "{recipeId}_{ingredientId}" — unique per ingredient
    val ingredientName: String,
    val amount: String,
    val unit: String,
    val recipeName: String,
    val isChecked: Boolean = false
)

// ── Mappers ───────────────────────────────────────────────────────────────────

fun ShoppingItemEntity.toShoppingListItem() = MainViewModel.ShoppingListItem(
    key            = key,
    ingredientName = ingredientName,
    amount         = amount,
    unit           = unit,
    recipeName     = recipeName,
    isChecked      = isChecked
)

fun MainViewModel.ShoppingListItem.toEntity() = ShoppingItemEntity(
    key            = key,
    ingredientName = ingredientName,
    amount         = amount,
    unit           = unit,
    recipeName     = recipeName,
    isChecked      = isChecked
)
