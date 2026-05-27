package com.kartik.mealtime.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using kotlinx.serialization.
 * Each object/class is the route itself — no string routes needed.
 *
 * Simple screens: @Serializable object
 * Screens with arguments: @Serializable data class with the arg as a field
 */

// ── Bottom nav screens ────────────────────────────────────────────────────────
@Serializable
object Home
@Serializable
object Categories
@Serializable
object Search
@Serializable
object Favorites
@Serializable
object Settings

// ── Detail screens ────────────────────────────────────────────────────────────
@Serializable
data class CategoryDetail(val categoryId: String)  // type-safe argument (process-death safe)
@Serializable
data class RecipeDetail(val recipeId: String)      // type-safe argument

// ── Secondary screens ─────────────────────────────────────────────────────────
@Serializable
object Auth
@Serializable
object Profile
@Serializable
object ShoppingList
@Serializable
object Chat
@Serializable
object AiCreations
