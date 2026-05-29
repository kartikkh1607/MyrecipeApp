package com.kartik.mealtime.ui.viewmodel

import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.repository.SignInProvider
import com.kartik.mealtime.data.repository.SyncRepository
import com.kartik.mealtime.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for [AuthViewModel] — sign-in validation and the account-deletion flow
 * (including the recent-login re-auth branch added for the Play Store deletion
 * requirement).
 *
 * The three repositories are concrete final classes (no interfaces), so they're
 * mocked with Mockito's inline mock-maker. Suspend functions are stubbed/verified
 * inside [runBlocking]. UnconfinedTestDispatcher makes viewModelScope run eagerly so
 * state is settled by the time an action returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var userRepository: UserRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var userPrefs: UserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        userRepository = mock(UserRepository::class.java)
        syncRepository = mock(SyncRepository::class.java)
        userPrefs = mock(UserPreferencesRepository::class.java)
        // Stub read during the VM's init (eager stateIn collection).
        `when`(userRepository.authState).thenReturn(flowOf(null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AuthViewModel(userRepository, syncRepository, userPrefs)

    private fun fakeUser(uid: String): FirebaseUser =
        mock(FirebaseUser::class.java).also { `when`(it.uid).thenReturn(uid) }

    // ── Sign-in validation ──────────────────────────────────────────────────────

    @Test
    fun `signIn with blank email surfaces an error and skips the repository`() {
        val vm = viewModel()
        vm.signIn("", "password")
        val state = vm.uiState.value
        assertTrue(state is AuthViewModel.AuthUiState.Error)
        assertEquals("Email cannot be empty", (state as AuthViewModel.AuthUiState.Error).message)
        runBlocking { verify(userRepository, never()).signIn("", "password") }
    }

    @Test
    fun `signIn with malformed email surfaces an invalid-email error`() {
        val vm = viewModel()
        vm.signIn("not-an-email", "password")
        val state = vm.uiState.value
        assertTrue(state is AuthViewModel.AuthUiState.Error)
        assertEquals(
            "Please enter a valid email address",
            (state as AuthViewModel.AuthUiState.Error).message
        )
    }

    @Test
    fun `signIn with short password surfaces a length error`() {
        val vm = viewModel()
        vm.signIn("user@example.com", "123")
        val state = vm.uiState.value
        assertTrue(state is AuthViewModel.AuthUiState.Error)
        assertEquals(
            "Password must be at least 6 characters",
            (state as AuthViewModel.AuthUiState.Error).message
        )
    }

    @Test
    fun `signIn success pulls synced data and ends in Success`() {
        val user = fakeUser("uid1")
        runBlocking {
            `when`(userRepository.signIn("user@example.com", "password"))
                .thenReturn(Result.success(user))
        }

        val vm = viewModel()
        vm.signIn("user@example.com", "password")

        assertTrue(vm.uiState.value is AuthViewModel.AuthUiState.Success)
        runBlocking {
            verify(syncRepository).pullFavorites("uid1")
            verify(syncRepository).pullShoppingItems("uid1")
        }
    }

    // ── Account deletion ─────────────────────────────────────────────────────────

    @Test
    fun `deleteAccount wipes data, deletes auth, and reports Success`() {
        val user = fakeUser("uid1")
        `when`(userRepository.currentUser).thenReturn(user)
        runBlocking {
            `when`(userRepository.deleteCurrentUser()).thenReturn(Result.success(Unit))
        }

        val vm = viewModel()
        vm.deleteAccount()

        assertTrue(vm.deleteState.value is AuthViewModel.DeleteAccountState.Success)
        runBlocking {
            verify(syncRepository).deleteAllUserData("uid1")
            verify(userRepository).deleteCurrentUser()
        }
    }

    @Test
    fun `deleteAccount with no signed-in user reports an error`() {
        `when`(userRepository.currentUser).thenReturn(null)

        val vm = viewModel()
        vm.deleteAccount()

        assertTrue(vm.deleteState.value is AuthViewModel.DeleteAccountState.Error)
        runBlocking { verify(syncRepository, never()).deleteAllUserData("uid1") }
    }

    @Test
    fun `deleteAccount surfaces NeedsReauth with the user's provider on recent-login`() {
        val user = fakeUser("uid1")
        `when`(userRepository.currentUser).thenReturn(user)
        `when`(userRepository.primarySignInProvider()).thenReturn(SignInProvider.PASSWORD)
        runBlocking {
            `when`(userRepository.deleteCurrentUser())
                .thenReturn(Result.failure(mock(FirebaseAuthRecentLoginRequiredException::class.java)))
        }

        val vm = viewModel()
        vm.deleteAccount()

        val state = vm.deleteState.value
        assertTrue(state is AuthViewModel.DeleteAccountState.NeedsReauth)
        assertEquals(
            SignInProvider.PASSWORD,
            (state as AuthViewModel.DeleteAccountState.NeedsReauth).provider
        )
        // Data is still wiped before the auth deletion is attempted.
        runBlocking { verify(syncRepository).deleteAllUserData("uid1") }
    }

    @Test
    fun `reauthenticateWithPasswordAndDelete finishes deletion on correct password`() {
        val user = fakeUser("uid1")
        `when`(userRepository.currentUser).thenReturn(user)
        runBlocking {
            `when`(userRepository.reauthenticateWithPassword("pw")).thenReturn(Result.success(Unit))
            `when`(userRepository.deleteCurrentUser()).thenReturn(Result.success(Unit))
        }

        val vm = viewModel()
        vm.reauthenticateWithPasswordAndDelete("pw")

        assertTrue(vm.deleteState.value is AuthViewModel.DeleteAccountState.Success)
        runBlocking { verify(userRepository).deleteCurrentUser() }
    }

    @Test
    fun `reauthenticateWithPasswordAndDelete reports an error on wrong password`() {
        runBlocking {
            `when`(userRepository.reauthenticateWithPassword("bad"))
                .thenReturn(Result.failure(RuntimeException("wrong password")))
        }

        val vm = viewModel()
        vm.reauthenticateWithPasswordAndDelete("bad")

        assertTrue(vm.deleteState.value is AuthViewModel.DeleteAccountState.Error)
        // Never reaches the auth-record deletion when re-auth fails.
        runBlocking { verify(userRepository, never()).deleteCurrentUser() }
    }
}
