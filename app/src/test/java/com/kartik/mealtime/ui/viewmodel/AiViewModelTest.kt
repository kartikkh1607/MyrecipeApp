package com.kartik.mealtime.ui.viewmodel

import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.preferences.DietaryPref
import com.kartik.mealtime.data.preferences.UserPreferences
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.ChatMessage
import com.kartik.mealtime.data.remote.PremiumRequiredException
import com.kartik.mealtime.data.repository.AiRecipeRepository
import com.kartik.mealtime.domain.repository.EntitlementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for [AiViewModel].
 *
 * The ViewModel exposes Compose [androidx.compose.runtime.State] (not Flows), so we
 * read `.value` directly after driving an action. UnconfinedTestDispatcher + setMain
 * runs `viewModelScope` coroutines eagerly, so state is final once the call returns.
 */
class AiViewModelTest {

    /** Controllable in-memory AiService — far simpler than mocking suspend funcs. */
    private class FakeAiService : AiService {
        var chatResult: Result<String> = Result.success("ok")
        var recommendationsResult: Result<String> = Result.success("ok")
        var generateRecipeResult: Result<com.kartik.mealtime.domain.model.Recipe> =
            Result.failure(Exception("not stubbed"))
        var transformRecipeResult: Result<com.kartik.mealtime.domain.model.Recipe> =
            Result.failure(Exception("not stubbed"))
        var lastMessage: String? = null
        var lastDietaryPreferences: String? = null
        var lastGenerateQuery: String? = null
        var lastTransformInstruction: String? = null
        var lastTransformBaseId: String? = null

        override suspend fun sendChatMessage(
            message: String,
            conversationHistory: List<ChatMessage>,
            dietaryPreferences: String
        ): Result<String> {
            lastMessage = message
            lastDietaryPreferences = dietaryPreferences
            return chatResult
        }

        override suspend fun getRecipeRecommendations(
            favoriteRecipes: List<String>,
            dietaryPreferences: String
        ): Result<String> {
            lastDietaryPreferences = dietaryPreferences
            return recommendationsResult
        }

        override suspend fun generateRecipe(
            query: String,
            dietaryPreferences: String
        ): Result<com.kartik.mealtime.domain.model.Recipe> {
            lastGenerateQuery = query
            lastDietaryPreferences = dietaryPreferences
            return generateRecipeResult
        }

        override suspend fun transformRecipe(
            base: com.kartik.mealtime.domain.model.Recipe,
            instruction: String,
            dietaryPreferences: String
        ): Result<com.kartik.mealtime.domain.model.Recipe> {
            lastTransformBaseId = base.id
            lastTransformInstruction = instruction
            lastDietaryPreferences = dietaryPreferences
            return transformRecipeResult
        }
    }

