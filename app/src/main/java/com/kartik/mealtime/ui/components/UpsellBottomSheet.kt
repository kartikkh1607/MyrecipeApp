package com.kartik.mealtime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            perks.forEach { perk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = perk,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

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

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Maybe later")
            }
        }
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
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) Amber.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (highlighted) BorderStroke(1.5.dp, Amber) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (savingsPercent != null && savingsPercent > 0) {
                        Spacer(Modifier.size(8.dp))
                        SavingsBadge(savingsPercent)
                    }
                }
                Text(
                    text = "Billed ${plan.periodNoun}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${plan.formattedPrice} ${plan.periodSuffix}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) Amber else MaterialTheme.colorScheme.onSurface,
            )
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
