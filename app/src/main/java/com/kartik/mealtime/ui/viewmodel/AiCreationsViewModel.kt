package com.kartik.mealtime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.repository.AiRecipeRepository
import com.kartik.mealtime.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the "AI Creations" screen — the user's saved AI-generated recipes.
 * Read-only list plus delete; recipes open in the normal detail screen via their
 * `ai-` id (routed locally by the repository).
 */
@HiltViewModel
class AiCreationsViewModel @Inject constructor(
    private val repository: AiRecipeRepository
) : ViewModel() {

    val creations: StateFlow<ImmutableList<Recipe>> = repository.creations
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
