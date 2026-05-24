package com.example.myrecipeapp.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrecipeapp.data.analytics.AnalyticsHelper
import com.example.myrecipeapp.data.local.FavoriteDao
import com.example.myrecipeapp.data.preferences.UserPreferencesRepository
import com.example.myrecipeapp.data.remote.ChatMessage
import com.example.myrecipeapp.data.remote.GeminiAiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val aiService: GeminiAiService,
    private val analytics: AnalyticsHelper,
    private val userPrefsRepo: UserPreferencesRepository
) : ViewModel() {

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

        viewModelScope.launch {
            // Add user message
            val userMsg = ChatUiMessage(content = trimmed, isUser = true)
            _chatState.value = _chatState.value.copy(
                messages = _chatState.value.messages + userMsg,
                isTyping = true,
                error = null
            )

            conversationHistory.add(ChatMessage(content = trimmed, isUser = true))
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