package com.kartik.mealtime.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kartik.mealtime.domain.model.ThemeMode
import com.kartik.mealtime.ui.navigation.About
import com.kartik.mealtime.ui.navigation.LocalTabReselectEvents
import com.kartik.mealtime.ui.navigation.Profile
import com.kartik.mealtime.ui.navigation.Settings as SettingsRoute
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.filter

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: MainViewModel          // ✅ Issue #4: ViewModel needed for theme control
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    // ✅ Issue #4: observe current theme mode from DataStore
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()

    // Hoist list state so reselecting the Settings tab smooth-scrolls to top.
    val listState = rememberLazyListState()
    val tabReselectEvents = LocalTabReselectEvents.current
    LaunchedEffect(tabReselectEvents) {
        tabReselectEvents.events.filter { it is SettingsRoute }.collect {
            listState.animateScrollToItem(0)
        }
    }

    fun openPlayStore() {
        val appId = context.packageName.removeSuffix(".debug")
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appId")
                )
            )
        }
    }

    fun shareApp() {
        val appId = context.packageName.removeSuffix(".debug")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out MealTime — your personal recipe companion: https://play.google.com/store/apps/details?id=$appId"
            )
        }
        context.startActivity(Intent.createChooser(send, "Share app"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        var showThemeDialog by remember { mutableStateOf(false) }

        // Theme picker AlertDialog — branded styling that matches the app.
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                // Drop the default tonal tint so the dialog matches the surrounding surface.
                tonalElevation = 0.dp,
                title = {
                    Column {
                        Text(
                            "Choose theme",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pick the look that feels right.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ThemePickerRow(
                                mode = mode,
                                selected = currentTheme == mode,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsSection(title = "Preferences") {
                    // Theme row — tapping opens the AlertDialog picker
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = "${currentTheme.emoji()} ${currentTheme.label()}"
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showThemeDialog = true
                    }
                }
            }
            item {
                SettingsSection(title = "Account") {
                    SettingsItem(
                        Icons.Default.Person,
                        "Profile",
                        "Manage your profile information"
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(Profile)
                    }
                }
            }
            item {
                SettingsSection(title = "About") {
                    SettingsItem(Icons.Default.Info, "About", "App version and information") {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(About)
                    }
                    SettingsItem(Icons.Default.Star, "Rate App", "Share your feedback") {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        openPlayStore()
                    }
                    SettingsItem(
                        Icons.Default.Share,
                        "Share App",
                        "Tell your friends about this app"
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareApp()
                    }
                }
            }
        }
    }
}


// ── Reusable components ───────────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Theme picker row + swatch ─────────────────────────────────────────────────
// (Uses FQNs for compose foundation/material3 helpers so the IDE's
//  auto-import-cleanup can't strip the symbols between edits.)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerRow(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ThemeSwatch(mode)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${mode.emoji()}  ${mode.label()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (mode) {
                        ThemeMode.LIGHT -> "Bright & airy"
                        ThemeMode.DARK -> "Easy on the eyes"
                        ThemeMode.SYSTEM -> "Follow your device"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    // Brand palette swatches — direct hex so we don't need theme-color imports.
    val linen = androidx.compose.ui.graphics.Color(0xFFFAF7F5)
    val forest = androidx.compose.ui.graphics.Color(0xFF2D5A5A)
    val midnight = androidx.compose.ui.graphics.Color(0xFF1B1D21)
    val teal = androidx.compose.ui.graphics.Color(0xFF66B5B5)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            ThemeMode.LIGHT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(linen)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(forest)
                )
            }

            ThemeMode.DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(midnight)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(teal)
                )
            }

            ThemeMode.SYSTEM -> {
                // Vertically split: left half linen, right half midnight.
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(linen)
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(midnight)
                    )
                }
            }
        }
    }
}
