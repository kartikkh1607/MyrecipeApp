# MyrecipeApp Premium UI/UX Design System
## Intentional Minimalism for Culinary Excellence

---

## 1. Global Design System (The Foundation)

### Brand Identity
**Philosophy**: Intentional Minimalism - Elegant typography, refined color palette, beautiful and effortless UX
**Aesthetic**: Earthy, natural, and premium

### Color Palette

#### Light Theme (Linen Palette)
- **Background**: Linen (`0xFFFAF7F5`) - Warm, inviting off-white
- **Text Primary**: Graphite (`0xFF2E2E2E`) - Deep, soft charcoal
- **Text Secondary**: Stone (`0xFF8A8A8A`) - Neutral gray for subtitles
- **Primary Accent**: ForestGreen (`0xFF2D5A5A`) - Elegant, deep green
- **Surfaces**: White (`0xFFFFFFFF`) - Clean, pure white

#### Dark Theme (Midnight Palette)
- **Background**: Midnight (`0xFF1B1D21`) - Sophisticated dark blue-gray
- **Text Primary**: Cream (`0xFFF0EBE8`) - Soft, warm off-white
- **Text Secondary**: MutedGray (`0xFF7A7A7A`) - Gentle gray
- **Primary Accent**: Teal (`0xFF66B5B5`) - Vibrant teal for accents
- **Surfaces**: DarkSurface (`0xFF24262B`) - Lighter charcoal

### Typography Hierarchy
- **Display Large**: 32sp, Light weight, -0.5sp letter spacing
- **Headline Large**: 24sp, Medium weight, clean spacing
- **Title Large**: 16sp, Normal weight, subtle spacing
- **Body Large**: 16sp, Normal weight, generous 28sp line height
- **Label Large**: 14sp, Medium weight, 0.5sp letter spacing

---

## Component Styling Guidelines

### 1. Card Component
```kotlin
// Premium Card Styling
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp,        // Subtle elevation for elegance
        pressedElevation = 4.dp
    ),
    shape = RoundedCornerShape(12.dp),  // Refined corner radius
    border = BorderStroke(
        width = 0.5.dp,                 // Hair-thin border
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
)
```

**Design Principles:**
- Minimal elevation (2dp) for subtle depth
- 12dp corner radius for modern, soft feel
- Optional hair-thin border for definition
- Pure white/DarkSurface backgrounds

### 2. Button Components

#### Primary Button (Filled)
```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    shape = RoundedCornerShape(8.dp),   // Slightly rounded
    contentPadding = PaddingValues(
        horizontal = 24.dp,
        vertical = 16.dp
    )
) {
    Text(
        text = "Start Cooking",
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 0.5.sp
    )
}
```

#### Secondary Button (Outlined)
```kotlin
OutlinedButton(
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    ),
    border = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    ),
    shape = RoundedCornerShape(8.dp)
) {
    Text(
        text = "Save Recipe",
        style = MaterialTheme.typography.labelLarge
    )
}
```

### 3. TextField/Search Bar
```kotlin
OutlinedTextField(
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(12.dp),  // Matches card radius
    textStyle = MaterialTheme.typography.bodyMedium
)
```

### 4. Bottom Navigation Bar
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,              // Slightly elevated
    windowInsets = WindowInsets(0)
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(...) },
        label = { 
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            ) 
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    )
}
```

---

## 2. Screen-by-Screen Design Application

### HomeScreen Design
**Layout Simplification:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 20.dp)  // Generous side padding
) {
    // Integrated Header (no separate section)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Good morning, Chef",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        // Profile/Settings icon
    }
    
    // Search integrated naturally
    SearchBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    )
    
    // Featured section with minimal header
    Text(
        text = "Featured Today",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // Carousel with refined cards
    FeaturedCarousel()
    
    // Categories with chip-style design
    CategoryChips()
}
```

**Carousel Card Styling:**
- Background: Pure white with 2dp elevation
- Corner radius: 16dp for premium feel
- Image overlay: Subtle gradient (Black 0% to Black 40%)
- Typography: Light titles, Stone subtitles
- Spacing: 16dp between cards

### RecipeDetailScreen Design
**Information Hierarchy:**
```kotlin
LazyColumn(
    contentPadding = PaddingValues(bottom = 100.dp)
) {
    // Hero image with minimal overlay
    item {
        HeroImageSection(
            recipe = recipe,
            modifier = Modifier.height(300.dp)
        )
    }
    
    // Content with breathing room
    item {
        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 24.dp
            )
        ) {
            // Title with generous spacing
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Subtitle in Stone color
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
    
    // Ingredients section
    item {
        IngredientsSection(
            ingredients = recipe.ingredients,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
    
    // Instructions section
    item {
        InstructionsSection(
            steps = recipe.steps,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}
```

**Ingredients Section Styling:**
```kotlin
@Composable
fun IngredientsSection(
    ingredients: List<Ingredient>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            ingredients.forEachIndexed { index, ingredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${ingredient.amount} ${ingredient.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (index < ingredients.lastIndex) {
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
```

### CookingModeScreen Design
**Ultra-Focused Dark UI:**
```kotlin
@Composable
fun CookingModeScreen() {
    // Force dark theme for cooking mode
    MyrecipeAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Midnight)  // Force dark background
                .padding(20.dp)
        ) {
            // Minimal header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Cream
                    )
                }
                
                Text(
                    text = "Step ${currentStep + 1} of ${totalSteps}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedGray
                )
            }
            
            // Current step card - high contrast
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentStep.instruction,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Cream,
                        lineHeight = 32.sp
                    )
                }
            }
            
            // Navigation controls - teal accents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = hasPrevious,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Teal
                    ),
                    border = BorderStroke(1.dp, Teal)
                ) {
                    Text("Previous")
                }
                
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal,
                        contentColor = Midnight
                    )
                ) {
                    Text("Next Step")
                }
            }
        }
    }
}
```

### List & Grid Screens Template
**Consistent Layout for All Recipe Lists:**
```kotlin
@Composable
fun RecipeListScreen(
    title: String,
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(recipes) { recipe ->
            RecipeGridCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe) }
            )
        }
    }
}

@Composable
fun RecipeGridCard(
    recipe: Recipe,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),  // Portrait ratio
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Image section (60% of card)
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Content section (40% of card)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(12.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${recipe.totalTime} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = recipe.rating.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
```

---

## Design Principles Summary

### Spacing System
- **Macro spacing**: 20dp, 24dp, 32dp for major sections
- **Micro spacing**: 8dp, 12dp, 16dp for component internal spacing
- **Typography spacing**: Generous line heights (28sp for body text)

### Elevation System
- **Cards**: 1-2dp for subtle depth
- **Navigation**: 4dp for definition
- **Modals/Cooking mode**: 8dp for prominence
- **Floating elements**: 12dp+ for clear hierarchy

### Interactive States
- **Hover**: Subtle scale (0.98x) or elevation increase
- **Press**: Scale down to 0.95x with haptic feedback
- **Focus**: Primary color outline with 2dp width
- **Disabled**: 0.38 opacity with no interaction

### Accessibility
- **Contrast**: All text meets WCAG AA standards
- **Touch targets**: Minimum 48dp for all interactive elements
- **Focus indicators**: Clear visual focus states
- **Content descriptions**: Comprehensive for all UI elements

This design system ensures a consistent, premium, and accessible experience across all screens while maintaining the intentional minimalism philosophy.
