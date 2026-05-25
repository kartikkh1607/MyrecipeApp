package com.kartik.mealtime.ui.viewmodel

import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.preferences.DietaryPref
import com.kartik.mealtime.data.preferences.UserPreferences
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
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
        var lastMessage: String? = null
        var lastDietaryPreferences: String? = null

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
    }

    private lateinit var aiService: FakeAiService
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var analytics: AnalyticsHelper
    private lateinit var userPrefsRepo: UserPreferencesRepository
    private lateinit var viewModel: AiViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        aiService = FakeAiService()
        favoriteDao = mock(FavoriteDao::class.java)
        analytics = mock(AnalyticsHelper::class.java)   // void methods → no stubbing needed
        userPrefsRepo = mock(UserPreferencesRepository::class.java)
        `when`(userPrefsRepo.preferences).thenReturn(flowOf(UserPreferences()))
        viewModel = AiViewModel(favoriteDao, aiService, analytics, userPrefsRepo)
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
    fun `clearChat resets to the welcome message`() = runTest {
        viewModel.sendMessage("hello")
        assertTrue(viewModel.chatState.value.messages.size > 1)

        viewModel.clearChat()

        val messages = viewModel.chatState.value.messages
        assertEquals(1, messages.size)
        assertFalse(messages.first().isUser)
    }

    @Test
    fun `loadRecommendations success exposes parsed recommendations`() = runTest {
        `when`(favoriteDao.getAllSync()).thenReturn(emptyList())
        aiService.recommendationsResult = Result.success("**Tofu Bowl** - High protein")

        viewModel.loadRecommendations()

        val state = viewModel.recommendationState.value
        assertTrue(state is AiViewModel.RecommendationState.Success)
        assertEquals(
            "Tofu Bowl",
            (state as AiViewModel.RecommendationState.Success).recommendations.first().name
        )
    }

    @Test
    fun `loadRecommendations failure exposes error state`() = runTest {
        `when`(favoriteDao.getAllSync()).thenReturn(emptyList())
        aiService.recommendationsResult = Result.failure(RuntimeException("quota exceeded"))

        viewModel.loadRecommendations()

        val state = viewModel.recommendationState.value
        assertTrue(state is AiViewModel.RecommendationState.Error)
        assertEquals("quota exceeded", (state as AiViewModel.RecommendationState.Error).message)
    }
}
