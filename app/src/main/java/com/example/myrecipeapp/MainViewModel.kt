package com.example.myrecipeapp

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    data class RecipeState(
        val loading: Boolean = true,
        val list: List<Category> = emptyList(),
        val error: String? = null
    )

    // We’re locking the real data inside the ViewModel
    // and only giving UI a copy to look at (read-only).

    private val _CategoriesState =
        mutableStateOf(RecipeState()) // private box that stores all the UI info
    val CategoriesState: State<RecipeState> = _CategoriesState

    init {
        fetchCategories()
    }

    private fun fetchCategories() {
        viewModelScope.launch {     //  Starts a coroutine (background thread
            try {
                val response = recepieService.getCategories()
                _CategoriesState.value = _CategoriesState.value.copy(
                    list = response.categories,
                    loading = false,
                    error = null
                )

            } catch (e: Exception) {
                _CategoriesState.value = _CategoriesState.value.copy(
                    loading = false,
                    error = ("error fetching Categories ${e.message}")
                )
            }
        }
    }

}