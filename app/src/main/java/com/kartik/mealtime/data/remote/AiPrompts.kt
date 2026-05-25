package com.kartik.mealtime.data.remote

/**
 * Shared prompt construction for all AI providers. Keeping this in one place
 * guarantees Gemini and Groq produce identically-formatted output (notably the
 * **bold** recipe names that [com.kartik.mealtime.ui.viewmodel.AiViewModel]
 * parses), so a fallback response looks the same to the user.
 */
internal object AiPrompts {

    fun buildChatPrompt(
        history: List<ChatMessage>,
        newMessage: String,
        dietaryPreferences: String = ""
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are a friendly and knowledgeable recipe assistant. You help users:")
        sb.appendLine("- Find recipes based on ingredients they have")
        sb.appendLine("- Suggest recipe variations and substitutions")
        sb.appendLine("- Answer cooking questions and provide tips")
        sb.appendLine("- Suggest recipes based on preferences and dietary needs")
        sb.appendLine()
        // Per-user dietary context — only emitted when the user has set preferences.
        if (dietaryPreferences.isNotBlank()) {
            sb.appendLine("IMPORTANT — User's dietary preferences: $dietaryPreferences.")
            sb.appendLine("ONLY suggest recipes that match these preferences. If asked about")
            sb.appendLine("recipes that conflict, gently note the conflict and offer alternatives.")
            sb.appendLine()
        }
        sb.appendLine("Keep responses conversational, helpful, and concise.")
        sb.appendLine("When suggesting recipes, mention the recipe name in **bold**.")
        sb.appendLine()
        sb.appendLine("Conversation:")
        history.takeLast(6).forEach { msg ->
            sb.appendLine("${if (msg.isUser) "User" else "Assistant"}: ${msg.content}")
        }
        sb.appendLine()
        sb.appendLine("User: $newMessage")
        sb.appendLine("Assistant:")
        return sb.toString()
    }

    fun buildRecommendationPrompt(
        favoriteRecipes: List<String>,
        dietaryPreferences: String
    ): String {
        val favList = favoriteRecipes.take(10).joinToString(", ")
        return """
            |You are a recipe recommendation expert. Based on the user's taste preferences,
            |suggest 3-5 personalized recipes they might enjoy.
            |
            |User's favorite recipes: $favList
            |${if (dietaryPreferences.isNotBlank()) "Dietary preferences: $dietaryPreferences" else ""}
            |
            |Format your response like this:
            |• **Recipe Name** - Brief description of why they'd like it
            |
            |Keep suggestions varied and interesting. Do not repeat the same cuisine or style.
        """.trimMargin()
    }
}
