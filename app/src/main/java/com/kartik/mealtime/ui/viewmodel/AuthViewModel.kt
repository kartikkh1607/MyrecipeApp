package com.kartik.mealtime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.repository.SignInProvider
import com.kartik.mealtime.data.repository.SyncRepository
import com.kartik.mealtime.data.repository.UserRepository
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
                .onSuccess {
                    _uiState.value = AuthUiState.Info("Password reset link sent to your email")
                }
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

    // ── Account deletion ────────────────────────────────────────────────────────

    sealed interface DeleteAccountState {
        data object Idle : DeleteAccountState
        data object Deleting : DeleteAccountState
        data object Success : DeleteAccountState

        /**
         * Firebase needs a fresh sign-in before it will delete the account. [provider]
         * tells the UI which credential to collect (password vs. Google token). The
         * user's data is already deleted at this point — only the auth record remains.
         */
        data class NeedsReauth(val provider: SignInProvider) : DeleteAccountState
        data class Error(val message: String) : DeleteAccountState
    }

    private val _deleteState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteState: StateFlow<DeleteAccountState> = _deleteState.asStateFlow()

    /**
     * Permanently deletes the user's account. Order matters: we wipe the cloud + local
     * data FIRST (so no PII is orphaned in Firestore), then delete the Firebase Auth
     * record. If Firebase requires a recent login, we surface [DeleteAccountState.NeedsReauth]
     * so the UI can ask the user to sign in again and retry — the data is already gone.
     * On success the auth-state listener clears [currentUser], which (with the gate reset)
     * re-asserts the AuthScreen automatically, exactly like sign-out.
     */
    fun deleteAccount() {
        val uid = userRepository.currentUser?.uid
        if (uid == null) {
            _deleteState.value = DeleteAccountState.Error("You're not signed in.")
            return
        }
        viewModelScope.launch {
            _deleteState.value = DeleteAccountState.Deleting

            val dataResult = runCatching { syncRepository.deleteAllUserData(uid) }
            if (dataResult.isFailure) {
                _deleteState.value = DeleteAccountState.Error(
                    "Couldn't delete your data. Check your connection and try again."
                )
                return@launch
            }

            userRepository.deleteCurrentUser()
                .onSuccess {
                    userPreferencesRepository.setAuthGateSeen(false)
                    _deleteState.value = DeleteAccountState.Success
                }
                .onFailure { e ->
                    _deleteState.value =
                        if (e is FirebaseAuthRecentLoginRequiredException)
                            DeleteAccountState.NeedsReauth(userRepository.primarySignInProvider())
                        else DeleteAccountState.Error("Couldn't delete your account. Please try again.")
                }
        }
    }

    /**
     * Re-auth path for email/password users who hit [DeleteAccountState.NeedsReauth].
     * Verifies the password, then finishes deleting the (already data-wiped) account.
     */
    fun reauthenticateWithPasswordAndDelete(password: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteAccountState.Deleting
            userRepository.reauthenticateWithPassword(password)
                .onSuccess { finishAuthDeletion() }
                .onFailure {
                    _deleteState.value = DeleteAccountState.Error("Incorrect password. Please try again.")
                }
        }
    }

    /** Re-auth path for Google users — [idToken] is a fresh token from Credential Manager. */
    fun reauthenticateWithGoogleAndDelete(idToken: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteAccountState.Deleting
            userRepository.reauthenticateWithGoogle(idToken)
                .onSuccess { finishAuthDeletion() }
                .onFailure {
                    _deleteState.value =
                        DeleteAccountState.Error("Couldn't verify your Google account. Please try again.")
                }
        }
    }

    /** Deletes the Auth record after a successful re-auth. Data was wiped in the first pass. */
    private suspend fun finishAuthDeletion() {
        userRepository.deleteCurrentUser()
            .onSuccess {
                userPreferencesRepository.setAuthGateSeen(false)
                _deleteState.value = DeleteAccountState.Success
            }
            .onFailure {
                _deleteState.value = DeleteAccountState.Error("Couldn't delete your account. Please try again.")
            }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteAccountState.Idle
    }

    /** Surfaces a deletion error from the UI layer (e.g. a Google chooser failure). */
    fun setDeleteError(message: String) {
        _deleteState.value = DeleteAccountState.Error(message)
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