    private lateinit var aiService: FakeAiService
    private lateinit var analytics: AnalyticsHelper
    private lateinit var userPrefsRepo: UserPreferencesRepository
    private lateinit var aiRecipeRepository: AiRecipeRepository
    private lateinit var entitlement: EntitlementRepository
    private lateinit var viewModel: AiViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        aiService = FakeAiService()
        analytics = mock(AnalyticsHelper::class.java)   // void methods → no stubbing needed
        userPrefsRepo = mock(UserPreferencesRepository::class.java)
        `when`(userPrefsRepo.preferences).thenReturn(flowOf(UserPreferences()))
        aiRecipeRepository = mock(AiRecipeRepository::class.java)
        entitlement = mock(EntitlementRepository::class.java)
        `when`(entitlement.isPremium).thenReturn(flowOf(false))
        viewModel = AiViewModel(
            aiService, analytics, userPrefsRepo, aiRecipeRepository, entitlement
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with a single welcome message`() {
        val messages = viewModel.chatState.value.messages
        assertEquals(1, messages.size)
        assertFalse(messages.first().isUser)
    }

    @Test
    fun `blank message is ignored`() = runTest {
        viewModel.sendMessage("   ")
        assertEquals(1, viewModel.chatState.value.messages.size)
        assertNull(aiService.lastMessage)
    }

    @Test
    fun `successful send appends user message and AI reply`() = runTest {
        aiService.chatResult = Result.success("Try a stir-fry tonight.")

        viewModel.sendMessage("what can I cook?")

        val state = viewModel.chatState.value
        assertEquals(3, state.messages.size)  // welcome + user + AI
        assertEquals("what can I cook?", state.messages[1].content)
        assertTrue(state.messages[1].isUser)
        assertEquals("Try a stir-fry tonight.", state.messages[2].content)
        assertFalse(state.messages[2].isUser)
        assertFalse(state.isTyping)
        assertNull(state.error)
    }

    @Test
    fun `generateRecipe is blocked for non-premium users`() = runTest {
        // entitlement.isPremium is stubbed false in setUp.
        viewModel.generateRecipe("a quick pasta")

        val state = viewModel.recipeGenState.value
        assertTrue(state is AiViewModel.RecipeGenState.Error)
        // The AI service must never be called when the gate is closed.
        assertNull(aiService.lastGenerateQuery)
    }

    @Test
    fun `generateRecipe returns a ready recipe for premium users`() = runTest {
        `when`(entitlement.isPremium).thenReturn(flowOf(true))
        val recipe = com.kartik.mealtime.domain.model.Recipe(
            id = "ai-1", name = "Quick Pasta", description = "", imageUrl = "", category = "AI"
        )
        aiService.generateRecipeResult = Result.success(recipe)

        viewModel.generateRecipe("a quick pasta")

        val state = viewModel.recipeGenState.value
        assertTrue(state is AiViewModel.RecipeGenState.Ready)
        assertEquals(recipe, (state as AiViewModel.RecipeGenState.Ready).recipe)
        assertFalse(state.saved)
        assertEquals("a quick pasta", aiService.lastGenerateQuery)
    }

    @Test
    fun `saveGeneratedRecipe persists and flips saved flag`() = runTest {
        `when`(entitlement.isPremium).thenReturn(flowOf(true))
        val recipe = com.kartik.mealtime.domain.model.Recipe(
            id = "ai-1", name = "Quick Pasta", description = "", imageUrl = "", category = "AI"
        )
        aiService.generateRecipeResult = Result.success(recipe)
        viewModel.generateRecipe("a quick pasta")

        viewModel.saveGeneratedRecipe()

        // saved flips to true only after aiRecipeRepository.save() returns, so this
        // also proves the persistence call was made.
        val state = viewModel.recipeGenState.value
        assertTrue(state is AiViewModel.RecipeGenState.Ready)
        assertTrue((state as AiViewModel.RecipeGenState.Ready).saved)
    }

    @Test
    fun `transformRecipe is blocked for non-premium users`() = runTest {
        // entitlement.isPremium is stubbed false in setUp.
        val base = com.kartik.mealtime.domain.model.Recipe(
            id = "src-1", name = "Chicken Curry", description = "", imageUrl = "", category = "Dinner"
        )

        viewModel.transformRecipe(base, "make it vegan")

        assertTrue(viewModel.recipeGenState.value is AiViewModel.RecipeGenState.Error)
        // The AI service must never be called when the gate is closed.
        assertNull(aiService.lastTransformInstruction)
    }

    @Test
    fun `transformRecipe returns a ready remixed recipe for premium users`() = runTest {
        `when`(entitlement.isPremium).thenReturn(flowOf(true))
        val base = com.kartik.mealtime.domain.model.Recipe(
            id = "src-1", name = "Chicken Curry", description = "", imageUrl = "", category = "Dinner"
        )
        val remixed = com.kartik.mealtime.domain.model.Recipe(
            id = "ai-2", name = "Tofu Curry", description = "", imageUrl = "", category = "Dinner"
        )
        aiService.transformRecipeResult = Result.success(remixed)

        viewModel.transformRecipe(base, "make it vegan")

        val state = viewModel.recipeGenState.value
        assertTrue(state is AiViewModel.RecipeGenState.Ready)
        assertEquals(remixed, (state as AiViewModel.RecipeGenState.Ready).recipe)
        assertEquals("src-1", aiService.lastTransformBaseId)
        assertEquals("make it vegan", aiService.lastTransformInstruction)
    }

    @Test
    fun `saving a remix persists it as REMIX with the source recipe id`() = runTest {
        `when`(entitlement.isPremium).thenReturn(flowOf(true))
        val base = com.kartik.mealtime.domain.model.Recipe(
            id = "src-1", name = "Chicken Curry", description = "", imageUrl = "", category = "Dinner"
        )
        val remixed = com.kartik.mealtime.domain.model.Recipe(
            id = "ai-2", name = "Tofu Curry", description = "", imageUrl = "", category = "Dinner"
        )
        aiService.transformRecipeResult = Result.success(remixed)
        viewModel.transformRecipe(base, "make it vegan")

        viewModel.saveGeneratedRecipe()

        // Concrete args only — matchers on a suspend mock corrupt sibling tests (see test memory).
        verify(aiRecipeRepository).save(remixed, AiRecipeSource.REMIX, "src-1")
    }

    @Test
    fun `failed send sets error and clears typing`() = runTest {
        aiService.chatResult = Result.failure(RuntimeException("network down"))

        viewModel.sendMessage("hi")

        val state = viewModel.chatState.value
        assertEquals("network down", state.error)
        assertFalse(state.isTyping)
        // The failed turn must not add a phantom AI bubble.
        assertEquals(2, state.messages.size)  // welcome + user only
    }

    @Test
    fun `AI reply with bold-dash pattern is parsed into suggested recipes`() = runTest {
        aiService.chatResult = Result.success(
            "Here are ideas:\n" +
                "**Veggie Pasta** - Quick weeknight dinner\n" +
                "**Greek Salad** – Fresh and light"  // note: en-dash variant
        )

        viewModel.sendMessage("ideas?")

        val suggestions = viewModel.chatState.value.messages.last().suggestedRecipes
        assertEquals(2, suggestions.size)
        assertEquals("Veggie Pasta", suggestions[0].name)
        assertEquals("Quick weeknight dinner", suggestions[0].description)
        assertEquals("Greek Salad", suggestions[1].name)
    }

    @Test
    fun `suggested recipes are capped at five`() = runTest {
        aiService.chatResult = Result.success(
            (1..8).joinToString("\n") { "**Recipe $it** - desc $it" }
        )

        viewModel.sendMessage("lots of ideas")

        assertEquals(5, viewModel.chatState.value.messages.last().suggestedRecipes.size)
    }

    @Test
    fun `plain reply yields no suggestions`() = runTest {
        aiService.chatResult = Result.success("Just preheat the oven to 180C.")

        viewModel.sendMessage("how hot?")

        assertTrue(viewModel.chatState.value.messages.last().suggestedRecipes.isEmpty())
    }

    @Test
    fun `dietary preferences are forwarded to the AI service`() = runTest {
        `when`(userPrefsRepo.preferences).thenReturn(
            flowOf(UserPreferences(dietaryPrefs = setOf(DietaryPref.VEGAN, DietaryPref.KETO)))
        )

        viewModel.sendMessage("dinner?")

        val forwarded = aiService.lastDietaryPreferences!!
        assertTrue(forwarded.contains("Vegan"))
        assertTrue(forwarded.contains("Keto"))
    }

    @Test
    fun `send within cooldown is dropped, send after cooldown is accepted`() = runTest {
        var clock = 10_000L
        viewModel.nowMs = { clock }

        viewModel.sendMessage("first")
        assertEquals("first", aiService.lastMessage)

        // 500ms later — inside the 1.5s cooldown → dropped (service not re-invoked).
        clock = 10_500L
        viewModel.sendMessage("too soon")
        assertEquals("first", aiService.lastMessage)

        // 2s after the first send — past the cooldown → accepted.
        clock = 12_000L
        viewModel.sendMessage("later")
        assertEquals("later", aiService.lastMessage)
    }

    @Test
    fun `clearChat resets to the welcome message`() = runTest {
        viewModel.sendMessage("hello")
        assertTrue(viewModel.chatState.value.messages.size > 1)

        viewModel.clearChat()

        val messages = viewModel.chatState.value.messages
        assertEquals(1, messages.size)
        assertFalse(messages.first().isUser)
    }

    @Test
    fun `generateRecipe maps a premium-required failure to an upsell event, not an error`() = runTest {
        // Premium client-side, but the proxy rejects (402) — entitlement out of sync.
        `when`(entitlement.isPremium).thenReturn(flowOf(true))
        aiService.generateRecipeResult = Result.failure(PremiumRequiredException())

        val events = mutableListOf<Unit>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.upsellEvents.toList(events)
        }

        viewModel.generateRecipe("a quick pasta")

        // The paywall is signalled and the gen sheet is dismissed — no error bubble.
        assertEquals(1, events.size)
        assertTrue(viewModel.recipeGenState.value is AiViewModel.RecipeGenState.Idle)

        collector.cancel()
    }
}
