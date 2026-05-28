package com.kartik.mealtime.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kartik.mealtime.R
import com.kartik.mealtime.data.repository.SignInProvider
import com.kartik.mealtime.ui.theme.ForestGreen
import com.kartik.mealtime.ui.viewmodel.AuthViewModel
import com.kartik.mealtime.ui.viewmodel.FavoritesViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import com.kartik.mealtime.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onNavigateToAuth: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteRecipes by favoritesViewModel.favoriteRecipes
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val userPrefs by userViewModel.preferences.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val deleteState by authViewModel.deleteState.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // On successful deletion the auth-state listener clears currentUser and the gate
    // re-asserts AuthScreen (same path as sign-out) — just reset the transient state.
    LaunchedEffect(deleteState) {
        if (deleteState is AuthViewModel.DeleteAccountState.Success) {
            authViewModel.resetDeleteState()
        }
    }

    // ── Inline re-auth wiring for account deletion ─────────────────────────────
    // When Firebase demands a recent login, we collect a fresh credential here and
    // retry, so the user never has to leave the screen. Google reauth reuses the same
    // Credential Manager flow as the sign-in screen.
    var reauthPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val reauthScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.web_client_id)

    fun launchGoogleReauth() {
        if (webClientId.isBlank()) {
            authViewModel.setDeleteError("Google sign-in isn't configured.")
            return
        }
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        reauthScope.launch {
            try {
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    authViewModel.reauthenticateWithGoogleAndDelete(idToken)
                } else {
                    authViewModel.setDeleteError("Unexpected credential type returned.")
                }
            } catch (_: GetCredentialCancellationException) {
                // User dismissed the chooser — stay on the confirm dialog, no error.
            } catch (_: NoCredentialException) {
                authViewModel.setDeleteError("No Google accounts found on this device.")
            } catch (_: GoogleIdTokenParsingException) {
                authViewModel.setDeleteError("Couldn't read your Google ID token. Try again.")
            } catch (e: GetCredentialException) {
                authViewModel.setDeleteError(e.message ?: "Google sign-in failed.")
            }
        }
    }

    // ── Edit profile bottom sheet ─────────────────────────────────────────────
    if (showEditSheet) {
        EditProfileSheet(
            currentPrefs = userPrefs,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                userViewModel.saveProfile(updated)
                showEditSheet = false
            }
        )
    }

    // Sign-out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out? Your data is saved and will sync when you sign back in.") },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.signOut()
                        showSignOutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Delete-account dialogs ─────────────────────────────────────────────────
    // Confirmation — destructive, irreversible, so it spells out exactly what is lost.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete account?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes your account and all synced data — your " +
                            "favorites and shopping list. This can't be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete forever") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Progress — non-dismissable while the cloud + auth deletion runs.
    if (deleteState is AuthViewModel.DeleteAccountState.Deleting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Deleting account…", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    Text("Removing your data…")
                }
            },
            confirmButton = { },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Recent-login required — collect a fresh credential inline, then finish deleting.
    // The user's data is already removed at this point; only the auth record remains.
    (deleteState as? AuthViewModel.DeleteAccountState.NeedsReauth)?.let { reauth ->
        when (reauth.provider) {
            SignInProvider.PASSWORD -> AlertDialog(
                onDismissRequest = { authViewModel.resetDeleteState(); reauthPassword = "" },
                icon = {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Confirm your password", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "For your security, re-enter your password to permanently " +
                                    "delete your account. Your synced data has already been removed."
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reauthPassword,
                            onValueChange = { reauthPassword = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pwd = reauthPassword
                            reauthPassword = ""
                            authViewModel.reauthenticateWithPasswordAndDelete(pwd)
                        },
                        enabled = reauthPassword.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete forever") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        authViewModel.resetDeleteState(); reauthPassword = ""
                    }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(20.dp)
            )

            SignInProvider.GOOGLE -> AlertDialog(
                onDismissRequest = { authViewModel.resetDeleteState() },
                icon = {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Confirm with Google", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "For your security, confirm your Google account to permanently " +
                                "delete your account. Your synced data has already been removed."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { launchGoogleReauth() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Continue with Google") }
                },
                dismissButton = {
                    TextButton(onClick = { authViewModel.resetDeleteState() }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(20.dp)
            )

            SignInProvider.OTHER -> AlertDialog(
                onDismissRequest = { authViewModel.resetDeleteState() },
                title = { Text("Please sign in again", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "For your security, sign out and sign back in, then delete your " +
                                "account again. Your synced data has already been removed."
                    )
                },
                confirmButton = {
                    Button(onClick = { authViewModel.resetDeleteState() }) { Text("OK") }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }

    (deleteState as? AuthViewModel.DeleteAccountState.Error)?.let { err ->
        AlertDialog(
            onDismissRequest = { authViewModel.resetDeleteState() },
            title = { Text("Couldn't delete account", fontWeight = FontWeight.Bold) },
            text = { Text(err.message) },
            confirmButton = {
                Button(onClick = { authViewModel.resetDeleteState() }) { Text("OK") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero Header ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ForestGreen, Color(0xFF1F4040))
                    )
                )
        ) {
            // Decorative orbs
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            )
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.BottomStart)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
            )

            // Back button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Avatar + name centered
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(
                        initialScale = 0.6f,
                        animationSpec = spring(Spring.DampingRatioLowBouncy, 280f)
                    ) + fadeIn()
                ) {
                    // Avatar circle — shows the user's chosen emoji.
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userPrefs.avatarEmoji,
                            fontSize = 44.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { 20 },
                        animationSpec = spring(stiffness = 300f)
                    ) + fadeIn()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Display name — falls back to a friendly placeholder when blank.
                        val displayName = userPrefs.displayName.ifBlank {
                            if (currentUser != null) "Welcome back" else "Hello, Chef"
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Sub-label: email if signed in, prompt otherwise.
                        if (currentUser != null) {
                            Text(
                                text = currentUser!!.email ?: "User",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        } else {
                            Text(
                                "Sign in to sync across devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Edit Profile button ────────────────────────────────────────────────
        // Pulled out of the cramped gradient header into a clear, high-contrast
        // filled button so it's unmistakable. Opens the edit bottom sheet.
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(stiffness = 300f)
            ) + fadeIn()
        ) {
            Button(
                onClick = { showEditSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Stats Row ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = spring(stiffness = 260f)
            ) + fadeIn()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFE91E63),
                    value = favoriteRecipes.size.toString(),
                    label = "Favorites"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = Color(0xFFFF6B35),
                    value = userPrefs.cookingStreakDays.toString(),
                    label = if (userPrefs.cookingStreakDays == 1) "Day streak" else "Day streak"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Visibility,
                    iconTint = ForestGreen,
                    value = userPrefs.totalRecipesViewed.toString(),
                    label = "Viewed"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Achievements ───────────────────────────────────────────────────────
        // Derived purely from the already-tracked stats — locked badges show what's
        // still to earn, giving the streak/views counters something to build toward.
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 45 },
                animationSpec = spring(stiffness = 250f)
            ) + fadeIn()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                val achievements = remember(
                    userPrefs.cookingStreakDays,
                    userPrefs.totalRecipesViewed,
                    favoriteRecipes.size
                ) {
                    buildAchievements(
                        streakDays = userPrefs.cookingStreakDays,
                        recipesViewed = userPrefs.totalRecipesViewed,
                        favorites = favoriteRecipes.size
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Achievements")
                    Text(
                        "${achievements.count { it.unlocked }} / ${achievements.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(achievements, key = { it.title }) { achievement ->
                            AchievementBadge(achievement)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Account Section ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(stiffness = 240f)
            ) + fadeIn()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionLabel("Account")
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (currentUser != null) {
                        // Signed in state
                        Column(modifier = Modifier.padding(4.dp)) {
                            ProfileMenuItem(
                                icon = Icons.Default.Person,
                                label = "Account",
                                value = currentUser!!.email ?: ""
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            ProfileMenuItem(
                                icon = Icons.Default.CheckCircle,
                                label = "Sync",
                                value = "Data synced to cloud",
                                valueColor = ForestGreen
                            )
                        }
                    } else {
                        // Guest state — prompt to sign in
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Sign in to unlock cloud sync",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Your favorites and shopping list will sync across all your devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToAuth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Sign In / Register", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Cooking profile ────────────────────────────────────────────────────
        // Compact pill summary of the cook's skill, portions, heat, and units — all
        // editable from the same sheet and all fed to the AI Chef.
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 52 },
                animationSpec = spring(stiffness = 235f)
            ) + fadeIn()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Cooking profile")
                    TextButton(onClick = { showEditSheet = true }) {
                        Text(
                            "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val servingsText =
                        if (userPrefs.defaultServings == 1) "1 serving"
                        else "${userPrefs.defaultServings} servings"
                    val cookingPills = listOf(
                        "${userPrefs.skillLevel.emoji}  ${userPrefs.skillLevel.label}",
                        "🍽️  $servingsText",
                        "${userPrefs.spiceLevel.emoji}  ${userPrefs.spiceLevel.label}",
                        "📏  ${userPrefs.unitSystem.label}"
                    )
                    LazyRow(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cookingPills, key = { it }) { pill ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    pill,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Dietary Preferences Section ───────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 55 },
                animationSpec = spring(stiffness = 230f)
            ) + fadeIn()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Dietary preferences")
                    TextButton(onClick = { showEditSheet = true }) {
                        Text(
                            "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (userPrefs.dietaryPrefs.isEmpty()) {
                        // Empty state — gentle prompt to set preferences.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No preferences set",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Tap Edit to personalize recipe suggestions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Chip cloud of selected prefs.
                        LazyRow(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userPrefs.dietaryPrefs.toList(), key = { it.name }) { pref ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(pref.emoji, fontSize = 14.sp)
                                        Text(
                                            pref.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Allergies & avoidances ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 58 },
                animationSpec = spring(stiffness = 225f)
            ) + fadeIn()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Allergies & avoidances")
                    TextButton(onClick = { showEditSheet = true }) {
                        Text(
                            "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (userPrefs.allergies.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No allergies set",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Add any and the AI Chef will avoid them",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userPrefs.allergies.toList(), key = { it.name }) { allergen ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(allergen.emoji, fontSize = 14.sp)
                                        Text(
                                            allergen.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Sign-out button ────────────────────────────────────────────────────
        if (currentUser != null) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 70 },
                    animationSpec = spring(stiffness = 200f)
                ) + fadeIn()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sign Out", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Permanent account deletion — required by Google Play for apps that
                    // let users create an account. Low-emphasis text button so it reads as
                    // the rare, destructive action it is.
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Delete account",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

