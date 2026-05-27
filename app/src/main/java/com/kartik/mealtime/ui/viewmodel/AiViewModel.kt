package com.kartik.mealtime.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.ChatMessage
import com.kartik.mealtime.data.repository.AiRecipeRepository
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.repository.EntitlementRepository
import com.kartik.mealtime.ui.viewmodel.AiViewModel.Companion.MAX_HISTORY_MESSAGES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val aiService: AiService,
    private val analytics: AnalyticsHelper,
    private val userPrefsRepo: UserPreferencesRepository,
    private val aiRecipeRepository: AiRecipeRepository,
    private val entitlement: EntitlementRepository
) : ViewModel() {

    /** Premium gate — the UI shows an upsell instead of generating when this is false. */
    val isPremium: StateFlow<Boolean> = entitlement.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── Chat State ─────────────────────────────────────────────────────────────
    @Immutable
    data class ChatState(
        val messages: List<ChatUiMessage> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isTyping: Boolean = false  // AI is "typing" response
    )

    @Immutable
    data class ChatUiMessage(
        val id: String = java.util.UUID.randomUUID().toString(),
        val content: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val suggestedRecipes: List<SuggestedRecipe> = emptyList()
    )

    @Immutable
    data class SuggestedRecipe(
        val name: String,
        val description: String
    )

    private val _chatState = mutableStateOf(ChatState())
    val chatState: State<ChatState> = _chatState

    private val _recommendationState = mutableStateOf<RecommendationState>(RecommendationState.Idle)
    val recommendationState: State<RecommendationState> = _recommendationState

    sealed class RecommendationState {
        data object Idle : RecommendationState()
        data object Loading : RecommendationState()
        data class Success(val recommendations: List<SuggestedRecipe>) : RecommendationState()
        data class Error(val message: String) : RecommendationState()
    }

    private val conversationHistory = mutableListOf<ChatMessage>()

    /**
     * Monotonic clock for the send cooldown, overridable in tests. Defaults to
     * SystemClock.elapsedRealtime() so it's immune to wall-clock / NTP changes.
     */
    internal var nowMs: () -> Long = { android.os.SystemClock.elapsedRealtime() }

    /** Timestamp of the last accepted send; null until the first send. */
    private var lastSendMs: Long? = null

    private companion object {
        /** Minimum gap between accepted sends — blocks double-taps / rapid spam that
         *  would burn the shared Gemini key's 15-req/min free-tier quota for everyone. */
        const val SEND_COOLDOWN_MS = 1_500L

        /** Sliding window: only the most recent turns are sent to Gemini, keeping the
         *  prompt bounded so long chats don't exceed the context window or inflate cost. */
        const val MAX_HISTORY_MESSAGES = 20
    }

    init {
        // Welcome message
        _chatState.value = ChatState(
            messages = listOf(
                ChatUiMessage(
                    content = "Hi there! I'm your AI recipe assistant. Tell me what ingredients you have, what you're craving, or any cooking questions you have!",
                    isUser = false
                )
            )
        )
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        // Rate-limit: drop the send if a response is still streaming or we're inside the
        // cooldown window. The send button is also disabled while isTyping, so this is
        // defense-in-depth against double-taps and programmatic spam.
        val now = nowMs()
        val last = lastSendMs
        if (_chatState.value.isTyping || (last != null && now - last < SEND_COOLDOWN_MS)) return
        lastSendMs = now

        viewModelScope.launch {
            // Add user message
            val userMsg = ChatUiMessage(content = trimmed, isUser = true)
            _chatState.value = _chatState.value.copy(
                messages = _chatState.value.messages + userMsg,
                isTyping = true,
                error = null
            )

            conversationHistory.add(ChatMessage(content = trimmed, isUser = true))
            trimHistory()
            analytics.logAiChatMessageSent()

            // Personalization: include the user's full food profile (diet, allergies,
            // skill, servings, spice, units) so Gemini tailors suggestions accordingly.
            val personalization = userPrefsRepo.preferences.first().aiPersonalizationContext()

            // Send to AI
            val result = aiService.sendChatMessage(
                message = trimmed,
                conversationHistory = conversationHistory.map {
                    ChatMessage(content = it.content, isUser = it.isUser)
                },
                dietaryPreferences = personalization
            )

            result.fold(
                onSuccess = { response ->
                    conversationHistory.add(ChatMessage(content = response, isUser = false))
                    trimHistory()

                    val suggestions = extractRecipeSuggestions(response)

                    val aiMsg = ChatUiMessage(
                        content = response,
                        isUser = false,
                        suggestedRecipes = suggestions
                    )
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages + aiMsg,
                        isTyping = false
                    )
                },
                onFailure = { error ->
                    _chatState.value = _chatState.value.copy(
                        isTyping = false,
                        error = error.message ?: "Something went wrong. Please try again."
                    )
                }
            )
        }
    }

    fun clearChat() {
        conversationHistory.clear()
        _chatState.value = ChatState(
            messages = listOf(
                ChatUiMessage(
                    content = "Hi there! I'm your AI recipe assistant. Tell me what ingredients you have, what you're craving, or any cooking questions you have!",
                    isUser = false
                )
            )
        )
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _recommendationState.value = RecommendationState.Loading

            val favoriteNames = favoriteDao.getAllSync().map { it.name }
            val personalization = userPrefsRepo.preferences.first().aiPersonalizationContext()

            val result = aiService.getRecipeRecommendations(favoriteNames, personalization)
            result.fold(
                onSuccess = { response ->
                    val suggestions = parseRecommendations(response)
                    _recommendationState.value = RecommendationState.Success(suggestions)
                },
                onFailure = { error ->
                    _recommendationState.value = RecommendationState.Error(
                        error.message ?: "Failed to load recommendations"
                    )
                }
            )
        }
    }

    fun refreshRecommendations() {
        loadRecommendations()
    }

    // ── Full recipe generation (premium) ──────────────────────────────────────

    @Immutable
    sealed interface RecipeGenState {
        data object Idle : RecipeGenState
        data object Loading : RecipeGenState
        /** A generated recipe ready to review; [saved] flips true once persisted. */
        data class Ready(val recipe: Recipe, val saved: Boolean = false) : RecipeGenState
        data class Error(val message: String) : RecipeGenState
    }

    private val _recipeGenState = mutableStateOf<RecipeGenState>(RecipeGenState.Idle)
    val recipeGenState: State<RecipeGenState> = _recipeGenState

    /**
     * Generates a full structured recipe from [query]. Caller is responsible for
     * the premium gate (via [isPremium]); this also guards as defense-in-depth.
     */
    fun generateRecipe(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || _recipeGenState.value is RecipeGenState.Loading) return

        viewModelScope.launch {
            // Read the source of truth directly so the guard is always current
            // (the exposed isPremium StateFlow is only hot while the UI subscribes).
            if (!entitlement.isPremium.first()) {
                _recipeGenState.value = RecipeGenState.Error("Recipe generation is a premium feature.")
                return@launch
            }
            _recipeGenState.value = RecipeGenState.Loading
            analytics.logAiChatMessageSent()

            val personalization = userPrefsRepo.preferences.first().aiPersonalizationContext()
            aiService.generateRecipe(trimmed, personalization).fold(
                onSuccess = { _recipeGenState.value = RecipeGenState.Ready(it) },
                onFailure = {
                    _recipeGenState.value = RecipeGenState.Error(
                        it.message ?: "Couldn't generate a recipe. Please try again."
                    )
                }
            )
        }
    }

    /** Persists the currently-shown generated recipe into the AI Creations store. */
    fun saveGeneratedRecipe() {
        val current = _recipeGenState.value as? RecipeGenState.Ready ?: return
        if (current.saved) return
        viewModelScope.launch {
            aiRecipeRepository.save(current.recipe, AiRecipeSource.GENERATED)
            _recipeGenState.value = current.copy(saved = true)
        }
    }

    fun dismissGeneratedRecipe() {
        _recipeGenState.value = RecipeGenState.Idle
    }

    /** Caps [conversationHistory] at [MAX_HISTORY_MESSAGES], dropping the oldest turns. */
    private fun trimHistory() {
        while (conversationHistory.size > MAX_HISTORY_MESSAGES) {
            conversationHistory.removeAt(0)
        }
    }

    private fun extractRecipeSuggestions(text: String): List<SuggestedRecipe> {
        val suggestions = mutableListOf<SuggestedRecipe>()

        // Look for patterns like **Recipe Name** - Description
        val pattern = Regex("\\*\\*([^*]+)\\*\\*\\s*[-–]\\s*([^\n]+)")
        pattern.findAll(text).forEach { match ->
            suggestions.add(
                SuggestedRecipe(
                    name = match.groupValues[1].trim(),
                    description = match.groupValues[2].trim()
                )
            )
        }

        return suggestions.take(5)
    }

    private fun parseRecommendations(text: String): List<SuggestedRecipe> {
        return extractRecipeSuggestions(text).ifEmpty {
            listOf(
                SuggestedRecipe(
                    name = "Loading recommendations...",
                    description = "Please try again"
                )
            )
        }
    }

}