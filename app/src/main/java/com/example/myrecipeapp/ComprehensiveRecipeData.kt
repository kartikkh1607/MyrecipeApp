package com.example.myrecipeapp

/**
 * Comprehensive Recipe Database
 * Contains thousands of recipes organized by category for demo purposes
 */
object ComprehensiveRecipeData {

    fun getRecipesByCategory(categoryId: String): List<SampleRecipe> {
        return when (categoryId) {
            "indian" -> getIndianRecipes()
            "italian" -> getItalianRecipes()
            "mexican" -> getMexicanRecipes()
            "chinese" -> getChineseRecipes()
            "continental" -> getContinentalRecipes()
            "mediterranean" -> getMediterraneanRecipes()
            "breakfast" -> getBreakfastRecipes()
            "lunch" -> getLunchRecipes()
            "dinner" -> getDinnerRecipes()
            "appetizers" -> getAppetizerRecipes()
            "desserts" -> getDessertRecipes()
            "snacks" -> getSnackRecipes()
            "vegetarian" -> getVegetarianRecipes()
            "vegan" -> getVeganRecipes()
            "keto" -> getKetoRecipes()
            "soups" -> getSoupRecipes()
            "beverages" -> getBeverageRecipes()
            else -> emptyList()
        }
    }

    fun getAllRecipes(): List<SampleRecipe> {
        return getIndianRecipes() + getItalianRecipes() + getMexicanRecipes() + 
               getChineseRecipes() + getContinentalRecipes() + getMediterraneanRecipes() +
               getBreakfastRecipes() + getLunchRecipes() + getDinnerRecipes() +
               getAppetizerRecipes() + getDessertRecipes() + getSnackRecipes() +
               getVegetarianRecipes() + getVeganRecipes() + getKetoRecipes() +
               getSoupRecipes() + getBeverageRecipes()
    }

