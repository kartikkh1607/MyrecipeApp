package com.kartik.mealtime.data.remote.dto

import com.kartik.mealtime.domain.model.RecipeDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for the Spoonacular DTO → domain mappers.
 *
 * These are the single highest-value tests for Play Store stability: Spoonacular
 * routinely omits fields (image, summary, ingredients, cuisines, nutrition), and a
 * naive mapper would NPE on real responses. Every test here pins a defensive default.
 */
class DtoMappersTest {

    // ── Test builders ─────────────────────────────────────────────────────────
    // Mirrors SpoonacularRecipeDto's required params with safe defaults so each
    // test overrides only the field under test.
    private fun recipeDto(
        id: Int = 1,
        title: String = "Test Recipe",
        image: String? = "https://img/1.jpg",
        servings: Int = 4,
        readyInMinutes: Int = 30,
        healthScore: Float = 80f,
        spoonacularScore: Float = 90f,
        summary: String? = "A tasty dish",
        dishTypes: List<String>? = listOf("main course"),
        cuisines: List<String>? = listOf("italian"),
        isVegetarian: Boolean = false,
        isVegan: Boolean = false,
        isGlutenFree: Boolean = false,
        isDairyFree: Boolean = false,
        ingredients: List<SpoonacularIngredientDto>? = emptyList(),
        instructions: List<SpoonacularInstructionDto>? = emptyList(),
        nutrition: SpoonacularNutritionDto? = null,
        videoUrl: String? = null
    ) = SpoonacularRecipeDto(
        id = id,
        title = title,
        image = image,
        servings = servings,
        readyInMinutes = readyInMinutes,
        healthScore = healthScore,
        spoonacularScore = spoonacularScore,
        summary = summary,
        dishTypes = dishTypes,
        cuisines = cuisines,
        isVegetarian = isVegetarian,
        isVegan = isVegan,
        isGlutenFree = isGlutenFree,
        isDairyFree = isDairyFree,
        ingredients = ingredients,
        instructions = instructions,
        nutrition = nutrition,
        videoUrl = videoUrl
    )

    // ── Defensive null handling (the real crash surface) ────────────────────────

    @Test
    fun `null image falls back to placeholder`() {
        val recipe = recipeDto(image = null).toDomain()
        assertEquals("https://placehold.co/600x400?text=No+Image", recipe.imageUrl)
    }

    @Test
    fun `null summary maps to empty description`() {
        val recipe = recipeDto(summary = null).toDomain()
        assertEquals("", recipe.description)
    }

    @Test
    fun `null dishTypes defaults category to General and tags to empty`() {
        val recipe = recipeDto(dishTypes = null).toDomain()
        assertEquals("General", recipe.category)
        assertTrue(recipe.tags.isEmpty())
    }

    @Test
    fun `null cuisines maps to empty cuisine`() {
        val recipe = recipeDto(cuisines = null).toDomain()
        assertEquals("", recipe.cuisine)
    }

    @Test
    fun `null ingredients maps to empty list`() {
        val recipe = recipeDto(ingredients = null).toDomain()
        assertTrue(recipe.ingredients.isEmpty())
    }

    @Test
    fun `null instructions maps to empty list`() {
        val recipe = recipeDto(instructions = null).toDomain()
        assertTrue(recipe.instructions.isEmpty())
    }

    @Test
    fun `instruction block with null steps does not crash`() {
        val dto = recipeDto(
            instructions = listOf(SpoonacularInstructionDto(name = "", steps = null))
        )
        assertTrue(dto.toDomain().instructions.isEmpty())
    }

    @Test
    fun `null nutrition leaves nutritionInfo and calories null`() {
        val recipe = recipeDto(nutrition = null).toDomain()
        assertNull(recipe.nutritionInfo)
        assertNull(recipe.calories)
    }

    // ── Derived-value math ──────────────────────────────────────────────────────

    @Test
    fun `rating is healthScore divided by 20`() {
        assertEquals(4.0f, recipeDto(healthScore = 80f).toDomain().rating, 0.001f)
    }

