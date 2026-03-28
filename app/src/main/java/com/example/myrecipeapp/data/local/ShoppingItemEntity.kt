package com.example.myrecipeapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myrecipeapp.domain.model.ShoppingListItem

/**
 * Room entity for shopping list items.
 *
 * [key] is the natural deduplication key: "{recipeId}_{ingredientId}".
 * Using INSERT OR IGNORE on this primary key prevents the same ingredient
 * from being added twice when the user taps "Add to Shopping List" again.
 *
 * Note: field is named [checked] (not isChecked) to avoid Kotlin boolean
 * getter-naming ambiguity that can confuse Room's annotation processor.
 */
@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val key: String,
    val ingredientName: String,
    val amount: String,
    val unit: String,
    val recipeName: String,
    @ColumnInfo(name = "checked") val checked: Boolean = false
)

// ── Mapper ────────────────────────────────────────────────────────────────────

fun ShoppingItemEntity.toShoppingListItem() = ShoppingListItem(
    key            = key,
    ingredientName = ingredientName,
    amount         = amount,
    unit           = unit,
    recipeName     = recipeName,
    isChecked      = checked
)
