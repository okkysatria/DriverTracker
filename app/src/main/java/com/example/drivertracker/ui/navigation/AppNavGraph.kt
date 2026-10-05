package com.example.drivertracker.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.screens.dashboard.DashboardScreen
import com.example.drivertracker.ui.screens.radar.SmartHeatmapScreen
import com.example.drivertracker.ui.screens.recorder.RekamOrderScreen
import com.example.drivertracker.ui.screens.settings.SettingsScreen
import com.example.drivertracker.ui.screens.routeposter.TrackPosterScreen
import kotlinx.serialization.Serializable

val DriverTrackerGreen = Color(0xFF00AA13)
val InactiveGray = Color(0xFF757575)

@Serializable
sealed interface Screen : NavKey {

    @Serializable
    data object RekamOrder : Screen

    @Serializable
    data object SmartRadar : Screen

    @Serializable
    data object TrackPoster : Screen

    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object Settings : Screen

    val title: String
        get() = when (this) {
            RekamOrder -> "Perekam"
            SmartRadar -> "Radar"
            TrackPoster -> "Rute"
            Dashboard -> "Laporan"
            Settings -> "Pengaturan"
        }

    val icon: ImageVector
        get() = when (this) {
            RekamOrder -> Icons.Rounded.PlayArrow
            SmartRadar -> Icons.Rounded.Map
            TrackPoster -> Icons.Rounded.Brush
            Dashboard -> Icons.Rounded.BarChart
            Settings -> Icons.Rounded.Settings
        }

    val route: String
        get() = when (this) {
            RekamOrder -> "rekam_order"
            SmartRadar -> "smart_radar"
            TrackPoster -> "track_poster"
            Dashboard -> "dashboard"
            Settings -> "settings"
        }

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                RekamOrder,
                SmartRadar,
                TrackPoster,
                Dashboard,
                Settings
            )
    }
}

@Composable
fun AppNavGraph(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(Screen.RekamOrder)
    val currentScreen = backStack.lastOrNull() as? Screen ?: Screen.RekamOrder

    val shouldOpenSaveDialog by viewModel.shouldOpenSaveDialog.collectAsStateWithLifecycle()
    LaunchedEffect(shouldOpenSaveDialog) {
        if (shouldOpenSaveDialog) {
            backStack.clear()
            backStack.add(Screen.RekamOrder)
        }
    }

    val entryProvider = remember(viewModel) {
        entryProvider<NavKey> {
            entry<Screen.RekamOrder> {
                RekamOrderScreen(viewModel = viewModel)
            }
            entry<Screen.SmartRadar> {
                SmartHeatmapScreen(viewModel = viewModel)
            }
            entry<Screen.TrackPoster> {
                TrackPosterScreen(viewModel = viewModel)
            }
            entry<Screen.Dashboard> {
                DashboardScreen(viewModel = viewModel)
            }
            entry<Screen.Settings> {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    if (currentScreen != screen) {
                        if (screen == Screen.RekamOrder) {
                            backStack.clear()
                            backStack.add(Screen.RekamOrder)
                        } else {
                            backStack.remove(screen)
                            backStack.add(screen)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = if (isSelected) DriverTrackerGreen else InactiveGray
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DriverTrackerGreen else InactiveGray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DriverTrackerGreen,
                    selectedTextColor = DriverTrackerGreen,
                    unselectedIconColor = InactiveGray,
                    unselectedTextColor = InactiveGray,
                    indicatorColor = DriverTrackerGreen.copy(alpha = 0.15f)
                )
            )
        }
    }
}
