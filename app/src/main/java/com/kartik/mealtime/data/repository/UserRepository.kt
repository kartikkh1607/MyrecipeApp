package com.kartik.mealtime.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Which credential a re-authentication flow must collect before a sensitive op. */
enum class SignInProvider { PASSWORD, GOOGLE, OTHER }

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user!!
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await().user!!
    }

    /**
     * Sign in to Firebase using a Google ID token obtained from Credential Manager.
     * Caller flow: CredentialManager.getCredential → GoogleIdTokenCredential.idToken → this.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await().user!!
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() = auth.signOut()

    /**
     * Permanently deletes the currently signed-in Firebase Auth account. Callers must
     * delete the user's Firestore + local data first (see [SyncRepository.deleteAllUserData])
     * so nothing is orphaned. Fails with [com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException]
     * if Firebase requires a fresh sign-in before allowing the deletion.
     */
    suspend fun deleteCurrentUser(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No signed-in user")
        user.delete().await()
    }

    /** The signed-in user's primary provider, used to pick the right re-auth flow. */
    fun primarySignInProvider(): SignInProvider {
        val providers = auth.currentUser?.providerData?.map { it.providerId } ?: emptyList()
        return when {
            GoogleAuthProvider.PROVIDER_ID in providers -> SignInProvider.GOOGLE   // "google.com"
            EmailAuthProvider.PROVIDER_ID in providers -> SignInProvider.PASSWORD  // "password"
            else -> SignInProvider.OTHER
        }
    }

    /** Re-authenticates an email/password user with their password (for sensitive ops). */
    suspend fun reauthenticateWithPassword(password: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No signed-in user")
        val email = user.email ?: error("Account has no email")
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        Unit
    }

    /** Re-authenticates a Google user with a fresh ID token from Credential Manager. */
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No signed-in user")
        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }
}
