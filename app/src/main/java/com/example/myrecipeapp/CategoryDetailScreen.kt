package com.example.myrecipeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: RecipeCategory,
    onBackClick: () -> Unit = {},
    onRecipeClick: (String) -> Unit = {}
) {
    var selectedDietaryFilter by remember { mutableStateOf(DietaryFilter.ALL) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    
    // Get comprehensive recipes for this category
    val allRecipes = remember { ComprehensiveRecipeData.getExtensiveRecipesByCategory(category.id) }
    val filteredRecipes = if (selectedDietaryFilter == DietaryFilter.ALL) {
        allRecipes
    } else {
        allRecipes.filter { it.dietaryTags.contains(selectedDietaryFilter) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with hero image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Filter button
            IconButton(
                onClick = { showFilterBottomSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = Color.White
                )
            }
            
            // Category info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                // Recipe count badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${filteredRecipes.size} Recipes",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Recipes list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active filter indicator
            if (selectedDietaryFilter != DietaryFilter.ALL) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Showing ${selectedDietaryFilter.name.lowercase().replaceFirstChar { it.uppercase() }} recipes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { selectedDietaryFilter = DietaryFilter.ALL }
                            ) {
                                Text("Clear Filter")
                            }
                        }
                    }
                }
            }
            
            items(filteredRecipes) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) }
                )
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false }
        ) {
            DietaryFilterBottomSheet(
                selectedFilter = selectedDietaryFilter,
                onFilterSelected = { filter ->
                    selectedDietaryFilter = filter
                    showFilterBottomSheet = false
                },
                availableFilters = category.dietaryTags + DietaryFilter.ALL
            )
        }
    }
}

@Composable
fun RecipeCard(
    recipe: SampleRecipe,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = recipe.rating.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    Text(
                        text = "${recipe.cookTime} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun DietaryFilterBottomSheet(
    selectedFilter: DietaryFilter,
    onFilterSelected: (DietaryFilter) -> Unit,
    availableFilters: List<DietaryFilter>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Filter by Dietary Preference",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableFilters.distinct()) { filter ->
                FilterChip(
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                DietaryFilter.ALL -> "All"
                                DietaryFilter.NON_VEG -> "Non-Veg"
                                else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
                            }
                        )
                    },
                    selected = selectedFilter == filter
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Sample recipe data class(
data class SampleRecipe(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val rating: Double,
    val cookTime: Int,
    val dietaryTags: List<DietaryFilter>
)

// Sample recipes for different categories
fun getSampleRecipesForCategory(categoryId: String): List<SampleRecipe> {
    return when (categoryId) {
        "indian" -> listOf(
            SampleRecipe(
                id = "butter_chicken",
                name = "Butter Chicken",
                description = "Creamy and rich chicken curry with aromatic spices",
                imageUrl = "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=400",
                rating = 4.8,
                cookTime = 45,
                dietaryTags = listOf(DietaryFilter.NON_VEG)
            ),
            SampleRecipe(
                id = "dal_tadka",
                name = "Dal Tadka",
                description = "Comforting lentil curry with tempered spices",
                imageUrl = "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400",
                rating = 4.5,
                cookTime = 30,
                dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
            )
        )
        "italian" -> listOf(
            SampleRecipe(
                id = "margherita_pizza",
                name = "Margherita Pizza",
                description = "Classic pizza with fresh mozzarella, tomatoes, and basil",
                imageUrl = "https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?w=400",
                rating = 4.7,
                cookTime = 25,
                dietaryTags = listOf(DietaryFilter.VEGETARIAN)
            ),
            SampleRecipe(
                id = "carbonara",
                name = "Spaghetti Carbonara",
                description = "Creamy pasta with eggs, cheese, and pancetta",
                imageUrl = "https://images.unsplash.com/photo-1621996346565-e3dbc353d2e5?w=400",
                rating = 4.6,
                cookTime = 20,
                dietaryTags = listOf(DietaryFilter.NON_VEG)
            )
        )
        else -> listOf(
            SampleRecipe(
                id = "sample_1",
                name = "Delicious Recipe",
                description = "A wonderful recipe from this category",
                imageUrl = "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=400",
                rating = 4.3,
                cookTime = 35,
                dietaryTags = listOf(DietaryFilter.VEGETARIAN)
            )
        )
    }
}

