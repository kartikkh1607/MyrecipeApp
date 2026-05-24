package com.kartik.mealtime.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.kartik.mealtime.data.ads.AdConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive-width banner ad. Picks the optimal banner height for the device
 * width — Google recommends this over fixed [AdSize.BANNER] for fill-rate.
 *
 * AdView is destroyed on composable disposal to prevent memory leaks.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val adWidthPx = configuration.screenWidthDp

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidthPx)
                )
                adUnitId = AdConfig.bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { adView -> adView.destroy() }
    )
}
