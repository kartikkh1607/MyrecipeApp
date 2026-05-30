package com.kartik.mealtime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.kartik.mealtime.data.billing.BillingManager
import com.kartik.mealtime.ui.theme.Amber

/**
 * Paywall for the premium subscription. Renders the real monthly + annual base plans
 * (localized prices straight from [ProductDetails]); tapping a plan launches the Play
 * purchase flow via [onSelectPlan]. Decoupled from billing mechanics — the caller owns
 * the [BillingManager]/[com.kartik.mealtime.ui.viewmodel.BillingViewModel].
 *
 * If [productDetails] is null (still loading, or Play Billing unavailable on the device)
 * the plans area shows a loading/unavailable state instead of a dead button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpsellBottomSheet(
    title: String,
    description: String,
    perks: List<String>,
    productDetails: ProductDetails?,
    onSelectPlan: (offerToken: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val plans = remember(productDetails) { productDetails?.premiumPlans().orEmpty() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Hero header ───────────────────────────────────────────────────────
            UpsellHero(title = title, description = description)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 28.dp),
            ) {
                // ── Perks ─────────────────────────────────────────────────────────
                perks.forEach { perk ->
                    PerkRow(text = perk)
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(14.dp))

                // ── Plans ─────────────────────────────────────────────────────────
                if (plans.isEmpty()) {
                    PlansUnavailable(loading = productDetails == null)
                } else {
                    val annualSavings = annualSavingsPercent(plans)
                    plans.forEach { plan ->
                        PlanRow(
                            plan = plan,
                            savingsPercent = if (plan.basePlanId == BillingManager.BASE_PLAN_ANNUAL) annualSavings else null,
                            onClick = { onSelectPlan(plan.offerToken) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Reassurance footer ────────────────────────────────────────────
                Text(
                    text = "Cancel anytime in Google Play. Existing subscribers will be restored automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )

                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Maybe later")
                }
            }
        }
    }
}

@Composable
private fun UpsellHero(title: String, description: String) {
    // Amber → forest gradient evokes the brand's "premium / cooked-to-perfection" feel.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFB347), Amber, Color(0xFF92400E))
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun PerkRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Amber.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PlanRow(
    plan: PlanOption,
    savingsPercent: Int?,
    onClick: () -> Unit,
) {
    val highlighted = plan.basePlanId == BillingManager.BASE_PLAN_ANNUAL
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) Amber.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (highlighted) BorderStroke(1.5.dp, Amber) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (savingsPercent != null && savingsPercent > 0) {
                        Spacer(Modifier.size(8.dp))
                        SavingsBadge(savingsPercent)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Billed ${plan.periodNoun}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = plan.formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) Amber else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = plan.periodSuffix,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavingsBadge(percent: Int) {
    Surface(shape = RoundedCornerShape(8.dp), color = Amber) {
        Text(
            text = "SAVE $percent%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun PlansUnavailable(loading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Amber, modifier = Modifier.size(28.dp))
        } else {
            Text(
                text = "Subscriptions are unavailable right now. Please try again later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Plan extraction ────────────────────────────────────────────────────────────

private data class PlanOption(
    val basePlanId: String,
    val label: String,
    val periodNoun: String,     // "monthly" / "annually"
    val periodSuffix: String,   // "/mo" / "/yr"
    val formattedPrice: String, // localized, e.g. "₹149.00"
    val priceMicros: Long,      // for cross-plan savings math
    val offerToken: String,
)

/**
 * Flattens the subscription's offers into one [PlanOption] per base plan. For each base
 * plan we take the simplest offer (fewest pricing phases — i.e. the plain base plan over
 * any promo/intro offer) and read its ongoing recurring phase. Ordered monthly → annual.
 */
private fun ProductDetails.premiumPlans(): List<PlanOption> {
    val offers = subscriptionOfferDetails ?: return emptyList()
    val plans = offers
        .groupBy { it.basePlanId }
        .mapNotNull { (basePlanId, planOffers) ->
            val offer = planOffers.minByOrNull { it.pricingPhases.pricingPhaseList.size }
                ?: return@mapNotNull null
            val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
            val (label, noun, suffix) = when (basePlanId) {
                BillingManager.BASE_PLAN_MONTHLY -> Triple("Monthly", "monthly", "/mo")
                BillingManager.BASE_PLAN_ANNUAL -> Triple("Annual", "annually", "/yr")
                else -> Triple(basePlanId.replaceFirstChar { it.uppercase() }, basePlanId, "")
            }
            PlanOption(
                basePlanId = basePlanId,
                label = label,
                periodNoun = noun,
                periodSuffix = suffix,
                formattedPrice = phase.formattedPrice,
                priceMicros = phase.priceAmountMicros,
                offerToken = offer.offerToken,
            )
        }
    val order = listOf(BillingManager.BASE_PLAN_MONTHLY, BillingManager.BASE_PLAN_ANNUAL)
    return plans.sortedBy {
        order.indexOf(it.basePlanId).let { i -> if (i == -1) Int.MAX_VALUE else i }
    }
}

/** % saved by paying annually vs 12× the monthly price; null if either plan is missing. */
private fun annualSavingsPercent(plans: List<PlanOption>): Int? {
    val monthly = plans.firstOrNull { it.basePlanId == BillingManager.BASE_PLAN_MONTHLY } ?: return null
    val annual = plans.firstOrNull { it.basePlanId == BillingManager.BASE_PLAN_ANNUAL } ?: return null
    val yearlyAtMonthly = monthly.priceMicros * 12
    if (yearlyAtMonthly <= 0L || annual.priceMicros <= 0L) return null
    val saved = (1.0 - annual.priceMicros.toDouble() / yearlyAtMonthly) * 100
    return saved.toInt().coerceIn(0, 99)
}
