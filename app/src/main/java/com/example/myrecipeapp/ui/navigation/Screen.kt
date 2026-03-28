package com.example.myrecipeapp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using kotlinx.serialization.
 * Each object/class is the route itself — no string routes needed.
 *
 * Simple screens: @Serializable object
 * Screens with arguments: @Serializable data class with the arg as a field
 */

// ── Bottom nav screens ────────────────────────────────────────────────────────
@Serializable object Home
@Serializable object Categories
@Serializable object Search
@Serializable object Favorites
@Serializable object Settings

// ── Detail screens ────────────────────────────────────────────────────────────
@Serializable object CategoryDetail           // category passed via savedStateHandle (Parcelable)
@Serializable data class RecipeDetail(val recipeId: String)  // type-safe argument

// ── Secondary screens ─────────────────────────────────────────────────────────
@Serializable object Profile
@Serializable object About
@Serializable object ShoppingList
