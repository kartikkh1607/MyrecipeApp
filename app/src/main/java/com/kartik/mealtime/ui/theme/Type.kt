package com.kartik.mealtime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.kartik.mealtime.R

// ── Google Fonts provider ─────────────────────────────────────────────────────
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage  = "com.google.android.gms",
    certificates     = R.array.com_google_android_gms_fonts_certs
)

// ── "Linen" type pairing ──────────────────────────────────────────────────────
// Newsreader — editorial serif for display & headline (screen titles, hero copy).
// Hanken Grotesk — clean humanist sans for titles, body, labels, and UI chrome.

private val Newsreader = FontFamily(
    Font(googleFont = GoogleFont("Newsreader"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Newsreader"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Newsreader"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Newsreader"), fontProvider = provider, weight = FontWeight.Bold),
)

private val HankenGrotesk = FontFamily(
    Font(googleFont = GoogleFont("Hanken Grotesk"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Hanken Grotesk"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Hanken Grotesk"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Hanken Grotesk"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Hanken Grotesk"), fontProvider = provider, weight = FontWeight.ExtraBold),
)

// ── Typography System ─────────────────────────────────────────────────────────
// Newsreader     → Display, Headline (screen titles, hero headings — editorial)
// Hanken Grotesk → Title, Body, Label (UI chrome, body copy, chips)
// Display tracking is gently negative (-0.01em ≈ subtle) per the Linen direction.
val Typography = Typography(

    // Display — hero sections
    displayLarge = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),

    // Headlines — screen titles and section headers (Newsreader editorial richness)
    headlineLarge = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),

    // Titles — card titles and component labels (Hanken Grotesk for readability)
    titleLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),

    // Body — main content and descriptions
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),

    // Labels — buttons, tags, and UI elements
    labelLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)
