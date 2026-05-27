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

    /**
     * The JSON contract a generated recipe must follow. Shared verbatim by Gemini
     * and Groq so both providers emit the exact shape [AiRecipeParser] expects.
     */
    private const val RECIPE_JSON_SCHEMA = """
{
  "name": string,
  "description": string,
  "category": string,            // e.g. "Dinner", "Dessert", "Breakfast"
  "cuisine": string,             // e.g. "Italian", "Indian"
  "difficulty": "EASY" | "MEDIUM" | "HARD",
  "prepTimeMinutes": integer,
  "cookTimeMinutes": integer,
  "servings": integer,
  "calories": integer,           // per serving, approximate
  "ingredients": [ { "name": string, "amount": string, "unit": string } ],
  "instructions": [ { "step": integer, "instruction": string, "tip": string } ],
  "tags": [ string ],
  "nutrition": { "calories": integer, "protein": number, "carbs": number, "fat": number, "fiber": number, "sugar": number, "sodium": number },
  "isVegetarian": boolean, "isVegan": boolean, "isGlutenFree": boolean,
  "isDairyFree": boolean, "isKeto": boolean, "isLowCarb": boolean
}"""

    /**
     * Prompt for generating ONE complete recipe as strict JSON. Used by both
     * providers; the response is parsed by [AiRecipeParser].
     */
    fun buildGenerateRecipePrompt(
        query: String,
        dietaryPreferences: String = ""
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are a professional chef. Create ONE complete, realistic recipe for the request below.")
        sb.appendLine()
        sb.appendLine("Request: $query")
        if (dietaryPreferences.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("The recipe MUST respect these dietary preferences: $dietaryPreferences.")
            sb.appendLine("Set the matching boolean flags (isVegetarian, isVegan, etc.) accordingly.")
        }
        sb.appendLine()
        sb.appendLine("Respond with ONLY a JSON object (no markdown, no commentary) matching this schema:")
        sb.appendLine(RECIPE_JSON_SCHEMA)
        sb.appendLine()
        sb.appendLine("Use concrete amounts, realistic times, and clear numbered steps. Estimate nutrition reasonably.")
        return sb.toString()
    }

    /**
     * Prompt for transforming an existing recipe per a free-text instruction (the
     * "Remix with AI" feature). Embeds a compact summary of [base] so the model has the
     * full starting point, applies [instruction], and returns ONE complete recipe as
     * strict JSON in the same schema [buildGenerateRecipePrompt] uses, so the response
     * parses through [AiRecipeParser] identically.
     */
    fun buildTransformPrompt(
        base: com.kartik.mealtime.domain.model.Recipe,
        instruction: String,
        dietaryPreferences: String = ""
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are a professional chef. Transform the recipe below according to the user's request.")
        sb.appendLine("Return a COMPLETE recipe with every field recomputed (ingredients, steps, times, servings, nutrition, and the dietary boolean flags) so it is consistent with the change.")
        sb.appendLine()
        sb.appendLine("Original recipe:")
        sb.appendLine(base.toPromptSummary())
        sb.appendLine()
        sb.appendLine("Requested change: $instruction")
        if (dietaryPreferences.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("The result MUST also respect these dietary preferences: $dietaryPreferences.")
        }
        sb.appendLine()
        sb.appendLine("Respond with ONLY a JSON object (no markdown, no commentary) matching this schema:")
        sb.appendLine(RECIPE_JSON_SCHEMA)
        sb.appendLine()
        sb.appendLine("Keep the dish recognizably derived from the original unless the request says otherwise. Use concrete amounts and realistic times.")
        return sb.toString()
    }

    /** Compact, model-friendly rendering of a recipe used as context in transform prompts. */
    private fun com.kartik.mealtime.domain.model.Recipe.toPromptSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("Name: $name")
        if (description.isNotBlank()) sb.appendLine("Description: $description")
        if (cuisine.isNotBlank()) sb.appendLine("Cuisine: $cuisine")
        if (category.isNotBlank()) sb.appendLine("Category: $category")
        sb.appendLine("Servings: $servings; Prep: ${prepTime}m; Cook: ${cookTime}m")
        if (ingredients.isNotEmpty()) {
            sb.appendLine("Ingredients:")
            ingredients.forEach { ing ->
                val qty = listOf(ing.amount, ing.unit).filter { it.isNotBlank() }.joinToString(" ")
                sb.appendLine("- ${if (qty.isBlank()) ing.name else "$qty ${ing.name}"}")
            }
        }
        if (instructions.isNotEmpty()) {
            sb.appendLine("Steps:")
            instructions.forEach { step -> sb.appendLine("${step.stepNumber}. ${step.instruction}") }
        }
        return sb.toString().trimEnd()
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
