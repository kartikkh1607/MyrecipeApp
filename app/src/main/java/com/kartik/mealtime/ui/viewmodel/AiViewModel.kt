package com.kartik.mealtime.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.ChatMessage
import com.kartik.mealtime.ui.viewmodel.AiViewModel.Companion.MAX_HISTORY_MESSAGES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val aiService: AiService,
    private val analytics: AnalyticsHelper,
    private val userPrefsRepo: UserPreferencesRepository
) : ViewModel() {

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

            // Personalization: include the user's dietary prefs so Gemini
            // filters suggestions accordingly. "" if user hasn't set any.
            val dietaryPrefs = userPrefsRepo.preferences.first().dietaryPrefs
                .joinToString(", ") { it.label }

            // Send to AI
            val result = aiService.sendChatMessage(
                message = trimmed,
                conversationHistory = conversationHistory.map {
                    ChatMessage(content = it.content, isUser = it.isUser)
                },
                dietaryPreferences = dietaryPrefs
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
            val dietaryPrefs = userPrefsRepo.preferences.first().dietaryPrefs
                .joinToString(", ") { it.label }

            val result = aiService.getRecipeRecommendations(favoriteNames, dietaryPrefs)
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