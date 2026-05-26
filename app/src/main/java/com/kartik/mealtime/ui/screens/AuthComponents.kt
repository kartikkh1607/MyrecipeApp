package com.kartik.mealtime.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kartik.mealtime.ui.theme.Amber
import com.kartik.mealtime.ui.theme.Success

// ── Validation helpers ─────────────────────────────────────────────────────────
// Kept identical to AuthViewModel.validate() so the "enable submit" check on screen
// never disagrees with what the ViewModel will accept.

internal fun isValidEmail(email: String): Boolean =
    email.contains("@") && email.substringAfter("@").contains(".")

/**
 * Coarse password strength on a 0..3 scale (length + character variety).
 * 0 → empty, 1 → weak, 2 → fair, 3 → strong. Not a security gate — just UX feedback.
 */
internal fun passwordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 6) score++
    if (password.length >= 10) score++
    if (password.any { it.isLetter() } && password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score.coerceIn(1, 3)
}

// ── Sliding Pill Tab Switcher ──────────────────────────────────────────────────

@Composable
internal fun PillTabSwitcher(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Sign In", "Register")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(5.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "tab_indicator"
        )

        // Sliding highlight that follows the active tab.
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(3.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, label ->
                val selected = selectedTab == index
                val textColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(250),
                    label = "tab_text_$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

// ── Custom Auth Field ─────────────────────────────────────────────────────────

@Composable
internal fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    supportingText: String? = null,
    contentType: ContentType? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val shape = RoundedCornerShape(14.dp)

    val targetBorder = when {
        isError -> MaterialTheme.colorScheme.error
        isSuccess -> Success
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderColor by animateColorAsState(targetBorder, tween(200), label = "field_border")

    val iconTint = when {
        isError -> MaterialTheme.colorScheme.error
        isSuccess -> Success
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 5.dp else 1.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "field_elevation"
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .shadow(elevation, shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, borderColor, shape)
                .padding(start = 16.dp, end = if (isPassword) 6.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = if (isPassword && !passwordVisible)
                            PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = imeAction
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { onNext?.invoke() },
                            onDone = { onDone?.invoke() }
                        ),
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onFocusChanged?.invoke(it.isFocused) }
                            .semantics {
                                if (contentType != null) this.contentType = contentType
                                if (isError && supportingText != null) error(supportingText)
                            }
                    )
                }
                // 48dp touch target (IconButton default) keeps the toggle accessible
                // even though the glyph itself stays a compact 22dp.
                if (isPassword && onTogglePassword != null) {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ── Inline helper / error text ────────────────────────────────────────
        AnimatedVisibility(visible = supportingText != null) {
            Text(
                text = supportingText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    isSuccess -> Success
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = 6.dp, top = 6.dp)
            )
        }
    }
}

// ── Password Strength Meter ─────────────────────────────────────────────────────

@Composable
internal fun PasswordStrengthMeter(password: String, modifier: Modifier = Modifier) {
    val score = remember(password) { passwordStrength(password) }
    val (label, color) = when {
        score <= 1 -> "Weak" to MaterialTheme.colorScheme.error
        score == 2 -> "Fair" to Amber
        else -> "Strong" to Success
    }

    Column(modifier = modifier.padding(start = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                val filled = index < score
                val segmentColor by animateColorAsState(
                    targetValue = if (filled) color else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(250),
                    label = "strength_segment_$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(segmentColor)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Password strength: $label",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}
