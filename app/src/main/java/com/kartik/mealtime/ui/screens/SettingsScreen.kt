package com.kartik.mealtime.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
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
    viewModel: MainViewModel,         // âœ… Issue #4: ViewModel needed for theme control
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    // âœ… Issue #4: observe current theme mode from DataStore
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()

    // Dev-only premium override (debug builds). Lets us exercise premium AI features
    // before Play Billing exists. Never shown in release builds.
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val productDetails by billingViewModel.productDetails.collectAsStateWithLifecycle()

    // Upgrade flow â€” same paywall used by the AI screens.
    var showUpsell by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        billingViewModel.purchaseEvents.collect { result ->
            when (result) {
                BillingManager.PurchaseResult.Success -> {
                    showUpsell = false
                    Toast.makeText(context, "You're Premium now â€” enjoy!", Toast.LENGTH_SHORT)
                        .show()
                }

                BillingManager.PurchaseResult.Cancelled -> Unit
                is BillingManager.PurchaseResult.Error ->
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Legal links â€” folded in from the former About screen. Each row appears only
    // when its URL is configured in strings.xml, so we never show a broken link.
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_of_service_url)

    // Ad-consent ("Privacy choices") â€” shown only for EEA/UK users, who must be able
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
                "Check out MealTime â€” your personal recipe companion: https://play.google.com/store/apps/details?id=$appId"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Premium status â€” the first thing the user sees in Settings.
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
                SettingsSection(title = "Appearance") {
                    ThemeModeSelector(
                        current = currentTheme,
                        onSelect = { mode ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setThemeMode(mode)
                        }
                    )
                }
            }
            item {
                SettingsSection(title = "Account") {
                    SettingsItem(
                        Icons.Default.Person,
                        "Profile",
                        "Manage your profile information",
                        last = true
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
                        "Tell your friends about this app",
                        last = true
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
                            subtitle = "Tap to toggle the local premium override",
                            last = true
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setPremiumDevOverride(!isPremium)
                        }
                    }
                }
            }
            // â”€â”€ Legal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                                isExternal = true,
                                last = true
                            ) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                uriHandler.openUri(termsUrl)
                            }
                        }
                    }
                }
            }
            // â”€â”€ Version footer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    // â”€â”€ Premium upsell â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showUpsell) {
        UpsellBottomSheet(
            title = "MealTime Premium",
            description = "Unlock every AI feature and keep MealTime ad-free.",
            perks = listOf(
                "AI Recipe Assistant â€” turn any idea into a full recipe",
                "Recipe Remix â€” adapt dishes to your diet",
                "AI Meal Planner â€” multi-day plans with shopping list",
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


// â”€â”€ Premium status card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    val accent = MaterialTheme.colorScheme.tertiary // terracotta in Linen
    val accentQuiet = MaterialTheme.colorScheme.tertiaryContainer
    val onAccent = MaterialTheme.colorScheme.onTertiary

    Surface(
        onClick = if (isPremium) onManageClick else onUpgradeClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isPremium) accentQuiet else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isPremium) accent.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Accent-tinted square tile — same 34dp/radius-sm as SettingsItem
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isPremium) accent else accentQuiet),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremium) onAccent else accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Uppercase accent kicker — matches Linen "PREMIUM MEMBER" / accent labels
                if (isPremium) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = "MealTime Premium",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        isPremium -> "All premium features unlocked"
                        cheapestMonthly != null -> "From $cheapestMonthly/mo — unlock AI & remove ads"
                        else -> "Unlock AI features & remove ads"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (isPremium) Icons.AutoMirrored.Filled.OpenInNew
                else Icons.Default.ChevronRight,
                contentDescription = if (isPremium) "Manage subscription" else "Upgrade",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Localized price of the monthly base plan (e.g. "â‚¹149.00"), or null if unavailable. */
private fun ProductDetails.cheapestMonthlyPrice(): String? {
    val offers = subscriptionOfferDetails ?: return null
    val monthly = offers.firstOrNull { it.basePlanId == BillingManager.BASE_PLAN_MONTHLY }
        ?: return null
    return monthly.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
}


// â”€â”€ Reusable components â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        // Linen "Group" header — uppercase muted label, generous letter-spacing.
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        // Surface card — thin outline + soft shadow, rows flush to edges with hairlines.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            shadowElevation = 1.dp
        ) {
            Column { content() }
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
    last: Boolean = false,
    onClick: () -> Unit
) {
    // In dark mode, primary-on-primaryContainer is only 1.4:1 (Teal on darker Teal).
    // Flip to solid primary + onPrimary so the icon actually reads (7.5:1).
    val isDarkScheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tileBackground = if (isDarkScheme) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer
    val iconTint = if (isDarkScheme) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.primary

    // Row inside the shared section card; a hairline divider separates it from the
    // next row (skipped for the last item) — matches the Linen grouped-list pattern.
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tileBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (isExternal) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ChevronRight,
                null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
    if (!last) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

// ── Inline theme selector — Light / Dark / System cards (Appearance) ──────────
// Matches Linen prototype's per-mode cards: sun/moon icon + label, primaryContainer
// fill + primary border on selection. The app keeps "System" as a 3rd card since
// it's a real ThemeMode value.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val options = listOf(
        Triple(ThemeMode.LIGHT, Icons.Default.LightMode, "Light"),
        Triple(ThemeMode.DARK, Icons.Default.DarkMode, "Dark"),
        Triple(ThemeMode.SYSTEM, Icons.Default.BrightnessAuto, "System"),
    )
    Column(modifier = Modifier.padding(14.dp)) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (mode, icon, label) ->
                val selected = current == mode
                Surface(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
