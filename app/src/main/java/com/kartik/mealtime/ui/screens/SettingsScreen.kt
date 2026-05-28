package com.kartik.mealtime.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.android.billingclient.api.ProductDetails
import com.kartik.mealtime.BuildConfig
import com.kartik.mealtime.R
import com.kartik.mealtime.data.billing.BillingManager
import com.kartik.mealtime.domain.model.ThemeMode
import com.kartik.mealtime.ui.components.UpsellBottomSheet
import com.kartik.mealtime.ui.components.findActivity
import com.kartik.mealtime.ui.components.rememberConsentManager
import com.kartik.mealtime.ui.navigation.LocalTabReselectEvents
import com.kartik.mealtime.ui.navigation.Profile
import com.kartik.mealtime.ui.theme.Amber
import com.kartik.mealtime.ui.viewmodel.BillingViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.filter
import com.kartik.mealtime.ui.navigation.Settings as SettingsRoute

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: MainViewModel,         // ✅ Issue #4: ViewModel needed for theme control
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    // ✅ Issue #4: observe current theme mode from DataStore
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()

    // Dev-only premium override (debug builds). Lets us exercise premium AI features
    // before Play Billing exists. Never shown in release builds.
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val productDetails by billingViewModel.productDetails.collectAsStateWithLifecycle()

    // Upgrade flow — same paywall used by the AI screens.
    var showUpsell by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        billingViewModel.purchaseEvents.collect { result ->
            when (result) {
                BillingManager.PurchaseResult.Success -> {
                    showUpsell = false
                    Toast.makeText(context, "You're Premium now — enjoy!", Toast.LENGTH_SHORT).show()
                }
                BillingManager.PurchaseResult.Cancelled -> Unit
                is BillingManager.PurchaseResult.Error ->
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Legal links — folded in from the former About screen. Each row appears only
    // when its URL is configured in strings.xml, so we never show a broken link.
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_of_service_url)

    // Ad-consent ("Privacy choices") — shown only for EEA/UK users, who must be able
    // to change their UMP consent after the initial prompt. Hidden everywhere else.
    val consentManager = rememberConsentManager()
    val privacyOptionsRequired by consentManager.privacyOptionsRequired.collectAsStateWithLifecycle()
    val activity = context.findActivity()

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

    fun openManageSubscription() {
        val appId = context.packageName.removeSuffix(".debug")
        val url = "https://play.google.com/store/account/subscriptions" +
                "?sku=${BillingManager.SUBSCRIPTION_PRODUCT_ID}&package=$appId"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            uriHandler.openUri(url)
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Personalize your MealTime experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Premium status — the first thing the user sees in Settings.
            item {
                PremiumStatusCard(
                    isPremium = isPremium,
                    productDetails = productDetails,
                    onUpgradeClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showUpsell = true
                    },
                    onManageClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        openManageSubscription()
                    }
                )
            }
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
                SettingsSection(title = "Support") {
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
            if (BuildConfig.DEBUG) {
                item {
                    SettingsSection(title = "Developer") {
                        SettingsItem(
                            icon = Icons.Default.WorkspacePremium,
                            title = if (isPremium) "Premium: ON (dev)" else "Premium: OFF (dev)",
                            subtitle = "Tap to toggle the local premium override"
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setPremiumDevOverride(!isPremium)
                        }
                    }
                }
            }
            // ── Legal ─────────────────────────────────────────────────────────────
            // Rows render only when the matching URL is configured in strings.xml.
            if (privacyUrl.isNotBlank() || termsUrl.isNotBlank() ||
                (privacyOptionsRequired && activity != null)
            ) {
                item {
                    SettingsSection(title = "Legal") {
                        if (privacyOptionsRequired && activity != null) {
                            SettingsItem(
                                Icons.Default.Shield,
                                "Privacy choices",
                                "Manage your ad consent"
                            ) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                consentManager.showPrivacyOptionsForm(activity)
                            }
                        }
                        if (privacyUrl.isNotBlank()) {
                            SettingsItem(
                                Icons.Default.PrivacyTip,
                                "Privacy Policy",
                                "How we handle your data",
                                isExternal = true
                            ) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                uriHandler.openUri(privacyUrl)
                            }
                        }
                        if (termsUrl.isNotBlank()) {
                            SettingsItem(
                                Icons.Default.Description,
                                "Terms of Service",
                                "The rules for using MealTime",
                                isExternal = true
                            ) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                uriHandler.openUri(termsUrl)
                            }
                        }
                    }
                }
            }
            // ── Version footer ──────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "MealTime",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // ── Premium upsell ────────────────────────────────────────────────────────
    if (showUpsell) {
        UpsellBottomSheet(
            title = "MealTime Premium",
            description = "Unlock every AI feature and keep MealTime ad-free.",
            perks = listOf(
                "AI Recipe Assistant — turn any idea into a full recipe",
                "Recipe Remix — adapt dishes to your diet",
                "AI Meal Planner — multi-day plans with shopping list",
                "No ads, anywhere in the app",
            ),
            productDetails = productDetails,
            onSelectPlan = { offerToken ->
                activity?.let { billingViewModel.purchase(it, offerToken) }
            },
            onDismiss = { showUpsell = false },
        )
    }
}


// ── Premium status card ───────────────────────────────────────────────────────

/**
 * Top-of-Settings card that reflects the user's entitlement. For premium users it
 * shows an "Active" badge and a Manage-subscription affordance; for free users it
 * shows the headline benefit + price, opening the paywall on tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumStatusCard(
    isPremium: Boolean,
    productDetails: ProductDetails?,
    onUpgradeClick: () -> Unit,
    onManageClick: () -> Unit,
) {
    val cheapestMonthly = remember(productDetails) { productDetails?.cheapestMonthlyPrice() }
    Surface(
        onClick = if (isPremium) onManageClick else onUpgradeClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isPremium) {
                            listOf(Color(0xFF92400E), Color(0xFFF59E0B))
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                Amber.copy(alpha = 0.18f),
                            )
                        }
                    )
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPremium) Color.White.copy(alpha = 0.20f)
                            else Amber.copy(alpha = 0.22f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isPremium) Color.White else Amber,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MealTime Premium",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPremium) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isPremium) {
                            Spacer(Modifier.width(8.dp))
                            ActiveBadge()
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            isPremium -> "All premium features unlocked"
                            cheapestMonthly != null -> "From $cheapestMonthly/mo — unlock AI features & remove ads"
                            else -> "Unlock AI features & remove ads"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPremium) Color.White.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isPremium) Icons.AutoMirrored.Filled.OpenInNew
                    else Icons.Default.ChevronRight,
                    contentDescription = if (isPremium) "Manage subscription" else "Upgrade",
                    tint = if (isPremium) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.22f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

/** Localized price of the monthly base plan (e.g. "₹149.00"), or null if unavailable. */
private fun ProductDetails.cheapestMonthlyPrice(): String? {
    val offers = subscriptionOfferDetails ?: return null
    val monthly = offers.firstOrNull { it.basePlanId == BillingManager.BASE_PLAN_MONTHLY }
        ?: return null
    return monthly.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
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
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExternal: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // In dark mode, primary-on-primaryContainer is only 1.4:1 (Teal on darker Teal).
        // Flip to solid primary + onPrimary so the icon actually reads (7.5:1).
        val isDarkScheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val tileBackground = if (isDarkScheme) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer
        val iconTint = if (isDarkScheme) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.primary

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tinted rounded-square icon tile — the premium brand accent.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tileBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(22.dp),
                    tint = iconTint
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (isExternal) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ChevronRight,
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
