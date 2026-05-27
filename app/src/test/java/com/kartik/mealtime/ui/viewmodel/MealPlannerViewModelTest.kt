package com.kartik.mealtime.ui.viewmodel

import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.preferences.UserPreferences
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.ChatMessage
import com.kartik.mealtime.data.remote.PremiumRequiredException
import com.kartik.mealtime.data.repository.AiRecipeRepository
import com.kartik.mealtime.domain.model.MealPlan
import com.kartik.mealtime.domain.model.MealPlanDay
import com.kartik.mealtime.domain.model.PlannedMeal
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.repository.EntitlementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for [MealPlannerViewModel]. Same approach as [AiViewModelTest]: a
 * hand-written [AiService] fake, Compose State read via `.value`, and an
 * UnconfinedTestDispatcher so `viewModelScope` runs eagerly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealPlannerViewModelTest {

    private class FakeAiService : AiService {
        var mealPlanResult: Result<MealPlan> = Result.failure(Exception("not stubbed"))
        var lastDays: Int? = null
        var lastDietaryPreferences: String? = null

        override suspend fun sendChatMessage(
            message: String,
            conversationHistory: List<ChatMessage>,
            dietaryPreferences: String
        ): Result<String> = Result.success("")

        override suspend fun getRecipeRecommendations(
            favoriteRecipes: List<String>,
            dietaryPreferences: String
        ): Result<String> = Result.success("")

        override suspend fun generateRecipe(
            query: String,
            dietaryPreferences: String
        ): Result<Recipe> = Result.failure(Exception("not used"))

        override suspend fun transformRecipe(
            base: Recipe,
            instruction: String,
            dietaryPreferences: String
        ): Result<Recipe> = Result.failure(Exception("not used"))

        override suspend fun generateMealPlan(
            days: Int,
            dietaryPreferences: String,
            favoriteRecipes: List<String>
        ): Result<MealPlan> {
            lastDays = days
            lastDietaryPreferences = dietaryPreferences
            return mealPlanResult
        }
    }

    private fun samplePlan() = MealPlan(
        title = "3-Day Plan",
        days = listOf(
            MealPlanDay(
                dayNumber = 1,
                meals = listOf(
                    PlannedMeal(
                        mealType = "Breakfast",
                        recipe = Recipe(
                            id = "ai-b1", name = "Oats", description = "",
                            imageUrl = "", category = "Breakfast"
                        )
                    )
                )
            )
        )
    )

    private lateinit var aiService: FakeAiService
    private lateinit var analytics: AnalyticsHelper
    private lateinit var userPrefsRepo: UserPreferencesRepository
    private lateinit var aiRecipeRepository: AiRecipeRepository
    private lateinit var entitlement: EntitlementRepository
    private lateinit var viewModel: MealPlannerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        aiService = FakeAiService()
        analytics = mock(AnalyticsHelper::class.java)
        userPrefsRepo = mock(UserPreferencesRepository::class.java)
        `when`(userPrefsRepo.preferences).thenReturn(flowOf(UserPreferences()))
        aiRecipeRepository = mock(AiRecipeRepository::class.java)
        entitlement = mock(EntitlementRepository::class.java)
        `when`(entitlement.isPremium).thenReturn(flowOf(false))
        viewModel = MealPlannerViewModel(
            aiService, analytics, userPrefsRepo, aiRecipeRepository, entitlement
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `defaults to a 3-day plan`() {
        assertEquals(3, viewModel.selectedDays.value)
    }

    @Test
    fun `setDays clamps to the supported range`() {
        viewModel.setDays(99)
        assertEquals(7, viewModel.selectedDays.value)
        viewModel.setDays(0)
        assertEquals(1, viewModel.selectedDays.value)
    }

    @Test
    fun `generate is blocked for non-premium users`() = runTest {
        viewModel.generate()

        assertTrue(viewModel.planState.value is MealPlannerViewModel.MealPlanState.Error)
        assertNull(aiService.lastDays)  // service must not be called when gated
    }

    @Test
    fun `generate returns a ready plan for premium users and forwards the day count`() = runTest {
        `when`(entitlement.isPremium).thenReturn(flowOf(true))
        aiService.mealPlanResult = Result.success(samplePlan())
        viewModel.setDays(5)

        viewModel.generate()

        val state = viewModel.planState.value
        assertTrue(state is MealPlannerViewModel.MealPlanState.Ready)
        assertEquals("3-Day Plan", (state as MealPlannerViewModel.MealPlanState.Ready).plan.title)
        assertEquals(5, aiService.lastDays)
    }

    @Test
    fun `saving a meal persists it as GENERATED and flips saved`() = runTest {
        val recipe = Recipe(
            id = "ai-b1", name = "Oats", description = "", imageUrl = "", category = "Breakfast"
        )
        viewModel.openMeal(recipe)

        viewModel.saveMeal()

        val sheet = viewModel.mealSheet.value
        assertTrue(sheet is AiViewModel.RecipeGenState.Ready)
        assertTrue((sheet as AiViewModel.RecipeGenState.Ready).saved)
        // Concrete args only — matchers on a suspend mock corrupt sibling tests (test memory).
        verify(aiRecipeRepository).save(recipe, AiRecipeSource.GENERATED, null)
    }

    @Test
    fun `generate maps a premium-required failure to an upsell event`() = runTest {
        `when`(entitlement.isPremium).thenReturn(flowOf(true))
        aiService.mealPlanResult = Result.failure(PremiumRequiredException())

        val events = mutableListOf<Unit>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.upsellEvents.toList(events)
        }

        viewModel.generate()

        assertEquals(1, events.size)
        assertTrue(viewModel.planState.value is MealPlannerViewModel.MealPlanState.Idle)

        collector.cancel()
    }
}
