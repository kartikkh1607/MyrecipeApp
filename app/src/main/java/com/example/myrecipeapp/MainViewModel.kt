package com.example.myrecipeapp

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // --- STATE FOR THE HOME SCREEN ---
    data class HomeRecipeState(
        val loading: Boolean = true,
        val featuredRecipes: List<FeaturedRecipe> = emptyList(),
        val error: String? = null
    )
    private val _homeRecipeState = mutableStateOf(HomeRecipeState())
    val homeRecipeState: State<HomeRecipeState> = _homeRecipeState

    // --- STATE FOR A SINGLE RECIPE DETAIL ---
    data class RecipeDetailState(
        val loading: Boolean = true,
        val recipe: Recipe? = null,
        val error: String? = null
    )
    private val _recipeDetailState = mutableStateOf(RecipeDetailState())
    val recipeDetailState: State<RecipeDetailState> = _recipeDetailState

    // --- STATE FOR RECIPE CATEGORIES (from Spoonacular API) ---
    data class RecipeCategoryState(
        val loading: Boolean = true,
        val categories: List<RecipeCategory> = emptyList(),
        val error: String? = null
    )
    private val _recipeCategoriesState = mutableStateOf(RecipeCategoryState())
    val recipeCategoriesState: State<RecipeCategoryState> = _recipeCategoriesState

    // --- STATE FOR SEARCH RESULTS ---
    data class SearchState(
        val loading: Boolean = false,
        val recipes: List<Recipe> = emptyList(),
        val error: String? = null,
        val query: String = ""
    )
    private val _searchState = mutableStateOf(SearchState())
    val searchState: State<SearchState> = _searchState

    init {
        // Fetch all necessary data when the ViewModel is first created.
        fetchFeaturedRecipes()
        fetchRecipeCategories()
    }

    /**
     * Fetches featured recipes for the home screen carousel.
     * Falls back to sample data if Spoonacular API is unavailable or not configured.
     */
    private fun fetchFeaturedRecipes() {
        viewModelScope.launch {
            try {
                _homeRecipeState.value = _homeRecipeState.value.copy(loading = true, error = null)

                // Check if we have a valid API key configured
                if (isSpoonacularApiConfigured()) {
                    Log.d("MainViewModel", "Fetching recipes from Spoonacular API")
                    
                    // Make the network call to Spoonacular to get 5 random recipes
                    val response = spoonacularApiService.getRandomRecipes(number = 5)

                    // Use the adapter to convert the raw API models into our app's clean Recipe models
                    val recipes = response.recipes.map { it.toRecipe() }

                    // Create the final list of FeaturedRecipe objects for the UI
                    val featured = recipes.mapIndexed { index, recipe ->
                        FeaturedRecipe(
                            recipe = recipe,
                            type = when (index) {
                                0 -> FeaturedType.RECIPE_OF_THE_DAY
                                1 -> FeaturedType.POPULAR_THIS_WEEK
                                else -> FeaturedType.QUICK_MEALS
                            },
                            subtitle = if (recipe.category.isNotEmpty()) recipe.category else "New Discovery",
                            badgeText = if (recipe.category.isNotEmpty()) recipe.category.uppercase() else "NEW",
                            gradientColors = listOf("#FF6B6B", "#FF8E53")
                        )
                    }

                    // Update the state with the new data
                    _homeRecipeState.value = _homeRecipeState.value.copy(loading = false, featuredRecipes = featured)
                } else {
                    // Fallback to sample data
                    Log.d("MainViewModel", "Using sample data (Spoonacular API not configured)")
                    useSampleFeaturedRecipes()
                }

            } catch (e: Exception) {
                Log.w("MainViewModel", "Error fetching from Spoonacular API: ${e.message}")
                Log.d("MainViewModel", "Falling back to sample data")
                // Fallback to sample data on any API error
                useSampleFeaturedRecipes()
            }
        }
    }

    /**
     * Use sample featured recipes as fallback
     */
    private fun useSampleFeaturedRecipes() {
        val sampleFeatured = SampleData.getFeaturedRecipes()
        _homeRecipeState.value = _homeRecipeState.value.copy(
            loading = false, 
            featuredRecipes = sampleFeatured,
            error = null
        )
    }

    /**
     * Check if Spoonacular API is properly configured
     */
    private fun isSpoonacularApiConfigured(): Boolean {
        // This checks if the API key has been replaced with a real one
        // In production, you might want to store the API key in BuildConfig or a secure location
        return false // Set to true when you have a valid Spoonacular API key
    }

    /**
     * Fetches detailed information for a single recipe by its ID.
     * Falls back to sample data if API is unavailable.
     */
    fun fetchRecipeDetails(recipeId: String) {
        viewModelScope.launch {
            try {
                _recipeDetailState.value = _recipeDetailState.value.copy(loading = true, error = null)

                if (isSpoonacularApiConfigured()) {
                    // Try to fetch from Spoonacular API
                    val response = spoonacularApiService.getRecipeInformation(id = recipeId.toInt())
                    val recipe = response.toRecipe()
                    _recipeDetailState.value = _recipeDetailState.value.copy(loading = false, recipe = recipe)
                } else {
                    // Use sample data
                    val sampleRecipe = getSampleRecipeById(recipeId)
                    _recipeDetailState.value = _recipeDetailState.value.copy(
                        loading = false, 
                        recipe = sampleRecipe
                    )
                }

            } catch (e: Exception) {
                Log.w("MainViewModel", "Error fetching recipe details: ${e.message}")
                // Fallback to sample data
                val sampleRecipe = getSampleRecipeById(recipeId)
                _recipeDetailState.value = _recipeDetailState.value.copy(
                    loading = false, 
                    recipe = sampleRecipe
                )
            }
        }
    }

    /**
     * Get sample recipe by ID for fallback
     */
    private fun getSampleRecipeById(recipeId: String): Recipe? {
        return SampleData.getFeaturedRecipes()
            .map { it.recipe }
            .find { it.id == recipeId }
    }

    /**
     * Search recipes with query
     */
    fun searchRecipes(query: String) {
        viewModelScope.launch {
            try {
                _searchState.value = _searchState.value.copy(loading = true, error = null, query = query)

                if (isSpoonacularApiConfigured() && query.isNotEmpty()) {
                    // Search using Spoonacular API
                    val response = spoonacularApiService.searchRecipes(query = query, number = 20)
                    val recipes = response.results.map { it.toRecipe() }
                    _searchState.value = _searchState.value.copy(loading = false, recipes = recipes)
                } else {
                    // Use filtered sample data
                    val sampleRecipes = SampleData.getFeaturedRecipes()
                        .map { it.recipe }
                        .filter { 
                            it.name.contains(query, ignoreCase = true) ||
                            it.category.contains(query, ignoreCase = true) ||
                            it.cuisine.contains(query, ignoreCase = true)
                        }
                    _searchState.value = _searchState.value.copy(loading = false, recipes = sampleRecipes)
                }

            } catch (e: Exception) {
                Log.w("MainViewModel", "Error searching recipes: ${e.message}")
                _searchState.value = _searchState.value.copy(
                    loading = false, 
                    error = "Failed to search recipes"
                )
            }
        }
    }


    /**
     * Fetches recipe categories using modern Spoonacular-based approach.
     * Uses sample data as these categories work well with Spoonacular API.
     */
    private fun fetchRecipeCategories() {
        viewModelScope.launch {
            try {
                _recipeCategoriesState.value = _recipeCategoriesState.value.copy(loading = true, error = null)
                
                // Use our curated recipe categories that work well with Spoonacular
                val categories = RecipeCategorySamples.getRecipeCategories()
                
                _recipeCategoriesState.value = _recipeCategoriesState.value.copy(
                    categories = categories,
                    loading = false,
                    error = null
                )
                
                Log.d("MainViewModel", "Loaded ${categories.size} recipe categories")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading recipe categories: ${e.message}")
                _recipeCategoriesState.value = _recipeCategoriesState.value.copy(
                    loading = false,
                    error = "Failed to load recipe categories: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get recipes by category using Spoonacular API
     */
    fun getRecipesByCategory(categoryId: String, limit: Int = 20) {
        viewModelScope.launch {
            try {
                val category = RecipeCategorySamples.getCategoryById(categoryId)
                if (category == null) {
                    Log.w("MainViewModel", "Category not found: $categoryId")
                    return@launch
                }
                
                _searchState.value = _searchState.value.copy(loading = true, error = null)
                
                if (isSpoonacularApiConfigured()) {
                    // Search for recipes in this category using Spoonacular API
                    val response = spoonacularApiService.searchRecipes(
                        query = "",
                        type = category.name.lowercase(),
                        number = limit
                    )
                    val recipes = response.results.map { it.toRecipe() }
                    _searchState.value = _searchState.value.copy(loading = false, recipes = recipes)
                } else {
                    // Use sample data filtered by category
                    val sampleRecipes = SampleData.getFeaturedRecipes()
                        .map { it.recipe }
                        .filter { it.category.equals(category.name, ignoreCase = true) }
                        .take(limit)
                    
                    _searchState.value = _searchState.value.copy(loading = false, recipes = sampleRecipes)
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching recipes for category $categoryId: ${e.message}")
                _searchState.value = _searchState.value.copy(
                    loading = false,
                    error = "Failed to load recipes for this category"
                )
            }
        }
    }
}

