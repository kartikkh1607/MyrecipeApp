package com.kartik.mealtime.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    fun logRecipeViewed(recipeId: String, recipeName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, recipeId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, recipeName)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "recipe")
        })
    }

    fun logRecipeFavorited(recipeId: String, recipeName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, recipeId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, recipeName)
        })
    }

    fun logSearch(query: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SEARCH, Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, query)
        })
    }

    fun logCategoryViewed(categoryId: String, categoryName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "category")
            putString(FirebaseAnalytics.Param.ITEM_ID, categoryId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, categoryName)
        })
    }

    fun logCookingModeStarted(recipeId: String, recipeName: String) {
        analytics.logEvent("cooking_mode_started", Bundle().apply {
            putString("recipe_id", recipeId)
            putString("recipe_name", recipeName)
        })
    }

    fun logAiChatMessageSent() {
        analytics.logEvent("ai_chat_message_sent", null)
    }

    fun logShoppingListUpdated(action: String, recipeName: String? = null) {
        analytics.logEvent("shopping_list_updated", Bundle().apply {
            putString("action", action)
            recipeName?.let { putString("recipe_name", it) }
        })
    }
}
