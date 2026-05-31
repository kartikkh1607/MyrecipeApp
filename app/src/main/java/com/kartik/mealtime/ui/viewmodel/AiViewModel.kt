package com.kartik.mealtime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.ChatMessage
import com.kartik.mealtime.data.remote.PremiumRequiredException
import com.kartik.mealtime.data.remote.QuotaExceededException
import com.kartik.mealtime.data.repository.AiRecipeRepository
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.repository.EntitlementRepository
import com.kartik.mealtime.ui.viewmodel.AiViewModel.Companion.MAX_HISTORY_MESSAGES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
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
    data class ChatState(
        val messages: List<ChatUiMessage> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isTyping: Boolean = false  // AI is "typing" response
    )

    data class ChatUiMessage(
        val id: String = java.util.UUID.randomUUID().toString(),
        val content: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val suggestedRecipes: List<SuggestedRecipe> = emptyList()
    )

    data class SuggestedRecipe(
        val name: String,
        val description: String
    )

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    /**
     * One-shot signal that the user tried a premium action without entitlement (the proxy
     * returned 402). The screen collects this to open the upsell paywall.
     */
    private val _upsellEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val upsellEvents: SharedFlow<Unit> = _upsellEvents.asSharedFlow()

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
                    // Free-tier daily cap → upsell (same psychological wall as a premium gate).
                    // Premium-tier cap is genuinely "wait for midnight UTC" — keep the message.
                    if (error is QuotaExceededException && error.tier == "free") {
                        _chatState.value = _chatState.value.copy(isTyping = false)
                        _upsellEvents.tryEmit(Unit)
                    } else {
                        _chatState.value = _chatState.value.copy(
                            isTyping = false,
                            error = error.message ?: "Something went wrong. Please try again."
                        )
                    }
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

    // ── Full recipe generation (premium) ──────────────────────────────────────

    sealed interface RecipeGenState {
        data object Idle : RecipeGenState
        data object Loading : RecipeGenState
        /** A generated recipe ready to review; [saved] flips true once persisted. */
        data class Ready(val recipe: Recipe, val saved: Boolean = false) : RecipeGenState
        data class Error(val message: String) : RecipeGenState
    }

    private val _recipeGenState = MutableStateFlow<RecipeGenState>(RecipeGenState.Idle)
    val recipeGenState: StateFlow<RecipeGenState> = _recipeGenState.asStateFlow()

    /**
     * How the recipe currently in [recipeGenState] was produced, so [saveGeneratedRecipe]
     * tags it correctly in AI Creations. Set by [generateRecipe] (GENERATED) and
     * [transformRecipe] (REMIX, with the source recipe's id).
     */
    private var pendingSource: AiRecipeSource = AiRecipeSource.GENERATED
    private var pendingSourceId: String? = null

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
            pendingSource = AiRecipeSource.GENERATED
            pendingSourceId = null
            analytics.logAiChatMessageSent()

            val personalization = userPrefsRepo.preferences.first().aiPersonalizationContext()
            aiService.generateRecipe(trimmed, personalization).fold(
                onSuccess = { _recipeGenState.value = RecipeGenState.Ready(it) },
                onFailure = { error ->
                    // A 402 means entitlement lapsed/out-of-sync — show the upsell, not an error.
                    // A free-tier quota error here is theoretically impossible (server gates premium
                    // before checking quota) but we route it to the upsell for defense-in-depth.
                    if (error is PremiumRequiredException ||
                        (error is QuotaExceededException && error.tier == "free")
                    ) {
                        _recipeGenState.value = RecipeGenState.Idle
                        _upsellEvents.tryEmit(Unit)
                    } else {
                        _recipeGenState.value = RecipeGenState.Error(
                            error.message ?: "Couldn't generate a recipe. Please try again."
                        )
                    }
                }
            )
        }
    }

    /**
     * Remixes [base] per a free-text [instruction] (e.g. "make it vegan", "double the
     * servings") into a new recipe, surfaced through the same [recipeGenState]/
     * [GeneratedRecipeSheet] as generation. Premium-gated like [generateRecipe].
     */
    fun transformRecipe(base: Recipe, instruction: String) {
        val trimmed = instruction.trim()
        if (trimmed.isBlank() || _recipeGenState.value is RecipeGenState.Loading) return

        viewModelScope.launch {
            if (!entitlement.isPremium.first()) {
                _recipeGenState.value = RecipeGenState.Error("Recipe remix is a premium feature.")
                return@launch
            }
            _recipeGenState.value = RecipeGenState.Loading
            pendingSource = AiRecipeSource.REMIX
            pendingSourceId = base.id
            analytics.logAiChatMessageSent()

            val personalization = userPrefsRepo.preferences.first().aiPersonalizationContext()
            aiService.transformRecipe(base, trimmed, personalization).fold(
                onSuccess = { _recipeGenState.value = RecipeGenState.Ready(it) },
                onFailure = { error ->
                    if (error is PremiumRequiredException ||
                        (error is QuotaExceededException && error.tier == "free")
                    ) {
                        _recipeGenState.value = RecipeGenState.Idle
                        _upsellEvents.tryEmit(Unit)
                    } else {
                        _recipeGenState.value = RecipeGenState.Error(
                            error.message ?: "Couldn't remix this recipe. Please try again."
                        )
                    }
                }
            )
        }
    }

    /** Persists the currently-shown generated/remixed recipe into the AI Creations store. */
    fun saveGeneratedRecipe() {
        val current = _recipeGenState.value as? RecipeGenState.Ready ?: return
        if (current.saved) return
        viewModelScope.launch {
            aiRecipeRepository.save(current.recipe, pendingSource, pendingSourceId)
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

}