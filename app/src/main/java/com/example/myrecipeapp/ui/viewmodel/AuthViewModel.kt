package com.example.myrecipeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrecipeapp.data.repository.SyncRepository
import com.example.myrecipeapp.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = userRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, userRepository.currentUser)

    sealed interface AuthUiState {
        data object Idle : AuthUiState
        data object Loading : AuthUiState
        data object Success : AuthUiState
        data class Error(val message: String) : AuthUiState
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.signIn(email, password)
                .onSuccess { user ->
                    syncRepository.pullFavorites(user.uid)
                    syncRepository.pullShoppingItems(user.uid)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.friendlyMessage()) }
        }
    }

    fun register(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.register(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.friendlyMessage()) }
        }
    }

    fun signOut() {
        userRepository.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun validate(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> { _uiState.value = AuthUiState.Error("Email cannot be empty"); false }
            password.length < 6 -> { _uiState.value = AuthUiState.Error("Password must be at least 6 characters"); false }
            else -> true
        }
    }

    private fun Throwable.friendlyMessage(): String = when {
        message?.contains("email address is already") == true -> "An account with this email already exists."
        message?.contains("password is invalid") == true -> "Incorrect password."
        message?.contains("no user record") == true -> "No account found with this email."
        message?.contains("network") == true -> "Network error. Check your connection."
        else -> "Something went wrong. Please try again."
    }
}
