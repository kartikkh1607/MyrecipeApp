package com.kartik.mealtime.data.remote

import com.kartik.mealtime.domain.model.RecipeDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AiRecipeParser] — the riskiest part of the structured-output
 * pipeline, since it must survive whatever a language model actually emits.
 */
class AiRecipeParserTest {

    private val validJson = """
        {
          "name": "Spicy Paneer Curry",
          "description": "A creamy, spicy curry.",
          "category": "Dinner",
          "cuisine": "Indian",
          "difficulty": "medium",
          "prepTimeMinutes": 15,
          "cookTimeMinutes": 25,
          "servings": 4,
          "calories": 420,
          "ingredients": [
            { "name": "Paneer", "amount": "250", "unit": "g" },
            { "name": "Yogurt", "amount": "1", "unit": "cup" }
          ],
          "instructions": [
            { "step": 1, "instruction": "Cube the paneer." },
            { "step": 2, "instruction": "Simmer in spiced yogurt.", "tip": "Don't boil the yogurt." }
          ],
          "tags": ["spicy", "vegetarian"],
          "isVegetarian": true
        }
    """.trimIndent()

    @Test
    fun `parses a valid recipe and assigns an ai- id`() {
        val recipe = AiRecipeParser.parse(validJson).getOrThrow()

        assertTrue("id should be prefixed", recipe.id.startsWith("ai-"))
        assertEquals("Spicy Paneer Curry", recipe.name)
        assertEquals(RecipeDifficulty.MEDIUM, recipe.difficulty)
        assertEquals(2, recipe.ingredients.size)
        assertEquals(2, recipe.instructions.size)
        assertTrue(recipe.isVegetarian)
        assertEquals(420, recipe.calories)
        // image is intentionally blank for AI recipes
        assertEquals("", recipe.imageUrl)
    }

    @Test
    fun `tolerates markdown-fenced json`() {
        val fenced = "Here you go!\n```json\n$validJson\n```\nEnjoy."
        val recipe = AiRecipeParser.parse(fenced).getOrThrow()
        assertEquals("Spicy Paneer Curry", recipe.name)
    }

    @Test
    fun `renumbers steps sequentially regardless of model numbering`() {
        val recipe = AiRecipeParser.parse(validJson).getOrThrow()
        assertEquals(listOf(1, 2), recipe.instructions.map { it.stepNumber })
    }

    @Test
    fun `malformed json fails gracefully`() {
        val result = AiRecipeParser.parse("not json at all")
        assertTrue(result.isFailure)
    }

    @Test
    fun `recipe without name or content is rejected`() {
        val empty = """{ "description": "nothing useful here" }"""
        val result = AiRecipeParser.parse(empty)
        assertTrue(result.isFailure)
    }
}
