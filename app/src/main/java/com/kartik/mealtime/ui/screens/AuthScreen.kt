package com.kartik.mealtime.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kartik.mealtime.R
import com.kartik.mealtime.ui.theme.ForestGreen
import com.kartik.mealtime.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/** Which auth path is mid-flight (drives per-button spinner). */
internal enum class AuthPath { None, Email, Google }

@Composable
fun AuthScreen(
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isLoading = uiState is AuthViewModel.AuthUiState.Loading

    // Tracks which auth path is in-flight so we can show the spinner on the
    // right button. Resets to None when uiState settles to Success / Error / Info.
    var activePath by remember { mutableStateOf(AuthPath.None) }
    val emailLoading = isLoading && activePath == AuthPath.Email
    val googleLoading = isLoading && activePath == AuthPath.Google

    // ── Inline validation state ──────────────────────────────────────────────
    // A field's error stays hidden until the user has either left it once
    // (touched) or pressed submit — so we don't yell while they're mid-typing.
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var emailHadFocus by remember { mutableStateOf(false) }
    var passwordHadFocus by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }

    val emailValid = isValidEmail(email)
    val passwordValid = password.length >= 6
    val passwordsMatch = password == confirmPassword
    val nameValid = name.isNotBlank()

    val nameError: String? = when {
        submitAttempted && selectedTab == 1 && name.isBlank() -> "Name is required"
        else -> null
    }

    val emailError: String? = when {
        submitAttempted && email.isBlank() -> "Email is required"
        (emailTouched || submitAttempted) && email.isNotBlank() && !emailValid ->
            "Enter a valid email address"

        else -> null
    }
    val passwordError: String? = when {
        submitAttempted && password.isBlank() -> "Password is required"
        (passwordTouched || submitAttempted) && password.isNotBlank() && !passwordValid ->
            "Use at least 6 characters"

        else -> null
    }
    val confirmError = when {
        confirmPassword.isNotEmpty() && !passwordsMatch -> "Passwords don't match"
        submitAttempted && selectedTab == 1 && confirmPassword.isEmpty() -> "Confirm your password"
        else -> null
    }
    val confirmSuccess = confirmPassword.isNotEmpty() && passwordsMatch
    val confirmSupport = confirmError ?: if (confirmSuccess) "Passwords match" else null

    // Clears transient validation flags when switching between Sign In / Register.
    fun switchTab(tab: Int) {
        selectedTab = tab
        viewModel.resetState()
        confirmPassword = ""
        name = ""
        submitAttempted = false
        emailTouched = false
        passwordTouched = false
    }

    // Reveals any outstanding errors, then submits only when the form is valid.
    fun attemptSubmit() {
        focusManager.clearFocus()
        submitAttempted = true
        if (selectedTab == 0) {
            if (emailValid && passwordValid) {
                activePath = AuthPath.Email
                viewModel.signIn(email, password)
            }
        } else if (nameValid && emailValid && passwordValid && passwordsMatch) {
            activePath = AuthPath.Email
            viewModel.register(email, password, name)
        }
    }

    // ── Google Sign-In wiring (Credential Manager) ───────────────────────────
    // setServerClientId expects the *Web* OAuth client ID (auto-created when
    // Google sign-in is enabled in Firebase Console). The empty default in
    // strings.xml disables the button until the developer fills it in.
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.web_client_id)
    val googleEnabled = webClientId.isNotBlank()

    fun launchGoogleSignIn() {
        if (!googleEnabled) {
            viewModel.setError("Google sign-in isn't configured yet.")
            return
        }
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // show all Google accounts on device
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    activePath = AuthPath.Google
                    viewModel.signInWithGoogle(googleIdToken)
                } else {
                    viewModel.setError("Unexpected credential type returned.")
                }
            } catch (_: GetCredentialCancellationException) {
                // User dismissed the chooser — silent.
            } catch (_: NoCredentialException) {
                viewModel.setError("No Google accounts found on this device.")
            } catch (_: GoogleIdTokenParsingException) {
                viewModel.setError("Couldn't read your Google ID token. Try again.")
            } catch (e: GetCredentialException) {
                viewModel.setError(e.message ?: "Google sign-in failed.")
            }
        }
    }

    // ── Security: prevent screenshots / screen recording on the auth screen ──
    // FLAG_SECURE blocks: screenshots, screen-recording apps, app-switcher thumbnails,
    // and casting/projection. Cleared on dispose so other screens behave normally.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthViewModel.AuthUiState.Success -> {
                // Firebase emits the signed-in user → authDestination recomputes →
                // MainActivity swaps in MainScreen (verified) or VerifyEmailScreen
                // (new/unverified email account) automatically. No explicit nav needed.
                activePath = AuthPath.None
            }

            is AuthViewModel.AuthUiState.Error -> {
                activePath = AuthPath.None
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }

            is AuthViewModel.AuthUiState.Info -> {
                activePath = AuthPath.None
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }

            else -> Unit
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero Section ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(288.dp)
                    .clipToBounds()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF20403F),   // deep top
                                ForestGreen,          // brand mid
                                Color(0xFF3C6E6B)     // lighter base
                            )
                        )
                    )
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                // Soft decorative orbs for depth — clipped to the hero bounds.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-90).dp, y = (-70).dp)
                        .size(220.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 80.dp, y = (-40).dp)
                        .size(170.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Brand mark — the app's cream knife+fork glyph, inverted onto a
                    // floating white badge so it reads cleanly over the green hero.
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .shadow(18.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "MealTime",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Your personal recipe companion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // ── Form Card ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // ── Pill tab switcher ─────────────────────────────────────
                    PillTabSwitcher(
                        selectedTab = selectedTab,
                        onTabSelected = { tab -> switchTab(tab) }
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Welcome subtitle ──────────────────────────────────────
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 6 })
                                .togetherWith(fadeOut(tween(180)))
                        },
                        label = "auth_welcome"
                    ) { tab ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (tab == 0) "Welcome back!" else "Create your account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (tab == 0)
                                    "Sign in to continue cooking"
                                else
                                    "Save favorites & sync across devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Fields (animated between sign-in and register) ────────
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInVertically(tween(300)) { it / 8 } + fadeIn(tween(300)))
                                .togetherWith(slideOutVertically(tween(200)) { -it / 8 } + fadeOut(
                                    tween(200)
                                ))
                        },
                        label = "auth_fields"
                    ) { tab ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Name field — register only. Captured here so the greeting
                            // and Profile screen can show it right after sign-up.
                            AnimatedVisibility(
                                visible = tab == 1,
                                enter = slideInVertically(spring(Spring.DampingRatioMediumBouncy)) { -it / 2 } + fadeIn(),
                                exit = slideOutVertically { -it / 2 } + fadeOut()
                            ) {
                                AuthField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = "Name",
                                    leadingIcon = Icons.Default.Person,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                    isError = nameError != null,
                                    supportingText = nameError,
                                    contentType = ContentType.PersonFullName
                                )
                            }

                            AuthField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = "Email address",
                                leadingIcon = Icons.Default.Email,
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                isError = emailError != null,
                                supportingText = emailError,
                                contentType = ContentType.EmailAddress,
                                onFocusChanged = { focused ->
                                    if (focused) emailHadFocus = true
                                    else if (emailHadFocus) emailTouched = true
                                }
                            )

                            AuthField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = "Password",
                                leadingIcon = Icons.Default.Lock,
                                keyboardType = KeyboardType.Password,
                                imeAction = if (tab == 1) ImeAction.Next else ImeAction.Done,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = { if (tab == 0) attemptSubmit() },
                                isError = passwordError != null,
                                supportingText = passwordError,
                                contentType = if (tab == 1) ContentType.NewPassword else ContentType.Password,
                                onFocusChanged = { focused ->
                                    if (focused) passwordHadFocus = true
                                    else if (passwordHadFocus) passwordTouched = true
                                }
                            )

                            // Strength meter — register only, once they start typing.
                            AnimatedVisibility(
                                visible = tab == 1 && password.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                PasswordStrengthMeter(password)
                            }

                            // Confirm password only on Register tab
                            AnimatedVisibility(
                                visible = tab == 1,
                                enter = slideInVertically(spring(Spring.DampingRatioMediumBouncy)) { it / 2 } + fadeIn(),
                                exit = slideOutVertically { it / 2 } + fadeOut()
                            ) {
                                AuthField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    placeholder = "Confirm password",
                                    leadingIcon = Icons.Default.Lock,
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                    isPassword = true,
                                    passwordVisible = confirmPasswordVisible,
                                    onTogglePassword = {
                                        confirmPasswordVisible = !confirmPasswordVisible
                                    },
                                    onDone = { attemptSubmit() },
                                    isError = confirmError != null,
                                    isSuccess = confirmSuccess,
                                    supportingText = confirmSupport,
                                    contentType = ContentType.NewPassword
                                )
                            }
                        }
                    }

                    // ── Forgot password? link (Sign In tab only) ──────────────
                    AnimatedVisibility(
                        visible = selectedTab == 0,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(150))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.sendPasswordReset(email) },
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(if (selectedTab == 0) 8.dp else 24.dp))

                    // ── Primary button ────────────────────────────────────────
                    Button(
                        onClick = { attemptSubmit() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        if (emailLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (selectedTab == 0) "Sign In" else "Create Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Divider ───────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Text(
                            "  or  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Google Sign-In button ─────────────────────────────────
                    OutlinedButton(
                        onClick = { launchGoogleSignIn() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        if (googleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // ── Tab switch hint ───────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Don't have an account?" else "Already have an account?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { switchTab(if (selectedTab == 0) 1 else 0) }
                        ) {
                            Text(
                                text = if (selectedTab == 0) "Register" else "Sign In",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Pill Tab Switcher ─────────────────────────────────────────────────────────

