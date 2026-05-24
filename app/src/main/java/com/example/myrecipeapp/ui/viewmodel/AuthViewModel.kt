package com.example.myrecipeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrecipeapp.data.preferences.UserPreferencesRepository
import com.example.myrecipeapp.data.repository.SyncRepository
import com.example.myrecipeapp.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = userRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, userRepository.currentUser)

    /** True when user already passed the gate (signed in OR tapped "Maybe later"). */
    val authGateSeen: StateFlow<Boolean> = userPreferencesRepository.authGateSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Drives the first-launch (and post-sign-out) auth gate in MainActivity.
     * Show the AuthScreen iff the user is signed out AND has not yet skipped.
     */
    val needsAuthGate: StateFlow<Boolean> = combine(currentUser, authGateSeen) { user, seen ->
        user == null && !seen
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun markAuthGateSeen() {
        viewModelScope.launch { userPreferencesRepository.setAuthGateSeen(true) }
    }

    sealed interface AuthUiState {
        data object Idle : AuthUiState
        data object Loading : AuthUiState
        data object Success : AuthUiState
        data class Error(val message: String) : AuthUiState
        /** Transient positive feedback (e.g. "Password reset sent"). Screen shows snackbar then resets to Idle. */
        data class Info(val message: String) : AuthUiState
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

    /**
     * Exchange a Google ID token (from Credential Manager) for a Firebase session.
     * The screen layer is responsible for surfacing the Google account picker — this
     * method only handles the final Firebase exchange + sync.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.signInWithGoogleIdToken(idToken)
                .onSuccess { user ->
                    syncRepository.pullFavorites(user.uid)
                    syncRepository.pullShoppingItems(user.uid)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(it.friendlyMessage()) }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = AuthUiState.Error("Enter your email above first")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.sendPasswordReset(email.trim())
                .onSuccess { _uiState.value = AuthUiState.Info("Password reset link sent to your email") }
                .onFailure { _uiState.value = AuthUiState.Error(it.friendlyMessage()) }
        }
    }

    fun signOut() {
        userRepository.signOut()
        _uiState.value = AuthUiState.Idle
        // Reset the gate so the user is greeted with AuthScreen again next launch
        // (matches the documented "Reset to false on sign-out" contract).
        viewModelScope.launch { userPreferencesRepository.setAuthGateSeen(false) }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun setError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    private fun validate(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = AuthUiState.Error("Email cannot be empty")
                false
            }
            !email.contains("@") || !email.substringAfter("@").contains(".") -> {
                _uiState.value = AuthUiState.Error("Please enter a valid email address")
                false
            }
            password.length < 6 -> {
                _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
                false
            }
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
