package com.example.myrecipeapp.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Shared snackbar host with the app's branded styling:
 *  - inverseSurface container + inverseOnSurface text (so it stands out from the screen bg)
 *  - tertiary-colored action button (amber)
 *  - 14dp rounded corners
 *
 * Use this anywhere a [SnackbarHostState] is needed instead of inlining the styling.
 */
@Composable
fun BrandedSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.tertiary,
            shape = RoundedCornerShape(14.dp)
        )
    }
}