    private fun getIndianRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "indian_001",
            name = "Butter Chicken",
            description = "Creamy tomato-based curry with tender chicken in aromatic spices",
            imageUrl = "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 45,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "indian_002",
            name = "Dal Tadka",
            description = "Comforting yellow lentils tempered with aromatic spices",
            imageUrl = "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "indian_003",
            name = "Paneer Makhani",
            description = "Rich and creamy cottage cheese curry in tomato-cashew gravy",
            imageUrl = "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 35,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "indian_004",
            name = "Biryani",
            description = "Fragrant basmati rice layered with spiced meat and saffron",
            imageUrl = "https://images.unsplash.com/photo-1563379091339-03246963d51a?w=500&h=300&fit=crop",
            rating = 4.9,
            cookTime = 90,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "indian_005",
            name = "Chole Bhature",
            description = "Spicy chickpea curry served with fluffy deep-fried bread",
            imageUrl = "https://images.unsplash.com/photo-1606491956689-2ea866880c84?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "indian_006",
            name = "Masala Dosa",
            description = "Crispy fermented crepe filled with spiced potato curry",
            imageUrl = "https://images.unsplash.com/photo-1567337712985-67da2d1bee0d?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 40,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "indian_007",
            name = "Tandoori Chicken",
            description = "Marinated chicken cooked in traditional clay oven with yogurt and spices",
            imageUrl = "https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 120,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "indian_008",
            name = "Palak Paneer",
            description = "Creamy spinach curry with soft cottage cheese cubes",
            imageUrl = "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 35,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "indian_009",
            name = "Rajma Chawal",
            description = "Red kidney beans curry served with steamed basmati rice",
            imageUrl = "https://images.unsplash.com/photo-1574653853027-5ac1dbc3e3f4?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 50,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "indian_010",
            name = "Samosa",
            description = "Crispy pastry filled with spiced potatoes and peas",
            imageUrl = "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 45,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        )
    )

    private fun getItalianRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "italian_001",
            name = "Margherita Pizza",
            description = "Classic Neapolitan pizza with fresh mozzarella, tomatoes, and basil",
            imageUrl = "https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "italian_002",
            name = "Spaghetti Carbonara",
            description = "Creamy pasta with eggs, cheese, pancetta, and black pepper",
            imageUrl = "https://images.unsplash.com/photo-1621996346565-e3dbc353d2e5?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "italian_003",
            name = "Lasagna Bolognese",
            description = "Layered pasta with rich meat sauce, béchamel, and cheese",
            imageUrl = "https://images.unsplash.com/photo-1574894709920-11b28e7367e3?w=500&h=300&fit=crop",
            rating = 4.9,
            cookTime = 90,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "italian_004",
            name = "Risotto alla Milanese",
            description = "Creamy saffron risotto with Parmesan cheese",
            imageUrl = "https://images.unsplash.com/photo-1476124369491-e7addf5db371?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 35,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "italian_005",
            name = "Fettuccine Alfredo",
            description = "Creamy pasta with butter, cream, and Parmesan cheese",
            imageUrl = "https://images.unsplash.com/photo-1563379091339-03246963d51a?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "italian_006",
            name = "Osso Buco",
            description = "Braised veal shanks with vegetables in white wine and tomatoes",
            imageUrl = "https://images.unsplash.com/photo-1565299624946-b28f40a0ca4b?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 150,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "italian_007",
            name = "Caprese Salad",
            description = "Fresh mozzarella, tomatoes, and basil with balsamic glaze",
            imageUrl = "https://images.unsplash.com/photo-1608897013039-887f21d8c804?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 10,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "italian_008",
            name = "Tiramisu",
            description = "Classic Italian dessert with coffee-soaked ladyfingers and mascarpone",
            imageUrl = "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "italian_009",
            name = "Minestrone Soup",
            description = "Hearty vegetable soup with beans, pasta, and herbs",
            imageUrl = "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 45,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "italian_010",
            name = "Gnocchi Pomodoro",
            description = "Potato dumplings in fresh tomato and basil sauce",
            imageUrl = "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        )
    )

    private fun getMexicanRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "mexican_001",
            name = "Tacos al Pastor",
            description = "Marinated pork tacos with pineapple, onions, and cilantro",
            imageUrl = "https://images.unsplash.com/photo-1565299585323-38dd9ba57e62?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "mexican_002",
            name = "Guacamole",
            description = "Fresh avocado dip with lime, onions, tomatoes, and cilantro",
            imageUrl = "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 10,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "mexican_003",
            name = "Chicken Enchiladas",
            description = "Rolled tortillas filled with chicken and topped with red sauce and cheese",
            imageUrl = "https://images.unsplash.com/photo-1599343658661-6b0f99da3aed?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 45,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "mexican_004",
            name = "Churros",
            description = "Crispy fried dough pastry rolled in cinnamon sugar",
            imageUrl = "https://images.unsplash.com/photo-1557308536-ee471ef2c390?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "mexican_005",
            name = "Pozole Rojo",
            description = "Traditional hominy soup with pork in red chile broth",
            imageUrl = "https://images.unsplash.com/photo-1567438651994-7e3b52bef5e7?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 120,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "mexican_006",
            name = "Black Bean Quesadilla",
            description = "Crispy tortilla filled with black beans, cheese, and peppers",
            imageUrl = "https://images.unsplash.com/photo-1565299585323-38dd9ba57e62?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "mexican_007",
            name = "Carne Asada",
            description = "Grilled marinated skirt steak served with tortillas and salsa",
            imageUrl = "https://images.unsplash.com/photo-1599974579688-2ca1b2ddc8f2?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "mexican_008",
            name = "Tres Leches Cake",
            description = "Sponge cake soaked in three kinds of milk with whipped cream topping",
            imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "mexican_009",
            name = "Elote",
            description = "Mexican street corn with mayo, cheese, chili powder, and lime",
            imageUrl = "https://images.unsplash.com/photo-1551024601-bec78aea704b?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "mexican_010",
            name = "Mole Poblano",
            description = "Complex sauce with chocolate and chiles served with chicken",
            imageUrl = "https://images.unsplash.com/photo-1565299585323-38dd9ba57e62?w=500&h=300&fit=crop",
            rating = 4.9,
            cookTime = 180,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        )
    )

    private fun getChineseRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "chinese_001",
            name = "Kung Pao Chicken",
            description = "Spicy stir-fried chicken with peanuts and vegetables",
            imageUrl = "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "chinese_002",
            name = "Mapo Tofu",
            description = "Spicy Sichuan tofu in fermented bean sauce",
            imageUrl = "https://images.unsplash.com/photo-1526318896980-cf78c088247c?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "chinese_003",
            name = "Sweet and Sour Pork",
            description = "Crispy pork with bell peppers in tangy sweet and sour sauce",
            imageUrl = "https://images.unsplash.com/photo-1565299624946-b28f40a0ca4b?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "chinese_004",
            name = "Beef with Broccoli",
            description = "Tender beef stir-fried with fresh broccoli in savory sauce",
            imageUrl = "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "chinese_005",
            name = "Dim Sum Dumplings",
            description = "Steamed pork and shrimp dumplings with soy dipping sauce",
            imageUrl = "https://images.unsplash.com/photo-1563379091339-03246963d51a?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 45,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "chinese_006",
            name = "Ma Po Eggplant",
            description = "Spicy Sichuan eggplant in garlic and chili oil sauce",
            imageUrl = "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "chinese_007",
            name = "General Tso's Chicken",
            description = "Crispy chicken in sweet and spicy glaze with sesame seeds",
            imageUrl = "https://images.unsplash.com/photo-1596797038530-2c107229654b?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "chinese_008",
            name = "Hot Pot",
            description = "Interactive communal dining with broth, meat, and vegetables",
            imageUrl = "https://images.unsplash.com/photo-1544562590-f3b95b055bc8?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "chinese_009",
            name = "Char Siu Bao",
            description = "Steamed barbecue pork buns with sweet and savory filling",
            imageUrl = "https://images.unsplash.com/photo-1563379091339-03246963d51a?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 90,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "chinese_010",
            name = "Fried Rice",
            description = "Wok-fried rice with eggs, vegetables, and soy sauce",
            imageUrl = "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=500&h=300&fit=crop",
            rating = 4.2,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        )
    )

    private fun getContinentalRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "continental_001",
            name = "Beef Wellington",
            description = "Tender beef fillet wrapped in puff pastry with mushroom duxelles",
            imageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500&h=300&fit=crop",
            rating = 4.9,
            cookTime = 120,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "continental_002",
            name = "Chicken Cordon Bleu",
            description = "Breaded chicken breast stuffed with ham and Swiss cheese",
            imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 40,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "continental_003",
            name = "Ratatouille",
            description = "French vegetable stew with tomatoes, eggplant, and zucchini",
            imageUrl = "https://images.unsplash.com/photo-1600891964092-4316c288032e?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 50,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "continental_004",
            name = "Coq au Vin",
            description = "Chicken braised in red wine with mushrooms and bacon",
            imageUrl = "https://images.unsplash.com/photo-1598515213692-d72b28d16471?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 90,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "continental_005",
            name = "Fish and Chips",
            description = "Beer-battered fish with crispy fries and mushy peas",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 35,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "continental_006",
            name = "Shepherd's Pie",
            description = "Ground lamb with vegetables topped with creamy mashed potatoes",
            imageUrl = "https://images.unsplash.com/photo-1574484284002-952d92456975?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 75,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "continental_007",
            name = "Quiche Lorraine",
            description = "Savory custard tart with bacon and Swiss cheese",
            imageUrl = "https://images.unsplash.com/photo-1541745537411-b8046dc6d66c?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "continental_008",
            name = "Caesar Salad",
            description = "Crisp romaine lettuce with parmesan, croutons, and caesar dressing",
            imageUrl = "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=500&h=300&fit=crop",
            rating = 4.2,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "continental_009",
            name = "Beef Bourguignon",
            description = "Slow-cooked beef stew in red wine with pearl onions and mushrooms",
            imageUrl = "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 180,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "continental_010",
            name = "Crème Brûlée",
            description = "Rich vanilla custard with caramelized sugar crust",
            imageUrl = "https://images.unsplash.com/photo-1551024506-0bccd828d307?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 90,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        )
    )

    private fun getMediterraneanRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "mediterranean_001",
            name = "Greek Moussaka",
            description = "Layered casserole with eggplant, ground meat, and béchamel sauce",
            imageUrl = "https://images.unsplash.com/photo-1544982503-9f984c14501a?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 90,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "mediterranean_002",
            name = "Hummus",
            description = "Creamy chickpea dip with tahini, garlic, and olive oil",
            imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "mediterranean_003",
            name = "Grilled Octopus",
            description = "Tender grilled octopus with lemon, olive oil, and herbs",
            imageUrl = "https://images.unsplash.com/photo-1559847844-d721426d6edc?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "mediterranean_004",
            name = "Tabbouleh",
            description = "Fresh parsley salad with tomatoes, mint, and bulgur",
            imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "mediterranean_005",
            name = "Paella Valenciana",
            description = "Spanish rice dish with saffron, chicken, rabbit, and green beans",
            imageUrl = "https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "mediterranean_006",
            name = "Falafel",
            description = "Deep-fried chickpea balls with herbs and spices",
            imageUrl = "https://images.unsplash.com/photo-1553621042-f6e147245754?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "mediterranean_007",
            name = "Spanakopita",
            description = "Greek spinach and feta cheese pie in phyllo pastry",
            imageUrl = "https://images.unsplash.com/photo-1541745537411-b8046dc6d66c?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 45,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "mediterranean_008",
            name = "Gazpacho",
            description = "Cold Spanish tomato soup with vegetables and herbs",
            imageUrl = "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=500&h=300&fit=crop",
            rating = 4.2,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "mediterranean_009",
            name = "Lamb Gyros",
            description = "Seasoned lamb in pita bread with tzatziki and vegetables",
            imageUrl = "https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 40,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "mediterranean_010",
            name = "Baklava",
            description = "Flaky phyllo pastry layered with nuts and honey syrup",
            imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 90,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        )
    )

    private fun getBreakfastRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "breakfast_001",
            name = "Pancakes",
            description = "Fluffy buttermilk pancakes with maple syrup and butter",
            imageUrl = "https://images.unsplash.com/photo-1493770348161-369560ae357d?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "breakfast_002",
            name = "Avocado Toast",
            description = "Toasted sourdough with smashed avocado, lime, and sea salt",
            imageUrl = "https://images.unsplash.com/photo-1541519227354-08fa5d50c44d?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 10,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "breakfast_003",
            name = "Eggs Benedict",
            description = "Poached eggs on English muffins with ham and hollandaise sauce",
            imageUrl = "https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "breakfast_004",
            name = "Oatmeal Bowl",
            description = "Creamy steel-cut oats with berries, nuts, and honey",
            imageUrl = "https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f?w=500&h=300&fit=crop",
            rating = 4.2,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "breakfast_005",
            name = "French Toast",
            description = "Brioche bread dipped in custard and grilled to golden perfection",
            imageUrl = "https://images.unsplash.com/photo-1586985289906-406988974504?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        )
    )

    private fun getLunchRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "lunch_001",
            name = "Club Sandwich",
            description = "Triple-layer sandwich with turkey, bacon, lettuce, and tomato",
            imageUrl = "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        ),
        SampleRecipe(
            id = "lunch_002",
            name = "Buddha Bowl",
            description = "Nutritious bowl with quinoa, roasted vegetables, and tahini dressing",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "lunch_003",
            name = "Chicken Caesar Wrap",
            description = "Grilled chicken with romaine lettuce and caesar dressing in tortilla",
            imageUrl = "https://images.unsplash.com/photo-1565299585323-38dd9ba57e62?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 20,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        )
    )

    private fun getDinnerRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "dinner_001",
            name = "Grilled Salmon",
            description = "Atlantic salmon with lemon herb butter and asparagus",
            imageUrl = "https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        ),
        SampleRecipe(
            id = "dinner_002",
            name = "Stuffed Bell Peppers",
            description = "Bell peppers stuffed with rice, ground meat, and herbs",
            imageUrl = "https://images.unsplash.com/photo-1604152135912-04a022e23696?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 60,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        )
    )

    private fun getAppetizerRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "appetizer_001",
            name = "Bruschetta",
            description = "Toasted bread topped with fresh tomatoes, garlic, and basil",
            imageUrl = "https://images.unsplash.com/photo-1541529086526-db283c563270?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        ),
        SampleRecipe(
            id = "appetizer_002",
            name = "Buffalo Wings",
            description = "Crispy chicken wings tossed in spicy buffalo sauce",
            imageUrl = "https://images.unsplash.com/photo-1594221708779-94832f4320d1?w=500&h=300&fit=crop",
            rating = 4.7,
            cookTime = 30,
            dietaryTags = listOf(DietaryFilter.NON_VEG, DietaryFilter.GLUTEN_FREE)
        )
    )

    private fun getDessertRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "dessert_001",
            name = "Chocolate Lava Cake",
            description = "Rich chocolate cake with molten center and vanilla ice cream",
            imageUrl = "https://images.unsplash.com/photo-1551024506-0bccd828d307?w=500&h=300&fit=crop",
            rating = 4.8,
            cookTime = 35,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        ),
        SampleRecipe(
            id = "dessert_002",
            name = "Cheesecake",
            description = "Creamy New York style cheesecake with berry compote",
            imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&h=300&fit=crop",
            rating = 4.6,
            cookTime = 180,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        )
    )

    private fun getSnackRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "snack_001",
            name = "Energy Balls",
            description = "No-bake protein balls with dates, nuts, and chocolate chips",
            imageUrl = "https://images.unsplash.com/photo-1621939514649-280e2ee25f60?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 15,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN)
        )
    )

    private fun getVegetarianRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "vegetarian_001",
            name = "Caprese Stuffed Portobello",
            description = "Portobello mushrooms stuffed with tomatoes, mozzarella, and basil",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 25,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.GLUTEN_FREE)
        )
    )

    private fun getVeganRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "vegan_001",
            name = "Quinoa Buddha Bowl",
            description = "Nutrient-rich bowl with quinoa, roasted vegetables, and tahini dressing",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 35,
            dietaryTags = listOf(DietaryFilter.VEGAN, DietaryFilter.GLUTEN_FREE)
        )
    )

    private fun getKetoRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "keto_001",
            name = "Keto Cauliflower Mac",
            description = "Low-carb cauliflower baked in rich cheese sauce",
            imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=500&h=300&fit=crop",
            rating = 4.3,
            cookTime = 40,
            dietaryTags = listOf(DietaryFilter.KETO, DietaryFilter.LOW_CARB, DietaryFilter.VEGETARIAN)
        )
    )

    private fun getSoupRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "soup_001",
            name = "Chicken Noodle Soup",
            description = "Comforting soup with tender chicken, vegetables, and egg noodles",
            imageUrl = "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=500&h=300&fit=crop",
            rating = 4.5,
            cookTime = 45,
            dietaryTags = listOf(DietaryFilter.NON_VEG)
        )
    )

    private fun getBeverageRecipes(): List<SampleRecipe> = listOf(
        SampleRecipe(
            id = "beverage_001",
            name = "Mango Smoothie",
            description = "Refreshing blend of mango, yogurt, and honey",
            imageUrl = "https://images.unsplash.com/photo-1544145945-f90425340c7e?w=500&h=300&fit=crop",
            rating = 4.4,
            cookTime = 5,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN)
        )
    )

    /**
     * Generate more recipes for extensive database
     * This function creates variations of base recipes to simulate a large database
     */
    fun generateExtendedRecipeList(baseRecipes: List<SampleRecipe>, targetCount: Int): List<SampleRecipe> {
        if (baseRecipes.isEmpty()) return emptyList()
        
        val extendedList = mutableListOf<SampleRecipe>()
        var currentId = 1
        
        // Add original recipes
        extendedList.addAll(baseRecipes)
        
        // Generate variations until we reach target count
        while (extendedList.size < targetCount && baseRecipes.isNotEmpty()) {
            val baseRecipe = baseRecipes.random()
            val variations = listOf(
                "Spicy", "Mild", "Grilled", "Baked", "Steamed", "Quick", "Traditional", 
                "Modern", "Classic", "Fusion", "Healthy", "Crispy", "Creamy", "Tangy"
            )
            
            val cookingMethods = listOf(
                "Air Fryer", "Instant Pot", "Slow Cooker", "Oven Baked", "Pan Fried", 
                "Grilled", "Roasted", "Sautéed"
            )
            
            val variation = variations.random()
            val method = cookingMethods.random()
            
            val newRecipe = baseRecipe.copy(
                id = "${baseRecipe.id}_var_${currentId++}",
                name = "$variation ${baseRecipe.name}",
                description = "$method ${baseRecipe.description.lowercase()}",
                rating = (40..50).random() / 10.0, // Random rating between 4.0-5.0
                cookTime = (baseRecipe.cookTime * (0.8 + Math.random() * 0.4)).toInt() // Random between 0.8x and 1.2x
            )
            
            extendedList.add(newRecipe)
        }
        
        return extendedList.take(targetCount)
    }

    /**
     * Get extensive recipe list for any category
     */
    fun getExtensiveRecipesByCategory(categoryId: String): List<SampleRecipe> {
        val baseRecipes = getRecipesByCategory(categoryId)
        val targetCount = when (categoryId) {
            "indian" -> 2850
            "italian" -> 3240
            "continental" -> 2220
            "chinese" -> 1980
            "mexican" -> 1850
            "mediterranean" -> 1760
            "breakfast" -> 1180
            "lunch" -> 1450
            "dinner" -> 1680
            "appetizers" -> 890
            "desserts" -> 1120
            "snacks" -> 640
            "vegetarian" -> 2200
            "vegan" -> 1580
            "keto" -> 750
            "soups" -> 520
            "beverages" -> 420
            else -> 100
        }
        
        return generateExtendedRecipeList(baseRecipes, targetCount)
    }

    /**
     * Convert SampleRecipe to full Recipe with ingredients and instructions
     */
    fun sampleRecipeToFullRecipe(sampleRecipe: SampleRecipe): Recipe {
        return Recipe(
            id = sampleRecipe.id,
            name = sampleRecipe.name,
            description = sampleRecipe.description,
            imageUrl = sampleRecipe.imageUrl,
            category = getCategoryNameById(sampleRecipe.id),
            cuisine = getCuisineFromId(sampleRecipe.id),
            difficulty = RecipeDifficulty.MEDIUM,
            prepTime = (sampleRecipe.cookTime * 0.3).toInt(),
            cookTime = sampleRecipe.cookTime,
            servings = 4,
            rating = sampleRecipe.rating.toFloat(),
            reviewCount = (50..500).random(),
            calories = (200..800).random(),
            ingredients = generateIngredientsForRecipe(sampleRecipe),
            instructions = generateInstructionsForRecipe(sampleRecipe),
            tags = generateTagsForRecipe(sampleRecipe),
            nutritionInfo = generateNutritionInfo((200..800).random()),
            isVegetarian = sampleRecipe.dietaryTags.contains(DietaryFilter.VEGETARIAN),
            isVegan = sampleRecipe.dietaryTags.contains(DietaryFilter.VEGAN),
            isGlutenFree = sampleRecipe.dietaryTags.contains(DietaryFilter.GLUTEN_FREE),
            isDairyFree = sampleRecipe.dietaryTags.contains(DietaryFilter.DAIRY_FREE),
            isKeto = sampleRecipe.dietaryTags.contains(DietaryFilter.KETO),
            isLowCarb = sampleRecipe.dietaryTags.contains(DietaryFilter.LOW_CARB)
        )
    }

    private fun getCategoryNameById(recipeId: String): String {
        return when {
            recipeId.startsWith("indian_") -> "Indian"
            recipeId.startsWith("italian_") -> "Italian"
            recipeId.startsWith("mexican_") -> "Mexican"
            recipeId.startsWith("chinese_") -> "Chinese"
            recipeId.startsWith("continental_") -> "Continental"
            recipeId.startsWith("mediterranean_") -> "Mediterranean"
            recipeId.startsWith("breakfast_") -> "Breakfast"
            recipeId.startsWith("lunch_") -> "Lunch"
            recipeId.startsWith("dinner_") -> "Dinner"
            recipeId.startsWith("appetizer_") -> "Appetizers"
            recipeId.startsWith("dessert_") -> "Desserts"
            recipeId.startsWith("snack_") -> "Snacks"
            else -> "International"
        }
    }

    private fun getCuisineFromId(recipeId: String): String {
        return when {
            recipeId.startsWith("indian_") -> "Indian"
            recipeId.startsWith("italian_") -> "Italian"
            recipeId.startsWith("mexican_") -> "Mexican"
            recipeId.startsWith("chinese_") -> "Chinese"
            recipeId.startsWith("mediterranean_") -> "Mediterranean"
            else -> "International"
        }
    }

    private fun generateIngredientsForRecipe(recipe: SampleRecipe): List<Ingredient> {
        val commonIngredients = listOf(
            Ingredient("1", "Onion", "1", "medium", "diced"),
            Ingredient("2", "Garlic", "3", "cloves", "minced"),
            Ingredient("3", "Olive Oil", "2", "tbsp"),
            Ingredient("4", "Salt", "1", "tsp", "to taste"),
            Ingredient("5", "Black Pepper", "1/2", "tsp", "freshly ground")
        )
        
        val specificIngredients = when {
            recipe.id.contains("chicken") -> listOf(
                Ingredient("6", "Chicken", "500", "g", "boneless, cut into pieces"),
                Ingredient("7", "Ginger", "1", "inch", "grated")
            )
            recipe.id.contains("pasta") || recipe.id.contains("spaghetti") -> listOf(
                Ingredient("6", "Pasta", "300", "g"),
                Ingredient("7", "Parmesan Cheese", "50", "g", "grated")
            )
            recipe.id.contains("rice") || recipe.id.contains("biryani") -> listOf(
                Ingredient("6", "Basmati Rice", "2", "cups"),
                Ingredient("7", "Bay Leaves", "2", "pieces")
            )
            else -> listOf(
                Ingredient("6", "Main Ingredient", "300", "g"),
                Ingredient("7", "Spices", "1", "tsp", "mixed")
            )
        }
        
        return commonIngredients + specificIngredients
    }

    private fun generateInstructionsForRecipe(recipe: SampleRecipe): List<RecipeStep> {
        return listOf(
            RecipeStep(
                stepNumber = 1,
                instruction = "Prepare all ingredients by washing, chopping, and measuring them.",
                duration = (recipe.cookTime * 0.15).toInt(),
                tips = "Mise en place - having everything ready makes cooking smoother."
            ),
            RecipeStep(
                stepNumber = 2,
                instruction = "Heat oil in a large pan over medium heat. Add onions and garlic, sauté until fragrant.",
                duration = (recipe.cookTime * 0.2).toInt()
            ),
            RecipeStep(
                stepNumber = 3,
                instruction = "Add main ingredients and cook according to recipe requirements, stirring occasionally.",
                duration = (recipe.cookTime * 0.5).toInt(),
                tips = "Adjust heat as needed to prevent burning."
            ),
            RecipeStep(
                stepNumber = 4,
                instruction = "Season with salt, pepper, and other spices. Cook until done.",
                duration = (recipe.cookTime * 0.15).toInt()
            ),
            RecipeStep(
                stepNumber = 5,
                instruction = "Serve hot with your choice of accompaniments. Enjoy!",
                duration = 2,
                tips = "Garnish with fresh herbs for extra flavor."
            )
        )
    }

    private fun generateTagsForRecipe(recipe: SampleRecipe): List<String> {
        val baseTags = mutableListOf<String>()
        
        if (recipe.dietaryTags.contains(DietaryFilter.VEGETARIAN)) baseTags.add("Vegetarian")
        if (recipe.dietaryTags.contains(DietaryFilter.VEGAN)) baseTags.add("Vegan")
        if (recipe.dietaryTags.contains(DietaryFilter.GLUTEN_FREE)) baseTags.add("Gluten-Free")
        if (recipe.dietaryTags.contains(DietaryFilter.KETO)) baseTags.add("Keto")
        
        when {
            recipe.cookTime <= 30 -> baseTags.add("Quick")
            recipe.cookTime > 60 -> baseTags.add("Slow-cooked")
        }
        
        when {
            recipe.id.contains("spicy") || recipe.id.contains("hot") -> baseTags.add("Spicy")
            recipe.id.contains("sweet") -> baseTags.add("Sweet")
            recipe.id.contains("healthy") -> baseTags.add("Healthy")
        }
        
        return baseTags
    }

    private fun generateNutritionInfo(calories: Int): NutritionInfo {
        return NutritionInfo(
            calories = calories,
            protein = (calories * 0.15f / 4), // Approximate protein calculation
            carbs = (calories * 0.5f / 4), // Approximate carbs calculation
            fat = (calories * 0.35f / 9), // Approximate fat calculation
            fiber = (5..15).random().toFloat(),
            sugar = (2..20).random().toFloat(),
            sodium = (300..800).random().toFloat()
        )
    }
}