    @Test
    fun `rating is coerced to a maximum of 5`() {
        assertEquals(5.0f, recipeDto(healthScore = 200f).toDomain().rating, 0.001f)
    }

    @Test
    fun `prep and cook time split readyInMinutes 40-60`() {
        val recipe = recipeDto(readyInMinutes = 30).toDomain()
        assertEquals(12, recipe.prepTime)  // 30 * 0.4
        assertEquals(18, recipe.cookTime)  // 30 * 0.6
    }

    @Test
    fun `difficulty buckets on readyInMinutes`() {
        assertEquals(RecipeDifficulty.EASY, recipeDto(readyInMinutes = 10).toDomain().difficulty)
        assertEquals(RecipeDifficulty.MEDIUM, recipeDto(readyInMinutes = 30).toDomain().difficulty)
        assertEquals(RecipeDifficulty.HARD, recipeDto(readyInMinutes = 60).toDomain().difficulty)
    }

    @Test
    fun `difficulty boundaries are exclusive at 20 and 45`() {
        assertEquals(RecipeDifficulty.MEDIUM, recipeDto(readyInMinutes = 20).toDomain().difficulty)
        assertEquals(RecipeDifficulty.HARD, recipeDto(readyInMinutes = 45).toDomain().difficulty)
    }

    // ── HTML stripping & truncation ─────────────────────────────────────────────

    @Test
    fun `summary HTML tags are stripped`() {
        val recipe = recipeDto(summary = "<b>Hello</b> <i>there</i>").toDomain()
        assertEquals("Hello there", recipe.description)
    }

    @Test
    fun `summary is truncated to 500 characters before stripping`() {
        val recipe = recipeDto(summary = "a".repeat(600)).toDomain()
        assertTrue(recipe.description.length <= 500)
    }

    // ── Scalar passthrough ──────────────────────────────────────────────────────

    @Test
    fun `id is stringified and flags pass through`() {
        val recipe = recipeDto(id = 42, isVegan = true, isGlutenFree = true).toDomain()
        assertEquals("42", recipe.id)
        assertTrue(recipe.isVegan)
        assertTrue(recipe.isGlutenFree)
    }

    // ── Ingredient mapping ──────────────────────────────────────────────────────

    @Test
    fun `ingredient falls back to original when nameClean is null`() {
        val dto = SpoonacularIngredientDto(
            id = 1, name = null, amount = 2f, unit = "cups", original = "2 cups flour"
        )
        assertEquals("2 cups flour", dto.toDomain().name)
    }

    @Test
    fun `ingredient amount is formatted with one decimal and ROOT locale`() {
        val dto = SpoonacularIngredientDto(
            id = 1, name = "flour", amount = 1.5f, unit = "cups", original = "1.5 cups"
        )
        // Locale.ROOT guarantees a dot, never a comma (DE/FR would render "1,5").
        assertEquals("1.5", dto.toDomain().amount)
    }

    // ── Nutrition mapping ───────────────────────────────────────────────────────

    @Test
    fun `nutrition looks up nutrients case-insensitively and defaults missing to zero`() {
        val nutrition = SpoonacularNutritionDto(
            nutrients = listOf(
                SpoonacularNutrientDto(name = "Calories", amount = 320.7f, unit = "kcal"),
                SpoonacularNutrientDto(name = "Protein", amount = 10.2f, unit = "g")
            )
        )
        val info = recipeDto(nutrition = nutrition).toDomain().nutritionInfo!!
        assertEquals(320, info.calories)           // truncated to Int
        assertEquals(10.2f, info.protein, 0.001f)
        assertEquals(0f, info.carbs, 0.001f)        // absent → default 0
    }

    @Test
    fun `calories mirror nutrition calories on the recipe`() {
        val nutrition = SpoonacularNutritionDto(
            nutrients = listOf(SpoonacularNutrientDto("Calories", 250f, "kcal"))
        )
        assertEquals(250, recipeDto(nutrition = nutrition).toDomain().calories)
    }
}
