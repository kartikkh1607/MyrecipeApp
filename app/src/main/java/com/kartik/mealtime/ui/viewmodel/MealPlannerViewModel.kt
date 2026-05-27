package com.kartik.mealtime.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.PremiumRequiredException
import com.kartik.mealtime.data.repository.AiRecipeRepository
import com.kartik.mealtime.domain.model.MealPlan
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.repository.EntitlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the premium AI meal planner. Generation reuses the structured-output
 * foundation; the per-meal review sheet reuses [AiViewModel.RecipeGenState] +
 * `GeneratedRecipeSheet`, so saving a meal behaves exactly like saving a generated
 * recipe. The plan itself is in-memory only (v1).
 */
@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val aiService: AiService,
    private val analytics: AnalyticsHelper,
    private val userPrefsRepo: UserPreferencesRepository,
    private val aiRecipeRepository: AiRecipeRepository,
    private val entitlement: EntitlementRepository,
) : ViewModel() {

    /** Premium gate — the UI shows an upsell instead of generating when this is false. */
    val isPremium: StateFlow<Boolean> = entitlement.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** One-shot signal that a premium action was attempted without entitlement (402). */
    private val _upsellEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val upsellEvents: SharedFlow<Unit> = _upsellEvents.asSharedFlow()

    @Immutable
    sealed interface MealPlanState {
        data object Idle : MealPlanState
        data object Loading : MealPlanState
        data class Ready(val plan: MealPlan) : MealPlanState
        data class Error(val message: String) : MealPlanState
    }

    private val _planState = mutableStateOf<MealPlanState>(MealPlanState.Idle)
    val planState: State<MealPlanState> = _planState

    private val _selectedDays = mutableIntStateOf(DEFAULT_DAYS)
    val selectedDays: State<Int> = _selectedDays

    /** Per-meal review sheet — reuses generation's state so GeneratedRecipeSheet just works. */
    private val _mealSheet = mutableStateOf<AiViewModel.RecipeGenState>(AiViewModel.RecipeGenState.Idle)
    val mealSheet: State<AiViewModel.RecipeGenState> = _mealSheet

    fun setDays(days: Int) {
        _selectedDays.intValue = days.coerceIn(MIN_DAYS, MAX_DAYS)
    }

    /** Generates a plan for [selectedDays] days. Premium-gated (also enforced server-side). */
    fun generate() {
        if (_planState.value is MealPlanState.Loading) return
        viewModelScope.launch {
            // Read the source of truth directly; the exposed isPremium flow is only hot
            // while the UI subscribes.
            if (!entitlement.isPremium.first()) {
                _planState.value = MealPlanState.Error("Meal planning is a premium feature.")
                return@launch
            }
            _planState.value = MealPlanState.Loading
            analytics.logAiChatMessageSent()

            val prefs = userPrefsRepo.preferences.first().aiPersonalizationContext()
            aiService.generateMealPlan(_selectedDays.intValue, prefs).fold(
                onSuccess = { _planState.value = MealPlanState.Ready(it) },
                onFailure = { error ->
                    if (error is PremiumRequiredException) {
                        _planState.value = MealPlanState.Idle
                        _upsellEvents.tryEmit(Unit)
                    } else {
                        _planState.value = MealPlanState.Error(
                            error.message ?: "Couldn't build a meal plan. Please try again."
                        )
                    }
                }
            )
        }
    }

    /** Clears a generated plan, returning to the day-picker. */
    fun reset() {
        _planState.value = MealPlanState.Idle
    }

    // ── Per-meal review sheet ──────────────────────────────────────────────────

    fun openMeal(recipe: Recipe) {
        _mealSheet.value = AiViewModel.RecipeGenState.Ready(recipe)
    }

    fun saveMeal() {
        val current = _mealSheet.value as? AiViewModel.RecipeGenState.Ready ?: return
        if (current.saved) return
        viewModelScope.launch {
            aiRecipeRepository.save(current.recipe, AiRecipeSource.GENERATED, sourceRecipeId = null)
            _mealSheet.value = current.copy(saved = true)
        }
    }

    fun dismissMeal() {
        _mealSheet.value = AiViewModel.RecipeGenState.Idle
    }

    private companion object {
        const val DEFAULT_DAYS = 3
        const val MIN_DAYS = 1
        const val MAX_DAYS = 7
    }
}
