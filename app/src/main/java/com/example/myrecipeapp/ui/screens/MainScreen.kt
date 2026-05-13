package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myrecipeapp.ui.navigation.Categories
import com.example.myrecipeapp.ui.navigation.Favorites
import com.example.myrecipeapp.ui.navigation.Home
import com.example.myrecipeapp.ui.navigation.LocalTabReselectEvents
import com.example.myrecipeapp.ui.navigation.Navigation
import com.example.myrecipeapp.ui.navigation.Search
import com.example.myrecipeapp.ui.navigation.Settings
import com.example.myrecipeapp.ui.navigation.TabReselectEvents
import com.example.myrecipeapp.ui.viewmodel.AuthViewModel
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    // Only show the bottom nav on the 5 main tab destinations.
    // Any detail / full-screen route gets a hidden nav bar.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val tabRoutes = remember {
        listOf(
            Home::class,
            Categories::class,
            Search::class,
            Favorites::class,
            Settings::class,
        )
    }
    val showBottomBar = tabRoutes.any { routeClass ->
        currentDestination?.hasRoute(routeClass) == true
    }

    // Shared bus for "user tapped the already-selected tab" — tab screens
    // listen via LocalTabReselectEvents and scroll-to-top in response.
    val tabReselectEvents = remember { TabReselectEvents() }

    CompositionLocalProvider(LocalTabReselectEvents provides tabReselectEvents) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    BottomNavigationBar(
                        navController = navController,
                        onReselect = tabReselectEvents::emit
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            // Each screen owns its own status-bar inset (so heroes can paint behind it).
            // Only reserve space for the bottom nav here.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp)
            ) {
                Navigation(
                    navController = navController,
                    viewModel = viewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    onReselect: (Any) -> Unit = {}
) {
    // Remembered so the list isn't rebuilt on every recomposition triggered by navigation
    // Search FAB sits centered (index 2). "See all" on Home is a shortcut into
    // the same Categories grid; both nav paths coexist.
    val items = remember {
        listOf(
            BottomNavItem("Home", Icons.Default.Home, Home),
            BottomNavItem("Categories", Icons.AutoMirrored.Filled.List, Categories),
            BottomNavItem("Search", Icons.Default.Search, Search),
            BottomNavItem("Favorites", Icons.Default.Favorite, Favorites),
            BottomNavItem("Settings", Icons.Default.Settings, Settings)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val hapticFeedback = LocalHapticFeedback.current

    // Tapping the active tab → emit a reselect event (scroll-to-top, etc.) and
    // skip navigation entirely so we never push a duplicate destination.
    // Tapping any other tab → standard navigation, with popUpTo<Home> to keep
    // the stack shallow as the user moves between tabs.
    fun navigateToTab(route: Any, isSelected: Boolean) {
        if (isSelected) {
            onReselect(route)
            return
        }
        navController.navigate(route) {
            popUpTo<Home> { inclusive = false }  // false keeps Home on the stack as root
            launchSingleTop = true
        }
    }

    // Let Material3 NavigationBar handle the system navigation inset itself: items
    // stay centered in the 72dp content area and an extra `inset` of bar color is
    // painted below them, hidden behind 3-button bars and sitting under the gesture
    // pill. No fixed height — the bar grows by the inset so items are never crushed.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            windowInsets = WindowInsets.navigationBars,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            items.forEach { item ->
                val isSelected = currentDestination?.hasRoute(item.route::class) == true
                // Type-safe check instead of fragile string comparison
                val isSearchButton = item.route is Search

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "icon_scale_${item.label}"
                )

                if (isSearchButton) {
                    FloatingActionButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigateToTab(item.route, isSelected)
                        },
                        modifier = Modifier
                            .size(56.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier
                                .size(28.dp)
                                .scale(iconScale)
                        )
                    }
                } else {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(iconScale)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.scale(if (isSelected) 1.05f else 1f)
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            hapticFeedback.performHapticFeedback(
                                if (isSelected) HapticFeedbackType.TextHandleMove
                                else HapticFeedbackType.LongPress
                            )
                            navigateToTab(item.route, isSelected)
                        },
                        modifier = Modifier.scale(0.95f),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)
